package com.research.repository;

import com.research.model.ProjectMessage;
import com.research.model.ResearchProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProjectMessageRepository extends JpaRepository<ProjectMessage, Long> {
    List<ProjectMessage> findByProjectOrderBySentAtAsc(ResearchProject project);
}
