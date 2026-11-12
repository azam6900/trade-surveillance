package com.webworks.alert_case_service.alert;

import com.webworks.alert_case_service.web.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/alerts")
@PreAuthorize("hasAnyRole('ANALYST','SUPERVISOR')")
public class AlertController {

    private final AlertQueryService query;

    public AlertController(AlertQueryService query) {
        this.query = query;
    }

    @GetMapping
    public PageResponse<AlertSummary> list(
            @RequestParam(required = false) String rule,
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(required = false) String traderId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return query.search(new AlertFilter(rule, status, traderId, from, to), pageable);
    }

    @GetMapping("/{id}")
    public AlertDetail get(@PathVariable UUID id) {
        return query.get(id);
    }
}