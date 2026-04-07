package com.research.pattern;

import com.research.model.ResearchPaper;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * Observable subject - the paper publication event source.
 * Maintains a list of observers and fires events.
 */
@Component
public class PaperPublicationSubject {

    private final List<ResearchUpdateObserver> observers = new ArrayList<>();

    public void addObserver(ResearchUpdateObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(ResearchUpdateObserver observer) {
        observers.remove(observer);
    }

    public void notifyObservers(ResearchPaper paper, List<String> paperKeywords) {
        for (ResearchUpdateObserver observer : observers) {
            for (String keyword : paperKeywords) {
                observer.onNewPaper(paper, keyword);
            }
        }
    }
}
