package org.babynexign.babybilling.commutatorservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String CDR_QUEUE_NAME = "cdr.processing.brt";
    public static final String CDR_EXCHANGE_NAME = "cdr.direct";
    public static final String CDR_ROUTING_KEY = "cdr.brt";

    @Bean
    public Queue queue() {
        return new Queue(CDR_QUEUE_NAME, true);
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(CDR_EXCHANGE_NAME);
    }

    @Bean
    public Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(CDR_ROUTING_KEY);
    }
}
