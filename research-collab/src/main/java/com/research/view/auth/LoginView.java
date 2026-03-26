package com.research.view.auth;

import com.research.model.User;
import com.research.service.AuthService;
import com.research.view.dashboard.DashboardView;
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
 * LoginView - JavaFX login and registration screen.
 * Member 1's view layer for the Auth use case.
 * MVC: This is the View. AuthService is the Controller+Model.
 */
@Component
public class LoginView {

    private final AuthService authService;
    private final DashboardView dashboardView;

    public LoginView(AuthService authService, DashboardView dashboardView) {
        this.authService = authService;
        this.dashboardView = dashboardView;
    }

    public void show(Stage stage) {
        stage.setTitle("Research Collaboration System — Login");
        stage.setWidth(900);
        stage.setHeight(650);
        stage.setResizable(false);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0f1117;");

        // ── Left Panel: Branding ──────────────────────────────────────
        VBox leftPanel = new VBox(20);
        leftPanel.setPrefWidth(380);
        leftPanel.setAlignment(Pos.CENTER);
        leftPanel.setPadding(new Insets(60));
        leftPanel.setStyle("-fx-background-color: #1a1f2e;");

        Text appName = new Text("ResearchConnect");
        appName.setFont(Font.font("Georgia", FontWeight.BOLD, 28));
        appName.setFill(Color.web("#6c9bff"));

        Text tagline = new Text("Find experts. Collaborate.\nAdvance knowledge.");
        tagline.setFont(Font.font("Georgia", FontPosture.ITALIC, 15));
        tagline.setFill(Color.web("#8892a4"));
        tagline.setTextAlignment(TextAlignment.CENTER);

        // Stats summary boxes
        HBox statsRow = new HBox(16);
        statsRow.setAlignment(Pos.CENTER);
        statsRow.getChildren().addAll(
            statBox("24+", "Experts"),
            statBox("∞", "Papers"),
            statBox("AI", "Powered")
        );

        Text poweredBy = new Text("Powered by n8n · Spring Boot · JavaFX");
        poweredBy.setFont(Font.font("Courier New", 11));
        poweredBy.setFill(Color.web("#4a5568"));

        leftPanel.getChildren().addAll(appName, tagline, statsRow, poweredBy);

        // ── Right Panel: Login/Register Form ─────────────────────────
        VBox rightPanel = new VBox(20);
        rightPanel.setAlignment(Pos.CENTER);
        rightPanel.setPadding(new Insets(60, 50, 60, 50));
        rightPanel.setStyle("-fx-background-color: #0f1117;");

        // Tab selector: Login / Register
        ToggleGroup modeGroup = new ToggleGroup();
        ToggleButton loginBtn = new ToggleButton("Login");
        ToggleButton registerBtn = new ToggleButton("Register");
        loginBtn.setToggleGroup(modeGroup);
        registerBtn.setToggleGroup(modeGroup);
        loginBtn.setSelected(true);
        styleToggleButton(loginBtn, true);
        styleToggleButton(registerBtn, false);

        HBox tabRow = new HBox(0, loginBtn, registerBtn);
        tabRow.setAlignment(Pos.CENTER);

        // Shared fields
        Label emailLabel = new Label("Email");
        styleLabel(emailLabel);
        TextField emailField = new TextField();
        styleTextField(emailField, "researcher@pes.edu");

        Label passwordLabel = new Label("Password");
        styleLabel(passwordLabel);
        PasswordField passwordField = new PasswordField();
        styleTextField(passwordField, "••••••••");

        // Register-only fields (hidden by default)
        Label nameLabel = new Label("Full Name");
        styleLabel(nameLabel);
        TextField nameField = new TextField();
        styleTextField(nameField, "Dr. John Smith");
        nameLabel.setVisible(false);
        nameField.setVisible(false);
        nameLabel.setManaged(false);
        nameField.setManaged(false);

        Label roleLabel = new Label("Role");
        styleLabel(roleLabel);
        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("RESEARCHER", "REVIEWER", "COLLABORATOR", "VISITOR");
        roleCombo.setValue("RESEARCHER");
        styleComboBox(roleCombo);
        roleLabel.setVisible(false);
        roleCombo.setVisible(false);
        roleLabel.setManaged(false);
        roleCombo.setManaged(false);

        // Action button
        Button actionBtn = new Button("Login");
        actionBtn.setStyle(
            "-fx-background-color: #6c9bff; -fx-text-fill: white; " +
            "-fx-font-size: 15px; -fx-font-weight: bold; " +
            "-fx-pref-width: 340px; -fx-pref-height: 48px; " +
            "-fx-background-radius: 8px; -fx-cursor: hand;");
        actionBtn.setOnMouseEntered(e -> actionBtn.setStyle(
            "-fx-background-color: #4f7fff; -fx-text-fill: white; " +
            "-fx-font-size: 15px; -fx-font-weight: bold; " +
            "-fx-pref-width: 340px; -fx-pref-height: 48px; " +
            "-fx-background-radius: 8px; -fx-cursor: hand;"));
        actionBtn.setOnMouseExited(e -> actionBtn.setStyle(
            "-fx-background-color: #6c9bff; -fx-text-fill: white; " +
            "-fx-font-size: 15px; -fx-font-weight: bold; " +
            "-fx-pref-width: 340px; -fx-pref-height: 48px; " +
            "-fx-background-radius: 8px; -fx-cursor: hand;"));

        Label statusLabel = new Label("");
        statusLabel.setFont(Font.font("System", 12));
        statusLabel.setTextFill(Color.web("#ff6b6b"));
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(340);

        // Toggle between Login / Register modes
        modeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            boolean isRegister = newVal == registerBtn;
            nameLabel.setVisible(isRegister);
            nameField.setVisible(isRegister);
            nameLabel.setManaged(isRegister);
            nameField.setManaged(isRegister);
            roleLabel.setVisible(isRegister);
            roleCombo.setVisible(isRegister);
            roleLabel.setManaged(isRegister);
            roleCombo.setManaged(isRegister);
            actionBtn.setText(isRegister ? "Create Account" : "Login");
            statusLabel.setText("");
        });

        // Login / Register action
        actionBtn.setOnAction(e -> {
            statusLabel.setTextFill(Color.web("#ff6b6b"));
            String email = emailField.getText().trim();
            String password = passwordField.getText().trim();

            if (email.isEmpty() || password.isEmpty()) {
                statusLabel.setText("Please fill in all fields.");
                return;
            }

            boolean isRegister = modeGroup.getSelectedToggle() == registerBtn;
            try {
                if (isRegister) {
                    String name = nameField.getText().trim();
                    String roleStr = roleCombo.getValue();
                    if (name.isEmpty()) {
                        statusLabel.setText("Please enter your name.");
                        return;
                    }
                    User.UserRole role = User.UserRole.valueOf(roleStr);
                    authService.register(name, email, password, role);
                    statusLabel.setTextFill(Color.web("#68d391"));
                    statusLabel.setText("Account created! Please login.");
                    modeGroup.selectToggle(loginBtn);
                } else {
                    User user = authService.login(email, password);
                    statusLabel.setTextFill(Color.web("#68d391"));
                    statusLabel.setText("Welcome, " + user.getName() + "!");
                    dashboardView.show(stage, user);
                }
            } catch (Exception ex) {
                statusLabel.setText(ex.getMessage());
            }
        });

        rightPanel.getChildren().addAll(
            tabRow,
            nameLabel, nameField,
            emailLabel, emailField,
            passwordLabel, passwordField,
            roleLabel, roleCombo,
            actionBtn, statusLabel
        );

        root.setLeft(leftPanel);
        root.setCenter(rightPanel);

        stage.setScene(new Scene(root));
        stage.show();
    }

    // ── Styling helpers ───────────────────────────────────────────────

    private VBox statBox(String value, String label) {
        Text val = new Text(value);
        val.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
        val.setFill(Color.web("#6c9bff"));
        Text lbl = new Text(label);
        lbl.setFont(Font.font("System", 11));
        lbl.setFill(Color.web("#8892a4"));
        VBox box = new VBox(4, val, lbl);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(12));
        box.setStyle("-fx-background-color: #0f1117; -fx-background-radius: 8px;");
        box.setPrefWidth(90);
        return box;
    }

    private void styleLabel(Label label) {
        label.setFont(Font.font("System", FontWeight.BOLD, 12));
        label.setTextFill(Color.web("#8892a4"));
    }

    private void styleTextField(TextInputControl field, String prompt) {
        field.setPromptText(prompt);
        field.setStyle(
            "-fx-background-color: #1a1f2e; -fx-text-fill: #e2e8f0; " +
            "-fx-prompt-text-fill: #4a5568; -fx-border-color: #2d3748; " +
            "-fx-border-radius: 6px; -fx-background-radius: 6px; " +
            "-fx-pref-width: 340px; -fx-pref-height: 42px; -fx-padding: 0 12px;");
    }

    private void styleComboBox(ComboBox<?> combo) {
        combo.setStyle(
            "-fx-background-color: #1a1f2e; -fx-text-fill: #e2e8f0; " +
            "-fx-border-color: #2d3748; -fx-border-radius: 6px; " +
            "-fx-background-radius: 6px; -fx-pref-width: 340px; -fx-pref-height: 42px;");
    }

    private void styleToggleButton(ToggleButton btn, boolean isLeft) {
        String radius = isLeft ? "6px 0 0 6px" : "0 6px 6px 0";
        btn.setStyle(
            "-fx-background-color: #1a1f2e; -fx-text-fill: #8892a4; " +
            "-fx-font-size: 13px; -fx-pref-width: 140px; -fx-pref-height: 40px; " +
            "-fx-background-radius: " + radius + "; -fx-cursor: hand;");
        btn.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (isSelected) {
                btn.setStyle(
                    "-fx-background-color: #6c9bff; -fx-text-fill: white; " +
                    "-fx-font-size: 13px; -fx-font-weight: bold; " +
                    "-fx-pref-width: 140px; -fx-pref-height: 40px; " +
                    "-fx-background-radius: " + radius + "; -fx-cursor: hand;");
            } else {
                btn.setStyle(
                    "-fx-background-color: #1a1f2e; -fx-text-fill: #8892a4; " +
                    "-fx-font-size: 13px; -fx-pref-width: 140px; -fx-pref-height: 40px; " +
                    "-fx-background-radius: " + radius + "; -fx-cursor: hand;");
            }
        });
    }
}
