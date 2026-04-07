package com.research.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ResearchProject - maps to ResearchProject in Class Diagram.
 */
@Entity
@Table(name = "research_projects")
public class ResearchProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long projectId;

    @Column(nullable = false)
    private String topic;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String domain;

    @Enumerated(EnumType.STRING)
    private ProjectStatus status = ProjectStatus.ACTIVE;

    private boolean lookingForCollaborators = false;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_id")
    private Researcher owner;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "project_members",
        joinColumns = @JoinColumn(name = "project_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> members = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── Getters ──────────────────────────────────────────────
    public Long getProjectId() { return projectId; }
    public String getTopic() { return topic; }
    public String getDescription() { return description; }
    public String getDomain() { return domain; }
    public ProjectStatus getStatus() { return status; }
    public boolean isLookingForCollaborators() { return lookingForCollaborators; }
    public Researcher getOwner() { return owner; }
    public List<User> getMembers() { return members; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // ── Setters ──────────────────────────────────────────────
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public void setTopic(String topic) { this.topic = topic; }
    public void setDescription(String description) { this.description = description; }
    public void setDomain(String domain) { this.domain = domain; }
    public void setStatus(ProjectStatus status) { this.status = status; }
    public void setLookingForCollaborators(boolean lookingForCollaborators) { this.lookingForCollaborators = lookingForCollaborators; }
    public void setOwner(Researcher owner) { this.owner = owner; }
    public void setMembers(List<User> members) { this.members = members; }

    // ── Business methods ─────────────────────────────────────
    public void addMember(User user) {
        if (!members.contains(user)) {
            members.add(user);
        }
    }

    public void uploadData(String dataDescription) {
        this.description = (this.description == null ? "" : this.description)
                + "\n[Data] " + dataDescription;
    }

    public enum ProjectStatus {
        ACTIVE, COMPLETED, ARCHIVED
    }
}
