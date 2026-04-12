package com.research.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Client that communicates with the PES Mentor Scout Cloud Run Agent.
 * Implements the 2-step ADK Protocol:
 *   Step 1: POST /apps/{app_name}/users/{user_id}/sessions  → get registered session_id
 *   Step 2: POST /run with that registered session_id
 */
public class MentorScoutClient {

    private static final String BASE_URL  = "https://mentor-scout-482781773486.us-central1.run.app";
    private static final String APP_NAME  = "mentor_scout";
    private static final String USER_ID   = "java_client";

    private final HttpClient client;
    private String cachedSessionId = null; // Reused for the entire app session

    public MentorScoutClient() {
        this.client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        System.out.println("[MentorScout] Client initialized. Endpoint: " + BASE_URL);
    }

    // ── STEP 1: Register a session on the server ────────────────────────────────
    private String createSession() throws Exception {
        String url = BASE_URL + "/apps/" + APP_NAME + "/users/" + USER_ID + "/sessions";
        System.out.println("[MentorScout] STEP 1 - Registering session at: " + url);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(30))
            .POST(HttpRequest.BodyPublishers.ofString("{}"))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("[MentorScout] Session HTTP Status: " + response.statusCode());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Session creation failed: " + response.statusCode() + " — " + response.body());
        }

        JSONObject json = new JSONObject(response.body());
        String sessionId = json.getString("id");
        System.out.println("[MentorScout] Session registered successfully. session_id: " + sessionId);
        return sessionId;
    }

    // ── STEP 2: Build the /run payload with registered session_id ────────────────
    private String buildPayload(String sessionId, String message) {
        JSONObject part = new JSONObject();
        part.put("text", message);

        JSONArray parts = new JSONArray();
        parts.put(part);

        JSONObject newMessage = new JSONObject();
        newMessage.put("role", "user");
        newMessage.put("parts", parts);

        JSONObject payload = new JSONObject();
        payload.put("app_name", APP_NAME);
        payload.put("user_id", USER_ID);
        payload.put("session_id", sessionId);
        payload.put("new_message", newMessage);

        return payload.toString();
    }

    // ── CORE: Create session once → reuse for all subsequent queries ──────────────
    public String callAgent(String userMessage) throws Exception {
        System.out.println("\n[MentorScout] === callAgent() ===");
        System.out.println("[MentorScout] Query: '" + userMessage + "'");

        // Only create session once per app lifetime
        if (cachedSessionId == null) {
            System.out.println("[MentorScout] No active session. Creating new session...");
            cachedSessionId = createSession();
        } else {
            System.out.println("[MentorScout] Reusing existing session_id: " + cachedSessionId);
        }

        // Step 2: Fire the actual query using the registered session
        String payload = buildPayload(cachedSessionId, userMessage);
        System.out.println("[MentorScout] STEP 2 - Firing POST to /run with session_id: " + cachedSessionId);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/run"))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(90))
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("[MentorScout] /run HTTP Status: " + response.statusCode());

        if (response.statusCode() != 200) {
            System.err.println("[MentorScout ERROR] Server returned " + response.statusCode());
            System.err.println("[MentorScout ERROR] Full response body: " + response.body());
            // Reset session so next call creates a fresh one
            cachedSessionId = null;
            System.out.println("[MentorScout] Cached session invalidated. Will create fresh session on next call.");
            throw new RuntimeException("Mentor Scout API Error: " + response.statusCode() + " — " + response.body());
        }

        String result = extractAgentText(response.body());
        System.out.println("[MentorScout] Agent response extracted. Length: " + result.length() + " chars");
        return result;
    }

    // ── Parse ADK response and extract agent text ────────────────────────────────
    private String extractAgentText(String rawJson) {
        StringBuilder result = new StringBuilder();
        try {
            JSONArray events = new JSONArray(rawJson);
            System.out.println("[MentorScout] Parsing " + events.length() + " ADK event(s)...");
            for (int i = 0; i < events.length(); i++) {
                JSONObject event = events.getJSONObject(i);
                if (event.has("content")) {
                    JSONObject content = event.getJSONObject("content");
                    if (content.has("parts")) {
                        JSONArray partsArr = content.getJSONArray("parts");
                        for (int j = 0; j < partsArr.length(); j++) {
                            JSONObject p = partsArr.getJSONObject(j);
                            if (p.has("text")) {
                                result.append(p.getString("text"));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[MentorScout ERROR] Failed to parse ADK response: " + e.getMessage());
            return "Could not parse Mentor Scout response: " + e.getMessage();
        }
        return result.toString().trim();
    }
}
