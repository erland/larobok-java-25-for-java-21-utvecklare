# Kapitel 6: Primitive Types i patterns, instanceof och switch

## Varför detta kapitel finns

Pattern matching i Java har stegvis gjort kod som tidigare var full av typkontroller, castar och långa `if`-kedjor mer uttrycksfull. I Java 21 var många av de praktiskt viktiga delarna redan på plats: pattern matching för `instanceof`, record patterns och `switch` över referenstyper.

Men Java har fortfarande en tydlig historisk gräns mellan **referenstyper** och **primitiva typer**. `int`, `long`, `double` och de andra primitiva typerna beter sig inte som vanliga objekt. De har inte identitet, de kan inte vara `null`, och de har särskilda regler för konvertering, jämförelse och representation.

**Primitive Types in Patterns, instanceof, and switch** försöker minska friktionen mellan dessa två världar. I JDK 25 är featuren fortfarande en **preview feature**. Det är viktigt: kapitlets mål är inte att rekommendera omedelbar produktionsadoption, utan att ge dig en korrekt mental modell och ett säkert sätt att experimentera.

I OrderFlow använder vi featuren för att resonera om numeriska signaler:

- ordervärden
- riskpoäng
- lagerantal
- gränsvärden från externa system

Det är typisk kod där team ofta blandar `int`, `long`, `double`, wrappers, manuella intervallkontroller och `switch`.

## Lärandemål

Efter kapitlet ska läsaren kunna:

- förklara vad primitive patterns tillför jämfört med Java 21
- beskriva varför featuren är preview i JDK 25 och vad det betyder praktiskt
- använda `switch` med primitiva värden i små experiment
- förstå skillnaden mellan exakt, säker och förlustbringande primitiv konvertering
- känna igen när pattern matching gör numerisk kod tydligare
- avgöra varför produktionskod bör vara försiktig med preview-features

## Innan vi börjar

Från kapitel 1 och 2 har vi två grundregler:

1. **Kompatibilitetsmigration först.** Bygg och kör Java 21-koden på JDK 25 innan ni adopterar nya features.
2. **Feature-status styr adoption.** Final, preview, incubating och experimental ska behandlas olika.

Det här kapitlet handlar om en **preview feature** i JDK 25. Det innebär att kod som använder featuren normalt måste kompileras och köras med `--enable-preview`, och att syntax eller semantik fortfarande kan ändras i en senare Java-version.

Det gör featuren intressant för labb, prototyper, intern utbildning och designutvärdering. Det gör den däremot olämplig som oreflekterad standard i långlivad produktionskod.

## Huvudförklaring

### Problemet i Java 21

I Java 21 kan `switch` användas betydligt bredare än i äldre Java. Men när kod arbetar med numeriska värden hamnar man fortfarande ofta i klassisk kontrollflödeskod.

Exempel:

```java
static String classifyQuantity(int quantity) {
    if (quantity < 0) {
        return "invalid";
    }
    if (quantity == 0) {
        return "empty";
    }
    if (quantity <= 10) {
        return "low";
    }
    return "available";
}
```

Det här är tydligt nog. Problemet uppstår när samma klassificering växer:

- flera numeriska typer blandas
- data kommer från externa system som `Object`, `Number` eller text
- gränser och specialfall sprids över flera `if`-satser
- kodbasen använder både `Integer`, `Long`, `BigDecimal`, `double` och primitiva typer

Pattern matching har redan förbättrat referenstypsdelen av problemet. Men för primitiva typer har Java traditionellt varit mer begränsat.

### Vad Java 25-previewen vill uppnå

Featuren i JDK 25 har tre huvudidéer:

1. pattern matching ska kunna uttrycka primitiva typmönster
2. `instanceof` ska kunna resonera om fler typrelationer än bara referenstyper
3. `switch` ska kunna arbeta mer enhetligt med primitiva värden

Tanken är inte att göra `int` till ett objekt. Tanken är att låta språkets mönsterlogik fungera mer konsekvent även när värden är primitiva.

En förenklad mental modell är:

> Om Java redan kan avgöra att ett värde säkert kan behandlas som en viss primitiv typ, ska samma idé kunna uttryckas i patterns och `switch`.

Det viktiga ordet är **säkert**.

### Exakthet och konvertering

Primitiva typer kan konverteras på olika sätt. Vissa konverteringar är ofarliga, andra kan tappa information.

Exempel:

```java
int i = 42;
long l = i;       // säker widening conversion

long big = 10_000_000_000L;
// int x = big;   // inte säker utan explicit cast
```

När primitive patterns används måste du tänka på samma grundfråga:

- Kan värdet representeras i måltypen?
- Kräver konverteringen avrundning?
- Kan tecken, precision eller storlek gå förlorad?
- Är matchningen tänkt att testa typ, intervall eller båda?

Det här gör featuren kraftfull, men också lätt att misstolka om teamet inte är överens om stilregler.

### `switch` över primitiva värden

Ett av de mest praktiska användningsområdena är `switch` med fler primitiva typer och mer uttrycksfulla case.

I ett OrderFlow-scenario kan vi klassificera ett ordervärde:

```java
static String classifyOrderValue(long amountInCents) {
    return switch (amountInCents) {
        case long cents when cents < 0 -> "invalid";
        case 0L -> "free";
        case long cents when cents < 10_000 -> "small";
        case long cents when cents < 100_000 -> "standard";
        default -> "large";
    };
}
```

Koden visar två saker:

- `switch` kan uttrycka både specialvärden och intervall
- `when`-villkor gör att klassificeringslogiken kan ligga nära själva matchningen

Det här kan vara mer läsbart än en lång `if`-kedja när varje gren verkligen beskriver en kategori.

Men det är inte automatiskt bättre. Om logiken är enkel kan en `if`-kedja fortfarande vara tydligast.

### `instanceof` och primitiva måltyper

`instanceof` har historiskt handlat om referenstyper. Med primitive patterns utforskar Java en mer generell modell där uttrycket kan testa om ett värde passar en viss typ.

Ett förenklat exempel:

```java
static String describeNumber(Object value) {
    return switch (value) {
        case Integer i -> "integer: " + i;
        case Long l -> "long: " + l;
        case Double d when Double.isFinite(d) -> "finite double: " + d;
        case Double d -> "non-finite double: " + d;
        case null -> "missing";
        default -> "not numeric";
    };
}
```

Det här exemplet använder fortfarande wrapper-typer eftersom `Object` inte kan innehålla ett rått primitivt värde. Men i kod som redan arbetar med primitiva uttryck blir primitive patterns relevanta för att undvika onödig boxning och för att uttrycka konverteringsregler tydligare.

### Varför preview-statusen spelar roll

Preview betyder inte “dåligt”. Det betyder att Java-plattformen vill ha verklig erfarenhet innan featuren fryses.

För ett erfaret utvecklingsteam är den viktigaste konsekvensen organisatorisk:

- byggkommandon måste använda `--enable-preview`
- testkörning måste också använda `--enable-preview`
- CI måste vara tydlig med att preview-kod finns
- publika bibliotek bör undvika att exponera preview-syntax i kod som användare behöver kompilera
- uppgradering till en senare JDK kan kräva kodändringar

I OrderFlow är regeln därför:

> Preview-features får användas i `java25-labs/`, benchmarkexperiment och utbildningskod, men inte i huvudapplikationens produktionsmoduler utan ett uttryckligt arkitekturbeslut.

## Exempel: klassificera ordersignaler

Vi börjar med en klassisk Java 21-lösning:

```java
static String classifySignal(long signal) {
    if (signal < 0) {
        return "invalid";
    }
    if (signal == 0) {
        return "none";
    }
    if (signal <= 100) {
        return "normal";
    }
    if (signal <= 1_000) {
        return "elevated";
    }
    return "critical";
}
```

Den är enkel, robust och lätt att förstå. Det finns inget egenvärde i att skriva om den.

Men om klassificeringen består av många specialfall, intervall och typfall kan `switch` bli mer uttrycksfullt:

```java
static String classifySignalPreview(long signal) {
    return switch (signal) {
        case long s when s < 0 -> "invalid";
        case 0L -> "none";
        case long s when s <= 100 -> "normal";
        case long s when s <= 1_000 -> "elevated";
        default -> "critical";
    };
}
```

Här beskriver varje rad en kategori. Det gör det lättare att granska klassificeringsreglerna som en tabell.

### Kompilera preview-exemplet

För en enskild fil:

```bash
javac --release 25 --enable-preview OrderSignalClassifier.java
java --enable-preview OrderSignalClassifier
```

Om ni använder Maven eller Gradle behöver motsvarande flaggor läggas in i byggkonfigurationen. Gör det helst i en separat labbmodul så att preview-valet inte råkar sprida sig till hela systemet.

## Vanliga misstag

- Misstag: Att behandla preview som final.
  - Varför det händer: Featuren finns i JDK:n och exemplen kompilerar.
  - Hur man undviker det: Dokumentera preview-status i README, CI och arkitekturbeslut.

- Misstag: Att ersätta tydliga `if`-satser med komplexa `switch`-uttryck.
  - Varför det händer: Ny syntax känns modernare.
  - Hur man undviker det: Använd featuren bara när den gör kategorier, typfall eller gränser tydligare.

- Misstag: Att glömma `--enable-preview` vid körning.
  - Varför det händer: Teamet lägger flaggan i kompileringen men inte i test- eller runtime-steget.
  - Hur man undviker det: Lägg preview-experiment i egen modul med tydliga build scripts.

- Misstag: Att ignorera numerisk precision.
  - Varför det händer: Pattern matching kan få typfrågor att se enklare ut än de är.
  - Hur man undviker det: Skriv tester för gränsvärden, overflow, avrundning och negativa tal.

- Misstag: Att använda primitive patterns som ersättning för domänmodellering.
  - Varför det händer: Numeriska regler blir kompakta.
  - Hur man undviker det: Behåll domänbegrepp som `Money`, `Quantity` och `RiskScore` där de ger mening.

## Övningar

### Övning 1: Identifiera kandidater

Gå igenom en Java 21-kodbas och leta efter kod som:

- klassificerar numeriska värden
- blandar `if`, `switch` och manuella castar
- använder wrapper-typer bara för att kunna göra typkontroller
- har många specialfall kring `0`, negativa tal eller maxvärden

Markera tre kandidater där primitive patterns skulle kunna göra koden tydligare i en Java 25-labbgren.

### Övning 2: Skriv om en klassificering

Skriv en Java 25-preview-version av följande metod:

```java
static String classifyStockDelta(int delta) {
    if (delta < -100) {
        return "large-negative-change";
    }
    if (delta < 0) {
        return "negative-change";
    }
    if (delta == 0) {
        return "unchanged";
    }
    if (delta <= 100) {
        return "positive-change";
    }
    return "large-positive-change";
}
```

Jämför sedan versionerna. Vilken är tydligast? Varför?

### Övning 3: Byggpolicy för preview

Skriv en kort policy för ert team:

- Var får preview-kod finnas?
- Vem får godkänna preview i produktionskod?
- Hur ska CI markera preview-användning?
- Hur ofta ska preview-kod omprövas vid JDK-uppgradering?

### Fördjupning

Undersök vilka gränsfall som är viktiga för er domän:

- `Integer.MAX_VALUE`
- `Long.MAX_VALUE`
- negativa värden
- avrundning från flyttal
- `NaN` och infinity för `double`
- externa numeriska format från JSON, databaser eller meddelandeköer

Skriv tester före ni provar ny syntax.

## Snabb sammanfattning

- Primitive patterns försöker göra pattern matching mer enhetlig över referenstyper och primitiva typer.
- I JDK 25 är featuren preview och kräver därför särskild bygg- och körkonfiguration.
- `switch` över primitiva värden kan göra numerisk klassificering tydligare när det finns många kategorier.
- Numerisk precision, intervall och konverteringsregler är fortfarande viktiga.
- Preview-features passar bäst i labb, experiment och utbildning tills teamet har ett tydligt adoptionsbeslut.
- Domänmodeller som `Money`, `Quantity` och `RiskScore` ska inte ersättas slentrianmässigt av råa primitiva värden.

## Quiz/reflektionsfrågor

1. Varför bör primitive patterns i JDK 25 behandlas annorlunda än Flexible Constructor Bodies?
2. Vad är skillnaden mellan att matcha ett numeriskt intervall och att matcha en numerisk typ?
3. När är en `if`-kedja tydligare än ett `switch`-uttryck?
4. Vilka risker uppstår om preview-kod hamnar i ett publikt bibliotek?
5. Varför räcker det inte att bara tänka på syntax när man arbetar med primitiva numeriska värden?
6. Vilka tester skulle du skriva innan du refaktorerar numerisk klassificeringskod?

## Nästa steg

I nästa kapitel går vi från språksyntax till samtidighet och kontextöverföring. Vi tittar på **Scoped Values**, en feature som är särskilt relevant i system som OrderFlow där korrelations-ID, tenant-information och request-kontakt ofta behöver följa med genom flera lager utan att spridas som extra metodparametrar överallt.
