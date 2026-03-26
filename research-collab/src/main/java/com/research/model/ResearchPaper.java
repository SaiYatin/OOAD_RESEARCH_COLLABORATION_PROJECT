package com.research.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ResearchPaper - central entity of the system.
 * Maps to ResearchPaper class in Class Diagram.
 * Fields: paperId, title, author, link, status
 */
@Entity
@Table(name = "research_papers")
@Getter
@Setter
public class ResearchPaper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paperId;

    @Column(nullable = false)
    private String title;

    private String author;

    @Column(columnDefinition = "TEXT")
    private String abstractText;

    private String link;

    @Column(columnDefinition = "TEXT")
    private String keywords;   // comma-separated - drives Observer notifications

    private String domain;     // e.g. "Machine Learning", "NLP", "Robotics"

    @Enumerated(EnumType.STRING)
    private PaperStatus status = PaperStatus.DRAFT;

    @Column(columnDefinition = "TEXT")
    private String reviewNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "researcher_id")
    private Researcher researcher;

    @Column(updatable = false)
    private LocalDateTime uploadedAt;

    private LocalDateTime updatedAt;

    // Linked collaboration requests
    @OneToMany(mappedBy = "paper", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CollaborationRequest> collaborationRequests = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Class diagram methods
    public String getDetails() {
        return String.format("Title: %s | Author: %s | Domain: %s | Status: %s",
                title, author, domain, status);
    }

    public void updateStatus(PaperStatus newStatus) {
        this.status = newStatus;
    }

    public enum PaperStatus {
        DRAFT, SUBMITTED, UNDER_REVIEW, PUBLISHED, REJECTED
    }
}
