# Övningar till kapitel 11: JFR-nyheter för CPU, sampling och metodtracing

## Syfte

Övningarna tränar dig i att använda JFR som ett systematiskt analysverktyg vid migrering från Java 21 till Java 25. Målet är inte att hitta “den snabbaste koden”, utan att formulera hypoteser, samla relevant data och fatta rimliga beslut.

## Övning 1: Baslinjeprofil

1. Kompilera exempelprogrammet:

```bash
javac --release 25 code/OrderValidationJfrTarget.java
```

2. Kör med vanlig JFR-profil:

```bash
java \
  -XX:StartFlightRecording=filename=orderflow-profile.jfr,settings=profile,duration=20s \
  -cp code \
  OrderValidationJfrTarget
```

3. Analysera inspelningen med `jfr` eller JDK Mission Control.

Besvara:

- Vilka metoder verkar dominera?
- Är observationen tillräcklig för ett optimeringsbeslut?
- Vilken ytterligare mätning vill du göra?

## Övning 2: Method Timing

Kör:

```bash
java \
  '-XX:StartFlightRecording:method-timing=OrderValidationJfrTarget::validateOrder,filename=orderflow-method-timing.jfr' \
  -cp code \
  OrderValidationJfrTarget
```

Analysera:

```bash
jfr view method-timing orderflow-method-timing.jfr
```

Besvara:

- Hur ofta anropas metoden?
- Är genomsnittstiden rimlig i relation till programmets beteende?
- Vad kan inte Method Timing svara på?

## Övning 3: Method Trace

Kör:

```bash
java \
  '-XX:StartFlightRecording:method-trace=OrderValidationJfrTarget::validateOrder,filename=orderflow-method-trace.jfr' \
  -cp code \
  OrderValidationJfrTarget
```

Analysera:

```bash
jfr view MethodTrace orderflow-method-trace.jfr
```

eller:

```bash
jfr print --events jdk.MethodTrace --stack-depth 20 orderflow-method-trace.jfr
```

Besvara:

- Vilken anropskontext ser du?
- Hade du kunnat få samma information med vanlig sampling?
- Vilken risk finns om filtret görs för brett?

## Övning 4: CPU-time profiling på Linux

Om du kör på Linux med en JDK 25 som stöder eventet, prova:

```bash
java \
  -XX:StartFlightRecording=jdk.CPUTimeSample#enabled=true,filename=orderflow-cpu.jfr \
  -cp code \
  OrderValidationJfrTarget
```

Analysera:

```bash
jfr view cpu-time-hot-methods orderflow-cpu.jfr
```

Besvara:

- Skiljer sig CPU-profilen från den vanliga JFR-profilen?
- Vilka delar av programmet är CPU-bundna?
- Skulle du betrakta resultatet som produktionsbeslutande eller hypotesgenererande?

## Övning 5: Skriv en analysrapport

Skriv en kort rapport med följande rubriker:

1. Hypotes
2. JDK-version och OS
3. JFR-konfiguration
4. Viktigaste observationer
5. Alternativa förklaringar
6. Rekommenderat nästa steg

Rapporten ska vara kort nog att kunna läsas i en pull request.
