package com.research.pattern;

import com.research.model.Expert;
import org.springframework.stereotype.Component;
import org.springframework.context.ApplicationContext;
import java.util.List;

/**
 * Strategy Context - holds and executes the current strategy.
 */
@Component
public class RecommendationContext {

    private RecommendationStrategy strategy;
    private final ApplicationContext applicationContext;

    public RecommendationContext(KeywordMatchingStrategy defaultStrategy, ApplicationContext applicationContext) {
        this.strategy = defaultStrategy;
        this.applicationContext = applicationContext;
    }

    public void setStrategyByMode(String mode) {
        if ("ai".equals(mode)) {
            this.strategy = applicationContext.getBean("bigQuerySemanticStrategy", RecommendationStrategy.class);
        } else {
            this.strategy = applicationContext.getBean("keywordMatchingStrategy", RecommendationStrategy.class);
        }
    }

    public List<Expert> execute(String query, List<Expert> experts, int topN) {
        return strategy.recommend(query, experts, topN);
    }

    public String currentStrategyName() {
        return strategy.strategyName();
    }
}
