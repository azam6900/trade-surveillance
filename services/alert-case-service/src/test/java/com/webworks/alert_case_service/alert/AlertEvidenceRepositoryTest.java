package com.webworks.alert_case_service.alert;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@Testcontainers
class AlertEvidenceRepositoryTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    @Autowired
    AlertEvidenceRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void savesAndReloadsEvidenceOfAnyShape() {
        UUID id = UUID.randomUUID();
        Map<String, Object> evidence = Map.of("placedQty", 50_000L,
                "events", List.of(Map.of("orderId", "L-1", "action", "NEW")));
        repository.save(new AlertEvidence(id, "SPOOFING", evidence, Instant.now()));

        AlertEvidence loaded = repository.findById(id).orElseThrow();
        assertThat(loaded.rule()).isEqualTo("SPOOFING");
        assertThat(loaded.evidence()).containsKey("events");
    }

    @Test
    void savingTwiceWithTheSameIdKeepsOneDocument() {
        UUID id = UUID.randomUUID();
        repository.save(new AlertEvidence(id, "CANCEL_RATIO", Map.of("cancels", 40), Instant.now()));
        repository.save(new AlertEvidence(id, "CANCEL_RATIO", Map.of("cancels", 40), Instant.now()));

        assertThat(repository.count()).isEqualTo(1);
    }
}