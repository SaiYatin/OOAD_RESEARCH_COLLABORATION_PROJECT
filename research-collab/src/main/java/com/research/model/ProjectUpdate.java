package com.research.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * ProjectUpdate — findings/updates posted to a research project.
 */
@Entity
@Table(name = "project_updates")
public class ProjectUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id", nullable = false)
    private ResearchProject project;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(updatable = false)
    private LocalDateTime postedAt;

    @PrePersist
    protected void onCreate() { postedAt = LocalDateTime.now(); }

    // Getters & Setters
    public Long getId() { return id; }
    public ResearchProject getProject() { return project; }
    public void setProject(ResearchProject project) { this.project = project; }
    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getPostedAt() { return postedAt; }
}
