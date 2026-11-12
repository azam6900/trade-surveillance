package com.webworks.alert_case_service.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import tools.jackson.core.JacksonException;

@Configuration
public class KafkaErrorConfig {

    @Bean
    DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> template) {
        // Always publish to alerts.raw.DLT, in the same partition as the original message.
        var recoverer = new DeadLetterPublishingRecoverer(template,
                (record, exception) ->
                        new TopicPartition(com.webworks.tradecommon.Topics.ALERTS_DLT, record.partition()));
        var handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3));
        handler.addNotRetryableExceptions(JacksonException.class);
        return handler;
    }
}