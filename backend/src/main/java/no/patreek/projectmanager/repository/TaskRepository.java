package no.patreek.projectmanager.repository;

import no.patreek.projectmanager.domain.entity.Task;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @Override
    @EntityGraph(attributePaths = {"project", "user"})
    List<Task> findAll();

    @Override
    @EntityGraph(attributePaths = {"project", "user"})
    Optional<Task> findById(Long id);

    @EntityGraph(attributePaths = {"project", "user"})
    List<Task> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"project", "user"})
    List<Task> findByProjectId(Long projectId);

    @EntityGraph(attributePaths = {"project", "user"})
    List<Task> findByProjectIdAndUserId(Long projectId, Long userId);

    @EntityGraph(attributePaths = {"project", "user"})
    List<Task> findByUserIsNull();
}
