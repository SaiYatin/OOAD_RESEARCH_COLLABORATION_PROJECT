package com.research.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

/**
 * Researcher - can create projects, upload papers, collaborate.
 * Maps to Researcher class in Class Diagram.
 * Design Principle: SRP - handles only researcher-specific behaviour.
 */
@Entity
@DiscriminatorValue("RESEARCHER")
@Getter
@Setter
public class Researcher extends User {

    @Column(columnDefinition = "TEXT")
    private String researchInterests;

    private String institution;

    private String department;

    @OneToMany(mappedBy = "researcher", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ResearchPaper> papers = new ArrayList<>();

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ResearchProject> projects = new ArrayList<>();

    // Tracks research threads the researcher follows (for email notifications)
    @ElementCollection
    @CollectionTable(name = "researcher_followed_keywords",
                     joinColumns = @JoinColumn(name = "researcher_id"))
    @Column(name = "keyword")
    private List<String> followedKeywords = new ArrayList<>();

    @Override
    public void register() {
        setRole(UserRole.RESEARCHER);
    }

    @Override
    public boolean login(String email, String password) {
        return this.getEmail().equals(email) && this.getPassword().equals(password);
    }

    // Class diagram methods
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
