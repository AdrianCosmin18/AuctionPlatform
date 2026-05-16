package org.nedelcu.cosmin.auction.api.common.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String AUCTION_EXCHANGE = "auction.events.exchange";
    public static final String AUCTION_EVENTS_QUEUE = "auction.events.queue";
    public static final String AUCTION_ROUTING_KEY = "auction.events";

    @Bean
    public TopicExchange auctionExchange() {
        return new TopicExchange(AUCTION_EXCHANGE);
    }

    @Bean
    public Queue auctionEventsQueue() {
        return QueueBuilder.durable(AUCTION_EVENTS_QUEUE).build();
    }

    @Bean
    public Binding auctionEventsBinding() {
        return BindingBuilder.bind(auctionEventsQueue())
                .to(auctionExchange())
                .with(AUCTION_ROUTING_KEY);
    }
}
