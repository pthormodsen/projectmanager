package no.patreek.projectmanager.controller.api;

import jakarta.validation.Valid;
import no.patreek.projectmanager.controller.api.dto.CreateUserRequest;
import no.patreek.projectmanager.controller.api.dto.TaskResponse;
import no.patreek.projectmanager.controller.api.dto.UserResponse;
import no.patreek.projectmanager.domain.entity.Task;
import no.patreek.projectmanager.domain.entity.User;
import no.patreek.projectmanager.repository.UserRepository;
import no.patreek.projectmanager.service.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final TaskService taskService;
    private final PasswordEncoder passwordEncoder;

    public UserController(
        UserRepository userRepository,
        TaskService taskService,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.taskService = taskService;
        this.passwordEncoder = passwordEncoder;
    }

    // GET /api/users
    @GetMapping
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
            .map(UserResponse::from)
            .toList();
    }

    // POST /api/users
    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest req) {
        if (userRepository.findByUsername(req.username()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }

        User user = new User();
        user.setUsername(req.username());
        user.setEmail(req.email());
        user.setPassword(passwordEncoder.encode(req.password()));
        User saved = userRepository.save(user);
        return ResponseEntity.status(201).body(UserResponse.from(saved));
    }

    // GET /api/users/{id}/tasks
    @GetMapping("/{id}/tasks")
    public ResponseEntity<List<TaskResponse>> getTasksForUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(taskService.findByUserId(id).stream()
            .map(TaskResponse::from)
            .toList());
    }

    // POST /api/users/{id}/tasks
    @PostMapping("/{id}/tasks")
    public ResponseEntity<TaskResponse> createTaskForUser(
        @PathVariable Long id,
        @RequestBody Task task
    ) {
        return userRepository.findById(id)
            .map(user -> {
                task.setUser(user);
                return ResponseEntity
                    .status(201)
                    .body(TaskResponse.from(taskService.create(task)));
            })
            .orElse(ResponseEntity.notFound().build());
    }
}
