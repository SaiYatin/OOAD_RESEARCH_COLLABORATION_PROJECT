package com.research.pattern;

import com.research.model.ResearchPaper;
import java.util.List;

/**
 * Component interface for the Decorator pattern - defines basic search contract.
 */
public interface PaperSearchComponent {
    List<ResearchPaper> search(String query, List<ResearchPaper> papers);
}
