package com.coruja.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String EXCHANGE_NAME = "radares_exchange";
    public static final String MONITORAMENTO_QUEUE_NAME = "monitoramento_radares_queue";
    public static final String ROUTING_KEY_PATTERN = "radares.*"; // Ouve tudo

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue monitoramentoQueue() {
        // 'true' torna a fila durável (sobrevive a reinicializações do RabbitMQ)
        return new Queue(MONITORAMENTO_QUEUE_NAME, true);
    }

    @Bean
    public Binding monitoramentoBinding(Queue monitoramentoQueue, TopicExchange exchange) {
        // Conecta o exchange à nossa fila usando o padrão de roteamento
        return BindingBuilder.bind(monitoramentoQueue).to(exchange).with(ROUTING_KEY_PATTERN);
    }
}
