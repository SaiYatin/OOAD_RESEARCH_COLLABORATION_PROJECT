package com.research.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * CollaborationRequest - manages the collaboration workflow.
 */
@Entity
@Table(name = "collaboration_requests")
public class CollaborationRequest {

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

    // ── Getters ──────────────────────────────────────────────
    public Long getRequestId() { return requestId; }
    public User getSender() { return sender; }
    public User getReceiver() { return receiver; }
    public ResearchPaper getPaper() { return paper; }
    public ResearchProject getProject() { return project; }
    public String getMessage() { return message; }
    public RequestStatus getStatus() { return status; }
    public LocalDateTime getSentAt() { return sentAt; }
    public LocalDateTime getRespondedAt() { return respondedAt; }

    // ── Setters ──────────────────────────────────────────────
    public void setRequestId(Long requestId) { this.requestId = requestId; }
    public void setSender(User sender) { this.sender = sender; }
    public void setReceiver(User receiver) { this.receiver = receiver; }
    public void setPaper(ResearchPaper paper) { this.paper = paper; }
    public void setProject(ResearchProject project) { this.project = project; }
    public void setMessage(String message) { this.message = message; }
    public void setStatus(RequestStatus status) { this.status = status; }

    // ── Business methods ─────────────────────────────────────
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
