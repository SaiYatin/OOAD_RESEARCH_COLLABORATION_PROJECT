package com.research.pattern;

import com.research.model.ResearchPaper;

/**
 * Observer interface - any listener that reacts to paper events.
 */
public interface ResearchUpdateObserver {
    void onNewPaper(ResearchPaper paper, String matchedKeyword);
}
