package com.webworks.ingestion_service.web;

import com.webworks.ingestion_service.service.OrderPublisher;
import com.webworks.tradecommon.OrderEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderPublisher publisher;

    public OrderController(OrderPublisher publisher) {
        this.publisher = publisher;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ANALYST','SUPERVISOR')")
    public ResponseEntity<Map<String, Object>> submit(@RequestBody OrderEvent event) {
        List<String> errors = publisher.publish(event);
        if (errors.isEmpty()) {
            return ResponseEntity.accepted().body(Map.of("status", "accepted"));
        }
        return ResponseEntity.unprocessableEntity().body(Map.of("status", "rejected", "errors", errors));
    }
}