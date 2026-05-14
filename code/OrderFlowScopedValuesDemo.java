import java.lang.ScopedValue;

public class OrderFlowScopedValuesDemo {
    private static final ScopedValue<RequestContext> REQUEST_CONTEXT =
            ScopedValue.newInstance();

    public static void main(String[] args) throws Exception {
        var service = new OrderService();

        var context = new RequestContext(
                "corr-2026-05-14-001",
                "nordic-shop"
        );

        var request = new OrderRequest("order-1001", 3);

        Receipt receipt = ScopedValue
                .where(REQUEST_CONTEXT, context)
                .call(() -> service.placeOrder(request));

        System.out.println("Receipt: " + receipt);
    }

    record RequestContext(String correlationId, String tenant) {
    }

    record OrderRequest(String orderId, int quantity) {
    }

    record Receipt(String orderId, String correlationId) {
    }

    static final class OrderService {
        Receipt placeOrder(OrderRequest request) {
            validate(request);
            log("Order accepted: " + request.orderId());

            RequestContext context = currentContext();
            return new Receipt(request.orderId(), context.correlationId());
        }

        private void validate(OrderRequest request) {
            if (request.quantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be positive");
            }
            log("Validated quantity: " + request.quantity());
        }
    }

    static void log(String message) {
        RequestContext context = currentContext();
        System.out.println("[%s] [%s] %s".formatted(
                context.correlationId(),
                context.tenant(),
                message
        ));
    }

    static RequestContext currentContext() {
        return REQUEST_CONTEXT.orElseThrow(() ->
                new IllegalStateException("No request context is bound"));
    }
}
