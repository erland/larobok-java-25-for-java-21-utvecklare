# Övningar till Kapitel 8: Structured Concurrency och virtual threads

## Övning 1: Hitta rätt scope

Välj ett serviceflöde där flera externa anrop görs i samma request.

Beskriv:

- vilka anrop som hör ihop
- vad som ska hända om ett anrop misslyckas
- var helheten ska vänta in resultaten
- vilka uppgifter som kan avbrytas

## Övning 2: Från Future till StructuredTaskScope

Skriv om ett exempel med `ExecutorService` och flera `Future`-objekt till `StructuredTaskScope`.

Krav:

- Scopet ska vara lokalt i metoden.
- Alla `fork(...)`-anrop ska ligga i samma block.
- Resultat ska läsas efter `join()`.
- Inga `Future` eller `Subtask` ska returneras från metoden.

## Övning 3: Fel och cancellation

Ändra kodexemplet så att en subtask kastar ett undantag.

Undersök:

1. Var fångas felet?
2. Vilka andra subtasks körde klart?
3. Vilka avbröts?
4. Vad behöver loggas för att felet ska bli begripligt?

## Övning 4: Designbeslut

Skriv ett kort Architecture Decision Record, ADR, för om OrderFlow ska använda Structured Concurrency i Java 25.

Ta med:

- feature-status: preview
- vilka moduler som får prova API:et
- hur preview-flaggor hanteras i build och runtime
- vilka produktionsrisker som finns
- hur beslutet ska omprövas när API:et ändrar status

## Fördjupning

Kombinera kapitel 7 och 8:

- Bind ett korrelations-ID med `ScopedValue`.
- Starta tre subtasks i ett `StructuredTaskScope`.
- Läs korrelations-ID i klientklasserna.
- Diskutera om detta gör koden tydligare eller mer implicit.
