package com.webworks.ingestion_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import com.webworks.tradecommon.Topics;

@Configuration
public class KafkaTopicsConfig {

    @Bean
    NewTopic ordersEvents() {
        return TopicBuilder.name(Topics.ORDERS).partitions(6).replicas(1).build();
    }

    @Bean
    NewTopic ordersInvalid() {
        return TopicBuilder.name(Topics.ORDERS_INVALID).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic alertsRaw() {
        return TopicBuilder.name(Topics.ALERTS).partitions(3).replicas(1).build();
    }
}