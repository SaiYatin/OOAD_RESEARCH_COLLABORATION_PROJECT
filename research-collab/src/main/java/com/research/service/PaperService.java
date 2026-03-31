package com.research.service;

import com.research.model.ResearchPaper;
import com.research.model.Researcher;
import com.research.repository.ResearchPaperRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * PaperService (public) — Member 1's minor use case.
 * Handles paper upload, search, status changes, and publish flow.
 * Extracted from RecommendationService file so views can inject it directly.
 *
 * Design Principle: SRP — only manages paper lifecycle.
 * Design Principle: DIP — depends on repository interface, not impl.
 */
@Service
public class PaperService {

    private final ResearchPaperRepository paperRepository;
    private final RecommendationService recommendationService;

    public PaperService(ResearchPaperRepository paperRepository,
                        RecommendationService recommendationService) {
        this.paperRepository = paperRepository;
        this.recommendationService = recommendationService;
    }

    public List<ResearchPaper> searchPapers(String query) {
        return paperRepository.searchByQuery(query);
    }

    public List<ResearchPaper> getAllPapers() {
        return paperRepository.findAll();
    }

    public List<ResearchPaper> getPublishedPapers() {
        return paperRepository.findByStatus(ResearchPaper.PaperStatus.PUBLISHED);
    }

    public List<ResearchPaper> getPapersByResearcher(Researcher researcher) {
        return paperRepository.findByResearcher(researcher);
    }

    /**
     * Upload a new paper — Member 1's primary extra feature.
     * Sets the researcher, saves to DB, status starts as DRAFT.
     */
    @Transactional
    public ResearchPaper uploadPaper(ResearchPaper paper, Researcher uploader) {
        paper.setResearcher(uploader);
        paper.setStatus(ResearchPaper.PaperStatus.DRAFT);
        return paperRepository.save(paper);
    }

    /**
     * Submit a draft paper for review.
     * Status: DRAFT → SUBMITTED
     */
    @Transactional
    public ResearchPaper submitForReview(Long paperId) {
        ResearchPaper paper = getById(paperId);
        if (paper.getStatus() != ResearchPaper.PaperStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT papers can be submitted.");
        }
        paper.updateStatus(ResearchPaper.PaperStatus.SUBMITTED);
        return paperRepository.save(paper);
    }

    /**
     * Publish a paper (admin/reviewer action).
     * Fires Observer → email notifications to keyword followers.
     */
    @Transactional
    public ResearchPaper publishPaper(Long paperId) {
        ResearchPaper paper = getById(paperId);
        paper.updateStatus(ResearchPaper.PaperStatus.PUBLISHED);
        ResearchPaper saved = paperRepository.save(paper);
        recommendationService.onNewPaperPublished(saved);
        return saved;
    }

    /**
     * Delete a paper entirely.
     */
    @Transactional
    public void deletePaper(Long paperId) {
        paperRepository.deleteById(paperId);
    }

    /**
     * Update paper details.
     */
    @Transactional
    public ResearchPaper updatePaper(Long paperId, String title, String abstractText,
                                      String keywords, String domain, String link) {
        ResearchPaper paper = getById(paperId);
        if (title != null && !title.isBlank())         paper.setTitle(title);
        if (abstractText != null)                       paper.setAbstractText(abstractText);
        if (keywords != null)                           paper.setKeywords(keywords);
        if (domain != null && !domain.isBlank())        paper.setDomain(domain);
        if (link != null)                               paper.setLink(link);
        return paperRepository.save(paper);
    }

    public ResearchPaper getById(Long paperId) {
        return paperRepository.findById(paperId)
            .orElseThrow(() -> new IllegalArgumentException("Paper not found: " + paperId));
    }
}
