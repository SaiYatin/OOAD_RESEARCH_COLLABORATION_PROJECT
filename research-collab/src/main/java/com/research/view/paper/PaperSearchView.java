package com.research.view.paper;

import com.research.model.ResearchPaper;
import com.research.model.Researcher;
import com.research.model.User;
import com.research.pattern.BasicPaperSearch;
import com.research.pattern.DomainFilterDecorator;
import com.research.pattern.PublishedOnlyDecorator;
import com.research.repository.ResearchPaperRepository;
import com.research.service.AuthService;
import com.research.service.PaperService;
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
 * PaperSearchView — Member 1's complete view.
 *
 * Tabs:
 *   1. Search All  — keyword search + Decorator chain (Published / Domain filters)
 *   2. Upload Paper — title/author/domain/keywords/abstract form → DRAFT → submit
 *   3. My Papers   — table of own papers, submit / publish / delete actions
 *
 * Design Pattern demonstrated: Decorator (search filter chain shown live in UI)
 * Design Principle: SRP (each tab one responsibility), DIP (uses PaperService)
 */
@Component
public class PaperSearchView {

    private final ResearchPaperRepository paperRepository;
    private final BasicPaperSearch        basicSearch;
    private final DomainFilterDecorator   domainDecorator;
    private final PublishedOnlyDecorator  publishedDecorator;
    private final PaperService            paperService;   // ← field properly declared

    public PaperSearchView(ResearchPaperRepository paperRepository,
                           BasicPaperSearch basicSearch,
                           DomainFilterDecorator domainDecorator,
                           PublishedOnlyDecorator publishedDecorator,
                           PaperService paperService) {
        this.paperRepository    = paperRepository;
        this.basicSearch        = basicSearch;
        this.domainDecorator    = domainDecorator;
        this.publishedDecorator = publishedDecorator;
        this.paperService       = paperService;          // ← properly assigned
    }

    // ── Top-level panel ───────────────────────────────────────────────
    public VBox buildPanel() {
        VBox panel = new VBox(0);
        panel.setStyle("-fx-background-color:#0f1117;");

        // Page header
        VBox header = new VBox(4);
        header.setPadding(new Insets(0, 0, 16, 0));
        Text title = new Text("Research Papers");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 26));
        title.setFill(Color.web("#e2e8f0"));
        Text subtitle = new Text("Search the database · Upload your work · Manage your papers");
        subtitle.setFont(Font.font("System", 13));
        subtitle.setFill(Color.web("#8892a4"));
        header.getChildren().addAll(title, subtitle);

        // Tab strip
        ToggleGroup tabGroup = new ToggleGroup();
        ToggleButton searchTab   = tabBtn("🔍  Search All",   tabGroup, true);
        ToggleButton uploadTab   = tabBtn("📤  Upload Paper", tabGroup, false);
        ToggleButton myPapersTab = tabBtn("📁  My Papers",    tabGroup, false);
        HBox tabRow = new HBox(4, searchTab, uploadTab, myPapersTab);
        tabRow.setPadding(new Insets(0, 0, 16, 0));

        StackPane content = new StackPane();
        VBox.setVgrow(content, Priority.ALWAYS);
        content.getChildren().add(buildSearchPane());

        tabGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            content.getChildren().clear();
            if      (n == searchTab)   content.getChildren().add(buildSearchPane());
            else if (n == uploadTab)   content.getChildren().add(buildUploadPane());
            else if (n == myPapersTab) content.getChildren().add(buildMyPapersPane());
        });

        panel.getChildren().addAll(header, tabRow, content);
        return panel;
    }

    // ══ TAB 1 — Search All (Decorator pattern) ════════════════════════
    private VBox buildSearchPane() {
        VBox pane = new VBox(14);

        // Search bar
        HBox searchRow = new HBox(12);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        TextField searchField = new TextField();
        searchField.setPromptText("Search by title, abstract, keywords…");
        searchField.setStyle(fieldStyle());
        HBox.setHgrow(searchField, Priority.ALWAYS);
        Button searchBtn = primaryBtn("Search");
        searchRow.getChildren().addAll(searchField, searchBtn);

        // Decorator filter controls
        HBox filterRow = new HBox(16);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        Label filterLbl = new Label("Filters:");
        filterLbl.setFont(Font.font("System", FontWeight.BOLD, 12));
        filterLbl.setTextFill(Color.web("#6c9bff"));

        CheckBox publishedCb = new CheckBox("Published Only");
        publishedCb.setStyle("-fx-text-fill:#a0aec0;-fx-font-size:12px;");

        TextField domainField = new TextField();
        domainField.setPromptText("Domain filter");
        domainField.setStyle(fieldStyle() + "-fx-pref-width:200px;-fx-pref-height:34px;");

        // Live decorator chain label — shows the pattern in action
        Label chainLbl = new Label("Decorator chain: BasicSearch");
        chainLbl.setFont(Font.font("Courier New", 10));
        chainLbl.setTextFill(Color.web("#4a5568"));

        Runnable updateChain = () -> {
            String c = "BasicSearch";
            if (publishedCb.isSelected())        c += " → PublishedOnly";
            if (!domainField.getText().isBlank()) c += " → DomainFilter";
            chainLbl.setText("Decorator chain: " + c);
        };
        publishedCb.setOnAction(e -> updateChain.run());
        domainField.textProperty().addListener((o, ov, nv) -> updateChain.run());
        filterRow.getChildren().addAll(filterLbl, publishedCb, domainField, chainLbl);

        Label countLbl = new Label("");
        countLbl.setFont(Font.font("System", 12));
        countLbl.setTextFill(Color.web("#4a5568"));

        TableView<ResearchPaper> table = buildTable(false);
        ObservableList<ResearchPaper> data = FXCollections.observableArrayList();
        table.setItems(data);

        // Load all on open
        List<ResearchPaper> all = paperRepository.findAll();
        data.setAll(all);
        countLbl.setText(all.size() + " papers in database");

        Runnable doSearch = () -> {
            String q      = searchField.getText().trim();
            List<ResearchPaper> papers = paperRepository.findAll();
            List<ResearchPaper> result;

            if (publishedCb.isSelected() && !domainField.getText().isBlank()) {
                result = domainDecorator.withDomain(domainField.getText().trim())
                         .search(q, publishedDecorator.search(q, papers));
            } else if (publishedCb.isSelected()) {
                result = publishedDecorator.search(q, papers);
            } else if (!domainField.getText().isBlank()) {
                result = domainDecorator.withDomain(domainField.getText().trim())
                         .search(q, papers);
            } else {
                result = basicSearch.search(q, papers);
            }

            data.setAll(result);
            countLbl.setText(result.size() + " result(s) — " + chainLbl.getText());
        };

        searchBtn.setOnAction(e -> doSearch.run());
        searchField.setOnAction(e -> doSearch.run());

        pane.getChildren().addAll(searchRow, filterRow, countLbl, table);
        return pane;
    }

    // ══ TAB 2 — Upload Paper ══════════════════════════════════════════
    private VBox buildUploadPane() {
        VBox pane = new VBox(10);
        pane.setMaxWidth(600);

        Text heading = new Text("Upload Research Paper");
        heading.setFont(Font.font("Georgia", FontWeight.BOLD, 20));
        heading.setFill(Color.web("#e2e8f0"));

        TextField titleField    = formField("Paper Title *");
        TextField authorField   = formField("Author(s) *");
        TextField domainField   = formField("Domain  (e.g. Artificial Intelligence)");
        TextField keywordsField = formField("Keywords — comma-separated");
        TextField linkField     = formField("Paper URL / DOI  (optional)");

        TextArea abstractArea = new TextArea();
        abstractArea.setPromptText("Abstract — brief summary of the paper");
        abstractArea.setPrefRowCount(4);
        abstractArea.setWrapText(true);
        abstractArea.setStyle(fieldStyle() + "-fx-pref-height:90px;");

        Button saveBtn   = primaryBtn("Save as Draft");
        Button submitBtn = new Button("Submit for Review");
        submitBtn.setStyle(secondaryBtnStyle());
        submitBtn.setDisable(true);
        HBox btnRow = new HBox(12, saveBtn, submitBtn);

        Label statusLbl = new Label("");
        statusLbl.setWrapText(true);
        statusLbl.setFont(Font.font("System", 12));

        final Long[] lastId = {null};

        saveBtn.setOnAction(e -> {
            String t = titleField.getText().trim();
            String a = authorField.getText().trim();
            if (t.isBlank() || a.isBlank()) {
                status(statusLbl, "Title and Author are required.", false);
                return;
            }
            User cu = AuthService.getCurrentUser();
            if (!(cu instanceof Researcher researcher)) {
                status(statusLbl, "Only Researcher accounts can upload papers.", false);
                return;
            }
            ResearchPaper p = new ResearchPaper();
            p.setTitle(t);
            p.setAuthor(a);
            p.setDomain(domainField.getText().trim());
            p.setKeywords(keywordsField.getText().trim());
            p.setLink(linkField.getText().trim());
            p.setAbstractText(abstractArea.getText().trim());

            ResearchPaper saved = paperService.uploadPaper(p, researcher);
            lastId[0] = saved.getPaperId();
            status(statusLbl, "✓ Saved as DRAFT (ID " + saved.getPaperId()
                   + "). Now submit it for review.", true);
            submitBtn.setDisable(false);
        });

        submitBtn.setOnAction(e -> {
            if (lastId[0] == null) { status(statusLbl, "Save paper first.", false); return; }
            try {
                paperService.submitForReview(lastId[0]);
                status(statusLbl, "✓ Submitted for review! Status: SUBMITTED.", true);
                submitBtn.setDisable(true);
            } catch (Exception ex) {
                status(statusLbl, ex.getMessage(), false);
            }
        });

        Label note = new Label(
            "ℹ  Publishing a paper triggers the Observer pattern → " +
            "n8n Workflow 3 emails users following matching keywords.");
        note.setWrapText(true);
        note.setFont(Font.font("System", FontPosture.ITALIC, 11));
        note.setTextFill(Color.web("#4a5568"));

        pane.getChildren().addAll(
            heading,
            lbl("Title *"),     titleField,
            lbl("Author(s) *"), authorField,
            lbl("Domain"),      domainField,
            lbl("Keywords"),    keywordsField,
            lbl("Link / DOI"),  linkField,
            lbl("Abstract"),    abstractArea,
            btnRow, statusLbl, note
        );
        return pane;
    }

    // ══ TAB 3 — My Papers ═════════════════════════════════════════════
    private VBox buildMyPapersPane() {
        VBox pane = new VBox(14);

        Text heading = new Text("My Papers");
        heading.setFont(Font.font("Georgia", FontWeight.BOLD, 20));
        heading.setFill(Color.web("#e2e8f0"));

        User cu = AuthService.getCurrentUser();
        if (!(cu instanceof Researcher researcher)) {
            Label note = new Label("Log in as a Researcher to see your papers.");
            note.setTextFill(Color.web("#8892a4"));
            pane.getChildren().addAll(heading, note);
            return pane;
        }

        List<ResearchPaper> mine = paperService.getPapersByResearcher(researcher);

        Label countLbl = new Label(mine.size() + " paper(s)");
        countLbl.setFont(Font.font("System", 12));
        countLbl.setTextFill(Color.web("#4a5568"));

        TableView<ResearchPaper> table = buildTable(true);
        ObservableList<ResearchPaper> data = FXCollections.observableArrayList(mine);
        table.setItems(data);

        Label actionStatus = new Label("");
        actionStatus.setFont(Font.font("System", 12));

        Button submitBtn  = new Button("Submit for Review");
        submitBtn.setStyle(secondaryBtnStyle());
        Button publishBtn = new Button("✓ Publish");
        publishBtn.setStyle(
            "-fx-background-color:#68d391;-fx-text-fill:#1a1f2e;-fx-font-weight:bold;" +
            "-fx-background-radius:6px;-fx-pref-height:36px;-fx-pref-width:120px;-fx-cursor:hand;");
        Button deleteBtn  = new Button("✕ Delete");
        deleteBtn.setStyle(
            "-fx-background-color:#fc8181;-fx-text-fill:white;-fx-font-weight:bold;" +
            "-fx-background-radius:6px;-fx-pref-height:36px;-fx-pref-width:100px;-fx-cursor:hand;");
        HBox actions = new HBox(10, submitBtn, publishBtn, deleteBtn);

        Runnable refresh = () -> {
            data.setAll(paperService.getPapersByResearcher(researcher));
            countLbl.setText(data.size() + " paper(s)");
        };

        submitBtn.setOnAction(e -> {
            ResearchPaper sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { status(actionStatus, "Select a paper.", false); return; }
            try {
                paperService.submitForReview(sel.getPaperId());
                refresh.run();
                status(actionStatus, "✓ Submitted for review.", true);
            } catch (Exception ex) { status(actionStatus, ex.getMessage(), false); }
        });

        publishBtn.setOnAction(e -> {
            ResearchPaper sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { status(actionStatus, "Select a paper.", false); return; }
            try {
                paperService.publishPaper(sel.getPaperId());
                refresh.run();
                status(actionStatus,
                    "✓ Published! Observer pattern fired → n8n email notifications sent.", true);
            } catch (Exception ex) { status(actionStatus, ex.getMessage(), false); }
        });

        deleteBtn.setOnAction(e -> {
            ResearchPaper sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { status(actionStatus, "Select a paper.", false); return; }
            Alert c = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete \"" + sel.getTitle() + "\"?", ButtonType.YES, ButtonType.NO);
            c.showAndWait().ifPresent(b -> {
                if (b == ButtonType.YES) {
                    paperService.deletePaper(sel.getPaperId());
                    refresh.run();
                    status(actionStatus, "Paper deleted.", true);
                }
            });
        });

        pane.getChildren().addAll(heading, countLbl, table, actions, actionStatus);
        return pane;
    }

    // ══ Shared table builder ══════════════════════════════════════════
    @SuppressWarnings("unchecked")
    private TableView<ResearchPaper> buildTable(boolean myPapers) {
        TableView<ResearchPaper> t = new TableView<>();
        t.setStyle(
            "-fx-background-color:#1a1f2e;-fx-border-color:#2d3748;" +
            "-fx-border-radius:8px;-fx-background-radius:8px;");
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(t, Priority.ALWAYS);
        t.setPlaceholder(new Label(myPapers ? "No papers yet." : "No results."));
        t.getColumns().addAll(
            col("Title",    "title",      280),
            col("Author",   "author",     150),
            col("Domain",   "domain",     150),
            col("Status",   "status",     110),
            col("Uploaded", "uploadedAt", 130)
        );
        t.setRowFactory(tv -> {
            TableRow<ResearchPaper> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty()) showDetail(row.getItem());
            });
            return row;
        });
        return t;
    }

    private void showDetail(ResearchPaper p) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Paper Details");
        dialog.setHeaderText(p.getTitle());

        VBox content = new VBox(8);
        content.setPadding(new Insets(12));
        content.setMinWidth(500);

        content.getChildren().addAll(
            detailLabel("ID:", String.valueOf(p.getPaperId())),
            detailLabel("Author:", p.getAuthor()),
            detailLabel("Domain:", p.getDomain()),
            detailLabel("Keywords:", p.getKeywords()),
            detailLabel("Status:", String.valueOf(p.getStatus())),
            detailLabel("Link:", p.getLink() != null ? p.getLink() : "—")
        );

        Label abstractHeader = new Label("Abstract:");
        abstractHeader.setFont(Font.font("System", FontWeight.BOLD, 12));
        abstractHeader.setTextFill(Color.web("#333"));
        Label abstractContent = new Label(p.getAbstractText() != null ? p.getAbstractText() : "—");
        abstractContent.setWrapText(true);
        abstractContent.setMaxWidth(480);
        content.getChildren().addAll(abstractHeader, abstractContent);

        // View Paper button
        String link = p.getLink();
        if (link != null && !link.isBlank()) {
            Button viewBtn = new Button("📄 View Paper");
            viewBtn.setStyle("-fx-background-color:#6c9bff;-fx-text-fill:white;-fx-font-weight:bold;" +
                             "-fx-background-radius:6px;-fx-cursor:hand;-fx-padding:8 20;-fx-font-size:13px;");
            Label viewStatus = new Label("");
            viewStatus.setFont(Font.font("System", 11));
            viewBtn.setOnAction(e -> {
                try {
                    java.io.File file = new java.io.File(link);
                    if (file.exists()) {
                        java.awt.Desktop.getDesktop().open(file);
                        viewStatus.setTextFill(Color.web("#68d391"));
                        viewStatus.setText("✓ Opened in default viewer");
                    } else if (link.startsWith("http")) {
                        java.awt.Desktop.getDesktop().browse(new java.net.URI(link));
                        viewStatus.setTextFill(Color.web("#68d391"));
                        viewStatus.setText("✓ Opened in browser");
                    } else {
                        viewStatus.setTextFill(Color.web("#fc8181"));
                        viewStatus.setText("File not found: " + link);
                    }
                } catch (Exception ex) {
                    viewStatus.setTextFill(Color.web("#fc8181"));
                    viewStatus.setText("Error: " + ex.getMessage());
                }
            });
            HBox viewRow = new HBox(10, viewBtn, viewStatus);
            viewRow.setAlignment(Pos.CENTER_LEFT);
            viewRow.setPadding(new Insets(8, 0, 0, 0));
            content.getChildren().add(viewRow);
        }

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.getDialogPane().setPrefWidth(540);
        dialog.showAndWait();
    }

    private HBox detailLabel(String label, String value) {
        Label l = new Label(label);
        l.setFont(Font.font("System", FontWeight.BOLD, 12));
        l.setMinWidth(80);
        Label v = new Label(value != null ? value : "—");
        v.setWrapText(true);
        return new HBox(8, l, v);
    }

    // ══ Style helpers ═════════════════════════════════════════════════
    @SuppressWarnings("unchecked")
    private TableColumn<ResearchPaper, String> col(String label, String prop, double w) {
        TableColumn<ResearchPaper, String> c = new TableColumn<>(label);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(w);
        c.setStyle(
            "-fx-background-color:#0f1117;-fx-text-fill:#8892a4;" +
            "-fx-font-weight:bold;-fx-font-size:12px;");
        return c;
    }

    private String fieldStyle() {
        return "-fx-background-color:#1a1f2e;-fx-text-fill:#e2e8f0;" +
               "-fx-prompt-text-fill:#4a5568;-fx-border-color:#2d3748;" +
               "-fx-border-radius:6px;-fx-background-radius:6px;" +
               "-fx-pref-height:40px;-fx-font-size:13px;-fx-padding:0 12px;";
    }

    private TextField formField(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setStyle(fieldStyle());
        f.setMaxWidth(Double.MAX_VALUE);
        return f;
    }

    private Label lbl(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("System", FontWeight.BOLD, 11));
        l.setTextFill(Color.web("#8892a4"));
        return l;
    }

    private Button primaryBtn(String text) {
        Button b = new Button(text);
        b.setStyle(
            "-fx-background-color:#6c9bff;-fx-text-fill:white;-fx-font-weight:bold;" +
            "-fx-pref-height:40px;-fx-pref-width:150px;" +
            "-fx-background-radius:6px;-fx-cursor:hand;");
        return b;
    }

    private String secondaryBtnStyle() {
        return "-fx-background-color:#2d3748;-fx-text-fill:#a0aec0;-fx-font-weight:bold;" +
               "-fx-background-radius:6px;-fx-pref-height:36px;-fx-pref-width:160px;-fx-cursor:hand;";
    }

    private ToggleButton tabBtn(String text, ToggleGroup g, boolean sel) {
        ToggleButton b = new ToggleButton(text);
        b.setToggleGroup(g);
        b.setSelected(sel);
        String base = "-fx-background-radius:6px;-fx-cursor:hand;-fx-font-size:12px;-fx-padding:8 16;";
        b.setStyle("-fx-background-color:#1a1f2e;-fx-text-fill:#8892a4;" + base);
        b.selectedProperty().addListener((obs, o, n) -> b.setStyle(n
            ? "-fx-background-color:#6c9bff;-fx-text-fill:white;-fx-font-weight:bold;" + base
            : "-fx-background-color:#1a1f2e;-fx-text-fill:#8892a4;" + base));
        return b;
    }

    private void status(Label l, String msg, boolean ok) {
        l.setTextFill(Color.web(ok ? "#68d391" : "#fc8181"));
        l.setText(msg);
    }
}
