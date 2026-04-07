package com.research.model;

/**
 * DEPRECATED — Collaborator role removed.
 * Researchers collaborate naturally; this class is kept only to prevent
 * Hibernate mapping errors for any legacy DB rows.
 */
import jakarta.persistence.*;

@Entity
@DiscriminatorValue("COLLABORATOR")
public class Collaborator extends User {

    @Override
    public void register() {
        setRole(UserRole.RESEARCHER); // map legacy collaborators to researcher
    }

    @Override
    public boolean login(String email, String password) {
        return this.getEmail().equals(email) && this.getPassword().equals(password);
    }
}
