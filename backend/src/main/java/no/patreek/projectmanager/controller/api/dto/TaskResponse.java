package no.patreek.projectmanager.controller.api.dto;

import no.patreek.projectmanager.domain.entity.Task;
import no.patreek.projectmanager.domain.enums.TaskStatus;

import java.time.LocalDate;

public record TaskResponse(
    Long id,
    String title,
    String description,
    boolean completed,
    LocalDate dueDate,
    TaskStatus status,
    ProjectResponse project,
    UserResponse user
) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
            task.getId(),
            task.getTitle(),
            task.getDescription(),
            task.isCompleted(),
            task.getDueDate(),
            task.getStatus(),
            ProjectResponse.from(task.getProject()),
            task.getUser() == null ? null : UserResponse.from(task.getUser())
        );
    }
}
