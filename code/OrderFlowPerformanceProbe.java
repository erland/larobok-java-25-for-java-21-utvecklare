import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Small probe for Chapter 10.
 *
 * This is not a benchmark. It is a simple, runnable program that creates many
 * small objects so that a team can practice measuring startup, heap usage and
 * GC behavior with different JVM flags.
 *
 * Compile:
 *   javac OrderFlowPerformanceProbe.java
 *
 * Run:
 *   java -Xms1g -Xmx1g OrderFlowPerformanceProbe
 *
 * Try compact object headers:
 *   java -XX:+UseCompactObjectHeaders -Xms1g -Xmx1g OrderFlowPerformanceProbe
 *
 * Add GC logging:
 *   java -Xms1g -Xmx1g -Xlog:gc*:file=gc.log:time,uptime,level,tags OrderFlowPerformanceProbe
 */
public class OrderFlowPerformanceProbe {
    record OrderLine(String sku, int quantity, long priceInOre) {}
    record Order(String id, List<OrderLine> lines) {}

    public static void main(String[] args) {
        var start = Instant.now();

        int orders = Integer.getInteger("orders", 200_000);
        int linesPerOrder = Integer.getInteger("lines", 5);

        var generated = new ArrayList<Order>(orders);

        for (int i = 0; i < orders; i++) {
            var lines = new ArrayList<OrderLine>(linesPerOrder);
            for (int j = 0; j < linesPerOrder; j++) {
                lines.add(new OrderLine("SKU-" + j, j + 1, 10_00L + j));
            }
            generated.add(new Order("ORDER-" + i, List.copyOf(lines)));
        }

        long total = generated.stream()
                .flatMap(order -> order.lines().stream())
                .mapToLong(line -> line.quantity() * line.priceInOre())
                .sum();

        var duration = Duration.between(start, Instant.now());

        System.out.println("orders=" + orders);
        System.out.println("linesPerOrder=" + linesPerOrder);
        System.out.println("total=" + total);
        System.out.println("durationMillis=" + duration.toMillis());
        System.out.println("usedMemoryMiB=" + usedMemoryMiB());
    }

    private static long usedMemoryMiB() {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        return used / 1024 / 1024;
    }
}
