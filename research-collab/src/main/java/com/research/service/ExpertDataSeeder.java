package com.research.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads expert data from the bundled CSV (n8n University Research Agent output)
 * into MySQL on every startup. Uses upsert-by-email so duplicates are harmless.
 */
@Component
public class ExpertDataSeeder implements CommandLineRunner {

    private final ExpertService expertService;

    public ExpertDataSeeder(ExpertService expertService) {
        this.expertService = expertService;
    }

    @Override
    public void run(String... args) {
        try {
            ClassPathResource res = new ClassPathResource("experts.csv");
            if (!res.exists()) {
                System.out.println("[DataSeeder] experts.csv not found in classpath — skipping.");
                return;
            }

            List<String[]> rows = parseCsv(res.getInputStream());
            System.out.println("[DataSeeder] Parsed " + rows.size() + " expert rows from CSV.");

            int imported = expertService.bulkImportFromCsv(rows);
            System.out.println("[DataSeeder] ✓ Imported/updated " + imported + " experts into MySQL.");

        } catch (Exception e) {
            System.err.println("[DataSeeder] CSV import failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Parse CSV handling quoted multi-line fields (research areas can span many lines).
     * CSV columns: name, designation, Mail, Phone, research
     */
    private List<String[]> parseCsv(InputStream is) throws IOException {
        List<String[]> rows = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));

        String headerLine = reader.readLine(); // skip header
        if (headerLine == null) return rows;

        StringBuilder currentLine = new StringBuilder();
        boolean insideQuote = false;

        String line;
        while ((line = reader.readLine()) != null) {
            // Handle \r if present
            line = line.replace("\r", "");

            if (!insideQuote) {
                currentLine = new StringBuilder(line);
            } else {
                currentLine.append("\n").append(line);
            }

            // Count unescaped quotes to determine if we're still inside a quoted field
            int quoteCount = 0;
            for (char c : currentLine.toString().toCharArray()) {
                if (c == '"') quoteCount++;
            }
            insideQuote = (quoteCount % 2 != 0);

            if (!insideQuote) {
                // Full logical row collected — parse it
                String[] fields = splitCsvRow(currentLine.toString());
                if (fields.length >= 5 && !fields[0].isBlank()) {
                    rows.add(fields);
                }
            }
        }
        reader.close();
        return rows;
    }

    /**
     * Split a single CSV row respecting quoted fields.
     */
    private String[] splitCsvRow(String row) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < row.length(); i++) {
            char c = row.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < row.length() && row.charAt(i + 1) == '"') {
                    current.append('"');
                    i++; // skip escaped quote
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString().trim());
        return fields.toArray(new String[0]);
    }
}
