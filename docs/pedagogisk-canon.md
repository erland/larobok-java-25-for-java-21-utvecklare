# Pedagogisk canon

## Terminologi och statusmarkeringar
- Final: feature som ingår utan preview/incubator/experimental-flaggor.
- Preview: feature som kräver uttryckligt val och kan ändras.
- Incubating: API under inkubering, ofta i särskild modul.
- Experimental: experimentell funktion som kräver försiktighet i produktion.

## Återkommande exempelprojekt
- Namn: OrderFlow
- Syfte: Realistiskt men litet ordersystem för migreringsexempel.
- Regler: Exempel ska vara små nog att förstå isolerat men realistiska nog för erfarna utvecklare.
- Kodstil: Modern Java, tydliga tester, inga stora ramverk om de inte behövs.

## Pedagogisk profil
- Språk: Svenska med etablerade engelska Java-termer.
- Svårighetsgrad: Erfaren, ibland avancerad.
- Läsarprofil: Systemutvecklare som kan Java 21.
- Ton: Kollegial, konkret och pragmatisk.
- Repetitionstakt: Kort repetition av Java 21-begrepp när de behövs för att förstå Java 25-nyheter.

## Versions- och faktaval
- Baslinje: Java 21.
- Målnivå: Java 25.
- Externa fakta om JEP-status, release och support ska verifieras mot OpenJDK och relevanta JDK-distributörer.

## Kapitel 1: Canon-tillägg

### Introducerade begrepp
- Releasebaslinje: den Java-version som kodbas, byggkedja, runtime och driftmiljö utgår från.
- Feature-status: klassning som final, preview, incubating eller experimental.
- Migreringsrisk: sannolikheten att en förändring kräver kodändring, byggändring, teständring, driftsbeslut eller arkitekturval.
- Kompatibilitetsmigration: första migrationfasen, där systemet byggs, testas och körs på Java 25 utan större kodadoption.
- Adoptionsmigration: andra fasen, där teamet aktivt väljer vilka Java 25-nyheter som ska användas.

### OrderFlow
- OrderFlow består tills vidare av `order-api`, `order-domain` och `order-worker`.
- Preview-experiment placeras i separata labb, exempelvis `java25-labs/`, och blandas inte in i produktionsmoduler utan explicit beslut.
- Första migreringsprincip: kompatibilitet före adoption.

### Käll- och faktaprincip
- JDK/JEP-status ska verifieras mot OpenJDK.
- Feature-listor får användas som karta, men boken ska alltid särskilja status och produktionsrisk.


## Kapitel 2: Canon-tillägg

### Introducerade begrepp
- Toolchain: den JDK och de verktyg som används för att kompilera, testa, paketera och analysera koden.
- Runtime-baslinje: den JDK-version, containerbild, JVM-flaggor och miljö som faktiskt används vid körning.
- Kompatibilitetstest: testkörning som syftar till att visa att befintligt beteende fungerar på en ny Java-version.

### OrderFlow
- Första Java 25-loopen för OrderFlow är en separat kompatibilitetspipeline.
- OrderFlow behåller `--release 21` i första loopen även när bygg-JDK är 25.
- Fel ska klassificeras som miljö, plugin, testantagande, runtime, beroende eller applikationskod innan refaktorering påbörjas.

### Pedagogisk regel
- Kapitel 2 etablerar ordningen: bygg- och runtime-kompatibilitet före adoption av nya språk- eller API-features.


## Kapitel 3: Canon-tillägg

### Introducerade begrepp
- Compact source file: källkodsfil där toppnivåfält och toppnivåmetoder behandlas som medlemmar i en implicit klass.
- Instance main method: körbar `main`-metod som kan vara en instansmetod och inte behöver den klassiska `public static void main(String[] args)`-formen.
- `java.lang.IO`: enkel klass för radbaserad konsol-I/O i små program och exempel.

### OrderFlow
- Compact source files används i OrderFlow främst för `java25-labs/`, diagnostik, demos och migreringsverktyg.
- De ska inte användas för domänmodell, publika API:er eller långlivade produktionskomponenter utan särskilt beslut.
- När ett verktyg behöver tester, ägarskap, konfiguration eller CI-integration bör det växa till vanlig Java-struktur.

### Pedagogisk regel
- Kapitel 3 behandlar featuren som ett praktiskt verktyg för små program, inte som ersättning för normal applikationsarkitektur.
- Implicithet accepteras i små lokala program men ska göras explicit när koden blir långlivad eller central.


## Kapitel 4: Canon-tillägg

### Introducerade begrepp
- Module import declaration: importdeklaration på formen `import module M;` som importerar publika toppnivåtyper från paket som modulen exporterar till aktuell modul.
- Exported package: paket som en modul gör tillgängligt för annan kod.
- Ambiguous simple name: kompileringsproblem där ett enkelt namn, exempelvis `List` eller `Date`, kan syfta på flera importerade typer.

### OrderFlow
- Modulimports används i boken främst för små labb, migreringsverktyg och pedagogiska exempel.
- Produktionskod i OrderFlow rekommenderas fortsatt använda explicita imports om inte teamet fattar ett dokumenterat beslut.
- `java25-labs/` får använda `import module java.base;` när filen är liten och syftet är explorativt.

### Importpolicy
- Bred import är acceptabel när den förbättrar pedagogisk tydlighet eller minskar friktion i små verktyg.
- Långlivad produktionskod prioriterar lokal läsbarhet och explicita beroenden.
- Namnkonflikter ska lösas med explicit typimport, package import eller fullt kvalificerat namn.

## Kapitel 5: Canon-tillägg

### Introducerade begrepp
- Constructor prologue: kod i en konstruktor som står före ett explicit `super(...)`- eller `this(...)`-anrop.
- Constructor epilogue: kod i en konstruktor som står efter ett explicit konstruktoranrop.
- Early construction context: begränsat sammanhang där objektet under konstruktion inte får användas fritt.
- Säker initiering: initiering som minskar risken att halvinitierat objektläge observeras.

### OrderFlow
- Flexible Constructor Bodies används främst för fail-fast-validering, enkla normaliseringar och säkrare initiering i befintliga arvshierarkier.
- Prologen i OrderFlow ska vara kort och får inte innehålla I/O, databasåtkomst, tjänsteanrop, publicering av domänevent eller komplicerad affärslogik.
- Om skapandeprocessen kräver beroenden eller flera domänsteg ska fabriksmetod, builder eller domänservice övervägas i stället.
- Arvskod där superklasskonstruktorer anropar överlagringsbara metoder ska fortfarande betraktas som designrisk även om Java 25 minskar vissa initieringsproblem.

### Pedagogisk regel
- Kapitel 5 behandlar featuren som final i Java 25 men betonar kontrollerad adoption.
- Skillnaden mellan tekniskt tillåtet och designmässigt lämpligt ska framgå i varje exempel.



## Kapitel 6: Canon-tillägg

### Introducerade begrepp
- Primitive pattern: pattern som matchar eller binder ett värde i relation till en primitiv typ.
- Numerisk klassificering: indelning av numeriska värden i namngivna kategorier.
- Preview feature: feature som finns i JDK:n för test och feedback men ännu inte är slutligt fastlåst.
- Guarded case: `case` med `when`-villkor i ett `switch`-uttryck.

### OrderFlow
- Primitive patterns behandlas som labb- och utbildningsmaterial, inte som standard i produktionskod.
- Preview-kod får ligga i `java25-labs/`, experiment och interna workshops.
- Produktionsanvändning kräver uttryckligt arkitekturbeslut, CI-markering och plan för omprövning vid nästa JDK-uppgradering.
- Numerisk klassificering ska testas med gränsvärden, negativa tal, maxvärden och relevanta externa dataformat.

### Pedagogisk regel
- Kapitel 6 ska konsekvent skilja mellan språkets möjlighet och teamets adoptionsbeslut.
- Primitive patterns får inte framställas som ersättning för domäntyper som `Money`, `Quantity` eller `RiskScore`.


## Kapitel 7: Canon-tillägg

### Introducerade begrepp
- Scoped value: en nyckel som kan bindas till ett värde under ett avgränsat dynamiskt scope.
- Dynamiskt scope: den körning som startar i `run(...)` eller `call(...)` och omfattar direkta och indirekta anrop därifrån.
- Rebinding: ny bindning av samma scoped value i ett inre scope; yttre bindning återställs efteråt.
- Immutable request-context: ett contextobjekt, ofta ett record, som inte ändras efter skapande.

### Beslut och avgränsningar
- Scoped Values behandlas som final i Java 25.
- `ThreadLocal` avfärdas inte generellt; kapitlet positionerar Scoped Values som bättre för envägsdelning av immutable context med tydlig livstid.
- OrderFlow använder `RequestContext` med `correlationId` och `tenant` som återkommande exempel.
- Structured Concurrency introduceras endast som framåtblick; djupbehandling sker i kapitel 8.


## Kapitel 8: Canon-tillägg

### Introducerade begrepp
- Task scope: ett avgränsat block där relaterade samtidiga uppgifter startas, väntas in och avslutas.
- Cancellation: avbrott av uppgifter som inte längre behövs eller som hör till en misslyckad helhet.
- Join: punkten där ägartråden väntar in subtasks innan resultat behandlas.
- Subtask: en uppgift som startas inom ett `StructuredTaskScope`.

### OrderFlow
- Structured Concurrency används i OrderFlow som labb- och designexempel eftersom API:et är preview i Java 25.
- Parallella serviceanrop ska hållas nära applikationsservice-lagret och inte spridas in i domänmodellen.
- Virtual threads betraktas som final Java 21-teknik, men de ersätter inte behovet av tydlig livstid, cancellation och felpolicy.
- Kombinationen Scoped Values + Structured Concurrency får användas för korrelations-ID i exempel, men implicit kontext ska dokumenteras.

### Pedagogisk regel
- Kapitel 8 ska tydligt skilja mellan virtual threads som final feature och Structured Concurrency som preview API.
- Kod med StructuredTaskScope ska markeras med krav på `--enable-preview`.


## Kapitel 9: Canon-tillägg

### Introducerade begrepp
- Gatherer: objekt som beskriver en mellanliggande stream-transformation.
- Fönster: grupp av intilliggande element i en stream.
- Stateful intermediate operation: mellanliggande operation som behöver begränsat tillstånd för att producera resultat.

### OrderFlow
- OrderFlow använder Stream Gatherers för analys av orderhändelser, inte för kärnlogik som blir svår att felsöka.
- `windowFixed(...)` används för batchorienterad analys.
- `windowSliding(...)` används för sekvens- och riskmönster där närliggande händelser är viktiga.

### Adoptionsregel
- Gatherers får användas i produktionskod när de förenklar dataflödet och kan namnges affärsnära.
- Långa pipelines med anonym komplex logik ska refaktoreras till namngivna metoder.
- Parallella streams med gatherers kräver separat designbeslut och mätning.


## Kapitel 10: Canon-tillägg

### Introducerade begrepp
- AOT cache: cache som skapas före en produktionskörning och hjälper JVM:en att flytta vissa arbeten tidigare.
- Training run: representativ körning som används för att skapa en relevant AOT cache.
- Object header: JVM-metadata per objekt.
- Compact Object Headers: Java 25-produktfeature som gör objektens header-layout mer kompakt.
- Garbage collector mode: driftläge för en garbage collector, exempelvis generational eller non-generational.
- Mätmatris: tabell som separerar hypoteser, JVM-flaggor och metrikvärden.

### OrderFlow
- OrderFlow använder Kapitel 10 för att skapa en mätplan, inte för att direkt besluta om produktionsflaggor.
- Prestandaexperiment ska delas upp i ren Java 25-migrering, AOT-labb, compact object headers-labb och eventuellt separat GC-labb.
- AOT cache behandlas som en versionsbunden artefakt kopplad till applikationsversion, JDK-version och JVM-konfiguration.
- Compact Object Headers utvärderas endast med jämförbar heap-, GC-, CPU- och latencydata.

### Adoptionsregel
- JVM-flaggor får inte införas i produktion i samma steg som grundmigreringen från Java 21 till Java 25.
- Varje prestandafeature ska ha en hypotes, mätmetod, regressionsgräns och rollback-plan.
- Kapitlet ska undvika ogrundade prestandalöften och konsekvent beskriva effekter som något som måste mätas.


## Kapitel 11: Canon-tillägg

### Introducerade begrepp
- CPU-time profiling: JFR-baserad profilering av CPU-tid, experimental och Linux-inriktad i Java 25.
- Cooperative sampling: JFR-förbättring som gör stack sampling stabilare genom säkrare stack walking och minskad safepoint bias.
- Method Timing: JFR-funktion som mäter invokationer och ungefärlig exekveringstid för metoder som matchar ett filter.
- Method Trace: JFR-funktion som registrerar anropskontext och stack traces för metoder som matchar ett filter.
- JFR-filter: urval för metod, klass eller annotation som styr vad JFR ska mäta eller spåra.

### OrderFlow
- OrderFlow använder JFR som migrerings- och diagnostikverktyg, inte som ersättning för lasttest, loggar eller domänanalys.
- JFR-mätningar ska dokumenteras i en mätmatris med JDK-version, OS, JFR-konfiguration, hypotes och beslut.
- Method Timing och Method Trace ska användas smalt och hypotesdrivet.
- CPU-time profiling markeras som experimental och Linux-specifik i Java 25.

### Adoptionsregel
- Profilering ska inte blandas ihop med optimering. En profil identifierar misstänkta områden, men ändringsbeslut kräver hypotes, reproducerbar mätning och regressionskontroll.
- Riktad tracing ska undvikas som standardinställning i produktion och endast aktiveras med tydligt syfte och begränsad omfattning.


## Kapitel 12: Canon-tillägg

### Introducerade begrepp
- KDF: Key Derivation Function; standardiserat sätt att härleda nyckelmaterial från hemligt material och kontextdata.
- PEM encoding: textrepresentation av kryptografiska objekt som nycklar och certifikat.
- Integrity by default: princip där integritetsbrytande operationer, exempelvis native access eller osäkra minnesoperationer, kräver tydligt godkännande.
- Native access: åtkomst till native code via exempelvis JNI eller FFM.
- Beslutsmatris: tabell som samlar status, risk, ägare och rekommenderat produktionsbeslut.

### OrderFlow
- OrderFlow ska skilja mellan runtime-migration och kryptografisk adoption.
- KDF API får införas först efter testvektorer och säkerhetsgranskning.
- PEM API i Java 25 är preview och ska tills vidare behandlas som labb-/adapterfunktion, inte som publik produktionsyta.
- `sun.misc.Unsafe`-varningar i egna koden ska leda till refaktorering. Varningar i beroenden ska ha ägare, plan och slutdatum.
- Native access ska vara explicit dokumenterad i driftkonfigurationen.

### Pedagogisk regel
- Kapitel 12 avslutar första kapitelserien genom att översätta tekniska features till produktionsbeslut.
- Säkerhets- och native-beslut ska inte presenteras som enbart kodfrågor utan som gemensamma beslut mellan utveckling, plattform och säkerhet.
