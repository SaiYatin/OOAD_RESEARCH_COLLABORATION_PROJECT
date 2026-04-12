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
        System.out.println(">>> [RecommendationFacade] recommend() called for query: '" + userQuery + "'");
        List<String> keywords = keywordExtractor.extract(userQuery);
        System.out.println(">>> [RecommendationFacade] Extracted keywords: " + keywords);
        List<Expert> scored = expertScorer.score(allExperts, keywords);
        List<Expert> topExperts = resultRanker.topN(scored, keywords, 10);
        System.out.println(">>> [RecommendationFacade] Returning top " + topExperts.size() + " experts.");
        return topExperts;
    }
}
