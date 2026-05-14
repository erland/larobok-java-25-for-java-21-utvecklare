# Övningar till kapitel 3: Compact Source Files och Instance Main Methods

## Övning 1: Skriv om ett Java 21-verktyg

Utgångspunkt:

```java
public class OrderPrefixCheck {
    public static void main(String[] args) {
        for (String id : java.util.List.of("A-1", "B-2", "A-3")) {
            System.out.println(id + " -> " + id.startsWith("A-"));
        }
    }
}
```

Skriv om programmet som en Java 25 compact source file.

Krav:

- ingen explicit klass
- `void main()`
- ingen `static` hjälpmetod
- använd gärna `IO.println`

## Övning 2: Lägg till klassificering

Utöka lösningen med en separat metod:

```java
String statusFor(String orderId) {
    // din kod här
}
```

Regler:

- `"standard"` för id:n som börjar med `A-`
- `"manual-review"` för andra id:n med formatet bokstav-bindestreck-siffror
- `"invalid"` för tomma eller felaktiga id:n

## Övning 3: Bestäm gränsen för när verktyget ska bli en vanlig klass

Fyll i tabellen.

| Signal | Flytta till vanlig klass/modul? | Varför? |
|---|---|---|
| Verktyget körs manuellt en gång under migrering |  |  |
| Verktyget används i CI |  |  |
| Verktyget hanterar produktionsdata |  |  |
| Verktyget får flera kommandon |  |  |
| Verktyget behöver enhetstester |  |  |

## Fördjupning: teamregel för compact source files

Skriv en kort teamregel för hur compact source files får användas i ett Java 25-projekt.

Regeln bör täcka:

- var filerna får ligga
- vem som äger dem
- när de får användas i CI
- när de måste flyttas till vanlig modul
- hur känsliga data hanteras

## Reflektion

1. Vilket litet verktyg i din nuvarande Java-miljö skulle vinna mest på compact source files?
2. Vilken implicithet i featuren tycker du är mest riskabel?
3. Skulle du tillåta compact source files i produktionsrepo? Under vilka villkor?
