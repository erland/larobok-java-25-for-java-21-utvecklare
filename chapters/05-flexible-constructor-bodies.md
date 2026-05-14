# Kapitel 5: Flexible Constructor Bodies

## Varför detta kapitel finns

I Java 21 fanns en regel som nästan alla erfarna Java-utvecklare hade internaliserat: om en konstruktor anropar `super(...)` eller `this(...)` måste det anropet stå först i konstruktorn.

Regeln var enkel, men den skapade ofta onödig friktion. Den gjorde det svårt att skriva konstruktorer som:

- validerar argument innan en dyr eller riskabel superklasskonstruktor körs
- beräknar mellanvärden som behövs för `super(...)`
- förbereder ett värde som används på flera ställen i konstruktoranropet
- initierar subklassens egna fält innan superklassens konstruktor potentiellt låter objektet synas för annan kod

I Java 25 finaliseras **Flexible Constructor Bodies**. Det betyder att konstruktorkroppar får en mer flexibel modell: viss kod kan stå före ett explicit `super(...)`- eller `this(...)`-anrop.

Det här är inte en uppmaning att skriva mer avancerade konstruktorer än nödvändigt. Det är snarare en möjlighet att uttrycka sådant som många team redan behövde göra, men som tidigare krävde statiska hjälpfunktioner, fabriksmetoder eller mindre naturlig kodstruktur.

I OrderFlow använder vi featuren för två saker:

1. fail-fast-validering av domänvärden
2. säkrare initiering när arv redan finns i modellen

## Lärandemål

Efter kapitlet ska läsaren kunna:

- förklara vad Java 25 ändrar i konstruktormodellen
- skilja mellan constructor prologue och constructor epilogue
- skriva validering och säkra beräkningar före `super(...)`
- förstå vad en early construction context tillåter och förbjuder
- avgöra när featuren förbättrar läsbarhet och när en fabriksmetod fortfarande är bättre
- identifiera risker med arv, överlagrade metoder och halvinitierat tillstånd

## Innan vi börjar

Från tidigare kapitel har vi etablerat två viktiga principer:

- kompatibilitet före adoption
- nya språkfeatures ska införas där de ger tydlig nytta

**Flexible Constructor Bodies** är en final språkfeature i Java 25. Det gör den annorlunda än preview-features som kräver `--enable-preview`. Men en final feature kan fortfarande kräva att byggverktyg, IDE, kodformatterare, statiska analysverktyg och style checks hänger med.

Första steget i en migration är därför inte att skriva om alla konstruktorer. Första steget är att förstå var den gamla konstruktorregeln har tvingat fram sämre kod.

## Huvudförklaring

### Problemet i Java 21

Anta att OrderFlow har en domänklass `Order` som ärver från en intern basklass `AuditedEntity`.

```java
abstract class AuditedEntity {
    private final String entityType;

    protected AuditedEntity(String entityType) {
        this.entityType = entityType;
        onConstructionEvent();
    }

    protected void onConstructionEvent() {
        // Standardbeteende.
    }

    public String entityType() {
        return entityType;
    }
}
```

Basklassen är inte idealisk. Den anropar en metod från konstruktorn. Det är en känd designrisk, särskilt om metoden kan överlagras i subklasser.

I en perfekt värld hade vi kanske ändrat basklassen. I verkliga system finns ofta äldre arvshierarkier, bibliotekskod eller intern plattformskod som inte kan ändras direkt.

Nu vill vi skapa en `Order`:

```java
final class Order extends AuditedEntity {
    private final String orderId;
    private final Money total;

    Order(String rawOrderId, Money total) {
        super("order");

        if (rawOrderId == null || rawOrderId.isBlank()) {
            throw new IllegalArgumentException("orderId must not be blank");
        }

        this.orderId = rawOrderId.strip();
        this.total = Objects.requireNonNull(total);
    }

    @Override
    protected void onConstructionEvent() {
        System.out.println("Creating order " + orderId);
    }
}
```

I Java 21 är `super("order")` tvunget att ligga först. Det betyder att `AuditedEntity` kan anropa `onConstructionEvent()` innan `Order` har hunnit sätta `orderId` och `total`.

Det är exakt den typ av halvinitierat tillstånd som erfarna Java-utvecklare brukar vilja undvika.

### Java 25-modellen: prologue och epilogue

Java 25 delar upp konstruktorkroppen i två delar:

- **constructor prologue**: kod före ett explicit `super(...)` eller `this(...)`
- **constructor epilogue**: kod efter det explicita konstruktoranropet

Exempel:

```java
Order(String rawOrderId, Money total) {
    // prologue
    if (rawOrderId == null || rawOrderId.isBlank()) {
        throw new IllegalArgumentException("orderId must not be blank");
    }

    this.orderId = rawOrderId.strip();
    this.total = Objects.requireNonNull(total);

    super("order");

    // epilogue
}
```

Det viktiga är inte bara att koden får stå före `super(...)`. Det viktiga är att Java fortfarande skyddar objektet under konstruktion.

Kod i prologen körs i en **early construction context**. Den får göra säkra saker, men den får inte använda objektet som om det redan vore färdigkonstruerat.

### Vad prologen får göra

I praktiken får prologen användas för sådant som inte kräver ett färdigt `this`.

Typiska tillåtna användningar:

```java
Order(String rawOrderId, Money total) {
    if (rawOrderId == null || rawOrderId.isBlank()) {
        throw new IllegalArgumentException("orderId must not be blank");
    }

    var normalizedId = rawOrderId.strip();
    var checkedTotal = Objects.requireNonNull(total);

    super("order");

    this.orderId = normalizedId;
    this.total = checkedTotal;
}
```

Det här är användbart när du vill förbereda argument till `super(...)`.

Prologen får också kasta undantag:

```java
CustomerOrder(String customerId, Money total) {
    if (customerId == null || customerId.isBlank()) {
        throw new IllegalArgumentException("customerId must not be blank");
    }

    super("customer-order");
}
```

Fail-fast blir tydligare än i Java 21, där valideringen ofta fick gömmas i en statisk hjälpfunktion:

```java
CustomerOrder(String customerId, Money total) {
    super("customer-order", requireCustomerId(customerId), requireTotal(total));
}
```

Det gamla mönstret fungerar fortfarande. Skillnaden är att Java 25 gör det möjligt att skriva enkel validering där läsaren förväntar sig att hitta den.

### Vad prologen inte får göra

Prologen får inte behandla objektet som färdigkonstruerat.

Det här är fel:

```java
Order(String rawOrderId, Money total) {
    System.out.println(this);      // fel: använder this
    validate();                    // fel: implicit this-anrop
    var id = orderId;              // fel: läser fält på this

    super("order");
}
```

Det här är också fel:

```java
Order(String rawOrderId, Money total) {
    super.entityType();            // fel: använder super före super(...)

    super("order");
}
```

En bra tumregel:

> I prologen får du validera indata, beräkna lokala värden och i vissa fall initiera egna oinitierade fält. Du får inte använda objektets beteende.

### Direkt fältinitiering före super

En viktig nyhet är att prologen kan initiera fält i den aktuella klassen, under begränsade former. Det gäller fält som deklareras i samma klass och som inte redan har en egen initializer.

```java
final class Order extends AuditedEntity {
    private final String orderId;
    private final Money total;

    Order(String rawOrderId, Money total) {
        if (rawOrderId == null || rawOrderId.isBlank()) {
            throw new IllegalArgumentException("orderId must not be blank");
        }

        this.orderId = rawOrderId.strip();
        this.total = Objects.requireNonNull(total);

        super("order");
    }

    @Override
    protected void onConstructionEvent() {
        System.out.println("Creating order " + orderId + " with total " + total);
    }
}
```

Det här kan se ovant ut första gången. Men det adresserar ett verkligt problem: om superklassens konstruktor på något sätt gör objektet observerbart, kan subklassens fält redan vara i giltigt tillstånd.

Samtidigt ska detta inte användas som ursäkt för riskabla basklasser. En konstruktor som anropar överlagringsbara metoder är fortfarande misstänkt design. Java 25 gör mönstret mindre farligt i vissa fall, men det gör det inte automatiskt bra.

### Skillnaden mellan validering och beteende

Det är frestande att använda prologen till mer och mer logik. Motstå den frestelsen.

Bra prologkod:

```java
Order(String rawOrderId, Money total) {
    if (rawOrderId == null || rawOrderId.isBlank()) {
        throw new IllegalArgumentException("orderId must not be blank");
    }

    var normalizedId = rawOrderId.strip();
    var checkedTotal = Objects.requireNonNull(total);

    super("order");

    this.orderId = normalizedId;
    this.total = checkedTotal;
}
```

Sämre prologkod:

```java
Order(String rawOrderId, Money total) {
    var normalizedId = rawOrderId.strip();
    var discount = pricingService().discountFor(normalizedId); // fel riktning

    super("order");

    this.orderId = normalizedId;
    this.total = total.minus(discount);
}
```

Även om något liknande skulle kunna göras tekniskt, hör det inte hemma i konstruktorn om det kräver tjänsteanrop, miljöberoenden eller affärsprocesser. Då är en fabriksmetod, builder, domänservice eller explicit skapandeprocess bättre.

### När `this(...)` används

Flexible Constructor Bodies gäller också alternativa konstruktoranrop med `this(...)`.

I Java 21 behövde `this(...)` stå först:

```java
Order(String rawOrderId) {
    this(rawOrderId, Money.zero("SEK"));
}
```

I Java 25 kan konstruktorn göra säker validering eller beräkning före `this(...)`:

```java
Order(String rawOrderId) {
    if (rawOrderId == null || rawOrderId.isBlank()) {
        throw new IllegalArgumentException("orderId must not be blank");
    }

    this(rawOrderId.strip(), Money.zero("SEK"));
}
```

Det här är ofta ett bättre exempel än arvsexemplet, eftersom det inte kräver någon problematisk basklass. Det låter dig hålla konstruktoröverladdningar läsbara utan att flytta trivial validering till statiska metoder.

### Records och enums

Records och enums har särskilda konstruktorregler även i Java 25. Flexible Constructor Bodies tar inte bort dessa grundregler.

För records gäller fortfarande bland annat att:

- den kanoniska konstruktorn har särskilda regler
- en icke-kanonisk record-konstruktor måste delegera med `this(...)`

Men en icke-kanonisk konstruktor kan dra nytta av att kod får stå före `this(...)`, till exempel för enkel validering eller normalisering.

```java
record OrderId(String value) {
    OrderId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
        value = value.strip();
    }

    OrderId(long sequence) {
        if (sequence <= 0) {
            throw new IllegalArgumentException("sequence must be positive");
        }

        this("OF-" + sequence);
    }
}
```

Här är den kompakta kanoniska konstruktorn fortfarande ofta bäst för record-invarianter. Flexible Constructor Bodies är främst intressant för överladdade konstruktorer som delegerar.

## Exempel: OrderFlow och säkrare konstruktorer

Vi skapar ett litet exempel för OrderFlow: en order ska alltid ha ett icke-tomt order-ID och ett totalbelopp. Basklassen `AuditedEntity` är medvetet lite obekväm eftersom den illustrerar äldre arvskod.

```java
import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

public class FlexibleConstructorBodiesDemo {
    public static void main(String[] args) {
        var order = new Order(" OF-1001 ", new Money("SEK", "199.00"));
        System.out.println(order.summary());

        try {
            new Order("   ", new Money("SEK", "10.00"));
        } catch (IllegalArgumentException ex) {
            System.out.println("Validation failed: " + ex.getMessage());
        }
    }

    static abstract class AuditedEntity {
        private final String entityType;

        protected AuditedEntity(String entityType) {
            this.entityType = Objects.requireNonNull(entityType);
            onConstructionEvent();
        }

        protected void onConstructionEvent() {
            // Hook för äldre kod. I ny kod hade vi undvikit överlagringsbart anrop här.
        }

        String entityType() {
            return entityType;
        }
    }

    static final class Order extends AuditedEntity {
        private final String orderId;
        private final Money total;

        Order(String rawOrderId, Money total) {
            if (rawOrderId == null || rawOrderId.isBlank()) {
                throw new IllegalArgumentException("orderId must not be blank");
            }

            this.orderId = rawOrderId.strip();
            this.total = Objects.requireNonNull(total);

            super("order");
        }

        @Override
        protected void onConstructionEvent() {
            System.out.println("Constructing " + orderId + " with total " + total);
        }

        String summary() {
            return entityType() + " " + orderId + " = " + total;
        }
    }

    record Money(Currency currency, BigDecimal amount) {
        Money(String currencyCode, String amount) {
            this(Currency.getInstance(currencyCode), new BigDecimal(amount));
        }

        Money {
            Objects.requireNonNull(currency);
            Objects.requireNonNull(amount);
            if (amount.signum() < 0) {
                throw new IllegalArgumentException("amount must not be negative");
            }
        }

        @Override
        public String toString() {
            return amount + " " + currency;
        }
    }
}
```

Notera tre saker:

1. `Order` validerar `rawOrderId` innan `super("order")`.
2. `Order` initierar sina egna fält innan superklassens konstruktor körs.
3. `onConstructionEvent()` kan därför läsa `orderId` och `total` utan att se defaultvärden.

I en renare design hade `AuditedEntity` inte anropat en överlagringsbar metod i konstruktorn. Men många produktionssystem har arv som inte kan städas upp direkt. Java 25 ger ett säkrare mellansteg.

## Vanliga misstag

### Misstag: att använda prologen som mini-fabriksmetod

**Varför det händer:**  
När kod före `super(...)` blir möjlig är det lockande att lägga in mer affärslogik där.

**Hur man undviker det:**  
Håll prologen kort. Den ska främst validera, normalisera och förbereda konstruktorargument. Om den behöver externa beroenden, I/O, databas, feature flags eller tjänsteanrop hör koden troligen inte hemma i konstruktorn.

### Misstag: att tro att `this` är fritt tillgängligt

**Varför det händer:**  
Koden ligger syntaktiskt i en instans-konstruktor, så det känns naturligt att använda instansfält och instansmetoder.

**Hur man undviker det:**  
Tänk på prologen som ett begränsat område. Du är på väg att skapa objektet, men objektet är inte fullt tillgängligt ännu. Läs inte fält, anropa inte instansmetoder och skicka inte vidare `this`.

### Misstag: att legitimera dåliga superklasskonstruktorer

**Varför det händer:**  
Eftersom subklassfält kan initieras tidigare kan gamla problem med överlagringsbara anrop från konstruktorer verka lösta.

**Hur man undviker det:**  
Se featuren som riskreducering, inte som designrekommendation. Dokumentera arvshierarkier som fortfarande har konstruktorrisker och planera refaktorering när det är rimligt.

### Misstag: att införa Java 25-syntax innan verktygen stödjer den

**Varför det händer:**  
Kompilatorn stödjer featuren, men IDE, formatterare, static analysis, coverage-verktyg eller kodgeneratorer kan ligga efter.

**Hur man undviker det:**  
Lägg till ett litet kompilerings- och analyscase i CI innan featuren används brett. Kontrollera särskilt kodstil, formattering, lintregler och byggplugins.

## Övningar

### Övning 1: Refaktorera bort statisk hjälpfunktion

Utgå från en Java 21-liknande konstruktor:

```java
final class InvoiceLine extends AuditedEntity {
    private final String sku;
    private final int quantity;

    InvoiceLine(String sku, int quantity) {
        super("invoice-line", requireSku(sku), requireQuantity(quantity));
        this.sku = sku.strip();
        this.quantity = quantity;
    }

    private static String requireSku(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("sku must not be blank");
        }
        return value.strip();
    }

    private static int requireQuantity(int value) {
        if (value <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        return value;
    }
}
```

Skriv om konstruktorn med Java 25-stil så att valideringen ligger direkt i konstruktorn. Fundera på vilka hjälpfunktioner som fortfarande kan vara värda att behålla.

### Övning 2: Identifiera förbjuden prologkod

Markera vilka rader som inte hör hemma före `super(...)`:

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

Skriv sedan en korrigerad version.

### Övning 3: Välj design

Du har en konstruktor som behöver:

1. validera tre argument
2. läsa konfiguration
3. skapa ett domänobjekt
4. publicera ett domänevent

Vilka delar kan rimligen ligga i en constructor prologue? Vilka delar bör flyttas till fabriksmetod, builder eller service? Motivera svaret.

### Fördjupning

Sök i en befintlig Java 21-kodbas efter konstruktorer där `super(...)` anropar statiska hjälpfunktioner enbart för enkel validering eller normalisering. Välj tre fall och klassificera dem:

- bör skrivas om med Flexible Constructor Bodies
- bör behålla statisk hjälpfunktion
- bör göras om till fabriksmetod eller annan skapandeprocess

## Snabb sammanfattning

- Java 25 finaliserar Flexible Constructor Bodies.
- Kod får stå före explicit `super(...)` eller `this(...)`.
- Kod före konstruktoranropet kallas constructor prologue.
- Kod efter konstruktoranropet kallas constructor epilogue.
- Prologen körs i en early construction context och får inte använda objektet som färdigkonstruerat.
- Fail-fast-validering och enkla beräkningar före `super(...)` blir mer naturliga.
- Vissa egna fält kan initieras före `super(...)`, vilket kan minska risken för halvinitierat tillstånd.
- Featuren förbättrar uttryckskraft och säkerhet men ersätter inte god konstruktor- och arvsdesign.

## Quiz/reflektionsfrågor

1. Varför tvingades många Java 21-konstruktorer använda statiska hjälpfunktioner i `super(...)`-argument?
2. Vad är skillnaden mellan constructor prologue och constructor epilogue?
3. Varför är `this.validate()` olämpligt före `super(...)`?
4. När kan det vara rimligt att initiera ett fält före `super(...)`?
5. Varför är det fortfarande problematiskt när en superklasskonstruktor anropar en överlagringsbar metod?
6. Vilken typ av kod bör inte placeras i en constructor prologue även om den tekniskt skulle kunna skrivas där?
7. Hur bör ett team införa Flexible Constructor Bodies i en stor kodbas utan att skapa verktygsproblem?

## Nästa steg

Nästa kapitel går vidare till en preview-feature: **Primitive Types i patterns, instanceof och switch**. Där blir statusmarkeringen extra viktig. Till skillnad från Flexible Constructor Bodies kräver preview-features ett mer försiktigt införandebeslut, tydliga kompilatorflaggor och separata experimentytor.
