# Övningar till kapitel 2: Verktygskedjan och första migreringsloopen

## Övning 1: Inventera toolchain

Fyll i för ett verkligt Java 21-projekt.

| Fråga | Svar |
|---|---|
| Lokal JDK |  |
| CI-JDK |  |
| Produktions-JDK |  |
| Byggverktyg och version |  |
| Explicit `--release` eller motsvarande |  |
| Annotation processors |  |
| Java agents/JNI/profilerare |  |
| Container-image |  |
| JVM-flaggor |  |

## Övning 2: Klassificera fel

Skapa fem fiktiva eller verkliga migreringsfel och sortera dem.

| ID | Feltyp | Symptom | Första åtgärd |
|---|---|---|---|
|  | Miljö / plugin / test / runtime / beroende / applikationskod |  |  |

## Övning 3: Skriv en CI-checklista

Checklistan ska minst innehålla:

- JDK-version skrivs ut.
- Byggverktygets version skrivs ut.
- Projektet byggs rent.
- Tester körs.
- Loggar sparas.
- Fel klassificeras.
- Preview-features hålls utanför första loopen.

## Reflektion

Besvara kort:

1. Vilket beslut är mest riskabelt i ditt projekt: byta bygg-JDK, runtime-JDK eller källkodsnivå?
2. Vilka beroenden eller verktyg tror du är mest känsliga för Java 25?
3. Vilka resultat måste finnas innan teamet får börja använda Java 25-specifika features?
