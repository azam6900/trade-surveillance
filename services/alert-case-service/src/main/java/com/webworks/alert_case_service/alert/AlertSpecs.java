package com.webworks.alert_case_service.alert;

import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class AlertSpecs {

    private AlertSpecs() {
    }

    public static Specification<Alert> matching(AlertFilter f) {
        List<Specification<Alert>> parts = new ArrayList<>();
        if (f.rule() != null) parts.add((root, q, cb) -> cb.equal(root.get("rule"), f.rule()));
        if (f.status() != null) parts.add((root, q, cb) -> cb.equal(root.get("status"), f.status()));
        if (f.traderId() != null) parts.add((root, q, cb) -> cb.equal(root.get("traderId"), f.traderId()));
        if (f.from() != null) parts.add((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), f.from()));
        if (f.to() != null) parts.add((root, q, cb) -> cb.lessThan(root.get("createdAt"), f.to()));
        return Specification.allOf(parts);
    }
}