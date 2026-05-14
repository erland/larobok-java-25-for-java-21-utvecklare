# Inledning

Java 25 är inte en bok för dig som lär dig Java från början. Den här boken utgår från att du redan arbetar med Java 21 och vill förstå vad som faktiskt spelar roll när nästa större Java-baslinje ska bedömas, införas och användas i riktiga system.

Bokens perspektiv är praktiskt. Varje nyhet behandlas inte bara som en språk- eller API-detalj, utan som ett beslut: När är den relevant? Vad kräver den av byggkedjan? Är den final, preview, incubating eller experimental? Vilka risker finns vid migration? Vilka vinster kan vara värda att mäta?

## Vem boken är till för

Boken riktar sig till erfarna systemutvecklare, tech leads, arkitekter och plattformsutvecklare som redan kan modern Java. Du bör vara bekväm med Java 21, records, pattern matching, switch expressions, virtual threads, byggverktyg och testning.

## Hur boken är upplagd

Boken börjar med en karta över skillnaderna mellan Java 21 och Java 25. Därefter går den igenom verktygskedja och migration, språknyheter, concurrency, dataflöden, JVM/prestanda, observability, säkerhet och produktionsbeslut.

Ett återkommande exempelprojekt, `OrderFlow`, används för att göra resonemangen konkreta. Det är inte tänkt som ett fullskaligt system, utan som en gemensam kontext för att visa kod, tradeoffs och migreringsbeslut.

## Hur du kan använda boken

Du kan läsa boken från början till slut om du planerar en bred uppgradering från Java 21. Du kan också använda den som handbok och hoppa till de kapitel som motsvarar dina viktigaste frågor: språk, concurrency, prestanda, diagnostik eller säkerhet.

När en feature är preview, incubating eller experimental markeras det tydligt. Sådana delar bör ses som underlag för utvärdering, inte automatiskt som rekommendation för produktionskod.
