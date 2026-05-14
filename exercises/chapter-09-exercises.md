# Övningar till kapitel 9: Stream Gatherers och nya sätt att forma dataflöden

## Övning 1: Batcha orderhändelser

Skapa en lista med minst tio orderhändelser.

Använd:

```java
Gatherers.windowFixed(4)
```

Skriv ut en rad per batch med:

- antal händelser
- summa av orderbelopp
- högsta orderbelopp
- order-ID:n i batchen

### Kontrollfråga

Vad händer med sista batchen om listans längd inte är delbar med fyra?

## Övning 2: Hitta glidande riskmönster

Använd:

```java
Gatherers.windowSliding(3)
```

Hitta alla fönster där minst två order har belopp över 10 000.

Skriv ut:

- order-ID:n i fönstret
- totalsumma
- antal höga order

## Övning 3: Jämför med loop

Lös övning 1 igen med en vanlig `for`-loop.

Jämför lösningarna:

1. Vilken är lättast att läsa?
2. Vilken är lättast att felsöka?
3. Vilken är lättast att ändra från batchstorlek 4 till 5?
4. Vilken passar bäst i ert teams kodstandard?

## Fördjupning: Refaktorera en befintlig Java 21-loop

Hitta en loop i en befintlig kodbas som:

- grupperar element
- bygger sekvensmönster
- gör löpande summering
- använder index för att jämföra närliggande element

Bedöm om koden vinner på att skrivas med gatherers.

Skriv ett kort adoptionsbeslut:

```text
Vi bör / bör inte använda gatherers här eftersom ...
Risker:
Kodstandard:
Testkrav:
```
