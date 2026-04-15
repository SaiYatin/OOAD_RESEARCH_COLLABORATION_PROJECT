package com.research.controller;

import com.research.model.User;
import com.research.service.AuthService;
import org.springframework.stereotype.Controller;

/**
 * AuthController — Controller layer for Authentication & User Management use case.
 *
 * @author Member 4
 * @usecase User Registration, Login, Session Management & Role-Based Access
 *
 * Design Pattern demonstrated: Factory Method (UserFactory creates role-specific User subclasses)
 * Design Principle demonstrated: LSP — all User subclasses (Researcher, Reviewer, Admin, Visitor)
 *                                  are substitutable wherever a User is expected
 *
 * MVC Role: Controller — mediates between LoginView/DashboardView (View)
 *           and AuthService (Model)
 */
@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Register a new user.
     * Uses Factory Method pattern internally to create correct User subclass.
     */
    public User register(String name, String email, String password, User.UserRole role) {
        return authService.register(name, email, password, role);
    }

    /** Authenticate a user and start a session. */
    public User login(String email, String password) {
        return authService.login(email, password);
    }

    /** End the current user session. */
    public void logout() {
        authService.logout();
    }

    /** Get the currently logged-in user (null if visitor). */
    public User getCurrentUser() {
        return AuthService.getCurrentUser();
    }

    /** Check if any user is logged in. */
    public boolean isLoggedIn() {
        return AuthService.isLoggedIn();
    }

    /** Check if the current user has a specific role. */
    public boolean hasRole(User.UserRole role) {
        return AuthService.hasRole(role);
    }
}
