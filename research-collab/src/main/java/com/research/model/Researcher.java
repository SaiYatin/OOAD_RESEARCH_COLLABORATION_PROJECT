package com.research.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Researcher - can create projects, upload papers, collaborate.
 */
@Entity
@DiscriminatorValue("RESEARCHER")
public class Researcher extends User {

    @Column(columnDefinition = "TEXT")
    private String researchInterests;

    private String institution;

    private String department;

    @OneToMany(mappedBy = "researcher", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<ResearchPaper> papers = new ArrayList<>();

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<ResearchProject> projects = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "researcher_followed_keywords",
                     joinColumns = @JoinColumn(name = "researcher_id"))
    @Column(name = "keyword")
    private List<String> followedKeywords = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "researcher_interested_domains",
                     joinColumns = @JoinColumn(name = "researcher_id"))
    @Column(name = "domain")
    private List<String> interestedDomains = new ArrayList<>();

    @Override
    public void register() {
        setRole(UserRole.RESEARCHER);
    }

    @Override
    public boolean login(String email, String password) {
        return this.getEmail().equals(email) && this.getPassword().equals(password);
    }

    // ── Getters ──────────────────────────────────────────────
    public String getResearchInterests() { return researchInterests; }
    public String getInstitution() { return institution; }
    public String getDepartment() { return department; }
    public List<ResearchPaper> getPapers() { return papers; }
    public List<ResearchProject> getProjects() { return projects; }
    public List<String> getFollowedKeywords() { return followedKeywords; }
    public List<String> getInterestedDomains() { return interestedDomains; }
    public void setInterestedDomains(List<String> interestedDomains) { this.interestedDomains = interestedDomains; }

    // ── Setters ──────────────────────────────────────────────
    public void setResearchInterests(String researchInterests) { this.researchInterests = researchInterests; }
    public void setInstitution(String institution) { this.institution = institution; }
    public void setDepartment(String department) { this.department = department; }

    // ── Business methods ─────────────────────────────────────
    public ResearchProject createResearchProject(String topic) {
        ResearchProject project = new ResearchProject();
        project.setTopic(topic);
        project.setOwner(this);
        return project;
    }

    public void uploadPaper(ResearchPaper paper) {
        paper.setResearcher(this);
        this.papers.add(paper);
    }

    public void collaborate(ResearchProject project) {
        project.getMembers().add(this);
    }
}
