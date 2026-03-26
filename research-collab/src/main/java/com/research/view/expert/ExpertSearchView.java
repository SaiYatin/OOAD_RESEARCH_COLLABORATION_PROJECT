package com.research.view.expert;

import com.research.model.Expert;
import com.research.service.ExpertService;
import com.research.service.RecommendationService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ExpertSearchView - Member 4's primary UI.
 * Find & recommend domain experts using Strategy-selectable algorithms.
 * Integrates with n8n Search Agent via AI strategy option.
 * MVC: View layer.
 */
@Component
public class ExpertSearchView {

    private final RecommendationService recommendationService;
    private final ExpertService expertService;

    public ExpertSearchView(RecommendationService recommendationService,
                            ExpertService expertService) {
        this.recommendationService = recommendationService;
        this.expertService = expertService;
    }

    public VBox buildPanel() {
        VBox panel = new VBox(20);
        panel.setStyle("-fx-background-color: #0f1117;");

        // ── Header ────────────────────────────────────────────────────
        Text title = new Text("Expert Recommendation");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 26));
        title.setFill(Color.web("#e2e8f0"));

        Text subtitle = new Text("Find PES University researchers matching your domain");
        subtitle.setFont(Font.font("System", 13));
        subtitle.setFill(Color.web("#8892a4"));

        // ── Strategy Selector ─────────────────────────────────────────
        HBox strategyRow = new HBox(12);
        strategyRow.setAlignment(Pos.CENTER_LEFT);

        Label stratLabel = new Label("Algorithm:");
        stratLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        stratLabel.setTextFill(Color.web("#8892a4"));

        ToggleGroup stratGroup = new ToggleGroup();
        RadioButton kwBtn  = radioBtn("Keyword Match", stratGroup, true);
        RadioButton aiBtn  = radioBtn("AI-Powered (n8n)", stratGroup, false);
        RadioButton hybBtn = radioBtn("Hybrid", stratGroup, false);

        Label stratInfo = new Label("Strategy: Keyword Matching");
        stratInfo.setFont(Font.font("System", FontPosture.ITALIC, 11));
        stratInfo.setTextFill(Color.web("#4a5568"));

        stratGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            if (n == kwBtn)  stratInfo.setText("Strategy: Keyword Matching (local, fast)");
            if (n == aiBtn)  stratInfo.setText("Strategy: AI-Powered via n8n Gemini Search Agent");
            if (n == hybBtn) stratInfo.setText("Strategy: Hybrid — keyword pre-filter + AI re-rank");
        });

        strategyRow.getChildren().addAll(stratLabel, kwBtn, aiBtn, hybBtn, stratInfo);

        // ── Search Bar ────────────────────────────────────────────────
        HBox searchRow = new HBox(12);
        searchRow.setAlignment(Pos.CENTER_LEFT);

        TextField queryField = new TextField();
        queryField.setPromptText("e.g. 'machine learning', 'composite materials', 'NLP'...");
        queryField.setStyle(
            "-fx-background-color: #1a1f2e; -fx-text-fill: #e2e8f0; " +
            "-fx-prompt-text-fill: #4a5568; -fx-border-color: #2d3748; " +
            "-fx-border-radius: 6px; -fx-background-radius: 6px; " +
            "-fx-pref-height: 42px; -fx-font-size: 13px; -fx-padding: 0 12px;");
        HBox.setHgrow(queryField, Priority.ALWAYS);

        Button findBtn = new Button("Find Experts");
        findBtn.setStyle(
            "-fx-background-color: #6c9bff; -fx-text-fill: white; " +
            "-fx-font-weight: bold; -fx-pref-height: 42px; -fx-pref-width: 130px; " +
            "-fx-background-radius: 6px; -fx-cursor: hand;");

        searchRow.getChildren().addAll(queryField, findBtn);

        // ── Results ───────────────────────────────────────────────────
        Label resultCount = new Label("");
        resultCount.setFont(Font.font("System", 12));
        resultCount.setTextFill(Color.web("#68d391"));

        VBox resultsContainer = new VBox(12);
        VBox.setVgrow(resultsContainer, Priority.ALWAYS);

        ScrollPane scroll = new ScrollPane(resultsContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // ── Action: find experts ──────────────────────────────────────
        findBtn.setOnAction(e -> {
            String query = queryField.getText().trim();
            if (query.isBlank()) {
                resultCount.setText("Please enter a search query.");
                resultCount.setTextFill(Color.web("#fc8181"));
                return;
            }

            String mode = kwBtn.isSelected() ? "keyword"
                        : aiBtn.isSelected()  ? "ai"
                        : "hybrid";

            List<Expert> experts = recommendationService.recommendWithStrategy(query, mode);
            resultsContainer.getChildren().clear();

            if (experts.isEmpty()) {
                Label none = new Label("No matching experts found for: \"" + query + "\"");
                none.setTextFill(Color.web("#8892a4"));
                none.setFont(Font.font("System", 14));
                resultsContainer.getChildren().add(none);
                resultCount.setText("0 results");
                return;
            }

            resultCount.setTextFill(Color.web("#68d391"));
            resultCount.setText(experts.size() + " expert(s) matched using "
                    + recommendationService.getCurrentStrategyName());

            for (int i = 0; i < experts.size(); i++) {
                resultsContainer.getChildren().add(expertCard(experts.get(i), i + 1, query));
            }
        });

        // Default: show all experts
        queryField.setOnAction(e -> findBtn.fire());

        panel.getChildren().addAll(title, subtitle, strategyRow, searchRow,
                                    resultCount, scroll);
        return panel;
    }

    private HBox expertCard(Expert expert, int rank, String query) {
        HBox card = new HBox(16);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setStyle(
            "-fx-background-color: #1a1f2e; -fx-background-radius: 10px; " +
            "-fx-border-color: #2d3748; -fx-border-radius: 10px;");
        card.setAlignment(Pos.CENTER_LEFT);

        // Rank badge
        Label rankBadge = new Label("#" + rank);
        rankBadge.setFont(Font.font("Georgia", FontWeight.BOLD, 16));
        rankBadge.setTextFill(Color.web("#6c9bff"));
        rankBadge.setMinWidth(36);
        rankBadge.setAlignment(Pos.CENTER);

        // Info
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Text name = new Text(expert.getName());
        name.setFont(Font.font("Georgia", FontWeight.BOLD, 16));
        name.setFill(Color.web("#e2e8f0"));

        Label designation = new Label(expert.getDesignation() + " — " + expert.getInstitution());
        designation.setFont(Font.font("System", 12));
        designation.setTextFill(Color.web("#8892a4"));

        Label domain = new Label("Domain: " + (expert.getDomain() != null
                ? expert.getDomain() : "General"));
        domain.setFont(Font.font("System", FontWeight.BOLD, 11));
        domain.setTextFill(Color.web("#6c9bff"));
        domain.setStyle("-fx-background-color: #6c9bff22; -fx-padding: 2 8; " +
                        "-fx-background-radius: 10px;");

        String researchPreview = expert.getResearchAreas() != null
            ? expert.getResearchAreas().substring(0,
                Math.min(120, expert.getResearchAreas().length())) + "..."
            : "Research areas not listed.";
        Label research = new Label(researchPreview);
        research.setFont(Font.font("System", 12));
        research.setTextFill(Color.web("#718096"));
        research.setWrapText(true);

        info.getChildren().addAll(name, designation, domain, research);

        // Contact button
        VBox actions = new VBox(8);
        actions.setAlignment(Pos.CENTER);

        Button contactBtn = new Button("✉ Contact");
        contactBtn.setStyle(
            "-fx-background-color: #6c9bff; -fx-text-fill: white; " +
            "-fx-font-size: 11px; -fx-pref-width: 100px; -fx-pref-height: 34px; " +
            "-fx-background-radius: 6px; -fx-cursor: hand;");
        contactBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Expert Contact");
            alert.setHeaderText(expert.getName());
            alert.setContentText(
                "Email: " + expert.getEmail() + "\n" +
                "Phone: " + expert.getPhone() + "\n\n" +
                "Research Areas:\n" + expert.getResearchAreas()
            );
            alert.showAndWait();
        });

        Label score = new Label("Score: " + String.format("%.1f", expert.scoreAgainst(query)));
        score.setFont(Font.font("System", 10));
        score.setTextFill(Color.web("#4a5568"));

        actions.getChildren().addAll(contactBtn, score);
        card.getChildren().addAll(rankBadge, info, actions);
        return card;
    }

    private RadioButton radioBtn(String text, ToggleGroup group, boolean selected) {
        RadioButton rb = new RadioButton(text);
        rb.setToggleGroup(group);
        rb.setSelected(selected);
        rb.setStyle("-fx-text-fill: #a0aec0; -fx-font-size: 12px; -fx-cursor: hand;");
        return rb;
    }
}
