package com.webworks.alert_case_service.cases;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateCaseRequest(@NotBlank @Size(max = 200) String title, @NotEmpty List<UUID> alertIds) {
}