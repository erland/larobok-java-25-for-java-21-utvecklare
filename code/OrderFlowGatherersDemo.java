import java.util.List;
import java.util.stream.Gatherers;

public class OrderFlowGatherersDemo {

    record OrderEvent(String orderId, String type, int amount) {
    }

    record WindowSummary(List<String> orderIds, int totalAmount, int largeOrderCount) {
        @Override
        public String toString() {
            return "WindowSummary[orderIds=" + orderIds
                    + ", totalAmount=" + totalAmount
                    + ", largeOrderCount=" + largeOrderCount + "]";
        }
    }

    public static void main(String[] args) {
        var events = List.of(
                new OrderEvent("A-100", "CREATED", 1_200),
                new OrderEvent("A-101", "CREATED", 25_000),
                new OrderEvent("A-102", "CREATED", 18_000),
                new OrderEvent("A-103", "CREATED", 900),
                new OrderEvent("A-104", "CREATED", 42_000),
                new OrderEvent("A-105", "CREATED", 300),
                new OrderEvent("A-106", "CREATED", 11_000),
                new OrderEvent("A-107", "CREATED", 8_500)
        );

        System.out.println("Fasta fönster om tre:");
        events.stream()
                .gather(Gatherers.windowFixed(3))
                .map(OrderFlowGatherersDemo::summarize)
                .forEach(System.out::println);

        System.out.println();
        System.out.println("Glidande fönster med minst två stora order:");
        events.stream()
                .gather(Gatherers.windowSliding(3))
                .filter(OrderFlowGatherersDemo::containsMultipleLargeOrders)
                .map(OrderFlowGatherersDemo::summarize)
                .forEach(System.out::println);
    }

    private static WindowSummary summarize(List<OrderEvent> window) {
        var orderIds = window.stream()
                .map(OrderEvent::orderId)
                .toList();

        int totalAmount = window.stream()
                .mapToInt(OrderEvent::amount)
                .sum();

        int largeOrderCount = (int) window.stream()
                .filter(event -> event.amount() > 10_000)
                .count();

        return new WindowSummary(orderIds, totalAmount, largeOrderCount);
    }

    private static boolean containsMultipleLargeOrders(List<OrderEvent> window) {
        long largeOrders = window.stream()
                .filter(event -> event.amount() > 10_000)
                .count();

        return largeOrders >= 2;
    }
}
