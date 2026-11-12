package com.webworks.detection_service.config;

import com.webworks.tradecommon.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicsConfig {

    @Bean
    NewTopic ordersEvents() {
        return TopicBuilder.name(Topics.ORDERS).partitions(6).replicas(1).build();
    }

    @Bean
    NewTopic alertsRaw() {
        return TopicBuilder.name(Topics.ALERTS).partitions(3).replicas(1).build();
    }
}