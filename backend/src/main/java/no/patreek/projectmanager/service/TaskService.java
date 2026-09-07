package no.patreek.projectmanager.service;

import no.patreek.projectmanager.domain.entity.Task;

import java.util.List;
import java.util.Optional;

public interface TaskService {

    List<Task> findAll();

    Optional<Task> findById(Long id);

    Task create(Task task);

    Optional<Task> updateById(Long id, Task task);

    boolean deleteById(Long id);

    List<Task> saveAll(List<Task> tasks);

    List<Task> findByUserId(Long userId);

    List<Task> findByProjectIdAndUserId(Long projectId, Long userId);

    List<Task> findOwnerlessTasks();

    no.patreek.projectmanager.domain.entity.Project getDefaultProject();

    Task save(Task task);
}
