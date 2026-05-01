package com.finpred.core.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do RabbitMQ para filas de importação assíncrona.
 */
@Configuration
public class RabbitMQConfig {

    public static final String IMPORT_QUEUE = "finpred.import.queue";

    @Bean
    public Queue importQueue() {
        return new Queue(IMPORT_QUEUE, true); // durable = true
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
