# Kapitel 1: Från Java 21 till Java 25

## Varför detta kapitel finns

Java 21 är en stark baslinje. Den gav många team en modern LTS-plattform med virtual threads, record patterns, pattern matching for `switch`, sequenced collections och flera preview-/incubator-spår som pekade framåt.

Java 25 är nästa stora målpunkt för många organisationer. För en erfaren Java 21-utvecklare är den viktigaste frågan därför inte “vad är nytt?” i största allmänhet, utan:

- vilka förändringar påverkar befintliga system?
- vilka features är mogna nog för produktion?
- vilka features är främst intressanta för experiment, utbildning eller framtida design?
- var finns migreringsrisker, byggkedjeproblem och kompatibilitetsfrågor?

Det här kapitlet ger en karta. Senare kapitel går djupare i språk, bibliotek, concurrency, JVM, observability och säkerhet.

## Lärandemål

Efter kapitlet ska läsaren kunna:

- beskriva Java 25 som målplattform i relation till Java 21
- skilja mellan final, preview, incubating och experimental
- göra en första riskklassning av Java 25-nyheter för en befintlig kodbas
- planera en första migreringsinventering utan att börja ändra kod för tidigt
- förklara varför alla “nya features” inte ska behandlas lika i arkitektur- och produktionsbeslut

## Innan vi börjar

Boken antar att du redan är bekväm med Java 21 i praktiken. Du behöver inte kunna varje JEP i detalj, men du bör känna igen moderna Java-begrepp som records, pattern matching, virtual threads, sealed classes och den sexmånadersbaserade releasekadensen.

Vi använder tre huvudbegrepp i kapitlet:

1. **Releasebaslinje**: den Java-version som kodbas, byggkedja, runtime och driftmiljö utgår från.
2. **Feature-status**: om en funktion är final, preview, incubating eller experimental.
3. **Migreringsrisk**: sannolikheten att en förändring kräver kodändring, byggändring, teständring, driftsbeslut eller arkitekturval.

## Huvudförklaring

### Java 25 är inte “Java 21 plus några syntaxdetaljer”

Det är frestande att se Java 25 som en liten språkuppdatering. För ett riktigt system är det för snävt. Java 25 innehåller förändringar i flera lager:

| Lager | Exempel på områden | Typisk fråga för teamet |
|---|---|---|
| Språk | compact source files, module imports, constructor bodies | Påverkar detta vår kodstil eller bara småverktyg och utbildning? |
| Bibliotek | scoped values, stream gatherers, cryptography APIs | Finns det nya standard-API:er som ersätter egna lösningar? |
| Concurrency | structured concurrency, virtual-thread-relaterade förbättringar | Kan vi göra parallella flöden tydligare och säkrare? |
| JVM/runtime | AOT, object headers, GC-ändringar | Finns mätbara vinster för starttid, minne eller throughput? |
| Observability | JFR-nyheter | Kan vi felsöka produktion bättre med mindre overhead? |
| Integrity/säkerhet | Unsafe, JNI, Security Manager, post-kvant-krypto | Behöver vi ändra riskbild eller tredjepartsberoenden? |
| Verktyg | source-code launch, javadoc-markdown, runtime images | Kan våra lokala verktyg, docs eller CI förenklas? |

För ett team som redan använder Java 21 är målet inte att använda allt. Målet är att förstå vad som är relevant, moget och värt att migrera till.

### Feature-status styr hur du får använda en nyhet

En Java-feature kan ha olika status. Statusen påverkar både teknikval och hur du bör kommunicera beslutet i teamet.

#### Final

En final feature är en del av plattformen utan krav på särskilda preview- eller incubator-flaggor. Den kan fortfarande ha begränsningar, buggar eller prestandaegenskaper att utvärdera, men den är inte markerad som experimentell språk- eller API-design.

Exempel på finala områden i Java 25-spåret är bland annat:

- `ScopedValue`
- `Stream Gatherers`
- `Compact Source Files and Instance Main Methods`
- `Flexible Constructor Bodies`
- `Module Import Declarations`
- flera JFR- och JVM-relaterade förbättringar

Det betyder inte att du ska använda allt överallt. Det betyder att statusen i sig inte är ett preview-hinder.

#### Preview

En preview feature är avsedd för tidig återkoppling. Den kräver explicit aktivering, till exempel med `--enable-preview`, och kan ändras i senare versioner. Preview passar bra för labbar, prototyper, utbildning och begränsade interna experiment. Den är däremot sällan rätt som kärna i en långlivad publik API-design.

Exempel i Java 25-spåret:

- `Structured Concurrency` är fortfarande preview.
- `Primitive Types in Patterns, instanceof, and switch` är preview.
- `Stable Values` är preview.
- `PEM Encodings of Cryptographic Objects` är preview.

I den här boken markerar vi preview tydligt varje gång det påverkar kod, kompilering eller produktionsbeslut.

#### Incubating

Ett incubating API är ett API under inkubering. Det ligger ofta i en särskild modul och signalerar att API:t inte är slutligt. I Java 25 är `Vector API` fortfarande incubating. Det kan vara mycket intressant för vissa prestandakritiska domäner, men det ska behandlas som ett medvetet specialval, inte som allmän standardkod.

#### Experimental

Experimental innebär ännu starkare försiktighet. Ett experimentellt verktyg eller en experimentell JVM-funktion kan vara värdefull för analys, labbar och vissa kontrollerade miljöer, men bör inte okritiskt bli en normal produktionsinställning.

Ett exempel i Java 25-området är `JFR CPU-Time Profiling (Experimental)`.

### Migrering börjar med inventering, inte med refaktorering

En vanlig fälla är att börja använda nya språkfeatures direkt. För en produktionskodbas är det bättre att först skapa en migreringskarta.

En enkel första karta kan ha fyra kolumner:

| Område | Relevans | Risk | Första åtgärd |
|---|---:|---:|---|
| Byggkedja | Hög | Medel | Verifiera JDK, compiler flags, CI-images och testkörning |
| Runtime | Hög | Medel | Kör regressionstester med JDK 25 utan kodändringar |
| Språkfeatures | Medel | Låg–medel | Dokumentera kodstil innan adoption |
| Preview features | Varierar | Hög | Separera labb från produktionskod |
| Observability | Hög | Låg–medel | Testa JFR-profiler i staging |
| Säkerhet/integrity | Hög | Medel–hög | Inventera JNI, Unsafe, Security Manager och beroenden |

Den viktiga poängen är att migrationen har två faser:

1. **Kompatibilitetsmigration**: kan vi bygga, testa och köra med Java 25 utan beteendeförändringar?
2. **Adoptionsmigration**: vilka nya möjligheter vill vi faktiskt använda?

Blanda inte ihop dem. Ett team som blandar faserna får svårare felsökning, större pull requests och oklarare risk.

### En praktisk klassificering

För bokens återkommande exempel, OrderFlow, använder vi följande klassificering.

| Klass | Beskrivning | Exempelbeslut |
|---|---|---|
| A: Baslinjekritiskt | Måste fungera för att köra systemet | JDK-installation, CI, test, container image |
| B: Produktionsrelevant | Kan ge tydligt värde i riktig drift | JFR-förbättringar, Scoped Values, vissa GC/runtime-val |
| C: Kodstilsrelevant | Kan förbättra läsbarhet eller uttryckskraft | Flexible Constructor Bodies, Stream Gatherers |
| D: Verktygs- eller utbildningsrelevant | Nyttigt för demos, scripts, lokala verktyg | Compact Source Files, instance main methods |
| E: Experimentellt eller preview | Kräver extra policy | Structured Concurrency, primitive patterns, Vector API |

Den här klassificeringen är inte absolut. Ett team som bygger numerisk beräkningskod kan sätta Vector API högre. Ett team som skriver många interna CLI-verktyg kan få stort värde av compact source files. Poängen är att klassificeringen ska göras utifrån systemets verkliga behov, inte utifrån nyhetsvärde.

## Exempel: OrderFlow-teamets första inventering

OrderFlow är ett fiktivt ordersystem med tre huvudsakliga delar:

- `order-api`: HTTP/API-lager
- `order-domain`: domänmodell och validering
- `order-worker`: asynkron bearbetning av orderhändelser

Teamet kör i dag Java 21. De använder Maven, containerbaserad drift och en blandning av traditionella trådar och virtual threads för vissa I/O-tunga flöden.

Teamets första fråga är inte:

> Hur använder vi alla nya Java 25-features?

Den bättre frågan är:

> Vad måste vi veta för att våga byta releasebaslinje från Java 21 till Java 25?

De skapar följande initiala inventering:

| Del | Observation | Risk | Beslut |
|---|---|---:|---|
| CI | Byggimages är låsta till JDK 21 | Medel | Skapa separat Java 25-pipeline |
| Tester | En del integrationstester är långsamma | Medel | Kör testsviten oförändrad först |
| Domänmodell | Många konstruktorer validerar indata indirekt | Låg | Utvärdera Flexible Constructor Bodies senare |
| Kontextdata | Korrelations-ID ligger i `ThreadLocal` | Medel | Undersök Scoped Values i separat spike |
| Observability | JFR används bara vid incidenter | Låg | Skapa standardprofil för staging |
| Native/Unsafe | Ett beroende använder låg-nivå-API:er | Hög | Inventera varningar och uppgraderingsväg |

Den här tabellen blir inte en slutlig plan. Den är ett sätt att undvika slumpmässig migration.

### Kodnära exempel: håll preview utanför huvudflödet

Anta att en utvecklare vill experimentera med en preview-feature. Då bör teamet undvika att lägga den direkt i produktionsmodulen.

En möjlig struktur är:

```text
orderflow/
├── order-api/
├── order-domain/
├── order-worker/
└── java25-labs/
    ├── structured-concurrency-lab/
    └── primitive-patterns-lab/
```

Det är inte en Java-regel, utan en arbetsregel. Den gör det tydligt att labbar får använda särskilda compiler-flaggor medan produktionsmodulerna först migreras konservativt.

En preview-kompilering kan till exempel behöva en flagga av den här typen:

```bash
javac --release 25 --enable-preview Example.java
java --enable-preview Example
```

Använd detta som signal: om en feature kräver sådana flaggor behöver teamet en policy för var den får användas.

## Vanliga misstag

- Misstag: Att behandla alla JDK 25-nyheter som lika mogna.
  - Varför det händer: Feature-listor presenteras ofta som en enda lista.
  - Hur man undviker det: Klassificera varje feature efter status och användningsrisk.

- Misstag: Att börja refaktorera innan systemet kör stabilt på ny JDK.
  - Varför det händer: Nya språkfeatures är mer synliga än bygg- och runtimefrågor.
  - Hur man undviker det: Dela upp migrationen i kompatibilitet först, adoption sedan.

- Misstag: Att använda preview i produktionskod utan explicit beslut.
  - Varför det händer: Preview-kod kan kännas färdig när den kompilerar.
  - Hur man undviker det: Kräv synlig markering i byggfiler, modulstruktur och arkitekturbeslut.

- Misstag: Att ignorera “tråkiga” förändringar inom säkerhet, JNI, Unsafe eller borttagna plattformar.
  - Varför det händer: De påverkar ofta beroenden snarare än den egna applikationskoden.
  - Hur man undviker det: Inventera varningar, native-bibliotek, agents och transitive dependencies tidigt.

## Övningar

### Övning 1: Skapa en feature-karta

Utgå från ett system du känner väl. Skapa en tabell med följande kolumner:

| Feature/område | Status | Relevans | Risk | Första åtgärd |
|---|---|---:|---:|---|

Fyll i minst åtta rader. Blanda språkfeatures, bibliotek, JVM/runtime, observability och säkerhet.

Bedöm varje rad med värdena låg, medel eller hög.

### Övning 2: Separera kompatibilitet från adoption

Skriv två separata checklistor:

1. Vad behöver vara sant för att systemet ska kunna köra på Java 25?
2. Vilka Java 25-nyheter är värda att utvärdera efter att systemet kör stabilt?

Jämför listorna. Om samma punkt finns i båda listorna, formulera om den tills det framgår om den handlar om migration eller adoption.

### Fördjupning: Skriv ett arkitekturbeslut

Skriv ett kort ADR-utkast med rubrikerna:

- Kontext
- Beslut
- Konsekvenser
- Features vi avvaktar med
- Features vi utvärderar först

ADR:en ska förklara varför teamet först migrerar bygg och runtime, och därefter tar ställning till nya språk- och biblioteksfeatures.

## Snabb sammanfattning

- Java 25 bör förstås som en ny releasebaslinje, inte bara som en lista med nya syntaxdetaljer.
- Feature-status är central: final, preview, incubating och experimental innebär olika risknivåer.
- En säker migration börjar med bygg, test och runtime innan större kodadoption.
- Preview och incubating bör hanteras med tydliga teamregler.
- För erfarna Java 21-team är det ofta mer värdefullt att skapa en beslutsmodell än att snabbt använda varje ny feature.
- OrderFlow-exemplet kommer att användas genom boken för att visa hur Java 25 påverkar verkliga utvecklingsbeslut.

## Quiz/reflektionsfrågor

1. Varför är det riskabelt att blanda kompatibilitetsmigration och adoptionsmigration?
2. Vad är skillnaden mellan en final feature och en preview feature i praktiskt teamarbete?
3. När kan en incubating feature ändå vara värd att undersöka?
4. Vilka tre områden skulle du inventera först i en Java 21-kodbas inför Java 25?
5. Hur skulle du formulera en teamregel för preview-features?

## Nästa steg

I nästa kapitel går vi från karta till praktik. Vi sätter upp den första migreringsloopen: JDK, byggverktyg, testkörning, CI och en konservativ strategi för att hitta regressionsrisker innan vi börjar använda nya Java 25-features.
