package com.research.view.dashboard;

import com.research.model.User;
import com.research.controller.AuthController;
import com.research.view.admin.AdminView;
import com.research.view.expert.ExpertSearchView;
import com.research.view.paper.PaperSearchView;
import com.research.view.collab.CollaborationView;
import com.research.view.research.MyResearchesView;
import com.research.view.reviewer.ReviewerDashboardView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

/**
 * DashboardView — Main application shell.
 *
 * @author Member 4
 * @usecase User Registration, Login, Session Management & Role-Based Navigation
 *
 * Supports three modes:
 *   - Visitor (user=null): limited browsing, login/signup in top bar
 *   - Researcher: full research features
 *   - Reviewer: review-focused dashboard
 *   - Admin: admin panel added
 *
 * Design Pattern demonstrated: Factory Method (user creation via AuthController)
 * Design Principle: LSP (all User subclasses substitutable)
 *
 * MVC Role: View — delegates authentication to AuthController
 */
@Component
public class DashboardView {

    private final ExpertSearchView expertSearchView;
    private final PaperSearchView paperSearchView;
    private final CollaborationView collaborationView;
    private final MyResearchesView myResearchesView;
    private final ReviewerDashboardView reviewerDashboardView;
    private final AdminView adminView;
    private final AuthController authController;

    private StackPane contentArea;
    private Stage currentStage;

    public DashboardView(ExpertSearchView expertSearchView,
                         PaperSearchView paperSearchView,
                         CollaborationView collaborationView,
                         MyResearchesView myResearchesView,
                         ReviewerDashboardView reviewerDashboardView,
                         AdminView adminView,
                         AuthController authController) {
        this.expertSearchView = expertSearchView;
        this.paperSearchView = paperSearchView;
        this.collaborationView = collaborationView;
        this.myResearchesView = myResearchesView;
        this.reviewerDashboardView = reviewerDashboardView;
        this.adminView = adminView;
        this.authController = authController;
    }

    /**
     * @param user null = Visitor mode (no login)
     */
    public void show(Stage stage, User user) {
        this.currentStage = stage;
        boolean isVisitor = (user == null);

        stage.setTitle(isVisitor ? "ResearchConnect — Browse as Visitor" : "ResearchConnect — " + user.getName());
        stage.setWidth(1200);
        stage.setHeight(750);
        stage.setResizable(true);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0f1117;");

        // ── Top Bar ──────────────────────────────────────────────────
        HBox topBar = new HBox();
        topBar.setPadding(new Insets(14, 24, 14, 24));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: #1a1f2e; -fx-border-color: #2d3748; " +
                        "-fx-border-width: 0 0 1 0;");

        Text logo = new Text("ResearchConnect");
        logo.setFont(Font.font("Georgia", FontWeight.BOLD, 20));
        logo.setFill(Color.web("#6c9bff"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topBar.getChildren().addAll(logo, spacer);

        if (isVisitor) {
            // Visitor: Login / Sign Up buttons
            Label visitorBadge = new Label("👁️  Browsing as Visitor");
            visitorBadge.setStyle("-fx-background-color: #2d3748; -fx-text-fill: #a0aec0; " +
                                  "-fx-padding: 6 14; -fx-background-radius: 20px; -fx-font-size: 12px;");

            Button loginBtn = new Button("Login");
            loginBtn.setStyle("-fx-background-color: #6c9bff; -fx-text-fill: white; " +
                              "-fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; " +
                              "-fx-background-radius: 6px; -fx-padding: 6 18;");
            loginBtn.setOnAction(e -> showLoginDialog(stage, false));

            Button signupBtn = new Button("Sign Up");
            signupBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #6c9bff; " +
                               "-fx-cursor: hand; -fx-font-size: 12px; -fx-border-color: #6c9bff; " +
                               "-fx-border-radius: 6px; -fx-padding: 6 18;");
            signupBtn.setOnAction(e -> showLoginDialog(stage, true));

            topBar.getChildren().addAll(visitorBadge,
                    new Region() {{ setMinWidth(12); }}, loginBtn,
                    new Region() {{ setMinWidth(6); }}, signupBtn);
        } else {
            // Logged-in user
            Label userBadge = new Label(user.getName() + "  [" + user.getRole() + "]");
            userBadge.setStyle("-fx-background-color: #2d3748; -fx-text-fill: #a0aec0; " +
                               "-fx-padding: 6 14; -fx-background-radius: 20px; -fx-font-size: 12px;");

            Button logoutBtn = new Button("Logout");
            logoutBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #fc8181; " +
                               "-fx-cursor: hand; -fx-font-size: 12px; -fx-border-color: #fc8181; " +
                               "-fx-border-radius: 4px; -fx-padding: 4 10;");
            logoutBtn.setOnAction(e -> {
                authController.logout();
                show(stage, null); // Back to visitor mode
            });

            topBar.getChildren().addAll(userBadge,
                    new Region() {{ setMinWidth(12); }}, logoutBtn);
        }

        // ── Sidebar ──────────────────────────────────────────────────
        VBox sidebar = new VBox(4);
        sidebar.setPrefWidth(220);
        sidebar.setPadding(new Insets(24, 12, 24, 12));
        sidebar.setStyle("-fx-background-color: #1a1f2e;");

        Label sectionLabel = new Label("NAVIGATION");
        sectionLabel.setFont(Font.font("System", FontWeight.BOLD, 10));
        sectionLabel.setTextFill(Color.web("#4a5568"));
        sectionLabel.setPadding(new Insets(0, 0, 8, 8));
        sidebar.getChildren().add(sectionLabel);

        // ── Content Area ─────────────────────────────────────────────
        contentArea = new StackPane();
        contentArea.setStyle("-fx-background-color: #0f1117;");
        contentArea.setPadding(new Insets(24));

        // Build role-specific navigation
        if (isVisitor) {
            buildVisitorNav(sidebar, stage);
        } else if (user.getRole() == User.UserRole.REVIEWER) {
            buildReviewerNav(sidebar, user);
        } else {
            buildResearcherNav(sidebar, user);
        }

        // Admin section
        if (!isVisitor && user.getRole() == User.UserRole.ADMIN) {
            Label adminLabel = new Label("ADMIN");
            adminLabel.setFont(Font.font("System", FontWeight.BOLD, 10));
            adminLabel.setTextFill(Color.web("#4a5568"));
            adminLabel.setPadding(new Insets(16, 0, 8, 8));
            Button adminBtn = navButton("⚙️  Admin Panel", false);
            adminBtn.setOnAction(e -> {
                swapContent(adminView.buildPanel());
                adminBtn.setStyle("-fx-background-color: #fc818122; -fx-text-fill: #fc8181; " +
                    "-fx-background-radius: 6px; -fx-cursor: hand; -fx-alignment: CENTER-LEFT; " +
                    "-fx-font-weight: bold; -fx-border-color: transparent transparent transparent #fc8181; " +
                    "-fx-border-width: 0 0 0 3;");
            });
            sidebar.getChildren().addAll(adminLabel, adminBtn);
        }

        // Version info
        Region sidebarSpacer = new Region();
        VBox.setVgrow(sidebarSpacer, Priority.ALWAYS);
        Label versionLabel = new Label("v2.0.0 · Research Collab");
        versionLabel.setFont(Font.font("System", 10));
        versionLabel.setTextFill(Color.web("#2d3748"));
        versionLabel.setPadding(new Insets(0, 0, 0, 8));
        sidebar.getChildren().addAll(sidebarSpacer, versionLabel);

        root.setTop(topBar);
        root.setLeft(sidebar);
        root.setCenter(contentArea);

        stage.setScene(new Scene(root));
        stage.show();
    }

    // ═══════════════════════════════════════════════════════════
    // Visitor Navigation (limited)
    // ═══════════════════════════════════════════════════════════

    private void buildVisitorNav(VBox sidebar, Stage stage) {
        Button paperBtn  = navButton("📄  Paper Search", true);
        Button expertBtn = navButton("🔬  Find Experts", false);
        Button collabBtn = navButton("🤝  Active Collaborations", false);

        Button[] navBtns = {paperBtn, expertBtn, collabBtn};

        // Default: paper search (visitor mode = no upload/my papers tabs)
        contentArea.getChildren().add(paperSearchView.buildPanel(true));

        paperBtn.setOnAction(e -> {
            resetNavButtons(navBtns); selectNavButton(paperBtn);
            swapContent(paperSearchView.buildPanel(true));
        });
        expertBtn.setOnAction(e -> {
            resetNavButtons(navBtns); selectNavButton(expertBtn);
            swapContent(expertSearchView.buildPanel(true, stage));
        });
        collabBtn.setOnAction(e -> {
            resetNavButtons(navBtns); selectNavButton(collabBtn);
            swapContent(collaborationView.buildVisitorPanel(stage));
        });

        for (Button btn : navBtns) sidebar.getChildren().add(btn);
    }

    // ═══════════════════════════════════════════════════════════
    // Researcher Navigation
    // ═══════════════════════════════════════════════════════════

    private void buildResearcherNav(VBox sidebar, User user) {
        Button paperBtn    = navButton("📄  Paper Search", true);
        Button expertBtn   = navButton("🔬  Find Experts", false);
        Button collabBtn   = navButton("🤝  Collaborations", false);
        Button researchBtn = navButton("🔬  My Researches", false);
        Button profileBtn  = navButton("👤  My Profile", false);

        Button[] navBtns = {paperBtn, expertBtn, collabBtn, researchBtn, profileBtn};

        contentArea.getChildren().add(paperSearchView.buildPanel(false));

        paperBtn.setOnAction(e -> {
            resetNavButtons(navBtns); selectNavButton(paperBtn);
            swapContent(paperSearchView.buildPanel(false));
        });
        expertBtn.setOnAction(e -> {
            resetNavButtons(navBtns); selectNavButton(expertBtn);
            swapContent(expertSearchView.buildPanel());
        });
        collabBtn.setOnAction(e -> {
            resetNavButtons(navBtns); selectNavButton(collabBtn);
            swapContent(collaborationView.buildPanel(user));
        });
        researchBtn.setOnAction(e -> {
            resetNavButtons(navBtns); selectNavButton(researchBtn);
            swapContent(myResearchesView.buildPanel());
        });
        profileBtn.setOnAction(e -> {
            resetNavButtons(navBtns); selectNavButton(profileBtn);
            swapContent(buildProfilePanel(user));
        });

        for (Button btn : navBtns) sidebar.getChildren().add(btn);
    }

    // ═══════════════════════════════════════════════════════════
    // Reviewer Navigation
    // ═══════════════════════════════════════════════════════════

    private void buildReviewerNav(VBox sidebar, User user) {
        Button reviewBtn  = navButton("📋  Review Papers", true);
        Button paperBtn   = navButton("📄  Paper Search", false);
        Button profileBtn = navButton("👤  My Profile", false);

        Button[] navBtns = {reviewBtn, paperBtn, profileBtn};

        contentArea.getChildren().add(reviewerDashboardView.buildPanel());

        reviewBtn.setOnAction(e -> {
            resetNavButtons(navBtns); selectNavButton(reviewBtn);
            swapContent(reviewerDashboardView.buildPanel());
        });
        paperBtn.setOnAction(e -> {
            resetNavButtons(navBtns); selectNavButton(paperBtn);
            swapContent(paperSearchView.buildPanel(false));
        });
        profileBtn.setOnAction(e -> {
            resetNavButtons(navBtns); selectNavButton(profileBtn);
            swapContent(buildProfilePanel(user));
        });

        for (Button btn : navBtns) sidebar.getChildren().add(btn);
    }

    // ═══════════════════════════════════════════════════════════
    // Login / Signup Dialog (shown in-app, Netflix style)
    // ═══════════════════════════════════════════════════════════

    public void showLoginDialog(Stage stage, boolean startWithRegister) {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle(startWithRegister ? "Create Account" : "Login to ResearchConnect");

        VBox content = new VBox(14);
        content.setPadding(new Insets(20));
        content.setMinWidth(380);
        content.setStyle("-fx-background-color: #0f1117;");

        Text heading = new Text(startWithRegister ? "Create Account" : "Welcome Back");
        heading.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
        heading.setFill(Color.web("#e2e8f0"));

        Text subtext = new Text(startWithRegister
            ? "Sign up to collaborate, publish, and more."
            : "Login to access all features.");
        subtext.setFont(Font.font("System", 12));
        subtext.setFill(Color.web("#8892a4"));

        // Toggle
        ToggleGroup mode = new ToggleGroup();
        ToggleButton loginTab = new ToggleButton("Login");
        ToggleButton registerTab = new ToggleButton("Register");
        loginTab.setToggleGroup(mode);
        registerTab.setToggleGroup(mode);
        if (startWithRegister) registerTab.setSelected(true);
        else loginTab.setSelected(true);
        String tabBase = "-fx-cursor:hand;-fx-font-size:12px;-fx-pref-width:120px;-fx-pref-height:36px;";
        loginTab.setStyle("-fx-background-color:#1a1f2e;-fx-text-fill:#8892a4;-fx-background-radius:6 0 0 6;" + tabBase);
        registerTab.setStyle("-fx-background-color:#1a1f2e;-fx-text-fill:#8892a4;-fx-background-radius:0 6 6 0;" + tabBase);
        Runnable updateTabs = () -> {
            loginTab.setStyle((loginTab.isSelected()
                ? "-fx-background-color:#6c9bff;-fx-text-fill:white;-fx-font-weight:bold;"
                : "-fx-background-color:#1a1f2e;-fx-text-fill:#8892a4;")
                + "-fx-background-radius:6 0 0 6;" + tabBase);
            registerTab.setStyle((registerTab.isSelected()
                ? "-fx-background-color:#6c9bff;-fx-text-fill:white;-fx-font-weight:bold;"
                : "-fx-background-color:#1a1f2e;-fx-text-fill:#8892a4;")
                + "-fx-background-radius:0 6 6 0;" + tabBase);
        };
        updateTabs.run();
        mode.selectedToggleProperty().addListener((obs, o, n) -> updateTabs.run());
        HBox tabRow = new HBox(0, loginTab, registerTab);
        tabRow.setAlignment(Pos.CENTER);

        // Fields
        TextField nameField = new TextField();
        nameField.setPromptText("Full name");
        nameField.setStyle(fieldStyle());
        Label nameLabel = new Label("Full Name");
        nameLabel.setTextFill(Color.web("#8892a4"));
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 11));

        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("RESEARCHER", "REVIEWER");
        roleCombo.setValue("RESEARCHER");
        roleCombo.setStyle("-fx-background-color:#1a1f2e;-fx-border-color:#2d3748;-fx-border-radius:6px;-fx-pref-width:340px;-fx-pref-height:40px;");
        Label roleLabel = new Label("Role");
        roleLabel.setTextFill(Color.web("#8892a4"));
        roleLabel.setFont(Font.font("System", FontWeight.BOLD, 11));

        TextField emailField = new TextField();
        emailField.setPromptText("email@example.com");
        emailField.setStyle(fieldStyle());
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setStyle(fieldStyle());

        Label statusLabel = new Label("");
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(340);

        // Register-only fields visibility
        Runnable updateVisibility = () -> {
            boolean isReg = registerTab.isSelected();
            nameLabel.setVisible(isReg); nameLabel.setManaged(isReg);
            nameField.setVisible(isReg); nameField.setManaged(isReg);
            roleLabel.setVisible(isReg); roleLabel.setManaged(isReg);
            roleCombo.setVisible(isReg); roleCombo.setManaged(isReg);
            heading.setText(isReg ? "Create Account" : "Welcome Back");
            subtext.setText(isReg ? "Sign up to collaborate, publish, and more." : "Login to access all features.");
        };
        mode.selectedToggleProperty().addListener((obs, o, n) -> updateVisibility.run());
        updateVisibility.run();

        Button actionBtn = new Button(startWithRegister ? "Create Account" : "Login");
        actionBtn.setStyle("-fx-background-color:#6c9bff;-fx-text-fill:white;-fx-font-size:14px;" +
                           "-fx-font-weight:bold;-fx-pref-width:340px;-fx-pref-height:44px;" +
                           "-fx-background-radius:8px;-fx-cursor:hand;");

        mode.selectedToggleProperty().addListener((obs, o, n) ->
            actionBtn.setText(registerTab.isSelected() ? "Create Account" : "Login"));

        actionBtn.setOnAction(e -> {
            statusLabel.setTextFill(Color.web("#fc8181"));
            String email = emailField.getText().trim();
            String password = passwordField.getText().trim();
            if (email.isEmpty() || password.isEmpty()) {
                statusLabel.setText("Please fill in all fields.");
                return;
            }
            boolean isReg = registerTab.isSelected();
            try {
                if (isReg) {
                    String name = nameField.getText().trim();
                    if (name.isEmpty()) { statusLabel.setText("Please enter your name."); return; }
                    User.UserRole role = User.UserRole.valueOf(roleCombo.getValue());
                    authController.register(name, email, password, role);
                    statusLabel.setTextFill(Color.web("#68d391"));
                    statusLabel.setText("Account created! Switching to login...");
                    mode.selectToggle(loginTab);
                } else {
                    User user = authController.login(email, password);
                    dialog.setResult(user);
                    dialog.close();
                    show(stage, user); // Re-show dashboard as logged-in user
                }
            } catch (Exception ex) {
                statusLabel.setText(ex.getMessage());
            }
        });

        content.getChildren().addAll(heading, subtext, tabRow,
            nameLabel, nameField, roleLabel, roleCombo,
            new Label("Email") {{ setTextFill(Color.web("#8892a4")); setFont(Font.font("System", FontWeight.BOLD, 11)); }},
            emailField,
            new Label("Password") {{ setTextFill(Color.web("#8892a4")); setFont(Font.font("System", FontWeight.BOLD, 11)); }},
            passwordField, actionBtn, statusLabel);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dialog.getDialogPane().setStyle("-fx-background-color:#0f1117;");
        dialog.showAndWait();
    }

    // ═══════════════════════════════════════════════════════════
    // Shared helpers
    // ═══════════════════════════════════════════════════════════

    private void swapContent(javafx.scene.Node panel) {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(panel);
    }

    public Stage getCurrentStage() { return currentStage; }

    private VBox buildProfilePanel(User user) {
        VBox panel = new VBox(16);
        panel.setPadding(new Insets(32));
        panel.setStyle("-fx-background-color: #1a1f2e; -fx-background-radius: 12px;");
        panel.setMaxWidth(500);

        Text title = new Text("My Profile");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 24));
        title.setFill(Color.web("#e2e8f0"));

        Label[] fields = {
            infoField("Name", user.getName()),
            infoField("Email", user.getEmail()),
            infoField("Role", user.getRole().toString()),
            infoField("Member Since", user.getCreatedAt() != null
                    ? user.getCreatedAt().toLocalDate().toString() : "—")
        };

        panel.getChildren().add(title);
        for (Label f : fields) panel.getChildren().add(f);
        return panel;
    }

    private Label infoField(String key, String value) {
        Label lbl = new Label(key + ": " + value);
        lbl.setFont(Font.font("System", 14));
        lbl.setTextFill(Color.web("#a0aec0"));
        lbl.setPadding(new Insets(4, 0, 4, 0));
        return lbl;
    }

    private String fieldStyle() {
        return "-fx-background-color:#1a1f2e;-fx-text-fill:#e2e8f0;" +
               "-fx-prompt-text-fill:#4a5568;-fx-border-color:#2d3748;" +
               "-fx-border-radius:6px;-fx-background-radius:6px;" +
               "-fx-pref-width:340px;-fx-pref-height:42px;-fx-padding:0 12px;";
    }

    private Button navButton(String text, boolean selected) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(10, 16, 10, 16));
        btn.setFont(Font.font("System", 13));
        if (selected) selectNavButton(btn);
        else btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8892a4; " +
                         "-fx-background-radius: 6px; -fx-cursor: hand; -fx-alignment: CENTER-LEFT;");
        btn.setOnMouseEntered(e -> {
            if (!btn.getStyle().contains("#6c9bff"))
                btn.setStyle("-fx-background-color: #2d3748; -fx-text-fill: #e2e8f0; " +
                             "-fx-background-radius: 6px; -fx-cursor: hand; -fx-alignment: CENTER-LEFT;");
        });
        btn.setOnMouseExited(e -> {
            if (!btn.getStyle().contains("#6c9bff"))
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8892a4; " +
                             "-fx-background-radius: 6px; -fx-cursor: hand; -fx-alignment: CENTER-LEFT;");
        });
        return btn;
    }

    private void selectNavButton(Button btn) {
        btn.setStyle("-fx-background-color: #6c9bff22; -fx-text-fill: #6c9bff; " +
                     "-fx-background-radius: 6px; -fx-cursor: hand; -fx-alignment: CENTER-LEFT; " +
                     "-fx-font-weight: bold; -fx-border-color: transparent transparent transparent #6c9bff; " +
                     "-fx-border-width: 0 0 0 3;");
    }

    private void resetNavButtons(Button[] btns) {
        for (Button b : btns)
            b.setStyle("-fx-background-color: transparent; -fx-text-fill: #8892a4; " +
                       "-fx-background-radius: 6px; -fx-cursor: hand; -fx-alignment: CENTER-LEFT;");
    }
}
