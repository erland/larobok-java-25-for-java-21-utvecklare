# Övningar till kapitel 4: Module Import Declarations

## Övning 1: Byt till modulimport

Utgå från följande importblock:

```java
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
```

Ersätt det med:

```java
import module java.base;
```

Besvara:

1. Vilka typer används fortfarande med enkla namn?
2. Är det lättare eller svårare att se vilka paket klassen använder?
3. Skulle du välja modulimporten i en produktionsklass?

## Övning 2: Namnkonflikt

Skapa en fil som använder både:

- `java.util.List`
- `java.awt.List`

Testa med:

```java
import module java.base;
import module java.desktop;
```

Dokumentera kompilatorfelet och lös det med antingen:

- explicit typimport
- package import
- fullt kvalificerat typnamn

## Övning 3: Teamregel

Skriv en kort importpolicy för ett Java 25-team.

Policyn ska täcka:

- produktionskod
- labb- och migreringsverktyg
- dokumentations- och utbildningsexempel
- hur kodgranskare ska hantera breda modulimports

## Fördjupning: `java.sql`

Skriv ett litet exempel med:

```java
import module java.sql;
```

Undersök vilka typer som blir tillgängliga från `java.sql` och vilka som kommer från indirekt relaterade moduler.

Reflektera över om detta gör koden tydligare eller mer överraskande.
