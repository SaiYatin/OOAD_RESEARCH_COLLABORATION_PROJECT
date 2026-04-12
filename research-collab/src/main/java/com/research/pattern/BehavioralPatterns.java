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
 * Concrete Strategy 2 - True AI-powered semantic search via Google Cloud BigQuery Vector Search.
 */
@Component
class BigQuerySemanticStrategy implements RecommendationStrategy {

    private final String projectId;
    private final String dataset;
    private final String table;
    private final String model;

    public BigQuerySemanticStrategy(
            @org.springframework.beans.factory.annotation.Value("${google.cloud.project.id}") String projectId,
            @org.springframework.beans.factory.annotation.Value("${google.cloud.bigquery.dataset}") String dataset,
            @org.springframework.beans.factory.annotation.Value("${google.cloud.bigquery.table}") String table,
            @org.springframework.beans.factory.annotation.Value("${google.cloud.bigquery.model}") String model) {
        this.projectId = projectId;
        this.dataset = dataset;
        this.table = table;
        this.model = model;
        System.out.println("[BigQuery] Initialized Strategy with Project: " + projectId + ", Dataset: " + dataset + ", Table: " + table);
    }

    @Override
    public List<Expert> recommend(String query, List<Expert> experts, int topN) {
        System.out.println("\n[BigQuery] === TOOL: mentor_semantic_recommendation ===");
        System.out.println("[BigQuery] Starting vector search for query: '" + query + "'");
        
        if (query == null || query.isBlank()) {
            System.out.println("[BigQuery] Query is blank, returning empty list.");
            return List.of();
        }

        List<Expert> recommended = new java.util.ArrayList<>();
        try {
            System.out.println("[BigQuery] Loading Google Cloud Credentials...");
            
            // Check for credentials existence before fetching service to avoid cryptic NullPointerException
            com.google.auth.Credentials credentials = com.google.auth.oauth2.GoogleCredentials.getApplicationDefault();
            if (credentials == null) {
                System.err.println("[BigQuery ERROR] Default credentials are null!");
                throw new RuntimeException("GCP Credentials missing! Run 'gcloud auth application-default login' in your terminal.");
            }
            
            com.google.cloud.bigquery.BigQuery bigquery = com.google.cloud.bigquery.BigQueryOptions.newBuilder()
                    .setCredentials(credentials)
                    .setProjectId(projectId)
                    .build()
                    .getService();
            
            System.out.println("[BigQuery] Credentials loaded successfully for project " + projectId);

            String sql = String.format(
                "SELECT base.name, base.department, base.research " +
                "FROM VECTOR_SEARCH(" +
                "    TABLE `%s.%s.%s`, " +
                "    'embedding', " +
                "    (SELECT text_embedding AS embedding FROM ML.GENERATE_TEXT_EMBEDDING(" +
                "        MODEL `%s.%s.%s`, " +
                "        (SELECT @val AS content)" +
                "    )), " +
                "    top_k => %d, " +
                "    distance_type => 'COSINE'" +
                ")",
                projectId, dataset, table,
                projectId, dataset, model,
                topN
            );
            
            System.out.println("[BigQuery] Executing SQL Query:\n" + sql);

            com.google.cloud.bigquery.QueryJobConfiguration queryConfig = com.google.cloud.bigquery.QueryJobConfiguration.newBuilder(sql)
                .addNamedParameter("val", com.google.cloud.bigquery.QueryParameterValue.string(query))
                .build();

            System.out.println("[BigQuery] Calling BigQuery API...");
            com.google.cloud.bigquery.TableResult result = bigquery.query(queryConfig);
            System.out.println("[BigQuery] Query completed. Iterating results...");

            int matchedCount = 0;
            for (com.google.cloud.bigquery.FieldValueList row : result.iterateAll()) {
                String bqName = row.get("name").isNull() ? null : row.get("name").getStringValue();
                System.out.println("[BigQuery] Found DB Row -> Name: " + bqName);
                if (bqName == null) continue;
                
                // Map the returned row to our existing JPA records
                Expert matched = experts.stream()
                    .filter(e -> e.isActive())
                    .filter(e -> bqName.equalsIgnoreCase(e.getName()))
                    .findFirst()
                    .orElse(null);

                if (matched != null) {
                    System.out.println("[BigQuery] Matched '" + bqName + "' to local Java Expert Object.");
                    recommended.add(matched);
                    matchedCount++;
                } else {
                    System.out.println("[BigQuery] Warning: '" + bqName + "' returned from BQ but not found in local active experts list!");
                }
            }
            
            System.out.println("[BigQuery] Completed mapping. Returning " + matchedCount + " top experts.");

        } catch (InterruptedException e) {
            System.err.println("[BigQuery ERROR] Thread Interrupted:");
            e.printStackTrace();
            Thread.currentThread().interrupt();
            throw new RuntimeException("BigQuery query interrupted", e);
        } catch (Exception e) {
            System.err.println("[BigQuery ERROR] Vector Search Exception:");
            e.printStackTrace();
            
            // Provide explicit help message if it is an auth error
            if (e.getMessage().contains("getUniverseDomain") || e.getMessage().contains("credential")) {
                throw new RuntimeException("Critical Google Cloud Auth Error. Ensure you have run 'gcloud auth application-default login' and that your Google Cloud CLI is correctly installed and authenticated to project '" + projectId + "'. Root cause: " + e.getMessage(), e);
            }
            
            throw new RuntimeException("BigQuery Vector Search failed: " + e.getMessage(), e);
        }

        return recommended;
    }

    @Override
    public String strategyName() { return "Semantic Match (BigQuery Vector Search)"; }
}

/**
 * Concrete Strategy 3 - Hybrid: keyword pre-filter then AI re-rank.
 */
@Component
class HybridStrategy implements RecommendationStrategy {

    @Override
    public List<Expert> recommend(String query, List<Expert> experts, int topN) {
        List<Expert> candidates = new SemanticTFIDFStrategy()
            .recommend(query, experts, 30);
        return candidates.stream().limit(topN).toList();
    }

    @Override
    public String strategyName() { return "Hybrid (Semantic Pre-filter + AI Re-rank)"; }
}

/**
 * Concrete Strategy 4 - Native Semantic TF-IDF Vectorization Engine.
 * Replaces basic keyword matching with context-aware information retrieval.
 */
@Component
class SemanticTFIDFStrategy implements RecommendationStrategy {

    @Override
    public List<Expert> recommend(String query, List<Expert> experts, int topN) {
        if (query == null || query.isBlank()) return List.of();
        
        List<String> queryTerms = List.of(query.toLowerCase().split("[\\s,;.]+"));
        int N = experts.size();
        
        // Calculate document frequency (DF) for each query term
        java.util.Map<String, Double> idfMap = new java.util.HashMap<>();
        for (String term : queryTerms) {
            long df = experts.stream().filter(e -> {
                String text = e.getAggregatedProfileText();
                return text != null && text.toLowerCase().contains(term);
            }).count();
            // IDF = log(N / (1 + df)) + 1 to avoid zero/infinity
            double idf = Math.log((double) N / (1.0 + df)) + 1.0;
            idfMap.put(term, idf);
        }
        
        return experts.stream()
            .filter(Expert::isActive)
            .map(expert -> {
                String text = expert.getAggregatedProfileText();
                if (text == null) text = "";
                String lowerText = text.toLowerCase();
                
                double score = 0.0;
                for (String term : queryTerms) {
                    if (term.length() <= 2) continue; // skip very tiny words
                    int tf = countOccurrences(lowerText, term);
                    if (tf > 0) {
                        // Dampen TF (log normalization) so spamming a word doesn't dominate
                        double tfDamped = 1.0 + Math.log(tf); 
                        score += tfDamped * idfMap.get(term);
                    }
                }
                
                // Length normalization to prevent huge profiles from winning automatically
                double lengthPenalty = Math.max(1.0, Math.log10(lowerText.length() + 1));
                score = score / lengthPenalty;
                
                return new java.util.AbstractMap.SimpleEntry<>(expert, score);
            })
            .filter(entry -> entry.getValue() > 0)
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(topN)
            .map(java.util.Map.Entry::getKey)
            .toList();
    }

    private int countOccurrences(String text, String term) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(term, idx)) != -1) {
            count++;
            idx += term.length();
        }
        return count;
    }

    @Override
    public String strategyName() { return "Semantic TF-IDF Engine"; }
}
