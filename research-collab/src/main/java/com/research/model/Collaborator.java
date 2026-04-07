package com.research.model;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("COLLABORATOR")
public class Collaborator extends User {

    @Override
    public void register() { setRole(UserRole.COLLABORATOR); }

    @Override
    public boolean login(String email, String password) {
        return this.getEmail().equals(email) && this.getPassword().equals(password);
    }

    public void joinProject(ResearchProject project) {
        project.getMembers().add(this);
    }

    public void viewSharedFiles() { }
    public void communicate() { }
}
