/*
 * OrderValidationJfrTarget.java
 *
 * Ett litet målprogram för JFR-experiment i kapitel 11.
 *
 * Kompilera:
 *   javac --release 25 code/OrderValidationJfrTarget.java
 *
 * Kör:
 *   java -cp code OrderValidationJfrTarget
 *
 * Exempel med JFR:
 *   java -XX:StartFlightRecording=filename=orderflow-profile.jfr,settings=profile,duration=20s -cp code OrderValidationJfrTarget
 *
 *   java '-XX:StartFlightRecording:method-timing=OrderValidationJfrTarget::validateOrder,filename=orderflow-method-timing.jfr' -cp code OrderValidationJfrTarget
 *
 *   java '-XX:StartFlightRecording:method-trace=OrderValidationJfrTarget::validateOrder,filename=orderflow-method-trace.jfr' -cp code OrderValidationJfrTarget
 */
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

public class OrderValidationJfrTarget {
    private static final Random RANDOM = new Random(42);

    record Order(String id, String customerId, BigDecimal amount, List<String> items) {}

    public static void main(String[] args) {
        List<Order> orders = createOrders(20_000);

        Instant start = Instant.now();
        long accepted = 0;
        long rejected = 0;

        for (int round = 0; round < 30; round++) {
            for (Order order : orders) {
                if (validateOrder(order)) {
                    accepted++;
                } else {
                    rejected++;
                }
            }
        }

        Duration duration = Duration.between(start, Instant.now());
        System.out.printf(
                Locale.ROOT,
                "accepted=%d rejected=%d duration=%d ms%n",
                accepted,
                rejected,
                duration.toMillis()
        );
    }

    static boolean validateOrder(Order order) {
        Objects.requireNonNull(order, "order");
        return hasValidIdentity(order)
                && hasReasonableAmount(order)
                && hasValidItems(order)
                && passesFraudFingerprint(order);
    }

    private static boolean hasValidIdentity(Order order) {
        return order.id() != null
                && order.id().startsWith("ORD-")
                && order.customerId() != null
                && order.customerId().startsWith("CUST-");
    }

    private static boolean hasReasonableAmount(Order order) {
        return order.amount() != null
                && order.amount().signum() > 0
                && order.amount().compareTo(new BigDecimal("50000")) < 0;
    }

    private static boolean hasValidItems(Order order) {
        if (order.items() == null || order.items().isEmpty()) {
            return false;
        }

        int checksum = 0;
        for (String item : order.items()) {
            if (item == null || item.isBlank()) {
                return false;
            }
            checksum += item.hashCode();
        }
        return checksum != 0;
    }

    /*
     * Medvetet CPU-tyngre än resten av valideringen.
     * I ett riktigt system skulle detta kunna motsvara ett dyrt regelverk,
     * en signaturkontroll eller en komplex riskklassificering.
     */
    private static boolean passesFraudFingerprint(Order order) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = (order.id() + ":" + order.customerId() + ":" + order.amount()).getBytes();
            byte[] hash = bytes;

            for (int i = 0; i < 120; i++) {
                digest.update(hash);
                digest.update((byte) i);
                hash = digest.digest(bytes);
            }

            String fingerprint = HexFormat.of().formatHex(hash);
            return !fingerprint.endsWith("0000");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 saknas", e);
        }
    }

    private static List<Order> createOrders(int count) {
        List<Order> orders = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            BigDecimal amount = BigDecimal.valueOf(50 + RANDOM.nextInt(10_000));
            List<String> items = List.of("SKU-" + RANDOM.nextInt(1000), "SKU-" + RANDOM.nextInt(1000));
            orders.add(new Order("ORD-" + i, "CUST-" + (i % 400), amount, items));
        }
        return orders;
    }
}
