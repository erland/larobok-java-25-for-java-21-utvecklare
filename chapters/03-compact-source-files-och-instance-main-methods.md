# Kapitel 3: Compact Source Files och Instance Main Methods

## Varför detta kapitel finns

Java 25 introducerar en viktig förändring för små Java-program: **Compact Source Files och Instance Main Methods** är finaliserade. För en erfaren Java 21-utvecklare är det lätt att avfärda detta som en nybörjarfeature, eftersom den klassiska målbilden är att göra `HelloWorld.java` mindre ceremonitung.

Det vore ett misstag.

Den här featuren är inte tänkt att ersätta vanliga klasser, paket, moduler eller applikationsstruktur. Däremot gör den Java mer användbart för små program där den gamla strukturen ofta kändes onödigt tung:

- små operationsverktyg
- diagnostikprogram
- engångsverktyg vid migrering
- demo- och workshopkod
- prototyper som senare kan växa in i vanlig Java-kod
- interna CLI-script där teamet vill stanna i Java-ekosystemet

I en Java 21-kodbas kunde teamet redan köra en enskild källkodsfil med `java SomeTool.java`, men själva filen behövde fortfarande normalt innehålla en klass och en klassisk `public static void main(String[] args)`. I Java 25 kan vissa små program uttryckas direkt som metoder och fält i en källkodsfil.

Kapitlets viktigaste poäng är inte att all kod ska bli kortare. Poängen är att teamet får ett nytt, standardiserat sätt att skriva små Java-program utan att lämna Java.

## Lärandemål

Efter kapitlet ska läsaren kunna:

- förklara skillnaden mellan en vanlig källkodsfil och en compact source file
- skriva och köra ett litet Java 25-program med `void main()`
- avgöra när en instance main method är lämplig
- förstå hur compact source files kan växa till vanliga klasser
- identifiera begränsningar och fallgropar för produktionskod
- använda featuren för små verktyg utan att skapa en parallell “Java-dialekt”

## Innan vi börjar

Från kapitel 2 har vi med oss två viktiga regler:

1. **Kompatibilitet före adoption.** Börja inte använda nya språkfeatures förrän bygg, test och runtime är stabila.
2. **Separera labb från produktionskod.** Nya språkfeatures kan testas i små verktyg eller labb innan de används i huvudkodbasen.

Det här kapitlet hör därför inte hemma i första kompatibilitetsloopen för OrderFlow. Det hör hemma i adoptionsfasen, där teamet frågar: “Finns det någon Java 25-feature som faktiskt gör vår vardag bättre?”

Vi introducerar två huvudbegrepp:

1. **Compact source file**: en källkodsfil där metoder och fält kan stå på toppnivå utan en explicit klassdeklaration. Kompilatorn behandlar dem som medlemmar i en implicit deklarerad klass.
2. **Instance main method**: en körbar `main`-metod som inte behöver vara `static`, `public` eller ta `String[] args`.

## Huvudförklaring

### Från klassisk main till instance main

Den klassiska formen ser ut så här:

```java
public class OrderTool {
    public static void main(String[] args) {
        System.out.println("Kontrollerar orderflöde...");
    }
}
```

Det här är välbekant, stabilt och fortfarande rätt form för många program. Men för ett litet verktyg är det mycket struktur innan programmet gör något.

Med Java 25 kan samma typ av enkel startpunkt skrivas med en instance main method:

```java
class OrderTool {
    void main() {
        System.out.println("Kontrollerar orderflöde...");
    }
}
```

Skillnaden är inte bara kosmetisk. `main` är nu en instansmetod. Launchern kan skapa en instans av klassen och anropa metoden. För små program betyder det att hjälpfält och hjälpmetoder kan skrivas som vanliga instansmedlemmar i stället för att allt måste vara `static`.

Exempel:

```java
class OrderTool {
    String environment = "test";

    void main() {
        printHeader();
        System.out.println("Miljö: " + environment);
    }

    void printHeader() {
        System.out.println("OrderFlow diagnostic");
    }
}
```

För en erfaren utvecklare är detta inte revolutionerande, men det tar bort en gammal friktion: små program behöver inte börja med en statisk värld som senare måste förklaras bort.

### Compact source files: ingen explicit klass

Java 25 går ett steg längre. Om programmet är litet kan klassen utelämnas helt:

```java
void main() {
    System.out.println("Kontrollerar orderflöde...");
}
```

Filen är då en **compact source file**. Den innehåller fortfarande Java-kod. Det är inte ett script i en separat syntax. Metoder måste fortfarande vara metoder, uttryck måste fortfarande stå i metodkroppar, och reglerna för Java gäller fortfarande.

Kompilatorn betraktar toppnivåmetoder och toppnivåfält som medlemmar i en implicit klass. Den klassen är ett implementeringsdetalj. Du ska inte bygga API:er som förutsätter vad den heter.

Ett mer realistiskt litet verktyg kan se ut så här:

```java
String defaultRegion = "eu-north";

void main() {
    var orderId = "A-10042";

    printHeader();
    validateOrderId(orderId);
    System.out.println("Region: " + defaultRegion);
}

void printHeader() {
    System.out.println("OrderFlow diagnostic");
}

void validateOrderId(String orderId) {
    if (!orderId.startsWith("A-")) {
        throw new IllegalArgumentException("Oväntat order-id: " + orderId);
    }
}
```

Notera vad som inte finns:

- ingen explicit `class`
- ingen `public static void main(String[] args)`
- inga `static`-modifierare för hjälpmetoderna
- inget ramverk
- ingen separat build-konfiguration för att förstå exemplet

För ett litet verktyg kan det vara exakt rätt nivå.

### Körning med source-code launcher

Compact source files fungerar väl tillsammans med source-code launcher:

```bash
java OrderCheck.java
```

Det gör att ett litet Java-program kan ligga bredvid en kodbas och köras utan att först läggas in som en full modul i bygget.

Exempel på projektstruktur under en migrationsfas:

```text
orderflow/
├── order-api/
├── order-domain/
├── order-worker/
└── java25-labs/
    └── OrderCheck.java
```

I kapitel 2 bestämde vi att labb och experiment inte ska blandas in i produktionsmodulerna utan beslut. `java25-labs/` är därför en bra plats för små Java 25-verktyg som teamet vill testa.

### `java.lang.IO`: enkel radbaserad I/O

Java 25 lägger också till `java.lang.IO`, en klass med enkla metoder för radbaserad konsol-I/O. Eftersom den ligger i `java.lang` kan den användas utan import.

Exempel:

```java
void main() {
    var orderId = IO.readln("Order-id: ");
    IO.println("Kontrollerar " + orderId);
}
```

För erfarna utvecklare ersätter detta inte `System.out`, logging, CLI-bibliotek eller robust terminalhantering i större verktyg. Det är däremot praktiskt för mycket små interaktiva program, demos och undervisningskod.

En viktig detalj: metoderna i `IO` anropas med klassnamnet, till exempel `IO.println(...)`. De är inte magiskt importerade som fria funktioner.

### Automatiska imports i compact source files

I vanliga Java-filer är bara `java.lang` implicit importerat. Compact source files har en extra förenkling: publika toppnivåtyper från paket som exporteras av `java.base` är tillgängliga som om modulen importerats på begäran.

Det betyder att små program kan använda exempelvis `List`, `Map`, `Path` och `BigDecimal` utan att börja med en rad imports.

```java
void main() {
    var orderIds = List.of("A-10042", "A-10043", "B-20001");

    for (var id : orderIds) {
        IO.println(id + " -> " + classify(id));
    }
}

String classify(String orderId) {
    return orderId.startsWith("A-") ? "standard" : "special";
}
```

Det här är bekvämt, men också något att vara uppmärksam på. När ett compact source file växer till en vanlig klass kan du behöva lägga till explicita imports eller använda modulimporter. Vi kommer tillbaka till modulimporter i nästa kapitel.

### Vad compact source files inte är

Det är viktigt att vara strikt här. Compact source files är inte:

- ett nytt script-språk
- ersättning för paketstruktur
- ersättning för moduler
- en rekommendation att skriva produktionslogik utan klasser
- ett sätt att skriva top-level statements direkt utanför metoder
- en anledning att hoppa över testbar design

Koden är fortfarande Java. Skillnaden är att viss struktur kan vara implicit när programmet är litet.

Det är därför bättre att tänka på featuren som en **on-ramp** än som en genväg. Den hjälper små program att börja enkelt, men den ska kunna växa in i vanlig Java när programmet inte längre är litet.

## Exempel: ett litet operationsverktyg för OrderFlow

Anta att OrderFlow-teamet ofta behöver kontrollera en lista med order-id:n under migreringen. De vill inte skapa en ny modul, men de vill använda Java-typer, samma JDK och samma vana språk.

De börjar med ett litet verktyg:

```java
void main() {
    var orderIds = List.of("A-10042", "A-10043", "B-20001", "A-10044");

    IO.println("OrderFlow order check");
    IO.println("---------------------");

    for (var id : orderIds) {
        IO.println(id + " -> " + statusFor(id));
    }
}

String statusFor(String orderId) {
    if (!orderId.matches("[A-Z]-\\d+")) {
        return "invalid";
    }

    if (orderId.startsWith("A-")) {
        return "standard";
    }

    return "manual-review";
}
```

Filen kan heta `OrderCheck.java` och köras så här:

```bash
java OrderCheck.java
```

För ett engångsverktyg är detta fullt tillräckligt.

### När verktyget växer

Efter några dagar vill teamet läsa order-id:n från en fil. Programmet börjar få fler metoder:

```java
void main() throws Exception {
    var path = Path.of("orders.txt");

    for (var line : Files.readAllLines(path)) {
        var orderId = line.trim();

        if (!orderId.isBlank()) {
            IO.println(orderId + " -> " + statusFor(orderId));
        }
    }
}

String statusFor(String orderId) {
    if (!orderId.matches("[A-Z]-\\d+")) {
        return "invalid";
    }

    return orderId.startsWith("A-") ? "standard" : "manual-review";
}
```

Det är fortfarande rimligt som ett litet verktyg. Men om programmet får konfiguration, flera kommandon, testfall, logging och felhantering bör teamet flytta det till vanlig struktur.

En möjlig nästa form:

```java
import module java.base;

class OrderCheck {
    void main() throws Exception {
        var path = Path.of("orders.txt");

        for (var line : Files.readAllLines(path)) {
            var orderId = line.trim();

            if (!orderId.isBlank()) {
                IO.println(orderId + " -> " + statusFor(orderId));
            }
        }
    }

    String statusFor(String orderId) {
        if (!orderId.matches("[A-Z]-\\d+")) {
            return "invalid";
        }

        return orderId.startsWith("A-") ? "standard" : "manual-review";
    }
}
```

Här är programmet fortfarande litet, men klassen är nu explicit. Det gör det lättare att fortsätta växa mot en vanlig modul eller ett testbart verktyg.

Nästa kapitel går djupare in i just `import module`.

## Designriktlinjer för erfarna utvecklare

### Använd compact source files för rätt sorts kod

Bra kandidater:

- små diagnostikverktyg
- reproducerbara felcase
- demoexempel
- migreringshjälpmedel
- workshopkod
- korta integrationsprov mot API:er
- engångsprogram som ändå bör vara versionshanterade

Dåliga kandidater:

- domänmodell
- långlivad applikationskod
- publika bibliotek
- kod som ska exponera API
- kod som kräver tydlig paketstruktur
- kod där teamet behöver konventionell teststruktur
- kod där flera utvecklare ska bygga vidare under lång tid

En enkel tumregel:

> Om filen börjar behöva arkitektur, gör arkitekturen explicit.

### Undvik att skapa en “script-kultur” vid sidan av kodbasen

Compact source files gör det lättare att skriva små program. Det betyder inte att teamet ska börja samla otestade lokala filer som ingen äger.

För OrderFlow kan teamet använda följande regler:

- Labbverktyg ligger i `java25-labs/`.
- Varje verktyg har en kort kommentar om syfte.
- Verktyg som används fler än ett fåtal gånger får en ägare.
- Verktyg som påverkar produktion flyttas till vanlig modul.
- Verktyg som hanterar känsliga data granskas som annan kod.

Exempel på toppkommentar:

```java
// Syfte: Snabb kontroll av order-id-format under Java 25-migreringen.
// Ägare: OrderFlow-plattformsteamet.
// Status: Labbverktyg, inte produktionskod.

void main() {
    IO.println("OrderFlow diagnostic");
}
```

Det är inte en Java-regel. Det är en teamregel. Men den gör featuren säkrare att använda i en erfaren organisation.

### Var försiktig med implicithet

Erfarna utvecklare är ofta skeptiska till implicithet, med goda skäl. Compact source files gör flera saker implicita:

- klassdeklarationen
- den genererade klassens namn
- delar av importmiljön
- startpunktens klassiska form

Det är acceptabelt när programmet är litet och lokalt. Det blir mindre acceptabelt när programmet blir centralt, långlivat eller beroende av tydliga API-gränser.

Det betyder inte att featuren är “bara för nybörjare”. Det betyder att den har ett tydligt användningsområde.

## Vanliga misstag

### Misstag 1: Att använda compact source files i produktionsdomänen

**Varför det händer:**  
Teamet gillar den kortare formen och börjar använda den även där kod borde ha tydlig struktur.

**Hur man undviker det:**  
Sätt en regel: compact source files är för små verktyg, demos och labb. Produktionsdomän, API:er och långlivade komponenter skrivs som vanliga klasser.

### Misstag 2: Att glömma att `main` fortfarande är en metod

**Varför det händer:**  
Den korta formen kan se ut som scripting, men Java tillåter inte godtyckliga statements på toppnivå.

Fel:

```java
IO.println("Startar"); // Inte rätt som toppnivåstatement

void main() {
    IO.println("Kör");
}
```

Rätt:

```java
void main() {
    IO.println("Startar");
    IO.println("Kör");
}
```

**Hur man undviker det:**  
Tänk: toppnivå i compact source files är för fält och metoder, inte för körbara statements.

### Misstag 3: Att förlita sig på den implicita klassens namn

**Varför det händer:**  
Kompilatorn måste generera någon form av klass, och det kan locka utvecklare att betrakta den som en stabil detalj.

**Hur man undviker det:**  
Referera inte till den implicita klassen som API. När du behöver ett namn, skriv en explicit klass.

### Misstag 4: Att missa gränsen mellan labb och standard

**Varför det händer:**  
Kapitlet kommer efter två migrationskapitel, och teamet kan vilja börja använda Java 25-syntax direkt.

**Hur man undviker det:**  
Behåll ordningen:

1. bygg och testa på Java 25
2. stabilisera runtime-baslinjen
3. experimentera i labb
4. adoptera features i produktionskod först efter beslut

### Misstag 5: Att tro att `IO` ersätter logging

**Varför det händer:**  
`IO.println` är smidigt och tydligt i små program.

**Hur man undviker det:**  
Använd `IO` för små interaktiva program och exempel. Använd etablerad logging och felhantering i applikationskod.

## Övningar

### Övning 1: Skriv om ett litet verktyg

Utgå från detta Java 21-liknande program:

```java
public class OrderPrefixCheck {
    public static void main(String[] args) {
        for (String id : java.util.List.of("A-1", "B-2", "A-3")) {
            System.out.println(id + " -> " + id.startsWith("A-"));
        }
    }
}
```

Skriv om det som en compact source file för Java 25.

Mål:

- ingen explicit klass
- `void main()`
- använd `var` där det förbättrar läsbarheten
- använd gärna `IO.println`

### Övning 2: Gör verktyget lite mer realistiskt

Bygg vidare på din lösning:

- flytta klassificeringen till en separat metod
- returnera `"standard"` för `A-`
- returnera `"manual-review"` för andra giltiga prefix
- returnera `"invalid"` för tomma eller felaktiga id:n

### Övning 3: Bestäm när verktyget ska växa upp

Skriv tre kriterier för när ditt compact source file bör flyttas till en vanlig klass eller modul.

Exempel på kriterier:

- fler än en utvecklare ändrar filen regelbundet
- verktyget används i CI
- verktyget behöver tester
- verktyget hanterar produktionsdata
- verktyget får flera kommandon eller konfigurationslägen

### Fördjupning

Ta ett verkligt litet internt Java-verktyg från din organisation. Klassificera det:

| Fråga | Svar |
|---|---|
| Skulle compact source file göra verktyget tydligare? |  |
| Är verktyget kortlivat eller långlivat? |  |
| Behöver det paketstruktur? |  |
| Behöver det tester? |  |
| Hanterar det känsliga data? |  |
| Bör det ligga i `java25-labs/`, en riktig modul eller inte versionshanteras alls? |  |

## Snabb sammanfattning

- Java 25 finaliserar Compact Source Files och Instance Main Methods.
- En instance main method behöver inte vara `public static void main(String[] args)`.
- En compact source file kan innehålla toppnivåfält och toppnivåmetoder utan explicit klass.
- Koden är fortfarande Java, inte ett separat scriptspråk.
- `java.lang.IO` ger enkel radbaserad konsol-I/O.
- Compact source files passar bäst för små verktyg, demos, labb och prototyper.
- När kod behöver API, paket, tester, ägarskap eller lång livslängd bör den växa till vanlig Java-struktur.

## Quiz/reflektionsfrågor

1. Vad är den viktigaste skillnaden mellan en vanlig klass med instance main och en compact source file?
2. Varför är det fel att beskriva compact source files som ett separat Java-scriptspråk?
3. Vilka typer från JDK:n blir enklare att använda i compact source files tack vare automatiska imports?
4. Varför bör du inte förlita dig på namnet på den implicita klassen?
5. När skulle du välja att skriva en explicit klass trots att compact source file fungerar tekniskt?
6. Hur kan ett team hindra små Java 25-verktyg från att bli oägd produktionskritisk kod?

## Nästa steg

Det här kapitlet visade hur små Java-program kan bli mindre ceremonityngda i Java 25. Vi använde också en första glimt av modulimport:

```java
import module java.base;
```

I nästa kapitel går vi vidare till **Module Import Declarations**. Där undersöker vi hur modulimporter fungerar, varför de är särskilt relevanta tillsammans med compact source files och vilka risker som uppstår när imports blir bredare och mer implicita.
