package com.research.controller;

import com.research.model.*;
import com.research.repository.*;
import com.research.service.ExpertService;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * AdminController — Controller layer for Admin Panel use case.
 *
 * @author Member 4
 * @usecase Admin Dashboard, User/Expert/Project Management & System Statistics
 *
 * Design Pattern demonstrated: Singleton (Spring-managed controller instance)
 * Design Principle demonstrated: SRP — only handles admin operations
 *
 * MVC Role: Controller — mediates between AdminView (View) and Repositories (Model)
 */
@Controller
public class AdminController {

    private final UserRepository userRepository;
    private final ExpertRepository expertRepository;
    private final ResearchProjectRepository projectRepository;
    private final CollaborationRequestRepository requestRepository;
    private final ExpertService expertService;

    public AdminController(UserRepository userRepository,
                           ExpertRepository expertRepository,
                           ResearchProjectRepository projectRepository,
                           CollaborationRequestRepository requestRepository,
                           ExpertService expertService) {
        this.userRepository = userRepository;
        this.expertRepository = expertRepository;
        this.projectRepository = projectRepository;
        this.requestRepository = requestRepository;
        this.expertService = expertService;
    }

    /** Get all registered users. */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /** Get all active experts. */
    public List<Expert> getAllActiveExperts() {
        return expertService.getAllActiveExperts();
    }

    /** Get expert count. */
    public long getExpertCount() {
        return expertRepository.count();
    }

    /** Save/add an expert. */
    public Expert saveExpert(Expert expert) {
        return expertService.saveExpert(expert);
    }

    /** Get all projects. */
    public List<ResearchProject> getAllProjects() {
        return projectRepository.findAll();
    }

    /** Get system statistics. */
    public long getUserCount() { return userRepository.count(); }
    public long getProjectCount() { return projectRepository.count(); }
    public long getRequestCount() { return requestRepository.count(); }

    public long getActiveProjectCount() {
        return projectRepository.findByStatus(ResearchProject.ProjectStatus.ACTIVE).size();
    }

    public long getOpenCollabCount() {
        return projectRepository.findByLookingForCollaboratorsTrue().size();
    }
}
