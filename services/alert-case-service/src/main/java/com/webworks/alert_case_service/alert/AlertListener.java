package com.webworks.alert_case_service.alert;

import com.webworks.tradecommon.AlertEvent;
import com.webworks.tradecommon.Json;
import com.webworks.tradecommon.Topics;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AlertListener {

    private final AlertIngestService service;

    public AlertListener(AlertIngestService service) {
        this.service = service;
    }

    @KafkaListener(topics = Topics.ALERTS, groupId = "alert-case")
    public void onAlert(String json) {
        service.handle(Json.read(json, AlertEvent.class));
    }
}