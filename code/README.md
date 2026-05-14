# Kod

Körbara kodexempel skapas kapitelvis.

- `OrderStatusReport.java`: exempel för kapitel 4 med `import module java.base;`.
## Kapitel 5

- `FlexibleConstructorBodiesDemo.java` visar Java 25-syntax för Flexible Constructor Bodies.
- Exemplet kräver en Java 25-kompilator. Äldre JDK-versioner accepterar inte `super(...)` efter andra statements i konstruktorn.



## `OrderSignalClassifier.java`

Exempel till kapitel 6. Kräver JDK 25 med preview-features aktiverade.

```bash
javac --release 25 --enable-preview OrderSignalClassifier.java
java --enable-preview OrderSignalClassifier
```

Exemplet visar numerisk klassificering med `switch`, primitive patterns och `when`-villkor. Det är labbkod, inte en rekommendation att använda preview-features direkt i produktionsmoduler.


## `OrderFlowScopedValuesDemo.java`

Exempel till kapitel 7. Kräver JDK 25 men inga preview-flaggor.

```bash
javac OrderFlowScopedValuesDemo.java
java OrderFlowScopedValuesDemo
```

Exemplet visar `ScopedValue.newInstance()`, `ScopedValue.where(...).call(...)`, `orElseThrow(...)` och immutable request-context med record.


## Kapitel 8

`OrderAggregationStructuredDemo.java` visar ett Java 25-preview-exempel med `StructuredTaskScope`.

Kompilera och kör med:

```bash
javac --release 25 --enable-preview OrderAggregationStructuredDemo.java
java --enable-preview OrderAggregationStructuredDemo
```

Exemplet är avsett för labb och designförståelse. `StructuredTaskScope` är preview i Java 25.


## Kapitel 9

`OrderFlowGatherersDemo.java` visar Stream Gatherers med `Gatherers.windowFixed(...)` och `Gatherers.windowSliding(...)`.

Kompilera och kör med JDK 25:

```bash
javac --release 25 OrderFlowGatherersDemo.java
java OrderFlowGatherersDemo
```

Exemplet kräver inga preview-flaggor. Stream Gatherers finaliserades i JDK 24 och finns därför som en stabil del av Java 25.


## OrderFlowPerformanceProbe.java

Kodexempel till kapitel 10. Programmet skapar många små orderobjekt och kan användas för att öva på mätning av startup, heap och GC-beteende med olika JVM-flaggor.

Exempel:

```bash
javac OrderFlowPerformanceProbe.java
java -Xms1g -Xmx1g OrderFlowPerformanceProbe
java -XX:+UseCompactObjectHeaders -Xms1g -Xmx1g OrderFlowPerformanceProbe
```


## Kapitel 11

- `OrderValidationJfrTarget.java` är ett målprogram för JFR-experiment med vanlig profilering, Method Timing, Method Trace och, på Linux, CPU-time profiling.

Exempel:

```bash
javac --release 25 code/OrderValidationJfrTarget.java
java -XX:StartFlightRecording=filename=orderflow-profile.jfr,settings=profile,duration=20s -cp code OrderValidationJfrTarget
```


## Kapitel 12

- `SecurityAdoptionMatrix.java` är ett körbart exempel som modellerar en Java 25-beslutsmatris för kryptografi, `Unsafe`, JNI och native access.
- Exemplet använder inga preview-API:er och kan kompileras med:

```bash
javac --release 25 SecurityAdoptionMatrix.java
java SecurityAdoptionMatrix
```
