# Kapitel 7: Scoped Values i praktiken

## Varför detta kapitel finns

Många Java-system behöver bära med sig kontext genom en körning: korrelations-ID, tenant, användaridentitet, behörighetsinformation, transaktionsmetadata, språkval, request-deadline eller spårningsinformation. I Java 21 löstes detta ofta på tre sätt:

1. skicka kontexten som metodparameter genom hela anropskedjan
2. använda `ThreadLocal`
3. låta ett ramverk bära kontexten åt applikationskoden

Alla tre fungerar, men de har olika kostnader. Metodparametrar är tydliga men kan bli brusiga när informationen bara behövs långt ned i anropskedjan. `ThreadLocal` är praktiskt men har lång livstid, muterbarhet och städningsproblem. Ramverkslösningar kan dölja beroenden så mycket att systemet blir svårt att felsöka.

**Scoped Values** i Java 25 ger ett fjärde alternativ: ett avgränsat, läsbart och i praktiken envägsflöde av kontext från en anropare till dess direkta och indirekta anrop. Det passar särskilt bra när ett värde ska vara tillgängligt under en tydlig del av körningen, men inte längre än så.

I OrderFlow använder vi Scoped Values för ett enkelt men realistiskt problem: att göra ett korrelations-ID tillgängligt i loggning, validering och externa anrop utan att skicka det som parameter genom varje metod.

## Lärandemål

Efter kapitlet ska läsaren kunna:

- förklara skillnaden mellan `ThreadLocal` och `ScopedValue`
- skapa och läsa ett scoped value med `ScopedValue.newInstance()`, `ScopedValue.where(...)`, `run(...)` och `get()`
- beskriva vad dynamiskt scope betyder i praktisk Java-kod
- använda Scoped Values för korrelations-ID i en liten anropskedja
- känna igen när Scoped Values är ett bättre respektive sämre val än metodparametrar
- resonera om livstid, åtkomstkontroll, rebinding och immutable context

## Innan vi börjar

Från tidigare kapitel har vi etablerat två viktiga arbetssätt:

- Nya Java 25-features ska förstås genom sin **feature-status**.
- Adoption ska skilja mellan kompatibilitetsmigration och avsiktligt designval.

Till skillnad från kapitlet om primitive patterns är Scoped Values **final i Java 25**. Det betyder inte att alla `ThreadLocal`-användningar automatiskt ska skrivas om. Det betyder att API:et kan behandlas som en stabil del av plattformen och utvärderas för produktionskod.

Det här kapitlet introducerar två huvudbegrepp:

- **scoped value**: en nyckel som kan bindas till ett värde under ett avgränsat dynamiskt scope
- **dynamiskt scope**: den körning som startar i ett anrop till exempelvis `run(...)` och omfattar de metoder som anropas direkt eller indirekt därifrån

## Huvudförklaring

### Problemet: kontext som inte hör hemma överallt

Anta att OrderFlow tar emot en orderbegäran. Vi vill att alla loggrader och vissa domänkontroller ska kunna se samma korrelations-ID.

Med rena parametrar kan det se ut så här:

```java
final class OrderService {
    Receipt placeOrder(OrderRequest request, String correlationId) {
        validate(request, correlationId);
        reserveInventory(request, correlationId);
        return createReceipt(request, correlationId);
    }

    private void validate(OrderRequest request, String correlationId) {
        OrderLogger.info(correlationId, "Validating order " + request.orderId());
    }
}
```

Detta är explicit och enkelt att testa. Men i en större kodbas sprids `correlationId` snabbt:

```java
service -> validator -> policy -> logger -> audit -> outbound client
```

Alla lager behöver inte egentligen *äga* korrelations-ID:t. Vissa skickar bara vidare parametern för att ett senare lager ska kunna läsa den. Då kan parameterlistor bli svårare att förstå.

### Java 21-lösningen: ThreadLocal

En vanlig lösning är `ThreadLocal`:

```java
final class RequestContextHolder {
    private static final ThreadLocal<RequestContext> CURRENT = new ThreadLocal<>();

    static void set(RequestContext context) {
        CURRENT.set(context);
    }

    static RequestContext get() {
        return CURRENT.get();
    }

    static void clear() {
        CURRENT.remove();
    }
}
```

Användningen kan bli smidig:

```java
try {
    RequestContextHolder.set(context);
    service.placeOrder(request);
} finally {
    RequestContextHolder.clear();
}
```

Problemet är inte att `ThreadLocal` är oanvändbart. Problemet är att det är lätt att ge värdet fel livstid.

Vanliga problem:

- `clear()` glöms bort
- värdet muteras längre ned i anropskedjan
- trådar återanvänds i thread pools
- relationen mellan bindning och användning syns dåligt i koden
- många virtuella trådar gör vissa gamla antaganden om trådassocierad kontext mindre attraktiva

`ThreadLocal` kan fortfarande vara rätt verktyg, särskilt för befintliga ramverk och integrationer. Men när målet är envägsdelning av immutable context under en tydlig körning är Scoped Values ofta lättare att resonera om.

### Grundidén i Scoped Values

Ett scoped value består av en nyckel och en tillfällig bindning.

Nyckeln deklareras typiskt som `static final`:

```java
import java.lang.ScopedValue;

final class OrderContext {
    static final ScopedValue<RequestContext> CURRENT =
            ScopedValue.newInstance();

    private OrderContext() {
    }
}
```

Sedan binds ett värde under en körning:

```java
ScopedValue.where(OrderContext.CURRENT, context)
        .run(() -> service.placeOrder(request));
```

Kod som körs direkt eller indirekt inom `run(...)` kan läsa värdet:

```java
RequestContext context = OrderContext.CURRENT.get();
```

När `run(...)` är färdig blir bindningen automatiskt otillgänglig. Det gäller både vid normal retur och vid exception. Det finns alltså inget motsvarande `clear()` som utvecklaren måste komma ihåg.

### Dynamiskt scope

Ordet *scope* brukar ofta betyda var i källkoden en variabel kan användas. Scoped Values handlar i stället om **dynamiskt scope**: vilka anrop som sker under en viss körning.

Förenklat:

```java
ScopedValue.where(KEY, value).run(() -> a());

void a() {
    b();
}

void b() {
    c();
}

void c() {
    System.out.println(KEY.get());
}
```

`c()` kan läsa värdet eftersom `c()` körs indirekt från `run(...)`.

Men efter att `run(...)` är klar är värdet inte längre bundet:

```java
ScopedValue.where(KEY, value).run(() -> a());

// Här är KEY inte längre bundet.
```

Det här är den stora praktiska skillnaden mot många `ThreadLocal`-mönster. Livstiden syns i strukturen:

```java
where(...).run(...)
```

Det är svårare att råka bära med sig gammal request-kontext till nästa request.

### Ett första OrderFlow-exempel

Vi börjar med en liten context-record:

```java
record RequestContext(String correlationId, String tenant) {
}
```

Sedan skapar vi en hållare för vårt scoped value:

```java
import java.lang.ScopedValue;

final class OrderRequestContext {
    static final ScopedValue<RequestContext> CURRENT =
            ScopedValue.newInstance();

    private OrderRequestContext() {
    }

    static RequestContext current() {
        return CURRENT.orElseThrow(() ->
                new IllegalStateException("No request context is bound"));
    }
}
```

Notera att vi kapslar in `CURRENT`. I riktig kod kan du ofta vilja göra själva nyckeln `private` och bara exponera metoder som passar din design. Ett `ScopedValue` fungerar som en slags åtkomstnyckel: kod som kan se nyckeln kan läsa värdet när det är bundet.

En enkel service:

```java
final class OrderService {
    Receipt placeOrder(OrderRequest request) {
        RequestContext context = OrderRequestContext.current();

        OrderLogger.info("Placing order " + request.orderId());
        return new Receipt(request.orderId(), context.correlationId());
    }
}
```

Loggaren kan också läsa samma context:

```java
final class OrderLogger {
    static void info(String message) {
        RequestContext context = OrderRequestContext.current();
        System.out.println("[%s] [%s] %s".formatted(
                context.correlationId(),
                context.tenant(),
                message
        ));
    }
}
```

Yttre kod binder contexten:

```java
RequestContext context =
        new RequestContext("corr-2026-05-14-001", "nordic-shop");

ScopedValue.where(OrderRequestContext.CURRENT, context)
        .run(() -> orderService.placeOrder(request));
```

Det viktiga är att `OrderService` inte längre behöver ta `correlationId` som parameter om värdet bara är tvärgående körningskontext.

### `run(...)` och `call(...)`

`run(...)` används när operationen inte behöver returnera ett värde:

```java
ScopedValue.where(OrderRequestContext.CURRENT, context)
        .run(() -> audit(request));
```

När operationen ska returnera något används `call(...)`:

```java
Receipt receipt = ScopedValue
        .where(OrderRequestContext.CURRENT, context)
        .call(() -> orderService.placeOrder(request));
```

Det här är ofta praktiskt i request-handlers, batch-steg och testkod.

### `get()`, `orElse(...)` och `orElseThrow(...)`

Det finns tre vanliga sätt att läsa ett scoped value:

```java
RequestContext context = CURRENT.get();
```

`get()` är bra när frånvaro är ett programmeringsfel. Om värdet inte är bundet kastas exception.

```java
RequestContext context = CURRENT.orElse(defaultContext);
```

`orElse(...)` är bra när det finns ett rimligt defaultvärde. I Java 25 accepterar `orElse(...)` inte längre `null` som fallback-argument, så använd ett riktigt defaultobjekt eller `orElseThrow(...)`.

```java
RequestContext context = CURRENT.orElseThrow(() ->
        new IllegalStateException("Missing request context"));
```

`orElseThrow(...)` är ofta mest uttrycksfullt i applikationskod där frånvaro ska ge ett domän- eller konfigurationsfel.

### Rebinding: ett nytt värde i ett inre scope

Scoped Values saknar `set()`-metod. Det betyder att en callee inte kan ändra värdet för sin caller. Däremot kan man skapa ett nytt inre scope med en ny bindning.

```java
ScopedValue.where(OrderRequestContext.CURRENT, outerContext)
        .run(() -> {
            OrderLogger.info("outer");

            RequestContext innerContext =
                    new RequestContext("corr-inner", outerContext.tenant());

            ScopedValue.where(OrderRequestContext.CURRENT, innerContext)
                    .run(() -> OrderLogger.info("inner"));

            OrderLogger.info("outer again");
        });
```

Körningen kan förstås så här:

- första loggraden ser `outerContext`
- det inre scopet ser `innerContext`
- efter det inre scopet återgår läsningen till `outerContext`
- efter det yttre scopet är inget värde bundet

Rebinding ska användas sparsamt. Om den används för mycket blir kontextflödet lika svårt att följa som muterbar global state. Men det är användbart vid till exempel delegerade operationer, testfall, temporär impersonering eller isolerade interna subflöden.

### Varför immutable context spelar roll

Ett scoped value hindrar inte att objektet du binder är muterbart. Det hindrar bara att själva bindningen ändras från en callee.

Det här är alltså en dålig idé:

```java
final class MutableRequestContext {
    String correlationId;
    String tenant;
}
```

Om flera metoder kan ändra samma contextobjekt försvinner mycket av vinsten. Använd hellre records eller andra immutable värdeobjekt:

```java
record RequestContext(String correlationId, String tenant) {
}
```

När context behöver förändras, skapa ett nytt objekt och bind det i ett inre scope.

## Exempel: Korrelations-ID i OrderFlow

Följande exempel är avsiktligt litet. Det visar kärnmönstret utan ramverk.

```java
import java.lang.ScopedValue;

public class OrderFlowScopedValuesDemo {
    private static final ScopedValue<RequestContext> REQUEST_CONTEXT =
            ScopedValue.newInstance();

    public static void main(String[] args) throws Exception {
        var service = new OrderService();

        var context = new RequestContext(
                "corr-2026-05-14-001",
                "nordic-shop"
        );

        var request = new OrderRequest("order-1001", 3);

        Receipt receipt = ScopedValue
                .where(REQUEST_CONTEXT, context)
                .call(() -> service.placeOrder(request));

        System.out.println("Receipt: " + receipt);
    }

    record RequestContext(String correlationId, String tenant) {
    }

    record OrderRequest(String orderId, int quantity) {
    }

    record Receipt(String orderId, String correlationId) {
    }

    static final class OrderService {
        Receipt placeOrder(OrderRequest request) {
            validate(request);
            log("Order accepted: " + request.orderId());

            RequestContext context = currentContext();
            return new Receipt(request.orderId(), context.correlationId());
        }

        private void validate(OrderRequest request) {
            if (request.quantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be positive");
            }
            log("Validated quantity: " + request.quantity());
        }
    }

    static void log(String message) {
        RequestContext context = currentContext();
        System.out.println("[%s] [%s] %s".formatted(
                context.correlationId(),
                context.tenant(),
                message
        ));
    }

    static RequestContext currentContext() {
        return REQUEST_CONTEXT.orElseThrow(() ->
                new IllegalStateException("No request context is bound"));
    }
}
```

Kompilering och körning med JDK 25:

```bash
javac OrderFlowScopedValuesDemo.java
java OrderFlowScopedValuesDemo
```

Till skillnad från preview-exemplen i kapitel 6 behövs ingen `--enable-preview` för Scoped Values i Java 25.

### Vad exemplet visar

Exemplet visar fyra saker:

1. `REQUEST_CONTEXT` är nyckeln.
2. `ScopedValue.where(...).call(...)` binder värdet under serviceanropet.
3. `validate(...)`, `log(...)` och `placeOrder(...)` kan läsa samma context utan parametertrådning.
4. När `call(...)` är klar upphör bindningen automatiskt.

Detta är inte ett argument för att ta bort alla context-parametrar. Det är ett argument för att använda parametrar för domändata och Scoped Values för välavgränsad körningskontext.

## När Scoped Values passar

Scoped Values passar bra när alla följande punkter stämmer:

- värdet är context, inte huvuddatan i domänmodellen
- värdet ska flöda från caller till callee
- callee ska inte kunna ändra caller-värdet
- livstiden ska vara tydligt kopplad till ett anrop
- objektet som binds är immutable eller behandlas som immutable
- antalet olika scoped values är litet

Typiska exempel:

- korrelations-ID
- tenant-ID
- request-metadata
- tracing-context
- säkerhetsprincipal i ett ramverk
- deadline eller timeout-budget
- testkontext i små integrationstester

## När Scoped Values inte passar

Scoped Values är inte rätt lösning för allt som känns globalt.

Undvik dem när:

- värdet är central domändata som bör synas i metodsignaturen
- metoden blir svår att förstå utan dold context
- contexten behöver ändras stegvis av många lager
- värdet ska leva längre än ett tydligt anrop
- du behöver kommunikation från callee tillbaka till caller
- API:et används av kod som inte ska ha tillgång till contextnyckeln
- befintliga ramverk redan hanterar context på ett tydligt och säkert sätt

En enkel tumregel:

> Om läsaren av metodsignaturen behöver känna till värdet för att förstå vad metoden gör, använd parameter. Om värdet är tvärgående körningsmetadata kan Scoped Values vara rimligt.

## Vanliga misstag

### Misstag: Att behandla Scoped Values som global state

Varför det händer: API:et kan läsas från flera platser utan parameter.

Hur man undviker det: Begränsa åtkomsten till nyckeln. Lägg den i en liten contextklass, använd `private static final` när det går, och exponera bara tydliga läsmetoder.

### Misstag: Att binda muterbara objekt

Varför det händer: Det är enkelt att binda vilket objekt som helst.

Hur man undviker det: Bind records eller immutable klasser. Om något behöver ändras, skapa ett nytt objekt och använd rebinding i ett inre scope.

### Misstag: Att ersätta alla parametrar

Varför det händer: Det känns bekvämt att slippa långa parameterlistor.

Hur man undviker det: Låt domändata vara parametrar. Använd Scoped Values för tvärgående kontext, inte för kärndata som order, kund, pris eller produkt.

### Misstag: Att glömma felbeteende när värdet saknas

Varför det händer: `get()` är kort och lätt att använda.

Hur man undviker det: Skapa en central `currentContext()` som använder `orElseThrow(...)` med ett tydligt felmeddelande.

### Misstag: För många scoped values

Varför det händer: Varje liten contextbit får en egen nyckel.

Hur man undviker det: Samla relaterad context i ett record, till exempel `RequestContext`, och bind ett enda scoped value.

## Övningar

### Övning 1: Ersätt ThreadLocal

Utgå från följande kod:

```java
final class Correlation {
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    static void set(String id) {
        CURRENT.set(id);
    }

    static String get() {
        return CURRENT.get();
    }

    static void clear() {
        CURRENT.remove();
    }
}
```

Skriv om den till en lösning med `ScopedValue<String>`. Se till att anropande kod inte behöver komma ihåg `clear()`.

### Övning 2: Gör contexten immutable

Skapa ett record:

```java
record RequestContext(String correlationId, String tenant, String userId) {
}
```

Bind detta record med `ScopedValue.where(...)` och använd det i tre metoder:

- `validateOrder(...)`
- `reserveInventory(...)`
- `writeAuditEvent(...)`

Fundera på vilka av dessa metoder som egentligen bör ta context som parameter och vilka som rimligen kan läsa den från scoped value.

### Övning 3: Rebinding i ett inre scope

Skapa ett yttre `RequestContext` med tenant `nordic-shop`. Skapa sedan ett inre scope där `userId` byts till `system-job`.

Verifiera med loggutskrifter att:

- yttre scope ser första användaren
- inre scope ser `system-job`
- yttre scope återställs efter det inre scopet

### Fördjupning: Designregel för teamet

Skriv en kort teamregel för när OrderFlow får använda Scoped Values. Regeln ska innehålla:

- vilka typer av data som får bindas
- vilka typer av data som inte får bindas
- hur nycklar ska kapslas in
- hur frånvaro ska hanteras
- hur kodgranskning ska upptäcka missbruk

## Snabb sammanfattning

- Scoped Values är final i Java 25.
- De passar för envägsdelning av immutable context från caller till callee.
- Bindningen är avgränsad till ett dynamiskt scope.
- När `run(...)` eller `call(...)` avslutas försvinner bindningen automatiskt.
- Scoped Values är ofta lättare att resonera om än `ThreadLocal` för request-context, särskilt när livstiden ska vara tydlig.
- De ersätter inte vanliga parametrar för central domändata.
- Bind hellre ett immutable context-record än många små separata värden.

## Quiz/reflektionsfrågor

1. Vad är skillnaden mellan lexikalt scope och dynamiskt scope i samband med Scoped Values?
2. Varför är ett scoped value inte samma sak som en global variabel?
3. Vilka problem med `ThreadLocal` försöker Scoped Values minska?
4. Varför bör objektet som binds vara immutable?
5. När är en vanlig metodparameter bättre än ett scoped value?
6. Vad händer med bindningen efter att `ScopedValue.where(...).run(...)` har avslutats?
7. Hur kan rebinding både vara användbart och farligt?
8. Varför är det ofta bättre att binda ett `RequestContext`-record än tre separata scoped values?

## Nästa steg

I det här kapitlet använde vi Scoped Values för kontext inom en anropskedja. Nästa kapitel går vidare till **Structured Concurrency och virtual threads**. Där blir frågan större: hur kan flera samtidiga uppgifter startas, avbrytas, joinas och förstås som en sammanhängande arbetsenhet?

Scoped Values och structured concurrency hör ihop i många moderna Java-designs, eftersom context ofta behöver följa med från ett yttre request-scope till kontrollerade child tasks. Nästa kapitel bygger vidare på just den kopplingen.
