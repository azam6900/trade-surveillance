package com.webworks.ingestion_service.web;

import com.webworks.ingestion_service.service.OrderPublisher;
import com.webworks.ingestion_service.web.OrderController;
import com.webworks.tradecommon.OrderEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    OrderPublisher publisher;

    @InjectMocks
    OrderController controller;

    private final OrderEvent order = new OrderEvent("O-1", "T-1", "RY.TO", OrderEvent.Side.BUY,
            OrderEvent.OrderType.LIMIT, new BigDecimal("142.30"), 100, OrderEvent.Action.NEW,
            Instant.parse("2026-11-14T14:00:00Z"));

    @Test
    void validOrderIsAccepted() {
        when(publisher.publish(order)).thenReturn(List.of());

        ResponseEntity<Map<String, Object>> response = controller.submit(order);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).containsEntry("status", "accepted");
    }

    @Test
    void invalidOrderIsRejectedWithTheErrors() {
        when(publisher.publish(order)).thenReturn(List.of("qty must be positive"));

        ResponseEntity<Map<String, Object>> response = controller.submit(order);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).containsEntry("status", "rejected");
        assertThat(response.getBody()).containsEntry("errors", List.of("qty must be positive"));
    }
}