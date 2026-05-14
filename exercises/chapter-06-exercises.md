# Övningar till kapitel 6: Primitive Types i patterns, instanceof och switch

## Övning 1: Kandidatinventering

Hitta tre metoder i en befintlig Java 21-kodbas som klassificerar numeriska värden. För varje metod, dokumentera:

- vilken primitiv typ som används
- vilka specialfall som finns
- vilka intervall som finns
- om wrapper-typer används i onödan
- om en Java 25-preview-version skulle bli tydligare

## Övning 2: Refaktorera i labbgren

Utgå från följande kod:

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

Skriv en experimentell version med Java 25-preview-syntax. Kompilera med:

```bash
javac --release 25 --enable-preview StockDeltaClassifier.java
java --enable-preview StockDeltaClassifier
```

Svara sedan:

1. Blev koden tydligare?
2. Vilka gränsvärden måste testas?
3. Skulle du acceptera denna refaktorering i produktionskod idag?

## Övning 3: Preview-policy

Skriv en kort teamregel för preview-features. Policyn ska besvara:

- Var får preview-kod ligga?
- Vem får godkänna den?
- Hur syns den i CI?
- När ska den tas bort, uppdateras eller göras till produktionskod?

## Reflektionsfrågor

1. Var går gränsen mellan tydlig numerisk klassificering och för mycket språkakrobatik?
2. När bör `Money`, `Quantity` eller andra domäntyper användas i stället för råa primitiva värden?
3. Hur påverkar preview-status bibliotek, API:er och långsiktigt underhåll?
