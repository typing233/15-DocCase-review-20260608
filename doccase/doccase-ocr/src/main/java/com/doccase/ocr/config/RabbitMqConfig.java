package com.doccase.ocr.config;

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
    public TopicExchange ocrExchange() {
        return new TopicExchange(MqConstants.EXCHANGE_OCR, true, false);
    }

    @Bean
    public TopicExchange dlxExchange() {
        return new TopicExchange(MqConstants.EXCHANGE_DLX, true, false);
    }

    @Bean
    public Queue ocrTaskQueue() {
        return QueueBuilder.durable(MqConstants.QUEUE_OCR_TASK)
                .withArgument("x-dead-letter-exchange", MqConstants.EXCHANGE_DLX)
                .withArgument("x-dead-letter-routing-key", "dlx.ocr")
                .build();
    }

    @Bean
    public Queue dlxQueue() {
        return QueueBuilder.durable(MqConstants.QUEUE_DLX).build();
    }

    @Bean
    public Binding ocrTaskBinding() {
        return BindingBuilder.bind(ocrTaskQueue()).to(ocrExchange()).with(MqConstants.RK_OCR_SUBMIT);
    }

    @Bean
    public Binding dlxBinding() {
        return BindingBuilder.bind(dlxQueue()).to(dlxExchange()).with("dlx.#");
    }
}
