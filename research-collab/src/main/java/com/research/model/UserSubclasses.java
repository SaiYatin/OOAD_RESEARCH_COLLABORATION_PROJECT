package com.research.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

// ─────────────────────────────────────────────
//  Admin - manages users, approves content, monitors system
//  Maps directly to Admin class in Class Diagram
// ─────────────────────────────────────────────
@Entity
@DiscriminatorValue("ADMIN")
@Getter @Setter
class Admin extends User {

    @Override
    public void register() { setRole(UserRole.ADMIN); }

    @Override
    public boolean login(String email, String password) {
        return this.getEmail().equals(email) && this.getPassword().equals(password);
    }

    public void manageUsers() { /* Delegated to AdminService */ }

    public void approveContent() { /* Delegated to ContentService */ }

    public void monitorSystem() { /* Delegated to AdminService */ }
}

// ─────────────────────────────────────────────
//  Reviewer - reviews papers, provides feedback, approves/rejects
//  Maps directly to Reviewer class in Class Diagram
// ─────────────────────────────────────────────
@Entity
@DiscriminatorValue("REVIEWER")
@Getter @Setter
class Reviewer extends User {

    private String specialization;

    @Override
    public void register() { setRole(UserRole.REVIEWER); }

    @Override
    public boolean login(String email, String password) {
        return this.getEmail().equals(email) && this.getPassword().equals(password);
    }

    public void reviewPaper(ResearchPaper paper) {
        paper.setStatus(ResearchPaper.PaperStatus.UNDER_REVIEW);
    }

    public void provideFeedback(ResearchPaper paper, String feedback) {
        paper.setReviewNotes(feedback);
    }

    public void approveReject(ResearchPaper paper, boolean approved) {
        paper.setStatus(approved
            ? ResearchPaper.PaperStatus.PUBLISHED
            : ResearchPaper.PaperStatus.REJECTED);
    }
}

// ─────────────────────────────────────────────
//  Visitor - can search papers and view abstracts only
//  Maps directly to Visitor class in Class Diagram
// ─────────────────────────────────────────────
@Entity
@DiscriminatorValue("VISITOR")
@Getter @Setter
class Visitor extends User {

    @Override
    public void register() { setRole(UserRole.VISITOR); }

    @Override
    public boolean login(String email, String password) {
        return this.getEmail().equals(email) && this.getPassword().equals(password);
    }

    // Class diagram methods
    public void searchPapers() { /* Routed through PaperService */ }

    public void viewAbstract() { /* Read-only view */ }
}

// ─────────────────────────────────────────────
//  Collaborator - joins projects, views shared files, communicates
//  Maps directly to Collaborator class in Class Diagram
// ─────────────────────────────────────────────
@Entity
@DiscriminatorValue("COLLABORATOR")
@Getter @Setter
class Collaborator extends User {

    @Override
    public void register() { setRole(UserRole.COLLABORATOR); }

    @Override
    public boolean login(String email, String password) {
        return this.getEmail().equals(email) && this.getPassword().equals(password);
    }

    public void joinProject(ResearchProject project) {
        project.getMembers().add(this);
    }

    public void viewSharedFiles() { /* Delegated to ProjectService */ }

    public void communicate() { /* Messaging service */ }
}
