package id.ac.ui.cs.advprog.bidmartordernotificationservice.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("unit")
class BidmartOrderMetricsTest {

    @Test
    void recordOrderCreatedIncrementsCounter() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        BidmartOrderMetrics metrics = new BidmartOrderMetrics(registry);

        metrics.recordOrderCreated();
        metrics.recordOrderCreated();

        assertEquals(2.0, registry.get("bidmart_orders_created_total").counter().count());
    }

    @Test
    void recordNotificationSentIncrementsCounter() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        BidmartOrderMetrics metrics = new BidmartOrderMetrics(registry);

        metrics.recordNotificationSent();

        assertEquals(1.0, registry.get("bidmart_notifications_sent_total").counter().count());
    }

    @Test
    void recordRabbitConsumedIncrementsCounter() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        BidmartOrderMetrics metrics = new BidmartOrderMetrics(registry);

        metrics.recordRabbitConsumed();

        assertEquals(1.0, registry.get("bidmart_order_rabbit_consume_total").counter().count());
    }
}
