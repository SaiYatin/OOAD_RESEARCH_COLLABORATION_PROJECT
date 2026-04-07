package com.research.config;

import com.research.model.Expert;
import com.research.service.ExpertService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Application configuration.
 * Seeds the expert database from the n8n-scraped CSV on first run.
 */
@Configuration
public class AppConfig {

    @Value("${n8n.webhook.url:http://localhost:5678/webhook/research-update}")
    private String n8nWebhookUrl;

    /**
     * On startup: import PES professor data from CSV into expert table.
     * This bridges the n8n University Research Agent output into the Java app.
     * Skip rows already in DB (upsert by email in ExpertService).
     */
    @Bean
    public CommandLineRunner seedExpertData(ExpertService expertService) {
        return args -> {
            // Check if data already seeded
            if (expertService.getAllActiveExperts().size() > 5) {
                System.out.println("[Startup] Expert data already seeded. Skipping.");
                return;
            }
            System.out.println("[Startup] Seeding expert data from CSV...");
            List<String[]> rows = loadCsvRows();
            int count = expertService.bulkImportFromCsv(rows);
            System.out.printf("[Startup] Seeded %d experts successfully.%n", count);
        };
    }

    /**
     * Loads the bundled CSV (University_Research_Agent - Sheet1.csv).
     * Place the CSV in src/main/resources/data/experts.csv
     */
    private List<String[]> loadCsvRows() {
        List<String[]> rows = new ArrayList<>();
        try {
            URL csvUrl = getClass().getClassLoader()
                                   .getResource("data/experts.csv");
            if (csvUrl == null) {
                System.out.println("[Startup] experts.csv not found in resources/data/");
                return rows;
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(csvUrl.openStream()))) {
                String headerLine = reader.readLine(); // skip header
                if (headerLine == null) return rows;

                StringBuilder currentLine = new StringBuilder();
                boolean insideQuote = false;
                String line;

                while ((line = reader.readLine()) != null) {
                    line = line.replace("\r", "");
                    if (!insideQuote) {
                        currentLine = new StringBuilder(line);
                    } else {
                        currentLine.append(" ").append(line);
                    }
                    int quoteCount = 0;
                    for (char c : currentLine.toString().toCharArray()) {
                        if (c == '"') quoteCount++;
                    }
                    insideQuote = (quoteCount % 2 != 0);

                    if (!insideQuote) {
                        String[] cols = splitCsvLine(currentLine.toString());
                        if (cols.length >= 5 && !cols[0].isBlank()) {
                            rows.add(cols);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[Startup] CSV load failed: " + e.getMessage());
        }
        return rows;
    }

    private String[] splitCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        result.add(sb.toString().trim());
        return result.toArray(new String[0]);
    }
}
