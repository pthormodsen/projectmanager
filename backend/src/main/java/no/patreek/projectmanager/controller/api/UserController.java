package no.patreek.projectmanager.controller.api;

import jakarta.validation.Valid;
import no.patreek.projectmanager.controller.api.dto.CreateUserRequest;
import no.patreek.projectmanager.controller.api.dto.TaskResponse;
import no.patreek.projectmanager.controller.api.dto.UserResponse;
import no.patreek.projectmanager.domain.enums.TaskStatus;
import no.patreek.projectmanager.domain.entity.Task;
import no.patreek.projectmanager.domain.entity.User;
import no.patreek.projectmanager.repository.UserRepository;
import no.patreek.projectmanager.service.CurrentUserService;
import no.patreek.projectmanager.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final TaskService taskService;
    private final CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder;

    public UserController(
        UserRepository userRepository,
        TaskService taskService,
        CurrentUserService currentUserService,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.taskService = taskService;
        this.currentUserService = currentUserService;
        this.passwordEncoder = passwordEncoder;
    }

    // GET /api/users
    @GetMapping
    public List<UserResponse> getAllUsers(Principal principal) {
        if (principal == null) {
            return userRepository.findAll().stream()
                .map(UserResponse::from)
                .toList();
        }
        return List.of(UserResponse.from(currentUserService.requireCurrentUser(principal)));
    }

    // POST /api/users
    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest req) {
        String username = req.username().trim();
        String email = req.email().trim();
        String password = req.password();

        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        User saved = userRepository.save(user);
        return ResponseEntity.status(201).body(UserResponse.from(saved));
    }

    // GET /api/users/{id}/tasks
    @GetMapping("/{id}/tasks")
    public ResponseEntity<List<TaskResponse>> getTasksForUser(@PathVariable Long id, Principal principal) {
        if (principal == null) {
            if (!userRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(taskService.findByUserId(id).stream()
                .map(TaskResponse::from)
                .toList());
        }

        var currentUser = currentUserService.requireCurrentUser(principal);
        if (!currentUser.getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(taskService.findByUserId(id).stream()
            .map(TaskResponse::from)
            .toList());
    }

    // POST /api/users/{id}/tasks
    @PostMapping("/{id}/tasks")
    public ResponseEntity<TaskResponse> createTaskForUser(
        @PathVariable Long id,
        @Valid @RequestBody Task task,
        Principal principal
    ) {
        if (principal == null) {
            return userRepository.findById(id)
                .map(user -> {
                    task.setUser(user);
                    return ResponseEntity
                        .status(201)
                        .body(TaskResponse.from(taskService.create(task)));
                })
                .orElse(ResponseEntity.notFound().build());
        }

        var currentUser = currentUserService.requireCurrentUser(principal);
        if (!currentUser.getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        task.setUser(currentUser);
        if (task.getStatus() == null) {
            task.setStatus(TaskStatus.TODO);
        }
        return ResponseEntity
            .status(201)
            .body(TaskResponse.from(taskService.create(task)));
    }
}
