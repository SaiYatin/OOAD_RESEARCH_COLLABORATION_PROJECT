package com.research.view.dashboard;

import com.research.model.User;
import com.research.service.AuthService;
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
 * DashboardView - main shell after successful login.
 * Left sidebar navigation → swaps content area per tab.
 * Adapts navigation based on user role (RESEARCHER, REVIEWER, ADMIN).
 */
@Component
public class DashboardView {

    private final ExpertSearchView expertSearchView;
    private final PaperSearchView paperSearchView;
    private final CollaborationView collaborationView;
    private final MyResearchesView myResearchesView;
    private final ReviewerDashboardView reviewerDashboardView;
    private final AdminView adminView;
    private final AuthService authService;

    private StackPane contentArea;

    public DashboardView(ExpertSearchView expertSearchView,
                         PaperSearchView paperSearchView,
                         CollaborationView collaborationView,
                         MyResearchesView myResearchesView,
                         ReviewerDashboardView reviewerDashboardView,
                         AdminView adminView,
                         AuthService authService) {
        this.expertSearchView = expertSearchView;
        this.paperSearchView = paperSearchView;
        this.collaborationView = collaborationView;
        this.myResearchesView = myResearchesView;
        this.reviewerDashboardView = reviewerDashboardView;
        this.adminView = adminView;
        this.authService = authService;
    }

    public void show(Stage stage, User user) {
        System.out.println(">>> [DashboardView] show() called for user: " + user.getEmail() + " | Role: " + user.getRole());
        stage.setTitle("ResearchConnect — " + user.getName());
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

        Label userBadge = new Label(user.getName() + "  [" + user.getRole() + "]");
        userBadge.setStyle("-fx-background-color: #2d3748; -fx-text-fill: #a0aec0; " +
                           "-fx-padding: 6 14; -fx-background-radius: 20px; -fx-font-size: 12px;");

        Button logoutBtn = new Button("Logout");
        logoutBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #fc8181; " +
                           "-fx-cursor: hand; -fx-font-size: 12px; -fx-border-color: #fc8181; " +
                           "-fx-border-radius: 4px; -fx-padding: 4 10;");
        logoutBtn.setOnAction(e -> {
            System.out.println(">>> [DashboardView] Logout button clicked. Logging out user.");
            authService.logout();
            com.research.view.auth.LoginView loginView =
                com.research.ResearchCollaborationApp.getSpringContext()
                    .getBean(com.research.view.auth.LoginView.class);
            loginView.show(stage);
        });

        topBar.getChildren().addAll(logo, spacer, userBadge,
                new Region() {{ setMinWidth(12); }}, logoutBtn);

        // ── Left Sidebar ─────────────────────────────────────────────
        VBox sidebar = new VBox(4);
        sidebar.setPrefWidth(220);
        sidebar.setPadding(new Insets(24, 12, 24, 12));
        sidebar.setStyle("-fx-background-color: #1a1f2e;");

        Label sectionLabel = new Label("NAVIGATION");
        sectionLabel.setFont(Font.font("System", FontWeight.BOLD, 10));
        sectionLabel.setTextFill(Color.web("#4a5568"));
        sectionLabel.setPadding(new Insets(0, 0, 8, 8));

        // ── Content Area ─────────────────────────────────────────────
        contentArea = new StackPane();
        contentArea.setStyle("-fx-background-color: #0f1117;");
        contentArea.setPadding(new Insets(24));

        sidebar.getChildren().add(sectionLabel);

        // Build navigation based on role
        if (user.getRole() == User.UserRole.REVIEWER) {
            buildReviewerNav(sidebar, user);
        } else {
            buildResearcherNav(sidebar, user);
        }

        // Admin section
        if (user.getRole() == User.UserRole.ADMIN) {
            Label adminLabel = new Label("ADMIN");
            adminLabel.setFont(Font.font("System", FontWeight.BOLD, 10));
            adminLabel.setTextFill(Color.web("#4a5568"));
            adminLabel.setPadding(new Insets(16, 0, 8, 8));
            Button adminBtn = navButton("⚙️  Admin Panel", false);
            adminBtn.setOnAction(e -> {
                System.out.println(">>> [DashboardView] Navigating to Admin Panel");
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
    // Researcher Navigation
    // ═══════════════════════════════════════════════════════════

    private void buildResearcherNav(VBox sidebar, User user) {
        Button paperBtn   = navButton("📄  Paper Search", true);
        Button expertBtn  = navButton("🔬  Find Experts", false);
        Button collabBtn  = navButton("🤝  Collaborations", false);
        Button researchBtn = navButton("🔬  My Researches", false);
        Button profileBtn = navButton("👤  My Profile", false);

        Button[] navBtns = {paperBtn, expertBtn, collabBtn, researchBtn, profileBtn};

        contentArea.getChildren().add(paperSearchView.buildPanel());

        paperBtn.setOnAction(e -> {
            System.out.println(">>> [DashboardView] Navigating to Paper Search");
            resetNavButtons(navBtns); selectNavButton(paperBtn);
            swapContent(paperSearchView.buildPanel());
        });
        expertBtn.setOnAction(e -> {
            System.out.println(">>> [DashboardView] Navigating to Expert Search");
            resetNavButtons(navBtns); selectNavButton(expertBtn);
            swapContent(expertSearchView.buildPanel());
        });
        collabBtn.setOnAction(e -> {
            System.out.println(">>> [DashboardView] Navigating to Collaborations");
            resetNavButtons(navBtns); selectNavButton(collabBtn);
            swapContent(collaborationView.buildPanel(user));
        });
        researchBtn.setOnAction(e -> {
            System.out.println(">>> [DashboardView] Navigating to My Researches");
            resetNavButtons(navBtns); selectNavButton(researchBtn);
            swapContent(myResearchesView.buildPanel());
        });
        profileBtn.setOnAction(e -> {
            System.out.println(">>> [DashboardView] Navigating to My Profile");
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
            System.out.println(">>> [DashboardView] Navigating to Review Papers");
            resetNavButtons(navBtns); selectNavButton(reviewBtn);
            swapContent(reviewerDashboardView.buildPanel());
        });
        paperBtn.setOnAction(e -> {
            System.out.println(">>> [DashboardView] Navigating to Paper Search");
            resetNavButtons(navBtns); selectNavButton(paperBtn);
            swapContent(paperSearchView.buildPanel());
        });
        profileBtn.setOnAction(e -> {
            System.out.println(">>> [DashboardView] Navigating to My Profile");
            resetNavButtons(navBtns); selectNavButton(profileBtn);
            swapContent(buildProfilePanel(user));
        });

        for (Button btn : navBtns) sidebar.getChildren().add(btn);
    }

    // ═══════════════════════════════════════════════════════════
    // Shared helpers
    // ═══════════════════════════════════════════════════════════

    private void swapContent(javafx.scene.Node panel) {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(panel);
    }

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

    private Button navButton(String text, boolean selected) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(10, 16, 10, 16));
        btn.setFont(Font.font("System", 13));
        if (selected) {
            selectNavButton(btn);
        } else {
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8892a4; " +
                         "-fx-background-radius: 6px; -fx-cursor: hand; -fx-alignment: CENTER-LEFT;");
        }
        btn.setOnMouseEntered(e -> {
            if (!btn.getStyle().contains("#6c9bff")) {
                btn.setStyle("-fx-background-color: #2d3748; -fx-text-fill: #e2e8f0; " +
                             "-fx-background-radius: 6px; -fx-cursor: hand; -fx-alignment: CENTER-LEFT;");
            }
        });
        btn.setOnMouseExited(e -> {
            if (!btn.getStyle().contains("#6c9bff")) {
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8892a4; " +
                             "-fx-background-radius: 6px; -fx-cursor: hand; -fx-alignment: CENTER-LEFT;");
            }
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
        for (Button b : btns) {
            b.setStyle("-fx-background-color: transparent; -fx-text-fill: #8892a4; " +
                       "-fx-background-radius: 6px; -fx-cursor: hand; -fx-alignment: CENTER-LEFT;");
        }
    }
}
