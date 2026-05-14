# Övningar till Kapitel 10: AOT, Compact Object Headers och GC-nyheter

## Övning 1: Bygg en mätmatris

Skapa en mätmatris för ett Java 21-system som ska utvärderas på Java 25.

Minsta innehåll:

| Körning | JDK | JVM-flaggor | Primär metrisk | Regressionsgräns |
|---|---:|---|---|---|
| Baslinje | 21 |  |  |  |
| Ren migrering | 25 |  |  |  |
| AOT-labb | 25 |  |  |  |
| Compact Object Headers-labb | 25 |  |  |  |

Besvara också:

1. Vilka mätningar måste göras innan teamet ändrar produktionsflaggor?
2. Vilken körning isolerar effekten av JDK-uppgraderingen?
3. Vilken körning isolerar effekten av compact object headers?

## Övning 2: Designa en AOT training run

Skriv ett kort designförslag för en training run.

Ta med:

- startkommando
- vilka applikationsflöden som ska köras
- vilka externa beroenden som mockas
- vilka testdata som används
- hur cachen versioneras
- hur en oanvändbar cache ska hanteras i drift

## Övning 3: Object header-hypotes

Välj en datamodell med många små objekt.

Besvara:

1. Vilka objekt skapas flest gånger?
2. Hur stor andel av heapen tror du att objektmetadata kan vara?
3. Vilka JVM- och applikationsmetrikvärden behövs för att bedöma effekten?
4. Vilka resultat skulle få dig att avstå från adoption trots positivt labbresultat?

## Fördjupning: GC-beslut

Skriv en kort beslutsnotering där du jämför att behålla nuvarande GC med att testa ett alternativt GC-läge i Java 25.

Noteringen ska innehålla:

- nuvarande GC
- varför ett byte övervägs
- hypotes
- mätmetod
- risker
- rollback-plan
