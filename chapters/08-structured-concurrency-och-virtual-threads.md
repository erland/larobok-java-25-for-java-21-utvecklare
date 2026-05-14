# Kapitel 8: Structured Concurrency och virtual threads

## Varför detta kapitel finns

I Java 21 blev **virtual threads** en final feature. Det gjorde det praktiskt att skriva serverkod i en tydlig thread-per-task-stil utan att automatiskt betala samma kostnad som med stora mängder plattformstrådar.

Men virtual threads löser inte alla problem med samtidighet. De gör trådar billigare, inte programlogik enklare av sig själv.

Ett vanligt problem i ett ordersystem är att flera relaterade anrop måste göras parallellt:

- hämta orderinformation
- hämta lagersaldo
- kontrollera betalningsstatus

Om ett av anropen misslyckas, eller om requesten avbryts, ska resten inte fortsätta i bakgrunden. Teamet vill kunna se uppgifterna som en gemensam arbetsenhet, inte som lösa futures som råkar startas från samma metod.

Det är här **Structured Concurrency** kommer in. I Java 25 är API:et fortfarande **preview**, men det visar en viktig riktning: relaterade samtidiga uppgifter bör ha en tydlig livstid, en ägare och ett samlat fel- och avbrottsflöde.

I OrderFlow använder vi Structured Concurrency för ett parallellt serviceanrop där order-, lager- och betalningsdata behövs innan ett beslut kan fattas.

## Lärandemål

Efter kapitlet ska läsaren kunna:

- förklara skillnaden mellan virtual threads och structured concurrency
- beskriva varför Structured Concurrency är preview i Java 25 och vad det betyder för adoption
- använda `StructuredTaskScope.open()` och `fork(...)` i ett litet exempel
- resonera om `join()`, cancellation och task scope som livstidsgräns
- känna igen risker med ostrukturerade `Future`- och `ExecutorService`-mönster
- avgöra när ett parallellt serviceanrop bör skrivas som strukturerad samtidighet

## Innan vi börjar

Från kapitel 7 tar vi med oss att kontext i modern Java bör ha tydlig livstid. Där gällde det data, till exempel korrelations-ID med Scoped Values.

I det här kapitlet gäller samma princip för samtidiga uppgifter.

Vi introducerar tre huvudbegrepp:

- **task scope**: ett avgränsat block där relaterade samtidiga uppgifter startas, väntas in och avslutas
- **cancellation**: avbrott av uppgifter som inte längre behövs eller som hör till en misslyckad helhet
- **join**: punkten där ägartråden väntar in subtasks innan resultatet behandlas

Vi repeterar också ett Java 21-begrepp:

- **virtual thread**: en lättviktig `Thread` som hanteras av JDK:n och lämpar sig väl för många blockerande I/O-uppgifter

## Huvudförklaring

### Virtual threads är billigare trådar, inte automatisk struktur

En virtual thread är fortfarande en tråd. Den har stack, kör sekventiell kod och kan blockera vid I/O utan att binda en plattformstråd under hela väntetiden.

Det gör den mycket attraktiv för serverkod där många uppgifter mest väntar på nätverk, databas eller filsystem.

Men följande kod är fortfarande svår att äga, även om varje uppgift körs i en virtual thread:

```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    Future<OrderData> order = executor.submit(() -> orderClient.fetch(orderId));
    Future<StockData> stock = executor.submit(() -> stockClient.fetch(orderId));
    Future<PaymentData> payment = executor.submit(() -> paymentClient.fetch(orderId));

    return combine(order.get(), stock.get(), payment.get());
}
```

Det är bättre än att skapa plattformstrådar manuellt, men vi behöver fortfarande svara på frågor som:

- Vad händer om `payment.get()` kastar ett undantag?
- Avbryts de andra uppgifterna?
- Är det tydligt i tråddumpen vilka trådar som hör till samma request?
- Vem äger livstiden för subtasks?
- Kan en subtask råka leva längre än requesten?

Structured Concurrency försöker göra svaret tydligare genom att knyta subtasks till ett scope.

### Grundidén: samtidighet som blockstruktur

I vanlig Java är livstiden för en lokal variabel tydlig:

```java
{
    var orderId = "A-100";
    // orderId finns här
}
// orderId finns inte här
```

Structured Concurrency vill ge en liknande känsla för samtidiga uppgifter:

```java
try (var scope = StructuredTaskScope.<Object>open()) {
    var order = scope.fork(() -> fetchOrder(orderId));
    var stock = scope.fork(() -> fetchStock(orderId));
    var payment = scope.fork(() -> fetchPayment(orderId));

    scope.join();

    return combine(order.get(), stock.get(), payment.get());
}
```

När blocket lämnas ska uppgifterna vara avslutade eller avbrutna. Det är en stor skillnad mot att sprida ut `Future`-objekt, callbacks eller executor-ägarskap över flera lager.

### Java 25-status: preview API

I Java 25 är `StructuredTaskScope` ett **preview API**. Det betyder att kapitlets kod är relevant för förståelse, labb och designutvärdering, men att produktionsadoption kräver ett uttryckligt beslut.

Exempel ska kompileras och köras med preview aktiverat:

```bash
javac --release 25 --enable-preview OrderAggregationStructuredDemo.java
java --enable-preview OrderAggregationStructuredDemo
```

Detta är samma typ av försiktighet som i kapitel 6. Skillnaden är att det här gäller ett API för concurrency snarare än språkets pattern matching.

### `StructuredTaskScope.open()`

I Java 25 öppnas ett scope med statiska factory methods. Den enklaste formen är:

```java
try (var scope = StructuredTaskScope.<Object>open()) {
    // fork subtasks
}
```

`try`-blocket är viktigt. Scopet är `AutoCloseable`, och blockstrukturen markerar ägarskapet.

Inuti scopet startas subtasks med `fork(...)`:

```java
var order = scope.fork(() -> orderClient.fetch(orderId));
```

`fork(...)` returnerar en `Subtask`. Efter `join()` kan ägaren läsa resultatet med `get()`.

### `join()` är en designpunkt

`join()` är inte bara “vänta lite”. Det är den punkt där koden säger:

> Här ska de relaterade uppgifterna vara klara, eller också ska helheten betraktas som misslyckad.

I den enklaste policyn väntar scopet på att alla subtasks lyckas eller på att någon misslyckas. Om en subtask misslyckas kan övriga subtasks avbrytas beroende på policy och state.

Det viktiga designbeslutet är att `join()` ligger på ett synligt ställe i koden. Man behöver inte leta efter en separat executor, en separat cleanup-rutin eller en framtida callback för att förstå var samtidigheten samlas ihop.

### Cancellation: misslyckande ska inte lämna skräp

Anta att OrderFlow behöver tre externa svar. Om betalningstjänsten svarar med ett tekniskt fel finns det ofta ingen mening att fortsätta vänta på lagersaldo.

Med ostrukturerad samtidighet är detta lätt att missa:

```java
Future<StockData> stock = executor.submit(...);
Future<PaymentData> payment = executor.submit(...);

// Om payment.get() kastar här, vem avbryter stock?
```

Structured Concurrency gör avbrottsflödet till en del av scopets ansvar. Det betyder inte att all felhantering försvinner, men det gör ägarskapet tydligare.

### Relation till virtual threads

Structured Concurrency och virtual threads är nära relaterade men inte samma sak.

Virtual threads svarar på frågan:

> Hur kan vi köra många blockerande uppgifter billigt?

Structured Concurrency svarar på frågan:

> Hur organiserar vi relaterade samtidiga uppgifter så att livstid, fel och avbrott blir begripliga?

I Java 25 startar subtasks i ett `StructuredTaskScope` som standard i virtual threads. Därför passar API:et särskilt bra för I/O-tunga arbetsflöden.

### Relation till Scoped Values

Scoped Values från kapitel 7 blir särskilt intressanta tillsammans med Structured Concurrency. En request-context kan bindas i ett scope och sedan läsas i underliggande kod. När structured subtasks skapas inom rätt körningsscope kan kontexten följa med på ett mer kontrollerat sätt än med bred, muterbar `ThreadLocal`-användning.

Det betyder inte att all kontext ska göras implicit. Men för korrelations-ID, tenant och request-deadline kan kombinationen vara tydligare än både globala variabler och parameterbrus.

## Exempel: parallell orderaggregering i OrderFlow

Vi bygger ett litet exempel där OrderFlow hämtar tre datadelar parallellt och sedan skapar en sammanfattning.

Kodexemplet finns även i `code/OrderAggregationStructuredDemo.java`.

```java
import java.time.Duration;
import java.util.concurrent.StructuredTaskScope;

public class OrderAggregationStructuredDemo {
    record OrderData(String orderId, String customerId) {}
    record StockData(String orderId, boolean available) {}
    record PaymentData(String orderId, boolean approved) {}
    record OrderDecision(String orderId, boolean accepted, String reason) {}

    public static void main(String[] args) throws InterruptedException {
        var service = new OrderAggregationService(
                new OrderClient(),
                new StockClient(),
                new PaymentClient()
        );

        System.out.println(service.decide("ORDER-1001"));
    }

    static final class OrderAggregationService {
        private final OrderClient orderClient;
        private final StockClient stockClient;
        private final PaymentClient paymentClient;

        OrderAggregationService(
                OrderClient orderClient,
                StockClient stockClient,
                PaymentClient paymentClient
        ) {
            this.orderClient = orderClient;
            this.stockClient = stockClient;
            this.paymentClient = paymentClient;
        }

        OrderDecision decide(String orderId) throws InterruptedException {
            try (var scope = StructuredTaskScope.<Object>open()) {
                var order = scope.fork(() -> orderClient.fetch(orderId));
                var stock = scope.fork(() -> stockClient.fetch(orderId));
                var payment = scope.fork(() -> paymentClient.fetch(orderId));

                scope.join();

                return combine(
                        (OrderData) order.get(),
                        (StockData) stock.get(),
                        (PaymentData) payment.get()
                );
            }
        }

        private OrderDecision combine(
                OrderData order,
                StockData stock,
                PaymentData payment
        ) {
            if (!stock.available()) {
                return new OrderDecision(order.orderId(), false, "out of stock");
            }
            if (!payment.approved()) {
                return new OrderDecision(order.orderId(), false, "payment rejected");
            }
            return new OrderDecision(order.orderId(), true, "accepted");
        }
    }

    static final class OrderClient {
        OrderData fetch(String orderId) throws InterruptedException {
            Thread.sleep(Duration.ofMillis(120));
            return new OrderData(orderId, "CUSTOMER-42");
        }
    }

    static final class StockClient {
        StockData fetch(String orderId) throws InterruptedException {
            Thread.sleep(Duration.ofMillis(80));
            return new StockData(orderId, true);
        }
    }

    static final class PaymentClient {
        PaymentData fetch(String orderId) throws InterruptedException {
            Thread.sleep(Duration.ofMillis(100));
            return new PaymentData(orderId, true);
        }
    }
}
```

Exemplet är avsiktligt litet. Poängen är inte att skapa en fullständig servicearkitektur, utan att visa den nya strukturen:

1. öppna ett scope
2. forka relaterade uppgifter
3. vänta in dem med `join()`
4. läsa resultat
5. lämna blocket utan kvarhängande subtasks

### Om typningen i exemplet

Exemplet använder `StructuredTaskScope.<Object>open()` eftersom våra subtasks returnerar olika typer: `OrderData`, `StockData` och `PaymentData`.

Det är inte alltid den vackraste lösningen. I produktionskod kan man ibland få tydligare kod genom att:

- gruppera resultat i en gemensam sealed hierarchy
- använda separata scopes för homogena uppgifter
- använda en passande `Joiner`
- hålla parallellismen nära en applikationsservice i stället för att sprida den över domänmodellen

Det viktiga är att inte göra concurrency-modellen mer generell än problemet kräver.

## Vanliga misstag

- Misstag: Att tro att virtual threads automatiskt ger strukturerad samtidighet.
  - Varför det händer: Virtual threads gör det lättare att starta många uppgifter.
  - Hur man undviker det: Beskriv alltid vem som äger uppgifterna och när de måste vara avslutade.

- Misstag: Att behandla Structured Concurrency som produktionsstandard utan statusbeslut.
  - Varför det händer: API:et känns naturligt och ligger nära Java 21:s virtual threads.
  - Hur man undviker det: Markera Java 25 Structured Concurrency som preview i arkitekturbeslut, build-filer och dokumentation.

- Misstag: Att forka uppgifter som har dolda sidoreffekter.
  - Varför det händer: Parallell kod gör ordningen mindre uppenbar.
  - Hur man undviker det: Låt subtasks helst hämta data eller beräkna resultat; samla mutationsbeslut efter `join()`.

- Misstag: Att använda Structured Concurrency för CPU-tung parallellism utan analys.
  - Varför det händer: “Parallellt” blandas ihop med “snabbare”.
  - Hur man undviker det: Använd virtual threads främst för många blockerande uppgifter. För CPU-tunga jobb krävs annan kapacitets- och backpressure-design.

- Misstag: Att glömma timeout, deadline eller yttre avbrott.
  - Varför det händer: Det lilla exemplet fungerar utan timeout.
  - Hur man undviker det: I riktig systemdesign ska request-deadline, klienttimeout och service-SLO vara en del av API-kontraktet.

## Övningar

### Övning 1: Identifiera scope-gränsen

Titta på ett befintligt serviceflöde där flera externa anrop görs.

Svara på:

1. Vilka anrop hör till samma request?
2. Vilka måste lyckas för att helheten ska lyckas?
3. Vilka kan avbrytas om ett annat anrop misslyckas?
4. Var i koden finns den naturliga `join()`-punkten?

Målet är inte att skriva om koden direkt, utan att hitta den strukturella formen.

### Övning 2: Skriv om ett `ExecutorService`-exempel

Utgå från kod som använder `Executors.newVirtualThreadPerTaskExecutor()` och tre `Future`-objekt.

Skriv om den till ett `StructuredTaskScope`.

Kontrollera särskilt:

- att scopet är lokalt i metoden
- att alla `fork(...)`-anrop är synliga i samma block
- att resultat bara läses efter `join()`
- att metoden inte returnerar en `Subtask` eller `Future`

### Övning 3: Lägg till fel

Ändra `PaymentClient` i exemplet så att den kastar ett undantag.

Undersök:

1. Var syns felet?
2. Hur påverkas de andra subtasks?
3. Hur skulle du översätta felet till ett domänresultat?
4. Vilken information behöver loggas?

### Fördjupning: Scoped Values + Structured Concurrency

Utöka exemplet med ett korrelations-ID från kapitel 7.

Diskutera:

- Var ska korrelations-ID bindas?
- Vilka subtasks behöver läsa det?
- Bör domänlogiken läsa kontexten direkt, eller ska bara infrastrukturlager göra det?
- Hur dokumenterar teamet gränsen?

## Snabb sammanfattning

- Virtual threads är final sedan Java 21 och gör thread-per-task-stil praktisk för många I/O-tunga system.
- Structured Concurrency i Java 25 är preview och måste därför aktiveras och hanteras som ett medvetet experiment.
- `StructuredTaskScope` grupperar relaterade subtasks till en tydlig arbetsenhet.
- `join()` är den punkt där ägartråden samlar ihop parallellt arbete.
- Ett scope gör livstid, cancellation och felhantering tydligare än spridda `Future`-objekt.
- Structured Concurrency passar särskilt bra för parallella serviceanrop där alla delar hör till samma request.

## Quiz/reflektionsfrågor

1. Vad löser virtual threads som Structured Concurrency inte löser?
2. Vad löser Structured Concurrency som virtual threads inte löser?
3. Varför är det viktigt att `StructuredTaskScope` är blockstrukturerat?
4. Vad innebär det att Structured Concurrency är preview i Java 25?
5. Varför bör resultat från subtasks läsas efter `join()`?
6. När är det bättre att behålla sekventiell kod i stället för att parallellisera?
7. Hur skulle du dokumentera ett team-beslut att prova Structured Concurrency i en labbmodul?

## Nästa steg

Nästa kapitel lämnar trådar och går över till dataflöden. Vi ska titta på **Stream Gatherers** och hur de kan användas för att forma data i pipelines när vanliga `map`, `filter` och `flatMap` inte uttrycker transformationen tillräckligt tydligt.
