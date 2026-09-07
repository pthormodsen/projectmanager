package no.patreek.projectmanager.controller.api.dto;

import jakarta.validation.constraints.NotNull;
import no.patreek.projectmanager.domain.enums.TaskStatus;

public record StatusRequest(
    @NotNull
    TaskStatus status
) {}
