package no.patreek.projectmanager.controller.api.dto;

import jakarta.validation.constraints.NotNull;

public record AssignUserRequest(
    @NotNull
    Long userId
) {}
