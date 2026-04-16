package com.research.repository;

import com.research.model.ResearchPaper;
import com.research.model.Researcher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ResearchPaperRepository extends JpaRepository<ResearchPaper, Long> {
    List<ResearchPaper> findByStatus(ResearchPaper.PaperStatus status);

    @Query("SELECT p FROM ResearchPaper p WHERE " +
           "LOWER(p.title) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(p.abstractText) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(p.keywords) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<ResearchPaper> searchByQuery(@Param("q") String query);

    List<ResearchPaper> findByResearcher(Researcher researcher);

    List<ResearchPaper> findByAssignedReviewer(com.research.model.User reviewer);

    List<ResearchPaper> findByAssignedReviewerAndStatus(com.research.model.User reviewer, ResearchPaper.PaperStatus status);

    List<ResearchPaper> findByDomain(String domain);

    @Query("SELECT p FROM ResearchPaper p WHERE p.status = 'PUBLISHED' " +
           "ORDER BY p.uploadedAt DESC")
    List<ResearchPaper> findLatestPublished();
}
