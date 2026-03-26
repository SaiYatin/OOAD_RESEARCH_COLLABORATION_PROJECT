package com.research.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

/**
 * Expert - a professor/domain expert that can be recommended.
 * Sourced from scraped PES staff data (via n8n University Research Agent).
 * Design Principle: SRP - handles expert profile management.
 */
@Entity
@Table(name = "experts")
@Getter
@Setter
public class Expert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long expertId;

    @Column(nullable = false)
    private String name;

    private String designation;

    @Column(unique = true)
    private String email;

    private String phone;

    @Column(columnDefinition = "TEXT")
    private String researchAreas;

    private String institution;

    private String profileUrl;

    // Keywords extracted for matching - drives the recommendation engine
    @ElementCollection
    @CollectionTable(name = "expert_keywords",
                     joinColumns = @JoinColumn(name = "expert_id"))
    @Column(name = "keyword")
    private List<String> keywords = new ArrayList<>();

    @Column(nullable = false)
    private boolean active = true;

    /**
     * Expert profile management - class diagram method.
     * Parses raw research text and extracts searchable keywords.
     */
    public void updateProfile(String newResearchAreas) {
        this.researchAreas = newResearchAreas;
        this.keywords = extractKeywords(newResearchAreas);
    }

    private List<String> extractKeywords(String text) {
        if (text == null || text.equalsIgnoreCase("Not listed")) return new ArrayList<>();
        List<String> result = new ArrayList<>();
        // Split on commas, newlines, periods - get meaningful tokens
        String[] tokens = text.split("[,\n.]+");
        for (String token : tokens) {
            String cleaned = token.trim().toLowerCase();
            if (cleaned.length() > 3 && cleaned.length() < 60) {
                result.add(cleaned);
            }
        }
        return result;
    }

    /**
     * Calculates a relevance score against a search query.
     * Used by the RecommendationStrategy (Design Pattern: Strategy).
     */
    public double scoreAgainst(String query) {
        if (query == null || researchAreas == null) return 0.0;
        String lowerQuery = query.toLowerCase();
        String lowerResearch = researchAreas.toLowerCase();
        long matchCount = keywords.stream()
            .filter(k -> lowerQuery.contains(k) || k.contains(lowerQuery))
            .count();
        // Boost exact match in research area
        double boost = lowerResearch.contains(lowerQuery) ? 2.0 : 1.0;
        return matchCount * boost;
    }
}
