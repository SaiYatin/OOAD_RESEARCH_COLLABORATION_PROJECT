package com.research.view.collab;

import com.research.model.*;
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
 * CollaborationView - Member 3's primary UI.
 * Displays pending requests, sent requests, and keyword-follow for notifications.
 * MVC: View layer — delegates all logic to CollaborationService.
 */
@Component
public class CollaborationView {

    private final CollaborationService collaborationService;

    public CollaborationView(CollaborationService collaborationService) {
        this.collaborationService = collaborationService;
    }

    public VBox buildPanel(User currentUser) {
        VBox panel = new VBox(24);
        panel.setStyle("-fx-background-color: #0f1117;");

        // ── Header ────────────────────────────────────────────────────
        Text title = new Text("Collaborations");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 26));
        title.setFill(Color.web("#e2e8f0"));

        Text subtitle = new Text("Manage collaboration requests and follow research threads");
        subtitle.setFont(Font.font("System", 13));
        subtitle.setFill(Color.web("#8892a4"));

        // ── Tab Strip ─────────────────────────────────────────────────
        ToggleGroup tabGroup = new ToggleGroup();
        ToggleButton inboxTab   = tabBtn("📥 Inbox",    tabGroup, true);
        ToggleButton sentTab    = tabBtn("📤 Sent",     tabGroup, false);
        ToggleButton followTab  = tabBtn("🔔 Following", tabGroup, false);
        ToggleButton sendTab    = tabBtn("➕ New Request", tabGroup, false);

        HBox tabRow = new HBox(4, inboxTab, sentTab, followTab, sendTab);
        tabRow.setAlignment(Pos.CENTER_LEFT);

        StackPane contentPane = new StackPane();
        VBox.setVgrow(contentPane, Priority.ALWAYS);

        // Build all panes
        VBox inboxPane  = buildInboxPane(currentUser);
        VBox sentPane   = buildSentPane(currentUser);
        VBox followPane = buildFollowPane(currentUser);
        VBox sendPane   = buildSendRequestPane(currentUser);

        contentPane.getChildren().add(inboxPane);

        tabGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            contentPane.getChildren().clear();
            if (n == inboxTab)  contentPane.getChildren().add(inboxPane);
            if (n == sentTab)   contentPane.getChildren().add(sentPane);
            if (n == followTab) contentPane.getChildren().add(followPane);
            if (n == sendTab)   contentPane.getChildren().add(sendPane);
        });

        panel.getChildren().addAll(title, subtitle, tabRow, contentPane);
        return panel;
    }

    // ── Inbox: pending requests ────────────────────────────────────────
    private VBox buildInboxPane(User user) {
        VBox pane = new VBox(12);

        List<CollaborationRequest> pending =
            collaborationService.getPendingRequestsForUser(user.getUserId());

        if (pending.isEmpty()) {
            Label none = emptyLabel("No pending collaboration requests.");
            pane.getChildren().add(none);
            return pane;
        }

        for (CollaborationRequest req : pending) {
            pane.getChildren().add(requestCard(req, true, user));
        }
        return pane;
    }

    // ── Sent: outgoing requests ────────────────────────────────────────
    private VBox buildSentPane(User user) {
        VBox pane = new VBox(12);

        List<CollaborationRequest> sent =
            collaborationService.getSentRequests(user.getUserId());

        if (sent.isEmpty()) {
            pane.getChildren().add(emptyLabel("You haven't sent any collaboration requests."));
            return pane;
        }

        for (CollaborationRequest req : sent) {
            pane.getChildren().add(requestCard(req, false, user));
        }
        return pane;
    }

    // ── Follow: keyword-based email notification subscriptions ─────────
    private VBox buildFollowPane(User user) {
        VBox pane = new VBox(20);
        pane.setMaxWidth(560);

        Text heading = new Text("Follow Research Keywords");
        heading.setFont(Font.font("Georgia", FontWeight.BOLD, 18));
        heading.setFill(Color.web("#e2e8f0"));

        Label desc = new Label(
            "Add keywords below. When a new paper matching your keywords is published, " +
            "you'll receive an email notification via our n8n automation workflow.");
        desc.setWrapText(true);
        desc.setFont(Font.font("System", 13));
        desc.setTextFill(Color.web("#8892a4"));

        // Existing keywords display
        VBox keywordList = new VBox(8);
        keywordList.setPadding(new Insets(12));
        keywordList.setStyle("-fx-background-color: #1a1f2e; -fx-background-radius: 8px;");

        Label kwTitle = new Label("Currently Following:");
        kwTitle.setFont(Font.font("System", FontWeight.BOLD, 12));
        kwTitle.setTextFill(Color.web("#6c9bff"));

        keywordList.getChildren().add(kwTitle);
        if (user instanceof Researcher researcher) {
            if (researcher.getFollowedKeywords().isEmpty()) {
                keywordList.getChildren().add(emptyLabel("No keywords followed yet."));
            } else {
                for (String kw : researcher.getFollowedKeywords()) {
                    keywordList.getChildren().add(keywordChip(kw));
                }
            }
        }

        // Add new keyword
        HBox addRow = new HBox(12);
        addRow.setAlignment(Pos.CENTER_LEFT);

        TextField kwField = new TextField();
        kwField.setPromptText("e.g. Machine Learning, NLP, Robotics");
        styleField(kwField);
        HBox.setHgrow(kwField, Priority.ALWAYS);

        Button addBtn = new Button("+ Follow");
        addBtn.setStyle(
            "-fx-background-color: #6c9bff; -fx-text-fill: white; " +
            "-fx-font-weight: bold; -fx-pref-height: 40px; -fx-pref-width: 100px; " +
            "-fx-background-radius: 6px; -fx-cursor: hand;");

        Label statusLbl = new Label("");
        statusLbl.setFont(Font.font("System", 12));

        addBtn.setOnAction(e -> {
            String kw = kwField.getText().trim();
            if (kw.isBlank()) {
                statusLbl.setText("Please enter a keyword.");
                statusLbl.setTextFill(Color.web("#fc8181"));
                return;
            }
            if (user instanceof Researcher researcher) {
                if (!researcher.getFollowedKeywords().contains(kw)) {
                    researcher.getFollowedKeywords().add(kw);
                    keywordList.getChildren().add(keywordChip(kw));
                }
                kwField.clear();
                statusLbl.setTextFill(Color.web("#68d391"));
                statusLbl.setText("Now following: \"" + kw + "\". "
                    + "Email alerts will be sent via n8n when new papers match.");
            } else {
                statusLbl.setTextFill(Color.web("#fc8181"));
                statusLbl.setText("Only Researcher accounts can follow keywords.");
            }
        });

        addRow.getChildren().addAll(kwField, addBtn);

        Label n8nNote = new Label(
            "ℹ️  Powered by n8n Email Notification Workflow — " +
            "triggers automatically when n8n detects a keyword match on paper publish.");
        n8nNote.setWrapText(true);
        n8nNote.setFont(Font.font("System", FontPosture.ITALIC, 11));
        n8nNote.setTextFill(Color.web("#4a5568"));

        pane.getChildren().addAll(heading, desc, keywordList, addRow, statusLbl, n8nNote);
        return pane;
    }

    // ── Send New Request form ──────────────────────────────────────────
    private VBox buildSendRequestPane(User currentUser) {
        VBox pane = new VBox(16);
        pane.setMaxWidth(520);

        Text heading = new Text("New Collaboration Request");
        heading.setFont(Font.font("Georgia", FontWeight.BOLD, 18));
        heading.setFill(Color.web("#e2e8f0"));

        Label recipientLabel = new Label("Recipient User ID");
        styleLabel(recipientLabel);
        TextField recipientField = new TextField();
        styleField(recipientField);
        recipientField.setPromptText("Enter the user ID of the researcher/expert");

        Label projectLabel = new Label("Project ID (optional)");
        styleLabel(projectLabel);
        TextField projectField = new TextField();
        styleField(projectField);
        projectField.setPromptText("Leave blank if not linking to a project");

        Label messageLabel = new Label("Message");
        styleLabel(messageLabel);
        TextArea messageArea = new TextArea();
        messageArea.setPromptText("Introduce yourself and explain the collaboration intent...");
        messageArea.setStyle(
            "-fx-background-color: #1a1f2e; -fx-text-fill: #e2e8f0; " +
            "-fx-prompt-text-fill: #4a5568; -fx-border-color: #2d3748; " +
            "-fx-border-radius: 6px; -fx-background-radius: 6px; " +
            "-fx-font-size: 13px; -fx-pref-height: 120px;");

        Button sendBtn = new Button("Send Request");
        sendBtn.setStyle(
            "-fx-background-color: #6c9bff; -fx-text-fill: white; " +
            "-fx-font-weight: bold; -fx-pref-height: 44px; -fx-pref-width: 180px; " +
            "-fx-background-radius: 6px; -fx-cursor: hand;");

        Label statusLbl = new Label("");
        statusLbl.setWrapText(true);
        statusLbl.setFont(Font.font("System", 12));

        sendBtn.setOnAction(e -> {
            try {
                Long receiverId = Long.parseLong(recipientField.getText().trim());
                Long projectId = projectField.getText().isBlank()
                    ? null : Long.parseLong(projectField.getText().trim());
                String message = messageArea.getText().trim();

                collaborationService.sendRequest(
                    currentUser.getUserId(), receiverId, projectId, message);

                statusLbl.setTextFill(Color.web("#68d391"));
                statusLbl.setText("Collaboration request sent successfully!");
                recipientField.clear();
                messageArea.clear();
                projectField.clear();
            } catch (NumberFormatException ex) {
                statusLbl.setTextFill(Color.web("#fc8181"));
                statusLbl.setText("Invalid User ID or Project ID. Enter numbers only.");
            } catch (Exception ex) {
                statusLbl.setTextFill(Color.web("#fc8181"));
                statusLbl.setText("Error: " + ex.getMessage());
            }
        });

        pane.getChildren().addAll(heading, recipientLabel, recipientField,
                projectLabel, projectField, messageLabel, messageArea,
                sendBtn, statusLbl);
        return pane;
    }

    // ── Request Card component ─────────────────────────────────────────
    private VBox requestCard(CollaborationRequest req, boolean showActions, User viewer) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setStyle("-fx-background-color: #1a1f2e; -fx-background-radius: 10px; " +
                      "-fx-border-color: #2d3748; -fx-border-radius: 10px;");

        String who = showActions
            ? "From: " + req.getSender().getName() + " (" + req.getSender().getEmail() + ")"
            : "To: " + req.getReceiver().getName() + " (" + req.getReceiver().getEmail() + ")";

        Label from = new Label(who);
        from.setFont(Font.font("System", FontWeight.BOLD, 13));
        from.setTextFill(Color.web("#e2e8f0"));

        Label msg = new Label(req.getMessage() != null && !req.getMessage().isBlank()
            ? req.getMessage() : "(No message)");
        msg.setWrapText(true);
        msg.setFont(Font.font("System", 13));
        msg.setTextFill(Color.web("#a0aec0"));

        String statusColor = switch (req.getStatus()) {
            case PENDING  -> "#ecc94b";
            case ACCEPTED -> "#68d391";
            case REJECTED -> "#fc8181";
        };
        Label statusBadge = new Label("● " + req.getStatus());
        statusBadge.setFont(Font.font("System", FontWeight.BOLD, 11));
        statusBadge.setTextFill(Color.web(statusColor));

        card.getChildren().addAll(from, msg, statusBadge);

        if (showActions && req.getStatus() == CollaborationRequest.RequestStatus.PENDING) {
            HBox actions = new HBox(10);
            Button acceptBtn = new Button("✓ Accept");
            acceptBtn.setStyle("-fx-background-color: #68d391; -fx-text-fill: #1a1f2e; " +
                               "-fx-font-weight: bold; -fx-background-radius: 6px; -fx-cursor: hand;");
            Button rejectBtn = new Button("✗ Reject");
            rejectBtn.setStyle("-fx-background-color: #fc8181; -fx-text-fill: white; " +
                               "-fx-font-weight: bold; -fx-background-radius: 6px; -fx-cursor: hand;");
            Label feedback = new Label("");
            feedback.setFont(Font.font("System", 12));

            acceptBtn.setOnAction(e -> {
                collaborationService.acceptRequest(req.getRequestId());
                feedback.setTextFill(Color.web("#68d391"));
                feedback.setText("Request accepted!");
                acceptBtn.setDisable(true);
                rejectBtn.setDisable(true);
            });
            rejectBtn.setOnAction(e -> {
                collaborationService.rejectRequest(req.getRequestId());
                feedback.setTextFill(Color.web("#fc8181"));
                feedback.setText("Request rejected.");
                acceptBtn.setDisable(true);
                rejectBtn.setDisable(true);
            });

            actions.getChildren().addAll(acceptBtn, rejectBtn, feedback);
            card.getChildren().add(actions);
        }

        return card;
    }

    // ── Helpers ────────────────────────────────────────────────────────
    private Label keywordChip(String keyword) {
        Label chip = new Label("🏷 " + keyword);
        chip.setFont(Font.font("System", 12));
        chip.setTextFill(Color.web("#6c9bff"));
        chip.setStyle("-fx-background-color: #6c9bff22; -fx-padding: 4 10; " +
                      "-fx-background-radius: 12px;");
        return chip;
    }

    private Label emptyLabel(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("System", FontPosture.ITALIC, 13));
        l.setTextFill(Color.web("#4a5568"));
        l.setPadding(new Insets(20, 0, 0, 0));
        return l;
    }

    private void styleLabel(Label l) {
        l.setFont(Font.font("System", FontWeight.BOLD, 12));
        l.setTextFill(Color.web("#8892a4"));
    }

    private void styleField(TextField f) {
        f.setStyle(
            "-fx-background-color: #1a1f2e; -fx-text-fill: #e2e8f0; " +
            "-fx-prompt-text-fill: #4a5568; -fx-border-color: #2d3748; " +
            "-fx-border-radius: 6px; -fx-background-radius: 6px; " +
            "-fx-pref-height: 40px; -fx-font-size: 13px; -fx-padding: 0 12px;");
    }

    private ToggleButton tabBtn(String text, ToggleGroup group, boolean selected) {
        ToggleButton btn = new ToggleButton(text);
        btn.setToggleGroup(group);
        btn.setSelected(selected);
        btn.setStyle("-fx-background-color: #1a1f2e; -fx-text-fill: #8892a4; " +
                     "-fx-background-radius: 6px; -fx-cursor: hand; -fx-font-size: 12px; " +
                     "-fx-padding: 8 16;");
        btn.selectedProperty().addListener((obs, o, n) -> {
            if (n) btn.setStyle("-fx-background-color: #6c9bff; -fx-text-fill: white; " +
                                "-fx-font-weight: bold; -fx-background-radius: 6px; " +
                                "-fx-cursor: hand; -fx-font-size: 12px; -fx-padding: 8 16;");
            else   btn.setStyle("-fx-background-color: #1a1f2e; -fx-text-fill: #8892a4; " +
                                "-fx-background-radius: 6px; -fx-cursor: hand; " +
                                "-fx-font-size: 12px; -fx-padding: 8 16;");
        });
        return btn;
    }
}
