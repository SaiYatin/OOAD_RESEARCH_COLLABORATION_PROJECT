package com.research.pattern;

import com.research.model.Expert;
import com.research.model.ResearchPaper;
import org.springframework.stereotype.Component;
import java.util.List;

// ══════════════════════════════════════════════════════════════
//  STRUCTURAL PATTERNS - package-private implementations
//  Public classes split into: RecommendationFacade.java,
//  PaperSearchComponent.java, BasicPaperSearch.java,
//  DomainFilterDecorator.java, PublishedOnlyDecorator.java
// ══════════════════════════════════════════════════════════════

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
                      .filter(e -> e.isActive())
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

/**
 * Abstract Decorator - wraps a PaperSearchComponent.
 */
abstract class PaperSearchDecorator implements PaperSearchComponent {
    protected final PaperSearchComponent wrapped;

    protected PaperSearchDecorator(PaperSearchComponent wrapped) {
        this.wrapped = wrapped;
    }
}
