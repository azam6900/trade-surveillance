package com.webworks.alert_case_service.cases;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CaseRepository extends JpaRepository<SurveillanceCase, UUID> {

    Page<SurveillanceCase> findByStatus(CaseStatus status, Pageable pageable);
}