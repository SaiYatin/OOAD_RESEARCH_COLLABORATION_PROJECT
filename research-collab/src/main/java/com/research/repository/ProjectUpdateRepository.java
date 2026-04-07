package com.research.repository;

import com.research.model.ProjectUpdate;
import com.research.model.ResearchProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProjectUpdateRepository extends JpaRepository<ProjectUpdate, Long> {
    List<ProjectUpdate> findByProjectOrderByPostedAtDesc(ResearchProject project);
}
