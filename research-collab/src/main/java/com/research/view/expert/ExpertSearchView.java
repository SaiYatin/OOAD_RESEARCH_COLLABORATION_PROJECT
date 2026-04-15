package com.research.view.expert;

import com.research.model.Expert;
import com.research.model.Researcher;
import com.research.model.ResearchProject;
import com.research.model.User;
import com.research.controller.ExpertController;
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

import java.util.ArrayList;
import java.util.List;

/**
 * ExpertSearchView — View layer for Expert Search & Recommendation.
 *
 * @author Member 2
 * @usecase Expert Search, Recommendation & Expert CRUD
 *
 * Tabs:
 *   1. Find Experts (Strategy pattern — swappable recommendation algorithms)
 *   2. Browse & Manage (Expert CRUD)
 *   3. Add Expert (Manual entry)
 *
 * Design Pattern demonstrated: Strategy (algorithm selection in UI)
 * Design Principle: OCP (new strategies without modifying existing code)
 *
 * MVC Role: View — delegates all business logic to ExpertController
 */
@Component
public class ExpertSearchView {

    private final ExpertController expertController;

    public ExpertSearchView(ExpertController expertController) {
        this.expertController = expertController;
    }

    private boolean visitorMode = false;
    private javafx.stage.Stage visitorStage = null;

    public VBox buildPanel() { return buildPanel(false, null); }

    public VBox buildPanel(boolean visitorMode, javafx.stage.Stage stage) {
        this.visitorMode = visitorMode;
        this.visitorStage = stage;

        VBox panel = new VBox(0);
        panel.setStyle("-fx-background-color: #0f1117;");

        VBox header = new VBox(4);
        header.setPadding(new Insets(0, 0, 16, 0));
        Text title = new Text("Experts & Recommendations");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 26));
        title.setFill(Color.web("#e2e8f0"));
        Text subtitle = new Text(visitorMode
            ? "Browse expert profiles and researcher contact details"
            : "AI-powered expert matching · Browse expert profiles · Send collaboration requests");
        subtitle.setFont(Font.font("System", 13));
        subtitle.setFill(Color.web("#8892a4"));
        header.getChildren().addAll(title, subtitle);

        ToggleGroup tabGroup = new ToggleGroup();
        ToggleButton findTab   = tabBtn("🔬  Find Experts",    tabGroup, true);
        ToggleButton browseTab = tabBtn("📋  Browse & Manage", tabGroup, false);
        HBox tabRow = new HBox(4, findTab, browseTab);
        tabRow.setPadding(new Insets(0, 0, 16, 0));

        StackPane content = new StackPane();
        VBox.setVgrow(content, Priority.ALWAYS);
        content.getChildren().add(buildFindPane());

        tabGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            content.getChildren().clear();
            if (n == findTab)   content.getChildren().add(buildFindPane());
            else if (n == browseTab) content.getChildren().add(buildBrowsePane());
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
            List<Expert> experts = expertController.findExperts(q, mode);
            cards.getChildren().clear();

            // Also search registered researchers matching the query
            List<Researcher> researchers = expertController.findResearchers(q);
            User currentUser = expertController.getCurrentUser();

            int totalResults = experts.size() + researchers.size();
            if (totalResults == 0) {
                Label none = new Label("No matching experts or researchers for: \"" + q + "\"");
                none.setTextFill(Color.web("#8892a4")); none.setFont(Font.font("System", 14));
                cards.getChildren().add(none);
                resultLbl.setText("0 results"); resultLbl.setTextFill(Color.web("#fc8181")); return;
            }

            resultLbl.setTextFill(Color.web("#68d391"));
            resultLbl.setText(experts.size() + " expert(s) + " + researchers.size() + " researcher(s)");

            // Show experts
            if (!experts.isEmpty()) {
                Label expHeader = new Label("📚 Faculty / CSV Experts");
                expHeader.setFont(Font.font("System", FontWeight.BOLD, 14));
                expHeader.setTextFill(Color.web("#6c9bff"));
                expHeader.setPadding(new Insets(4, 0, 4, 0));
                cards.getChildren().add(expHeader);
                for (int i = 0; i < experts.size(); i++) cards.getChildren().add(expertCard(experts.get(i), i+1, q));
            }

            // Show researchers
            if (!researchers.isEmpty()) {
                Label resHeader = new Label("👤 Registered Researchers");
                resHeader.setFont(Font.font("System", FontWeight.BOLD, 14));
                resHeader.setTextFill(Color.web("#68d391"));
                resHeader.setPadding(new Insets(8, 0, 4, 0));
                cards.getChildren().add(resHeader);
                for (Researcher r : researchers) {
                    // Don't show self
                    if (currentUser != null && r.getUserId().equals(currentUser.getUserId())) continue;
                    cards.getChildren().add(researcherCard(r));
                }
            }
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

        List<Expert> allActive = expertController.getAllExperts();
        data.setAll(allActive);
        countLbl.setText(allActive.size() + " active experts");

        filterBtn.setOnAction(e -> {
            String kw = filterField.getText().trim();
            if (kw.isBlank()) { data.setAll(expertController.getAllExperts()); return; }
            data.setAll(expertController.searchExperts(kw));
            countLbl.setText(data.size() + " result(s)");
        });
        showAllBtn.setOnAction(e -> {
            data.setAll(expertController.getAllExperts());
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
                Expert updated = expertController.updateExpertProfile(
                        sel.getExpertId(), researchArea.getText().trim());
                // Also update domain if user changed it
                if (!domainField.getText().isBlank()) {
                    updated.setDomain(domainField.getText().trim());
                }
                editStatus.setTextFill(Color.web("#68d391"));
                editStatus.setText("✓ Profile updated. Keywords re-extracted automatically.");
                data.setAll(expertController.getAllExperts());
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
                    expertController.toggleExpertStatus(sel.getExpertId());
                    data.setAll(expertController.getAllExperts());
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
                Expert expert = expertController.addExpert(
                    name,
                    desigField.getText().isBlank() ? "Teaching" : desigField.getText().trim(),
                    email,
                    phoneField.getText().trim(),
                    inst,
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

    /**
     * Card for registered researchers shown in search results.
     * Includes a "Send Request" button for collaboration.
     */
    private HBox researcherCard(Researcher r) {
        HBox card = new HBox(16);
        card.setPadding(new Insets(14, 18, 14, 18));
        card.setStyle("-fx-background-color:#1a1f2e;-fx-background-radius:10px;" +
                      "-fx-border-color:#68d39144;-fx-border-radius:10px;");
        card.setAlignment(Pos.CENTER_LEFT);

        Label userIcon = new Label("👤");
        userIcon.setFont(Font.font("System", 22));
        userIcon.setMinWidth(34);

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Text rName = new Text(r.getName());
        rName.setFont(Font.font("Georgia", FontWeight.BOLD, 15));
        rName.setFill(Color.web("#e2e8f0"));

        Label emailLbl = new Label(r.getEmail());
        emailLbl.setFont(Font.font("System", 11));
        emailLbl.setTextFill(Color.web("#8892a4"));

        Label interests = new Label(r.getResearchInterests() != null
            ? r.getResearchInterests() : "No interests listed");
        interests.setFont(Font.font("System", 11));
        interests.setTextFill(Color.web("#718096"));
        interests.setWrapText(true);

        Label roleBadge = new Label("RESEARCHER");
        roleBadge.setStyle("-fx-background-color:#68d39122;-fx-text-fill:#68d391;" +
                           "-fx-padding:2 8;-fx-background-radius:10px;-fx-font-size:10px;");

        info.getChildren().addAll(rName, emailLbl, interests, roleBadge);

        VBox actions = new VBox(6);
        actions.setAlignment(Pos.CENTER);

        Label reqStatus = new Label("");
        reqStatus.setFont(Font.font("System", 10));

        Button requestBtn = new Button("🤝 Collaborate");
        requestBtn.setStyle("-fx-background-color:#68d391;-fx-text-fill:#1a1f2e;-fx-font-size:11px;" +
                            "-fx-pref-width:110px;-fx-pref-height:32px;-fx-background-radius:6px;" +
                            "-fx-cursor:hand;-fx-font-weight:bold;");
        requestBtn.setOnAction(e -> {
            User currentUser = expertController.getCurrentUser();
            if (currentUser == null) {
                // Visitor mode — show login dialog
                if (visitorStage != null) {
                    com.research.view.dashboard.DashboardView dv =
                        com.research.ResearchCollaborationApp.getSpringContext()
                            .getBean(com.research.view.dashboard.DashboardView.class);
                    dv.showLoginDialog(visitorStage, false);
                } else {
                    reqStatus.setTextFill(Color.web("#fc8181"));
                    reqStatus.setText("Login first");
                }
                return;
            }
            // Get user's projects for selection
            List<ResearchProject> myProjects = new ArrayList<>();
            try {
                if (currentUser instanceof Researcher) {
                    myProjects = expertController.getMyProjects();
                }
            } catch (Exception ignored) {}

            if (myProjects.isEmpty()) {
                // No projects — send without project link
                Alert noProj = new Alert(Alert.AlertType.CONFIRMATION);
                noProj.setTitle("No Projects");
                noProj.setHeaderText("You don't have any projects yet.");
                noProj.setContentText("Create a project in 'My Researches' first, or send a general collaboration request?");
                noProj.getButtonTypes().setAll(new ButtonType("Send General Request"), ButtonType.CANCEL);
                noProj.showAndWait().ifPresent(bt -> {
                    if (bt.getText().equals("Send General Request")) {
                        try {
                            expertController.sendCollaborationRequest(
                                r.getUserId(), null,
                                "Hi! I'd like to collaborate with you.");
                            reqStatus.setTextFill(Color.web("#68d391"));
                            reqStatus.setText("Request sent!");
                            requestBtn.setDisable(true);
                            requestBtn.setText("Sent \u2713");
                        } catch (Exception ex) {
                            reqStatus.setTextFill(Color.web("#fc8181"));
                            reqStatus.setText(ex.getMessage());
                        }
                    }
                });
            } else {
                // Show project selection dialog
                ChoiceDialog<String> dialog = new ChoiceDialog<>();
                dialog.setTitle("Select Project");
                dialog.setHeaderText("Which project do you want to collaborate on with " + r.getName() + "?");
                dialog.setContentText("Project:");
                List<ResearchProject> finalProjects = myProjects;
                for (ResearchProject p : myProjects) {
                    dialog.getItems().add(p.getTopic() + " (ID:" + p.getProjectId() + ")");
                }
                if (!dialog.getItems().isEmpty()) dialog.setSelectedItem(dialog.getItems().get(0));
                dialog.showAndWait().ifPresent(selected -> {
                    try {
                        // Extract project ID from selection
                        String idStr = selected.substring(selected.lastIndexOf("ID:") + 3, selected.length() - 1);
                        Long projectId = Long.parseLong(idStr);
                        ResearchProject proj = finalProjects.stream()
                            .filter(p -> p.getProjectId().equals(projectId))
                            .findFirst().orElse(null);
                        String msg = "Hi! I'd like to collaborate on '" + 
                            (proj != null ? proj.getTopic() : "a project") + "' with you.";
                        expertController.sendCollaborationRequest(
                            r.getUserId(), projectId, msg);
                        reqStatus.setTextFill(Color.web("#68d391"));
                        reqStatus.setText("Request sent!");
                        requestBtn.setDisable(true);
                        requestBtn.setText("Sent \u2713");
                    } catch (Exception ex) {
                        reqStatus.setTextFill(Color.web("#fc8181"));
                        reqStatus.setText(ex.getMessage());
                    }
                });
            }
        });

        actions.getChildren().addAll(requestBtn, reqStatus);
        card.getChildren().addAll(userIcon, info, actions);
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
