package com.webworks.ingestion_service.service;

import com.webworks.tradecommon.OrderEvent;
import com.webworks.tradecommon.Topics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class OrderPublisherTest {

    @Mock
    KafkaTemplate<String, String> kafka;

    @InjectMocks
    OrderPublisher publisher;

    private OrderEvent order(String id, long qty) {
        return new OrderEvent(id, "T-1", "RY.TO", OrderEvent.Side.BUY, OrderEvent.OrderType.LIMIT,
                new BigDecimal("142.30"), qty, OrderEvent.Action.NEW, Instant.parse("2026-10-28T14:03:11Z"));
    }

    @Test
    void validOrderGoesToOrdersTopicKeyedByTrader() {
        var errors = publisher.publish(order("O-1", 100));

        assertThat(errors).isEmpty();
        verify(kafka).send(eq(Topics.ORDERS), eq("T-1"), anyString());
        verifyNoMoreInteractions(kafka);
    }

    @Test
    void invalidOrderGoesToInvalidTopicAndIsCounted() {
        var errors = publisher.publish(order("O-2", -5));

        assertThat(errors).contains("qty must be positive");
        verify(kafka).send(eq(Topics.ORDERS_INVALID), eq("T-1"), anyString());
        assertThat(publisher.rejectedCount()).isEqualTo(1);
    }
}