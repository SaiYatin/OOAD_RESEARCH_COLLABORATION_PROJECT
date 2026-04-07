package com.research.repository;

import com.research.model.Expert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpertRepository extends JpaRepository<Expert, Long> {
    List<Expert> findByActiveTrue();

    @Query("SELECT e FROM Expert e WHERE " +
           "LOWER(e.researchAreas) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Expert> findByResearchAreasContaining(@Param("keyword") String keyword);

    Optional<Expert> findByEmail(String email);

    @Query("SELECT DISTINCT e.domain FROM Expert e WHERE e.domain IS NOT NULL")
    List<String> findAllDomains();
}
