package com.research.view.paper;

import com.research.model.ResearchPaper;
import com.research.pattern.BasicPaperSearch;
import com.research.pattern.DomainFilterDecorator;
import com.research.pattern.PublishedOnlyDecorator;
import com.research.repository.ResearchPaperRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import main.java.com.research.service.PaperService;

import org.springframework.stereotype.Component;
import org.w3c.dom.Text;

import java.lang.classfile.Label;
import java.util.List;

import javax.swing.table.TableColumn;
import javax.swing.text.TableView;
import javax.swing.text.TableView.TableRow;

/**
 * PaperSearchView - Member 1's primary UI.
 * Uses the Decorator pattern to apply optional filters on paper search.
 * MVC: View layer — delegates search to pattern classes.
 */
@Component
public class PaperSearchView {

    private final ResearchPaperRepository paperRepository;
    private final BasicPaperSearch basicSearch;
    private final DomainFilterDecorator domainDecorator;
    private final PublishedOnlyDecorator publishedDecorator;

    public PaperSearchView(ResearchPaperRepository paperRepository,
                       BasicPaperSearch basicSearch,
                       DomainFilterDecorator domainDecorator,
                       PublishedOnlyDecorator publishedDecorator,
                       PaperService paperService) {
        this.paperRepository = paperRepository;
        this.basicSearch = basicSearch;
        this.domainDecorator = domainDecorator;
        this.publishedDecorator = publishedDecorator;
    }

    public VBox buildPanel() {
        VBox panel = new VBox(20);
        panel.setStyle("-fx-background-color: #0f1117;");

        // ── Header ────────────────────────────────────────────────────
        Text title = new Text("Research Paper Search");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 26));
        title.setFill(Color.web("#e2e8f0"));

        Text subtitle = new Text("Search papers by title, abstract or keywords");
        subtitle.setFont(Font.font("System", 13));
        subtitle.setFill(Color.web("#8892a4"));

        // ── Search Bar ────────────────────────────────────────────────
        HBox searchRow = new HBox(12);
        searchRow.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setPromptText("Search: 'machine learning', 'NLP', 'composite materials'...");
        searchField.setStyle(
            "-fx-background-color: #1a1f2e; -fx-text-fill: #e2e8f0; " +
            "-fx-prompt-text-fill: #4a5568; -fx-border-color: #2d3748; " +
            "-fx-border-radius: 6px; -fx-background-radius: 6px; " +
            "-fx-pref-height: 42px; -fx-font-size: 13px; -fx-padding: 0 12px;");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button searchBtn = primaryButton("Search");

        searchRow.getChildren().addAll(searchField, searchBtn);

        // ── Filters (Decorator pattern controls) ──────────────────────
        HBox filterRow = new HBox(16);
        filterRow.setAlignment(Pos.CENTER_LEFT);

        Label filterLabel = new Label("Filters:");
        filterLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        filterLabel.setTextFill(Color.web("#8892a4"));

        CheckBox publishedOnly = new CheckBox("Published Only");
        styleCheckBox(publishedOnly);

        TextField domainField = new TextField();
        domainField.setPromptText("Domain filter (e.g. AI)");
        domainField.setStyle(
            "-fx-background-color: #1a1f2e; -fx-text-fill: #e2e8f0; " +
            "-fx-prompt-text-fill: #4a5568; -fx-border-color: #2d3748; " +
            "-fx-border-radius: 6px; -fx-background-radius: 6px; " +
            "-fx-pref-height: 34px; -fx-pref-width: 200px; -fx-font-size: 12px;");

        filterRow.getChildren().addAll(filterLabel, publishedOnly, domainField);

        // ── Results Table ─────────────────────────────────────────────
        TableView<ResearchPaper> table = buildPaperTable();
        ObservableList<ResearchPaper> data = FXCollections.observableArrayList();
        table.setItems(data);

        Label resultCount = new Label("0 results");
        resultCount.setFont(Font.font("System", 12));
        resultCount.setTextFill(Color.web("#4a5568"));

        // ── Search action: applies Decorator chain ─────────────────────
        Runnable doSearch = () -> {
            String query = searchField.getText().trim();
            List<ResearchPaper> allPapers = paperRepository.findAll();

            // Build decorator chain based on selected filters
            // Pattern: BasicSearch → [PublishedOnly?] → [DomainFilter?]
            var searchChain = basicSearch;
            List<ResearchPaper> results;

            if (publishedOnly.isSelected() && !domainField.getText().isBlank()) {
                // Both filters applied
                results = domainDecorator
                    .withDomain(domainField.getText().trim())
                    .search(query,
                        publishedDecorator.search(query, allPapers));
            } else if (publishedOnly.isSelected()) {
                results = publishedDecorator.search(query, allPapers);
            } else if (!domainField.getText().isBlank()) {
                results = domainDecorator
                    .withDomain(domainField.getText().trim())
                    .search(query, allPapers);
            } else {
                results = searchChain.search(query, allPapers);
            }

            data.setAll(results);
            resultCount.setText(results.size() + " result(s) found");
        };

        searchBtn.setOnAction(e -> doSearch.run());
        searchField.setOnAction(e -> doSearch.run());

        // Load all papers on open
        data.setAll(paperRepository.findAll());
        resultCount.setText(data.size() + " papers available");

        panel.getChildren().addAll(title, subtitle, searchRow, filterRow,
                                    resultCount, table);
        return panel;
    }

    @SuppressWarnings("unchecked")
    private TableView<ResearchPaper> buildPaperTable() {
        TableView<ResearchPaper> table = new TableView<>();
        table.setStyle(
            "-fx-background-color: #1a1f2e; -fx-border-color: #2d3748; " +
            "-fx-border-radius: 8px; -fx-background-radius: 8px;");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.setPlaceholder(new Label("No papers found. Try a different search."));

        TableColumn<ResearchPaper, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setPrefWidth(300);

        TableColumn<ResearchPaper, String> authorCol = new TableColumn<>("Author");
        authorCol.setCellValueFactory(new PropertyValueFactory<>("author"));
        authorCol.setPrefWidth(160);

        TableColumn<ResearchPaper, String> domainCol = new TableColumn<>("Domain");
        domainCol.setCellValueFactory(new PropertyValueFactory<>("domain"));
        domainCol.setPrefWidth(160);

        TableColumn<ResearchPaper, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(110);

        TableColumn<ResearchPaper, String> dateCol = new TableColumn<>("Uploaded");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("uploadedAt"));
        dateCol.setPrefWidth(130);

        // Style column headers
        for (TableColumn<?, ?> col : List.of(titleCol, authorCol, domainCol, statusCol, dateCol)) {
            col.setStyle("-fx-background-color: #0f1117; -fx-text-fill: #8892a4; " +
                         "-fx-font-weight: bold; -fx-font-size: 12px;");
        }

        table.getColumns().addAll(titleCol, authorCol, domainCol, statusCol, dateCol);

        // Row double-click → show abstract popup
        table.setRowFactory(tv -> {
            TableRow<ResearchPaper> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    showAbstractPopup(row.getItem());
                }
            });
            return row;
        });

        return table;
    }

    private void showAbstractPopup(ResearchPaper paper) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Paper Abstract");
        alert.setHeaderText(paper.getTitle());
        alert.setContentText(
            "Author: " + paper.getAuthor() + "\n\n" +
            "Domain: " + paper.getDomain() + "\n\n" +
            "Abstract:\n" + (paper.getAbstractText() != null
                ? paper.getAbstractText() : "No abstract available.") + "\n\n" +
            "Status: " + paper.getStatus()
        );
        alert.showAndWait();
    }

    private Button primaryButton(String text) {
        Button btn = new Button(text);
        btn.setStyle(
            "-fx-background-color: #6c9bff; -fx-text-fill: white; " +
            "-fx-font-weight: bold; -fx-pref-height: 42px; -fx-pref-width: 100px; " +
            "-fx-background-radius: 6px; -fx-cursor: hand;");
        return btn;
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #8892a4; -fx-font-size: 12px;");
    }
}
