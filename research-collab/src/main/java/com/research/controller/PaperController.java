package com.research.controller;

import com.research.model.ResearchPaper;
import com.research.model.Researcher;
import com.research.service.AuthService;
import com.research.service.PaperService;
import com.research.pattern.BasicPaperSearch;
import com.research.pattern.DomainFilterDecorator;
import com.research.pattern.PublishedOnlyDecorator;
import com.research.pattern.PaperSearchComponent;
import com.research.repository.ResearchPaperRepository;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * PaperController — Controller layer for Paper Search, Upload & Review use case.
 *
 * @author Member 1
 * @usecase Paper Upload, Search & Review
 *
 * Design Pattern demonstrated: Decorator (search filter chain)
 * Design Principle demonstrated: SRP — only handles paper-related operations
 *
 * MVC Role: Controller — mediates between PaperSearchView (View) and
 *           PaperService + Repositories (Model)
 */
@Controller
public class PaperController {

    private final PaperService paperService;
    private final ResearchPaperRepository paperRepository;
    private final BasicPaperSearch basicSearch;
    private final DomainFilterDecorator domainDecorator;
    private final PublishedOnlyDecorator publishedDecorator;

    public PaperController(PaperService paperService,
                           ResearchPaperRepository paperRepository,
                           BasicPaperSearch basicSearch,
                           DomainFilterDecorator domainDecorator,
                           PublishedOnlyDecorator publishedDecorator) {
        this.paperService = paperService;
        this.paperRepository = paperRepository;
        this.basicSearch = basicSearch;
        this.domainDecorator = domainDecorator;
        this.publishedDecorator = publishedDecorator;
    }

    /** Get all papers in the database. */
    public List<ResearchPaper> getAllPapers() {
        return paperRepository.findAll();
    }

    /**
     * Search papers using the Decorator pattern chain.
     * Applies filters based on user selections.
     */
    public List<ResearchPaper> searchPapers(String query, boolean publishedOnly, String domainFilter) {
        List<ResearchPaper> papers = paperRepository.findAll();

        if (publishedOnly && domainFilter != null && !domainFilter.isBlank()) {
            return domainDecorator.withDomain(domainFilter)
                    .search(query, publishedDecorator.search(query, papers));
        } else if (publishedOnly) {
            return publishedDecorator.search(query, papers);
        } else if (domainFilter != null && !domainFilter.isBlank()) {
            return domainDecorator.withDomain(domainFilter).search(query, papers);
        } else {
            return basicSearch.search(query, papers);
        }
    }

    /** Upload a new paper as DRAFT. */
    public ResearchPaper uploadPaper(ResearchPaper paper) {
        Researcher currentResearcher = (Researcher) AuthService.getCurrentUser();
        return paperService.uploadPaper(paper, currentResearcher);
    }

    /** Submit a draft paper for review (DRAFT → SUBMITTED). */
    public ResearchPaper submitForReview(Long paperId) {
        return paperService.submitForReview(paperId);
    }

    /** Publish a paper (SUBMITTED/UNDER_REVIEW → PUBLISHED). */
    public ResearchPaper publishPaper(Long paperId) {
        return paperService.publishPaper(paperId);
    }

    /** Delete a paper. */
    public void deletePaper(Long paperId) {
        paperService.deletePaper(paperId);
    }

    /** Get papers owned by the current logged-in researcher. */
    public List<ResearchPaper> getMyPapers() {
        Researcher currentResearcher = (Researcher) AuthService.getCurrentUser();
        return paperService.getPapersByResearcher(currentResearcher);
    }

    /** Get papers by status (for reviewer dashboard). */
    public List<ResearchPaper> getPapersByStatus(ResearchPaper.PaperStatus status) {
        return paperRepository.findByStatus(status);
    }

    /** Save a paper entity directly (for reviewer feedback/status updates). */
    public ResearchPaper savePaper(ResearchPaper paper) {
        return paperRepository.save(paper);
    }

    /** Search papers by query string. */
    public List<ResearchPaper> searchByQuery(String query) {
        return paperRepository.searchByQuery(query);
    }

    /** Get the currently logged-in user. */
    public com.research.model.User getCurrentUser() {
        return AuthService.getCurrentUser();
    }
}
