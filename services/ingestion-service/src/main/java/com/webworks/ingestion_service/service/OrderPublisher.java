package com.webworks.ingestion_service.service;

import com.webworks.tradecommon.Json;
import com.webworks.tradecommon.OrderEvent;
import com.webworks.tradecommon.Topics;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class OrderPublisher {

    private final KafkaTemplate<String, String> kafka;
    private final AtomicLong rejected = new AtomicLong();

    public OrderPublisher(KafkaTemplate<String, String> kafka) {
        this.kafka = kafka;
    }

    /** Returns the validation problems; an empty list means the order was published. */
    public List<String> publish(OrderEvent e) {
        List<String> errors = OrderValidator.validate(e);
        if (!errors.isEmpty()) {
            rejected.incrementAndGet();
            kafka.send(Topics.ORDERS_INVALID, e == null ? null : e.traderId(), Json.write(e));
            return errors;
        }
        kafka.send(Topics.ORDERS, e.traderId(), Json.write(e));
        return List.of();
    }

    public long rejectedCount() {
        return rejected.get();
    }
}