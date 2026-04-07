package com.research.repository;

import com.research.model.Researcher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResearcherRepository extends JpaRepository<Researcher, Long> {
    Optional<Researcher> findByEmail(String email);

    @Query("SELECT r FROM Researcher r WHERE " +
           "LOWER(r.researchInterests) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Researcher> findByKeyword(@Param("keyword") String keyword);
}
