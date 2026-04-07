package com.research.pattern;

import com.research.model.Expert;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Facade pattern - single entry point for the recommendation subsystem.
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

    public List<Expert> recommend(String userQuery, List<Expert> allExperts) {
        List<String> keywords = keywordExtractor.extract(userQuery);
        List<Expert> scored = expertScorer.score(allExperts, keywords);
        return resultRanker.topN(scored, keywords, 10);
    }
}
