# Projektstatus

## Bok
Titel: Java 25 för Java 21-utvecklare
Språk: Svenska
Författare: Erland Lindmark
Version: 1.0

## Nuvarande fas
- Kapitelgenerering pågår.
- Kapitel 1–12 är skapade som utkast.
- Den planerade första kapitelserien är komplett; nästa fas är granskning och exportförberedelse.

## Kapitelstatus
| Kapitel | Titel | Status | Kommentar |
|---|---|---|---|
| 0 | Inledning | Utkast | Skapad i första projektversionen. |
| 1 | Från Java 21 till Java 25 | Utkast | Skapat. Introducerar releasebaslinje, feature-status och migreringsrisk. |
| 2 | Verktygskedjan och första migreringsloopen | Utkast | Skapat. Introducerar toolchain, runtime-baslinje och kompatibilitetstest. |
| 3 | Compact Source Files och Instance Main Methods | Utkast | Skapat. Introducerar compact source file, instance main method och `java.lang.IO`. |
| 4 | Module Import Declarations | Utkast | Skapat. Introducerar module import declaration och exported package. |
| 5 | Flexible Constructor Bodies | Utkast | Skapat. Introducerar constructor prologue, constructor epilogue, early construction context och säker initiering. |
| 6 | Primitive Types i patterns, instanceof och switch | Utkast | Skapat. Introducerar primitive pattern, preview feature och numerisk klassificering med `switch`. |
| 7 | Scoped Values i praktiken | Utkast | Skapat. Introducerar scoped value, dynamiskt scope, rebinding och immutable request-context. |
| 8 | Structured Concurrency och virtual threads | Utkast | Skapat. Introducerar task scope, cancellation, join och relationen till virtual threads. |
| 9 | Stream Gatherers och nya sätt att forma dataflöden | Utkast | Skapat. Introducerar gatherer, fönster och stateful intermediate operation. |
| 10 | AOT, Compact Object Headers och GC-nyheter | Utkast | Skapat. Introducerar AOT cache, object header, garbage collector mode och mätmatris. |
| 11 | JFR-nyheter för CPU, sampling och metodtracing | Utkast | Skapat. Introducerar CPU-time profiling, cooperative sampling, Method Timing och Method Trace. |
| 12 | Kryptografi, Unsafe, JNI och produktionsbeslut | Utkast | Skapat. Introducerar KDF, PEM encoding, integrity by default, native access och produktionsbeslutsmatris. |

## Introducerade begrepp
| Begrepp | Kapitel | Kort definition |
|---|---|---|
| Releasebaslinje | 1 | Den Java-version som kodbas, byggkedja, runtime och driftmiljö utgår från. |
| Feature-status | 1 | Klassning av en feature som final, preview, incubating eller experimental. |
| Migreringsrisk | 1 | Sannolikheten att en förändring kräver kodändring, byggändring, teständring, driftsbeslut eller arkitekturval. |
| Toolchain | 2 | Den JDK och de verktyg som används för att kompilera, testa, paketera och analysera koden. |
| Runtime-baslinje | 2 | Den JDK-version och de JVM-flaggor som faktiskt används när applikationen körs. |
| Compact source file | 3 | Källkodsfil där enkel programstruktur kan skrivas utan explicit klassdeklaration. |
| Instance main method | 3 | `main`-metod som kan vara en instansmetod i förenklad programstruktur. |
| Module import declaration | 4 | Importform som importerar exporterade paket från en modul. |
| Constructor prologue | 5 | Del av konstruktorn som får köras före explicit `super(...)` eller `this(...)`. |
| Primitive pattern | 6 | Pattern som matchar primitiva värden i pattern contexts. |
| Scoped Value | 7 | Mekanism för att dela immutable kontext inom ett avgränsat dynamiskt scope. |
| Structured Concurrency | 8 | Modell där relaterade samtidiga uppgifter hanteras som en sammanhängande arbetsenhet. |
| Gatherer | 9 | Objekt som beskriver en mellanliggande stream-transformation. |
| Fönster | 9 | Grupp av intilliggande element i en stream, fast eller glidande. |
| Stateful intermediate operation | 9 | Mellanliggande stream-operation som behöver minnas tidigare element. |

| AOT cache | 10 | Cache som skapas före en produktionskörning och kan innehålla klassladdning, länkning och profileringsinformation för snabbare startup/warmup. |
| Training run | 10 | Representativ körning som samlar underlag till en AOT cache. |
| Object header | 10 | JVM-metadata som finns per Java-objekt och påverkar minnesfotavtryck. |
| Compact Object Headers | 10 | Java 25-produktfeature som gör objektens header-layout mer kompakt och kan minska heaptryck i objektintensiva system. |
| Garbage collector mode | 10 | Driftläge för en garbage collector, exempelvis generational eller non-generational. |
| Mätmatris | 10 | Tabell som separerar hypoteser, JVM-flaggor och metrikvärden vid prestandaexperiment. |

| CPU-time profiling | 11 | JFR-baserad profilering som samlar CPU-tidsrelaterade samples, experimental och Linux-inriktad i Java 25. |
| Cooperative sampling | 11 | JFR-förbättring som gör stack sampling stabilare genom säkrare stack walking med minskad safepoint bias. |
| Method Timing | 11 | JFR-funktion som mäter invokationer och ungefärlig tid för metoder som matchar ett filter. |
| Method Trace | 11 | JFR-funktion som registrerar anropskontext och stack traces för metoder som matchar ett filter. |
| JFR-filter | 11 | Urval som styr vilka metoder, klasser eller annotationer som ska mätas eller spåras av JFR. |
| KDF | 12 | Key Derivation Function; algoritm/API för att härleda nyckelmaterial från annat hemligt material och kontextdata. |
| PEM encoding | 12 | Textformat för transport och lagring av kryptografiska objekt som nycklar och certifikat. |
| Integrity by default | 12 | Plattformspolicy där integritetsbrytande operationer kräver tydligt godkännande i stället för att ske implicit. |
| Native access | 12 | Åtkomst till native code via exempelvis JNI eller FFM, som bör aktiveras och dokumenteras uttryckligen. |
| Beslutsmatris | 12 | Tabell som samlar status, risk, ägare och produktionsbeslut för Java 25-adoption. |

## Öppna beslut
- Eventuell export till EPUB/PDF/DOCX återstår.
- Kapitelutkasten bör helhetsgranskas för stil, progression och teknisk korrekthet före publicering.
- Kodexempel bör provköras i faktisk JDK 25-miljö före publicering.
- Preview- och experimental-exempel bör dubbelkontrolleras mot aktuell dokumentation vid export.

## Nästa rekommenderade steg
- Gör en helhetsgranskning av boken: progression, terminologi, källor och kodexempel.
- Förbered export till önskat format när granskningen är klar.
