package com.research.pattern;

import com.research.model.ResearchPaper;
import com.research.model.Expert;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

// ══════════════════════════════════════════════════════════════
//  BEHAVIORAL PATTERN 1: OBSERVER
//  When a new ResearchPaper is added/published, all subscribers
//  following that research domain get notified via email.
//  Applied to: Email notification workflow (n8n integration).
// ══════════════════════════════════════════════════════════════

/**
 * Observer interface - any listener that reacts to paper events.
 */
public interface ResearchUpdateObserver {
    void onNewPaper(ResearchPaper paper, String matchedKeyword);
}

/**
 * Observable subject - the paper publication event source.
 * Maintains a list of observers and fires events.
 *
 * HOW APPLIED: When a paper is published or added via n8n workflow,
 * PaperPublicationSubject notifies all registered subscribers whose
 * followed keywords match the paper's domain/keywords.
 */
@Component
class PaperPublicationSubject {

    private final List<ResearchUpdateObserver> observers = new ArrayList<>();

    public void addObserver(ResearchUpdateObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(ResearchUpdateObserver observer) {
        observers.remove(observer);
    }

    /**
     * Called when a new paper is added/published.
     * Each observer gets notified with the matched keyword.
     */
    public void notifyObservers(ResearchPaper paper, List<String> paperKeywords) {
        for (ResearchUpdateObserver observer : observers) {
            for (String keyword : paperKeywords) {
                observer.onNewPaper(paper, keyword);
            }
        }
    }
}

/**
 * Concrete Observer - sends email via n8n webhook.
 * This triggers the n8n Email Notification Workflow.
 */
@Component
class EmailNotificationObserver implements ResearchUpdateObserver {

    private final N8nWebhookCaller webhookCaller;
    private final String subscriberEmail;
    private final List<String> followedKeywords;

    public EmailNotificationObserver(N8nWebhookCaller webhookCaller,
                                     String subscriberEmail,
                                     List<String> followedKeywords) {
        this.webhookCaller = webhookCaller;
        this.subscriberEmail = subscriberEmail;
        this.followedKeywords = followedKeywords;
    }

    @Override
    public void onNewPaper(ResearchPaper paper, String matchedKeyword) {
        // Only notify if this subscriber follows this keyword
        boolean follows = followedKeywords.stream()
            .anyMatch(k -> k.equalsIgnoreCase(matchedKeyword)
                        || matchedKeyword.toLowerCase().contains(k.toLowerCase()));

        if (follows) {
            webhookCaller.triggerEmailNotification(
                subscriberEmail, paper, matchedKeyword);
        }
    }
}

/**
 * N8n Webhook Caller - calls the n8n Email Notification workflow.
 * Used by Observer to actually fire the email via n8n.
 */
@Component
class N8nWebhookCaller {

    private final String webhookUrl;

    public N8nWebhookCaller(
            @org.springframework.beans.factory.annotation.Value("${n8n.webhook.url}")
            String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public void triggerEmailNotification(String recipientEmail,
                                          ResearchPaper paper,
                                          String matchedKeyword) {
        // Build JSON payload for n8n webhook
        String payload = String.format("""
            {
              "recipientEmail": "%s",
              "paperTitle": "%s",
              "paperDomain": "%s",
              "matchedKeyword": "%s",
              "paperLink": "%s",
              "author": "%s"
            }
            """,
            recipientEmail,
            paper.getTitle() != null ? paper.getTitle().replace("\"", "'") : "",
            paper.getDomain() != null ? paper.getDomain() : "",
            matchedKeyword,
            paper.getLink() != null ? paper.getLink() : "",
            paper.getAuthor() != null ? paper.getAuthor() : ""
        );

        // Send HTTP POST to n8n webhook (non-blocking)
        Thread.startVirtualThread(() -> {
            try {
                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(payload))
                    .build();
                client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                System.err.println("n8n webhook call failed: " + e.getMessage());
            }
        });
    }
}


// ══════════════════════════════════════════════════════════════
//  BEHAVIORAL PATTERN 2: STRATEGY
//  RecommendationStrategy allows swapping the recommendation
//  algorithm at runtime (keyword-based vs AI-based vs hybrid).
//  Applied to: Expert recommendation feature.
// ══════════════════════════════════════════════════════════════

/**
 * Strategy interface - all recommendation algorithms implement this.
 */
interface RecommendationStrategy {
    List<Expert> recommend(String query, List<Expert> experts, int topN);

    String strategyName();
}

/**
 * Concrete Strategy 1 - Keyword matching (local, fast).
 * Default strategy when n8n AI is unavailable.
 */
@Component
class KeywordMatchingStrategy implements RecommendationStrategy {

    @Override
    public List<Expert> recommend(String query, List<Expert> experts, int topN) {
        return experts.stream()
            .filter(Expert::isActive)
            .filter(e -> e.scoreAgainst(query) > 0)
            .sorted((a, b) -> Double.compare(b.scoreAgainst(query), a.scoreAgainst(query)))
            .limit(topN)
            .toList();
    }

    @Override
    public String strategyName() { return "Keyword Matching"; }
}

/**
 * Concrete Strategy 2 - AI-powered via n8n Search Agent webhook.
 * Calls the n8n Search Agent workflow for Gemini-powered results.
 */
@Component
class AIPoweredStrategy implements RecommendationStrategy {

    private final N8nWebhookCaller webhookCaller;

    public AIPoweredStrategy(N8nWebhookCaller webhookCaller) {
        this.webhookCaller = webhookCaller;
    }

    @Override
    public List<Expert> recommend(String query, List<Expert> experts, int topN) {
        // Falls back to keyword matching if AI is unavailable
        // In full impl: calls n8n Search Agent, parses returned JSON experts
        return new KeywordMatchingStrategy().recommend(query, experts, topN);
    }

    @Override
    public String strategyName() { return "AI-Powered (Gemini via n8n)"; }
}

/**
 * Concrete Strategy 3 - Hybrid: keyword pre-filter then AI re-rank.
 */
@Component
class HybridStrategy implements RecommendationStrategy {

    @Override
    public List<Expert> recommend(String query, List<Expert> experts, int topN) {
        // Step 1: Keyword pre-filter to 30 candidates
        List<Expert> candidates = new KeywordMatchingStrategy()
            .recommend(query, experts, 30);
        // Step 2: AI re-rank (stub - delegates to keyword score for now)
        return candidates.stream().limit(topN).toList();
    }

    @Override
    public String strategyName() { return "Hybrid (Keyword + AI Re-rank)"; }
}

/**
 * Strategy Context - holds and executes the current strategy.
 * HOW APPLIED: The UI lets the user pick a recommendation mode,
 * and this context switches the algorithm at runtime.
 */
@Component
class RecommendationContext {

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
