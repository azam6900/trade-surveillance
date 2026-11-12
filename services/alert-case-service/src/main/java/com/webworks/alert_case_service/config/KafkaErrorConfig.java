package com.webworks.alert_case_service.config;

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
        // After the retries are used up, publish the record to <topic>.DLT, same partition.
        var recoverer = new DeadLetterPublishingRecoverer(template);
        // Retry 3 times, 1 second apart (4 attempts in total).
        var handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3));
        // Unparseable JSON will never succeed: go straight to the DLT.
        handler.addNotRetryableExceptions(JacksonException.class);
        return handler;
    }
}