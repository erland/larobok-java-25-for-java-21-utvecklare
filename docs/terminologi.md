# Terminologi

| Term | Definition | Första planerade kapitel |
|---|---|---|
| Java 25 | Den Java/JDK-version som boken behandlar som målplattform. | 1 |
| Java 21 | Bokens antagna startpunkt och tidigare baslinje. | 1 |
| JEP | JDK Enhancement Proposal; formellt förslag och spårning för JDK-förändringar. | 1 |
| Final feature | Funktion som ingår utan preview/incubator/experimental-status. | 1 |
| Preview feature | Funktion som kan testas men inte bör behandlas som helt stabil API/design. | 1 |
| Incubating API | API som publiceras för experimentell återkoppling och kan ändras. | 1 |
| Scoped Value | Mekanism för att dela oföränderlig kontext inom ett avgränsat körningsscope. | 7 |
| Structured Concurrency | Modell där relaterade samtidiga uppgifter hanteras som en sammanhängande arbetsenhet. | 8 |
| AOT cache | Ahead-of-Time-relaterad cache som kan förbättra uppstart i vissa scenarier. | 10 |
| JFR | Java Flight Recorder, JVM-teknik för lågkostnadsprofilering och diagnostik. | 11 |

| Releasebaslinje | Den Java-version som kodbas, byggkedja, runtime och driftmiljö utgår från. | 1 |
| Feature-status | Klassning av en feature som final, preview, incubating eller experimental. | 1 |
| Migreringsrisk | Sannolikheten att en förändring kräver kodändring, byggändring, teständring, driftsbeslut eller arkitekturval. | 1 |
| Kompatibilitetsmigration | Fas där systemet byggs, testas och körs på ny JDK utan större kodadoption. | 1 |
| Adoptionsmigration | Fas där teamet väljer vilka nya features som ska införas efter stabil kompatibilitet. | 1 |
| Toolchain | Den JDK och de verktyg som används för att kompilera, testa, paketera och analysera koden. | 2 |
| Runtime-baslinje | Den JDK-version och de JVM-flaggor som faktiskt används när applikationen körs. | 2 |
| Kompatibilitetstest | Testkörning som syftar till att visa att befintligt beteende fungerar på en ny plattform. | 2 |

| Compact source file | Källkodsfil där toppnivåfält och toppnivåmetoder behandlas som medlemmar i en implicit klass. | 3 |
| Instance main method | Körbar `main`-metod som kan vara en instansmetod och inte behöver klassisk `public static void main(String[] args)`. | 3 |
| `java.lang.IO` | Klass i `java.lang` för enkel radbaserad konsol-I/O i små program och exempel. | 3 |


| Module import declaration | Importdeklaration på formen `import module M;` som importerar publika toppnivåtyper från paket som en modul exporterar. | 4 |
| Exported package | Paket som en modul gör tillgängligt för annan kod och som kan omfattas av modulimport. | 4 |
| Ambiguous simple name | Enkelt typnamn som kan syfta på flera importerade typer och därför blir tvetydigt vid kompilering. | 4 |


| Constructor prologue | Kod i en konstruktor som står före ett explicit `super(...)`- eller `this(...)`-anrop. | 5 |
| Constructor epilogue | Kod i en konstruktor som står efter ett explicit `super(...)`- eller `this(...)`-anrop. | 5 |
| Early construction context | Begränsat sammanhang före konstruktoranropet där objektet under konstruktion inte får användas fritt. | 5 |
| Säker initiering | Initieringsmönster som minskar risken att halvinitierat objektläge observeras. | 5 |



## Kapitel 6

| Term | Definition | Kommentar |
|---|---|---|
| Primitive pattern | Pattern som används för att resonera om primitiva typer och primitiva värden. | I JDK 25 är detta preview och ska markeras tydligt. |
| Numerisk klassificering | Indelning av numeriska värden i namngivna kategorier, exempelvis `invalid`, `normal` eller `critical`. | Används i OrderFlow för ordervärden och signalsystem. |
| Preview feature | Feature som är tillgänglig för experiment och feedback men ännu inte slutlig. | Kräver normalt `--enable-preview` vid kompilering och körning. |
| Guarded case | `case` i `switch` med ett extra `when`-villkor. | Används för intervall och specialregler. |

| Dynamiskt scope | Körningens avgränsade anropskedja där ett scoped value är bundet. | 7 |
| Rebinding | Att skapa en ny bindning för samma scoped value i ett inre scope. | 7 |
| RequestContext | Immutable contextobjekt för exempelvis korrelations-ID och tenant i OrderFlow. | 7 |
| ThreadLocal | Java-API för trådassocierad data; jämförs med Scoped Values i kapitel 7. | 7 |


## Kapitel 8

| Term | Definition | Kommentar |
|---|---|---|
| Virtual thread | Lättviktig `Thread` som hanteras av JDK:n och lämpar sig väl för många blockerande I/O-uppgifter. | Final sedan Java 21. |
| StructuredTaskScope | API för att gruppera relaterade subtasks i ett blockstrukturerat scope. | Preview i Java 25. |
| Task scope | Avgränsat block där relaterade samtidiga uppgifter startas, väntas in och avslutas. | Används för tydlig livstid. |
| Subtask | Uppgift som startas med `fork(...)` inom ett `StructuredTaskScope`. | Resultat läses efter `join()`. |
| Join | Punkt där scope-ägaren väntar in subtasks. | Designpunkt för att samla parallellt arbete. |
| Cancellation | Avbrott av uppgifter som inte längre behövs eller hör till en misslyckad helhet. | Viktigt för att undvika kvarhängande arbete. |


| Gatherer | Objekt som beskriver hur element i en stream ska omformas och skickas vidare som en mellanliggande operation. | 9 |
| Stream.gather | Stream-metod som applicerar en gatherer och returnerar en ny stream. | 9 |
| Fönster | Grupp av intilliggande element i en stream, exempelvis från `windowFixed` eller `windowSliding`. | 9 |
| Stateful intermediate operation | Mellanliggande stream-operation som behöver minnas tidigare element för att producera nästa resultat. | 9 |
| windowFixed | Färdig gatherer som delar upp element i icke-överlappande grupper. | 9 |
| windowSliding | Färdig gatherer som skapar överlappande grupper där fönstret flyttas ett steg i taget. | 9 |


## Kapitel 10

| Term | Definition | Kommentar |
|---|---|---|
| AOT cache | Cache som skapas före en produktionskörning och kan innehålla information som gör startup och warmup effektivare. | HotSpot AOT cache, inte native image. |
| Training run | Representativ körning som samlar underlag för en AOT cache. | Bör likna produktionens vanliga start- och kodvägar. |
| AOT method profiling | Användning av profiler från en tidigare körning så att JIT kan fatta bättre beslut tidigare. | Java 25-feature. |
| Object header | JVM-metadata per objekt. | Relevant för minnesfotavtryck i objektintensiva system. |
| Compact Object Headers | Kompaktare object-header-layout i Java 25. | Produktfeature, men ska mätas före adoption. |
| Garbage collector mode | Driftläge för en garbage collector. | Exempel: generational eller non-generational. |
| Mätmatris | Tabell som separerar körning, JDK, JVM-flaggor, hypotes och metrik. | Hjälper teamet att undvika blandade experiment. |
| Warmup | Perioden innan JVM:en har optimerat viktiga kodvägar för stabil prestanda. | Skiljs från ren starttid. |


## Kapitel 11

| Term | Definition | Kommentar |
|---|---|---|
| JFR | Java Flight Recorder, JVM-teknik för lågkostnadsprofilering och diagnostik. | Används för både migreringsjämförelse och riktad felsökning. |
| Execution-time sampling | Sampling baserad på vad trådar gör vid återkommande tidpunkter. | Bra allmän profil, men inte samma sak som CPU-tid. |
| CPU-time profiling | Profilering som fokuserar på CPU-tid snarare än väggklocktid. | Experimental och Linux-inriktad i Java 25. |
| `jdk.CPUTimeSample` | JFR-event för CPU-time profiling. | Inte aktiverat som standard. |
| Cooperative sampling | Förbättrad JFR-sampling som syftar till stabilare stack walking. | Implementationsteknik, inte nytt applikations-API. |
| Method Timing | JFR-funktion som mäter invokationer och ungefärlig exekveringstid för filtrerade metoder. | Används när misstänkt metod redan är identifierad. |
| Method Trace | JFR-funktion som registrerar stack traces för filtrerade metoder. | Används smalt för att förstå anropskontext. |
| JFR-filter | Mönster som väljer metod, klass eller annotation för riktad mätning. | För breda filter kan ge brus och overhead. |


## Kapitel 12

| Term | Definition | Kommentar |
|---|---|---|
| KDF | Key Derivation Function; algoritm/API för att härleda nyckelmaterial från annat hemligt material och kontextdata. | I Java 25 är KDF API finalt. |
| HKDF | HMAC-based Extract-and-Expand Key Derivation Function. | Ett vanligt KDF-exempel som nämns i Java 25-sammanhang. |
| PEM encoding | Textformat för kryptografiska objekt som nycklar, certifikat och CRL:er. | Java 25 har preview-API för detta. |
| DER | Binärt kodningsformat som ofta ligger under PEM-representationer av kryptografiska objekt. | Relevant vid nyckel- och certifikathantering. |
| Integrity by default | Plattformspolicy där integritetsbrytande operationer kräver uttryckligt godkännande. | Används för att förstå JNI, FFM och `Unsafe`. |
| Native access | Åtkomst till native code via exempelvis JNI eller FFM. | Bör aktiveras och dokumenteras selektivt. |
| `sun.misc.Unsafe` | Intern/unsupported API-yta för lågnivåoperationer, bland annat minnesåtkomst. | Ska ersättas med standard-API:er där det är möjligt. |
| Beslutsmatris | Tabell som dokumenterar område, status, risk, ägare och produktionsrekommendation. | Används i sista kapitlet som sammanfattande styrverktyg. |
