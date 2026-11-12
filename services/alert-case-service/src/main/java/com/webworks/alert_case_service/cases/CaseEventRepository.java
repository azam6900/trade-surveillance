package com.webworks.alert_case_service.cases;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CaseEventRepository extends JpaRepository<CaseEvent, Long> {

    List<CaseEvent> findByCaseIdOrderByIdAsc(UUID caseId);
}