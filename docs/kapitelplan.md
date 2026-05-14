# Kapitelplan

## Del 1: Orientering och migreringskarta

### Kapitel 1: Från Java 21 till Java 25
- Syfte: Ge helhetsbilden: vad som har ändrats, vad som är final, preview, incubating eller experimental.
- Läsarens förkunskaper: Praktisk Java 21.
- Nya huvudbegrepp: releasebaslinje, feature-status, migreringsrisk.
- Praktiskt exempel/scenario: OrderFlow-teamet inventerar en Java 21-kodbas inför Java 25.
- Övning: Klassificera features efter relevans och risk för ett befintligt system.
- Svårighetsgrad: Erfaren.
- Bygger vidare på: Java 21 och modern Java-releaseprocess.

### Kapitel 2: Verktygskedjan och första migreringsloopen
- Syfte: Visa hur man sätter upp JDK 25, bygger, testar och hittar regressionsrisker.
- Nya huvudbegrepp: toolchain, runtime-baslinje, kompatibilitetstest.
- Praktiskt exempel/scenario: Bygg OrderFlow med JDK 25 utan att ändra applikationskod.
- Övning: Skapa en migreringschecklista för CI.

## Del 2: Språket

### Kapitel 3: Compact Source Files och Instance Main Methods
- Syfte: Förstå förenklad programstruktur och när den är användbar för scripts, demos och tooling.
- Nya huvudbegrepp: compact source file, instance main method.
- Praktiskt exempel/scenario: Ett litet operationsverktyg för OrderFlow.
- Övning: Skriv om ett litet kommandoradsverktyg.

### Kapitel 4: Module Import Declarations
- Syfte: Förstå modulimporter och hur de påverkar läsbarhet i små och större program.
- Nya huvudbegrepp: module import declaration, exported package.
- Praktiskt exempel/scenario: Importera relevanta JDK-moduler i ett analysverktyg.
- Övning: Jämför vanliga imports med module imports.

### Kapitel 5: Flexible Constructor Bodies
- Syfte: Visa hur validering och säkra beräkningar före konstruktoranrop kan förbättra domänmodeller.
- Nya huvudbegrepp: constructor prologue, säker initiering.
- Praktiskt exempel/scenario: Order och Money-objekt med validering.
- Övning: Refaktorera konstruktorer utan att läcka halvinitierat tillstånd.

### Kapitel 6: Primitive Types i patterns, instanceof och switch
- Syfte: Förstå preview-status, möjligheter och begränsningar.
- Nya huvudbegrepp: primitive pattern, preview feature.
- Praktiskt exempel/scenario: Klassificera numeriska ordervärden och signaler.
- Övning: Experimentera med preview-kompilering.

## Del 3: Concurrency och dataflöden

### Kapitel 7: Scoped Values i praktiken
- Syfte: Använda Scoped Values som säkrare alternativ till ThreadLocal i moderna Java-system.
- Nya huvudbegrepp: scoped value, dynamiskt scope.
- Praktiskt exempel/scenario: Korrelations-ID i OrderFlow.
- Övning: Ersätt ThreadLocal med Scoped Values där det är lämpligt.

### Kapitel 8: Structured Concurrency och virtual threads
- Syfte: Förstå strukturerad samtidighet och hur den samspelar med virtual threads.
- Nya huvudbegrepp: task scope, cancellation, join.
- Praktiskt exempel/scenario: Hämta order-, lager- och betalningsdata parallellt.
- Övning: Bygg ett robust parallellt serviceanrop.

### Kapitel 9: Stream Gatherers och nya sätt att forma dataflöden
- Syfte: Visa hur Stream Gatherers kan förenkla sekventiell transformation.
- Nya huvudbegrepp: gatherer, intermediate operation.
- Praktiskt exempel/scenario: Batcha orderhändelser för vidare bearbetning.
- Övning: Implementera och testa ett dataflöde.

## Del 4: JVM, prestanda och observability

### Kapitel 10: AOT, Compact Object Headers och GC-nyheter
- Syfte: Ge en praktisk karta över prestandarelaterade nyheter utan att överlova.
- Nya huvudbegrepp: AOT cache, object header, garbage collector mode.
- Praktiskt exempel/scenario: Starttid, minnesprofil och throughput i OrderFlow.
- Övning: Skapa en mätplan före och efter migrering.

### Kapitel 11: JFR-nyheter för CPU, sampling och metodtracing
- Syfte: Använda JFR för bättre observability i Java 25.
- Nya huvudbegrepp: cooperative sampling, method timing, profiling event.
- Praktiskt exempel/scenario: Felsök långsam ordervalidering.
- Övning: Ta fram en JFR-baserad analysrapport.

## Del 5: Säkerhet, kompatibilitet och beslut

### Kapitel 12: Kryptografi, Unsafe, JNI och produktionsbeslut
- Syfte: Sammanfatta säkerhetsnyheter och kompatibilitetsfrågor inför produktion.
- Nya huvudbegrepp: KDF, PEM encoding, integrity by default.
- Praktiskt exempel/scenario: Säker nyckelhantering och riskanalys i OrderFlow.
- Övning: Skriv en beslutsmatris för Java 25-adoption.

## Progressionskontroll
- Begrepp introduceras i rätt ordning: först releasekarta, sedan språk, concurrency, prestanda, observability och säkerhet.
- För svåra hopp: preview/incubator/experimental markeras tydligt innan kod används.
- Repetitionstillfällen: Java 21-baslinjen återkopplas i varje kapitel.
- Slutprojekt eller sammanfattande moment: kapitel 12 leder till en migrations- och beslutsmatris.
