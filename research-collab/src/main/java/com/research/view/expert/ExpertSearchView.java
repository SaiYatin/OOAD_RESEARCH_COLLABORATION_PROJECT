package com.research.view.expert;

import com.research.repository.ResearchProjectRepository;
import com.research.repository.ResearcherRepository;
import com.research.service.CollaborationService;
import com.research.service.RecommendationService;
import javafx.geometry.Insets;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import org.springframework.stereotype.Component;

/**
 * ExpertSearchView — Main dashboard container.
 * 
 * Responsibilities:
 * - Render title/header
 * - Delegate strictly to ExpertSearchPane for the core logic
 * 
 * (Browse/Manage & Add functionality removed as part of loose coupling and CSV-only ingestion strategy)
 * 
 * Design Pattern: Strategy pattern used inside ExpertSearchPane
 * Design Principle: SRP (Single Responsibility Principle)
 */
@Component
public class ExpertSearchView {

    private final RecommendationService recommendationService;
    private final CollaborationService collaborationService;
    private final ResearcherRepository researcherRepository;
    private final ResearchProjectRepository projectRepository;

    public ExpertSearchView(RecommendationService recommendationService,
                            CollaborationService collaborationService,
                            ResearcherRepository researcherRepository,
                            ResearchProjectRepository projectRepository) {
        this.recommendationService = recommendationService;
        this.collaborationService = collaborationService;
        this.researcherRepository = researcherRepository;
        this.projectRepository = projectRepository;
    }

    public VBox buildPanel() {
        VBox panel = new VBox(16);
        panel.setStyle("-fx-background-color: #0f1117;");
        // Maintain standard overall padding
        panel.setPadding(new Insets(16, 24, 16, 24));

        VBox header = new VBox(4);
        header.setPadding(new Insets(0, 0, 8, 0));
        Text title = new Text("Experts & Recommendations");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 26));
        title.setFill(Color.web("#e2e8f0"));
        Text subtitle = new Text("AI-powered expert matching (Powered by Embedding Model) · Send collaboration requests");
        subtitle.setFont(Font.font("System", 13));
        subtitle.setFill(Color.web("#8892a4"));
        header.getChildren().addAll(title, subtitle);

        ExpertSearchPane searchPane = new ExpertSearchPane(
            recommendationService, 
            researcherRepository, 
            collaborationService, 
            projectRepository
        );
        VBox.setVgrow(searchPane, Priority.ALWAYS);

        panel.getChildren().addAll(header, searchPane);
        return panel;
    }
}
