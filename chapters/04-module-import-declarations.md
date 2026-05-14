# Kapitel 4: Module Import Declarations

## Varför detta kapitel finns

Java-utvecklare har länge haft två ganska olika importstilar:

- explicita imports, till exempel `import java.util.List;`
- package-on-demand imports, till exempel `import java.util.*;`

I stora produktionskodbaser är explicita imports ofta att föredra eftersom de gör beroenden lokala och tydliga. I små verktyg, demos, labb och explorativ kod kan importsektionen däremot bli oproportionerligt stor jämfört med själva programmet.

Java 25 lägger till en tredje möjlighet: **module import declarations**.

I stället för att importera ett paket i taget kan en källkodsfil importera alla publika toppnivåtyper som exporteras av en modul:

```java
import module java.base;
```

Det här kapitlet handlar inte om att ersätta alla imports i OrderFlow med modulimports. Det vore nästan alltid fel mål. Kapitlet handlar om att förstå var featuren hjälper, var den kan skapa otydlighet och hur den passar ihop med compact source files från kapitel 3.

## Lärandemål

Efter kapitlet ska läsaren kunna:

- förklara vad `import module M;` betyder
- skilja mellan modulimport, paketimport och explicit typimport
- använda `import module java.base;` i ett litet Java 25-program
- förstå hur transitive module dependencies påverkar vilka typer som importeras
- identifiera och lösa namnkonflikter som `List`, `Date` eller `Label`
- välja en rimlig importpolicy för produktionskod, labbkod och undervisningskod

## Innan vi börjar

Från kapitel 3 har vi med oss att en **compact source file** automatiskt beter sig som om `import module java.base;` fanns i början av filen. Det är därför exempel med compact source files kan använda typer som `List`, `Map`, `Stream`, `Path` och `Files` utan att först skriva importerna.

I vanliga klasser gäller inte samma automatiska import. Där importeras fortfarande `java.lang.*` automatiskt, men inte resten av `java.base`.

Det här kapitlet introducerar två huvudbegrepp:

1. **Module import declaration**: en importdeklaration på formen `import module M;`.
2. **Exported package**: ett paket som en modul gör tillgängligt för andra moduler.

## Huvudförklaring

### Vad importeras egentligen?

En module import declaration har formen:

```java
import module java.base;
```

Den betyder: importera, on-demand, publika toppnivåklasser och interfaces från paket som modulen exporterar till den aktuella modulen.

För en vanlig källkodsfil kan det se ut så här:

```java
import module java.base;

class OrderSummary {
    Map<String, Long> countByStatus(List<String> statuses) {
        return statuses.stream()
                .collect(Collectors.groupingBy(
                        Function.identity(),
                        Collectors.counting()));
    }
}
```

I Java 21 hade samma kod krävt flera imports:

```java
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

class OrderSummary {
    Map<String, Long> countByStatus(List<String> statuses) {
        return statuses.stream()
                .collect(Collectors.groupingBy(
                        Function.identity(),
                        Collectors.counting()));
    }
}
```

Den nya varianten är kortare, men den är också mindre exakt. Läsaren ser att filen använder typer från `java.base`, men inte vilka paket eller typer som faktiskt används förrän de dyker upp i koden.

Det är därför featuren bör behandlas som ett verktyg för rätt sammanhang, inte som en generell kodstil.

### `java.base` som vardagsmodul

`java.base` är särskilt viktig eftersom den innehåller många av de API:er som nästan all Java-kod använder:

- `java.lang`
- `java.util`
- `java.util.stream`
- `java.util.function`
- `java.nio.file`
- `java.time`
- `java.net`
- `java.io`
- flera andra grundläggande paket

Med `import module java.base;` får du en bred import av sådant som ofta behövs i små program.

Exempel: ett litet analysverktyg för OrderFlow-loggar.

```java
import module java.base;

class OrderLogSummary {
    Map<String, Long> summarize(Path logFile) throws IOException {
        try (var lines = Files.lines(logFile)) {
            return lines
                    .filter(line -> line.startsWith("status="))
                    .map(line -> line.substring("status=".length()))
                    .collect(Collectors.groupingBy(
                            Function.identity(),
                            TreeMap::new,
                            Collectors.counting()));
        }
    }
}
```

Här kommer flera typer från olika paket i `java.base`:

- `Path` och `Files` från `java.nio.file`
- `IOException` från `java.io`
- `Map` och `TreeMap` från `java.util`
- `Function` från `java.util.function`
- `Collectors` från `java.util.stream`

I en liten fil kan `import module java.base;` göra koden lättare att läsa eftersom importblocket inte tar över. I en större produktionsklass kan samma import göra det svårare att se exakt vilka API:er klassen använder.

### Kopplingen till compact source files

I kapitel 3 skrev vi små program utan explicit klass. Sådana compact source files får automatiskt åtkomst till publika typer från exporterade paket i `java.base`, ungefär som om filen började med:

```java
import module java.base;
```

Det betyder att detta kan vara en komplett compact source file:

```java
void main() throws IOException {
    var logFile = Path.of("orders.log");

    try (var lines = Files.lines(logFile)) {
        var countByStatus = lines
                .filter(line -> line.startsWith("status="))
                .map(line -> line.substring("status=".length()))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        countByStatus.forEach((status, count) ->
                IO.println(status + ": " + count));
    }
}
```

För små diagnostikverktyg är detta kraftfullt. Teamet kan skriva kort Java-kod som fortfarande använder vanliga JDK-API:er.

Men regeln från kapitel 3 gäller fortfarande: när ett verktyg växer, får tester, blir delat mellan team eller börjar bli affärskritiskt bör det normalt flyttas till vanlig projektstruktur med explicita klasser och medveten importstil.

### Transitive dependencies: importen kan nå längre än du tror

Modulimports tar hänsyn till modulläsbarhet. Om en modul exporterar paket och dessutom läser andra moduler transitivt, kan en module import declaration även importera publika typer från indirekt tillgängliga exporterade paket.

Ett typiskt exempel är `java.sql`.

```java
import module java.sql;

class SqlXmlExample {
    void inspect(SQLXML xml) throws SQLException {
        Source source = xml.getSource(Source.class);
        System.out.println(source);
    }
}
```

Här kommer `SQLXML` och `SQLException` från `java.sql`, medan `Source` kommer från `javax.xml.transform`, som finns i `java.xml`. Poängen är att `java.sql`-API:t refererar till XML-typer, och modulrelationen gör att importen kan följa den kopplingen.

Det är bekvämt när man utforskar ett API, men det gör också importens effekt mindre lokal än en vanlig `import java.sql.SQLXML;`.

För produktionskod bör du därför fråga:

- Är modulimporten tydligare än explicita imports här?
- Förstår läsaren varför den indirekta typen finns i scope?
- Är det här labbkod, dokumentationskod eller långlivad applikationskod?

### Namnkonflikter

Den största praktiska fallgropen är inte prestanda. Importer påverkar kompileringens namnupplösning, inte runtime-prestanda på ett sätt som normalt är relevant.

Den största fallgropen är **ambiguous simple names**.

Exempel:

```java
import module java.base;
import module java.desktop;

class AmbiguousExample {
    List list;
}
```

Det här är problematiskt eftersom `List` kan syfta på mer än en typ, till exempel `java.util.List` eller `java.awt.List`.

Liknande problem kan uppstå med `Date`:

```java
import module java.base;
import module java.sql;

class DateExample {
    Date date; // tvetydigt: java.util.Date eller java.sql.Date?
}
```

Lösningen är att vara mer specifik:

```java
import module java.base;
import module java.sql;
import java.sql.Date;

class DateExample {
    Date date;
}
```

En explicit typimport är mer specifik och vinner över mindre specifika module imports.

För en erfaren utvecklare är detta samma grundprincip som med vanliga wildcard-imports: ju bredare import, desto större risk att enkla namn blir otydliga.

## Exempel: OrderFlow-analysverktyg

OrderFlow-teamet har ett litet verktyg som läser en enkel textfil med orderstatusar och skriver ut en summering. Verktyget ligger i `java25-labs/` och är inte en del av produktionens runtime.

Fil: `OrderStatusReport.java`

```java
import module java.base;

class OrderStatusReport {
    void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: java OrderStatusReport.java <status-file>");
            return;
        }

        var statusFile = Path.of(args[0]);

        try (var lines = Files.lines(statusFile)) {
            var countByStatus = lines
                    .map(String::strip)
                    .filter(line -> !line.isBlank())
                    .collect(Collectors.groupingBy(
                            Function.identity(),
                            TreeMap::new,
                            Collectors.counting()));

            countByStatus.forEach((status, count) ->
                    System.out.println(status + ": " + count));
        }
    }
}
```

Körning som single-file source-code program:

```bash
java OrderStatusReport.java order-status.txt
```

Exempeldata:

```text
CREATED
PAID
CREATED
SHIPPED
PAID
PAID
```

Förväntad utskrift:

```text
CREATED: 2
PAID: 3
SHIPPED: 1
```

I den här typen av verktyg är `import module java.base;` rimligt:

- filen är kort
- koden är lokal och lätt att överblicka
- alla använda typer kommer från JDK:ns basmodul
- verktyget är explorativt och inte en del av kärnapplikationens API

Om samma kod flyttas in i `order-worker` som långlivad produktionskod kan teamet välja att ersätta modulimporten med explicita imports.

## Importpolicy för OrderFlow

En praktisk policy kan se ut så här:

### Produktionskod

Använd normalt explicita typimports.

```java
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
```

Motivering: långlivad kod läses oftare än den skrivs. Importsektionen fungerar som lokal dokumentation.

### Labbkod och migreringsverktyg

Tillåt `import module java.base;` när filen är liten och syftet är tydligt.

```java
import module java.base;
```

Motivering: explorativ kod vinner ibland mer på låg friktion än på maximal importprecision.

### Dokumentation, workshops och utbildning

Tillåt modulimports när importsektionen annars skymmer poängen med exemplet.

Motivering: pedagogisk kod ska fokusera på det begrepp som kapitlet lär ut.

### Blandade JDK-moduler

Var försiktig med flera breda modulimports i samma fil, särskilt om modulerna innehåller klassnamn som ofta kolliderar.

```java
import module java.base;
import module java.desktop;
```

Här bör du nästan alltid komplettera med explicita imports eller undvika kombinationen.

## Vanliga misstag

- Misstag: Att behandla `import module` som en ersättning för JPMS-modulering.
  - Varför det händer: Syntaxen använder ordet `module`.
  - Hur man undviker det: Kom ihåg att detta är en källkodsimport. Den modulariserar inte din applikation och skapar inte en `module-info.java`.

- Misstag: Att införa `import module java.base;` i hela produktionskodbasen.
  - Varför det händer: Featuren ser ut som en enkel förenkling.
  - Hur man undviker det: Sätt en importpolicy. Använd featuren där den gör koden tydligare, inte bara kortare.

- Misstag: Att ignorera namnkonflikter.
  - Varför det händer: Konflikter syns först när ett enkelt namn faktiskt används.
  - Hur man undviker det: Var extra försiktig med moduler som `java.desktop` och med namn som `List`, `Date`, `Label` och `Element`.

- Misstag: Att tro att module imports importerar från class path.
  - Varför det händer: Package imports kan användas för paket som finns på class path.
  - Hur man undviker det: `import module M;` använder modulnamn. För bibliotek utan tydlig modul behöver du vanlig importstil.

- Misstag: Att låta labbkodens importstil smitta av sig utan beslut.
  - Varför det händer: Kod kopieras från små exempel till riktig kod.
  - Hur man undviker det: När ett verktyg lyfts till produktion ska importstilen granskas som en del av refaktoreringen.

## Övningar

### Övning 1: Jämför importstilar

Ta följande klass:

```java
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

class StatusCounter {
    Map<String, Long> count(Path file) throws IOException {
        try (var lines = Files.lines(file)) {
            return lines
                    .map(String::strip)
                    .filter(line -> !line.isBlank())
                    .collect(Collectors.groupingBy(
                            Function.identity(),
                            TreeMap::new,
                            Collectors.counting()));
        }
    }
}
```

Skriv om importerna med `import module java.base;`.

Reflektera sedan:

1. Blev filen mer läsbar?
2. Försvann information som var användbar?
3. Skulle du acceptera den här importstilen i produktionskod?

### Övning 2: Hitta en namnkonflikt

Skapa ett litet exempel som använder både `java.util.List` och `java.awt.List`.

Testa först med:

```java
import module java.base;
import module java.desktop;
```

Lös sedan konflikten med explicita imports eller fullt kvalificerade namn.

### Övning 3: Skriv en importpolicy

Formulera tre regler för ett team som migrerar från Java 21 till Java 25:

1. När är `import module java.base;` tillåtet?
2. När ska explicita imports krävas?
3. Hur ska kodgranskning hantera modulimports?

### Fördjupning

Undersök vad som händer om du använder:

```java
import module java.se;
```

i en vanlig fil som körs från class path. Vilka extra kompilator- eller runtime-val behövs? Varför är `java.se` annorlunda än `java.base` i det här sammanhanget?

## Snabb sammanfattning

- Java 25 har module import declarations som final feature.
- Syntaxen är `import module M;`.
- En modulimport importerar publika toppnivåtyper från paket som modulen exporterar till den aktuella modulen.
- `import module java.base;` kan ersätta många vanliga imports i små program.
- Compact source files får i praktiken `java.base` importerad automatiskt.
- Modulimports är praktiska i labb, demos och små verktyg men bör användas med policy i produktionskod.
- Bredare imports ökar risken för tvetydiga enkla namn.
- Explicita imports eller package imports kan användas för att lösa namnkonflikter.

## Quiz/reflektionsfrågor

1. Vad är skillnaden mellan `import java.util.*;` och `import module java.base;`?
2. Varför är `import module` inte samma sak som att skapa en JPMS-modul?
3. När kan `import module java.base;` göra en fil mer läsbar?
4. När kan samma import göra en fil mindre läsbar?
5. Hur löser du en konflikt mellan `java.util.Date` och `java.sql.Date`?
6. Varför är module imports särskilt relevanta tillsammans med compact source files?
7. Vilken importpolicy skulle du rekommendera för långlivad produktionskod?

## Nästa steg

I nästa kapitel går vi vidare till **Flexible Constructor Bodies**. Där lämnar vi importerna och går in i ett mer klassiskt domänmodelleringsproblem: hur vi kan göra validering och beräkningar före ett konstruktoranrop utan att kompromissa med säker initiering.
