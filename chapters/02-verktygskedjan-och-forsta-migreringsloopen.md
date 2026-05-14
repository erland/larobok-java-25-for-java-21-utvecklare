# Kapitel 2: Verktygskedjan och första migreringsloopen

## Varför detta kapitel finns

En lyckad Java 25-migrering börjar sällan med ny syntax. Den börjar med att teamet kan bygga, testa, paketera och köra samma system på en ny JDK utan att samtidigt ändra applikationens design.

Det här kapitlet beskriver den första praktiska loopen:

1. installera eller peka ut JDK 25
2. låt byggverktyget använda rätt JDK
3. bygg med nuvarande källkodsnivå
4. kör testerna
5. kör applikationen i en miljö som liknar produktion
6. dokumentera regressionsrisker innan ni adopterar nya features

Målet är inte att “modernisera allt”. Målet är att snabbt få en ärlig bild av vad som redan fungerar, vad som är byggkedjeproblem och vad som är verkliga runtime- eller kompatibilitetsproblem.

## Lärandemål

Efter kapitlet ska läsaren kunna:

- skilja mellan JDK för bygg, JDK för test och JDK för runtime
- sätta upp en kontrollerad Java 25-toolchain i ett projekt
- köra en första kompatibilitetsmigration utan feature-adoption
- identifiera typiska felkällor i bygg, test, CI och drift
- skapa en enkel migreringschecklista för ett Java 21-system

## Innan vi börjar

I kapitel 1 skilde vi mellan **kompatibilitetsmigration** och **adoptionsmigration**. Det här kapitlet handlar nästan bara om kompatibilitetsmigration.

Det betyder:

- byt JDK först
- ändra så lite applikationskod som möjligt
- använd inte preview-features i produktionskod i detta steg
- mät och logga avvikelser innan ni optimerar
- separera “det kompilerar inte” från “det beter sig annorlunda”

Vi introducerar tre huvudbegrepp:

1. **Toolchain**: den JDK och de verktyg som används för att kompilera, testa, paketera och analysera koden.
2. **Runtime-baslinje**: den JDK-version och de JVM-flaggor som faktiskt används när applikationen körs.
3. **Kompatibilitetstest**: testkörning som syftar till att visa att befintligt beteende fungerar på en ny plattform.

## Huvudförklaring

### Tre JDK-frågor som ofta blandas ihop

I små projekt är “vilken Java-version använder vi?” ofta en enkel fråga. I ett större system finns minst tre svar.

| Fråga | Exempel | Varför den spelar roll |
|---|---|---|
| Vilken JDK bygger vi med? | CI använder JDK 25 | Påverkar `javac`, annotation processors, testverktyg och plugins. |
| Vilken bytecode-nivå producerar vi? | `--release 21` eller `--release 25` | Avgör vilka språk- och API-nivåer artefakten får använda. |
| Vilken JDK kör vi med? | Produktion kör JDK 25 | Påverkar JVM, GC, TLS, reflection, JNI, JFR och runtime-beteende. |

En försiktig första loop kan därför använda JDK 25 som bygg- och test-JDK, men fortfarande kompilera med `--release 21`. Det testar stora delar av verktygskedjan utan att samtidigt öppna dörren för Java 25-specifik källkod.

Senare, när teamet aktivt vill använda Java 25-features, kan källkodsnivån höjas. Det är ett separat beslut.

### Bygg först med minsta möjliga förändring

Anta att OrderFlow har tre moduler:

```text
order-api
order-domain
order-worker
```

Första migreringsloopen bör inte börja med att skriva om domänmodellen. Den bör börja med något tråkigare men viktigare:

```bash
java -version
./mvnw -version
./mvnw clean verify
```

eller, för Gradle:

```bash
java -version
./gradlew --version
./gradlew clean test
```

Det teamet vill veta är:

- Använder byggverktyget verkligen JDK 25?
- Fungerar befintliga plugins?
- Fungerar annotation processors?
- Fungerar testerna?
- Finns varningar om borttagna, föråldrade eller begränsade mekanismer?
- Finns skillnader mellan lokal maskin och CI?

En vanlig fallgrop är att lokal utveckling använder JDK 25 medan CI fortfarande använder JDK 21, eller tvärtom. Då kan teamet felsöka fel som egentligen bara är miljöskillnader.

### Exempel: Maven-toolchain

I ett Maven-projekt kan teamet uttrycka att bygget ska använda en viss JDK via toolchains. En förenklad `~/.m2/toolchains.xml` kan se ut så här:

```xml
<toolchains>
  <toolchain>
    <type>jdk</type>
    <provides>
      <version>25</version>
      <vendor>any</vendor>
    </provides>
    <configuration>
      <jdkHome>/opt/jdk-25</jdkHome>
    </configuration>
  </toolchain>
</toolchains>
```

I projektets `pom.xml` kan kompilatorn låsas till en release-nivå. I första loopen kan det vara rimligt att behålla Java 21 som käll- och API-nivå:

```xml
<properties>
  <maven.compiler.release>21</maven.compiler.release>
</properties>
```

Det här betyder inte att projektet “är kvar på Java 21” i alla avseenden. Det betyder att koden fortfarande kompileras mot Java 21:s standard-API och språkregler, medan bygget kan köras med en nyare JDK. Det är ett sätt att minska antalet variabler.

När teamet senare bestämmer sig för att använda Java 25 som källnivå kan egenskapen ändras:

```xml
<properties>
  <maven.compiler.release>25</maven.compiler.release>
</properties>
```

Den ändringen hör hemma i adoptionsmigrationen, inte i första kompatibilitetsloopen.

### Exempel: Gradle-toolchain

I Gradle kan en toolchain anges i byggfilen:

```kotlin
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}
```

Första loopen kan fortfarande välja att producera bytecode för Java 21, beroende på projektets mål och plugin-stöd:

```kotlin
tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}
```

Det viktiga är att teamet dokumenterar avsikten. Är målet att:

- bara testa bygget med JDK 25?
- producera artefakter som fortfarande kan köras på Java 21?
- kräva Java 25 även i runtime?
- börja använda Java 25-API:er?

Alla fyra är rimliga i olika situationer, men de ska inte blandas ihop.

### Runtime-baslinjen är mer än `java -version`

När applikationen väl startar med JDK 25 behöver teamet dokumentera mer än versionsnumret. Minst följande bör ingå i runtime-baslinjen:

| Område | Exempel på fråga |
|---|---|
| JDK-distribution | Kör vi samma distribution i utveckling, CI och produktion? |
| Container-image | Bygger vi om basimagen eller byter vi bara lokal JDK? |
| JVM-flaggor | Finns gamla flaggor som inte längre behövs eller ger varningar? |
| GC | Har vi låst GC-val, heap-inställningar eller pausmål? |
| TLS/krypto | Påverkas integrationer av standarder, certifikat eller algoritmer? |
| Observability | Fungerar loggning, metrics, tracing och JFR-profiler? |
| Native-kopplingar | Använder vi JNI, Panama-experiment, agents eller profilerare? |

För ett serverprojekt som OrderFlow kan en enkel kontrollkörning se ut så här:

```bash
java \
  -Xlog:gc*:file=logs/gc-java25.log:time,uptime,level,tags \
  -jar order-worker/target/order-worker.jar
```

Det här är inte en rekommendation att alltid logga GC så i produktion. Det är ett sätt att göra första testkörningen observerbar.

### Kompatibilitetstest före prestandatest

Ett vanligt misstag är att börja med benchmarkresultat. Prestanda är viktigt, men första frågan är enklare:

> Gör systemet fortfarande rätt sak?

En första testtrappa kan vara:

1. kompilering
2. enhetstester
3. integrationstester
4. kontraktstester mot externa system
5. smoke test i staging-liknande miljö
6. kort lasttest för att hitta uppenbara runtime-problem
7. först därefter riktade prestandamätningar

För OrderFlow kan teamet exempelvis börja med:

```bash
./mvnw clean verify
./mvnw -pl order-worker spring-boot:run
curl -s http://localhost:8080/actuator/health
```

Om applikationen inte använder Spring Boot ska motsvarande hälsokontroll bytas mot projektets faktiska start- och kontrollkommando. Poängen är inte ramverket, utan loopen: bygg, starta, observera, verifiera.

### Fel som bör sorteras tidigt

När ett Java 21-system testas med Java 25 är det praktiskt att kategorisera fel. Annars blir migreringsloggen snabbt en blandning av riktiga problem och brus.

| Feltyp | Exempel | Första åtgärd |
|---|---|---|
| Miljöfel | Fel JDK i CI | Lås toolchain och skriv ut version i loggen. |
| Pluginfel | Gammal byggplugin fungerar inte | Uppdatera plugin isolerat och kör om. |
| Testantagande | Test bygger på timing eller ordning | Avgör om testet eller koden är problemet. |
| Reflection/access | Ramverk eller bibliotek använder intern åtkomst | Uppdatera beroende eller dokumentera öppningsflagga som temporär åtgärd. |
| Native/agent | Profilerare, agent eller JNI-kod fallerar | Testa ny version av verktyget och isolera från applikationskoden. |
| Beteendeskillnad | Applikationen ger annan output | Skapa reproducerbart testfall innan kod ändras. |

Målet är att varje fel ska hamna i rätt kö: byggkedja, testmiljö, beroende, runtime-konfiguration eller applikationskod.

## Exempel: OrderFlow gör första loopen

OrderFlow-teamet vill veta om deras Java 21-system är redo att testas med Java 25. De beslutar följande:

- Inga Java 25-språkfeatures i första loopen.
- Maven körs med JDK 25.
- `maven.compiler.release` ligger kvar på `21`.
- CI får en separat pipeline: `java25-compat`.
- Pipelinefelet får inte blockera huvudflödet första veckan.
- Alla fel klassificeras innan någon börjar refaktorera.

De lägger till ett enkelt versionssteg i CI:

```bash
echo "Java:"
java -version

echo "Maven:"
./mvnw -version
```

Sedan kör de:

```bash
./mvnw clean verify
```

När tester faller sorterar de varje fel i en tabell:

| ID | Modul | Typ | Symptom | Nästa åtgärd |
|---|---|---|---|---|
| OF-25-001 | order-worker | Plugin | Build-plugin varnar för gammal API-användning | Uppdatera plugin i separat commit. |
| OF-25-002 | order-domain | Testantagande | Test beror på iterationordning | Gör testet deterministiskt. |
| OF-25-003 | order-worker | Runtime | Agent startar inte med JDK 25 | Testa ny agentversion. |

Efter första loopen har teamet ännu inte använt någon ny Java 25-feature. Det kan kännas otillfredsställande, men det är precis poängen. De har minskat osäkerheten innan de börjar modernisera.

## Vanliga misstag

- Misstag: Att höja källkodsnivån och byta runtime samtidigt.
  - Varför det händer: Teamet vill “göra migrationen på riktigt” direkt.
  - Hur man undviker det: Dela upp arbetet i kompatibilitet först och adoption senare.

- Misstag: Att bara testa lokalt.
  - Varför det händer: Lokala testresultat känns snabba och konkreta.
  - Hur man undviker det: Skapa en separat CI-loop som skriver ut JDK, byggverktyg och OS-/containerinformation.

- Misstag: Att behandla alla varningar som lika viktiga.
  - Varför det händer: En ny JDK kan ge många nya signaler.
  - Hur man undviker det: Klassificera varningar efter om de påverkar bygg, test, runtime, säkerhet eller framtida underhåll.

- Misstag: Att börja använda preview-features i samma branch som kompatibilitetsmigrationen.
  - Varför det händer: Nya språkfeatures är lockande.
  - Hur man undviker det: Lägg preview-experiment i separata labb eller spike-brancher.

- Misstag: Att glömma runtime-flaggor.
  - Varför det händer: Fokus hamnar på `pom.xml`, `build.gradle` och källkod.
  - Hur man undviker det: Dokumentera faktisk startkommandorad, container-image, JVM-flaggor och miljövariabler.

## Övningar

### Övning 1: Inventera din toolchain

Välj ett befintligt Java 21-projekt och fyll i tabellen.

| Fråga | Svar |
|---|---|
| Vilken JDK används lokalt? |  |
| Vilken JDK används i CI? |  |
| Vilken JDK används i produktion? |  |
| Vilket byggverktyg och vilken version används? |  |
| Är källkodsnivån explicit låst? |  |
| Finns annotation processors? |  |
| Finns Java agents, JNI eller profilerare? |  |
| Finns preview- eller incubator-flaggor? |  |

Målet är inte att lösa allt direkt. Målet är att se vilka delar som måste kontrolleras.

### Övning 2: Designa en Java 25-kompatibilitetspipeline

Skissa en separat CI-pipeline med minst fem steg. Den ska:

- skriva ut JDK-version
- skriva ut byggverktygets version
- bygga projektet
- köra tester
- spara testresultat och relevanta loggar

Markera vilka steg som ska vara blockerande och vilka som först bara ska rapportera resultat.

### Fördjupning: Separera `--release 21` och runtime Java 25

Sätt upp ett litet testprojekt där du bygger med JDK 25 men kompilerar med `--release 21`.

Undersök:

- Vad händer om du försöker använda ett Java 25-API?
- Vad händer om du försöker använda en Java 25-språkfeature?
- Hur skiljer sig felet från att köra samma kod med JDK 21?

Skriv ned skillnaden mellan bygg-JDK, källkodsnivå och runtime-JDK med egna ord.

## Snabb sammanfattning

- Första Java 25-loopen bör handla om kompatibilitet, inte modernisering.
- Toolchain, källkodsnivå och runtime-baslinje är tre olika beslut.
- Det är ofta klokt att bygga med JDK 25 men behålla `--release 21` i första loopen.
- CI måste skriva ut vilken JDK och vilket byggverktyg som faktiskt används.
- Fel bör klassificeras innan de åtgärdas.
- Preview- och experimentfeatures hör hemma i separata labb tills teamet aktivt beslutar annat.

## Quiz/reflektionsfrågor

1. Varför kan det vara värdefullt att bygga med JDK 25 men behålla `--release 21`?
2. Vad är skillnaden mellan toolchain och runtime-baslinje?
3. Vilka tre miljöskillnader brukar skapa falska migreringsproblem?
4. Varför bör kompatibilitetstest komma före riktade prestandamätningar?
5. När är det rimligt att börja höja källkodsnivån till Java 25?
6. Vilka delar av ditt nuvarande projekt skulle sannolikt fallera först vid en JDK-migrering?

## Nästa steg

Nu har vi en kontrollerad migreringsloop. Nästa kapitel går in i den första språkrelaterade Java 25-nyheten i planen: **Compact Source Files och Instance Main Methods**. Där byter vi perspektiv från plattformsbyte till kodstil och små program.
