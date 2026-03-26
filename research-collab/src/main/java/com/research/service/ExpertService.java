package com.research.service;

import com.research.model.Expert;
import com.research.repository.ExpertRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Arrays;
import java.util.List;

/**
 * ExpertService - Member 2's primary use case.
 * Handles CRUD for expert profiles sourced from n8n-scraped PES data.
 *
 * Design Principle: SRP - only handles expert profile operations.
 * Design Principle: OCP - new import formats added via new methods, existing untouched.
 */
@Service
public class ExpertService {

    private final ExpertRepository expertRepository;

    public ExpertService(ExpertRepository expertRepository) {
        this.expertRepository = expertRepository;
    }

    public List<Expert> getAllActiveExperts() {
        return expertRepository.findByActiveTrue();
    }

    public Expert getExpertById(Long id) {
        return expertRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Expert not found: " + id));
    }

    /**
     * Import expert from scraped CSV row (n8n University Research Agent output).
     * This is how experts get seeded from PES staff data.
     */
    @Transactional
    public Expert importFromCsvRow(String name, String designation,
                                    String email, String phone,
                                    String researchAreas) {
        // Upsert - update if email exists, insert if not
        Expert expert = expertRepository.findByEmail(email)
                                        .orElse(new Expert());
        expert.setName(name);
        expert.setDesignation(designation);
        expert.setEmail(email);
        expert.setPhone(phone);
        expert.setInstitution("PES University");
        expert.updateProfile(researchAreas); // Extracts keywords too
        expert.setActive(true);

        // Extract domain from research areas
        expert.setDomain(inferDomain(researchAreas));

        return expertRepository.save(expert);
    }

    /**
     * Update expert research profile.
     * Triggers keyword re-extraction automatically via Expert.updateProfile().
     */
    @Transactional
    public Expert updateResearchProfile(Long expertId, String newResearchAreas) {
        Expert expert = getExpertById(expertId);
        expert.updateProfile(newResearchAreas);
        expert.setDomain(inferDomain(newResearchAreas));
        return expertRepository.save(expert);
    }

    @Transactional
    public void deactivateExpert(Long expertId) {
        Expert expert = getExpertById(expertId);
        expert.setActive(false);
        expertRepository.save(expert);
    }

    public List<Expert> searchByKeyword(String keyword) {
        return expertRepository.findByResearchAreasContaining(keyword);
    }

    public List<String> getAllDomains() {
        return expertRepository.findAllDomains();
    }

    /**
     * Bulk import all experts from CSV (called at startup from n8n data).
     */
    @Transactional
    public int bulkImportFromCsv(List<String[]> csvRows) {
        int count = 0;
        for (String[] row : csvRows) {
            if (row.length >= 5) {
                try {
                    importFromCsvRow(row[0], row[1], row[2], row[3], row[4]);
                    count++;
                } catch (Exception e) {
                    System.err.println("Skipping row: " + Arrays.toString(row)
                            + " - " + e.getMessage());
                }
            }
        }
        return count;
    }

    /**
     * Infer broad domain from research area text.
     * Keeps it simple - keyword spotting.
     */
    private String inferDomain(String researchAreas) {
        if (researchAreas == null || researchAreas.equalsIgnoreCase("Not listed")) {
            return "General";
        }
        String lower = researchAreas.toLowerCase();
        if (lower.contains("machine learning") || lower.contains("deep learning")
                || lower.contains("neural") || lower.contains("nlp")
                || lower.contains("ai") || lower.contains("natural language")) {
            return "Artificial Intelligence";
        } else if (lower.contains("heat") || lower.contains("thermal")
                || lower.contains("fluid") || lower.contains("composite")) {
            return "Mechanical Engineering";
        } else if (lower.contains("signal") || lower.contains("speech")
                || lower.contains("audio") || lower.contains("image processing")) {
            return "Signal Processing";
        } else if (lower.contains("bio") || lower.contains("protein")
                || lower.contains("cancer") || lower.contains("molecular")) {
            return "Biotechnology";
        } else if (lower.contains("economics") || lower.contains("finance")
                || lower.contains("management")) {
            return "Management & Economics";
        } else if (lower.contains("civil") || lower.contains("concrete")
                || lower.contains("earthquake") || lower.contains("structure")) {
            return "Civil Engineering";
        }
        return "Interdisciplinary";
    }
}
