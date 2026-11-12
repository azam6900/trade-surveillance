package com.webworks.alert_case_service.cases;

import com.webworks.alert_case_service.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cases")
@PreAuthorize("hasAnyRole('ANALYST','SUPERVISOR')")
public class CaseController {

    private final CaseService service;

    public CaseController(CaseService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CaseDetail open(@Valid @RequestBody CreateCaseRequest req) {
        return service.open(req);
    }

    @GetMapping
    public PageResponse<CaseSummary> list(@RequestParam(required = false) CaseStatus status,
                                          @PageableDefault(size = 50, sort = "openedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.list(status, pageable);
    }

    @GetMapping("/{id}")
    public CaseDetail get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PatchMapping("/{id}")
    public CaseDetail update(@PathVariable UUID id, @Valid @RequestBody UpdateCaseRequest req) {
        return service.update(id, req);
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CaseEventView comment(@PathVariable UUID id, @Valid @RequestBody CommentRequest req) {
        return service.comment(id, req.text());
    }

    @GetMapping("/{id}/events")
    public List<CaseEventView> events(@PathVariable UUID id) {
        return service.events(id);
    }
}