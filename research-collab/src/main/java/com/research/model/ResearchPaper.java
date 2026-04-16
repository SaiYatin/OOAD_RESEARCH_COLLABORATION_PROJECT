package com.research.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ResearchPaper - central entity of the system.
 */
@Entity
@Table(name = "research_papers")
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
    private String keywords;

    private String domain;

    @Enumerated(EnumType.STRING)
    private PaperStatus status = PaperStatus.DRAFT;

    @Column(columnDefinition = "TEXT")
    private String reviewNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "researcher_id")
    private Researcher researcher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private User assignedReviewer;

    @Column(updatable = false)
    private LocalDateTime uploadedAt;

    private LocalDateTime updatedAt;

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

    // ── Getters ──────────────────────────────────────────────
    public Long getPaperId() { return paperId; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getAbstractText() { return abstractText; }
    public String getLink() { return link; }
    public String getKeywords() { return keywords; }
    public String getDomain() { return domain; }
    public PaperStatus getStatus() { return status; }
    public String getReviewNotes() { return reviewNotes; }
    public Researcher getResearcher() { return researcher; }
    public User getAssignedReviewer() { return assignedReviewer; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public List<CollaborationRequest> getCollaborationRequests() { return collaborationRequests; }

    // ── Setters ──────────────────────────────────────────────
    public void setPaperId(Long paperId) { this.paperId = paperId; }
    public void setTitle(String title) { this.title = title; }
    public void setAuthor(String author) { this.author = author; }
    public void setAbstractText(String abstractText) { this.abstractText = abstractText; }
    public void setLink(String link) { this.link = link; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    public void setDomain(String domain) { this.domain = domain; }
    public void setStatus(PaperStatus status) { this.status = status; }
    public void setReviewNotes(String reviewNotes) { this.reviewNotes = reviewNotes; }
    public void setResearcher(Researcher researcher) { this.researcher = researcher; }
    public void setAssignedReviewer(User reviewer) { this.assignedReviewer = reviewer; }

    // ── Business methods ─────────────────────────────────────
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
