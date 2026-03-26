package com.research.service;

import com.research.model.User;
import com.research.pattern.UserFactory;
import com.research.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

/**
 * AuthService - Member 1's primary use case.
 * Handles registration, login, role-based access.
 *
 * Design Principle: SRP - only handles authentication concerns.
 * Design Principle: DIP - depends on UserRepository abstraction, not concrete class.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserFactory userFactory;

    // Current logged-in user (session state for JavaFX)
    private static User currentUser;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       UserFactory userFactory) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userFactory = userFactory;
    }

    /**
     * Register a new user.
     * Uses Factory pattern to create the correct User subclass.
     */
    @Transactional
    public User register(String name, String email,
                         String password, User.UserRole role) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered: " + email);
        }
        // Factory creates correct subclass (Researcher/Admin/etc.)
        User user = userFactory.createUser(name, email,
                passwordEncoder.encode(password), role);
        return userRepository.save(user);
    }

    /**
     * Authenticate a user.
     * Returns the authenticated User or throws on failure.
     */
    public User login(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("No account found for: " + email);
        }
        User user = userOpt.get();
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Incorrect password.");
        }
        currentUser = user;
        return user;
    }

    public void logout() {
        currentUser = null;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static boolean hasRole(User.UserRole role) {
        return currentUser != null && currentUser.getRole() == role;
    }
}
