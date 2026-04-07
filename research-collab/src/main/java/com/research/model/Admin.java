package com.research.model;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("ADMIN")
public class Admin extends User {

    @Override
    public void register() { setRole(UserRole.ADMIN); }

    @Override
    public boolean login(String email, String password) {
        return this.getEmail().equals(email) && this.getPassword().equals(password);
    }

    public void manageUsers() { }
    public void approveContent() { }
    public void monitorSystem() { }
}
