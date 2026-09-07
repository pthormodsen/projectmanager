package no.patreek.projectmanager.repository;

import no.patreek.projectmanager.domain.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
}
