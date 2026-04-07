package com.research.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Expert - a professor/domain expert that can be recommended.
 */
@Entity
@Table(name = "experts")
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

    private String domain;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "expert_keywords",
                     joinColumns = @JoinColumn(name = "expert_id"))
    @Column(name = "keyword")
    private List<String> keywords = new ArrayList<>();

    @Column(nullable = false)
    private boolean active = true;

    // ── Getters ──────────────────────────────────────────────
    public Long getExpertId() { return expertId; }
    public String getName() { return name; }
    public String getDesignation() { return designation; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getResearchAreas() { return researchAreas; }
    public String getInstitution() { return institution; }
    public String getProfileUrl() { return profileUrl; }
    public String getDomain() { return domain; }
    public List<String> getKeywords() { return keywords; }
    public boolean isActive() { return active; }

    // ── Setters ──────────────────────────────────────────────
    public void setExpertId(Long expertId) { this.expertId = expertId; }
    public void setName(String name) { this.name = name; }
    public void setDesignation(String designation) { this.designation = designation; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setResearchAreas(String researchAreas) { this.researchAreas = researchAreas; }
    public void setInstitution(String institution) { this.institution = institution; }
    public void setProfileUrl(String profileUrl) { this.profileUrl = profileUrl; }
    public void setDomain(String domain) { this.domain = domain; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }
    public void setActive(boolean active) { this.active = active; }

    // ── Business methods ─────────────────────────────────────
    public void updateProfile(String newResearchAreas) {
        this.researchAreas = newResearchAreas;
        this.keywords = extractKeywords(newResearchAreas);
    }

    private List<String> extractKeywords(String text) {
        if (text == null || text.equalsIgnoreCase("Not listed")) return new ArrayList<>();
        List<String> result = new ArrayList<>();
        String[] tokens = text.split("[,\n.]+");
        for (String token : tokens) {
            String cleaned = token.trim().toLowerCase();
            if (cleaned.length() > 3 && cleaned.length() < 60) {
                result.add(cleaned);
            }
        }
        return result;
    }

    public double scoreAgainst(String query) {
        if (query == null || researchAreas == null) return 0.0;
        String lowerQuery = query.toLowerCase();
        String lowerResearch = researchAreas.toLowerCase();
        long matchCount = keywords.stream()
            .filter(k -> lowerQuery.contains(k) || k.contains(lowerQuery))
            .count();
        double boost = lowerResearch.contains(lowerQuery) ? 2.0 : 1.0;
        return matchCount * boost;
    }
}
