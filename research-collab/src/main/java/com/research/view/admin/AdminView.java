package com.research.view.admin;

import com.research.model.*;
import com.research.repository.*;
import com.research.service.ExpertService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AdminView — Admin-only panel for managing users, experts, projects.
 */
@Component
public class AdminView {

    private final UserRepository userRepository;
    private final ExpertRepository expertRepository;
    private final ResearchProjectRepository projectRepository;
    private final CollaborationRequestRepository requestRepository;
    private final ExpertService expertService;

    public AdminView(UserRepository userRepository,
                     ExpertRepository expertRepository,
                     ResearchProjectRepository projectRepository,
                     CollaborationRequestRepository requestRepository,
                     ExpertService expertService) {
        this.userRepository = userRepository;
        this.expertRepository = expertRepository;
        this.projectRepository = projectRepository;
        this.requestRepository = requestRepository;
        this.expertService = expertService;
    }

    public VBox buildPanel() {
        VBox panel = new VBox(0);
        panel.setStyle("-fx-background-color: #0f1117;");

        VBox header = new VBox(4);
        header.setPadding(new Insets(0, 0, 16, 0));
        Text title = new Text("⚙️ Admin Panel");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 26));
        title.setFill(Color.web("#e2e8f0"));
        Text subtitle = new Text("Manage users, experts, projects, and system configuration");
        subtitle.setFont(Font.font("System", 13));
        subtitle.setFill(Color.web("#8892a4"));
        header.getChildren().addAll(title, subtitle);

        // Tabs
        ToggleGroup tabGroup = new ToggleGroup();
        ToggleButton usersTab = tabBtn("👥 Users", tabGroup, true);
        ToggleButton expertsTab = tabBtn("🔬 Experts", tabGroup, false);
        ToggleButton projectsTab = tabBtn("🗂 Projects", tabGroup, false);
        ToggleButton statsTab = tabBtn("📊 Stats", tabGroup, false);
        HBox tabRow = new HBox(4, usersTab, expertsTab, projectsTab, statsTab);
        tabRow.setPadding(new Insets(0, 0, 16, 0));

        StackPane content = new StackPane();
        VBox.setVgrow(content, Priority.ALWAYS);
        content.getChildren().add(buildUsersPane());

        tabGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            content.getChildren().clear();
            if (n == usersTab) content.getChildren().add(buildUsersPane());
            else if (n == expertsTab) content.getChildren().add(buildExpertsPane());
            else if (n == projectsTab) content.getChildren().add(buildProjectsPane());
            else if (n == statsTab) content.getChildren().add(buildStatsPane());
        });

        panel.getChildren().addAll(header, tabRow, content);
        return panel;
    }

    // ── Users Tab ────────────────────────────────────────────
    private VBox buildUsersPane() {
        VBox pane = new VBox(12);
        Text heading = new Text("All Registered Users");
        heading.setFont(Font.font("Georgia", FontWeight.BOLD, 20));
        heading.setFill(Color.web("#e2e8f0"));
        pane.getChildren().add(heading);

        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            pane.getChildren().add(emptyLabel("No users registered."));
        } else {
            for (User user : users) {
                pane.getChildren().add(buildUserCard(user));
            }
        }
        return pane;
    }

    private HBox buildUserCard(User user) {
        HBox card = new HBox(16);
        card.setPadding(new Insets(12, 16, 12, 16));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(700);
        card.setStyle("-fx-background-color: #1a1f2e; -fx-background-radius: 8px; " +
                      "-fx-border-color: #2d3748; -fx-border-radius: 8px;");

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label nameLabel = new Label(user.getName());
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        nameLabel.setTextFill(Color.web("#e2e8f0"));

        Label emailLabel = new Label(user.getEmail());
        emailLabel.setFont(Font.font("System", 11));
        emailLabel.setTextFill(Color.web("#8892a4"));

        info.getChildren().addAll(nameLabel, emailLabel);

        Label roleBadge = new Label(user.getRole().name());
        String roleColor = switch (user.getRole()) {
            case ADMIN -> "#fc8181";
            case RESEARCHER -> "#6c9bff";
            case REVIEWER -> "#68d391";
            case VISITOR -> "#a0aec0";
        };
        roleBadge.setStyle("-fx-background-color: " + roleColor + "22; -fx-text-fill: " + roleColor + ";" +
                           "-fx-padding: 4 12; -fx-background-radius: 12px; -fx-font-size: 11px; -fx-font-weight: bold;");

        Label dateLabel = new Label(user.getCreatedAt() != null ? user.getCreatedAt().toLocalDate().toString() : "—");
        dateLabel.setTextFill(Color.web("#4a5568"));
        dateLabel.setFont(Font.font("System", 10));

        card.getChildren().addAll(info, roleBadge, dateLabel);
        return card;
    }

    // ── Experts Tab (Add + Manage) ───────────────────────────
    private VBox buildExpertsPane() {
        VBox pane = new VBox(16);
        Text heading = new Text("Expert Management");
        heading.setFont(Font.font("Georgia", FontWeight.BOLD, 20));
        heading.setFill(Color.web("#e2e8f0"));

        // Add Expert Form
        VBox form = new VBox(10);
        form.setPadding(new Insets(16));
        form.setStyle("-fx-background-color: #1a1f2e; -fx-background-radius: 10px;");
        form.setMaxWidth(600);

        Text formTitle = new Text("Add New Expert");
        formTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        formTitle.setFill(Color.web("#6c9bff"));

        TextField nameField = styledField("Expert Name");
        TextField emailField = styledField("Email");
        TextField domainField = styledField("Domain (e.g. Machine Learning)");
        TextField areasField = styledField("Research Areas (comma separated)");
        TextField instField = styledField("Institution");

        Label status = new Label("");
        status.setFont(Font.font("System", 12));

        Button addBtn = new Button("+ Add Expert");
        addBtn.setStyle("-fx-background-color: #68d391; -fx-text-fill: white; " +
                        "-fx-background-radius: 6px; -fx-cursor: hand; -fx-font-weight: bold;");
        addBtn.setOnAction(e -> {
            try {
                Expert expert = new Expert();
                expert.setName(nameField.getText().trim());
                expert.setEmail(emailField.getText().trim());
                expert.setDomain(domainField.getText().trim());
                expert.setResearchAreas(areasField.getText().trim());
                expert.setInstitution(instField.getText().trim());
                expert.setActive(true);
                expertService.saveExpert(expert);
                status.setTextFill(Color.web("#68d391"));
                status.setText("✓ Expert added successfully!");
                nameField.clear(); emailField.clear(); domainField.clear();
                areasField.clear(); instField.clear();
            } catch (Exception ex) {
                status.setTextFill(Color.web("#fc8181"));
                status.setText("Error: " + ex.getMessage());
            }
        });

        form.getChildren().addAll(formTitle, nameField, emailField, domainField, areasField, instField, addBtn, status);

        // Expert list
        Text listTitle = new Text("All Experts (" + expertRepository.count() + ")");
        listTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        listTitle.setFill(Color.web("#e2e8f0"));

        pane.getChildren().addAll(heading, form, listTitle);

        List<Expert> experts = expertService.getAllActiveExperts();
        for (Expert expert : experts) {
            HBox row = new HBox(12);
            row.setPadding(new Insets(8, 12, 8, 12));
            row.setAlignment(Pos.CENTER_LEFT);
            row.setMaxWidth(700);
            row.setStyle("-fx-background-color: #1a1f2e; -fx-background-radius: 6px;");

            Label name = new Label(expert.getName());
            name.setFont(Font.font("System", FontWeight.BOLD, 12));
            name.setTextFill(Color.web("#e2e8f0"));
            HBox.setHgrow(name, Priority.ALWAYS);

            Label domain = new Label(expert.getDomain() != null ? expert.getDomain() : "—");
            domain.setTextFill(Color.web("#6c9bff"));
            domain.setFont(Font.font("System", 11));

            Label email = new Label(expert.getEmail() != null ? expert.getEmail() : "");
            email.setTextFill(Color.web("#4a5568"));
            email.setFont(Font.font("System", 10));

            row.getChildren().addAll(name, domain, email);
            pane.getChildren().add(row);
        }

        return pane;
    }

    // ── Projects Tab ─────────────────────────────────────────
    private VBox buildProjectsPane() {
        VBox pane = new VBox(12);
        Text heading = new Text("All Research Projects");
        heading.setFont(Font.font("Georgia", FontWeight.BOLD, 20));
        heading.setFill(Color.web("#e2e8f0"));
        pane.getChildren().add(heading);

        List<ResearchProject> projects = projectRepository.findAll();
        if (projects.isEmpty()) {
            pane.getChildren().add(emptyLabel("No projects yet."));
        } else {
            for (ResearchProject p : projects) {
                VBox card = new VBox(6);
                card.setPadding(new Insets(12));
                card.setMaxWidth(700);
                card.setStyle("-fx-background-color: #1a1f2e; -fx-background-radius: 8px; " +
                              "-fx-border-color: #2d3748; -fx-border-radius: 8px;");

                HBox row1 = new HBox(10);
                row1.setAlignment(Pos.CENTER_LEFT);
                Label topic = new Label(p.getTopic());
                topic.setFont(Font.font("System", FontWeight.BOLD, 14));
                topic.setTextFill(Color.web("#e2e8f0"));

                Label statusBadge = new Label(p.getStatus().name());
                statusBadge.setStyle("-fx-background-color: #68d39122; -fx-text-fill: #68d391; " +
                                     "-fx-padding: 2 8; -fx-background-radius: 8px; -fx-font-size: 10px;");
                row1.getChildren().addAll(topic, statusBadge);

                Label owner = new Label("Owner: " + (p.getOwner() != null ? p.getOwner().getName() : "—"));
                owner.setTextFill(Color.web("#6c9bff"));
                owner.setFont(Font.font("System", 11));

                Label members = new Label("Members: " + p.getMembers().size());
                members.setTextFill(Color.web("#8892a4"));
                members.setFont(Font.font("System", 11));

                card.getChildren().addAll(row1, owner, members);
                pane.getChildren().add(card);
            }
        }
        return pane;
    }

    // ── Stats Tab ────────────────────────────────────────────
    private VBox buildStatsPane() {
        VBox pane = new VBox(16);
        Text heading = new Text("System Statistics");
        heading.setFont(Font.font("Georgia", FontWeight.BOLD, 20));
        heading.setFill(Color.web("#e2e8f0"));

        long userCount = userRepository.count();
        long expertCount = expertRepository.count();
        long projectCount = projectRepository.count();
        long requestCount = requestRepository.count();
        long activeProjects = projectRepository.findByStatus(ResearchProject.ProjectStatus.ACTIVE).size();
        long openCollabs = projectRepository.findByLookingForCollaboratorsTrue().size();

        HBox statsGrid = new HBox(16);
        statsGrid.setAlignment(Pos.CENTER_LEFT);
        statsGrid.getChildren().addAll(
            statCard("👥", String.valueOf(userCount), "Users"),
            statCard("🔬", String.valueOf(expertCount), "Experts"),
            statCard("🗂", String.valueOf(projectCount), "Projects"),
            statCard("📬", String.valueOf(requestCount), "Requests"),
            statCard("🟢", String.valueOf(activeProjects), "Active"),
            statCard("🤝", String.valueOf(openCollabs), "Open Collabs")
        );

        pane.getChildren().addAll(heading, statsGrid);
        return pane;
    }

    private VBox statCard(String icon, String value, String label) {
        VBox card = new VBox(4);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(16));
        card.setPrefWidth(120);
        card.setStyle("-fx-background-color: #1a1f2e; -fx-background-radius: 10px; " +
                      "-fx-border-color: #2d3748; -fx-border-radius: 10px;");

        Text iconText = new Text(icon);
        iconText.setFont(Font.font("System", 24));

        Text valText = new Text(value);
        valText.setFont(Font.font("Georgia", FontWeight.BOLD, 28));
        valText.setFill(Color.web("#6c9bff"));

        Text labelText = new Text(label);
        labelText.setFont(Font.font("System", 11));
        labelText.setFill(Color.web("#8892a4"));

        card.getChildren().addAll(iconText, valText, labelText);
        return card;
    }

    // ── Helpers ──────────────────────────────────────────────
    private Label emptyLabel(String text) {
        Label lbl = new Label(text);
        lbl.setTextFill(Color.web("#4a5568"));
        lbl.setFont(Font.font("System", FontPosture.ITALIC, 13));
        return lbl;
    }

    private TextField styledField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setMaxWidth(500);
        field.setStyle("-fx-background-color: #0f1117; -fx-text-fill: #e2e8f0; " +
                       "-fx-prompt-text-fill: #4a5568; -fx-border-color: #2d3748; " +
                       "-fx-border-radius: 6px; -fx-background-radius: 6px; " +
                       "-fx-pref-height: 36px; -fx-padding: 0 12px;");
        return field;
    }

    private ToggleButton tabBtn(String text, ToggleGroup group, boolean selected) {
        ToggleButton btn = new ToggleButton(text);
        btn.setToggleGroup(group);
        btn.setSelected(selected);
        btn.setPadding(new Insets(8, 18, 8, 18));
        String base = selected
            ? "-fx-background-color: #6c9bff; -fx-text-fill: white; -fx-font-weight: bold;"
            : "-fx-background-color: #1a1f2e; -fx-text-fill: #8892a4;";
        btn.setStyle(base + " -fx-background-radius: 8px; -fx-cursor: hand; -fx-font-size: 12px; " +
                     "-fx-border-color: #2d3748; -fx-border-radius: 8px;");
        btn.selectedProperty().addListener((obs, was, is) -> {
            String s = is
                ? "-fx-background-color: #6c9bff; -fx-text-fill: white; -fx-font-weight: bold;"
                : "-fx-background-color: #1a1f2e; -fx-text-fill: #8892a4;";
            btn.setStyle(s + " -fx-background-radius: 8px; -fx-cursor: hand; -fx-font-size: 12px; " +
                         "-fx-border-color: #2d3748; -fx-border-radius: 8px;");
        });
        return btn;
    }
}
