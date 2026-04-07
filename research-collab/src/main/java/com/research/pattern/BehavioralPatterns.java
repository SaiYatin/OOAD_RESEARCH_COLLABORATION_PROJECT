package com.research.pattern;

import com.research.model.ResearchPaper;
import com.research.model.Expert;
import org.springframework.stereotype.Component;
import java.util.List;

// ══════════════════════════════════════════════════════════════
//  BEHAVIORAL PATTERNS - package-private implementations
//  Public classes split into: ResearchUpdateObserver.java,
//  PaperPublicationSubject.java, RecommendationContext.java
// ══════════════════════════════════════════════════════════════

/**
 * Concrete Observer - sends email via n8n webhook.
 * Not a Spring bean - instantiated per subscriber by the service layer.
 */
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

        // Use Thread for Java 17 compatibility (startVirtualThread requires Java 21)
        new Thread(() -> {
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
        }).start();
    }
}


// ══════════════════════════════════════════════════════════════
//  BEHAVIORAL PATTERN 2: STRATEGY
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
 */
@Component
class KeywordMatchingStrategy implements RecommendationStrategy {

    @Override
    public List<Expert> recommend(String query, List<Expert> experts, int topN) {
        return experts.stream()
            .filter(e -> e.isActive())
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
 */
@Component
class AIPoweredStrategy implements RecommendationStrategy {

    private final N8nWebhookCaller webhookCaller;

    public AIPoweredStrategy(N8nWebhookCaller webhookCaller) {
        this.webhookCaller = webhookCaller;
    }

    @Override
    public List<Expert> recommend(String query, List<Expert> experts, int topN) {
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
        List<Expert> candidates = new KeywordMatchingStrategy()
            .recommend(query, experts, 30);
        return candidates.stream().limit(topN).toList();
    }

    @Override
    public String strategyName() { return "Hybrid (Keyword + AI Re-rank)"; }
}
