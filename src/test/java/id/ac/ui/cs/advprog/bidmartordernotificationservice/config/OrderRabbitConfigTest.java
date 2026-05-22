package id.ac.ui.cs.advprog.bidmartordernotificationservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OrderRabbitConfigTest {

    private final OrderRabbitConfig config = new OrderRabbitConfig();

    @Test
    void declaresAuctionCreatedBinding() {
        Queue queue = config.orderAuctionEventsQueue("order-notification.auction-events");
        TopicExchange exchange = config.bidmartEventsExchange("bidmart.events");
        Binding binding = config.orderAuctionCreatedBinding(queue, exchange);

        assertNotNull(binding);
        assertEquals("auction.created.v1", binding.getRoutingKey());
    }

    @Test
    void declaresBidPlacedBinding() {
        Queue queue = config.orderAuctionEventsQueue("order-notification.auction-events");
        TopicExchange exchange = config.bidmartEventsExchange("bidmart.events");
        Binding binding = config.orderBidPlacedBinding(queue, exchange);

        assertNotNull(binding);
        assertEquals("auction.bid-placed.v1", binding.getRoutingKey());
    }

    @Test
    void declaresUserDisabledBinding() {
        Queue queue = config.orderAuthEventsQueue("order-notification.auth-events");
        TopicExchange exchange = config.authEventsExchange("bidmart.auth.events");
        Binding binding = config.orderUserDisabledBinding(queue, exchange);

        assertNotNull(binding);
        assertEquals("auth.userdisabled.v1", binding.getRoutingKey());
    }
}
