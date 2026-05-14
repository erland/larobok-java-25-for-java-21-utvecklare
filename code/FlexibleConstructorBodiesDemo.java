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
            // Hook for legacy code. New code should normally avoid overridable constructor calls.
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
