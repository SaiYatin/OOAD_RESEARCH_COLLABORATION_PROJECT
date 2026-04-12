package com.research.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Expert - a professor/domain expert that can be recommended.
 */
@Entity
@Table(name = "pes_staff_table_2")
public class Expert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expert_id")
    private Long expertId;

    @Column(nullable = false)
    private String name;

    private String designation;

    private String department;

    private String campus;

    @Column(name = "mail", unique = true)
    private String email;

    private String phone;

    @Column(name = "research", columnDefinition = "TEXT")
    private String researchAreas;

    @Column(columnDefinition = "TEXT")
    private String teaching;

    @Column(columnDefinition = "TEXT")
    private String about;

    @Column(name = "publications_journals", columnDefinition = "TEXT")
    private String publicationsJournals;

    @Column(name = "publications_conferences", columnDefinition = "TEXT")
    private String publicationsConferences;

    @Column(columnDefinition = "TEXT")
    private String education;

    @Column(columnDefinition = "TEXT")
    private String responsibilities;

    private String institution;

    @Column(name = "image")
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
    public String getDepartment() { return department; }
    public String getCampus() { return campus; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getResearchAreas() { return researchAreas; }
    public String getTeaching() { return teaching; }
    public String getAbout() { return about; }
    public String getPublicationsJournals() { return publicationsJournals; }
    public String getPublicationsConferences() { return publicationsConferences; }
    public String getEducation() { return education; }
    public String getResponsibilities() { return responsibilities; }
    public String getInstitution() { return institution; }
    public String getProfileUrl() { return profileUrl; }
    public String getDomain() { return domain; }
    public List<String> getKeywords() { return keywords; }
    public boolean isActive() { return active; }

    // ── Setters ──────────────────────────────────────────────
    public void setExpertId(Long expertId) { this.expertId = expertId; }
    public void setName(String name) { this.name = name; }
    public void setDesignation(String designation) { this.designation = designation; }
    public void setDepartment(String department) { this.department = department; }
    public void setCampus(String campus) { this.campus = campus; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setResearchAreas(String researchAreas) { this.researchAreas = researchAreas; }
    public void setTeaching(String teaching) { this.teaching = teaching; }
    public void setAbout(String about) { this.about = about; }
    public void setPublicationsJournals(String publicationsJournals) { this.publicationsJournals = publicationsJournals; }
    public void setPublicationsConferences(String publicationsConferences) { this.publicationsConferences = publicationsConferences; }
    public void setEducation(String education) { this.education = education; }
    public void setResponsibilities(String responsibilities) { this.responsibilities = responsibilities; }
    public void setInstitution(String institution) { this.institution = institution; }
    public void setProfileUrl(String profileUrl) { this.profileUrl = profileUrl; }
    public void setDomain(String domain) { this.domain = domain; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }
    public void setActive(boolean active) { this.active = active; }

    // ── Business methods ─────────────────────────────────────
    public void updateProfile(String newResearchAreas) {
        this.researchAreas = newResearchAreas;
        // AI extraction aggregates from all relevant fields now
        String aggregatedContent = (researchAreas != null ? researchAreas + " " : "") +
                                   (teaching != null ? teaching + " " : "") +
                                   (about != null ? about + " " : "") +
                                   (publicationsJournals != null ? publicationsJournals : "");
        this.keywords = extractKeywords(aggregatedContent);
    }

    private List<String> extractKeywords(String text) {
        if (text == null || text.equalsIgnoreCase("Not listed") || text.trim().isEmpty()) return new ArrayList<>();
        List<String> result = new ArrayList<>();
        String[] tokens = text.split("[\\s,.\n;]+");
        for (String token : tokens) {
            String cleaned = token.trim().toLowerCase();
            // Drop short stopwords and very long glitch strings
            if (cleaned.length() > 3 && cleaned.length() < 30) {
                if (!result.contains(cleaned)) {
                    result.add(cleaned);
                }
            }
        }
        return result;
    }

    public String getAggregatedProfileText() {
        return (researchAreas != null ? researchAreas + " " : "") +
               (teaching != null ? teaching + " " : "") +
               (about != null ? about + " " : "") +
               (publicationsJournals != null ? publicationsJournals : "");
    }

    public double scoreAgainst(String query) {
        if (query == null) return 0.0;
        String lowerQuery = query.toLowerCase();
        
        // Since Python imported the main table but skipped the "expert_keywords" JPA join-table,
        // the "keywords" list is empty! We must calculate them dynamically from the text.
        List<String> dynamicKeywords = extractKeywords(getAggregatedProfileText());
        
        long matchCount = dynamicKeywords.stream()
            .filter(k -> lowerQuery.contains(k) || k.contains(lowerQuery))
            .count();
            
        double boost = 1.0;
        if (researchAreas != null && researchAreas.toLowerCase().contains(lowerQuery)) boost += 1.0;
        if (publicationsJournals != null && publicationsJournals.toLowerCase().contains(lowerQuery)) boost += 0.5;
        
        return matchCount * boost;
    }
}
