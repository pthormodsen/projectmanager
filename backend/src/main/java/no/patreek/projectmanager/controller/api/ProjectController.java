package no.patreek.projectmanager.controller.api;

import jakarta.validation.Valid;
import no.patreek.projectmanager.controller.api.dto.ProjectResponse;
import no.patreek.projectmanager.controller.api.dto.TaskResponse;
import no.patreek.projectmanager.domain.entity.Project;
import no.patreek.projectmanager.domain.entity.Task;
import no.patreek.projectmanager.domain.enums.TaskStatus;
import no.patreek.projectmanager.repository.ProjectRepository;
import no.patreek.projectmanager.repository.TaskRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    public ProjectController(ProjectRepository projectRepository, TaskRepository taskRepository) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
    }

    // CRUD Projects
    @GetMapping
    public List<ProjectResponse> getAllProjects() {
        return projectRepository.findAll().stream()
            .map(ProjectResponse::from)
            .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProject(@PathVariable Long id) {
        return projectRepository.findById(id)
            .map(ProjectResponse::from)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody Project project) {
        Project saved = projectRepository.save(project);
        return ResponseEntity.status(201).body(ProjectResponse.from(saved));
    }

    // Tasks inside a project
    @GetMapping("/{projectId}/tasks")
    public ResponseEntity<List<TaskResponse>> getTasksForProject(@PathVariable Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(taskRepository.findByProjectId(projectId).stream()
            .map(TaskResponse::from)
            .toList());
    }

    @PostMapping("/{projectId}/tasks")
    public ResponseEntity<TaskResponse> createTaskInProject(
        @PathVariable Long projectId,
        @Valid @RequestBody Task task
    ) {
        return projectRepository.findById(projectId)
            .map(project -> {
                task.setProject(project);
                if (task.getStatus() == null) task.setStatus(TaskStatus.TODO);
                Task saved = taskRepository.save(task);
                return ResponseEntity.status(201).body(TaskResponse.from(saved));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{projectId}/tasks/bulk")
    public ResponseEntity<List<TaskResponse>> createTasksInProjectBulk(
        @PathVariable Long projectId,
        @RequestBody List<Task> tasks
    ) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Project not found"));

        tasks.forEach(task -> {
            task.setProject(project);

            if (task.getStatus() == null) {
                task.setStatus(TaskStatus.TODO);
            }
        });

        List<Task> savedTasks = taskRepository.saveAll(tasks);
        return ResponseEntity.status(201).body(savedTasks.stream()
            .map(TaskResponse::from)
            .toList());
    }

}
