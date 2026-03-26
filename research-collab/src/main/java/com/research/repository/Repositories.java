package com.research.repository;

import com.research.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

// ─────────────────────────────────────────────
//  UserRepository
// ─────────────────────────────────────────────
@Repository
interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRole(User.UserRole role);
}

// ─────────────────────────────────────────────
//  ResearcherRepository
// ─────────────────────────────────────────────
@Repository
interface ResearcherRepository extends JpaRepository<Researcher, Long> {
    Optional<Researcher> findByEmail(String email);

    @Query("SELECT r FROM Researcher r WHERE " +
           "LOWER(r.researchInterests) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Researcher> findByKeyword(@Param("keyword") String keyword);
}

// ─────────────────────────────────────────────
//  ExpertRepository - sourced from n8n scraped PES data
// ─────────────────────────────────────────────
@Repository
public interface ExpertRepository extends JpaRepository<Expert, Long> {
    List<Expert> findByActiveTrue();

    @Query("SELECT e FROM Expert e WHERE " +
           "LOWER(e.researchAreas) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Expert> findByResearchAreasContaining(@Param("keyword") String keyword);

    Optional<Expert> findByEmail(String email);

    @Query("SELECT DISTINCT e.domain FROM Expert e WHERE e.domain IS NOT NULL")
    List<String> findAllDomains();
}

// ─────────────────────────────────────────────
//  ResearchPaperRepository
// ─────────────────────────────────────────────
@Repository
public interface ResearchPaperRepository extends JpaRepository<ResearchPaper, Long> {
    List<ResearchPaper> findByStatus(ResearchPaper.PaperStatus status);

    @Query("SELECT p FROM ResearchPaper p WHERE " +
           "LOWER(p.title) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(p.abstractText) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(p.keywords) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<ResearchPaper> searchByQuery(@Param("q") String query);

    List<ResearchPaper> findByResearcher(Researcher researcher);

    List<ResearchPaper> findByDomain(String domain);

    @Query("SELECT p FROM ResearchPaper p WHERE p.status = 'PUBLISHED' " +
           "ORDER BY p.uploadedAt DESC")
    List<ResearchPaper> findLatestPublished();
}

// ─────────────────────────────────────────────
//  ResearchProjectRepository
// ─────────────────────────────────────────────
@Repository
public interface ResearchProjectRepository extends JpaRepository<ResearchProject, Long> {
    List<ResearchProject> findByOwner(Researcher owner);

    @Query("SELECT p FROM ResearchProject p JOIN p.members m WHERE m.userId = :userId")
    List<ResearchProject> findByMemberId(@Param("userId") Long userId);
}

// ─────────────────────────────────────────────
//  CollaborationRequestRepository
// ─────────────────────────────────────────────
@Repository
public interface CollaborationRequestRepository
        extends JpaRepository<CollaborationRequest, Long> {
    List<CollaborationRequest> findBySender(User sender);
    List<CollaborationRequest> findByReceiver(User receiver);
    List<CollaborationRequest> findByReceiverAndStatus(
            User receiver, CollaborationRequest.RequestStatus status);
}
