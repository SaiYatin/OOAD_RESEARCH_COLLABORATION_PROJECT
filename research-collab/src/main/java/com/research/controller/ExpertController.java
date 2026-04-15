package com.research.controller;

import com.research.model.Expert;
import com.research.model.Researcher;
import com.research.model.ResearchProject;
import com.research.model.User;
import com.research.repository.ResearchProjectRepository;
import com.research.repository.ResearcherRepository;
import com.research.service.AuthService;
import com.research.service.CollaborationService;
import com.research.service.ExpertService;
import com.research.service.RecommendationService;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;

/**
 * ExpertController — Controller layer for Expert Search & Recommendation use case.
 *
 * @author Member 2
 * @usecase Expert Search, Recommendation & Expert CRUD
 *
 * Design Pattern demonstrated: Strategy (swappable recommendation algorithms)
 * Design Principle demonstrated: OCP — new strategies can be added without modifying existing code
 *
 * MVC Role: Controller — mediates between ExpertSearchView (View) and
 *           ExpertService + RecommendationService (Model)
 */
@Controller
public class ExpertController {

    private final RecommendationService recommendationService;
    private final ExpertService expertService;
    private final CollaborationService collaborationService;
    private final ResearcherRepository researcherRepository;
    private final ResearchProjectRepository projectRepository;

    public ExpertController(RecommendationService recommendationService,
                            ExpertService expertService,
                            CollaborationService collaborationService,
                            ResearcherRepository researcherRepository,
                            ResearchProjectRepository projectRepository) {
        this.recommendationService = recommendationService;
        this.expertService = expertService;
        this.collaborationService = collaborationService;
        this.researcherRepository = researcherRepository;
        this.projectRepository = projectRepository;
    }

    /**
     * Find experts using the Strategy pattern.
     * @param query search query
     * @param mode "keyword", "ai", or "hybrid"
     * @return ranked list of matching experts
     */
    public List<Expert> findExperts(String query, String mode) {
        return recommendationService.recommendWithStrategy(query, mode);
    }

    /** Search registered researchers by keyword. */
    public List<Researcher> findResearchers(String query) {
        return researcherRepository.findByKeyword(query);
    }

    /** Get all experts for browse view. */
    public List<Expert> getAllExperts() {
        return expertService.getAllExperts();
    }

    /** Search experts by keyword. */
    public List<Expert> searchExperts(String keyword) {
        return expertService.searchExperts(keyword);
    }

    /** Update expert's research areas. */
    public Expert updateExpertProfile(Long expertId, String researchAreas) {
        return expertService.updateResearchProfile(expertId, researchAreas);
    }

    /** Toggle expert active status. */
    public Expert toggleExpertStatus(Long expertId) {
        return expertService.toggleActive(expertId);
    }

    /** Add a new expert manually. */
    public Expert addExpert(String name, String designation, String email,
                            String phone, String institution, String researchAreas) {
        return expertService.addExpert(name, designation, email, phone, institution, researchAreas);
    }

    /** Get projects owned by the current user (for collaboration request). */
    public List<ResearchProject> getMyProjects() {
        User currentUser = AuthService.getCurrentUser();
        if (currentUser instanceof Researcher) {
            return projectRepository.findByOwner((Researcher) currentUser);
        }
        return new ArrayList<>();
    }

    /**
     * Send a collaboration request from current user to a researcher for a project.
     * Delegates to CollaborationService.
     */
    public void sendCollaborationRequest(Long receiverId, Long projectId, String message) {
        User currentUser = AuthService.getCurrentUser();
        collaborationService.sendRequest(
                currentUser.getUserId(), receiverId, projectId, message);
    }

    /** Get the currently logged-in user. */
    public User getCurrentUser() {
        return AuthService.getCurrentUser();
    }
}
