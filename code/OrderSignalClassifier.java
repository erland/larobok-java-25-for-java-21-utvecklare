// Requires JDK 25 with preview features enabled.
// Compile:
//   javac --release 25 --enable-preview OrderSignalClassifier.java
// Run:
//   java --enable-preview OrderSignalClassifier

public class OrderSignalClassifier {
    public static void main(String[] args) {
        long[] signals = {-1, 0, 42, 500, 5_000};

        for (long signal : signals) {
            System.out.printf("%d -> %s%n", signal, classifySignal(signal));
        }
    }

    static String classifySignal(long signal) {
        return switch (signal) {
            case long s when s < 0 -> "invalid";
            case 0L -> "none";
            case long s when s <= 100 -> "normal";
            case long s when s <= 1_000 -> "elevated";
            default -> "critical";
        };
    }
}
