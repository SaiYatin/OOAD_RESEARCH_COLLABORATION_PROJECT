package com.research.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ResearchProject - maps to ResearchProject in Class Diagram.
 * Fields: projectId, topic, members
 * Methods: addMember(), uploadData()
 */
@Entity
@Table(name = "research_projects")
@Getter
@Setter
public class ResearchProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long projectId;

    @Column(nullable = false)
    private String topic;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private Researcher owner;

    // List<User> members from class diagram
    @ManyToMany
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

    // Class diagram methods
    public void addMember(User user) {
        if (!members.contains(user)) {
            members.add(user);
        }
    }

    public void uploadData(String dataDescription) {
        this.description = (this.description == null ? "" : this.description)
                + "\n[Data] " + dataDescription;
    }
}

/**
 * CollaborationRequest - manages the collaboration workflow.
 * Sent by Researcher/Collaborator to join a project or work with an expert.
 */
@Entity
@Table(name = "collaboration_requests")
@Getter
@Setter
class CollaborationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paper_id")
    private ResearchPaper paper;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private ResearchProject project;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    private RequestStatus status = RequestStatus.PENDING;

    private LocalDateTime sentAt;
    private LocalDateTime respondedAt;

    @PrePersist
    protected void onCreate() { sentAt = LocalDateTime.now(); }

    public void accept() {
        this.status = RequestStatus.ACCEPTED;
        this.respondedAt = LocalDateTime.now();
    }

    public void reject() {
        this.status = RequestStatus.REJECTED;
        this.respondedAt = LocalDateTime.now();
    }

    public enum RequestStatus {
        PENDING, ACCEPTED, REJECTED
    }
}
