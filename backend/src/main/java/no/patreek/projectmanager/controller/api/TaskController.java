package no.patreek.projectmanager.controller.api;

import jakarta.validation.Valid;
import no.patreek.projectmanager.controller.api.dto.AssignUserRequest;
import no.patreek.projectmanager.controller.api.dto.CreateTaskRequest;
import no.patreek.projectmanager.controller.api.dto.StatusRequest;
import no.patreek.projectmanager.controller.api.dto.TaskResponse;
import no.patreek.projectmanager.controller.api.dto.UpdateTaskRequest;
import no.patreek.projectmanager.domain.entity.Task;
import no.patreek.projectmanager.domain.enums.TaskStatus;
import no.patreek.projectmanager.repository.ProjectRepository;
import no.patreek.projectmanager.repository.UserRepository;
import no.patreek.projectmanager.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    public TaskController(
        TaskService taskService,
        UserRepository userRepository,
        ProjectRepository projectRepository
    ) {
        this.taskService = taskService;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
    }

    // GET /api/tasks
    @GetMapping
    public ResponseEntity<List<TaskResponse>> getAllTasks(Principal principal) {
        return currentUser(principal)
            .map(user -> taskService.findByUserId(user.getId()))
            .map(tasks -> tasks.stream()
                .map(TaskResponse::from)
                .toList())
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.ok(taskService.findAll().stream()
                .map(TaskResponse::from)
                .toList()));
    }

    // GET /api/tasks/{id}
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long id, Principal principal) {
        return taskService.findById(id)
            .filter(task -> canAccess(task, principal))
            .map(TaskResponse::from)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/tasks
    @PostMapping
    public ResponseEntity<?> createTask(@Valid @RequestBody CreateTaskRequest req, Principal principal) {
        Task task = new Task();

        task.setTitle(req.title());
        task.setDescription(req.description());
        task.setStatus(req.status() == null ? TaskStatus.TODO : req.status());
        task.setCompleted(false);
        if (req.projectId() == null) {
            task.setProject(taskService.getDefaultProject());
        } else {
            var project = projectRepository.findById(req.projectId());
            if (project.isEmpty()) {
                return ResponseEntity.badRequest().body("Project not found");
            }
            task.setProject(project.get());
        }
        var currentUser = currentUser(principal);
        if (currentUser.isPresent()) {
            task.setUser(currentUser.get());
        } else if (principal != null) {
            throw new UsernameNotFoundException("Signed-in user not found");
        } else if (req.userId() != null) {
            var user = userRepository.findById(req.userId());
            if (user.isEmpty()) {
                return ResponseEntity.badRequest().body("User not found");
            }
            task.setUser(user.get());
        }

        Task saved = taskService.create(task);
        return ResponseEntity.status(201).body(TaskResponse.from(saved));
    }

    // PUT /api/tasks/{id}
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTask(
        @PathVariable Long id,
        @Valid @RequestBody UpdateTaskRequest taskData,
        Principal principal
    ) {
        if (!canAccessTaskId(id, principal)) {
            return ResponseEntity.notFound().build();
        }
        Task task = new Task();
        task.setTitle(taskData.title());
        task.setDescription(taskData.description());
        task.setCompleted(taskData.completed());
        return taskService.updateById(id, task)
            .map(updated -> ResponseEntity.noContent().build())
            .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /api/tasks/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable Long id, Principal principal) {
        if (!canAccessTaskId(id, principal)) {
            return ResponseEntity.notFound().build();
        }
        boolean deleted = taskService.deleteById(id);
        return deleted
            ? ResponseEntity.noContent().build()
            : ResponseEntity.notFound().build();
    }

    // BULK CREATE
    @PostMapping("/bulk")
    public ResponseEntity<List<TaskResponse>> createTasks(@RequestBody List<Task> tasks, Principal principal) {
        var currentUser = currentUser(principal);
        if (currentUser.isPresent()) {
            tasks.forEach(task -> task.setUser(currentUser.get()));
        } else if (principal != null) {
            throw new UsernameNotFoundException("Signed-in user not found");
        }
        List<TaskResponse> savedTasks = taskService.saveAll(tasks).stream()
            .map(TaskResponse::from)
            .toList();
        return ResponseEntity.status(201).body(savedTasks);
    }

    // PATCH /api/tasks/{id}/status
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
        @PathVariable Long id,
        @Valid @RequestBody StatusRequest req,
        Principal principal
    ) {
        return taskService.findById(id)
            .filter(task -> canAccess(task, principal))
            .map(task -> {
                task.setStatus(req.status());
                taskService.save(task);
                return ResponseEntity.noContent().build();
            })
            .orElse(ResponseEntity.notFound().build());
    }

    // PATCH /api/tasks/{id}/assignee
    @PatchMapping("/{id}/assignee")
    public ResponseEntity<?> assignUser(
        @PathVariable Long id,
        @Valid @RequestBody AssignUserRequest req,
        Principal principal
    ) {
        if (!canAccessTaskId(id, principal)) {
            return ResponseEntity.notFound().build();
        }
        var currentUser = currentUser(principal);
        if (currentUser.isPresent() && !currentUser.get().getId().equals(req.userId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Cannot assign another user's tasks");
        }
        return taskService.findById(id)
            .map(task -> userRepository.findById(req.userId())
                .map(user -> {
                    task.setUser(user);
                    taskService.save(task);
                    return ResponseEntity.noContent().build();
                })
                .orElse(ResponseEntity.badRequest().body("User not found"))
            )
            .orElse(ResponseEntity.notFound().build());
    }

    private Optional<no.patreek.projectmanager.domain.entity.User> currentUser(Principal principal) {
        if (principal == null) {
            return Optional.empty();
        }
        return userRepository.findByUsername(principal.getName());
    }

    private boolean canAccessTaskId(Long id, Principal principal) {
        return taskService.findById(id)
            .map(task -> canAccess(task, principal))
            .orElse(false);
    }

    private boolean canAccess(Task task, Principal principal) {
        if (principal == null) {
            return true;
        }
        var currentUser = currentUser(principal);
        return currentUser.isPresent()
            && task.getUser() != null
            && task.getUser().getId().equals(currentUser.get().getId());
    }
}
