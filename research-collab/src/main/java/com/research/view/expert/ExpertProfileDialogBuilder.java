package com.research.view.expert;

import com.research.model.Expert;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Creational Pattern: Builder
 * Constructs the massive 14-column Expert Profile details dialog dynamically.
 * Adheres to SRP by handling all complex UI generation outside the main view.
 */
public class ExpertProfileDialogBuilder {

    private final Expert expert;
    private final Dialog<Void> dialog;
    private final VBox root;

    public ExpertProfileDialogBuilder(Expert expert) {
        this.expert = expert;
        this.dialog = new Dialog<>();
        this.root = new VBox(20);

        initializeDialog();
    }

    private void initializeDialog() {
        dialog.setTitle("Expert Profile View");
        dialog.setHeaderText(null);
        dialog.getDialogPane().setStyle("-fx-background-color:#0f1117;");

        ButtonType closeType = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeType);

        root.setPadding(new Insets(20));
        root.setPrefWidth(800);
        root.setPrefHeight(650);
    }

    public Dialog<Void> build() {
        HBox topSection = buildHeader();
        ScrollPane scrollSection = buildScrollableContext();

        root.getChildren().addAll(topSection, scrollSection);
        dialog.getDialogPane().setContent(root);
        return dialog;
    }

    private HBox buildHeader() {
        HBox topSection = new HBox(25);

        // Image Box
        VBox imageBox = new VBox();
        imageBox.setAlignment(Pos.CENTER);
        imageBox.setStyle(
                "-fx-background-color:#1a1f2e;-fx-background-radius:10px;-fx-border-color:#2d3748;-fx-border-radius:10px;-fx-padding:10;");
        imageBox.setPrefWidth(160);
        imageBox.setPrefHeight(200);

        String url = expert.getProfileUrl();
        if (url != null && !url.isBlank() && !url.trim().equalsIgnoreCase("nan")
                && !url.trim().equalsIgnoreCase("not listed")) {
            try {
                ImageView iv = new ImageView(new Image(url, 140, 180, true, true));
                imageBox.getChildren().add(iv);
            } catch (Exception e) {
                imageBox.getChildren().add(createAvatarFallback());
            }
        } else {
            imageBox.getChildren().add(createAvatarFallback());
        }

        // Profile Identity Info
        VBox headerInfo = new VBox(6);
        headerInfo.setAlignment(Pos.CENTER_LEFT);

        TextField nameField = createCopyableField(expert.getName(), 26, true);
        nameField.setPrefWidth(550);

        TextField desigField = createCopyableField(expert.getDesignation() + " — " + (expert.getDepartment() != null ? expert.getDepartment() : "General Department"), 13, true, "#e2e8f0");
        
        TextField campusField = createCopyableField("Campus: " + (expert.getCampus() != null ? expert.getCampus() : "PES University"), 11, true, "#8892a4");
        
        VBox contactBox = new VBox(2);
        contactBox.setPadding(new Insets(10, 0, 0, 0));
        TextField emailField = createCopyableField("Email: " + expert.getEmail(), 14, false, "#6c9bff");
        emailField.setPrefWidth(400);
        TextField phoneField = createCopyableField("Phone: " + (expert.getPhone() != null ? expert.getPhone() : "Not listed"), 14, false, "#e2e8f0");

        contactBox.getChildren().addAll(emailField, phoneField);
        headerInfo.getChildren().addAll(nameField, desigField, campusField, contactBox);
        topSection.getChildren().addAll(imageBox, headerInfo);

        return topSection;
    }

    private ScrollPane buildScrollableContext() {
        VBox bottomSection = new VBox(18);
        bottomSection.setPadding(new Insets(10, 20, 10, 0));

        addSectionIfPresent(bottomSection, "About & Responsibilities", expert.getAbout());
        addSectionIfPresent(bottomSection, "Education", expert.getEducation());
        addSectionIfPresent(bottomSection, "Teaching Details", expert.getTeaching());
        addSectionIfPresent(bottomSection, "Research Interests", expert.getResearchAreas());
        addSectionIfPresent(bottomSection, "Publications (Journals)", expert.getPublicationsJournals());
        addSectionIfPresent(bottomSection, "Publications (Conferences)", expert.getPublicationsConferences());

        ScrollPane scrollPane = new ScrollPane(bottomSection);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background:transparent;-fx-background-color:transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        return scrollPane;
    }

    // --- Private Detail Helpers ---

    private Label createAvatarFallback() {
        Label noImg = new Label("👤");
        noImg.setFont(Font.font(60));
        noImg.setTextFill(Color.web("#4a5568"));
        return noImg;
    }

    private TextField createCopyableField(String text, int fontSize, boolean isBold) {
        return createCopyableField(text, fontSize, isBold, "#e2e8f0");
    }

    private TextField createCopyableField(String text, int fontSize, boolean isBold, String hexColor) {
        TextField t = new TextField(text != null && !text.equalsIgnoreCase("nan") ? text : "Not Listed");
        t.setEditable(false);
        String weight = isBold ? "bold" : "normal";
        t.setStyle("-fx-background-color: transparent; -fx-text-fill: " + hexColor + "; -fx-padding: 0; " +
                   "-fx-font-family: 'Segoe UI'; -fx-font-size: " + fontSize + "px; -fx-font-weight: " + weight + "; " +
                   "-fx-background-insets: 0; -fx-background-radius: 0; -fx-border-width: 0;");
        t.focusedProperty().addListener((obs, oldV, newV) -> t.setStyle(t.getStyle() + "-fx-faint-focus-color: transparent; -fx-focus-color: transparent;"));
        return t;
    }

    private void addSectionIfPresent(VBox container, String title, String content) {
        if (content == null || content.isBlank() || content.trim().equalsIgnoreCase("nan")
                || content.trim().equalsIgnoreCase("not listed"))
            return;
        Label titleLbl = bold(title);
        titleLbl.setTextFill(Color.web("#68d391"));

        TextArea ta = new TextArea(content);
        ta.setEditable(false);
        ta.setWrapText(true);
        ta.setStyle(
                "-fx-background-color:#1a1f2e;-fx-control-inner-background:#1a1f2e;-fx-text-fill:#8892a4;-fx-border-color:#2d3748;-fx-border-radius:6px; -fx-padding: 5;");

        ta.setPrefRowCount(Math.min(15, countLines(content)));

        container.getChildren().addAll(titleLbl, ta);
    }

    private int countLines(String str) {
        if (str == null || str.isEmpty()) {
            return 1;
        }
        int lines = 1;
        int length = str.length();
        for (int i = 0; i < length; i++) {
            if (str.charAt(i) == '\n')
                lines++;
        }
        return lines + (length / 100); // Buffer for wrapped texts
    }

    private Label lbl(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("System", FontWeight.BOLD, 11));
        l.setTextFill(Color.web("#8892a4"));
        return l;
    }

    private Label bold(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("System", FontWeight.BOLD, 12));
        l.setTextFill(Color.web("#6c9bff"));
        return l;
    }
}
