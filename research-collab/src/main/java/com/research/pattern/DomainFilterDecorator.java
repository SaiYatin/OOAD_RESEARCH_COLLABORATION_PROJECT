package com.research.pattern;

import com.research.model.ResearchPaper;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Concrete Decorator 1 - adds domain filter on top of base search.
 */
@Component
public class DomainFilterDecorator extends PaperSearchDecorator {
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
