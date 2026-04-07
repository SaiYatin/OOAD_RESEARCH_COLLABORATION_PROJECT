package com.research.repository;

import com.research.model.ResearchProject;
import com.research.model.Researcher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ResearchProjectRepository extends JpaRepository<ResearchProject, Long> {
    List<ResearchProject> findByOwner(Researcher owner);

    @Query("SELECT p FROM ResearchProject p JOIN p.members m WHERE m.userId = :userId")
    List<ResearchProject> findByMemberId(@Param("userId") Long userId);

    List<ResearchProject> findByLookingForCollaboratorsTrue();

    @Query("SELECT p FROM ResearchProject p WHERE p.lookingForCollaborators = true AND p.domain IN :domains")
    List<ResearchProject> findByLookingForCollaboratorsTrueAndDomainIn(@Param("domains") List<String> domains);

    @Query("SELECT DISTINCT p.domain FROM ResearchProject p WHERE p.domain IS NOT NULL")
    List<String> findAllDomains();

    List<ResearchProject> findByStatus(ResearchProject.ProjectStatus status);
}
