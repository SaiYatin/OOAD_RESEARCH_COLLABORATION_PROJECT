package com.research.view.expert;

import com.research.model.Expert;
import com.research.model.Researcher;
import com.research.model.User;
import com.research.repository.ResearchProjectRepository;
import com.research.repository.ResearcherRepository;
import com.research.service.AuthService;
import com.research.service.CollaborationService;
import com.research.service.MentorScoutClient;
import com.research.service.RecommendationService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import java.awt.Desktop;
import java.net.URI;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExpertSearchPane extends VBox {

    private final RecommendationService recommendationService;
    private final ResearcherRepository researcherRepository;
    private final CollaborationService collaborationService;
    private final ResearchProjectRepository projectRepository;
    private final MentorScoutClient mentorScoutClient = new MentorScoutClient();

    public ExpertSearchPane(RecommendationService recommendationService,
            ResearcherRepository researcherRepository,
            CollaborationService collaborationService,
            ResearchProjectRepository projectRepository) {
        this.recommendationService = recommendationService;
        this.researcherRepository = researcherRepository;
        this.collaborationService = collaborationService;
        this.projectRepository = projectRepository;

        setSpacing(16);

        // Strategy selector
        HBox stratRow = new HBox(16);
        stratRow.setAlignment(Pos.CENTER_LEFT);
        Label stratLbl = bold("Algorithm (Strategy pattern):");
        ToggleGroup stratGroup = new ToggleGroup();
        RadioButton kwRb = radio("Keyword Match", stratGroup, true);
        RadioButton aiRb = radio("Semantic Match (Embedding Model)", stratGroup, false);
        RadioButton scoutRb = radio("Mentor Scout (AI Agent) 🤖", stratGroup, false);
        Label stratInfo = new Label("fast local keyword scoring");
        stratInfo.setFont(Font.font("System", FontPosture.ITALIC, 11));
        stratInfo.setTextFill(Color.web("#4a5568"));
        stratGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            if (n == kwRb)
                stratInfo.setText("fast local keyword scoring");
            if (n == aiRb)
                stratInfo.setText("semantic web research using an embedding model");
            if (n == scoutRb)
                stratInfo.setText("live AI agent — powered by PES Mentor Scout on Google Cloud Run");
        });
        stratRow.getChildren().addAll(stratLbl, kwRb, aiRb, scoutRb, stratInfo);

        // Search bar
        HBox searchRow = new HBox(12);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        TextArea queryField = new TextArea();
        queryField
                .setPromptText("e.g. 'machine learning', or ask Mentor Scout: 'Who works on blockchain at RR Campus?'");
        queryField.setPrefRowCount(2);
        queryField.setWrapText(true);
        queryField.setStyle("-fx-control-inner-background: white; -fx-background-color: white; -fx-text-fill: black; " +
                "-fx-prompt-text-fill: #a0aec0; -fx-border-color: #2d3748; " +
                "-fx-border-radius: 6px; -fx-background-radius: 6px; " +
                "-fx-font-size: 14px; -fx-pref-height: 60px;");
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
            if (q.isBlank()) {
                resultLbl.setText("Enter a query first.");
                resultLbl.setTextFill(Color.web("#fc8181"));
                return;
            }

            cards.getChildren().clear();
            findBtn.setDisable(true);

            // --- MENTOR SCOUT MODE ---
            if (scoutRb.isSelected()) {
                System.out.println("\n[ExpertSearchPane] Mode: MENTOR SCOUT AGENT");
                resultLbl.setText("Consulting Mentor Scout Agent...");
                resultLbl.setTextFill(Color.web("#f6ad55"));

                javafx.concurrent.Task<String> scoutTask = new javafx.concurrent.Task<>() {
                    @Override
                    protected String call() throws Exception {
                        return mentorScoutClient.callAgent(q);
                    }
                };

                scoutTask.setOnSucceeded(e -> {
                    findBtn.setDisable(false);
                    String agentResponse = scoutTask.getValue();
                    System.out.println("[ExpertSearchPane] Mentor Scout response received.");
                    resultLbl.setText("Mentor Scout responded ✓");
                    resultLbl.setTextFill(Color.web("#68d391"));
                    renderScoutResponse(cards, agentResponse);
                });

                scoutTask.setOnFailed(e -> {
                    findBtn.setDisable(false);
                    Throwable ex = scoutTask.getException();
                    System.err.println("[ExpertSearchPane] Mentor Scout call failed: "
                            + (ex != null ? ex.getMessage() : "unknown"));
                    resultLbl.setText("Mentor Scout error: " + (ex != null ? ex.getMessage() : "Unknown error"));
                    resultLbl.setTextFill(Color.web("#ff6b6b"));
                });

                Thread th = new Thread(scoutTask);
                th.setDaemon(true);
                th.start();
                return;
            }

            // --- KEYWORD / SEMANTIC MODE ---
            if (kwRb.isSelected()) {
                System.out.println("[ExpertSearchPane] Mode: KEYWORD MATCH");
                resultLbl.setText("Searching...");
            } else {
                System.out.println("[ExpertSearchPane] Mode: SEMANTIC EMBEDDING");
                resultLbl.setText("Searching... (AI Match may take a moment)");
            }
            resultLbl.setTextFill(Color.web("#6c9bff"));

            String mode = kwRb.isSelected() ? "keyword" : "ai";
            User currentUser = AuthService.getCurrentUser();

            javafx.concurrent.Task<java.util.AbstractMap.SimpleEntry<List<Expert>, List<Researcher>>> searchTask = new javafx.concurrent.Task<>() {
                @Override
                protected java.util.AbstractMap.SimpleEntry<List<Expert>, List<Researcher>> call() throws Exception {
                    List<Expert> experts = recommendationService.recommendWithStrategy(q, mode);
                    List<Researcher> researchers = researcherRepository.findByKeyword(q);
                    return new java.util.AbstractMap.SimpleEntry<>(experts, researchers);
                }
            };

            searchTask.setOnSucceeded(e -> {
                findBtn.setDisable(false);
                List<Expert> experts = searchTask.getValue().getKey();
                List<Researcher> researchers = searchTask.getValue().getValue();

                int totalResults = experts.size() + researchers.size();
                if (totalResults == 0) {
                    Label none = new Label("No matching experts or researchers for: \"" + q + "\"");
                    none.setTextFill(Color.web("#8892a4"));
                    none.setFont(Font.font("System", 14));
                    cards.getChildren().add(none);
                    resultLbl.setText("0 results");
                    resultLbl.setTextFill(Color.web("#fc8181"));
                    return;
                }

                resultLbl.setTextFill(Color.web("#68d391"));
                resultLbl.setText(experts.size() + " expert(s) + " + researchers.size() + " researcher(s)");

                if (!experts.isEmpty()) {
                    Label expHeader = new Label("📚 Faculty");
                    expHeader.setFont(Font.font("System", FontWeight.BOLD, 14));
                    expHeader.setTextFill(Color.web("#6c9bff"));
                    expHeader.setPadding(new Insets(4, 0, 4, 0));
                    cards.getChildren().add(expHeader);
                    for (int i = 0; i < experts.size(); i++) {
                        cards.getChildren().add(new ExpertCardUI(experts.get(i), i + 1, q));
                    }
                }

                if (!researchers.isEmpty()) {
                    Label resHeader = new Label("👤 Registered Researchers");
                    resHeader.setFont(Font.font("System", FontWeight.BOLD, 14));
                    resHeader.setTextFill(Color.web("#68d391"));
                    resHeader.setPadding(new Insets(8, 0, 4, 0));
                    cards.getChildren().add(resHeader);
                    for (Researcher r : researchers) {
                        if (currentUser != null && r.getUserId().equals(currentUser.getUserId()))
                            continue;
                        cards.getChildren().add(new ResearcherCardUI(r, collaborationService, projectRepository));
                    }
                }
            });

            searchTask.setOnFailed(e -> {
                findBtn.setDisable(false);
                Throwable ex = searchTask.getException();
                resultLbl.setText("Search failed: " + (ex != null ? ex.getMessage() : "Unknown error"));
                resultLbl.setTextFill(Color.web("#ff6b6b"));
            });

            Thread th = new Thread(searchTask);
            th.setDaemon(true);
            th.start();
        };

        findBtn.setOnAction(e -> doFind.run());
        queryField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                if (e.isShiftDown()) {
                    queryField.appendText("\n");
                } else {
                    e.consume();
                    doFind.run();
                }
            }
        });

        getChildren().addAll(stratRow, searchRow, resultLbl, scroll);
    }

    /**
     * Smart Markdown + HTML renderer for Mentor Scout Agent responses.
     * All text is fully selectable. Images are rendered. Links are clickable.
     */
    private void renderScoutResponse(VBox cards, String response) {
        if (response == null || response.isBlank()) {
            Label empty = new Label("Mentor Scout returned no content.");
            empty.setTextFill(Color.web("#8892a4"));
            cards.getChildren().add(empty);
            return;
        }

        VBox responseBox = new VBox(6);
        responseBox.setPadding(new Insets(18));
        responseBox.setStyle("-fx-background-color:#1a1f2e;-fx-background-radius:10px;-fx-border-color:#2d3748;-fx-border-radius:10px;");

        // Header with Copy All button
        HBox cardHeader = new HBox();
        cardHeader.setAlignment(Pos.CENTER_LEFT);
        Label agentBadge = new Label("\uD83E\uDD16  Mentor Scout Response");
        agentBadge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        agentBadge.setTextFill(Color.web("#f6ad55"));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button copyBtn = new Button("\uD83D\uDCCB Copy All");
        copyBtn.setStyle("-fx-background-color: #2d3748; -fx-text-fill: #a0aec0; -fx-font-size: 10px; -fx-background-radius: 4px; -fx-cursor: hand;");
        copyBtn.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(response);
            clipboard.setContent(content);
            copyBtn.setText("Copied! \u2713");
            new Thread(() -> { try { Thread.sleep(2000); javafx.application.Platform.runLater(() -> copyBtn.setText("\uD83D\uDCCB Copy All")); } catch (Exception ignored) {} }).start();
        });
        cardHeader.getChildren().addAll(agentBadge, spacer, copyBtn);
        responseBox.getChildren().addAll(cardHeader, separatorLine());

        // Patterns compiled once
        Pattern imgPattern   = Pattern.compile("<img[^>]+src=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
        Pattern mdLinkPattern = Pattern.compile("\\[([^\\]]+)\\]\\((https?://[^)]+)\\)"); // [text](url)
        Pattern urlPattern   = Pattern.compile("(https?://[^\\s,;\"'<>()\\]]+)");

        String[] lines = response.split("\n");
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;

            // 1. <img src="..."> HTML tag → render image
            Matcher imgMatcher = imgPattern.matcher(line);
            if (imgMatcher.find()) {
                String imgUrl = imgMatcher.group(1);
                System.out.println("[ScoutRenderer] Detected <img> tag. URL: " + imgUrl);
                try {
                    ImageView iv = new ImageView(new Image(imgUrl, 220, 280, true, true));
                    iv.setStyle("-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.6),12,0,0,5);");
                    VBox imgBox = new VBox(iv);
                    imgBox.setPadding(new Insets(8, 0, 8, 0));
                    responseBox.getChildren().add(imgBox);
                } catch (Exception ex) {
                    System.err.println("[ScoutRenderer] Image load failed: " + ex.getMessage());
                }
                continue;
            }

            // 2. Markdown link [Label Text](https://url) → Hyperlink
            Matcher mdMatcher = mdLinkPattern.matcher(line);
            if (mdMatcher.find()) {
                String prefix = line.substring(0, mdMatcher.start()).trim();
                String label  = mdMatcher.group(1);
                String url    = mdMatcher.group(2).trim();
                HBox linkRow  = new HBox(6);
                linkRow.setAlignment(Pos.CENTER_LEFT);
                if (!prefix.isEmpty()) {
                    TextField prefixField = selectableTextField(prefix + " ", 12, FontWeight.NORMAL, "#c9d1db");
                    linkRow.getChildren().add(prefixField);
                }
                Hyperlink link = new Hyperlink(label + "  ↗");
                link.setFont(Font.font("Segoe UI", 12));
                link.setTextFill(Color.web("#6c9bff"));
                link.setStyle("-fx-padding:0;");
                final String finalUrl = url;
                link.setOnAction(e -> openBrowser(finalUrl));
                ContextMenu cm = new ContextMenu();
                MenuItem copyUrl = new MenuItem("Copy URL");
                copyUrl.setOnAction(ev -> { ClipboardContent cc = new ClipboardContent(); cc.putString(finalUrl); Clipboard.getSystemClipboard().setContent(cc); });
                cm.getItems().add(copyUrl);
                link.setContextMenu(cm);
                linkRow.getChildren().add(link);
                responseBox.getChildren().add(linkRow);
                continue;
            }

            // 3. #### / ### / ## headings → selectable TextField
            if (line.startsWith("####")) {
                responseBox.getChildren().add(selectableTextField(stripMarkdown(line.replaceFirst("^####\\s*", "")), 13, FontWeight.BOLD, "#68d391"));
                continue;
            }
            if (line.startsWith("###")) {
                responseBox.getChildren().add(selectableTextField(stripMarkdown(line.replaceFirst("^###\\s*", "")), 15, FontWeight.BOLD, "#f6ad55"));
                continue;
            }
            if (line.startsWith("##")) {
                responseBox.getChildren().add(selectableTextField(stripMarkdown(line.replaceFirst("^##\\s*", "")), 17, FontWeight.BOLD, "#6c9bff"));
                continue;
            }

            // 4. Strip bullet prefix
            String displayLine = line;
            boolean isBullet = false;
            if (line.startsWith("- ") || line.startsWith("* ")) {
                displayLine = "  \u2022  " + line.substring(2).trim();
                isBullet = true;
            }

            // 5. Standalone URL line → Hyperlink only
            Matcher urlOnlyMatcher = urlPattern.matcher(displayLine);
            if (displayLine.trim().matches("^https?://\\S+$") && urlOnlyMatcher.find()) {
                String url = urlOnlyMatcher.group(1);
                Hyperlink link = new Hyperlink(url);
                link.setFont(Font.font("Segoe UI", 12));
                link.setTextFill(Color.web("#6c9bff"));
                link.setStyle("-fx-padding:0;");
                link.setOnAction(e -> openBrowser(url));
                responseBox.getChildren().add(link);
                continue;
            }

            // 6. Everything else → selectable TextField (supports Ctrl+A, Ctrl+C, mouse drag)
            String cleaned = stripMarkdown(displayLine);
            TextField tf = selectableTextField(cleaned, 13, FontWeight.NORMAL, "#c9d1db");
            responseBox.getChildren().add(tf);
        }

        cards.getChildren().add(responseBox);
    }

    private TextField selectableTextField(String text, int size, FontWeight weight, String hexColor) {
        TextField tf = new TextField(text);
        tf.setEditable(false);
        tf.setFont(Font.font("Segoe UI", weight, size));
        tf.setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-text-fill: " + hexColor + "; " +
                    "-fx-background-insets: 0; -fx-background-radius: 0; -fx-border-width: 0;");
        tf.focusedProperty().addListener((obs, oldV, newV) -> tf.setStyle(tf.getStyle() + "-fx-faint-focus-color: transparent; -fx-focus-color: transparent;"));
        return tf;
    }

    private void openBrowser(String url) {
        System.out.println("[ScoutRenderer] Opening URL in browser: " + url);
        try {
            new ProcessBuilder("cmd", "/c", "start", url.replace("&", "^&")).start();
        } catch (Exception ex) {
            System.err.println("[ScoutRenderer] Could not open browser: " + ex.getMessage());
            try { java.awt.Desktop.getDesktop().browse(new URI(url)); } catch (Exception ignored) {}
        }
    }

    private String stripMarkdown(String text) {
        return text.replaceAll("\\*\\*(.+?)\\*\\*", "$1").replaceAll("[#*`]+", "").trim();
    }

    private javafx.scene.control.Separator separatorLine() {
        javafx.scene.control.Separator sep = new javafx.scene.control.Separator();
        sep.setStyle("-fx-background-color:#2d3748;");
        sep.setPadding(new Insets(4, 0, 4, 0));
        return sep;
    }

    private Label bold(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("System", FontWeight.BOLD, 12));
        l.setTextFill(Color.web("#6c9bff"));
        return l;
    }

    private Button primaryBtn(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:#6c9bff;-fx-text-fill:white;-fx-font-weight:bold;" +
                "-fx-pref-height:40px;-fx-pref-width:140px;-fx-background-radius:6px;-fx-cursor:hand;");
        return b;
    }

    private RadioButton radio(String text, ToggleGroup g, boolean sel) {
        RadioButton r = new RadioButton(text);
        r.setToggleGroup(g);
        r.setSelected(sel);
        r.setStyle("-fx-text-fill:#a0aec0;-fx-font-size:12px;-fx-cursor:hand;");
        return r;
    }
}
