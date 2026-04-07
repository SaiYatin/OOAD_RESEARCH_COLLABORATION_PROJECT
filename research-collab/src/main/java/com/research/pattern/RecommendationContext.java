package com.research.pattern;

import com.research.model.Expert;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Strategy Context - holds and executes the current strategy.
 */
@Component
public class RecommendationContext {

    private RecommendationStrategy strategy;

    public RecommendationContext(KeywordMatchingStrategy defaultStrategy) {
        this.strategy = defaultStrategy;
    }

    public void setStrategy(RecommendationStrategy strategy) {
        this.strategy = strategy;
    }

    public List<Expert> execute(String query, List<Expert> experts, int topN) {
        return strategy.recommend(query, experts, topN);
    }

    public String currentStrategyName() {
        return strategy.strategyName();
    }
}
