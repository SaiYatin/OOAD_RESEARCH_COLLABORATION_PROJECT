package com.research.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * PublicOpinion — feedback from followers on public research projects.
 */
@Entity
@Table(name = "public_opinions")
public class PublicOpinion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id", nullable = false)
    private ResearchProject project;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(updatable = false)
    private LocalDateTime postedAt;

    @PrePersist
    protected void onCreate() { postedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public ResearchProject getProject() { return project; }
    public void setProject(ResearchProject project) { this.project = project; }
    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getPostedAt() { return postedAt; }
}
