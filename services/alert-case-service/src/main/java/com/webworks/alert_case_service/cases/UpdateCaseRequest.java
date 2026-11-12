package com.webworks.alert_case_service.cases;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Every field is optional except version: send only what changes. */
public record UpdateCaseRequest(CaseStatus status, @Size(max = 64) String assignee,
                                @Min(1) @Max(5) Integer priority, @NotNull Long version) {
}