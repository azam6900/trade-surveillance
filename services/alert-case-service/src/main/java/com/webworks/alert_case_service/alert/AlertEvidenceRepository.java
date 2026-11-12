package com.webworks.alert_case_service.alert;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface AlertEvidenceRepository extends MongoRepository<AlertEvidence, UUID> {
}