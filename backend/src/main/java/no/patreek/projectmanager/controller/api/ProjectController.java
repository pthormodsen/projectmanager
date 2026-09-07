package no.patreek.projectmanager.controller.api;

import jakarta.validation.Valid;
import no.patreek.projectmanager.controller.api.dto.CreateProjectRequest;
import no.patreek.projectmanager.controller.api.dto.ProjectResponse;
import no.patreek.projectmanager.controller.api.dto.TaskResponse;
import no.patreek.projectmanager.domain.entity.Project;
import no.patreek.projectmanager.domain.entity.Task;
import no.patreek.projectmanager.domain.enums.TaskStatus;
import no.patreek.projectmanager.repository.ProjectRepository;
import no.patreek.projectmanager.repository.TaskRepository;
import no.patreek.projectmanager.service.CurrentUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final CurrentUserService currentUserService;

    public ProjectController(
        ProjectRepository projectRepository,
        TaskRepository taskRepository,
        CurrentUserService currentUserService
    ) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.currentUserService = currentUserService;
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
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody CreateProjectRequest req) {
        Project project = new Project();
        project.setName(req.name().trim());
        project.setDescription(req.description() == null ? null : req.description().trim());
        Project saved = projectRepository.save(project);
        return ResponseEntity.status(201).body(ProjectResponse.from(saved));
    }

    // Tasks inside a project
    @GetMapping("/{projectId}/tasks")
    public ResponseEntity<List<TaskResponse>> getTasksForProject(@PathVariable Long projectId, Principal principal) {
        if (!projectRepository.existsById(projectId)) {
            return ResponseEntity.notFound().build();
        }

        if (principal == null) {
            return ResponseEntity.ok(taskRepository.findByProjectId(projectId).stream()
                .map(TaskResponse::from)
                .toList());
        }

        var currentUser = currentUserService.requireCurrentUser(principal);
        return ResponseEntity.ok(taskRepository.findByProjectIdAndUserId(projectId, currentUser.getId()).stream()
            .map(TaskResponse::from)
            .toList());
    }

    @PostMapping("/{projectId}/tasks")
    public ResponseEntity<TaskResponse> createTaskInProject(
        @PathVariable Long projectId,
        @Valid @RequestBody Task task,
        Principal principal
    ) {
        return projectRepository.findById(projectId)
            .map(project -> {
                task.setProject(project);
                if (principal != null) {
                    task.setUser(currentUserService.requireCurrentUser(principal));
                }
                if (task.getStatus() == null) task.setStatus(TaskStatus.TODO);
                Task saved = taskRepository.save(task);
                return ResponseEntity.status(201).body(TaskResponse.from(saved));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{projectId}/tasks/bulk")
    public ResponseEntity<List<TaskResponse>> createTasksInProjectBulk(
        @PathVariable Long projectId,
        @RequestBody List<Task> tasks,
        Principal principal
    ) {
        var project = projectRepository.findById(projectId);
        if (project.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var currentUser = principal == null ? null : currentUserService.requireCurrentUser(principal);

        tasks.forEach(task -> {
            task.setProject(project.get());
            if (currentUser != null) {
                task.setUser(currentUser);
            }

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
