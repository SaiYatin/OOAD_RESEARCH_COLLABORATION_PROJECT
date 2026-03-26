package com.research.service;

import com.research.model.*;
import com.research.pattern.*;
import com.research.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * RecommendationService - Member 4's primary use case.
 * Uses RecommendationFacade (Structural) + RecommendationContext/Strategy (Behavioral).
 *
 * Design Principle: DIP - depends on abstractions (Facade, Strategy interface).
 * Design Principle: OCP - new strategies added without changing this service.
 */
@Service
public class RecommendationService {

    private final ExpertRepository expertRepository;
    private final RecommendationFacade recommendationFacade;
    private final RecommendationContext recommendationContext;
    private final PaperPublicationSubject paperPublicationSubject;

    public RecommendationService(ExpertRepository expertRepository,
                                  RecommendationFacade recommendationFacade,
                                  RecommendationContext recommendationContext,
                                  PaperPublicationSubject paperPublicationSubject) {
        this.expertRepository = expertRepository;
        this.recommendationFacade = recommendationFacade;
        this.recommendationContext = recommendationContext;
        this.paperPublicationSubject = paperPublicationSubject;
    }

    /**
     * Recommend experts for a given research query.
     * Routes through the Facade which hides all subsystem complexity.
     */
    public List<Expert> recommendExperts(String query) {
        List<Expert> allExperts = expertRepository.findByActiveTrue();
        return recommendationFacade.recommend(query, allExperts);
    }

    /**
     * Strategy-based recommendation with mode selection.
     * Member 4's UI lets user pick the strategy (keyword/AI/hybrid).
     */
    public List<Expert> recommendWithStrategy(String query, String strategyMode) {
        List<Expert> allExperts = expertRepository.findByActiveTrue();
        return recommendationContext.execute(query, allExperts, 10);
    }

    /**
     * Called when a new paper is added/published.
     * Fires the Observer pattern → email notifications to followers.
     */
    public void onNewPaperPublished(ResearchPaper paper) {
        // Build keyword list from paper
        List<String> keywords = List.of();
        if (paper.getKeywords() != null) {
            keywords = List.of(paper.getKeywords().split("[,;]+"))
                           .stream()
                           .map(String::trim)
                           .filter(k -> !k.isBlank())
                           .toList();
        }
        if (paper.getDomain() != null) {
            keywords = new java.util.ArrayList<>(keywords);
            ((java.util.ArrayList<String>) keywords).add(paper.getDomain());
        }
        // Notify all observers (Observer pattern fires here)
        paperPublicationSubject.notifyObservers(paper, keywords);
    }

    public String getCurrentStrategyName() {
        return recommendationContext.currentStrategyName();
    }
}


/**
 * NotificationService - in-app notification store.
 * Used by Collaboration and Recommendation services.
 * Design Principle: SRP - only stores/retrieves notifications.
 */
@Service
class NotificationService {

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager em;

    public void notify(User recipient, String message, String type) {
        // Persist notification to DB for in-app display
        // Simple implementation - can be expanded with WebSocket push
        System.out.printf("[NOTIFICATION → %s] [%s] %s%n",
                recipient.getEmail(), type, message);
    }
}


/**
 * PaperService - paper upload, search, review workflow.
 * Minor use case shared across Member 1 (search) and Member 2 (upload).
 */
@Service
class PaperService {

    private final ResearchPaperRepository paperRepository;
    private final RecommendationService recommendationService;

    PaperService(ResearchPaperRepository paperRepository,
                 RecommendationService recommendationService) {
        this.paperRepository = paperRepository;
        this.recommendationService = recommendationService;
    }

    public List<ResearchPaper> searchPapers(String query) {
        return paperRepository.searchByQuery(query);
    }

    public List<ResearchPaper> getPublishedPapers() {
        return paperRepository.findByStatus(ResearchPaper.PaperStatus.PUBLISHED);
    }

    @Transactional
    public ResearchPaper uploadPaper(ResearchPaper paper, Researcher uploader) {
        uploader.uploadPaper(paper);
        ResearchPaper saved = paperRepository.save(paper);
        return saved;
    }

    @Transactional
    public ResearchPaper publishPaper(Long paperId) {
        ResearchPaper paper = paperRepository.findById(paperId)
            .orElseThrow(() -> new IllegalArgumentException("Paper not found"));
        paper.updateStatus(ResearchPaper.PaperStatus.PUBLISHED);
        ResearchPaper saved = paperRepository.save(paper);
        // Fire Observer → email notifications
        recommendationService.onNewPaperPublished(saved);
        return saved;
    }
}
