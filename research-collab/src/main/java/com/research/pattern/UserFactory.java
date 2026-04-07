package com.research.pattern;

import com.research.model.*;
import org.springframework.stereotype.Component;

/**
 * Factory Method pattern - centralises User object creation.
 */
@Component
public class UserFactory {

    public User createUser(String name, String email, String password,
                           User.UserRole role) {
        User user = switch (role) {
            case RESEARCHER   -> new Researcher();
            case ADMIN        -> new Admin();
            case REVIEWER     -> new Reviewer();
            case VISITOR      -> new Visitor();
        };
        user.setName(name);
        user.setEmail(email);
        user.setPassword(password);
        user.register();
        return user;
    }
}
