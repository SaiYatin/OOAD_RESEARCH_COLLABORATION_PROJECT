package com.research.model;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("VISITOR")
public class Visitor extends User {

    @Override
    public void register() { setRole(UserRole.VISITOR); }

    @Override
    public boolean login(String email, String password) {
        return this.getEmail().equals(email) && this.getPassword().equals(password);
    }

    public void searchPapers() { }
    public void viewAbstract() { }
}
