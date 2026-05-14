# Kapitel 10: AOT, Compact Object Headers och GC-nyheter

## Varför detta kapitel finns

När ett team uppgraderar från Java 21 till Java 25 är det lätt att fokusera på språkfeatures. Men för många produktionssystem är de mest affärsnära effekterna inte ny syntax utan kortare starttid, stabilare warmup, lägre minnesfotavtryck och bättre garbage collection-beteende.

Det här kapitlet handlar om den delen av Java 25: prestandarelaterade JVM-nyheter.

Vi kommer inte att lova att varje system automatiskt blir snabbare. En erfaren systemutvecklare vet att prestanda alltid beror på lastprofil, datamodell, heapstorlek, containergränser, GC-val, klassladdning, ramverk och mätmetod. Målet är i stället att ge en praktisk karta:

- vad som kan påverka starttid
- vad som kan påverka warmup
- vad som kan påverka minnesfotavtryck
- vad som kan påverka GC-beteende
- hur OrderFlow-teamet bör mäta före och efter

I OrderFlow använder vi kapitlet för att skapa en mätplan. Vi vill veta om Java 25 kan ge bättre uppstart i testmiljö, lägre heaptryck i orderanalys och färre GC-relaterade latensspikar. Men vi inför inga JVM-flaggor i produktion utan jämförbara mätningar.

## Lärandemål

Efter kapitlet ska läsaren kunna:

- förklara skillnaden mellan starttid, warmup, throughput, latency och minnesfotavtryck
- beskriva vad en AOT cache är och varför den kräver en representativ training run
- använda AOT-relaterade kommandon som experiment i labbmiljö
- förklara vad compact object headers försöker optimera
- resonera om när compact object headers kan vara relevanta för objektintensiva system
- skilja mellan Java 25-nyheter för G1, ZGC och Shenandoah utan att blanda ihop dem
- ta fram en enkel, reproducerbar mätplan för en Java 21-till-Java 25-migrering

## Innan vi börjar

Från kapitel 2 tar vi med oss principen att en migrering först ska bevisa kompatibilitet och därefter utvärdera nya möjligheter. Från kapitel 8 tar vi med oss att JVM-beteende måste förstås i relation till belastning och livstid, inte bara som en flagga på kommandoraden.

Det här kapitlet introducerar tre huvudbegrepp:

- **AOT cache**: en cache som skapas före en produktionskörning och kan innehålla information som gör start och warmup effektivare
- **object header**: JVM:ens metadata per objekt, exempelvis information som behövs för identitet, låsning och klasspekare
- **garbage collector mode**: ett driftläge för en garbage collector, exempelvis generational eller non-generational

## Huvudförklaring

### Börja med rätt prestandaord

Innan vi tittar på Java 25 behöver teamet vara överens om vad som ska förbättras.

**Starttid** är tiden från att processen startas till att applikationen är redo att ta emot trafik. För en CLI, en serverless-funktion eller en container som ofta skalas upp kan detta vara affärskritiskt.

**Warmup** är tiden tills JVM:en har samlat nog med information för att JIT-kompilera viktiga metoder effektivt. En applikation kan vara “startad” men ändå inte ha stabil throughput eller latency.

**Throughput** är hur mycket arbete systemet hinner göra per tidsenhet, till exempel order per sekund.

**Latency** är hur lång tid en enskild operation tar. För backend-system är percentiler viktigare än medelvärde: p95 och p99 säger ofta mer än average.

**Minnesfotavtryck** är hur mycket minne processen och heapen använder. I containerdrift påverkar det både kostnad och risken för OOM.

**GC-beteende** handlar om hur garbage collectorn påverkar CPU, pausmönster, heaputnyttjande och latens.

En Java 25-uppgradering kan påverka flera av dessa samtidigt. Därför är första designregeln:

> Mät ett mål i taget. Inför inte AOT, compact object headers och GC-byte samtidigt om syftet är att förstå effekten.

### AOT i Java 25: tidigare arbete, inte statisk native-kompilering

När många hör “AOT” tänker de på native images eller full statisk kompilering. Det är inte det vi menar här.

I Java 25-sammanhang handlar AOT-nyheterna i HotSpot om att flytta vissa JVM-arbeten tidigare i tiden. JDK 24 introducerade AOT class loading and linking. JDK 25 bygger vidare med enklare kommandoergonomi och AOT method profiling.

En AOT cache kan hjälpa JVM:en att återanvända information från en tidigare körning. Det kan minska arbete vid start och ge bättre underlag för tidig optimering. Den ersätter inte JIT-kompilatorn, och den gör inte Java-programmet till en fristående native-binär.

Det viktiga är training run.

En **training run** är en körning där applikationen startas och övar de kodvägar som senare ska gynnas av cachen. Om training run bara startar applikationen men aldrig initierar viktiga moduler, laddar relevanta klasser eller kör vanliga flöden, blir cachen mindre relevant.

För OrderFlow betyder det att en training run bör göra mer än att starta Spring- eller Jakarta-applikationen. Den bör till exempel:

- läsa konfiguration
- initiera databaslager, men gärna mot testdubbel eller lokal testdatabas
- skapa klienter för externa tjänster
- köra ett representativt orderflöde
- trigga de vanligaste validerings- och beräkningsvägarna
- undvika sällsynta batchjobb om de inte behövs vid normal startup

### Ett AOT-experiment i labbmiljö

Anta att OrderFlow paketeras som `orderflow.jar` och har en vanlig main-klass:

```text
com.example.orderflow.OrderFlowApplication
```

I Java 25 kan teamet skapa en AOT cache med förenklad kommandoergonomi:

```bash
java -XX:AOTCacheOutput=orderflow.aot \
     -cp orderflow.jar \
     com.example.orderflow.OrderFlowApplication \
     --training-mode
```

Sedan kan en testkörning använda cachen:

```bash
java -XX:AOTCache=orderflow.aot \
     -cp orderflow.jar \
     com.example.orderflow.OrderFlowApplication
```

I en verklig produktionspipeline ska detta inte bara klistras in i Dockerfilen. Teamet behöver först svara på några frågor:

- Vilken körning skapar cachen?
- Är training run deterministisk nog?
- Innehåller cachen testklasser eller testkonfiguration som inte hör hemma i produktion?
- Hur versioneras cachen tillsammans med applikationsartefakten?
- Vad händer om JVM:en varnar för att cachen inte kan användas?
- Mäter vi starttid, warmup eller båda?

En bra första regel är att behandla AOT cache som en byggartefakt som hör ihop med exakt den applikationsversion, JDK-version och JVM-konfiguration den skapades för.

### AOT method profiling: snabbare väg till bra JIT-beslut

JIT-kompilatorn behöver profiler för att veta vilka metoder och kodvägar som är viktiga. Traditionellt samlas den informationen in under körning. Det betyder att en process kan vara långsammare i början och sedan stabilisera sig.

AOT method profiling låter JVM:en använda profiler från en tidigare training run. Det kan ge JIT-kompilatorn bättre underlag tidigare. Men det är fortfarande dynamisk Java:

- produktionen kan fortsätta profilera
- JIT kan fortfarande ändra beslut
- felaktig eller orepresentativ training run kan ge mindre nytta
- effekten måste mätas på applikationens faktiska start- och lastprofil

För OrderFlow är detta särskilt intressant i miljöer där nya pods ofta startas och snabbt får trafik. Om varje pod måste “komma i form” under de första minuterna kan warmup vara lika viktig som rå startup time.

### Compact Object Headers: mindre metadata per objekt

Varje objekt i Java har metadata. Den syns inte i koden, men den finns i minnet. För applikationer med många små objekt kan sådan metadata bli en märkbar del av heapen.

**Compact Object Headers** gör objektens header-layout mer kompakt. Poängen är inte att ett enskilt `OrderLine`-objekt plötsligt blir dramatiskt billigare. Poängen är att många små objekt multiplicerar overhead.

Det kan vara relevant i system som:

- skapar många små domänobjekt
- håller stora mängder cachedata i heapen
- använder objektintensiva datastrukturer
- kör med hårda minnesgränser i containers
- har GC-tryck som delvis drivs av heapfotavtryck

Det är mindre sannolikt att ge stor effekt om applikationen främst domineras av stora byte-arrayer, externa native-buffertar, databasanrop eller nätverkslatens.

I Java 25 är compact object headers en produktfeature, men det betyder inte att den ska aktiveras blint. Den ska mätas.

Exempel på labbkörning:

```bash
java -XX:+UseCompactObjectHeaders \
     -Xms2g -Xmx2g \
     -jar orderflow.jar
```

Jämför med baslinjen:

```bash
java -Xms2g -Xmx2g \
     -jar orderflow.jar
```

Mät minst:

- heap used efter steady state
- antal och längd på GC-pauser
- CPU under samma last
- throughput
- p95/p99-latency
- eventuella JVM-varningar eller inkompatibiliteter

### GC-nyheter: inte ett automatiskt GC-byte

Java 25 innehåller flera GC-relaterade förändringar sedan Java 21. För OrderFlow räcker det inte att säga “nyare GC är bättre”. Val av garbage collector är ett produktionsbeslut.

#### G1

G1 är ofta standardvalet i många servermiljöer. Sedan Java 21 finns förbättringar som region pinning och late barrier expansion. För en vanlig migrering är G1 därför en rimlig startbaslinje: uppgradera JDK, behåll GC-valet och mät.

Det viktiga är att inte tolka en JDK-uppgradering och ett GC-byte som samma experiment.

#### ZGC

ZGC fick generational mode som default i JDK 23 och non-generational mode togs bort i JDK 24. För ett team som redan använder ZGC i Java 21 betyder det att Java 25-beteendet bör granskas särskilt, även om kommandoraden ser bekant ut.

Frågorna är:

- använder vi ZGC i dag?
- har vi explicit konfigurerat ZGC-lägen?
- har vi latensmål som motiverar ZGC?
- har vi mätdata från Java 21 att jämföra med?
- behöver runbooks eller dashboards uppdateras?

#### Shenandoah

Generational Shenandoah blev produktfeature i JDK 25. Det betyder att läget inte längre kräver samma experimental-upplåsning som tidigare, men det betyder inte att Shenandoah byter default-läge automatiskt.

Det passar i kapitlets bredare princip: en feature kan vara produktionsmogen som JVM-feature utan att vara rätt default för varje system.

### Exempel: OrderFlow skapar en mätmatris

OrderFlow-teamet väljer fyra körningar:

| Körning | JDK | JVM-flaggor | Syfte |
|---|---:|---|---|
| Baslinje | 21 | nuvarande produktionsflaggor | Mät nuläge |
| Ren migrering | 25 | samma avsikt som baslinjen | Separera JDK-effekt |
| AOT-labb | 25 | `-XX:AOTCache=...` | Mät startup/warmup |
| COH-labb | 25 | `-XX:+UseCompactObjectHeaders` | Mät minnesfotavtryck |

Teamet lägger till en femte körning först senare om GC-byte är aktuellt.

| Körning | JDK | JVM-flaggor | Syfte |
|---|---:|---|---|
| GC-labb | 25 | exempelvis ZGC eller Shenandoah-konfiguration | Mät GC-specifik hypotes |

Det här är långsammare än att testa allt samtidigt, men det ger bättre beslut. Om latency förbättras eller försämras vet teamet vilken ändring som sannolikt orsakade effekten.

## Exempel

Följande lilla program är inte ett benchmark. Det är ett mätbart demoobjekt som kan användas för att visa startup, heap och objektallokering på ett kontrollerat sätt.

```java
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class OrderFlowPerformanceProbe {
    record OrderLine(String sku, int quantity, long priceInOre) {}
    record Order(String id, List<OrderLine> lines) {}

    public static void main(String[] args) {
        var start = Instant.now();

        int orders = Integer.getInteger("orders", 200_000);
        int linesPerOrder = Integer.getInteger("lines", 5);

        var generated = new ArrayList<Order>(orders);

        for (int i = 0; i < orders; i++) {
            var lines = new ArrayList<OrderLine>(linesPerOrder);
            for (int j = 0; j < linesPerOrder; j++) {
                lines.add(new OrderLine("SKU-" + j, j + 1, 10_00L + j));
            }
            generated.add(new Order("ORDER-" + i, List.copyOf(lines)));
        }

        long total = generated.stream()
                .flatMap(order -> order.lines().stream())
                .mapToLong(line -> line.quantity() * line.priceInOre())
                .sum();

        var duration = Duration.between(start, Instant.now());

        System.out.println("orders=" + orders);
        System.out.println("linesPerOrder=" + linesPerOrder);
        System.out.println("total=" + total);
        System.out.println("durationMillis=" + duration.toMillis());
        System.out.println("usedMemoryMiB=" + usedMemoryMiB());
    }

    private static long usedMemoryMiB() {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        return used / 1024 / 1024;
    }
}
```

Kompilera:

```bash
javac OrderFlowPerformanceProbe.java
```

Kör baslinje:

```bash
java -Xms1g -Xmx1g OrderFlowPerformanceProbe
```

Kör med compact object headers:

```bash
java -XX:+UseCompactObjectHeaders \
     -Xms1g -Xmx1g \
     OrderFlowPerformanceProbe
```

Kör med GC-loggning:

```bash
java -Xms1g -Xmx1g \
     -Xlog:gc*:file=gc.log:time,uptime,level,tags \
     OrderFlowPerformanceProbe
```

Det här räcker inte för att fatta ett produktionsbeslut, men det räcker för att teamet ska se hur man isolerar en hypotes.

## Vanliga misstag

- Misstag: Att kalla AOT cache för “native compilation”.
  - Varför det händer: Ordet AOT används i flera olika sammanhang.
  - Hur man undviker det: Skriv uttryckligen “HotSpot AOT cache” i dokumentation och skilj det från native image-lösningar.

- Misstag: Att skapa en training run som inte liknar produktion.
  - Varför det händer: Det är enkelt att starta applikationen men svårare att återskapa vanliga kodvägar.
  - Hur man undviker det: Definiera en minimal produktionsliknande träningssekvens och versionera den.

- Misstag: Att mäta startup, warmup, throughput och latency i samma experiment utan separata hypoteser.
  - Varför det händer: Prestanda diskuteras ofta som ett enda ord.
  - Hur man undviker det: Skriv en mätmatris med en primär metrisk per körning.

- Misstag: Att aktivera compact object headers för att “det är nytt”.
  - Varför det händer: Featuren är lockande och enkel att prova.
  - Hur man undviker det: Kräv heap-, GC- och latencydata före adoption.

- Misstag: Att byta garbage collector samtidigt som JDK uppgraderas.
  - Varför det händer: Migreringen känns som ett naturligt tillfälle att städa JVM-flaggor.
  - Hur man undviker det: Gör först en ren Java 25-baslinje och därefter separata GC-experiment.

## Övningar

### Övning 1: Bygg en mätmatris

Utgå från ett system du känner till. Skapa en tabell med minst fyra körningar:

1. Java 21-baslinje
2. Java 25 utan feature-adoption
3. Java 25 med AOT cache
4. Java 25 med compact object headers

För varje körning, ange:

- JVM-flaggor
- primär metrisk
- sekundära metrikvärden
- hur länge testet ska köras
- vad som räknas som förbättring
- vad som räknas som regressionsrisk

### Övning 2: Designa en training run

Beskriv en training run för OrderFlow eller ett eget system.

Den ska innehålla:

- vilka klasser och moduler som bör laddas
- vilka externa beroenden som ska mockas
- vilka kodvägar som måste köras
- vilka kodvägar som medvetet ska undvikas
- hur training run ska versioneras

### Övning 3: Analysera object header-hypotesen

Välj en del av systemet där många små objekt skapas eller hålls i minne.

Besvara:

- Vilka objekt dominerar antalet instanser?
- Är minnesfotavtryck eller GC-tryck ett verkligt problem?
- Hur skulle du mäta skillnaden med och utan compact object headers?
- Vilka risker finns om effekten är positiv i labb men svag i produktion?

### Fördjupning

Skapa en liten benchmarkplan, men implementera den inte direkt.

Planen ska innehålla:

- varför vanlig `System.currentTimeMillis()` inte räcker för mikromätningar
- när JMH är bättre
- när full systemlast är bättre än mikrobenchmark
- hur du undviker att mäta testmiljön i stället för applikationen

## Snabb sammanfattning

- Java 25 innehåller flera JVM-nyheter som kan påverka startup, warmup, minne och GC-beteende.
- AOT cache handlar om att flytta vissa JVM-arbeten tidigare, inte om att skapa en native-binär.
- En AOT-lösning är bara så bra som sin training run.
- AOT method profiling kan ge JIT-kompilatorn bättre information tidigare, men produktionen fortsätter vara dynamisk.
- Compact Object Headers kan minska metadataöverhead per objekt och är särskilt intressant för objektintensiva system.
- GC-nyheter ska utvärderas som separata experiment, inte blandas ihop med själva Java 25-migreringen.
- Prestandabeslut kräver mätplan, baslinje och tydliga acceptanskriterier.

## Quiz/reflektionsfrågor

1. Vad är skillnaden mellan starttid och warmup?
2. Varför räcker det inte att skapa en AOT cache genom att bara starta applikationen?
3. Varför ska AOT cache inte beskrivas som native-kompilering?
4. Vilken typ av applikation kan tänkas vinna mest på compact object headers?
5. Varför bör ett team undvika att byta garbage collector samtidigt som de först uppgraderar till Java 25?
6. Vad behöver ingå i en mätplan innan en JVM-flagga införs i produktion?
7. Hur skulle du förklara skillnaden mellan “feature är produktmogen” och “feature är rätt för vårt system”?

## Nästa steg

I nästa kapitel går vi från prestandaexperiment till observability. Vi tittar på JFR-nyheter i Java 25 och hur Flight Recorder kan användas för CPU, sampling och metodtracing när OrderFlow-teamet behöver förstå vad JVM:en faktiskt gör.
