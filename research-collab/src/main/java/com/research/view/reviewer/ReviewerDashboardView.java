package com.research.view.reviewer;

import com.research.model.ResearchPaper;
import com.research.controller.PaperController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ReviewerDashboardView — Reviewer-specific dashboard.
 *
 * @author Member 1
 * @usecase Paper Review & Publication Management
 *
 * Tabs:
 *   1. Pending Review   — Papers with status SUBMITTED
 *   2. Under Review     — Papers currently being reviewed
 *   3. Review History   — Published & Rejected papers
 *   4. Paper Search     — Search all papers (read-only view with View Paper)
 *
 * Design Pattern demonstrated: Decorator (search filter chain via PaperController)
 * Design Principle: SRP (each tab one responsibility)
 *
 * MVC Role: View — delegates all business logic to PaperController
 */
@Component
public class ReviewerDashboardView {

    private final PaperController paperController;

    public ReviewerDashboardView(PaperController paperController) {
        this.paperController = paperController;
    }

    public VBox buildPanel() {
        VBox panel = new VBox(0);
        panel.setStyle("-fx-background-color: #0f1117;");

        VBox header = new VBox(4);
        header.setPadding(new Insets(0, 0, 16, 0));
        Text title = new Text("Reviewer Dashboard");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 26));
        title.setFill(Color.web("#e2e8f0"));

        long pendingCount = paperController.getPapersByStatus(ResearchPaper.PaperStatus.SUBMITTED).size();
        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        titleRow.getChildren().add(title);
        if (pendingCount > 0) {
            Label badge = new Label(pendingCount + " awaiting review");
            badge.setStyle("-fx-background-color:#ecc94b;-fx-text-fill:#1a1f2e;-fx-font-size:11px;" +
                           "-fx-padding:3 10;-fx-background-radius:12px;-fx-font-weight:bold;");
            titleRow.getChildren().add(badge);
        }

        Text subtitle = new Text("Review submitted papers · Provide feedback · Approve or reject");
        subtitle.setFont(Font.font("System", 13));
        subtitle.setFill(Color.web("#8892a4"));
        header.getChildren().addAll(titleRow, subtitle);

        ToggleGroup tabGroup = new ToggleGroup();
        ToggleButton pendingTab = tabBtn("📋 Pending (" + pendingCount + ")", tabGroup, true);
        ToggleButton reviewingTab = tabBtn("🔍 Under Review", tabGroup, false);
        ToggleButton historyTab = tabBtn("📊 Review History", tabGroup, false);
        ToggleButton searchTab = tabBtn("🔎 Search Papers", tabGroup, false);
        HBox tabRow = new HBox(4, pendingTab, reviewingTab, historyTab, searchTab);
        tabRow.setPadding(new Insets(0, 0, 16, 0));

        StackPane content = new StackPane();
        VBox.setVgrow(content, Priority.ALWAYS);
        content.getChildren().add(buildPendingPane());

        tabGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            content.getChildren().clear();
            if (n == pendingTab) content.getChildren().add(buildPendingPane());
            else if (n == reviewingTab) content.getChildren().add(buildUnderReviewPane());
            else if (n == historyTab) content.getChildren().add(buildHistoryPane());
            else if (n == searchTab) content.getChildren().add(buildSearchPane());
        });

        panel.getChildren().addAll(header, tabRow, content);
        return panel;
    }

    // ═══════════════════════════════════════════════════════════
    // TAB 1: Pending Review
    // ═══════════════════════════════════════════════════════════

    private VBox buildPendingPane() {
        VBox pane = new VBox(12);
        Text heading = sectionHeading("Papers Awaiting Review");
        Label desc = smallNote("These papers have been submitted by researchers and need your review.");
        pane.getChildren().addAll(heading, desc);

        List<ResearchPaper> pending = paperController.getPapersByStatus(ResearchPaper.PaperStatus.SUBMITTED);

        if (pending.isEmpty()) {
            pane.getChildren().add(emptyLabel("No papers waiting for review. All caught up! 🎉"));
        } else {
            for (ResearchPaper paper : pending) {
                pane.getChildren().add(buildReviewCard(paper, true));
            }
        }

        ScrollPane scroll = new ScrollPane(pane);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        VBox wrapper = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return wrapper;
    }

    // ═══════════════════════════════════════════════════════════
    // TAB 2: Under Review
    // ═══════════════════════════════════════════════════════════

    private VBox buildUnderReviewPane() {
        VBox pane = new VBox(12);
        Text heading = sectionHeading("Papers Under Review");
        Label desc = smallNote("Papers you are currently reviewing. Provide feedback and make a decision.");
        pane.getChildren().addAll(heading, desc);

        List<ResearchPaper> underReview = paperController.getMyUnderReview();

        if (underReview.isEmpty()) {
            pane.getChildren().add(emptyLabel("No papers under review right now."));
        } else {
            for (ResearchPaper paper : underReview) {
                pane.getChildren().add(buildReviewCard(paper, false));
            }
        }

        ScrollPane scroll = new ScrollPane(pane);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        VBox wrapper = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return wrapper;
    }

    // ═══════════════════════════════════════════════════════════
    // TAB 3: Review History
    // ═══════════════════════════════════════════════════════════

    private VBox buildHistoryPane() {
        VBox pane = new VBox(12);
        Text heading = sectionHeading("Review History");
        Label desc = smallNote("Papers that have been published or rejected.");
        pane.getChildren().addAll(heading, desc);

        List<ResearchPaper> published = paperController.getPapersByStatus(ResearchPaper.PaperStatus.PUBLISHED);
        List<ResearchPaper> rejected = paperController.getPapersByStatus(ResearchPaper.PaperStatus.REJECTED);

        if (published.isEmpty() && rejected.isEmpty()) {
            pane.getChildren().add(emptyLabel("No review history yet."));
        } else {
            if (!published.isEmpty()) {
                Label pubHeader = new Label("✅ Published (" + published.size() + ")");
                pubHeader.setFont(Font.font("System", FontWeight.BOLD, 14));
                pubHeader.setTextFill(Color.web("#68d391"));
                pubHeader.setPadding(new Insets(8, 0, 4, 0));
                pane.getChildren().add(pubHeader);
                for (ResearchPaper p : published) pane.getChildren().add(buildHistoryCard(p));
            }
            if (!rejected.isEmpty()) {
                Label rejHeader = new Label("❌ Rejected (" + rejected.size() + ")");
                rejHeader.setFont(Font.font("System", FontWeight.BOLD, 14));
                rejHeader.setTextFill(Color.web("#fc8181"));
                rejHeader.setPadding(new Insets(12, 0, 4, 0));
                pane.getChildren().add(rejHeader);
                for (ResearchPaper p : rejected) pane.getChildren().add(buildHistoryCard(p));
            }
        }

        ScrollPane scroll = new ScrollPane(pane);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        VBox wrapper = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return wrapper;
    }

    // ═══════════════════════════════════════════════════════════
    // TAB 4: Search Papers
    // ═══════════════════════════════════════════════════════════

    private VBox buildSearchPane() {
        VBox pane = new VBox(12);
        Text heading = sectionHeading("Search All Papers");
        Label desc = smallNote("Browse and search the full paper database.");

        TextField searchField = new TextField();
        searchField.setPromptText("Search by title, abstract, keywords...");
        searchField.setStyle(fieldStyle());
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button searchBtn = new Button("🔍 Search");
        searchBtn.setStyle("-fx-background-color:#6c9bff;-fx-text-fill:white;-fx-font-weight:bold;" +
                           "-fx-background-radius:6px;-fx-cursor:hand;-fx-padding:8 20;");

        HBox searchRow = new HBox(10, searchField, searchBtn);
        searchRow.setAlignment(Pos.CENTER_LEFT);

        @SuppressWarnings("unchecked")
        TableView<ResearchPaper> table = new TableView<>();
        table.setStyle("-fx-background-color:#1a1f2e;-fx-border-color:#2d3748;-fx-border-radius:8px;-fx-background-radius:8px;");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.setPlaceholder(new Label("No results."));

        TableColumn<ResearchPaper, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setPrefWidth(280);

        TableColumn<ResearchPaper, String> authorCol = new TableColumn<>("Author");
        authorCol.setCellValueFactory(new PropertyValueFactory<>("author"));
        authorCol.setPrefWidth(150);

        TableColumn<ResearchPaper, String> domainCol = new TableColumn<>("Domain");
        domainCol.setCellValueFactory(new PropertyValueFactory<>("domain"));
        domainCol.setPrefWidth(150);

        TableColumn<ResearchPaper, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(110);

        table.getColumns().addAll(titleCol, authorCol, domainCol, statusCol);

        // Double-click to view details
        table.setRowFactory(tv -> {
            TableRow<ResearchPaper> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty()) showPaperDetail(row.getItem());
            });
            return row;
        });

        ObservableList<ResearchPaper> data = FXCollections.observableArrayList(paperController.getAllPapers());
        table.setItems(data);

        Runnable doSearch = () -> {
            String q = searchField.getText().trim();
            if (q.isEmpty()) {
                data.setAll(paperController.getAllPapers());
            } else {
                data.setAll(paperController.searchByQuery(q));
            }
        };
        searchBtn.setOnAction(e -> doSearch.run());
        searchField.setOnAction(e -> doSearch.run());

        pane.getChildren().addAll(heading, desc, searchRow, table);
        return pane;
    }

    // ═══════════════════════════════════════════════════════════
    // Review Card (for pending + under review)
    // ═══════════════════════════════════════════════════════════

    private VBox buildReviewCard(ResearchPaper paper, boolean isPending) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setMaxWidth(750);
        card.setStyle("-fx-background-color:#1a1f2e;-fx-background-radius:10px;" +
                      "-fx-border-color:#2d3748;-fx-border-radius:10px;");

        // Title + status
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label titleLbl = new Label(paper.getTitle());
        titleLbl.setFont(Font.font("System", FontWeight.BOLD, 15));
        titleLbl.setTextFill(Color.web("#e2e8f0"));

        String statusColor = isPending ? "#ecc94b" : "#6c9bff";
        String statusText = isPending ? "SUBMITTED" : "UNDER REVIEW";
        Label statusBadge = new Label(statusText);
        statusBadge.setStyle("-fx-background-color:" + statusColor + "22;-fx-text-fill:" + statusColor + ";" +
                             "-fx-padding:2 8;-fx-background-radius:8px;-fx-font-size:10px;-fx-font-weight:bold;");
        titleRow.getChildren().addAll(titleLbl, statusBadge);

        // Metadata
        Label authorLbl = new Label("Author: " + (paper.getAuthor() != null ? paper.getAuthor() : "—"));
        authorLbl.setTextFill(Color.web("#8892a4"));
        authorLbl.setFont(Font.font("System", 12));

        HBox metaRow = new HBox(16);
        Label domainLbl = new Label("Domain: " + (paper.getDomain() != null ? paper.getDomain() : "—"));
        domainLbl.setTextFill(Color.web("#6c9bff"));
        domainLbl.setFont(Font.font("System", 11));
        Label keywordsLbl = new Label("Keywords: " + (paper.getKeywords() != null ? paper.getKeywords() : "—"));
        keywordsLbl.setTextFill(Color.web("#718096"));
        keywordsLbl.setFont(Font.font("System", 11));
        metaRow.getChildren().addAll(domainLbl, keywordsLbl);

        // Abstract preview
        String abstractPreview = paper.getAbstractText() != null
            ? paper.getAbstractText().substring(0, Math.min(200, paper.getAbstractText().length())) + "..."
            : "No abstract provided.";
        Label abstractLbl = new Label(abstractPreview);
        abstractLbl.setTextFill(Color.web("#a0aec0"));
        abstractLbl.setFont(Font.font("System", 12));
        abstractLbl.setWrapText(true);
        abstractLbl.setMaxWidth(700);

        // View Paper button
        Button viewBtn = new Button("📄 View Full Paper");
        viewBtn.setStyle("-fx-background-color:#2d3748;-fx-text-fill:#a0aec0;-fx-background-radius:6px;" +
                         "-fx-cursor:hand;-fx-padding:6 14;-fx-font-size:11px;");
        viewBtn.setOnAction(e -> showPaperDetail(paper));

        // Review actions
        Label actionStatus = new Label("");
        actionStatus.setFont(Font.font("System", 11));

        HBox actionRow = new HBox(10);
        actionRow.setAlignment(Pos.CENTER_LEFT);
        actionRow.setPadding(new Insets(6, 0, 0, 0));

        if (isPending) {
            // Start Review button
            Button startReviewBtn = new Button("🔍 Start Review");
            startReviewBtn.setStyle("-fx-background-color:#6c9bff;-fx-text-fill:white;-fx-font-weight:bold;" +
                                    "-fx-background-radius:6px;-fx-cursor:hand;-fx-padding:8 18;");
            startReviewBtn.setOnAction(e -> {
                paperController.startReview(paper);
                actionStatus.setTextFill(Color.web("#68d391"));
                actionStatus.setText("✓ Assigned to you — moved to Under Review");
                startReviewBtn.setDisable(true);
                startReviewBtn.setText("Reviewing...");
            });
            actionRow.getChildren().addAll(viewBtn, startReviewBtn, actionStatus);
        } else {
            // Feedback area
            TextArea feedbackArea = new TextArea(paper.getReviewNotes() != null ? paper.getReviewNotes() : "");
            feedbackArea.setPromptText("Write your review feedback here...");
            feedbackArea.setPrefRowCount(3);
            feedbackArea.setWrapText(true);
            feedbackArea.setMaxWidth(700);
            feedbackArea.setStyle("-fx-background-color:#0f1117;-fx-text-fill:#e2e8f0;-fx-prompt-text-fill:#4a5568;" +
                                  "-fx-border-color:#2d3748;-fx-border-radius:6px;-fx-background-radius:6px;");

            Button saveFeedbackBtn = new Button("💾 Save Feedback");
            saveFeedbackBtn.setStyle("-fx-background-color:#2d3748;-fx-text-fill:#a0aec0;-fx-background-radius:6px;" +
                                    "-fx-cursor:hand;-fx-padding:6 14;");
            saveFeedbackBtn.setOnAction(e -> {
                paper.setReviewNotes(feedbackArea.getText().trim());
                paperController.savePaper(paper);
                actionStatus.setTextFill(Color.web("#68d391"));
                actionStatus.setText("✓ Feedback saved");
            });

            Button approveBtn = new Button("✅ Approve & Publish");
            approveBtn.setStyle("-fx-background-color:#68d391;-fx-text-fill:white;-fx-font-weight:bold;" +
                                "-fx-background-radius:6px;-fx-cursor:hand;-fx-padding:8 18;");
            approveBtn.setOnAction(e -> {
                paper.setReviewNotes(feedbackArea.getText().trim());
                paper.setStatus(ResearchPaper.PaperStatus.PUBLISHED);
                paperController.savePaper(paper);
                actionStatus.setTextFill(Color.web("#68d391"));
                actionStatus.setText("✅ Paper approved and published!");
                approveBtn.setDisable(true);
            });

            Button rejectBtn = new Button("❌ Reject");
            rejectBtn.setStyle("-fx-background-color:#fc8181;-fx-text-fill:white;-fx-font-weight:bold;" +
                               "-fx-background-radius:6px;-fx-cursor:hand;-fx-padding:8 18;");
            rejectBtn.setOnAction(e -> {
                if (feedbackArea.getText().trim().isEmpty()) {
                    actionStatus.setTextFill(Color.web("#fc8181"));
                    actionStatus.setText("Please provide feedback before rejecting.");
                    return;
                }
                paper.setReviewNotes(feedbackArea.getText().trim());
                paper.setStatus(ResearchPaper.PaperStatus.REJECTED);
                paperController.savePaper(paper);
                actionStatus.setTextFill(Color.web("#fc8181"));
                actionStatus.setText("Paper rejected with feedback.");
                rejectBtn.setDisable(true);
            });

            HBox decisionRow = new HBox(10, viewBtn, saveFeedbackBtn, approveBtn, rejectBtn, actionStatus);
            decisionRow.setAlignment(Pos.CENTER_LEFT);
            decisionRow.setPadding(new Insets(6, 0, 0, 0));

            card.getChildren().addAll(titleRow, authorLbl, metaRow, abstractLbl, feedbackArea, decisionRow);
            return card;
        }

        card.getChildren().addAll(titleRow, authorLbl, metaRow, abstractLbl, actionRow);
        return card;
    }

    // ═══════════════════════════════════════════════════════════
    // History Card
    // ═══════════════════════════════════════════════════════════

    private VBox buildHistoryCard(ResearchPaper paper) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(12, 16, 12, 16));
        card.setMaxWidth(700);
        card.setStyle("-fx-background-color:#1a1f2e;-fx-background-radius:8px;");

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label titleLbl = new Label(paper.getTitle());
        titleLbl.setFont(Font.font("System", FontWeight.BOLD, 13));
        titleLbl.setTextFill(Color.web("#e2e8f0"));

        boolean isPublished = paper.getStatus() == ResearchPaper.PaperStatus.PUBLISHED;
        String statusColor = isPublished ? "#68d391" : "#fc8181";
        Label statusBadge = new Label(paper.getStatus().name());
        statusBadge.setStyle("-fx-background-color:" + statusColor + "22;-fx-text-fill:" + statusColor + ";" +
                             "-fx-padding:2 8;-fx-background-radius:8px;-fx-font-size:10px;-fx-font-weight:bold;");
        titleRow.getChildren().addAll(titleLbl, statusBadge);

        Label authorLbl = new Label("By " + (paper.getAuthor() != null ? paper.getAuthor() : "—") +
            " · " + (paper.getDomain() != null ? paper.getDomain() : ""));
        authorLbl.setTextFill(Color.web("#8892a4"));
        authorLbl.setFont(Font.font("System", 11));

        card.getChildren().addAll(titleRow, authorLbl);

        if (paper.getReviewNotes() != null && !paper.getReviewNotes().isBlank()) {
            Label notesLbl = new Label("Review: " + paper.getReviewNotes());
            notesLbl.setTextFill(Color.web("#718096"));
            notesLbl.setFont(Font.font("System", FontPosture.ITALIC, 11));
            notesLbl.setWrapText(true);
            notesLbl.setMaxWidth(660);
            card.getChildren().add(notesLbl);
        }

        // View paper button
        Button viewBtn = new Button("📄 View");
        viewBtn.setStyle("-fx-background-color:#2d3748;-fx-text-fill:#a0aec0;-fx-background-radius:6px;" +
                         "-fx-cursor:hand;-fx-padding:4 12;-fx-font-size:10px;");
        viewBtn.setOnAction(e -> showPaperDetail(paper));
        card.getChildren().add(viewBtn);

        return card;
    }

    // ═══════════════════════════════════════════════════════════
    // Paper Detail Dialog
    // ═══════════════════════════════════════════════════════════

    private void showPaperDetail(ResearchPaper p) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Paper Details");
        dialog.setHeaderText(p.getTitle());

        VBox content = new VBox(8);
        content.setPadding(new Insets(12));
        content.setMinWidth(500);

        content.getChildren().addAll(
            detailRow("Author:", p.getAuthor()),
            detailRow("Domain:", p.getDomain()),
            detailRow("Keywords:", p.getKeywords()),
            detailRow("Status:", String.valueOf(p.getStatus())),
            detailRow("Link:", p.getLink() != null ? p.getLink() : "—")
        );

        Label abstractHeader = new Label("Abstract:");
        abstractHeader.setFont(Font.font("System", FontWeight.BOLD, 12));
        Label abstractContent = new Label(p.getAbstractText() != null ? p.getAbstractText() : "—");
        abstractContent.setWrapText(true);
        abstractContent.setMaxWidth(480);
        content.getChildren().addAll(abstractHeader, abstractContent);

        if (p.getReviewNotes() != null && !p.getReviewNotes().isBlank()) {
            Label reviewHeader = new Label("Review Notes:");
            reviewHeader.setFont(Font.font("System", FontWeight.BOLD, 12));
            reviewHeader.setTextFill(Color.web("#6c9bff"));
            Label reviewContent = new Label(p.getReviewNotes());
            reviewContent.setWrapText(true);
            reviewContent.setMaxWidth(480);
            content.getChildren().addAll(reviewHeader, reviewContent);
        }

        // View Paper file button
        String link = p.getLink();
        if (link != null && !link.isBlank()) {
            Button viewBtn = new Button("📄 Open Paper File");
            viewBtn.setStyle("-fx-background-color:#6c9bff;-fx-text-fill:white;-fx-font-weight:bold;" +
                             "-fx-background-radius:6px;-fx-cursor:hand;-fx-padding:8 20;");
            Label viewStatus = new Label("");
            viewBtn.setOnAction(e -> {
                try {
                    java.io.File file = new java.io.File(link);
                    if (file.exists()) {
                        new ProcessBuilder("cmd", "/c", "start", "", file.getAbsolutePath()).start();
                        viewStatus.setTextFill(Color.web("#68d391"));
                        viewStatus.setText("✓ Opened");
                    } else if (link.startsWith("http")) {
                        new ProcessBuilder("cmd", "/c", "start", link).start();
                        viewStatus.setTextFill(Color.web("#68d391"));
                        viewStatus.setText("✓ Opened in browser");
                    } else {
                        viewStatus.setTextFill(Color.web("#fc8181"));
                        viewStatus.setText("File not found: " + link);
                    }
                } catch (Exception ex) {
                    viewStatus.setTextFill(Color.web("#fc8181"));
                    viewStatus.setText("Error: " + (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()));
                }
            });
            content.getChildren().add(new HBox(10, viewBtn, viewStatus));
        }

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.getDialogPane().setPrefWidth(540);
        dialog.showAndWait();
    }

    private HBox detailRow(String label, String value) {
        Label l = new Label(label);
        l.setFont(Font.font("System", FontWeight.BOLD, 12));
        l.setMinWidth(80);
        Label v = new Label(value != null ? value : "—");
        v.setWrapText(true);
        return new HBox(8, l, v);
    }

    // ═══════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════

    private Text sectionHeading(String text) {
        Text t = new Text(text);
        t.setFont(Font.font("Georgia", FontWeight.BOLD, 18));
        t.setFill(Color.web("#e2e8f0"));
        return t;
    }

    private Label smallNote(String text) {
        Label l = new Label(text);
        l.setWrapText(true);
        l.setFont(Font.font("System", 13));
        l.setTextFill(Color.web("#8892a4"));
        return l;
    }

    private Label emptyLabel(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("System", FontPosture.ITALIC, 13));
        l.setTextFill(Color.web("#4a5568"));
        l.setPadding(new Insets(16, 0, 0, 0));
        return l;
    }

    private String fieldStyle() {
        return "-fx-background-color:#1a1f2e;-fx-text-fill:#e2e8f0;-fx-prompt-text-fill:#4a5568;" +
               "-fx-border-color:#2d3748;-fx-border-radius:6px;-fx-background-radius:6px;" +
               "-fx-pref-height:40px;-fx-font-size:13px;-fx-padding:0 12px;";
    }

    private ToggleButton tabBtn(String text, ToggleGroup g, boolean sel) {
        ToggleButton b = new ToggleButton(text);
        b.setToggleGroup(g);
        b.setSelected(sel);
        String base = "-fx-background-radius:6px;-fx-cursor:hand;-fx-font-size:12px;-fx-padding:8 14;";
        b.setStyle("-fx-background-color:#1a1f2e;-fx-text-fill:#8892a4;" + base);
        b.selectedProperty().addListener((obs, o, n) -> b.setStyle(n
            ? "-fx-background-color:#6c9bff;-fx-text-fill:white;-fx-font-weight:bold;" + base
            : "-fx-background-color:#1a1f2e;-fx-text-fill:#8892a4;" + base));
        return b;
    }
}
