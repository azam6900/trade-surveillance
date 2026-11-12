package com.webworks.alert_case_service.config;

import com.webworks.tradecommon.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicsConfig {

    @Bean
    NewTopic alertsRaw() {
        return TopicBuilder.name(Topics.ALERTS).partitions(3).replicas(1).build();
    }

    // The dead-letter publisher writes to the SAME partition number as the original message,
    // so the DLT must have at least as many partitions as alerts.raw.
    @Bean
    NewTopic alertsDlt() {
        return TopicBuilder.name(Topics.ALERTS_DLT).partitions(3).replicas(1).build();
    }
}