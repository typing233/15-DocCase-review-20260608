package com.doccase.document.config;

import com.doccase.common.constant.MqConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public TopicExchange documentExchange() {
        return new TopicExchange(MqConstants.EXCHANGE_DOCUMENT, true, false);
    }

    @Bean
    public TopicExchange exportExchange() {
        return new TopicExchange(MqConstants.EXCHANGE_EXPORT, true, false);
    }

    @Bean
    public TopicExchange ocrExchange() {
        return new TopicExchange(MqConstants.EXCHANGE_OCR, true, false);
    }

    @Bean
    public Queue exportQueue() {
        return QueueBuilder.durable(MqConstants.QUEUE_EXPORT)
                .withArgument("x-dead-letter-exchange", MqConstants.EXCHANGE_DLX)
                .build();
    }

    @Bean
    public Binding exportBinding() {
        return BindingBuilder.bind(exportQueue()).to(exportExchange()).with(MqConstants.RK_EXPORT_REQUEST);
    }
}
