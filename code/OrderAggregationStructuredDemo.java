import java.time.Duration;
import java.util.concurrent.StructuredTaskScope;

/*
 * Kräver JDK 25 med preview aktiverat:
 *
 *   javac --release 25 --enable-preview OrderAggregationStructuredDemo.java
 *   java --enable-preview OrderAggregationStructuredDemo
 */
public class OrderAggregationStructuredDemo {
    record OrderData(String orderId, String customerId) {}
    record StockData(String orderId, boolean available) {}
    record PaymentData(String orderId, boolean approved) {}
    record OrderDecision(String orderId, boolean accepted, String reason) {}

    public static void main(String[] args) throws InterruptedException {
        var service = new OrderAggregationService(
                new OrderClient(),
                new StockClient(),
                new PaymentClient()
        );

        System.out.println(service.decide("ORDER-1001"));
    }

    static final class OrderAggregationService {
        private final OrderClient orderClient;
        private final StockClient stockClient;
        private final PaymentClient paymentClient;

        OrderAggregationService(
                OrderClient orderClient,
                StockClient stockClient,
                PaymentClient paymentClient
        ) {
            this.orderClient = orderClient;
            this.stockClient = stockClient;
            this.paymentClient = paymentClient;
        }

        OrderDecision decide(String orderId) throws InterruptedException {
            try (var scope = StructuredTaskScope.<Object>open()) {
                var order = scope.fork(() -> orderClient.fetch(orderId));
                var stock = scope.fork(() -> stockClient.fetch(orderId));
                var payment = scope.fork(() -> paymentClient.fetch(orderId));

                scope.join();

                return combine(
                        (OrderData) order.get(),
                        (StockData) stock.get(),
                        (PaymentData) payment.get()
                );
            }
        }

        private OrderDecision combine(
                OrderData order,
                StockData stock,
                PaymentData payment
        ) {
            if (!stock.available()) {
                return new OrderDecision(order.orderId(), false, "out of stock");
            }
            if (!payment.approved()) {
                return new OrderDecision(order.orderId(), false, "payment rejected");
            }
            return new OrderDecision(order.orderId(), true, "accepted");
        }
    }

    static final class OrderClient {
        OrderData fetch(String orderId) throws InterruptedException {
            Thread.sleep(Duration.ofMillis(120));
            return new OrderData(orderId, "CUSTOMER-42");
        }
    }

    static final class StockClient {
        StockData fetch(String orderId) throws InterruptedException {
            Thread.sleep(Duration.ofMillis(80));
            return new StockData(orderId, true);
        }
    }

    static final class PaymentClient {
        PaymentData fetch(String orderId) throws InterruptedException {
            Thread.sleep(Duration.ofMillis(100));
            return new PaymentData(orderId, true);
        }
    }
}
