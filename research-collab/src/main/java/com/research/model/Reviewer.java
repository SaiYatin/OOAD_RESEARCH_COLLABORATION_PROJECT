package com.research.model;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("REVIEWER")
public class Reviewer extends User {

    private String specialization;

    @Override
    public void register() { setRole(UserRole.REVIEWER); }

    @Override
    public boolean login(String email, String password) {
        return this.getEmail().equals(email) && this.getPassword().equals(password);
    }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public void reviewPaper(ResearchPaper paper) {
        paper.updateStatus(ResearchPaper.PaperStatus.UNDER_REVIEW);
    }

    public void provideFeedback(ResearchPaper paper, String feedback) {
        paper.setReviewNotes(feedback);
    }

    public void approveReject(ResearchPaper paper, boolean approved) {
        paper.updateStatus(approved
            ? ResearchPaper.PaperStatus.PUBLISHED
            : ResearchPaper.PaperStatus.REJECTED);
    }
}
