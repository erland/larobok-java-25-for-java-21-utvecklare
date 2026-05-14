# Kapitel 9: Stream Gatherers och nya sätt att forma dataflöden

## Varför detta kapitel finns

Java Streams har länge varit bra för linjära datatransformationer: filtrera, mappa, sortera och samla ihop resultat. För många kodbaser räcker det långt. Men erfarna Java-utvecklare känner också igen luckorna.

När ett dataflöde behöver formas i grupper, glidande fönster, stegvisa aggregationer eller tillståndsbaserade transformationer blir vanliga streams ofta svårare än de borde vara. Då hamnar logiken lätt i någon av tre kompromisser:

- ett `for`-block som gör allt tydligt men bryter pipeline-stilen
- en `Collector` som egentligen används för en mellanliggande transformation
- en egen iterator/spliterator som få i teamet vill underhålla

**Stream Gatherers** fyller den luckan. De ger Stream API ett sätt att definiera egna mellanliggande operationer, alltså transformationer som ligger mitt i en pipeline och kan skicka noll, ett eller flera resultat vidare.

I OrderFlow använder vi gatherers för att analysera en ström av orderhändelser. Vi vill kunna se händelser i fasta grupper, glidande fönster och korta sekvenser utan att tappa läsbarheten i pipeline-koden.

## Lärandemål

Efter kapitlet ska läsaren kunna:

- förklara varför gatherers kompletterar `map`, `filter`, `flatMap` och `collect`
- beskriva skillnaden mellan en mellanliggande operation och en terminal operation
- använda `Stream.gather(...)` med färdiga gatherers från `java.util.stream.Gatherers`
- välja mellan `windowFixed(...)` och `windowSliding(...)`
- resonera om stateful stream-transformationer utan att gömma affärslogik i svårlästa pipelines
- avgöra när en gatherer är mer lämplig än en loop, en `Collector` eller `flatMap`

## Innan vi börjar

Från tidigare kapitel tar vi med oss två arbetssätt:

- finala features kan utvärderas för produktionskod utan preview-flaggor
- ny syntax eller nya API:er ska införas där de förenklar designen, inte bara för att de finns

Stream Gatherers introducerades som preview i JDK 22 och 23, men finaliserades i JDK 24. Därför är de tillgängliga i Java 25 utan `--enable-preview`.

Det här kapitlet introducerar tre huvudbegrepp:

- **gatherer**: ett objekt som beskriver hur element i en stream ska omformas och skickas vidare
- **fönster**: en grupp element ur en stream, till exempel tre orderhändelser åt gången
- **stateful intermediate operation**: en mellanliggande stream-operation som behöver minnas tidigare element för att kunna producera nästa resultat

## Huvudförklaring

### Streams före gatherers

Anta att OrderFlow har en sekvens av orderhändelser:

```java
record OrderEvent(String orderId, String type, int amount) {}
```

En vanlig stream-pipeline passar bra för enkel filtrering:

```java
var expensiveOrders = events.stream()
        .filter(event -> event.amount() > 10_000)
        .map(OrderEvent::orderId)
        .toList();
```

Här är varje element oberoende. Ett event blir antingen bortfiltrerat eller mappat till ett order-ID.

Men tänk att vi vill analysera händelser tre och tre:

```text
[A, B, C], [D, E, F], [G, H, I]
```

Eller glidande:

```text
[A, B, C], [B, C, D], [C, D, E]
```

Det går att lösa med index, loopar eller specialkod. Men då lämnar vi ofta stream-modellen precis när dataflödet börjar bli intressant.

### Mellanliggande operation kontra terminal operation

En stream-pipeline består av två typer av operationer:

- **mellanliggande operationer**, till exempel `filter`, `map`, `flatMap`, `sorted`
- **terminala operationer**, till exempel `toList`, `forEach`, `reduce`, `collect`

En `Collector` används i en terminal operation:

```java
var result = events.stream()
        .filter(event -> event.amount() > 0)
        .collect(...);
```

När `collect(...)` körs är pipelinen slut.

En gatherer används i stället mitt i pipelinen:

```java
var result = events.stream()
        .gather(Gatherers.windowFixed(3))
        .map(window -> summarize(window))
        .toList();
```

Det är den viktiga designskillnaden. En gatherer producerar en ny stream som kan fortsätta bearbetas.

### Fasta fönster med `windowFixed`

`Gatherers.windowFixed(3)` delar upp en stream i grupper om tre element. Varje grupp skickas vidare som ett element i den nya streamen.

```java
var batches = events.stream()
        .gather(Gatherers.windowFixed(3))
        .toList();
```

Om input innehåller åtta element blir resultatet tre fönster:

```text
[1, 2, 3]
[4, 5, 6]
[7, 8]
```

Det sista fönstret kan alltså vara mindre än den angivna storleken.

I OrderFlow passar detta för exempelvis batchad validering, rapportering eller analys där vi vill titta på ett antal händelser åt gången.

```java
var batchSummaries = events.stream()
        .gather(Gatherers.windowFixed(3))
        .map(OrderAnalytics::summarizeWindow)
        .toList();
```

### Glidande fönster med `windowSliding`

`Gatherers.windowSliding(3)` skapar överlappande fönster. Varje nytt fönster flyttar ett steg framåt.

För input med fem element blir resultatet:

```text
[1, 2, 3]
[2, 3, 4]
[3, 4, 5]
```

I OrderFlow kan detta användas när ordningen mellan händelser spelar roll. Vi kan exempelvis leta efter mönster där flera höga orderbelopp kommer nära varandra i tid.

```java
var suspiciousWindows = events.stream()
        .gather(Gatherers.windowSliding(3))
        .filter(OrderAnalytics::containsMultipleLargeOrders)
        .toList();
```

Här är det inte varje enskild order som är intressant, utan kombinationen av närliggande orderhändelser.

### Varför inte bara `flatMap`?

`flatMap` är bra när ett element ska bli noll, ett eller flera element:

```java
orders.stream()
        .flatMap(order -> order.lines().stream())
        .toList();
```

Men `flatMap` utgår fortfarande från ett element i taget. Den är inte designad för att enkelt komma ihåg tidigare element, vänta på framtida element eller skapa fönster över en hel sekvens.

Gatherers är bättre när transformationen behöver ett litet, kontrollerat tillstånd.

### Varför inte bara en loop?

En loop kan vara det bästa valet.

Det här är ofta tydligt:

```java
List<List<OrderEvent>> windows = new ArrayList<>();

for (int i = 0; i < events.size(); i += 3) {
    windows.add(events.subList(i, Math.min(i + 3, events.size())));
}
```

Problemet är inte att loopar är dåliga. Problemet är att samma mönster upprepas i många varianter: fasta fönster, glidande fönster, löpande summering, gränsbaserad gruppering och specialiserade batchar.

När mönstret är en återanvändbar stream-transformation kan en gatherer ge bättre namngivning och bättre komposition.

En bra tumregel:

- använd loop när kontrollflödet är viktigare än pipeline-stilen
- använd `map`/`filter` när varje element kan behandlas oberoende
- använd `flatMap` när ett element expanderas till flera element
- använd `collect` när pipelinen ska avslutas
- använd gatherer när pipelinen behöver en mellanliggande, eventuellt stateful transformation

### Parallellitet och försiktighet

Gatherers kan användas i parallella streams, men alla gatherers lämpar sig inte lika bra för parallell körning. En gatherer som behöver ordning eller sekventiellt tillstånd kan bli svår att parallellisera korrekt.

För OrderFlow antar vi därför följande regel:

> Börja sekventiellt. Välj gatherers för uttrycksfullhet och läsbarhet först. Optimera parallellitet först när mätningar visar att det behövs.

Detta följer bokens övergripande princip: modern Java ska göra designen tydligare innan den gör den smartare.

## Exempel: OrderFlow analyserar orderhändelser

Anta att vi har följande händelser:

```java
var events = List.of(
        new OrderEvent("A-100", "CREATED", 1200),
        new OrderEvent("A-101", "CREATED", 25000),
        new OrderEvent("A-102", "CREATED", 18000),
        new OrderEvent("A-103", "CREATED", 900),
        new OrderEvent("A-104", "CREATED", 42000),
        new OrderEvent("A-105", "CREATED", 300)
);
```

Vi kan skapa fasta batchar:

```java
var fixedWindows = events.stream()
        .gather(Gatherers.windowFixed(3))
        .map(OrderAnalytics::summarizeWindow)
        .toList();
```

Och glidande fönster:

```java
var slidingWindows = events.stream()
        .gather(Gatherers.windowSliding(3))
        .filter(OrderAnalytics::containsMultipleLargeOrders)
        .map(OrderAnalytics::summarizeWindow)
        .toList();
```

Den första pipelinen svarar på frågan:

> Hur ser händelserna ut i batchar om tre?

Den andra svarar på frågan:

> Finns det perioder där flera stora order inträffar nära varandra?

Det viktiga är att båda frågorna formuleras som dataflöden. Ingen hjälpklass behöver exponera indexhantering, temporära listor eller specialregler för sista batchen.

## Vanliga misstag

- Misstag:
  - Att använda gatherers för alla stream-problem.
  - Varför det händer:
    - Featuren är ny och lockande.
  - Hur man undviker det:
    - Använd först vanliga stream-operationer. Välj gatherer när transformationen faktiskt behöver ett fönster, tillstånd eller en återanvändbar mellanoperation.

- Misstag:
  - Att gömma komplex affärslogik i en lång pipeline.
  - Varför det händer:
    - Pipeline-kod ser ofta kompakt ut även när den gör mycket.
  - Hur man undviker det:
    - Namnge steg med metoder som `summarizeWindow` och `containsMultipleLargeOrders`.

- Misstag:
  - Att anta att gatherers automatiskt gör streams snabbare.
  - Varför det händer:
    - Nya API:er misstolkas som optimeringar.
  - Hur man undviker det:
    - Behandla gatherers som ett uttrycksfullhetsverktyg. Mät prestanda separat.

- Misstag:
  - Att använda parallella streams utan att förstå gathererns tillstånd.
  - Varför det händer:
    - `parallelStream()` är lätt att lägga till.
  - Hur man undviker det:
    - Börja sekventiellt och dokumentera om en gatherer är ordningsberoende.

## Övningar

### Övning 1: Fasta fönster

Utgå från en lista med minst tio `OrderEvent`. Använd `Gatherers.windowFixed(4)` för att skapa batchar.

För varje batch, skriv ut:

- antal händelser
- totalt orderbelopp
- högsta orderbelopp

Diskutera vad som händer med sista batchen om antalet händelser inte är delbart med fyra.

### Övning 2: Glidande riskfönster

Använd `Gatherers.windowSliding(3)` och hitta alla fönster där minst två order har belopp över 10 000.

Skriv ut order-ID:n för varje matchande fönster.

### Fördjupning

Välj en befintlig loop i en Java 21-kodbas som skapar grupper, batchar eller sekvensmönster.

Besvara:

1. Blir koden tydligare med en gatherer?
2. Försvinner viktig kontrollflödesinformation?
3. Kan transformationen namnges på ett affärsnära sätt?
4. Behöver teamet särskilda kodstandarder för gatherer-användning?

## Snabb sammanfattning

- Stream Gatherers gör det möjligt att skapa egna mellanliggande stream-operationer.
- I Java 25 är gatherers tillgängliga utan preview-flaggor eftersom API:et finaliserades i JDK 24.
- `windowFixed(...)` skapar icke-överlappande fönster.
- `windowSliding(...)` skapar överlappande fönster.
- Gatherers ersätter inte loopar, `flatMap` eller collectors; de kompletterar dem.
- För produktionskod bör gatherers användas när de gör dataflödet mer begripligt, inte bara kortare.

## Quiz/reflektionsfrågor

1. Varför är en gatherer en mellanliggande operation och inte en terminal operation?
2. När är `windowSliding(3)` mer lämplig än `windowFixed(3)`?
3. Varför är `flatMap` inte alltid ett bra alternativ till en gatherer?
4. Vilka risker finns med stateful stream-transformationer?
5. Hur skulle du formulera en kodstandard för gatherers i ett erfaret Java-team?

## Nästa steg

Nästa kapitel flyttar fokus från språk- och biblioteksnyheter till runtime- och JVM-nära förbättringar. Vi tittar på AOT-relaterade möjligheter, Compact Object Headers och GC-nyheter, och skiljer mellan features som påverkar kodens design och features som främst påverkar drift, uppstart och minnesprofil.
