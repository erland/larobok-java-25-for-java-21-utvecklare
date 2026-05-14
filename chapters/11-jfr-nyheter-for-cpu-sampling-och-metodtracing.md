# Kapitel 11: JFR-nyheter för CPU, sampling och metodtracing

## Varför detta kapitel finns

När ett Java 21-system flyttas till Java 25 är det lätt att fokusera på språkfeatures, byggkedja och JVM-flaggor. Men en migrering är också ett bra tillfälle att förbättra hur teamet ser vad systemet faktiskt gör i drift. Java Flight Recorder, JFR, är ett av de viktigaste verktygen för den typen av arbete eftersom det finns nära JVM:en och kan samla diagnostik utan att applikationen behöver byggas om.

Det här kapitlet fokuserar på Java 25-nyheter som gör JFR mer användbart när teamet behöver svara på frågor som:

- Vilka metoder använder mest CPU-tid?
- Är vår sampling stabil nog för att lita på?
- Kan vi mäta en specifik metod utan att lägga in manuell tidtagning i produktionskod?
- När bör vi använda JFR, och när behövs fortfarande andra profileringsverktyg?

Kapitlet bygger vidare på Kapitel 10, där vi skilde mellan hypotes, mätmetod och produktionsbeslut. Här använder vi samma disciplin, men med fokus på observability.

## Lärandemål

Efter kapitlet ska läsaren kunna:

- förklara skillnaden mellan execution-time sampling och CPU-time profiling,
- beskriva vad cooperative sampling förbättrar i JFR:s stackinsamling,
- använda JFR Method Timing och Method Trace för att undersöka utvalda metoder,
- skapa en enkel JFR-baserad analysloop för en misstänkt långsam kodväg,
- avgöra när en JFR-mätning är tillräcklig och när fler verktyg behövs.

## Innan vi börjar

Vi repeterar tre saker från tidigare kapitel:

1. **Feature-status spelar roll.** I Java 25 är vissa JFR-förbättringar finala, medan CPU-time profiling uttryckligen är experimental.
2. **Mätning före slutsats.** Kapitel 10 införde mätmatrisen. Vi fortsätter använda den för att undvika gissningar.
3. **OrderFlow är vårt exempel.** Vi använder en ordervalidering som ibland blir långsam och behöver undersökas utan att vi först ändrar produktionskoden.

## Huvudförklaring

### JFR som migreringsverktyg

JFR är inte bara ett verktyg för akuta incidenter. Vid en Java-migrering kan det användas i minst tre lägen:

| Läge | Fråga | Typiskt resultat |
|---|---|---|
| Baslinjemätning | Hur beter sig systemet på Java 21? | Referensprofil före migrering |
| Jämförande mätning | Vad ändras på Java 25? | Skillnader i CPU, allokering, trådar, låsning och latency |
| Diagnostisk mätning | Varför uppstår en specifik avvikelse? | Riktad analys av metod, stack eller subsystem |

För en erfaren utvecklare är den viktiga poängen att JFR-data inte automatiskt är en slutsats. Den är underlag. Du behöver fortfarande formulera en hypotes, välja rätt event och kontrollera om mätningen påverkar beteendet du försöker observera.

### Execution-time sampling kontra CPU-time profiling

Traditionell sampling kan beskrivas som att JFR regelbundet tittar på vad trådarna gör. Det ger ofta bra profiler, men det är inte exakt samma sak som att mäta förbrukad CPU-tid.

Skillnaden är särskilt viktig i serversystem:

- En metod som väntar på nätverk kan ta lång väggklocktid men lite CPU.
- En metod som komprimerar, sorterar, parsar eller krypterar kan ta mycket CPU även om den inte sticker ut lika tydligt i total latency.
- Ett system kan vara latency-problematiskt av I/O-skäl eller CPU-problematiskt av beräkningsskäl. Optimeringen blir olika.

Java 25 introducerar **JFR CPU-Time Profiling** som experimental feature på Linux. Den nya eventtypen `jdk.CPUTimeSample` är avsedd att ge mer exakt CPU-tidsprofilering än vanlig execution sampling. Eftersom funktionen är experimental bör boken behandla den som ett diagnostiskt verktyg för kontrollerade mätningar, inte som något som automatiskt ska slås på i alla produktionsmiljöer.

En enkel start kan se ut så här:

```bash
java \
  -XX:StartFlightRecording=jdk.CPUTimeSample#enabled=true,filename=orderflow-cpu.jfr \
  -cp code \
  OrderValidationJfrTarget
```

Efter körningen kan inspelningen analyseras med JDK:s `jfr`-verktyg eller i JDK Mission Control:

```bash
jfr view cpu-time-hot-methods orderflow-cpu.jfr
```

På plattformar där CPU-time profiling inte stöds, eller där den inte är aktiverad, ska teamet falla tillbaka till vanliga JFR-profiler, OS-mätning och eventuellt kompletterande profileringsverktyg.

### Cooperative sampling

**Cooperative sampling** är en JFR-förbättring i Java 25 som handlar om hur JVM:en samlar stackar vid sampling. Målet är att förbättra stabiliteten när JFR samplar Java-trådars stackar asynkront. Förenklat innebär det att stack walking görs på säkrare punkter, samtidigt som JVM:en försöker minimera klassisk safepoint bias.

Det här är inte en ny API-yta som applikationskoden behöver anropa. Det är en förbättring i hur JFR fungerar under huven.

Den praktiska konsekvensen för ett utvecklingsteam är:

- JFR-profiler kan bli mer tillförlitliga i vissa situationer.
- Sampling är fortfarande sampling, inte absolut sanning.
- Kortvariga mätningar kan fortfarande vara missvisande.
- Profiler ska jämföras över flera körningar innan stora optimeringsbeslut tas.

En bra tumregel är att se cooperative sampling som en förbättrad mätmekanik, inte som en ursäkt att sluta tänka statistiskt.

### Method Timing

**Method Timing** är en Java 25-nyhet i JFR som gör det möjligt att mäta invokationer och ungefärlig exekveringstid för utvalda metoder. Detta är användbart när du redan har en misstänkt metod och vill slippa lägga in egen tidtagningskod.

Anta att OrderFlow ibland blir långsamt i ordervalideringen. Vi vill undersöka metoden `validateOrder`:

```bash
java \
  '-XX:StartFlightRecording:method-timing=OrderValidationJfrTarget::validateOrder,filename=orderflow-method-timing.jfr' \
  -cp code \
  OrderValidationJfrTarget
```

Efteråt:

```bash
jfr view method-timing orderflow-method-timing.jfr
```

Det här ger en annan typ av signal än CPU-sampling. Method Timing svarar på frågor som:

- Hur ofta körs metoden?
- Hur lång tid tar den i genomsnitt?
- Verkar den misstänkt i relation till sin roll?

Men det säger inte automatiskt varför metoden är långsam. För det behöver du ofta stackar, delmätningar, profiler eller domänförståelse.

### Method Trace

**Method Trace** är nära släkt med Method Timing men fokuserar på spårning. Det kan registrera stack traces för metoder som matchar ett filter. Det är särskilt användbart när frågan är: *Vem anropar detta, och i vilket sammanhang?*

Exempel:

```bash
java \
  '-XX:StartFlightRecording:method-trace=OrderValidationJfrTarget::validateOrder,filename=orderflow-method-trace.jfr' \
  -cp code \
  OrderValidationJfrTarget
```

Och sedan:

```bash
jfr view MethodTrace orderflow-method-trace.jfr
```

eller:

```bash
jfr print --events jdk.MethodTrace --stack-depth 20 orderflow-method-trace.jfr
```

Method Trace ska användas riktat. Att spåra för brett kan ge för mycket data och påverka körningen. En bra strategi är:

1. börja med vanlig JFR-profil,
2. identifiera misstänkta kodvägar,
3. använd Method Timing på ett fåtal metoder,
4. använd Method Trace när anropsvägen är oklar.

### Filterdesign

Method Timing och Method Trace bygger på filter. Ett filter kan peka på en specifik metod, en klass eller i vissa fall en annotation. För produktionsnära felsökning är filterdesignen lika viktig som själva kommandot.

| Filterstrategi | Exempel | När den passar |
|---|---|---|
| En metod | `OrderValidationJfrTarget::validateOrder` | När hypotesen är smal |
| En klass | `OrderValidationJfrTarget` | När flera metoder i samma klass är intressanta |
| En metodtyp | `::<clinit>` | När startup eller statisk initiering misstänks |
| Annotation | `@jakarta.ws.rs.GET` | När ramverkslager ska undersökas utan att namnge varje metod |

I OrderFlow bör teamet normalt börja smalt. Om mätningen visar att `validateOrder` bara är ett symptom kan filtret breddas till hela valideringsklassen eller till API-lagrets annotationsbaserade endpoints.

### En analysloop för OrderFlow

Vi använder följande scenario:

> Efter migrering till Java 25 visar belastningstestet att vissa orderflöden har högre p95-latency än tidigare. CPU-användningen verkar också något högre, men teamet vet inte om problemet ligger i validering, prissättning eller externa anrop.

En JFR-baserad analysloop kan se ut så här:

1. **Skapa baslinje.** Kör samma scenario på Java 21 och Java 25 med samma lastprofil.
2. **Samla bred JFR.** Använd `settings=profile` under en avgränsad period.
3. **Jämför hotspots.** Titta på CPU, allokering, trådar, låsning och I/O.
4. **Rikta mätningen.** Om validering sticker ut: använd Method Timing på valideringsmetoder.
5. **Spåra kontexten.** Om anropsvägen är oklar: använd Method Trace på samma metod.
6. **Bekräfta med ny körning.** Upprepa mätningen efter eventuell ändring.

Det viktiga är att inte hoppa direkt från “metod syns i profilen” till “metoden ska optimeras”. Fråga först om metoden är en verklig orsak, ett symptom eller bara ofta förekommande.

### Mätmatris för JFR

| Körning | JDK | JFR-konfiguration | Hypotes | Metrik | Beslut |
|---|---:|---|---|---|---|
| A | 21 | `settings=profile` | Baslinje före migration | p95, CPU, hotspots | Referens |
| B | 25 | `settings=profile` | Migration ändrar inte CPU-profil nämnvärt | Skillnad mot A | Utred avvikelse |
| C | 25 | `method-timing=...validateOrder` | Validering tar oproportionerligt mycket tid | Invocations, average time | Behåll eller gå vidare |
| D | 25 | `method-trace=...validateOrder` | Fel anropsväg gör valideringen dyr | Stack traces | Refaktorera anropsväg |
| E | 25 Linux | `jdk.CPUTimeSample#enabled=true` | Problemet är CPU-bundet | CPU-hot methods | Optimera CPU-kod |

Mätmatrisen är avsiktligt enkel. Den viktigaste vinsten är att varje körning har en hypotes och ett beslut. Det gör att JFR-filer inte bara samlas i en katalog utan att någon vet vad de ska användas till.

## Exempel

Filen `code/OrderValidationJfrTarget.java` innehåller ett litet program som simulerar ordervalidering. Det är inte tänkt att vara en perfekt benchmark. Det är ett målprogram för JFR-kommandon.

Kompilera:

```bash
javac --release 25 code/OrderValidationJfrTarget.java
```

Kör en vanlig inspelning:

```bash
java \
  -XX:StartFlightRecording=filename=orderflow-profile.jfr,settings=profile,duration=20s \
  -cp code \
  OrderValidationJfrTarget
```

Kör Method Timing:

```bash
java \
  '-XX:StartFlightRecording:method-timing=OrderValidationJfrTarget::validateOrder,filename=orderflow-method-timing.jfr' \
  -cp code \
  OrderValidationJfrTarget
```

Kör Method Trace:

```bash
java \
  '-XX:StartFlightRecording:method-trace=OrderValidationJfrTarget::validateOrder,filename=orderflow-method-trace.jfr' \
  -cp code \
  OrderValidationJfrTarget
```

På Linux kan du dessutom prova CPU-time sampling:

```bash
java \
  -XX:StartFlightRecording=jdk.CPUTimeSample#enabled=true,filename=orderflow-cpu.jfr \
  -cp code \
  OrderValidationJfrTarget
```

När du analyserar resultatet ska du inte bara leta efter “största metoden”. Leta efter relationen mellan anropsfrekvens, genomsnittstid, CPU-kostnad och affärsbetydelse.

## Vanliga misstag

- Misstag: Att behandla en JFR-profil som absolut sanning.
  - Varför det händer: Profilen känns exakt eftersom den innehåller konkreta metodnamn och tider.
  - Hur man undviker det: Upprepa mätningar, jämför flera profiler och kontrollera hypotesen med annan data.

- Misstag: Att slå på för bred Method Trace.
  - Varför det händer: Det är frestande att samla “allt” när problemet är oklart.
  - Hur man undviker det: Börja med bred sampling, smalna sedan av till ett fåtal metoder.

- Misstag: Att blanda migrering och optimering.
  - Varför det händer: Java 25-migreringen gör att teamet samtidigt upptäcker gamla prestandaproblem.
  - Hur man undviker det: Separera “fungerar på Java 25” från “vi optimerar systemet”.

- Misstag: Att tolka CPU-time profiling som plattformsoberoende.
  - Varför det händer: JFR uppfattas ofta som ett generellt JDK-verktyg.
  - Hur man undviker det: Dokumentera att Java 25 CPU-time profiling är experimental och Linux-specifik.

- Misstag: Att mäta en icke-representativ körning.
  - Varför det händer: Lokala testdata är ofta för små och för rena.
  - Hur man undviker det: Skapa lastprofiler som liknar verkliga orderflöden, inklusive fel, edge cases och varierande datamängder.

## Övningar

### Övning 1: Skapa en JFR-baslinje

Kör `OrderValidationJfrTarget` med en vanlig JFR-profil. Spara filen som `orderflow-profile.jfr`.

Besvara:

1. Vilka metoder syns tydligast i profilen?
2. Är de CPU-tunga, ofta anropade eller både och?
3. Vilken hypotes skulle du testa härnäst?

### Övning 2: Jämför Method Timing och Method Trace

Kör först Method Timing och sedan Method Trace för `validateOrder`.

Besvara:

1. Vad svarar Method Timing bättre på än Method Trace?
2. Vad svarar Method Trace bättre på än Method Timing?
3. Vilken av dem skulle du använda först i en produktionsnära incident?

### Övning 3: Bygg en mätmatris

Skapa en egen mätmatris för ett Java 21 till Java 25-system du känner till.

Matrisen ska innehålla:

- minst två baslinjekörningar,
- minst en riktad metodmätning,
- minst en tydlig hypotes,
- ett beslut efter varje körning.

### Fördjupning: Undvik falska optimeringar

Välj en metod som syns i en profil men som du misstänker inte är grundorsaken. Skriv ett kort resonemang:

- Varför syns metoden?
- Vilken annan data behövs?
- Vilken mätning skulle falsifiera optimeringsidén?

## Snabb sammanfattning

- JFR är ett centralt verktyg för att jämföra Java 21- och Java 25-beteende.
- CPU-time profiling i Java 25 ger mer CPU-inriktad profilering på Linux men är experimental.
- Cooperative sampling förbättrar JFR:s sampling under huven, men sampling kräver fortfarande statistisk försiktighet.
- Method Timing mäter utvalda metoders anropsfrekvens och ungefärliga tid.
- Method Trace visar anropskontext för utvalda metoder.
- Riktad JFR-mätning ska följa en hypotes, inte ersätta en hypotes.

## Quiz/reflektionsfrågor

1. Varför kan en metod ha hög latency men låg CPU-kostnad?
2. När är Method Timing bättre än vanlig bred sampling?
3. Varför bör Method Trace användas smalt?
4. Vad betyder det praktiskt att CPU-time profiling är experimental?
5. Hur kan en mätmatris minska risken för felaktiga optimeringsbeslut?

## Nästa steg

Nästa kapitel går vidare från observability till produktionsbeslut kring säkerhet, plattformsnära API:er och migrationsrisker: kryptografi, Unsafe, JNI och vilka förändringar som bör kräva arkitekturbeslut innan de införs.
