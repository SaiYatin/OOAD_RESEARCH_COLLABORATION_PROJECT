package com.research.pattern;

import com.research.model.ResearchPaper;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Concrete Decorator 2 - adds published-only filter.
 */
@Component
public class PublishedOnlyDecorator extends PaperSearchDecorator {
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
