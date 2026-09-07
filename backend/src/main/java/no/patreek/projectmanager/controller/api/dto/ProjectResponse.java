package no.patreek.projectmanager.controller.api.dto;

import no.patreek.projectmanager.domain.entity.Project;

public record ProjectResponse(
    Long id,
    String name,
    String description
) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
            project.getId(),
            project.getName(),
            project.getDescription()
        );
    }
}
