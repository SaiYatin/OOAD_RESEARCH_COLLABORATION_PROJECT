package com.research.pattern;

import com.research.model.ResearchPaper;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Concrete base component - plain keyword search.
 */
@Component
@Primary
public class BasicPaperSearch implements PaperSearchComponent {
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
