# Övningar till Kapitel 7: Scoped Values i praktiken

## Övning 1: Ersätt ThreadLocal

Skriv om denna klass till en Scoped Values-baserad lösning:

```java
final class Correlation {
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    static void set(String id) {
        CURRENT.set(id);
    }

    static String get() {
        return CURRENT.get();
    }

    static void clear() {
        CURRENT.remove();
    }
}
```

Krav:

- använd `ScopedValue<String>`
- skapa en metod som kör en operation med ett bundet korrelations-ID
- anropande kod ska inte behöva anropa `clear()`
- frånvaro ska ge ett tydligt felmeddelande

## Övning 2: RequestContext som record

Skapa:

```java
record RequestContext(String correlationId, String tenant, String userId) {
}
```

Använd recordet i ett litet OrderFlow-flöde:

- `validateOrder(...)`
- `reserveInventory(...)`
- `writeAuditEvent(...)`

Avgör vilka värden som ska vara vanliga parametrar och vilka som kan vara context.

## Övning 3: Rebinding

Skapa ett yttre scope med användaren `erland` och ett inre scope med användaren `system-job`.

Logga användaren:

1. före det inre scopet
2. inne i det inre scopet
3. efter det inre scopet

Förväntad observation: det inre scopet ändrar inte det yttre scopets bindning.

## Fördjupning: Teamregel

Skriv en kort policy för Scoped Values i ett utvecklingsteam. Policyn ska svara på:

- Vilken data får bindas?
- Vilken data får inte bindas?
- Ska `ScopedValue`-nycklar vara `private`, package-private eller publika?
- Ska kod använda `get()`, `orElse(...)` eller `orElseThrow(...)`?
- Hur ska kodgranskning hitta missbruk?
