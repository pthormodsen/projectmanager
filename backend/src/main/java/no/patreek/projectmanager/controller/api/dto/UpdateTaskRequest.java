package no.patreek.projectmanager.controller.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTaskRequest(
    @NotBlank(message = "Title is mandatory")
    @Size(min = 2, max = 255, message = "min 2, max 255 characters")
    String title,

    @Size(max = 255, message = "Max 255 characters")
    String description,

    boolean completed
) {}
