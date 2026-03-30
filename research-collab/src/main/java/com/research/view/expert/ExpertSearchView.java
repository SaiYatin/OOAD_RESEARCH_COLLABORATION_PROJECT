package com.research.view.expert;

import com.research.model.Expert;
import com.research.service.ExpertService;
import com.research.service.RecommendationService;
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
 * ExpertSearchView — Members 2 + 4 combined view.
 *
 * Tabs:
 *   1. Find Experts (Recommendation — Member 4, Strategy pattern)
 *   2. Browse & Manage (Expert CRUD — Member 2)
 *   3. Add Expert (Manual entry — Member 2's minor use case)
 *
 * Member 2's features:
 *   - Browse full expert list with search
 *   - Edit any expert's research profile (triggers keyword re-extraction)
 *   - Add a new expert manually
 *   - Deactivate/reactivate experts
 *
 * Design Pattern: Factory (Expert created uniformly via ExpertService)
 * Design Principle: SRP, OCP
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
        VBox panel = new VBox(0);
        panel.setStyle("-fx-background-color: #0f1117;");

        VBox header = new VBox(4);
        header.setPadding(new Insets(0, 0, 16, 0));
        Text title = new Text("Experts & Recommendations");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 26));
        title.setFill(Color.web("#e2e8f0"));
        Text subtitle = new Text("AI-powered expert matching · Manage expert profiles · Add new experts");
        subtitle.setFont(Font.font("System", 13));
        subtitle.setFill(Color.web("#8892a4"));
        header.getChildren().addAll(title, subtitle);

        ToggleGroup tabGroup = new ToggleGroup();
        ToggleButton findTab   = tabBtn("🔬  Find Experts",    tabGroup, true);
        ToggleButton browseTab = tabBtn("📋  Browse & Manage", tabGroup, false);
        ToggleButton addTab    = tabBtn("➕  Add Expert",      tabGroup, false);
        HBox tabRow = new HBox(4, findTab, browseTab, addTab);
        tabRow.setPadding(new Insets(0, 0, 16, 0));

        StackPane content = new StackPane();
        VBox.setVgrow(content, Priority.ALWAYS);
        content.getChildren().add(buildFindPane());

        tabGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            content.getChildren().clear();
            if (n == findTab)   content.getChildren().add(buildFindPane());
            else if (n == browseTab) content.getChildren().add(buildBrowsePane());
            else if (n == addTab)    content.getChildren().add(buildAddPane());
        });

        panel.getChildren().addAll(header, tabRow, content);
        return panel;
    }

    // ══ TAB 1: Find Experts (Member 4 — Strategy pattern) ═══════════

    private VBox buildFindPane() {
        VBox pane = new VBox(16);

        // Strategy selector
        HBox stratRow = new HBox(16);
        stratRow.setAlignment(Pos.CENTER_LEFT);
        Label stratLbl = bold("Algorithm (Strategy pattern):");
        ToggleGroup stratGroup = new ToggleGroup();
        RadioButton kwRb  = radio("Keyword Match", stratGroup, true);
        RadioButton aiRb  = radio("AI via n8n", stratGroup, false);
        RadioButton hybRb = radio("Hybrid", stratGroup, false);
        Label stratInfo = new Label("fast local keyword scoring");
        stratInfo.setFont(Font.font("System", FontPosture.ITALIC, 11));
        stratInfo.setTextFill(Color.web("#4a5568"));
        stratGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            if (n == kwRb)  stratInfo.setText("fast local keyword scoring");
            if (n == aiRb)  stratInfo.setText("calls n8n Gemini Search Agent");
            if (n == hybRb) stratInfo.setText("keyword pre-filter + AI re-rank");
        });
        stratRow.getChildren().addAll(stratLbl, kwRb, aiRb, hybRb, stratInfo);

        // Search bar
        HBox searchRow = new HBox(12);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        TextField queryField = new TextField();
        queryField.setPromptText("e.g. 'machine learning', 'NLP', 'composite materials'…");
        queryField.setStyle(fieldStyle());
        HBox.setHgrow(queryField, Priority.ALWAYS);
        Button findBtn = primaryBtn("Find Experts");
        searchRow.getChildren().addAll(queryField, findBtn);

        Label resultLbl = new Label("");
        resultLbl.setFont(Font.font("System", 12));
        resultLbl.setTextFill(Color.web("#68d391"));

        VBox cards = new VBox(10);
        ScrollPane scroll = new ScrollPane(cards);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Runnable doFind = () -> {
            String q = queryField.getText().trim();
            if (q.isBlank()) { resultLbl.setText("Enter a query first."); resultLbl.setTextFill(Color.web("#fc8181")); return; }
            String mode = kwRb.isSelected() ? "keyword" : aiRb.isSelected() ? "ai" : "hybrid";
            List<Expert> experts = recommendationService.recommendWithStrategy(q, mode);
            cards.getChildren().clear();
            if (experts.isEmpty()) {
                Label none = new Label("No matching experts for: \"" + q + "\"");
                none.setTextFill(Color.web("#8892a4")); none.setFont(Font.font("System", 14));
                cards.getChildren().add(none);
                resultLbl.setText("0 results"); resultLbl.setTextFill(Color.web("#fc8181")); return;
            }
            resultLbl.setTextFill(Color.web("#68d391"));
            resultLbl.setText(experts.size() + " expert(s) — " + recommendationService.getCurrentStrategyName());
            for (int i = 0; i < experts.size(); i++) cards.getChildren().add(expertCard(experts.get(i), i+1, q));
        };

        findBtn.setOnAction(e -> doFind.run());
        queryField.setOnAction(e -> doFind.run());

        pane.getChildren().addAll(stratRow, searchRow, resultLbl, scroll);
        return pane;
    }

    // ══ TAB 2: Browse & Manage (Member 2 — Expert CRUD) ═════════════

    private VBox buildBrowsePane() {
        VBox pane = new VBox(14);

        // Search filter
        HBox searchRow = new HBox(12);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        TextField filterField = new TextField();
        filterField.setPromptText("Filter by name or research area…");
        filterField.setStyle(fieldStyle());
        HBox.setHgrow(filterField, Priority.ALWAYS);
        Button filterBtn = primaryBtn("Filter");
        Button showAllBtn = new Button("Show All");
        showAllBtn.setStyle(secondaryBtnStyle());
        searchRow.getChildren().addAll(filterField, filterBtn, showAllBtn);

        Label countLbl = new Label("");
        countLbl.setFont(Font.font("System", 12));
        countLbl.setTextFill(Color.web("#4a5568"));

        // Table
        TableView<Expert> table = buildExpertTable();
        ObservableList<Expert> data = FXCollections.observableArrayList();
        table.setItems(data);

        List<Expert> allActive = expertService.getAllActiveExperts();
        data.setAll(allActive);
        countLbl.setText(allActive.size() + " active experts");

        filterBtn.setOnAction(e -> {
            String kw = filterField.getText().trim();
            if (kw.isBlank()) { data.setAll(expertService.getAllActiveExperts()); return; }
            data.setAll(expertService.searchByKeyword(kw));
            countLbl.setText(data.size() + " result(s)");
        });
        showAllBtn.setOnAction(e -> {
            data.setAll(expertService.getAllActiveExperts());
            countLbl.setText(data.size() + " active experts");
            filterField.clear();
        });

        // Edit panel (shows when row selected)
        VBox editPanel = buildEditPanel(table, data);

        pane.getChildren().addAll(searchRow, countLbl, table, editPanel);
        return pane;
    }

    @SuppressWarnings("unchecked")
    private TableView<Expert> buildExpertTable() {
        TableView<Expert> t = new TableView<>();
        t.setStyle("-fx-background-color:#1a1f2e;-fx-border-color:#2d3748;-fx-border-radius:8px;-fx-background-radius:8px;");
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        t.setPrefHeight(320);
        t.setPlaceholder(new Label("No experts found."));

        TableColumn<Expert,String> nameCol  = expertCol("Name",        "name",        200);
        TableColumn<Expert,String> desigCol = expertCol("Designation", "designation", 130);
        TableColumn<Expert,String> emailCol = expertCol("Email",       "email",       200);
        TableColumn<Expert,String> domCol   = expertCol("Domain",      "domain",      160);

        t.getColumns().addAll(nameCol, desigCol, emailCol, domCol);
        return t;
    }

    private VBox buildEditPanel(TableView<Expert> table,
                                ObservableList<Expert> data) {
        // Edit section — visible when a row is selected
        VBox editBox = new VBox(10);
        editBox.setPadding(new Insets(16));
        editBox.setStyle("-fx-background-color:#1a1f2e;-fx-background-radius:10px;-fx-border-color:#2d3748;-fx-border-radius:10px;");

        Label editHeading = new Label("Edit Selected Expert");
        editHeading.setFont(Font.font("Georgia", FontWeight.BOLD, 15));
        editHeading.setTextFill(Color.web("#e2e8f0"));

        Label nameLbl    = new Label("No expert selected.");
        nameLbl.setFont(Font.font("System", 12)); nameLbl.setTextFill(Color.web("#8892a4"));

        TextArea researchArea = new TextArea();
        researchArea.setPromptText("Edit research areas — keywords are re-extracted automatically");
        researchArea.setPrefRowCount(3); researchArea.setWrapText(true);
        researchArea.setStyle(fieldStyle() + "-fx-pref-height:80px;");
        researchArea.setDisable(true);

        TextField domainField = new TextField();
        domainField.setPromptText("Domain");
        domainField.setStyle(fieldStyle());
        domainField.setDisable(true);

        HBox btnRow = new HBox(10);
        Button saveEditBtn = new Button("Save Changes");
        saveEditBtn.setStyle("-fx-background-color:#6c9bff;-fx-text-fill:white;-fx-font-weight:bold;" +
                             "-fx-background-radius:6px;-fx-pref-height:36px;-fx-pref-width:140px;-fx-cursor:hand;");
        saveEditBtn.setDisable(true);

        Button deactivateBtn = new Button("Deactivate");
        deactivateBtn.setStyle("-fx-background-color:#fc8181;-fx-text-fill:white;-fx-font-weight:bold;" +
                               "-fx-background-radius:6px;-fx-pref-height:36px;-fx-pref-width:120px;-fx-cursor:hand;");
        deactivateBtn.setDisable(true);

        Label editStatus = new Label("");
        editStatus.setFont(Font.font("System", 12));
        btnRow.getChildren().addAll(saveEditBtn, deactivateBtn);

        // Populate fields when row selected
        table.getSelectionModel().selectedItemProperty().addListener((obs, o, selected) -> {
            if (selected == null) return;
            nameLbl.setText("Editing: " + selected.getName() + " — " + selected.getEmail());
            researchArea.setText(selected.getResearchAreas() != null ? selected.getResearchAreas() : "");
            domainField.setText(selected.getDomain() != null ? selected.getDomain() : "");
            researchArea.setDisable(false);
            domainField.setDisable(false);
            saveEditBtn.setDisable(false);
            deactivateBtn.setDisable(false);
            editStatus.setText("");
        });

        saveEditBtn.setOnAction(e -> {
            Expert sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            try {
                // updateResearchProfile also re-extracts keywords — OCP in action
                Expert updated = expertService.updateResearchProfile(
                        sel.getExpertId(), researchArea.getText().trim());
                // Also update domain if user changed it
                if (!domainField.getText().isBlank()) {
                    updated.setDomain(domainField.getText().trim());
                }
                editStatus.setTextFill(Color.web("#68d391"));
                editStatus.setText("✓ Profile updated. Keywords re-extracted automatically.");
                data.setAll(expertService.getAllActiveExperts());
            } catch (Exception ex) {
                editStatus.setTextFill(Color.web("#fc8181")); editStatus.setText(ex.getMessage());
            }
        });

        deactivateBtn.setOnAction(e -> {
            Expert sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            Alert c = new Alert(Alert.AlertType.CONFIRMATION,
                "Deactivate " + sel.getName() + "? They won't appear in recommendations.",
                ButtonType.YES, ButtonType.NO);
            c.showAndWait().ifPresent(b -> {
                if (b == ButtonType.YES) {
                    expertService.deactivateExpert(sel.getExpertId());
                    data.setAll(expertService.getAllActiveExperts());
                    editStatus.setTextFill(Color.web("#ecc94b"));
                    editStatus.setText("Expert deactivated and removed from recommendations.");
                    saveEditBtn.setDisable(true); deactivateBtn.setDisable(true);
                    researchArea.setDisable(true); domainField.setDisable(true);
                }
            });
        });

        editBox.getChildren().addAll(editHeading, nameLbl,
            lbl("Research Areas"), researchArea,
            lbl("Domain"), domainField,
            btnRow, editStatus);
        return editBox;
    }

    // ══ TAB 3: Add Expert (Member 2 minor use case) ══════════════════

    private VBox buildAddPane() {
        VBox pane = new VBox(10);
        pane.setMaxWidth(580);

        Text heading = new Text("Add New Expert Manually");
        heading.setFont(Font.font("Georgia", FontWeight.BOLD, 20));
        heading.setFill(Color.web("#e2e8f0"));

        Label desc = new Label(
            "Manually add a professor or domain expert not yet in the database. " +
            "Keywords are extracted automatically from their research areas.");
        desc.setWrapText(true);
        desc.setFont(Font.font("System", 13));
        desc.setTextFill(Color.web("#8892a4"));

        TextField nameField    = formField("Full Name  e.g. Dr. Arti Arya *");
        TextField desigField   = formField("Designation  e.g. Teaching / Professor");
        TextField emailField   = formField("Email *");
        TextField phoneField   = formField("Phone");
        TextField institutionField = formField("Institution  (default: PES University)");
        TextArea researchArea  = new TextArea();
        researchArea.setPromptText("Research areas — comma-separated topics, will be parsed for keywords");
        researchArea.setPrefRowCount(4); researchArea.setWrapText(true);
        researchArea.setStyle(fieldStyle() + "-fx-pref-height:90px;");

        Button addBtn = primaryBtn("Add Expert");
        Label statusLbl = new Label("");
        statusLbl.setWrapText(true); statusLbl.setFont(Font.font("System", 12));

        addBtn.setOnAction(e -> {
            String name  = nameField.getText().trim();
            String email = emailField.getText().trim();
            if (name.isBlank() || email.isBlank()) {
                status(statusLbl, "Name and Email are required.", false); return;
            }
            try {
                String inst = institutionField.getText().isBlank()
                    ? "PES University" : institutionField.getText().trim();
                Expert expert = expertService.importFromCsvRow(
                    name,
                    desigField.getText().isBlank() ? "Teaching" : desigField.getText().trim(),
                    email,
                    phoneField.getText().trim(),
                    researchArea.getText().trim()
                );
                status(statusLbl,
                    "✓ Expert added: " + expert.getName() +
                    " (ID: " + expert.getExpertId() + "). Keywords extracted: " +
                    expert.getKeywords().size() + " terms.", true);
                // Clear form
                nameField.clear(); emailField.clear(); desigField.clear();
                phoneField.clear(); institutionField.clear(); researchArea.clear();
            } catch (Exception ex) {
                status(statusLbl, "Error: " + ex.getMessage(), false);
            }
        });

        Label note = new Label("ℹ  Expert data is normally seeded from n8n Workflow 1 (CSV). Use this for manual additions.");
        note.setWrapText(true);
        note.setFont(Font.font("System", FontPosture.ITALIC, 11));
        note.setTextFill(Color.web("#4a5568"));

        pane.getChildren().addAll(
            heading, desc,
            lbl("Name *"),          nameField,
            lbl("Designation"),     desigField,
            lbl("Email *"),         emailField,
            lbl("Phone"),           phoneField,
            lbl("Institution"),     institutionField,
            lbl("Research Areas"),  researchArea,
            addBtn, statusLbl, note
        );
        return pane;
    }

    // ══ Expert card (used in Find tab) ═══════════════════════════════

    private HBox expertCard(Expert expert, int rank, String query) {
        HBox card = new HBox(16);
        card.setPadding(new Insets(14, 18, 14, 18));
        card.setStyle("-fx-background-color:#1a1f2e;-fx-background-radius:10px;" +
                      "-fx-border-color:#2d3748;-fx-border-radius:10px;");
        card.setAlignment(Pos.CENTER_LEFT);

        Label rankBadge = new Label("#" + rank);
        rankBadge.setFont(Font.font("Georgia", FontWeight.BOLD, 16));
        rankBadge.setTextFill(Color.web("#6c9bff"));
        rankBadge.setMinWidth(34);

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Text name = new Text(expert.getName());
        name.setFont(Font.font("Georgia", FontWeight.BOLD, 15));
        name.setFill(Color.web("#e2e8f0"));

        Label desig = new Label(expert.getDesignation() + " — " + expert.getInstitution());
        desig.setFont(Font.font("System", 11)); desig.setTextFill(Color.web("#8892a4"));

        Label domain = new Label(expert.getDomain() != null ? expert.getDomain() : "General");
        domain.setFont(Font.font("System", FontWeight.BOLD, 11));
        domain.setTextFill(Color.web("#6c9bff"));
        domain.setStyle("-fx-background-color:#6c9bff22;-fx-padding:2 8;-fx-background-radius:10px;");

        String preview = expert.getResearchAreas() != null
            ? expert.getResearchAreas().substring(0, Math.min(100, expert.getResearchAreas().length())) + "…"
            : "Research areas not listed.";
        Label research = new Label(preview);
        research.setFont(Font.font("System", 11)); research.setTextFill(Color.web("#718096"));
        research.setWrapText(true);

        info.getChildren().addAll(name, desig, domain, research);

        VBox actions = new VBox(6);
        actions.setAlignment(Pos.CENTER);
        Button contactBtn = new Button("✉ Contact");
        contactBtn.setStyle("-fx-background-color:#6c9bff;-fx-text-fill:white;-fx-font-size:11px;" +
                            "-fx-pref-width:95px;-fx-pref-height:32px;-fx-background-radius:6px;-fx-cursor:hand;");
        contactBtn.setOnAction(e -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("Contact"); a.setHeaderText(expert.getName());
            a.setContentText("Email: "+expert.getEmail()+"\nPhone: "+expert.getPhone()+
                "\n\nResearch:\n"+expert.getResearchAreas());
            a.showAndWait();
        });
        Label score = new Label("Score: " + String.format("%.1f", expert.scoreAgainst(query)));
        score.setFont(Font.font("System", 10)); score.setTextFill(Color.web("#4a5568"));
        actions.getChildren().addAll(contactBtn, score);

        card.getChildren().addAll(rankBadge, info, actions);
        return card;
    }

    // ══ Helpers ══════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private TableColumn<Expert,String> expertCol(String label, String prop, double w) {
        TableColumn<Expert,String> c = new TableColumn<>(label);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(w);
        c.setStyle("-fx-background-color:#0f1117;-fx-text-fill:#8892a4;-fx-font-weight:bold;-fx-font-size:12px;");
        return c;
    }

    private String fieldStyle() {
        return "-fx-background-color:#1a1f2e;-fx-text-fill:#e2e8f0;-fx-prompt-text-fill:#4a5568;" +
               "-fx-border-color:#2d3748;-fx-border-radius:6px;-fx-background-radius:6px;" +
               "-fx-pref-height:40px;-fx-font-size:13px;-fx-padding:0 12px;";
    }

    private TextField formField(String prompt) {
        TextField f = new TextField(); f.setPromptText(prompt);
        f.setStyle(fieldStyle()); f.setMaxWidth(Double.MAX_VALUE); return f;
    }

    private Label lbl(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("System", FontWeight.BOLD, 11));
        l.setTextFill(Color.web("#8892a4")); return l;
    }

    private Label bold(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("System", FontWeight.BOLD, 12));
        l.setTextFill(Color.web("#6c9bff")); return l;
    }

    private Button primaryBtn(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:#6c9bff;-fx-text-fill:white;-fx-font-weight:bold;" +
                   "-fx-pref-height:40px;-fx-pref-width:140px;-fx-background-radius:6px;-fx-cursor:hand;");
        return b;
    }

    private String secondaryBtnStyle() {
        return "-fx-background-color:#2d3748;-fx-text-fill:#a0aec0;-fx-font-weight:bold;" +
               "-fx-background-radius:6px;-fx-pref-height:36px;-fx-pref-width:120px;-fx-cursor:hand;";
    }

    private ToggleButton tabBtn(String text, ToggleGroup g, boolean sel) {
        ToggleButton b = new ToggleButton(text); b.setToggleGroup(g); b.setSelected(sel);
        String base = "-fx-background-radius:6px;-fx-cursor:hand;-fx-font-size:12px;-fx-padding:8 16;";
        b.setStyle("-fx-background-color:#1a1f2e;-fx-text-fill:#8892a4;" + base);
        b.selectedProperty().addListener((obs, o, n) -> b.setStyle(n
            ? "-fx-background-color:#6c9bff;-fx-text-fill:white;-fx-font-weight:bold;" + base
            : "-fx-background-color:#1a1f2e;-fx-text-fill:#8892a4;" + base));
        return b;
    }

    private RadioButton radio(String text, ToggleGroup g, boolean sel) {
        RadioButton r = new RadioButton(text); r.setToggleGroup(g); r.setSelected(sel);
        r.setStyle("-fx-text-fill:#a0aec0;-fx-font-size:12px;-fx-cursor:hand;"); return r;
    }

    private void status(Label l, String msg, boolean ok) {
        l.setTextFill(Color.web(ok ? "#68d391" : "#fc8181")); l.setText(msg);
    }
}
