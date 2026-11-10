package com.webworks.ingestion_service.service;

import com.webworks.tradecommon.Json;
import com.webworks.tradecommon.OrderEvent;
import com.webworks.tradecommon.Topics;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service
public class OrderPublisher {
    private final KafkaTemplate<String, String> kafka;
    void publish(OrderEvent e) {
        if (!OrderValidator.isValid(e)) { rejected.increment(); kafka.send(Topics.ORDERS_INVALID, key(e), Json.write(e)); return; }
        kafka.send(Topics.ORDERS, e.traderId(), Json.write(e));
    }
}