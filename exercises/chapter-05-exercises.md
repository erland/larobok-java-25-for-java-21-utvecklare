# Övningar till kapitel 5: Flexible Constructor Bodies

## Övning 1: Refaktorera konstruktorvalidering

Utgå från en konstruktor där validering göms i statiska hjälpfunktioner eftersom `super(...)` tidigare behövde stå först.

Mål:

- flytta enkel validering till constructor prologue
- behåll hjälpfunktioner som förbättrar återanvändning eller testbarhet
- undvik att göra prologen till affärslogik

Diskutera efteråt:

- blev konstruktorn tydligare?
- blev testbarheten bättre eller sämre?
- finns det risk att prologen växer för mycket?

## Övning 2: Korrigera felaktig prologkod

Gå igenom detta exempel:

```java
class Shipment extends AuditedEntity {
    private final String shipmentId;

    Shipment(String rawShipmentId) {
        var normalized = rawShipmentId.strip();
        System.out.println(this);
        validateShipmentId(normalized);
        this.shipmentId = normalized;
        var previous = shipmentId;

        super("shipment");
    }

    private void validateShipmentId(String value) {
        if (value.isBlank()) {
            throw new IllegalArgumentException("shipmentId must not be blank");
        }
    }
}
```

Uppgift:

1. Markera rader som använder `this` explicit eller implicit.
2. Flytta validering som kan vara statisk eller lokal.
3. Skriv en version som håller prologen kort och säker.

## Övning 3: Införandepolicy

Skriv en kort policy för när teamet får använda Flexible Constructor Bodies i OrderFlow.

Policyn ska minst ta upp:

- när featuren är tillåten
- när fabriksmetod eller builder är bättre
- hur kodgranskare ska bedöma prologens längd
- hur verktygsstöd ska verifieras i CI

## Fördjupning: Arvshierarkier

Identifiera en arvshierarki där en superklasskonstruktor anropar en metod som kan överlagras.

Bedöm:

- vilka fält i subklassen kan observeras för tidigt?
- skulle Flexible Constructor Bodies minska risken?
- är det bättre att ändra basklassens design?
- går ändringen att göra kompatibelt?
