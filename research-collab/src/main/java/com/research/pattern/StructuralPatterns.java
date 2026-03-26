package com.research.pattern;

import com.research.model.Expert;
import com.research.model.ResearchPaper;
import org.springframework.stereotype.Component;
import java.util.List;

// ══════════════════════════════════════════════════════════════
//  STRUCTURAL PATTERN 1: FACADE
//  RecommendationFacade hides the complexity of the multi-step
//  recommendation pipeline behind a single clean interface.
//  Applied to: Expert recommendation feature (Member 4's use case).
// ══════════════════════════════════════════════════════════════

/**
 * Facade pattern - single entry point for the recommendation subsystem.
 *
 * HOW APPLIED: The JavaFX controller calls ONE method: recommend(query).
 * Internally this coordinates: keyword extraction → expert scoring →
 * ranking → filtering inactive experts → result formatting.
 * The controller knows nothing about these steps.
 */
@Component
public class RecommendationFacade {

    private final KeywordExtractor keywordExtractor;
    private final ExpertScorer expertScorer;
    private final ResultRanker resultRanker;

    public RecommendationFacade(KeywordExtractor keywordExtractor,
                                ExpertScorer expertScorer,
                                ResultRanker resultRanker) {
        this.keywordExtractor = keywordExtractor;
        this.expertScorer = expertScorer;
        this.resultRanker = resultRanker;
    }

    /**
     * Single facade method - hides all subsystem complexity.
     */
    public List<Expert> recommend(String userQuery, List<Expert> allExperts) {
        // Step 1: extract meaningful keywords from raw query
        List<String> keywords = keywordExtractor.extract(userQuery);

        // Step 2: score each expert against extracted keywords
        List<Expert> scored = expertScorer.score(allExperts, keywords);

        // Step 3: rank and return top results
        return resultRanker.topN(scored, keywords, 10);
    }
}

// Sub-components of the recommendation subsystem (hidden by Facade)
@Component
class KeywordExtractor {
    public List<String> extract(String query) {
        if (query == null || query.isBlank()) return List.of();
        return List.of(query.toLowerCase()
                           .split("[\\s,;.]+"))
                   .stream()
                   .filter(w -> w.length() > 2)
                   .toList();
    }
}

@Component
class ExpertScorer {
    public List<Expert> score(List<Expert> experts, List<String> keywords) {
        return experts.stream()
                      .filter(Expert::isActive)
                      .filter(e -> keywords.stream()
                              .anyMatch(k -> e.scoreAgainst(k) > 0))
                      .toList();
    }
}

@Component
class ResultRanker {
    public List<Expert> topN(List<Expert> experts, List<String> keywords, int n) {
        String combined = String.join(" ", keywords);
        return experts.stream()
                      .sorted((a, b) -> Double.compare(
                              b.scoreAgainst(combined),
                              a.scoreAgainst(combined)))
                      .limit(n)
                      .toList();
    }
}


// ══════════════════════════════════════════════════════════════
//  STRUCTURAL PATTERN 2: DECORATOR
//  PaperSearchDecorator adds optional filters (domain, status)
//  on top of the base search without changing the base class.
//  Applied to: Paper search feature (Member 1's use case).
// ══════════════════════════════════════════════════════════════

/**
 * Component interface - defines basic search contract.
 */
interface PaperSearchComponent {
    List<ResearchPaper> search(String query, List<ResearchPaper> papers);
}

/**
 * Concrete base component - plain keyword search.
 */
@Component
class BasicPaperSearch implements PaperSearchComponent {
    @Override
    public List<ResearchPaper> search(String query, List<ResearchPaper> papers) {
        String q = query.toLowerCase();
        return papers.stream()
                     .filter(p -> p.getTitle().toLowerCase().contains(q)
                               || (p.getAbstractText() != null
                                   && p.getAbstractText().toLowerCase().contains(q))
                               || (p.getKeywords() != null
                                   && p.getKeywords().toLowerCase().contains(q)))
                     .toList();
    }
}

/**
 * Abstract Decorator - wraps a PaperSearchComponent.
 */
abstract class PaperSearchDecorator implements PaperSearchComponent {
    protected final PaperSearchComponent wrapped;

    protected PaperSearchDecorator(PaperSearchComponent wrapped) {
        this.wrapped = wrapped;
    }
}

/**
 * Concrete Decorator 1 - adds domain filter on top of base search.
 */
@Component
class DomainFilterDecorator extends PaperSearchDecorator {
    private String domainFilter;

    public DomainFilterDecorator(PaperSearchComponent wrapped) {
        super(wrapped);
    }

    public DomainFilterDecorator withDomain(String domain) {
        this.domainFilter = domain;
        return this;
    }

    @Override
    public List<ResearchPaper> search(String query, List<ResearchPaper> papers) {
        List<ResearchPaper> baseResults = wrapped.search(query, papers);
        if (domainFilter == null || domainFilter.isBlank()) return baseResults;
        return baseResults.stream()
                          .filter(p -> domainFilter.equalsIgnoreCase(p.getDomain()))
                          .toList();
    }
}

/**
 * Concrete Decorator 2 - adds published-only filter.
 */
@Component
class PublishedOnlyDecorator extends PaperSearchDecorator {
    public PublishedOnlyDecorator(PaperSearchComponent wrapped) {
        super(wrapped);
    }

    @Override
    public List<ResearchPaper> search(String query, List<ResearchPaper> papers) {
        return wrapped.search(query, papers).stream()
                      .filter(p -> ResearchPaper.PaperStatus.PUBLISHED
                                                .equals(p.getStatus()))
                      .toList();
    }
}
