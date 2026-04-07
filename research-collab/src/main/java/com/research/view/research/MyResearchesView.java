package com.research.view.research;

import com.research.model.*;
import com.research.repository.*;
import com.research.service.AuthService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.FileChooser;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MyResearchesView — Manages the researcher's own projects.
 * Tabs: My Projects, New Project, Discover & Follow
 */
@Component
public class MyResearchesView {

    private final ResearchProjectRepository projectRepository;
    private final ProjectMessageRepository messageRepository;
    private final ProjectUpdateRepository updateRepository;
    private final ResearchPaperRepository paperRepository;
    private final PublicOpinionRepository opinionRepository;

    private StackPane rootContent;

    private static final String[] AVAILABLE_DOMAINS = {
        "Machine Learning", "Deep Learning", "Natural Language Processing",
        "Computer Vision", "Data Science", "Signal Processing",
        "Robotics", "IoT", "Network Security", "Biotechnology",
        "Economics", "Composite Materials", "Heat Transfer",
        "Earthquake Engineering", "Mathematics", "Other"
    };

    public MyResearchesView(ResearchProjectRepository projectRepository,
                            ProjectMessageRepository messageRepository,
                            ProjectUpdateRepository updateRepository,
                            ResearchPaperRepository paperRepository,
                            PublicOpinionRepository opinionRepository) {
        this.projectRepository = projectRepository;
        this.messageRepository = messageRepository;
        this.updateRepository = updateRepository;
        this.paperRepository = paperRepository;
        this.opinionRepository = opinionRepository;
    }

    public VBox buildPanel() {
        User currentUser = AuthService.getCurrentUser();
        VBox panel = new VBox(0);
        panel.setStyle("-fx-background-color: #0f1117;");
        rootContent = new StackPane();
        VBox.setVgrow(rootContent, Priority.ALWAYS);
        rootContent.getChildren().add(buildMainView(currentUser));
        panel.getChildren().add(rootContent);
        return panel;
    }

    // ═══════════════════════════════════════════════════════════
    // Main View
    // ═══════════════════════════════════════════════════════════

    private VBox buildMainView(User currentUser) {
        VBox main = new VBox(0);
        VBox header = new VBox(4);
        header.setPadding(new Insets(0, 0, 16, 0));
        Text title = new Text("My Researches");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 26));
        title.setFill(Color.web("#e2e8f0"));
        Text subtitle = new Text("Create, manage, and discover research projects");
        subtitle.setFont(Font.font("System", 13));
        subtitle.setFill(Color.web("#8892a4"));
        header.getChildren().addAll(title, subtitle);

        ToggleGroup tabGroup = new ToggleGroup();
        ToggleButton myProjectsTab = tabBtn("🔬 My Projects", tabGroup, true);
        ToggleButton createTab = tabBtn("➕ New Project", tabGroup, false);
        ToggleButton discoverTab = tabBtn("🌐 Discover & Follow", tabGroup, false);
        HBox tabRow = new HBox(4, myProjectsTab, createTab, discoverTab);
        tabRow.setPadding(new Insets(0, 0, 16, 0));

        StackPane content = new StackPane();
        VBox.setVgrow(content, Priority.ALWAYS);
        content.getChildren().add(buildMyProjectsPane(currentUser));

        tabGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            content.getChildren().clear();
            if (n == myProjectsTab) content.getChildren().add(buildMyProjectsPane(currentUser));
            else if (n == createTab) content.getChildren().add(buildCreateProjectPane(currentUser));
            else if (n == discoverTab) content.getChildren().add(buildDiscoverPane(currentUser));
        });

        main.getChildren().addAll(header, tabRow, content);
        return main;
    }

    // ═══════════════════════════════════════════════════════════
    // My Projects (with status filter)
    // ═══════════════════════════════════════════════════════════

    private VBox buildMyProjectsPane(User currentUser) {
        VBox pane = new VBox(12);
        pane.setAlignment(Pos.TOP_LEFT);

        Text heading = new Text("Your Projects");
        heading.setFont(Font.font("Georgia", FontWeight.BOLD, 20));
        heading.setFill(Color.web("#e2e8f0"));

        // Filter row
        HBox filterRow = new HBox(8);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        Label filterLabel = new Label("Filter:");
        filterLabel.setTextFill(Color.web("#8892a4"));
        ToggleGroup filterGroup = new ToggleGroup();
        ToggleButton allBtn = tabBtn("All", filterGroup, true);
        ToggleButton activeBtn = tabBtn("Active", filterGroup, false);
        ToggleButton completedBtn = tabBtn("Completed", filterGroup, false);
        filterRow.getChildren().addAll(filterLabel, allBtn, activeBtn, completedBtn);

        VBox projectsList = new VBox(10);

        Runnable refreshList = () -> {
            projectsList.getChildren().clear();
            List<ResearchProject> allProjects = getMyProjects(currentUser);

            // Apply filter
            String filter = "all";
            if (activeBtn.isSelected()) filter = "active";
            else if (completedBtn.isSelected()) filter = "completed";

            String f = filter;
            List<ResearchProject> filtered = allProjects.stream().filter(p -> {
                if (f.equals("all")) return true;
                ResearchProject.ProjectStatus st = p.getStatus();
                if (f.equals("active")) return st == null || st == ResearchProject.ProjectStatus.ACTIVE;
                return st == ResearchProject.ProjectStatus.COMPLETED;
            }).collect(Collectors.toList());

            if (filtered.isEmpty()) {
                Label empty = new Label(allProjects.isEmpty()
                    ? "No projects yet. Create one from 'New Project' tab!"
                    : "No " + f + " projects found.");
                empty.setTextFill(Color.web("#fc8181"));
                empty.setFont(Font.font("System", FontPosture.ITALIC, 13));
                projectsList.getChildren().add(empty);
            } else {
                for (ResearchProject p : filtered) {
                    projectsList.getChildren().add(buildProjectCard(p, currentUser));
                }
            }
        };

        refreshList.run();
        filterGroup.selectedToggleProperty().addListener((obs, o, n) -> refreshList.run());

        pane.getChildren().addAll(heading, filterRow, projectsList);

        ScrollPane scroll = new ScrollPane(pane);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        VBox wrapper = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return wrapper;
    }

    private List<ResearchProject> getMyProjects(User currentUser) {
        List<ResearchProject> owned = new ArrayList<>();
        try {
            if (currentUser instanceof Researcher)
                owned = projectRepository.findByOwner((Researcher) currentUser);
        } catch (Exception ignored) {}

        List<ResearchProject> member = new ArrayList<>();
        try { member = projectRepository.findByMemberId(currentUser.getUserId()); }
        catch (Exception ignored) {}

        List<ResearchProject> all = new ArrayList<>(owned);
        for (ResearchProject p : member) {
            if (all.stream().noneMatch(x -> x.getProjectId().equals(p.getProjectId())))
                all.add(p);
        }
        return all;
    }

    private VBox buildProjectCard(ResearchProject project, User currentUser) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: #1a1f2e; -fx-background-radius: 10px; " +
                      "-fx-border-color: #2d3748; -fx-border-radius: 10px; -fx-cursor: hand;");
        card.setMaxWidth(700);

        card.setOnMouseClicked(e -> {
            rootContent.getChildren().clear();
            rootContent.getChildren().add(buildProjectDetailView(project, currentUser));
        });

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Text projectTitle = new Text(project.getTopic());
        projectTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        projectTitle.setFill(Color.web("#e2e8f0"));

        ResearchProject.ProjectStatus status = project.getStatus();
        String statusStr = status != null ? status.name() : "ACTIVE";
        boolean isActive = status == null || status == ResearchProject.ProjectStatus.ACTIVE;
        String statusColor = isActive ? "#68d391" : "#a0aec0";
        Label statusBadge = new Label(statusStr);
        statusBadge.setStyle("-fx-background-color:" + statusColor + "22;-fx-text-fill:" + statusColor + ";" +
                             "-fx-padding:2 8;-fx-background-radius:8px;-fx-font-size:10px;-fx-font-weight:bold;");
        titleRow.getChildren().addAll(projectTitle, statusBadge);

        if (project.isLookingForCollaborators() && isActive) {
            Label collabBadge = new Label("🤝 Open");
            collabBadge.setStyle("-fx-background-color:#6c9bff22;-fx-text-fill:#6c9bff;" +
                                 "-fx-padding:2 8;-fx-background-radius:8px;-fx-font-size:10px;");
            titleRow.getChildren().add(collabBadge);
        }

        Label domainLabel = new Label("Domain: " + (project.getDomain() != null ? project.getDomain() : "—"));
        domainLabel.setTextFill(Color.web("#8892a4"));
        domainLabel.setFont(Font.font("System", 12));

        String ownerName = project.getOwner() != null ? project.getOwner().getName() : "Unknown";
        boolean isOwner = project.getOwner() != null && project.getOwner().getUserId().equals(currentUser.getUserId());
        Label ownerLabel = new Label("Owner: " + ownerName + (isOwner ? " (You)" : ""));
        ownerLabel.setTextFill(Color.web("#6c9bff"));
        ownerLabel.setFont(Font.font("System", 11));

        // Filter out owner from members list (safety against stale DB data)
        Long ownerIdForFilter = project.getOwner() != null ? project.getOwner().getUserId() : null;
        String memberNames = project.getMembers().stream()
            .filter(m -> ownerIdForFilter == null || !m.getUserId().equals(ownerIdForFilter))
            .map(User::getName).collect(Collectors.joining(", "));
        Label membersLabel = new Label("Team: " + (memberNames.isEmpty() ? ownerName : ownerName + ", " + memberNames));
        membersLabel.setTextFill(Color.web("#8892a4"));
        membersLabel.setFont(Font.font("System", 11));

        card.getChildren().addAll(titleRow, domainLabel, ownerLabel, membersLabel);
        return card;
    }

    // ═══════════════════════════════════════════════════════════
    // Project Detail View
    // ═══════════════════════════════════════════════════════════

    private VBox buildProjectDetailView(ResearchProject project, User currentUser) {
        VBox detail = new VBox(0);
        detail.setStyle("-fx-background-color: #0f1117;");
        boolean isOwner = project.getOwner() != null && project.getOwner().getUserId().equals(currentUser.getUserId());

        Button backBtn = new Button("← Back to My Projects");
        backBtn.setStyle("-fx-background-color:transparent;-fx-text-fill:#6c9bff;-fx-cursor:hand;-fx-font-size:12px;-fx-padding:0 0 12 0;");
        backBtn.setOnAction(e -> {
            rootContent.getChildren().clear();
            rootContent.getChildren().add(buildMainView(currentUser));
        });

        Text titleText = new Text(project.getTopic());
        titleText.setFont(Font.font("Georgia", FontWeight.BOLD, 24));
        titleText.setFill(Color.web("#e2e8f0"));

        HBox metaRow = new HBox(12);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        Label domainBadge = new Label(project.getDomain() != null ? project.getDomain() : "General");
        domainBadge.setStyle("-fx-background-color:#6c9bff22;-fx-text-fill:#6c9bff;-fx-padding:3 10;-fx-background-radius:10px;-fx-font-size:11px;");
        ResearchProject.ProjectStatus st = project.getStatus();
        String stStr = st != null ? st.name() : "ACTIVE";
        Label statusL = new Label(stStr);
        statusL.setStyle("-fx-background-color:#68d39122;-fx-text-fill:#68d391;-fx-padding:3 10;-fx-background-radius:10px;-fx-font-size:11px;");
        Label ownerL = new Label("Owner: " + (project.getOwner() != null ? project.getOwner().getName() : "—"));
        ownerL.setTextFill(Color.web("#8892a4"));
        ownerL.setFont(Font.font("System", 11));
        metaRow.getChildren().addAll(domainBadge, statusL, ownerL);

        Label descL = new Label(project.getDescription() != null && !project.getDescription().isBlank()
            ? project.getDescription() : "No description");
        descL.setTextFill(Color.web("#a0aec0"));
        descL.setFont(Font.font("System", 13));
        descL.setWrapText(true);
        descL.setMaxWidth(700);

        VBox editBox = new VBox();
        if (isOwner) {
            TextArea editDesc = new TextArea(project.getDescription() != null ? project.getDescription() : "");
            editDesc.setPrefRowCount(2);
            editDesc.setWrapText(true);
            editDesc.setStyle("-fx-background-color:#1a1f2e;-fx-text-fill:#e2e8f0;-fx-border-color:#2d3748;-fx-border-radius:6px;-fx-background-radius:6px;");
            editDesc.setMaxWidth(700);
            Button saveDescBtn = new Button("💾 Save Description");
            saveDescBtn.setStyle("-fx-background-color:#6c9bff;-fx-text-fill:white;-fx-background-radius:6px;-fx-cursor:hand;-fx-font-size:11px;-fx-padding:6 14;");
            Label saveStatus = new Label("");
            saveStatus.setFont(Font.font("System", 11));
            saveDescBtn.setOnAction(e -> {
                project.setDescription(editDesc.getText().trim());
                projectRepository.save(project);
                saveStatus.setTextFill(Color.web("#68d391"));
                saveStatus.setText("✓ Saved!");
                descL.setText(editDesc.getText().trim());
            });
            editBox.getChildren().addAll(editDesc, new HBox(8, saveDescBtn, saveStatus));
            editBox.setSpacing(6);
        }

        ToggleGroup subTabs = new ToggleGroup();
        ToggleButton collabTab = tabBtn("👥 Collaborators", subTabs, true);
        ToggleButton updatesTab = tabBtn("📝 Updates & Findings", subTabs, false);
        ToggleButton chatTab = tabBtn("💬 Team Chat", subTabs, false);
        ToggleButton opinionsTab = tabBtn("🌐 Public Opinions", subTabs, false);
        HBox subTabRow = new HBox(4, collabTab, updatesTab, chatTab, opinionsTab);
        subTabRow.setPadding(new Insets(16, 0, 8, 0));

        StackPane subContent = new StackPane();
        VBox.setVgrow(subContent, Priority.ALWAYS);
        subContent.getChildren().add(buildCollaboratorsPane(project, isOwner, currentUser));

        subTabs.selectedToggleProperty().addListener((obs, o, n) -> {
            subContent.getChildren().clear();
            if (n == collabTab) subContent.getChildren().add(buildCollaboratorsPane(project, isOwner, currentUser));
            else if (n == updatesTab) subContent.getChildren().add(buildUpdatesPane(project, currentUser));
            else if (n == chatTab) subContent.getChildren().add(buildChatPane(project, currentUser));
            else if (n == opinionsTab) subContent.getChildren().add(buildOpinionsPane(project, currentUser, isOwner));
        });

        detail.getChildren().addAll(backBtn, titleText, metaRow, descL, editBox, subTabRow, subContent);
        return detail;
    }

    // ── Collaborators ─────────────────────────────────────────
    private VBox buildCollaboratorsPane(ResearchProject project, boolean isOwner, User currentUser) {
        VBox pane = new VBox(10);
        pane.setPadding(new Insets(8, 0, 0, 0));
        Text heading = new Text("Team Members");
        heading.setFont(Font.font("System", FontWeight.BOLD, 16));
        heading.setFill(Color.web("#e2e8f0"));
        pane.getChildren().add(heading);

        if (project.getOwner() != null)
            pane.getChildren().add(memberCard(project.getOwner().getName(), project.getOwner().getEmail(), "OWNER", "#6c9bff"));

        // Filter out owner from members list to avoid duplicate display
        Long ownerIdForDisplay = project.getOwner() != null ? project.getOwner().getUserId() : null;
        List<User> collaborators = project.getMembers().stream()
            .filter(m -> ownerIdForDisplay == null || !m.getUserId().equals(ownerIdForDisplay))
            .collect(Collectors.toList());
        for (User member : collaborators)
            pane.getChildren().add(memberCard(member.getName(), member.getEmail(), "COLLABORATOR", "#68d391"));

        if (collaborators.isEmpty()) {
            Label noMembers = new Label("No collaborators yet.");
            noMembers.setTextFill(Color.web("#4a5568"));
            noMembers.setFont(Font.font("System", FontPosture.ITALIC, 12));
            pane.getChildren().add(noMembers);
        }

        // Owner actions: complete project + upload paper
        if (isOwner) {
            HBox actions = new HBox(8);
            actions.setPadding(new Insets(12, 0, 0, 0));
            ResearchProject.ProjectStatus st = project.getStatus();
            if (st == null || st == ResearchProject.ProjectStatus.ACTIVE) {
                Button completeBtn = new Button("✓ Complete & Upload Paper");
                completeBtn.setStyle("-fx-background-color:#68d391;-fx-text-fill:white;-fx-background-radius:6px;-fx-cursor:hand;-fx-font-size:12px;-fx-padding:8 18;-fx-font-weight:bold;");
                completeBtn.setOnAction(e -> showCompletionDialog(project, currentUser));
                actions.getChildren().add(completeBtn);
            } else {
                Label doneLabel = new Label("✓ Project Completed");
                doneLabel.setStyle("-fx-background-color:#68d39122;-fx-text-fill:#68d391;-fx-padding:8 18;-fx-background-radius:6px;-fx-font-weight:bold;");
                actions.getChildren().add(doneLabel);
            }
            pane.getChildren().add(actions);
        }
        return pane;
    }

    private void showCompletionDialog(ResearchProject project, User currentUser) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Complete Project & Publish Paper");
        dialog.setHeaderText("Upload the research paper for: " + project.getTopic());

        VBox dContent = new VBox(12);
        dContent.setPadding(new Insets(16));
        dContent.setMinWidth(450);

        TextField paperTitle = new TextField(project.getTopic());
        paperTitle.setPromptText("Paper title");
        TextArea abstractText = new TextArea();
        abstractText.setPromptText("Abstract / summary of the paper...");
        abstractText.setPrefRowCount(3);
        abstractText.setWrapText(true);
        TextField keywords = new TextField();
        keywords.setPromptText("Keywords (comma-separated)");

        Label fileLabel = new Label("No file selected");
        fileLabel.setTextFill(Color.web("#8892a4"));
        final File[] selectedFile = {null};
        Button chooseFile = new Button("📄 Choose PDF File");
        chooseFile.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Research Paper PDF");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            File f = fc.showOpenDialog(null);
            if (f != null) {
                selectedFile[0] = f;
                fileLabel.setText("Selected: " + f.getName());
                fileLabel.setTextFill(Color.web("#68d391"));
            }
        });

        Label statusL = new Label("");
        dContent.getChildren().addAll(
            new Label("Paper Title:"), paperTitle,
            new Label("Abstract:"), abstractText,
            new Label("Keywords:"), keywords,
            new HBox(8, chooseFile, fileLabel), statusL);

        dialog.getDialogPane().setContent(dContent);
        ButtonType publishBtn = new ButtonType("Publish & Complete", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(publishBtn, ButtonType.CANCEL);

        dialog.setResultConverter(bt -> {
            if (bt == publishBtn) {
                try {
                    // Create the research paper
                    ResearchPaper paper = new ResearchPaper();
                    paper.setTitle(paperTitle.getText().trim());
                    paper.setAbstractText(abstractText.getText().trim());
                    paper.setKeywords(keywords.getText().trim());
                    paper.setDomain(project.getDomain());
                    paper.setStatus(ResearchPaper.PaperStatus.PUBLISHED);
                    paper.setLink(selectedFile[0] != null ? selectedFile[0].getAbsolutePath() : "");

                    // Set authors: owner + all collaborators
                    String authors = project.getOwner() != null ? project.getOwner().getName() : "";
                    String collabNames = project.getMembers().stream()
                            .map(User::getName).collect(Collectors.joining(", "));
                    if (!collabNames.isEmpty()) authors += ", " + collabNames;
                    paper.setAuthor(authors);

                    // Link to owner as researcher
                    if (currentUser instanceof Researcher) paper.setResearcher((Researcher) currentUser);

                    paperRepository.save(paper);

                    // Mark project complete
                    project.setStatus(ResearchProject.ProjectStatus.COMPLETED);
                    project.setLookingForCollaborators(false);
                    projectRepository.save(project);

                    // Refresh the view
                    rootContent.getChildren().clear();
                    rootContent.getChildren().add(buildProjectDetailView(project, currentUser));
                } catch (Exception ex) {
                    Alert err = new Alert(Alert.AlertType.ERROR, "Error: " + ex.getMessage());
                    err.showAndWait();
                }
            }
            return null;
        });
        dialog.showAndWait();
    }

    private HBox memberCard(String name, String email, String role, String color) {
        HBox card = new HBox(12);
        card.setPadding(new Insets(10, 14, 10, 14));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(500);
        card.setStyle("-fx-background-color:#1a1f2e;-fx-background-radius:8px;-fx-border-color:#2d3748;-fx-border-radius:8px;");
        Label icon = new Label("👤");
        icon.setFont(Font.font("System", 18));
        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label nameL = new Label(name);
        nameL.setFont(Font.font("System", FontWeight.BOLD, 13));
        nameL.setTextFill(Color.web("#e2e8f0"));
        Label emailL = new Label(email);
        emailL.setFont(Font.font("System", 10));
        emailL.setTextFill(Color.web("#8892a4"));
        info.getChildren().addAll(nameL, emailL);
        Label roleBadge = new Label(role);
        roleBadge.setStyle("-fx-background-color:" + color + "22;-fx-text-fill:" + color + ";-fx-padding:3 10;-fx-background-radius:10px;-fx-font-size:10px;-fx-font-weight:bold;");
        card.getChildren().addAll(icon, info, roleBadge);
        return card;
    }

    // ── Updates & Findings ────────────────────────────────────
    private VBox buildUpdatesPane(ResearchProject project, User currentUser) {
        VBox pane = new VBox(12);
        pane.setPadding(new Insets(8, 0, 0, 0));
        Text heading = new Text("Updates & Findings");
        heading.setFont(Font.font("System", FontWeight.BOLD, 16));
        heading.setFill(Color.web("#e2e8f0"));

        VBox postBox = new VBox(8);
        postBox.setPadding(new Insets(12));
        postBox.setStyle("-fx-background-color:#1a1f2e;-fx-background-radius:8px;");
        postBox.setMaxWidth(700);

        TextField updateTitle = new TextField();
        updateTitle.setPromptText("Update title");
        updateTitle.setStyle("-fx-background-color:#0f1117;-fx-text-fill:#e2e8f0;-fx-prompt-text-fill:#4a5568;-fx-border-color:#2d3748;-fx-border-radius:6px;-fx-background-radius:6px;-fx-pref-height:36px;-fx-padding:0 12;");
        TextArea updateContent = new TextArea();
        updateContent.setPromptText("Describe your findings...");
        updateContent.setPrefRowCount(3);
        updateContent.setWrapText(true);
        updateContent.setStyle("-fx-background-color:#0f1117;-fx-text-fill:#e2e8f0;-fx-prompt-text-fill:#4a5568;-fx-border-color:#2d3748;-fx-border-radius:6px;-fx-background-radius:6px;");
        Label postStatus = new Label("");
        postStatus.setFont(Font.font("System", 11));
        Button postBtn = new Button("📝 Post Update");
        postBtn.setStyle("-fx-background-color:#6c9bff;-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:6px;-fx-cursor:hand;-fx-padding:6 16;");

        VBox updatesList = new VBox(8);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

        Runnable refresh = () -> {
            updatesList.getChildren().clear();
            List<ProjectUpdate> updates = updateRepository.findByProjectOrderByPostedAtDesc(project);
            if (updates.isEmpty()) {
                Label noUpdates = new Label("No updates posted yet.");
                noUpdates.setTextFill(Color.web("#4a5568"));
                updatesList.getChildren().add(noUpdates);
            } else {
                for (ProjectUpdate u : updates) {
                    VBox c = new VBox(4);
                    c.setPadding(new Insets(10, 14, 10, 14));
                    c.setMaxWidth(700);
                    c.setStyle("-fx-background-color:#1a1f2e;-fx-background-radius:8px;");
                    Label t = new Label("📌 " + u.getTitle());
                    t.setFont(Font.font("System", FontWeight.BOLD, 13));
                    t.setTextFill(Color.web("#e2e8f0"));
                    Label ct = new Label(u.getContent());
                    ct.setTextFill(Color.web("#a0aec0"));
                    ct.setWrapText(true);
                    ct.setMaxWidth(670);
                    Label meta = new Label("By " + u.getAuthor().getName() + " · " +
                        (u.getPostedAt() != null ? u.getPostedAt().format(fmt) : ""));
                    meta.setTextFill(Color.web("#4a5568"));
                    meta.setFont(Font.font("System", 10));
                    c.getChildren().addAll(t, ct, meta);
                    updatesList.getChildren().add(c);
                }
            }
        };
        refresh.run();

        postBtn.setOnAction(e -> {
            if (updateTitle.getText().trim().isEmpty()) { postStatus.setTextFill(Color.web("#fc8181")); postStatus.setText("Enter a title."); return; }
            ProjectUpdate u = new ProjectUpdate();
            u.setProject(project);
            u.setAuthor(currentUser);
            u.setTitle(updateTitle.getText().trim());
            u.setContent(updateContent.getText().trim());
            updateRepository.save(u);
            updateTitle.clear(); updateContent.clear();
            postStatus.setTextFill(Color.web("#68d391")); postStatus.setText("✓ Posted!");
            refresh.run();
        });

        postBox.getChildren().addAll(updateTitle, updateContent, new HBox(8, postBtn, postStatus));
        pane.getChildren().addAll(heading, postBox, updatesList);
        ScrollPane scroll = new ScrollPane(pane);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        VBox w = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return w;
    }

    // ── Team Chat ─────────────────────────────────────────────
    private VBox buildChatPane(ResearchProject project, User currentUser) {
        VBox pane = new VBox(10);
        pane.setPadding(new Insets(8, 0, 0, 0));
        Text heading = new Text("Team Chat");
        heading.setFont(Font.font("System", FontWeight.BOLD, 16));
        heading.setFill(Color.web("#e2e8f0"));

        VBox messagesBox = new VBox(6);
        messagesBox.setPadding(new Insets(12));
        messagesBox.setStyle("-fx-background-color:#1a1f2e;-fx-background-radius:8px;");
        messagesBox.setMaxWidth(700);
        messagesBox.setMinHeight(300);
        ScrollPane chatScroll = new ScrollPane(messagesBox);
        chatScroll.setFitToWidth(true);
        chatScroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        chatScroll.setPrefHeight(350);
        VBox.setVgrow(chatScroll, Priority.ALWAYS);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
        Runnable refreshChat = () -> {
            messagesBox.getChildren().clear();
            List<ProjectMessage> messages = messageRepository.findByProjectOrderBySentAtAsc(project);
            if (messages.isEmpty()) {
                Label noMsg = new Label("No messages yet. Start the conversation!");
                noMsg.setTextFill(Color.web("#4a5568"));
                messagesBox.getChildren().add(noMsg);
            } else {
                for (ProjectMessage msg : messages) {
                    boolean isMe = msg.getSender().getUserId().equals(currentUser.getUserId());
                    HBox row = new HBox(8);
                    row.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                    row.setMaxWidth(700);
                    VBox bubble = new VBox(2);
                    bubble.setMaxWidth(450);
                    bubble.setPadding(new Insets(8, 12, 8, 12));
                    bubble.setStyle("-fx-background-color:" + (isMe ? "#6c9bff33" : "#2d3748") + ";-fx-background-radius:12px;");
                    Label senderL = new Label(isMe ? "You" : msg.getSender().getName());
                    senderL.setFont(Font.font("System", FontWeight.BOLD, 10));
                    senderL.setTextFill(Color.web(isMe ? "#6c9bff" : "#68d391"));
                    Label contentL = new Label(msg.getContent());
                    contentL.setTextFill(Color.web("#e2e8f0"));
                    contentL.setWrapText(true);
                    Label timeL = new Label(msg.getSentAt() != null ? msg.getSentAt().format(fmt) : "");
                    timeL.setTextFill(Color.web("#4a5568"));
                    timeL.setFont(Font.font("System", 9));
                    bubble.getChildren().addAll(senderL, contentL, timeL);
                    row.getChildren().add(bubble);
                    messagesBox.getChildren().add(row);
                }
            }
            chatScroll.layout();
            chatScroll.setVvalue(1.0);
        };
        refreshChat.run();

        HBox inputRow = new HBox(8);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        inputRow.setPadding(new Insets(8, 0, 0, 0));
        TextField msgField = new TextField();
        msgField.setPromptText("Type a message...");
        msgField.setStyle("-fx-background-color:#1a1f2e;-fx-text-fill:#e2e8f0;-fx-prompt-text-fill:#4a5568;-fx-border-color:#2d3748;-fx-border-radius:20px;-fx-background-radius:20px;-fx-pref-height:40px;-fx-padding:0 16;");
        HBox.setHgrow(msgField, Priority.ALWAYS);
        Button sendBtn = new Button("Send ▸");
        sendBtn.setStyle("-fx-background-color:#6c9bff;-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:20px;-fx-cursor:hand;-fx-pref-height:40px;-fx-padding:0 20;");
        Runnable send = () -> {
            if (msgField.getText().trim().isEmpty()) return;
            ProjectMessage msg = new ProjectMessage();
            msg.setProject(project);
            msg.setSender(currentUser);
            msg.setContent(msgField.getText().trim());
            messageRepository.save(msg);
            msgField.clear();
            refreshChat.run();
        };
        sendBtn.setOnAction(e -> send.run());
        msgField.setOnAction(e -> send.run());
        Button refreshBtn = new Button("🔄");
        refreshBtn.setStyle("-fx-background-color:#2d3748;-fx-text-fill:#a0aec0;-fx-background-radius:20px;-fx-cursor:hand;-fx-pref-height:40px;-fx-pref-width:40px;");
        refreshBtn.setOnAction(e -> refreshChat.run());
        inputRow.getChildren().addAll(msgField, sendBtn, refreshBtn);

        pane.getChildren().addAll(heading, chatScroll, inputRow);
        return pane;
    }

    // ── Public Opinions ───────────────────────────────────────
    private VBox buildOpinionsPane(ResearchProject project, User currentUser, boolean isOwner) {
        VBox pane = new VBox(10);
        pane.setPadding(new Insets(8, 0, 0, 0));
        Text heading = new Text("Public Opinions");
        heading.setFont(Font.font("System", FontWeight.BOLD, 16));
        heading.setFill(Color.web("#e2e8f0"));
        Label desc = new Label("Feedback and suggestions from the research community.");
        desc.setTextFill(Color.web("#8892a4"));
        desc.setFont(Font.font("System", 11));

        VBox opinionsList = new VBox(8);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

        Runnable refresh = () -> {
            opinionsList.getChildren().clear();
            List<PublicOpinion> opinions = opinionRepository.findByProjectOrderByPostedAtDesc(project);
            if (opinions.isEmpty()) {
                Label none = new Label("No public opinions yet.");
                none.setTextFill(Color.web("#4a5568"));
                opinionsList.getChildren().add(none);
            } else {
                for (PublicOpinion op : opinions) {
                    VBox card = new VBox(4);
                    card.setPadding(new Insets(10, 14, 10, 14));
                    card.setMaxWidth(700);
                    card.setStyle("-fx-background-color:#1a1f2e;-fx-background-radius:8px;");
                    Label content = new Label(op.getContent());
                    content.setTextFill(Color.web("#a0aec0"));
                    content.setWrapText(true);
                    content.setMaxWidth(670);
                    Label meta = new Label("— " + op.getAuthor().getName() + " · " +
                        (op.getPostedAt() != null ? op.getPostedAt().format(fmt) : ""));
                    meta.setTextFill(Color.web("#4a5568"));
                    meta.setFont(Font.font("System", 10));
                    card.getChildren().addAll(content, meta);
                    opinionsList.getChildren().add(card);
                }
            }
        };
        refresh.run();

        pane.getChildren().addAll(heading, desc, opinionsList);
        ScrollPane scroll = new ScrollPane(pane);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        VBox w = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return w;
    }

    // ═══════════════════════════════════════════════════════════
    // Discover & Follow
    // ═══════════════════════════════════════════════════════════

    private VBox buildDiscoverPane(User currentUser) {
        VBox pane = new VBox(12);
        pane.setAlignment(Pos.TOP_LEFT);

        Text heading = new Text("Discover & Follow Researches");
        heading.setFont(Font.font("Georgia", FontWeight.BOLD, 20));
        heading.setFill(Color.web("#e2e8f0"));
        Label desc = new Label("Browse public research projects, read updates, and share your opinions.");
        desc.setTextFill(Color.web("#8892a4"));
        desc.setFont(Font.font("System", 12));

        // Domain filter
        HBox filterRow = new HBox(8);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        Label filterLabel = new Label("Filter by domain:");
        filterLabel.setTextFill(Color.web("#8892a4"));
        ComboBox<String> domainFilter = new ComboBox<>();
        domainFilter.getItems().add("All Domains");
        try {
            domainFilter.getItems().addAll(projectRepository.findAllDomains());
        } catch (Exception ignored) {}
        domainFilter.setValue("All Domains");
        domainFilter.setStyle("-fx-background-color:#1a1f2e;-fx-border-color:#2d3748;-fx-border-radius:6px;");
        filterRow.getChildren().addAll(filterLabel, domainFilter);

        VBox projectCards = new VBox(10);
        ScrollPane scroll = new ScrollPane(projectCards);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // Get my project IDs to exclude
        List<Long> myProjectIds = getMyProjects(currentUser).stream()
            .map(ResearchProject::getProjectId).collect(Collectors.toList());

        Runnable refreshDiscover = () -> {
            projectCards.getChildren().clear();
            List<ResearchProject> allPublic;
            String domain = domainFilter.getValue();
            if (domain == null || domain.equals("All Domains")) {
                allPublic = projectRepository.findByLookingForCollaboratorsTrue();
            } else {
                allPublic = projectRepository.findByLookingForCollaboratorsTrueAndDomainIn(List.of(domain));
            }
            // Also add completed public projects
            try {
                for (ResearchProject p : projectRepository.findByStatus(ResearchProject.ProjectStatus.COMPLETED)) {
                    if (allPublic.stream().noneMatch(x -> x.getProjectId().equals(p.getProjectId())))
                        allPublic.add(p);
                }
            } catch (Exception ignored) {}

            // Filter out own projects
            allPublic = allPublic.stream()
                .filter(p -> !myProjectIds.contains(p.getProjectId()))
                .collect(Collectors.toList());

            if (allPublic.isEmpty()) {
                Label none = new Label("No public projects to discover right now.");
                none.setTextFill(Color.web("#4a5568"));
                projectCards.getChildren().add(none);
            } else {
                for (ResearchProject p : allPublic) {
                    projectCards.getChildren().add(buildDiscoverCard(p, currentUser));
                }
            }
        };

        refreshDiscover.run();
        domainFilter.setOnAction(e -> refreshDiscover.run());

        pane.getChildren().addAll(heading, desc, filterRow, scroll);
        return pane;
    }

    private VBox buildDiscoverCard(ResearchProject project, User currentUser) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color:#1a1f2e;-fx-background-radius:10px;-fx-border-color:#2d3748;-fx-border-radius:10px;");
        card.setMaxWidth(700);

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Text title = new Text(project.getTopic());
        title.setFont(Font.font("System", FontWeight.BOLD, 15));
        title.setFill(Color.web("#e2e8f0"));
        Label domain = new Label(project.getDomain() != null ? project.getDomain() : "General");
        domain.setStyle("-fx-background-color:#6c9bff22;-fx-text-fill:#6c9bff;-fx-padding:2 8;-fx-background-radius:8px;-fx-font-size:10px;");
        ResearchProject.ProjectStatus st = project.getStatus();
        Label statusL = new Label(st != null ? st.name() : "ACTIVE");
        String stColor = (st == null || st == ResearchProject.ProjectStatus.ACTIVE) ? "#68d391" : "#a0aec0";
        statusL.setStyle("-fx-background-color:" + stColor + "22;-fx-text-fill:" + stColor + ";-fx-padding:2 8;-fx-background-radius:8px;-fx-font-size:10px;");
        titleRow.getChildren().addAll(title, domain, statusL);

        Label descLabel = new Label(project.getDescription() != null && !project.getDescription().isBlank()
            ? project.getDescription() : "No description");
        descLabel.setTextFill(Color.web("#a0aec0"));
        descLabel.setFont(Font.font("System", 12));
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(670);

        String ownerName = project.getOwner() != null ? project.getOwner().getName() : "Unknown";
        Label ownerL = new Label("By " + ownerName);
        ownerL.setTextFill(Color.web("#6c9bff"));
        ownerL.setFont(Font.font("System", 11));

        // Updates preview
        List<ProjectUpdate> updates = new ArrayList<>();
        try { updates = updateRepository.findByProjectOrderByPostedAtDesc(project); } catch (Exception ignored) {}

        VBox updatesPreview = new VBox(4);
        if (!updates.isEmpty()) {
            Label updatesHeader = new Label("📝 Latest Updates (" + updates.size() + "):");
            updatesHeader.setTextFill(Color.web("#8892a4"));
            updatesHeader.setFont(Font.font("System", FontWeight.BOLD, 11));
            updatesPreview.getChildren().add(updatesHeader);
            int shown = 0;
            for (ProjectUpdate u : updates) {
                if (shown >= 2) break;
                Label uLabel = new Label("  • " + u.getTitle());
                uLabel.setTextFill(Color.web("#718096"));
                uLabel.setFont(Font.font("System", 11));
                updatesPreview.getChildren().add(uLabel);
                shown++;
            }
        }

        // Opinion input
        HBox opinionRow = new HBox(8);
        opinionRow.setAlignment(Pos.CENTER_LEFT);
        TextField opinionField = new TextField();
        opinionField.setPromptText("Share your opinion on this research...");
        opinionField.setStyle("-fx-background-color:#0f1117;-fx-text-fill:#e2e8f0;-fx-prompt-text-fill:#4a5568;-fx-border-color:#2d3748;-fx-border-radius:6px;-fx-background-radius:6px;-fx-pref-height:36px;-fx-padding:0 12;");
        HBox.setHgrow(opinionField, Priority.ALWAYS);
        Label opStatus = new Label("");
        opStatus.setFont(Font.font("System", 10));
        Button submitOp = new Button("💬 Post");
        submitOp.setStyle("-fx-background-color:#6c9bff;-fx-text-fill:white;-fx-background-radius:6px;-fx-cursor:hand;-fx-padding:6 14;-fx-font-size:11px;");
        submitOp.setOnAction(e -> {
            if (opinionField.getText().trim().isEmpty()) return;
            PublicOpinion op = new PublicOpinion();
            op.setProject(project);
            op.setAuthor(currentUser);
            op.setContent(opinionField.getText().trim());
            opinionRepository.save(op);
            opinionField.clear();
            opStatus.setTextFill(Color.web("#68d391"));
            opStatus.setText("✓ Opinion posted!");
        });
        opinionRow.getChildren().addAll(opinionField, submitOp, opStatus);

        card.getChildren().addAll(titleRow, descLabel, ownerL, updatesPreview, opinionRow);
        return card;
    }

    // ═══════════════════════════════════════════════════════════
    // Create Project
    // ═══════════════════════════════════════════════════════════

    private VBox buildCreateProjectPane(User currentUser) {
        VBox pane = new VBox(16);
        pane.setMaxWidth(600);
        pane.setPadding(new Insets(16));
        pane.setStyle("-fx-background-color:#1a1f2e;-fx-background-radius:12px;");
        Text heading = new Text("Create New Research Project");
        heading.setFont(Font.font("Georgia", FontWeight.BOLD, 20));
        heading.setFill(Color.web("#e2e8f0"));

        TextField topicField = new TextField();
        topicField.setPromptText("e.g. 'AI-Driven Drug Discovery'");
        styleField(topicField);
        TextArea descArea = new TextArea();
        descArea.setPromptText("Describe your research goals...");
        descArea.setPrefRowCount(4);
        descArea.setStyle("-fx-background-color:#0f1117;-fx-text-fill:#e2e8f0;-fx-prompt-text-fill:#4a5568;-fx-border-color:#2d3748;-fx-border-radius:6px;-fx-background-radius:6px;");
        ComboBox<String> domainCombo = new ComboBox<>();
        domainCombo.getItems().addAll(AVAILABLE_DOMAINS);
        domainCombo.setPromptText("Select domain");
        domainCombo.setStyle("-fx-background-color:#0f1117;-fx-border-color:#2d3748;-fx-border-radius:6px;-fx-pref-width:340px;");

        ToggleGroup modeGroup = new ToggleGroup();
        RadioButton soloRadio = new RadioButton("Working Alone");
        soloRadio.setToggleGroup(modeGroup);
        soloRadio.setSelected(true);
        soloRadio.setTextFill(Color.web("#a0aec0"));
        RadioButton collabRadio = new RadioButton("Looking for Collaborators");
        collabRadio.setToggleGroup(modeGroup);
        collabRadio.setTextFill(Color.web("#a0aec0"));

        Label statusLabel = new Label("");
        Button createBtn = new Button("Create Project");
        createBtn.setStyle("-fx-background-color:#6c9bff;-fx-text-fill:white;-fx-font-size:14px;-fx-font-weight:bold;-fx-pref-width:260px;-fx-pref-height:44px;-fx-background-radius:8px;-fx-cursor:hand;");
        createBtn.setOnAction(e -> {
            if (topicField.getText().trim().isEmpty()) { statusLabel.setTextFill(Color.web("#fc8181")); statusLabel.setText("Enter a topic."); return; }
            if (domainCombo.getValue() == null) { statusLabel.setTextFill(Color.web("#fc8181")); statusLabel.setText("Select a domain."); return; }
            ResearchProject p = new ResearchProject();
            p.setTopic(topicField.getText().trim());
            p.setDescription(descArea.getText().trim());
            p.setDomain(domainCombo.getValue());
            p.setStatus(ResearchProject.ProjectStatus.ACTIVE);
            p.setLookingForCollaborators(collabRadio.isSelected());
            if (currentUser instanceof Researcher) p.setOwner((Researcher) currentUser);
            projectRepository.save(p);
            statusLabel.setTextFill(Color.web("#68d391"));
            statusLabel.setText("✓ Project created!");
            topicField.clear(); descArea.clear(); domainCombo.setValue(null);
        });

        pane.getChildren().addAll(heading,
            lbl("Topic *"), topicField, lbl("Description"), descArea,
            lbl("Domain *"), domainCombo, lbl("Mode"), soloRadio, collabRadio,
            createBtn, statusLabel);
        return pane;
    }

    // ═══════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════

    private Label lbl(String text) {
        Label l = new Label(text);
        l.setTextFill(Color.web("#8892a4"));
        l.setFont(Font.font("System", FontWeight.BOLD, 12));
        return l;
    }

    private void styleField(TextField field) {
        field.setStyle("-fx-background-color:#0f1117;-fx-text-fill:#e2e8f0;-fx-prompt-text-fill:#4a5568;-fx-border-color:#2d3748;-fx-border-radius:6px;-fx-background-radius:6px;-fx-pref-width:340px;-fx-pref-height:40px;-fx-padding:0 12px;");
    }

    private ToggleButton tabBtn(String text, ToggleGroup group, boolean selected) {
        ToggleButton btn = new ToggleButton(text);
        btn.setToggleGroup(group);
        btn.setSelected(selected);
        btn.setPadding(new Insets(8, 18, 8, 18));
        String base = selected
            ? "-fx-background-color:#6c9bff;-fx-text-fill:white;-fx-font-weight:bold;"
            : "-fx-background-color:#1a1f2e;-fx-text-fill:#8892a4;";
        btn.setStyle(base + "-fx-background-radius:8px;-fx-cursor:hand;-fx-font-size:12px;-fx-border-color:#2d3748;-fx-border-radius:8px;");
        btn.selectedProperty().addListener((obs, was, is) -> {
            String s = is ? "-fx-background-color:#6c9bff;-fx-text-fill:white;-fx-font-weight:bold;"
                : "-fx-background-color:#1a1f2e;-fx-text-fill:#8892a4;";
            btn.setStyle(s + "-fx-background-radius:8px;-fx-cursor:hand;-fx-font-size:12px;-fx-border-color:#2d3748;-fx-border-radius:8px;");
        });
        return btn;
    }
}
