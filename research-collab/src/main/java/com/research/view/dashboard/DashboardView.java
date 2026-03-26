package com.research.view.dashboard;

import com.research.model.User;
import com.research.service.AuthService;
import com.research.view.expert.ExpertSearchView;
import com.research.view.paper.PaperSearchView;
import com.research.view.collab.CollaborationView;
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
 * MVC: This is the View / routing layer.
 */
@Component
public class DashboardView {

    private final ExpertSearchView expertSearchView;
    private final PaperSearchView paperSearchView;
    private final CollaborationView collaborationView;
    private final AuthService authService;

    // Content area reference for panel swapping
    private StackPane contentArea;
    private Stage currentStage;

    public DashboardView(ExpertSearchView expertSearchView,
                         PaperSearchView paperSearchView,
                         CollaborationView collaborationView,
                         AuthService authService) {
        this.expertSearchView = expertSearchView;
        this.paperSearchView = paperSearchView;
        this.collaborationView = collaborationView;
        this.authService = authService;
    }

    public void show(Stage stage, User user) {
        this.currentStage = stage;
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
            authService.logout();
            // Return to login - rebuild LoginView
            stage.setWidth(900);
            stage.setHeight(650);
            stage.setResizable(false);
            // Re-launch login (in full impl would re-show LoginView)
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

        // Default view: Paper Search
        contentArea.getChildren().add(paperSearchView.buildPanel());

        // Nav buttons
        Button[] navBtns = {
            navButton("📄  Paper Search",   true),
            navButton("🔬  Find Experts",   false),
            navButton("🤝  Collaborations", false),
            navButton("👤  My Profile",     false)
        };

        navBtns[0].setOnAction(e -> {
            resetNavButtons(navBtns);
            selectNavButton(navBtns[0]);
            swapContent(paperSearchView.buildPanel());
        });
        navBtns[1].setOnAction(e -> {
            resetNavButtons(navBtns);
            selectNavButton(navBtns[1]);
            swapContent(expertSearchView.buildPanel());
        });
        navBtns[2].setOnAction(e -> {
            resetNavButtons(navBtns);
            selectNavButton(navBtns[2]);
            swapContent(collaborationView.buildPanel(user));
        });
        navBtns[3].setOnAction(e -> {
            resetNavButtons(navBtns);
            selectNavButton(navBtns[3]);
            swapContent(buildProfilePanel(user));
        });

        sidebar.getChildren().add(sectionLabel);
        for (Button btn : navBtns) sidebar.getChildren().add(btn);

        // Role-specific sections
        if (user.getRole() == User.UserRole.ADMIN) {
            Label adminLabel = new Label("ADMIN");
            adminLabel.setFont(Font.font("System", FontWeight.BOLD, 10));
            adminLabel.setTextFill(Color.web("#4a5568"));
            adminLabel.setPadding(new Insets(16, 0, 8, 8));
            Button adminBtn = navButton("⚙️  Admin Panel", false);
            sidebar.getChildren().addAll(adminLabel, adminBtn);
        }

        // Version info
        Region sidebarSpacer = new Region();
        VBox.setVgrow(sidebarSpacer, Priority.ALWAYS);
        Label versionLabel = new Label("v1.0.0 · Research Collab");
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

    // ── Nav button styling ────────────────────────────────────────────

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
