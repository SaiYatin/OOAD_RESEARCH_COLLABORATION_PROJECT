package com.research.view.collab;

import com.research.model.*;
import com.research.controller.CollaborationController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CollaborationView — View layer for Collaboration & Project Management.
 *
 * @author Member 3
 * @usecase Collaboration Requests, Project Management & Team Communication
 *
 * Tabs:
 *   1. Inbox            — incoming requests (grouped by project)
 *   2. Sent             — sent requests with status
 *   3. Following        — interest-based research feed + domain selection
 *   4. Active Collabs   — browse open projects, toggle all/filtered by interests
 *
 * Design Pattern demonstrated: Observer (publication notifications via n8n)
 * Design Principle: DIP (depends on controller abstraction, not concrete repositories)
 *
 * MVC Role: View — delegates all business logic to CollaborationController
 */
@Component
public class CollaborationView {

    private final CollaborationController collaborationController;

    private static final String[] AVAILABLE_DOMAINS = {
        "Machine Learning", "Deep Learning", "Natural Language Processing",
        "Computer Vision", "Data Science", "Signal Processing",
        "Robotics", "IoT", "Network Security", "Biotechnology",
        "Economics", "Composite Materials", "Heat Transfer",
        "Earthquake Engineering", "Mathematics", "Other"
    };

    public CollaborationView(CollaborationController collaborationController) {
        this.collaborationController = collaborationController;
    }

    public VBox buildPanel(User currentUser) {
        VBox panel = new VBox(0);
        panel.setStyle("-fx-background-color: #0f1117;");

        VBox header = new VBox(4);
        header.setPadding(new Insets(0, 0, 16, 0));

        long pendingCount = 0;
        try {
            pendingCount = collaborationController
                .getInboxRequests().size();
        } catch (Exception ignored) {}

        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Text title = new Text("Collaborations");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 26));
        title.setFill(Color.web("#e2e8f0"));
        if (pendingCount > 0) {
            Label badge = new Label(pendingCount + " pending");
            badge.setStyle("-fx-background-color:#fc8181;-fx-text-fill:white;-fx-font-size:11px;" +
                           "-fx-padding:3 10;-fx-background-radius:12px;-fx-font-weight:bold;");
            titleRow.getChildren().addAll(title, badge);
        } else {
            titleRow.getChildren().add(title);
        }

        Text subtitle = new Text("Requests · Follow Interests · Active Collaborations");
        subtitle.setFont(Font.font("System", 13));
        subtitle.setFill(Color.web("#8892a4"));
        header.getChildren().addAll(titleRow, subtitle);

        // Tabs — removed Projects (→ My Researches) and New Request (→ Find Experts)
        ToggleGroup tabGroup = new ToggleGroup();
        ToggleButton inboxTab   = tabBtn("📥 Inbox" + (pendingCount > 0 ? " (" + pendingCount + ")" : ""), tabGroup, true);
        ToggleButton sentTab    = tabBtn("📤 Sent", tabGroup, false);
        ToggleButton followTab  = tabBtn("🔔 Following", tabGroup, false);
        ToggleButton activeTab  = tabBtn("🤝 Active Collaborations", tabGroup, false);
        HBox tabRow = new HBox(4, inboxTab, sentTab, followTab, activeTab);
        tabRow.setPadding(new Insets(0, 0, 16, 0));

        StackPane content = new StackPane();
        VBox.setVgrow(content, Priority.ALWAYS);
        content.getChildren().add(buildInboxPane(currentUser));

        tabGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            content.getChildren().clear();
            if (n == inboxTab)      content.getChildren().add(buildInboxPane(currentUser));
            else if (n == sentTab)  content.getChildren().add(buildSentPane(currentUser));
            else if (n == followTab) content.getChildren().add(buildFollowPane(currentUser));
            else if (n == activeTab) content.getChildren().add(buildActiveCollabsPane(currentUser));
        });

        panel.getChildren().addAll(header, tabRow, content);
        return panel;
    }

    // ══ TAB 1: Inbox ════════════════════════════════════════════════

    private VBox buildInboxPane(User user) {
        VBox pane = new VBox(12);

        Text heading = sectionHeading("Incoming Requests");
        Label desc = smallNote("Collaboration requests sent to you. Accept to join their project.");
        pane.getChildren().addAll(heading, desc);

        List<CollaborationRequest> pending;
        try {
            pending = collaborationController.getInboxRequests();
        } catch (Exception e) {
            pane.getChildren().add(emptyLabel("Could not load requests: " + e.getMessage()));
            return pane;
        }

        if (pending.isEmpty()) {
            pane.getChildren().add(emptyLabel("No pending requests. You're all caught up!"));
            return pane;
        }

        // Group by project
        var byProject = pending.stream().collect(Collectors.groupingBy(
            req -> req.getProject() != null ? req.getProject().getTopic() : "General"));

        ScrollPane scroll = new ScrollPane();
        VBox cards = new VBox(10);
        scroll.setContent(cards); scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        for (var entry : byProject.entrySet()) {
            // Project group header
            Label groupLabel = new Label("📂 " + entry.getKey());
            groupLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
            groupLabel.setTextFill(Color.web("#6c9bff"));
            groupLabel.setPadding(new Insets(8, 0, 4, 0));
            cards.getChildren().add(groupLabel);

            for (CollaborationRequest req : entry.getValue()) {
                Label actionStatus = new Label("");
                actionStatus.setFont(Font.font("System", 11));
                VBox card = requestCard(req, true, actionStatus);

                Button acceptBtn = new Button("✓ Accept");
                acceptBtn.setStyle("-fx-background-color:#68d391;-fx-text-fill:#1a1f2e;-fx-font-weight:bold;" +
                                   "-fx-background-radius:6px;-fx-pref-height:34px;-fx-pref-width:100px;-fx-cursor:hand;");
                Button rejectBtn = new Button("✕ Reject");
                rejectBtn.setStyle("-fx-background-color:#fc8181;-fx-text-fill:white;-fx-font-weight:bold;" +
                                   "-fx-background-radius:6px;-fx-pref-height:34px;-fx-pref-width:100px;-fx-cursor:hand;");
                HBox btnRow = new HBox(10, acceptBtn, rejectBtn, actionStatus);
                btnRow.setAlignment(Pos.CENTER_LEFT);

                acceptBtn.setOnAction(e -> {
                    try {
                        collaborationController.acceptRequest(req.getRequestId());
                        status(actionStatus, "✓ Accepted! Sender added to project.", true);
                        acceptBtn.setDisable(true); rejectBtn.setDisable(true);
                    } catch (Exception ex) { status(actionStatus, ex.getMessage(), false); }
                });
                rejectBtn.setOnAction(e -> {
                    try {
                        collaborationController.declineRequest(req.getRequestId());
                        status(actionStatus, "Request rejected.", false);
                        acceptBtn.setDisable(true); rejectBtn.setDisable(true);
                    } catch (Exception ex) { status(actionStatus, ex.getMessage(), false); }
                });

                card.getChildren().add(btnRow);
                cards.getChildren().add(card);
            }
        }

        pane.getChildren().add(scroll);
        return pane;
    }

    // ══ TAB 2: Sent ═════════════════════════════════════════════════

    private VBox buildSentPane(User user) {
        VBox pane = new VBox(12);
        pane.getChildren().addAll(sectionHeading("Sent Requests"),
            smallNote("Track the status of requests you've sent."));

        List<CollaborationRequest> sent;
        try { sent = collaborationController.getSentRequests(); }
        catch (Exception e) { pane.getChildren().add(emptyLabel(e.getMessage())); return pane; }

        if (sent.isEmpty()) {
            pane.getChildren().add(emptyLabel("You haven't sent any requests yet. Use 'Find Experts' to discover collaborators."));
            return pane;
        }

        ScrollPane scroll = new ScrollPane();
        VBox cards = new VBox(10);
        scroll.setContent(cards); scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        for (CollaborationRequest req : sent) {
            Label dummy = new Label();
            cards.getChildren().add(requestCard(req, false, dummy));
        }
        pane.getChildren().add(scroll);
        return pane;
    }

    // ══ TAB 3: Following (Interests + Feed) ═════════════════════════

    private VBox buildFollowPane(User user) {
        VBox pane = new VBox(16);

        pane.getChildren().addAll(
            sectionHeading("Research Interests & Feed"),
            smallNote("Select domains you're interested in. You'll see papers matching your interests, " +
                      "and get email alerts via the Observer pattern when new papers are published.")
        );

        // Check if user has set interests
        List<String> currentInterests = new ArrayList<>();
        Researcher researcher = null;
        if (user instanceof Researcher r) {
            researcher = r;
            currentInterests = new ArrayList<>(r.getInterestedDomains());
        }

        boolean firstTime = currentInterests.isEmpty();

        // Domain selector
        VBox selectorBox = new VBox(10);
        selectorBox.setPadding(new Insets(16));
        selectorBox.setStyle("-fx-background-color:#1a1f2e;-fx-background-radius:10px;");
        selectorBox.setMaxWidth(600);

        Text selectorTitle = new Text(firstTime ? "🎯 Select Your Research Interests" : "📝 Your Research Interests");
        selectorTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        selectorTitle.setFill(Color.web("#6c9bff"));

        if (firstTime) {
            Label prompt = new Label("This is your first time! Pick domains you're interested in to customize your feed.");
            prompt.setTextFill(Color.web("#ecc94b"));
            prompt.setFont(Font.font("System", 12));
            prompt.setWrapText(true);
            selectorBox.getChildren().addAll(selectorTitle, prompt);
        } else {
            selectorBox.getChildren().add(selectorTitle);
        }

        // Checkboxes for domains
        FlowPane domainGrid = new FlowPane(8, 8);
        List<CheckBox> checkBoxes = new ArrayList<>();
        final List<String> interests = currentInterests;

        for (String domain : AVAILABLE_DOMAINS) {
            CheckBox cb = new CheckBox(domain);
            cb.setTextFill(Color.web("#a0aec0"));
            cb.setSelected(interests.contains(domain));
            cb.setStyle("-fx-font-size: 12px;");
            checkBoxes.add(cb);
            domainGrid.getChildren().add(cb);
        }

        Label saveStatus = new Label("");
        saveStatus.setFont(Font.font("System", 12));

        Button saveBtn = new Button("💾 Save Interests");
        saveBtn.setStyle("-fx-background-color:#6c9bff;-fx-text-fill:white;-fx-font-weight:bold;" +
                         "-fx-background-radius:6px;-fx-cursor:hand;-fx-padding:8 20;");

        final Researcher finalResearcher = researcher;
        saveBtn.setOnAction(e -> {
            if (finalResearcher == null) {
                status(saveStatus, "Only researchers can set interests.", false);
                return;
            }
            List<String> selected = checkBoxes.stream()
                .filter(CheckBox::isSelected)
                .map(CheckBox::getText)
                .collect(Collectors.toList());

            if (selected.isEmpty()) {
                status(saveStatus, "Select at least one domain.", false);
                return;
            }

            finalResearcher.setInterestedDomains(selected);
            collaborationController.saveUser(finalResearcher);
            status(saveStatus, "✓ Interests saved! Your feed is now personalized.", true);
        });

        selectorBox.getChildren().addAll(domainGrid, new HBox(10, saveBtn, saveStatus));

        // Keyword follow (Observer pattern)
        VBox keywordBox = new VBox(10);
        keywordBox.setPadding(new Insets(16));
        keywordBox.setStyle("-fx-background-color:#1a1f2e;-fx-background-radius:10px;");
        keywordBox.setMaxWidth(600);

        Text kwTitle = new Text("🏷 Email Alert Keywords (Observer Pattern)");
        kwTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        kwTitle.setFill(Color.web("#6c9bff"));

        Label kwDesc = new Label("Follow specific keywords. When a paper matching your keywords is published, " +
                                  "you'll get an email alert via n8n webhook.");
        kwDesc.setTextFill(Color.web("#8892a4"));
        kwDesc.setFont(Font.font("System", 11));
        kwDesc.setWrapText(true);

        HBox addRow = new HBox(8);
        addRow.setAlignment(Pos.CENTER_LEFT);
        TextField kwField = new TextField();
        kwField.setPromptText("Keyword e.g. Machine Learning, NLP...");
        kwField.setStyle("-fx-background-color:#0f1117;-fx-text-fill:#e2e8f0;-fx-prompt-text-fill:#4a5568;" +
                         "-fx-border-color:#2d3748;-fx-border-radius:6px;-fx-background-radius:6px;-fx-pref-height:36px;-fx-padding:0 12px;");
        HBox.setHgrow(kwField, Priority.ALWAYS);

        Button addBtn = new Button("+ Follow");
        addBtn.setStyle("-fx-background-color:#6c9bff;-fx-text-fill:white;-fx-font-weight:bold;" +
                        "-fx-background-radius:6px;-fx-cursor:hand;-fx-pref-height:36px;");
        Label addStatus = new Label("");
        addStatus.setFont(Font.font("System", 11));
        addRow.getChildren().addAll(kwField, addBtn);

        // Chips
        FlowPane chipBox = new FlowPane(6, 6);
        chipBox.setPadding(new Insets(4));

        final Runnable[] refreshChips = new Runnable[1];
        refreshChips[0] = () -> {
            chipBox.getChildren().clear();
            if (finalResearcher == null || finalResearcher.getFollowedKeywords().isEmpty()) {
                Label none = new Label("No keywords followed yet.");
                none.setFont(Font.font("System", FontPosture.ITALIC, 11));
                none.setTextFill(Color.web("#4a5568"));
                chipBox.getChildren().add(none);
            } else {
                for (String kw : finalResearcher.getFollowedKeywords()) {
                    HBox chip = new HBox(4);
                    chip.setAlignment(Pos.CENTER_LEFT);
                    Label chipLbl = new Label("🏷 " + kw);
                    chipLbl.setFont(Font.font("System", 11));
                    chipLbl.setTextFill(Color.web("#6c9bff"));
                    chipLbl.setStyle("-fx-background-color:#6c9bff22;-fx-padding:3 8;-fx-background-radius:10px;");
                    Button removeBtn = new Button("×");
                    removeBtn.setStyle("-fx-background-color:transparent;-fx-text-fill:#fc8181;" +
                                       "-fx-font-size:13px;-fx-cursor:hand;-fx-padding:0 3;");
                    final String kwFinal = kw;
                    removeBtn.setOnAction(ev -> {
                        finalResearcher.getFollowedKeywords().remove(kwFinal);
                        collaborationController.saveUser(finalResearcher);
                        refreshChips[0].run();
                    });
                    chip.getChildren().addAll(chipLbl, removeBtn);
                    chipBox.getChildren().add(chip);
                }
            }
        };
        refreshChips[0].run();

        addBtn.setOnAction(e -> {
            String kw = kwField.getText().trim();
            if (kw.isBlank() || finalResearcher == null) return;
            if (!finalResearcher.getFollowedKeywords().contains(kw)) {
                finalResearcher.getFollowedKeywords().add(kw);
                collaborationController.saveUser(finalResearcher);
            }
            kwField.clear();
            status(addStatus, "✓ Following \"" + kw + "\"", true);
            refreshChips[0].run();
        });

        keywordBox.getChildren().addAll(kwTitle, kwDesc, addRow, addStatus, chipBox);

        // Research feed based on interests
        VBox feedBox = new VBox(10);
        feedBox.setPadding(new Insets(16));
        feedBox.setStyle("-fx-background-color:#1a1f2e;-fx-background-radius:10px;");

        Text feedTitle = new Text("📄 Papers Matching Your Interests");
        feedTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        feedTitle.setFill(Color.web("#e2e8f0"));
        feedBox.getChildren().add(feedTitle);

        if (!currentInterests.isEmpty()) {
            List<ResearchPaper> matchingPapers = new ArrayList<>();
            try {
                for (String domain : currentInterests) {
                    matchingPapers.addAll(collaborationController.findPapersByDomain(domain));
                }
            } catch (Exception ignored) {}

            if (matchingPapers.isEmpty()) {
                feedBox.getChildren().add(emptyLabel("No papers matching your interests yet."));
            } else {
                for (ResearchPaper paper : matchingPapers.stream().distinct().limit(10).collect(Collectors.toList())) {
                    HBox row = new HBox(12);
                    row.setPadding(new Insets(8));
                    row.setStyle("-fx-background-color:#0f1117;-fx-background-radius:6px;");

                    VBox info = new VBox(2);
                    Label paperTitle = new Label(paper.getTitle());
                    paperTitle.setFont(Font.font("System", FontWeight.BOLD, 12));
                    paperTitle.setTextFill(Color.web("#e2e8f0"));
                    Label paperDomain = new Label(paper.getDomain() != null ? paper.getDomain() : "");
                    paperDomain.setTextFill(Color.web("#6c9bff"));
                    paperDomain.setFont(Font.font("System", 10));
                    info.getChildren().addAll(paperTitle, paperDomain);
                    row.getChildren().add(info);
                    feedBox.getChildren().add(row);
                }
            }
        } else {
            feedBox.getChildren().add(emptyLabel("Set your interests above to see personalized paper recommendations."));
        }

        pane.getChildren().addAll(selectorBox, keywordBox, feedBox);

        // Wrap in scroll
        ScrollPane scrollPane = new ScrollPane(pane);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color:transparent;-fx-background:transparent;");

        VBox wrapper = new VBox(scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        return wrapper;
    }

    // ══ TAB 4: Active Collaborations ════════════════════════════════

    private VBox buildActiveCollabsPane(User currentUser) {
        VBox pane = new VBox(12);

        pane.getChildren().addAll(
            sectionHeading("Active Collaborations"),
            smallNote("Browse research projects looking for collaborators. Send a request to join!")
        );

        // Toggle: All vs Filtered
        ToggleGroup filterGroup = new ToggleGroup();
        RadioButton allRadio = new RadioButton("All Open Projects");
        allRadio.setToggleGroup(filterGroup);
        allRadio.setSelected(true);
        allRadio.setTextFill(Color.web("#a0aec0"));

        RadioButton filteredRadio = new RadioButton("Matching My Interests");
        filteredRadio.setToggleGroup(filterGroup);
        filteredRadio.setTextFill(Color.web("#a0aec0"));

        HBox filterRow = new HBox(16, allRadio, filteredRadio);
        filterRow.setPadding(new Insets(0, 0, 8, 0));
        pane.getChildren().add(filterRow);

        VBox projectList = new VBox(10);

        Runnable loadProjects = () -> {
            projectList.getChildren().clear();
            List<ResearchProject> projects;

            if (filteredRadio.isSelected() && currentUser instanceof Researcher r) {
                List<String> myDomains = r.getInterestedDomains();
                if (myDomains.isEmpty()) {
                    projectList.getChildren().add(emptyLabel(
                        "Set your interests in the 'Following' tab first to filter projects."));
                    return;
                }
                projects = collaborationController.getOpenProjectsByDomains(myDomains);
            } else {
                projects = collaborationController.getOpenProjects();
            }

            if (projects.isEmpty()) {
                projectList.getChildren().add(emptyLabel("No open collaboration opportunities right now."));
                return;
            }

            for (ResearchProject project : projects) {
                // Skip own projects (owner or member)
                if (project.getOwner() != null &&
                    project.getOwner().getUserId().equals(currentUser.getUserId())) continue;
                boolean isMember = project.getMembers().stream()
                    .anyMatch(m -> m.getUserId().equals(currentUser.getUserId()));
                if (isMember) continue;

                VBox card = new VBox(8);
                card.setPadding(new Insets(14, 18, 14, 18));
                card.setMaxWidth(700);
                card.setStyle("-fx-background-color:#1a1f2e;-fx-background-radius:10px;" +
                              "-fx-border-color:#2d3748;-fx-border-radius:10px;");

                // Title + domain
                HBox titleRow = new HBox(10);
                titleRow.setAlignment(Pos.CENTER_LEFT);
                Label topicLbl = new Label(project.getTopic());
                topicLbl.setFont(Font.font("System", FontWeight.BOLD, 15));
                topicLbl.setTextFill(Color.web("#e2e8f0"));

                if (project.getDomain() != null) {
                    Label domainBadge = new Label(project.getDomain());
                    domainBadge.setStyle("-fx-background-color:#6c9bff22;-fx-text-fill:#6c9bff;" +
                                         "-fx-padding:2 8;-fx-background-radius:8px;-fx-font-size:10px;");
                    titleRow.getChildren().addAll(topicLbl, domainBadge);
                } else {
                    titleRow.getChildren().add(topicLbl);
                }

                // Owner + team size (exclude owner from member count)
                String ownerName = project.getOwner() != null ? project.getOwner().getName() : "Unknown";
                Long projOwnerId = project.getOwner() != null ? project.getOwner().getUserId() : null;
                long collabCount = project.getMembers().stream()
                    .filter(m -> projOwnerId == null || !m.getUserId().equals(projOwnerId))
                    .count();
                Label ownerLbl = new Label("👤 " + ownerName + " · 👥 Team: " + (collabCount + 1));
                ownerLbl.setTextFill(Color.web("#8892a4"));
                ownerLbl.setFont(Font.font("System", 12));

                // Description
                Label descLbl = new Label(project.getDescription() != null ? project.getDescription() : "No description provided.");
                descLbl.setTextFill(Color.web("#a0aec0"));
                descLbl.setFont(Font.font("System", 12));
                descLbl.setWrapText(true);
                descLbl.setMaxWidth(650);

                // Request button
                Label reqStatus = new Label("");
                reqStatus.setFont(Font.font("System", 11));

                Button requestBtn = new Button("🤝 Request to Join");
                requestBtn.setStyle("-fx-background-color:#6c9bff;-fx-text-fill:white;-fx-font-weight:bold;" +
                                    "-fx-background-radius:6px;-fx-cursor:hand;-fx-padding:6 16;");

                requestBtn.setOnAction(e -> {
                    try {
                        collaborationController.sendRequest(
                            project.getOwner().getUserId(),
                            project.getProjectId(),
                            "I'd like to join your project: " + project.getTopic()
                        );
                        status(reqStatus, "✓ Request sent to " + project.getOwner().getName() + "!", true);
                        requestBtn.setDisable(true);
                        requestBtn.setText("Request Sent ✓");
                    } catch (Exception ex) {
                        status(reqStatus, "Error: " + ex.getMessage(), false);
                    }
                });

                HBox actionRow = new HBox(10, requestBtn, reqStatus);
                actionRow.setAlignment(Pos.CENTER_LEFT);

                card.getChildren().addAll(titleRow, ownerLbl, descLbl, actionRow);
                projectList.getChildren().add(card);
            }
        };

        loadProjects.run();

        filterGroup.selectedToggleProperty().addListener((obs, o, n) -> loadProjects.run());

        ScrollPane scroll = new ScrollPane(projectList);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        pane.getChildren().add(scroll);
        return pane;
    }

    // ══ Request card component ════════════════════════════════════════

    private VBox requestCard(CollaborationRequest req, boolean inbox, Label actionStatus) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14, 18, 14, 18));
        card.setStyle("-fx-background-color:#1a1f2e;-fx-background-radius:10px;" +
                      "-fx-border-color:#2d3748;-fx-border-radius:10px;");

        String who = inbox
            ? "From: " + req.getSender().getName() + " <" + req.getSender().getEmail() + ">"
            : "To: "   + req.getReceiver().getName() + " <" + req.getReceiver().getEmail() + ">";

        Label fromLbl = new Label(who);
        fromLbl.setFont(Font.font("System", FontWeight.BOLD, 13));
        fromLbl.setTextFill(Color.web("#e2e8f0"));

        Label msgLbl = new Label(req.getMessage() != null && !req.getMessage().isBlank()
            ? req.getMessage() : "(No message)");
        msgLbl.setWrapText(true);
        msgLbl.setFont(Font.font("System", 13));
        msgLbl.setTextFill(Color.web("#a0aec0"));

        String statusColor = switch (req.getStatus()) {
            case PENDING  -> "#ecc94b";
            case ACCEPTED -> "#68d391";
            case REJECTED -> "#fc8181";
        };
        Label statusBadge = new Label("● " + req.getStatus());
        statusBadge.setFont(Font.font("System", FontWeight.BOLD, 11));
        statusBadge.setTextFill(Color.web(statusColor));

        String proj = req.getProject() != null
            ? "📂 " + req.getProject().getTopic() : "";
        if (!proj.isEmpty()) {
            Label projLbl = new Label(proj);
            projLbl.setFont(Font.font("System", FontPosture.ITALIC, 11));
            projLbl.setTextFill(Color.web("#6c9bff"));
            card.getChildren().addAll(fromLbl, msgLbl, projLbl, statusBadge);
        } else {
            card.getChildren().addAll(fromLbl, msgLbl, statusBadge);
        }
        return card;
    }

    // ══ Helpers ══════════════════════════════════════════════════════

    private Text sectionHeading(String text) {
        Text t = new Text(text);
        t.setFont(Font.font("Georgia", FontWeight.BOLD, 18));
        t.setFill(Color.web("#e2e8f0")); return t;
    }

    private Label smallNote(String text) {
        Label l = new Label(text);
        l.setWrapText(true); l.setFont(Font.font("System", 13));
        l.setTextFill(Color.web("#8892a4")); return l;
    }

    private Label emptyLabel(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("System", FontPosture.ITALIC, 13));
        l.setTextFill(Color.web("#4a5568")); l.setPadding(new Insets(16, 0, 0, 0)); return l;
    }

    private ToggleButton tabBtn(String text, ToggleGroup g, boolean sel) {
        ToggleButton b = new ToggleButton(text); b.setToggleGroup(g); b.setSelected(sel);
        String base = "-fx-background-radius:6px;-fx-cursor:hand;-fx-font-size:12px;-fx-padding:8 14;";
        b.setStyle("-fx-background-color:#1a1f2e;-fx-text-fill:#8892a4;" + base);
        b.selectedProperty().addListener((obs, o, n) -> b.setStyle(n
            ? "-fx-background-color:#6c9bff;-fx-text-fill:white;-fx-font-weight:bold;" + base
            : "-fx-background-color:#1a1f2e;-fx-text-fill:#8892a4;" + base));
        return b;
    }

    private void status(Label l, String msg, boolean ok) {
        l.setTextFill(Color.web(ok ? "#68d391" : "#fc8181")); l.setText(msg);
    }

    // ═══════════════════════════════════════════════════════════
    // Visitor-only panel: browse active collaborations (read-only)
    // ═══════════════════════════════════════════════════════════

    public VBox buildVisitorPanel(javafx.stage.Stage stage) {
        VBox panel = new VBox(0);
        panel.setStyle("-fx-background-color: #0f1117;");

        VBox header = new VBox(4);
        header.setPadding(new Insets(0, 0, 16, 0));
        Text title = new Text("Active Collaborations");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 26));
        title.setFill(Color.web("#e2e8f0"));
        Text subtitle = new Text("Browse open research collaborations · Login to participate");
        subtitle.setFont(Font.font("System", 13));
        subtitle.setFill(Color.web("#8892a4"));
        header.getChildren().addAll(title, subtitle);

        VBox projectList = new VBox(12);

        List<ResearchProject> projects = collaborationController.getOpenProjects();
        if (projects.isEmpty()) {
            projectList.getChildren().add(emptyLabel("No open collaboration opportunities right now."));
        } else {
            for (ResearchProject project : projects) {
                VBox card = new VBox(8);
                card.setPadding(new Insets(14, 18, 14, 18));
                card.setMaxWidth(700);
                card.setStyle("-fx-background-color:#1a1f2e;-fx-background-radius:10px;" +
                              "-fx-border-color:#2d3748;-fx-border-radius:10px;");

                HBox titleRow = new HBox(10);
                titleRow.setAlignment(Pos.CENTER_LEFT);
                Label topicLbl = new Label(project.getTopic());
                topicLbl.setFont(Font.font("System", FontWeight.BOLD, 15));
                topicLbl.setTextFill(Color.web("#e2e8f0"));

                if (project.getDomain() != null) {
                    Label domainBadge = new Label(project.getDomain());
                    domainBadge.setStyle("-fx-background-color:#6c9bff22;-fx-text-fill:#6c9bff;" +
                                         "-fx-padding:2 8;-fx-background-radius:8px;-fx-font-size:10px;");
                    titleRow.getChildren().addAll(topicLbl, domainBadge);
                } else {
                    titleRow.getChildren().add(topicLbl);
                }

                String ownerName = project.getOwner() != null ? project.getOwner().getName() : "Unknown";
                Label ownerLbl = new Label("👤 " + ownerName);
                ownerLbl.setTextFill(Color.web("#8892a4"));
                ownerLbl.setFont(Font.font("System", 12));

                Label descLbl = new Label(project.getDescription() != null ? project.getDescription() : "No description.");
                descLbl.setTextFill(Color.web("#a0aec0"));
                descLbl.setFont(Font.font("System", 12));
                descLbl.setWrapText(true);
                descLbl.setMaxWidth(650);

                Button requestBtn = new Button("🤝 Request to Join");
                requestBtn.setStyle("-fx-background-color:#6c9bff;-fx-text-fill:white;-fx-font-weight:bold;" +
                                    "-fx-background-radius:6px;-fx-cursor:hand;-fx-padding:6 16;");
                requestBtn.setOnAction(e -> {
                    com.research.view.dashboard.DashboardView dv =
                        com.research.ResearchCollaborationApp.getSpringContext()
                            .getBean(com.research.view.dashboard.DashboardView.class);
                    dv.showLoginDialog(stage, false);
                });

                card.getChildren().addAll(titleRow, ownerLbl, descLbl, requestBtn);
                projectList.getChildren().add(card);
            }
        }

        ScrollPane scroll = new ScrollPane(projectList);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        panel.getChildren().addAll(header, scroll);
        return panel;
    }
}
