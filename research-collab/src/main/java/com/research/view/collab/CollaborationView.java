package com.research.view.collab;

import com.research.model.*;
import com.research.repository.ResearchProjectRepository;
import com.research.repository.UserRepository;
import com.research.service.AuthService;
import com.research.service.CollaborationService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CollaborationView — Member 3's complete view.
 *
 * Tabs:
 *   1. Inbox     — pending requests you received, accept/reject
 *   2. Sent      — requests you sent, track status
 *   3. Projects  — create research projects, view members
 *   4. Following — follow keywords for email notifications (Observer pattern)
 *   5. New Request — send a collaboration request to anyone
 *
 * Design Pattern: Observer (keyword follow → n8n email on new paper)
 * Design Principle: SRP (each tab = one responsibility)
 */
@Component
public class CollaborationView {

    private final CollaborationService collaborationService;
    private final ResearchProjectRepository projectRepository;
    private final UserRepository userRepository;

    public CollaborationView(CollaborationService collaborationService,
                             ResearchProjectRepository projectRepository,
                             UserRepository userRepository) {
        this.collaborationService = collaborationService;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    public VBox buildPanel(User currentUser) {
        VBox panel = new VBox(0);
        panel.setStyle("-fx-background-color: #0f1117;");

        VBox header = new VBox(4);
        header.setPadding(new Insets(0, 0, 16, 0));

        // Pending badge count
        long pendingCount = 0;
        try {
            pendingCount = collaborationService
                .getPendingRequestsForUser(currentUser.getUserId()).size();
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

        Text subtitle = new Text("Requests · Projects · Follow research threads · Observer-driven email alerts");
        subtitle.setFont(Font.font("System", 13));
        subtitle.setFill(Color.web("#8892a4"));
        header.getChildren().addAll(titleRow, subtitle);

        // Tabs
        ToggleGroup tabGroup = new ToggleGroup();
        ToggleButton inboxTab   = tabBtn("📥 Inbox"       + (pendingCount > 0 ? " (" + pendingCount + ")" : ""), tabGroup, true);
        ToggleButton sentTab    = tabBtn("📤 Sent",         tabGroup, false);
        ToggleButton projectTab = tabBtn("🗂 Projects",     tabGroup, false);
        ToggleButton followTab  = tabBtn("🔔 Following",    tabGroup, false);
        ToggleButton newTab     = tabBtn("➕ New Request",  tabGroup, false);
        HBox tabRow = new HBox(4, inboxTab, sentTab, projectTab, followTab, newTab);
        tabRow.setPadding(new Insets(0, 0, 16, 0));

        StackPane content = new StackPane();
        VBox.setVgrow(content, Priority.ALWAYS);
        content.getChildren().add(buildInboxPane(currentUser));

        tabGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            content.getChildren().clear();
            if (n == inboxTab)   content.getChildren().add(buildInboxPane(currentUser));
            else if (n == sentTab)    content.getChildren().add(buildSentPane(currentUser));
            else if (n == projectTab) content.getChildren().add(buildProjectPane(currentUser));
            else if (n == followTab)  content.getChildren().add(buildFollowPane(currentUser));
            else if (n == newTab)     content.getChildren().add(buildNewRequestPane(currentUser));
        });

        panel.getChildren().addAll(header, tabRow, content);
        return panel;
    }

    // ══ TAB 1: Inbox ════════════════════════════════════════════════

    private VBox buildInboxPane(User user) {
        VBox pane = new VBox(12);

        Text heading = sectionHeading("Incoming Requests");
        Label desc = smallNote("Collaboration requests sent to you. Accept to add them to your project.");
        pane.getChildren().addAll(heading, desc);

        List<CollaborationRequest> pending;
        try {
            pending = collaborationService.getPendingRequestsForUser(user.getUserId());
        } catch (Exception e) {
            pane.getChildren().add(emptyLabel("Could not load requests: " + e.getMessage()));
            return pane;
        }

        if (pending.isEmpty()) {
            pane.getChildren().add(emptyLabel("No pending requests. You're all caught up!"));
            return pane;
        }

        ScrollPane scroll = new ScrollPane();
        VBox cards = new VBox(10);
        scroll.setContent(cards); scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        for (CollaborationRequest req : pending) {
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
                    collaborationService.acceptRequest(req.getRequestId());
                    status(actionStatus, "✓ Accepted! Sender added to project if linked.", true);
                    acceptBtn.setDisable(true); rejectBtn.setDisable(true);
                } catch (Exception ex) { status(actionStatus, ex.getMessage(), false); }
            });
            rejectBtn.setOnAction(e -> {
                try {
                    collaborationService.rejectRequest(req.getRequestId());
                    status(actionStatus, "Request rejected.", false);
                    acceptBtn.setDisable(true); rejectBtn.setDisable(true);
                } catch (Exception ex) { status(actionStatus, ex.getMessage(), false); }
            });

            card.getChildren().add(btnRow);
            cards.getChildren().add(card);
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
        try { sent = collaborationService.getSentRequests(user.getUserId()); }
        catch (Exception e) { pane.getChildren().add(emptyLabel(e.getMessage())); return pane; }

        if (sent.isEmpty()) {
            pane.getChildren().add(emptyLabel("You haven't sent any requests yet.")); return pane;
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

    // ══ TAB 3: Projects ═════════════════════════════════════════════

    private VBox buildProjectPane(User user) {
        VBox pane = new VBox(16);
        pane.getChildren().addAll(sectionHeading("Research Projects"),
            smallNote("Create and manage research projects. Accepted collaborators are added as members."));

        // Create project form
        VBox createBox = new VBox(10);
        createBox.setPadding(new Insets(16));
        createBox.setStyle("-fx-background-color:#1a1f2e;-fx-background-radius:10px;-fx-border-color:#2d3748;-fx-border-radius:10px;");
        createBox.setMaxWidth(520);

        Label createHeading = new Label("Create New Project");
        createHeading.setFont(Font.font("Georgia", FontWeight.BOLD, 15));
        createHeading.setTextFill(Color.web("#e2e8f0"));

        TextField topicField = new TextField();
        topicField.setPromptText("Project topic / title *");
        topicField.setStyle(fieldStyle());

        TextArea descArea = new TextArea();
        descArea.setPromptText("Project description (optional)");
        descArea.setPrefRowCount(3); descArea.setWrapText(true);
        descArea.setStyle(fieldStyle() + "-fx-pref-height:70px;");

        Button createBtn = primaryBtn("Create Project");
        Label createStatus = new Label("");
        createStatus.setFont(Font.font("System", 12));

        createBtn.setOnAction(e -> {
            String topic = topicField.getText().trim();
            if (topic.isBlank()) { status(createStatus, "Enter a project topic.", false); return; }
            if (!(user instanceof Researcher researcher)) {
                status(createStatus, "Only Researcher accounts can create projects.", false); return;
            }
            ResearchProject proj = researcher.createResearchProject(topic);
            proj.setDescription(descArea.getText().trim());
            projectRepository.save(proj);
            status(createStatus, "✓ Project \"" + topic + "\" created!", true);
            topicField.clear(); descArea.clear();
            // Refresh list below — reload pane on next click
        });

        createBox.getChildren().addAll(createHeading, lbl("Topic *"), topicField,
            lbl("Description"), descArea, createBtn, createStatus);

        // Existing projects list
        VBox projectList = new VBox(8);
        projectList.getChildren().add(sectionHeading("Your Projects"));

        if (user instanceof Researcher researcher) {
            List<ResearchProject> projects = projectRepository.findByOwner(researcher);
            if (projects.isEmpty()) {
                projectList.getChildren().add(emptyLabel("No projects yet."));
            } else {
                for (ResearchProject proj : projects) {
                    VBox projCard = new VBox(4);
                    projCard.setPadding(new Insets(12, 16, 12, 16));
                    projCard.setStyle("-fx-background-color:#1a1f2e;-fx-background-radius:8px;-fx-border-color:#2d3748;-fx-border-radius:8px;");
                    Label topicLbl = new Label(proj.getTopic());
                    topicLbl.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
                    topicLbl.setTextFill(Color.web("#e2e8f0"));
                    Label membersLbl = new Label("Members: " + proj.getMembers().size() +
                        " | ID: " + proj.getProjectId());
                    membersLbl.setFont(Font.font("System", 11));
                    membersLbl.setTextFill(Color.web("#8892a4"));
                    projCard.getChildren().addAll(topicLbl, membersLbl);
                    projectList.getChildren().add(projCard);
                }
            }
        } else {
            projectList.getChildren().add(emptyLabel("Log in as Researcher to manage projects."));
        }

        pane.getChildren().addAll(createBox, projectList);
        return pane;
    }

    // ══ TAB 4: Following (Observer pattern UI) ══════════════════════

    private VBox buildFollowPane(User user) {
        VBox pane = new VBox(16);
        pane.setMaxWidth(560);

        pane.getChildren().addAll(
            sectionHeading("Follow Research Keywords"),
            smallNote("When a new paper matching your keywords is published, " +
                      "the Observer pattern fires and n8n Workflow 3 sends you an email automatically.")
        );

        // Keyword input
        HBox addRow = new HBox(12);
        addRow.setAlignment(Pos.CENTER_LEFT);
        TextField kwField = new TextField();
        kwField.setPromptText("Keyword  e.g. Machine Learning, NLP, Robotics");
        kwField.setStyle(fieldStyle());
        HBox.setHgrow(kwField, Priority.ALWAYS);
        Button addBtn = primaryBtn("+ Follow");
        addRow.getChildren().addAll(kwField, addBtn);

        Label addStatus = new Label("");
        addStatus.setFont(Font.font("System", 12));

        // Current keywords display
        Label followLbl = new Label("Currently following:");
        followLbl.setFont(Font.font("System", FontWeight.BOLD, 12));
        followLbl.setTextFill(Color.web("#6c9bff"));

        VBox chipBox = new VBox(6);
        chipBox.setPadding(new Insets(10));
        chipBox.setStyle("-fx-background-color:#1a1f2e;-fx-background-radius:8px;");

        Runnable refreshChips = () -> {
            chipBox.getChildren().clear();
            if (!(user instanceof Researcher researcher)
                    || researcher.getFollowedKeywords().isEmpty()) {
                Label none = new Label("Not following any keywords yet.");
                none.setFont(Font.font("System", FontPosture.ITALIC, 12));
                none.setTextFill(Color.web("#4a5568"));
                chipBox.getChildren().add(none);
            } else {
                for (String kw : ((Researcher) user).getFollowedKeywords()) {
                    HBox chip = new HBox(8);
                    chip.setAlignment(Pos.CENTER_LEFT);
                    Label chipLbl = new Label("🏷  " + kw);
                    chipLbl.setFont(Font.font("System", 12));
                    chipLbl.setTextFill(Color.web("#6c9bff"));
                    chipLbl.setStyle("-fx-background-color:#6c9bff22;-fx-padding:4 10;-fx-background-radius:12px;");
                    Button removeBtn = new Button("×");
                    removeBtn.setStyle("-fx-background-color:transparent;-fx-text-fill:#fc8181;" +
                                       "-fx-font-size:14px;-fx-cursor:hand;-fx-padding:0 4;");
                    final String kwFinal = kw;
                    removeBtn.setOnAction(e -> {
                        ((Researcher) user).getFollowedKeywords().remove(kwFinal);
                        userRepository.save(user);
                        refreshChips.run();
                    });
                    chip.getChildren().addAll(chipLbl, removeBtn);
                    chipBox.getChildren().add(chip);
                }
            }
        };

        refreshChips.run();

        addBtn.setOnAction(e -> {
            String kw = kwField.getText().trim();
            if (kw.isBlank()) { status(addStatus, "Enter a keyword.", false); return; }
            if (!(user instanceof Researcher researcher)) {
                status(addStatus, "Only Researcher accounts can follow keywords.", false); return;
            }
            if (!researcher.getFollowedKeywords().contains(kw)) {
                researcher.getFollowedKeywords().add(kw);
                userRepository.save(researcher);
            }
            kwField.clear();
            status(addStatus, "✓ Now following \"" + kw + "\". Email alert fires via n8n when a matching paper is published.", true);
            refreshChips.run();
        });

        // How it works box
        VBox howItWorks = new VBox(6);
        howItWorks.setPadding(new Insets(12));
        howItWorks.setStyle("-fx-background-color:#1a1f2e;-fx-background-radius:8px;-fx-border-color:#6c9bff44;-fx-border-radius:8px;");
        Label howLbl = new Label("How the Observer pattern works here:");
        howLbl.setFont(Font.font("System", FontWeight.BOLD, 12));
        howLbl.setTextFill(Color.web("#6c9bff"));
        String[] steps = {
            "1. You follow a keyword (stored in your Researcher profile in MySQL)",
            "2. A researcher publishes a paper tagged with that keyword",
            "3. PaperService.publishPaper() calls RecommendationService.onNewPaperPublished()",
            "4. PaperPublicationSubject notifies all EmailNotificationObserver instances",
            "5. N8nWebhookCaller sends HTTP POST to n8n Workflow 3",
            "6. n8n builds the HTML email and sends it via Gmail SMTP to you"
        };
        howItWorks.getChildren().add(howLbl);
        for (String step : steps) {
            Label s = new Label(step);
            s.setFont(Font.font("Courier New", 11));
            s.setTextFill(Color.web("#8892a4"));
            s.setWrapText(true);
            howItWorks.getChildren().add(s);
        }

        pane.getChildren().addAll(addRow, addStatus, followLbl, chipBox, howItWorks);
        return pane;
    }

    // ══ TAB 5: New Request ══════════════════════════════════════════

    private VBox buildNewRequestPane(User currentUser) {
        VBox pane = new VBox(12);
        pane.setMaxWidth(520);
        pane.getChildren().addAll(
            sectionHeading("Send Collaboration Request"),
            smallNote("Send a request to any user by their ID. Find user IDs from the expert list or ask your team.")
        );

        TextField receiverField = formField("Recipient User ID  (number) *");
        TextField projectField  = formField("Project ID  (optional — links request to a project)");
        TextArea  messageArea   = new TextArea();
        messageArea.setPromptText("Your message — introduce yourself and your collaboration intent…");
        messageArea.setPrefRowCount(5); messageArea.setWrapText(true);
        messageArea.setStyle(fieldStyle() + "-fx-pref-height:110px;");

        Button sendBtn = primaryBtn("Send Request");
        Label statusLbl = new Label("");
        statusLbl.setWrapText(true); statusLbl.setFont(Font.font("System", 12));

        sendBtn.setOnAction(e -> {
            String recvStr = receiverField.getText().trim();
            if (recvStr.isBlank()) { status(statusLbl, "Enter recipient user ID.", false); return; }
            try {
                Long receiverId = Long.parseLong(recvStr);
                Long projectId  = projectField.getText().isBlank()
                    ? null : Long.parseLong(projectField.getText().trim());
                String msg = messageArea.getText().trim();

                collaborationService.sendRequest(
                    currentUser.getUserId(), receiverId, projectId, msg);

                status(statusLbl, "✓ Request sent to user #" + receiverId + "! They will see it in their Inbox.", true);
                receiverField.clear(); projectField.clear(); messageArea.clear();
            } catch (NumberFormatException ex) {
                status(statusLbl, "User ID and Project ID must be numbers.", false);
            } catch (Exception ex) {
                status(statusLbl, "Error: " + ex.getMessage(), false);
            }
        });

        // Your user ID hint
        Label myId = new Label("Your User ID: " + currentUser.getUserId() +
            "  |  Share this with others so they can send you requests.");
        myId.setFont(Font.font("Courier New", 11));
        myId.setTextFill(Color.web("#4a5568"));
        myId.setStyle("-fx-background-color:#1a1f2e;-fx-padding:8 12;-fx-background-radius:6px;");

        pane.getChildren().addAll(
            myId,
            lbl("Recipient User ID *"), receiverField,
            lbl("Project ID (optional)"), projectField,
            lbl("Message"), messageArea,
            sendBtn, statusLbl
        );
        return pane;
    }

    // ══ Request card component ════════════════════════════════════════

    private VBox requestCard(CollaborationRequest req, boolean inbox, Label actionStatus) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14, 18, 14, 18));
        card.setStyle("-fx-background-color:#1a1f2e;-fx-background-radius:10px;" +
                      "-fx-border-color:#2d3748;-fx-border-radius:10px;");

        String who = inbox
            ? "From: " + req.getSender().getName() + " <" + req.getSender().getEmail() + "> (ID " + req.getSender().getUserId() + ")"
            : "To: "   + req.getReceiver().getName() + " <" + req.getReceiver().getEmail() + "> (ID " + req.getReceiver().getUserId() + ")";

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
            ? "Linked project: " + req.getProject().getTopic() : "";
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

    private Label lbl(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("System", FontWeight.BOLD, 11));
        l.setTextFill(Color.web("#8892a4")); return l;
    }

    private String fieldStyle() {
        return "-fx-background-color:#1a1f2e;-fx-text-fill:#e2e8f0;-fx-prompt-text-fill:#4a5568;" +
               "-fx-border-color:#2d3748;-fx-border-radius:6px;-fx-background-radius:6px;" +
               "-fx-pref-height:40px;-fx-font-size:13px;-fx-padding:0 12px;";
    }

    private TextField formField(String prompt) {
        TextField f = new TextField(); f.setPromptText(prompt);
        f.setStyle(fieldStyle()); f.setMaxWidth(Double.MAX_VALUE); return f;
    }

    private Button primaryBtn(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:#6c9bff;-fx-text-fill:white;-fx-font-weight:bold;" +
                   "-fx-pref-height:40px;-fx-pref-width:150px;-fx-background-radius:6px;-fx-cursor:hand;");
        return b;
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
}
