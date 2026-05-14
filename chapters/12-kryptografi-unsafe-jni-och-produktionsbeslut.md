# Kapitel 12: Kryptografi, Unsafe, JNI och produktionsbeslut

## Varför detta kapitel finns

När ett team går från Java 21 till Java 25 är det lätt att fokusera på språknyheter, concurrency och prestanda. I produktion är de svårare besluten ofta mer jordnära: nyckelhantering, beroenden som använder intern eller osäker funktionalitet, native integrationer och hur mycket risk som ska accepteras i första releasen.

Det här kapitlet samlar därför tre spår:

1. säkerhets- och kryptografinyheter som kan vara värda att adoptera,
2. kompatibilitetsrisker runt `sun.misc.Unsafe`, JNI och native access,
3. en praktisk beslutsmatris för vad OrderFlow-teamet bör göra före produktionssättning på Java 25.

Målet är inte att göra dig till kryptograf. Målet är att ge dig ett tillräckligt tydligt tekniskt beslutsunderlag för att kunna föra rätt diskussion med säkerhetsansvariga, plattformsägare och beroendeägare.

## Lärandemål

Efter kapitlet ska läsaren kunna:

- skilja mellan kompatibilitetsmigration och säkerhets-/feature-adoption,
- beskriva vad KDF API tillför jämfört med äldre ad hoc-lösningar,
- förklara varför PEM-stöd i Java 25 är användbart men fortfarande preview,
- identifiera risker från `sun.misc.Unsafe`, JNI och native access,
- skapa en beslutsmatris för Java 25-adoption i ett produktionssystem.

## Innan vi börjar

Vi har redan etablerat bokens huvudprincip: **kompatibilitet före adoption**. Kapitel 1 introducerade releasebaslinje och feature-status. Kapitel 2 visade den första migreringsloopen. Kapitel 10 och 11 tog upp prestanda och observability. Nu använder vi samma arbetssätt för säkerhet och produktionsbeslut.

Kom ihåg statusorden:

- **Final**: kan användas utan preview- eller incubator-flaggor.
- **Preview**: kräver uttryckligt val, kan ändras och bör hanteras som adoptionsbeslut.
- **Experimental**: bör behandlas som mät- eller labbfunktion innan produktion.
- **Intern/unsupported API**: kan fungera i dag men är inte en stabil kontraktsyta.

## Huvudförklaring

### 1. Säkerhetsförändringar är inte bara “nya API:er”

I en vanlig migrering finns två sorters säkerhetsfrågor.

Den första sorten är positiv adoption: “Java 25 ger oss ett bättre sätt att göra något vi redan gör.” Exempel är ett standardiserat KDF API för nyckelhärledning eller ett API för PEM-kodning av kryptografiska objekt.

Den andra sorten är riskreduktion: “Java 25 gör det tydligare att något vi eller våra beroenden gör är riskabelt.” Exempel är varningar för `sun.misc.Unsafe` eller krav på mer explicit native access.

Båda sorterna är viktiga, men de ska inte hanteras på samma sätt. Ett nytt kryptografi-API kan införas stegvis bakom tester och säkerhetsgranskning. En varning från ett transitive dependency som använder `Unsafe` kan däremot kräva beroendeuppgradering, vendor-dialog eller runtime-policy.

### 2. KDF API: standardiserad nyckelhärledning

**KDF** står för *Key Derivation Function*. En KDF används när ett system behöver härleda kryptografiskt nyckelmaterial från annat nyckelmaterial och kontextdata, exempelvis salt, “info” eller protokollspecifika parametrar.

I äldre Java-kod kan man ofta se tre problem:

- egenimplementerad HKDF eller liknande logik,
- intern användning av provider-specifika klasser,
- otydlig separation mellan slumpgenerering, nyckelhärledning och nyckellagring.

Java 25 introducerar ett finalt KDF API. För OrderFlow innebär det inte att alla kryptografiska flöden automatiskt ska skrivas om, men det innebär att teamet bör inventera var nyckelhärledning sker och om standard-API:t kan minska specialkod.

Ett förenklat adoptionsbeslut kan se ut så här:

| Fråga | Rekommenderat svar innan adoption |
|---|---|
| Finns egen HKDF-/KDF-kod? | Ja/nej, identifierad i kodbasen. |
| Finns testvektorer? | Ja, helst från standardiserade testfall. |
| Är providerkraven kända? | Ja, inklusive driftmiljö och HSM/PKCS#11 om sådant används. |
| Har säkerhetsansvarig granskat ändringen? | Ja, innan produktionssättning. |
| Är rollback möjlig? | Ja, via konfigurerad implementation eller feature flag. |

Poängen är inte att “nytt API är alltid bättre”. Poängen är att ett standardiserat API ofta ger bättre underhållbarhet, tydligare providerintegration och färre lokala tolkningar.

### 3. PEM encoding: praktiskt, men preview i Java 25

PEM är ett textformat för exempelvis publika nycklar, privata nycklar, certifikat och certifikatkedjor. Många Java-system hanterar PEM redan i dag, men ofta via handskriven parsing, Base64-kod, tredjepartsbibliotek eller säkerhetsbibliotek med egna konventioner.

Java 25 innehåller ett preview-API för PEM encoding och decoding av kryptografiska objekt. Det är praktiskt eftersom det angriper ett vanligt problem: att gå mellan Java-objekt som `PublicKey`, `PrivateKey` eller certifikat och PEM-text.

Men eftersom API:t är preview ska det inte införas i produktionskod som om det vore ett stabilt långsiktigt kontrakt. I OrderFlow blir rekommendationen därför:

- använd PEM-preview i labb, migreringsverktyg och prototyper,
- undvik att göra det till publik API-yta,
- isolera eventuell användning bakom en liten adapter,
- dokumentera att den kräver preview-flaggor,
- planera omgranskning när API:t finaliseras eller ändras.

Det här är samma princip som tidigare kapitel använt för preview-språkfeatures: adoption är möjlig, men ska vara avgränsad och reversibel.

### 4. `sun.misc.Unsafe`: från dold risk till synlig signal

Många applikationsteam använder inte `sun.misc.Unsafe` direkt. Ändå kan det finnas i beroenden för serialization, off-heap-minne, högt optimerade datastrukturer eller äldre nätverksbibliotek.

I moderna JDK-versioner har Java-plattformen rört sig mot **integrity by default**: farliga eller integritetsbrytande operationer ska inte ske tyst. Java 25 ligger efter JDK 24 i den kedjan, vilket betyder att team som migrerar från Java 21 bör behandla `Unsafe`-varningar som produktionssignaler, inte som kosmetiskt brus.

Ett praktiskt arbetssätt är:

1. kör OrderFlow-testsviten med Java 25,
2. aktivera tydligare diagnostik för `Unsafe`-användning,
3. få fram vilken modul eller vilket beroende som orsakar varningen,
4. kontrollera om nyare version av beroendet använder standard-API:er,
5. dokumentera om undantag accepteras temporärt.

En vanlig fälla är att bara stänga av varningen. Det kan vara rimligt kortsiktigt i en kontrollerad migrering, men då ska beslutet ha ägare, slutdatum och uppföljning.

### 5. JNI och native access: gör implicit risk explicit

JNI är inte borta och är inte “förbjudet”. Det är fortfarande ett standardiserat sätt att integrera Java med native code. Det Java 25-teamet behöver förstå är att native access i allt högre grad kräver explicit godkännande.

För OrderFlow kan native access dyka upp via:

- komprimeringsbibliotek,
- kryptografiproviders,
- databasdrivrutiner eller klientbibliotek,
- observability-agenter,
- legacy-integrationer,
- FFM- eller JNI-baserade prestandabibliotek.

I JDK 24 introducerades varningar och mekanismer som förbereder hårdare restriktioner. När OrderFlow går till Java 25 bör teamet därför göra en native-inventering:

| Kontrollpunkt | Exempel på fråga |
|---|---|
| Var laddas native libraries? | `System.loadLibrary`, JNI, FFM, agent eller dependency? |
| Vem äger risken? | Applikationsteam, plattformsteam, vendor eller säkerhetsteam? |
| Krävs startup-flagga? | Exempelvis `--enable-native-access=...`. |
| Kan åtkomst begränsas? | Helst per modul, inte brett för hela classpath. |
| Finns Java-baserat alternativ? | Standard-API, ren Java-provider eller nyare beroende. |
| Är driftmiljön dokumenterad? | Container image, OS, CPU-arkitektur, biblioteksversioner. |

Det viktiga är att flytta native access från “något som bara händer” till “något vi uttryckligen accepterar”.

### 6. Beslutsmatris för OrderFlow

När alla tekniska detaljer samlas behöver teamet en enkel beslutsmodell. Här är en variant för Java 25-adoption:

| Område | Status | Produktionsbeslut | Kommentar |
|---|---:|---|---|
| Kompilera och testa på JDK 25 | Obligatoriskt | Ja | Första kompatibilitetsloopen. |
| Köra på Java 25 runtime | Obligatoriskt inför migration | Ja efter test | Kräver runtime-baslinje och rollbackplan. |
| KDF API | Final | Selektiv adoption | Endast efter säkerhetsgranskning och testvektorer. |
| PEM API | Preview | Labb/adapter, inte bred produktion | Kräver preview-flaggor och omprövning. |
| `sun.misc.Unsafe` i egna koden | Unsupported/risk | Nej | Refaktorera till standard-API. |
| `sun.misc.Unsafe` i beroenden | Riskindikator | Tillfälligt accepterat endast med plan | Uppgradera eller ersätt beroende. |
| JNI/native access | Standard men riskfyllt | Explicit godkännande | Begränsa åtkomst och dokumentera. |
| Preview language/API features | Preview | Separat adoptionsbeslut | Ska inte blandas in av misstag. |
| Experimental JVM-features | Experimental | Mätning/labb | Inte default i produktion utan beslut. |

Matrisen ska inte bara ligga i en wiki. Den bör kopplas till releasekriterier:

- inga okända `Unsafe`-varningar,
- inga okända native-access-varningar,
- preview-features får bara förekomma i godkända moduler,
- kryptografiska ändringar kräver säkerhetsgranskning,
- runtime-flaggor dokumenteras i driftkonfigurationen,
- rollback till tidigare runtime eller tidigare dependency-set är testad.

## Exempel: OrderFlow inför produktionssättning

OrderFlow-teamet har kört migreringsloopen från kapitel 2 och ser följande:

- applikationen kompilerar med JDK 25,
- produktionens bytecode-nivå är fortfarande `--release 21`,
- testerna passerar,
- en äldre serialization-komponent orsakar `sun.misc.Unsafe`-varning,
- en kryptografisk helper innehåller egen HKDF-liknande kod,
- en observability-agent använder native library,
- PEM-hantering sker via ett litet tredjepartsbibliotek.

Teamet gör då inte allt på en gång. De delar upp arbetet:

### Beslut A: runtime-migration

OrderFlow får köra på Java 25 i staging och därefter produktion, eftersom kompatibilitetstesterna passerar. Detta är ett **kompatibilitetsbeslut**.

### Beslut B: `Unsafe` i serialization

Teamet accepterar inte varningen permanent. Beroendet uppgraderas i en separat branch. Om uppgraderingen är riskfylld dokumenteras ett temporärt undantag med ägare och datum.

### Beslut C: KDF API

Den egenbyggda HKDF-liknande hjälparen markeras för ersättning. Men eftersom kryptografiskt beteende är säkerhetskritiskt krävs testvektorer, kodgranskning och säkerhetsgodkännande innan ändringen går till produktion.

### Beslut D: PEM preview

Teamet låter befintlig PEM-hantering ligga kvar i produktion. Java 25:s PEM API testas i `java25-labs/` och isoleras bakom ett experimentellt adaptergränssnitt.

### Beslut E: native access

Observability-agenten får fortsatt användas, men startup-flaggor och native library-version dokumenteras i driftkonfigurationen. Plattformsteamet blir ägare till uppföljning.

Resultatet blir en kontrollerad migration: Java 25 används utan att teamet samtidigt byter kryptografiskt beteende, preview-API och native policy i samma release.

## Vanliga misstag

- **Misstag: att behandla alla Java 25-nyheter som produktionsklara.**
  - Varför det händer: versionen är stabil, men enskilda features kan vara preview eller experimental.
  - Hur man undviker det: dokumentera feature-status per beslut.

- **Misstag: att ignorera `Unsafe`-varningar eftersom de kommer från beroenden.**
  - Varför det händer: applikationsteamet äger inte koden direkt.
  - Hur man undviker det: skapa beroendeärenden, vendorfrågor och temporära undantag med slutdatum.

- **Misstag: att införa kryptografiska API-ändringar som vanlig refaktorering.**
  - Varför det händer: API:t ser enkelt ut och testerna kanske passerar.
  - Hur man undviker det: använd testvektorer, säkerhetsgranskning och explicit rollbackplan.

- **Misstag: att aktivera native access brett utan analys.**
  - Varför det händer: `ALL-UNNAMED` är enkelt i classpath-baserade applikationer.
  - Hur man undviker det: flytta riskbärande beroenden mot modulpath där det går och begränsa åtkomst.

- **Misstag: att blanda preview-adoption med runtime-migration.**
  - Varför det händer: teamet vill “passa på” när JDK ändå byts.
  - Hur man undviker det: gör kompatibilitetsmigration först, feature-adoption därefter.

## Övningar

### Övning 1: Skapa en beslutsmatris

Utgå från ett system du känner till. Lista minst åtta Java 25-relaterade beslut i en tabell med kolumnerna:

- område,
- status,
- risk,
- ägare,
- rekommendation,
- blockerar produktion: ja/nej.

Markera särskilt preview, experimental, JNI/native access och `Unsafe`.

### Övning 2: Inventera kryptografisk kod

Sök i kodbasen efter:

- egen HKDF/KDF-logik,
- manuell PEM-parsing,
- direkt Base64-hantering av nycklar eller certifikat,
- provider-specifik kod,
- användning av `SecretKeyFactory`, `KeyGenerator` eller interna säkerhetsklasser.

Skriv för varje träff: behåll, ersätt, isolera eller utred.

### Övning 3: Native access-rapport

Identifiera vilka beroenden som kan ladda native libraries. Dokumentera:

- bibliotek,
- version,
- varför native code används,
- om Java-baserat alternativ finns,
- vilken runtime-flagga som krävs,
- vem som äger risken.

### Fördjupning

Gör en separat branch där ni kör applikationen med striktare diagnostik för `Unsafe` och native access. Målet är inte att allt ska fungera direkt, utan att få en tydlig lista över risker och ägare.

## Snabb sammanfattning

- Java 25-migrering handlar inte bara om nya språkfeatures.
- KDF API är finalt i Java 25 och kan minska egen kryptografisk specialkod, men kräver säkerhetsgranskad adoption.
- PEM API är preview i Java 25 och bör isoleras från produktionskontrakt.
- `sun.misc.Unsafe`-varningar är signaler om teknisk och framtida kompatibilitetsrisk.
- JNI och native access ska vara explicita, begränsade och dokumenterade.
- En beslutsmatris hjälper teamet att skilja kompatibilitetsmigration från feature-adoption.

## Quiz/reflektionsfrågor

1. Varför bör KDF-adoption behandlas annorlunda än en vanlig kodrefaktorering?
2. När kan det vara rimligt att acceptera en `sun.misc.Unsafe`-varning temporärt?
3. Varför är preview-status extra känslig för API:er som kan hamna i publika kontrakt?
4. Vad är skillnaden mellan att använda JNI och att godkänna native access i en driftmiljö?
5. Vilka Java 25-beslut i ditt system blockerar produktion, och vilka kan vänta till en senare adoptionsfas?

## Nästa steg

Bokens planerade första kapitelserie är nu komplett. Nästa naturliga steg är en helhetsgranskning: kontrollera progression, jämna ut nivån, verifiera källor och skapa en sammanhållen export till EPUB, PDF, DOCX eller Markdown.
