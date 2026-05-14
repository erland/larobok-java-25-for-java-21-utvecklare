# Övningar till Kapitel 12: Kryptografi, Unsafe, JNI och produktionsbeslut

## Övning 1: Beslutsmatris för Java 25

Skapa en beslutsmatris för ett system du arbetar med.

Använd följande kolumner:

| Område | Status | Risk | Ägare | Rekommendation | Blockerar produktion? |
|---|---|---|---|---|---|

Minst följande områden ska finnas med:

- runtime-migration till Java 25,
- preview-features,
- experimental JVM-features,
- KDF API,
- PEM API,
- `sun.misc.Unsafe`,
- JNI/native access,
- observability-agenter,
- kryptografiska providers,
- beroendeuppgraderingar.

## Övning 2: Krypto-inventering

Sök efter kryptografisk specialkod i en kodbas.

Leta efter:

- egen HKDF/KDF-implementation,
- manuell PEM-parsing,
- Base64-hantering av nycklar eller certifikat,
- intern användning av JDK-klasser,
- provider-specifik kod,
- otydliga salts, nyckellängder eller algoritmnamn.

För varje träff, skriv:

1. vad koden gör,
2. varför den finns,
3. om Java 25 ger ett standardiserat alternativ,
4. om ändringen kräver säkerhetsgranskning,
5. rekommendation: behåll, ersätt, isolera eller utred.

## Övning 3: Unsafe- och native-rapport

Kör testsviten med Java 25 och samla signaler om `Unsafe` och native access.

Dokumentera:

| Signal | Källa | Direkt/indirekt | Föreslagen åtgärd | Ägare | Deadline |
|---|---|---|---|---|---|

Bedöm om varje signal är:

- blockerande,
- accepterad temporärt,
- kräver beroendeuppgradering,
- kräver vendor-dialog,
- falskt positiv eller ofarlig i er miljö.

## Fördjupning: Releasekriterier

Skriv fem konkreta releasekriterier för Java 25-migreringen. Exempel:

- Inga okända `Unsafe`-varningar i staging.
- Alla preview-features är isolerade till labbmoduler.
- Native access är dokumenterad per modul eller dependency.
- Kryptografiska ändringar har testvektorer och säkerhetsgodkännande.
- Rollbackplan för runtime och beroendeuppsättning är testad.
