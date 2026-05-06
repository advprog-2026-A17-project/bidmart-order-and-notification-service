package id.ac.ui.cs.advprog.bidmartordernotificationservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderRabbitConfig {

    @Bean
    TopicExchange bidmartEventsExchange(
            @Value("${bidmart.rabbitmq.events-exchange:bidmart.events}") String exchangeName
    ) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    Queue orderAuctionEventsQueue(
            @Value("${bidmart.rabbitmq.order.auction-events-queue:order-notification.auction-events}") String queueName
    ) {
        return new Queue(queueName, true);
    }

    @Bean
    Binding orderBidPlacedBinding(Queue orderAuctionEventsQueue, TopicExchange bidmartEventsExchange) {
        return BindingBuilder.bind(orderAuctionEventsQueue)
                .to(bidmartEventsExchange)
                .with("auction.bid-placed.v1");
    }

    @Bean
    Binding orderOutbidBinding(Queue orderAuctionEventsQueue, TopicExchange bidmartEventsExchange) {
        return BindingBuilder.bind(orderAuctionEventsQueue)
                .to(bidmartEventsExchange)
                .with("auction.outbid.v1");
    }

    @Bean
    Binding orderAuctionEndedBinding(Queue orderAuctionEventsQueue, TopicExchange bidmartEventsExchange) {
        return BindingBuilder.bind(orderAuctionEventsQueue)
                .to(bidmartEventsExchange)
                .with("auction.ended.v1");
    }
}
