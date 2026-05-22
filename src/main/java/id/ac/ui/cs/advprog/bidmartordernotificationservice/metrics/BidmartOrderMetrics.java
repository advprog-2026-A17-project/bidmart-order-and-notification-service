package id.ac.ui.cs.advprog.bidmartordernotificationservice.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class BidmartOrderMetrics {

    private final Counter ordersCreatedTotal;
    private final Counter notificationsSentTotal;
    private final Counter rabbitConsumedTotal;

    public BidmartOrderMetrics(MeterRegistry registry) {
        ordersCreatedTotal = Counter.builder("bidmart_orders_created_total")
                .description("Orders created from auction events")
                .register(registry);
        notificationsSentTotal = Counter.builder("bidmart_notifications_sent_total")
                .description("Notifications dispatched")
                .register(registry);
        rabbitConsumedTotal = Counter.builder("bidmart_order_rabbit_consume_total")
                .description("Rabbit messages consumed by order service")
                .register(registry);
    }

    public void recordOrderCreated() {
        ordersCreatedTotal.increment();
    }

    public void recordNotificationSent() {
        notificationsSentTotal.increment();
    }

    public void recordRabbitConsumed() {
        rabbitConsumedTotal.increment();
    }
}
