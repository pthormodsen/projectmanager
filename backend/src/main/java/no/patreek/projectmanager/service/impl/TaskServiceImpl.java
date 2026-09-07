package no.patreek.projectmanager.service.impl;

import no.patreek.projectmanager.domain.entity.Project;
import no.patreek.projectmanager.domain.entity.Task;
import no.patreek.projectmanager.domain.enums.TaskStatus;
import no.patreek.projectmanager.repository.ProjectRepository;
import no.patreek.projectmanager.repository.TaskRepository;
import no.patreek.projectmanager.service.TaskService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TaskServiceImpl implements TaskService {

    private final TaskRepository repository;
    private final ProjectRepository projectRepository;

    public TaskServiceImpl(TaskRepository repository, ProjectRepository projectRepository) {
        this.repository = repository;
        this.projectRepository = projectRepository;
    }

    @Override
    public List<Task> findAll() {
        return repository.findAll();
    }

    @Override
    public Optional<Task> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public Task create(Task task) {
        if (task.getProject() == null) {
            task.setProject(getDefaultProject());
        }
        if (task.getStatus() == null) {
            task.setStatus(TaskStatus.TODO);
        }
        return repository.save(task);
    }

    @Override
    public Optional<Task> updateById(Long id, Task taskData) {
        return repository.findById(id).map(task -> {
            task.setTitle(taskData.getTitle());
            task.setDescription(taskData.getDescription());
            task.setCompleted(taskData.isCompleted());
            return repository.save(task);
        });
    }

    @Override
    public boolean deleteById(Long id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }

    @Override
    public List<Task> saveAll(List<Task> tasks) {
        return repository.saveAll(tasks);
    }

    @Override
    public List<Task> findByUserId(Long userId) {
        return repository.findByUserId(userId);
    }

    @Override
    public Project getDefaultProject() {
        return projectRepository.findAll()
            .stream()
            .findFirst()
            .orElseGet(() -> {
                Project project = new Project();
                project.setName("Inbox");
                project.setDescription("Default project for uncategorized tasks");
                return projectRepository.save(project);
            });
    }

    @Override
    public Task save(Task task) {
        return repository.save(task);
    }

}
