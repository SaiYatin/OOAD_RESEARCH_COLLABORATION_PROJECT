package com.research.repository;

import com.research.model.PublicOpinion;
import com.research.model.ResearchProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PublicOpinionRepository extends JpaRepository<PublicOpinion, Long> {
    List<PublicOpinion> findByProjectOrderByPostedAtDesc(ResearchProject project);
}
