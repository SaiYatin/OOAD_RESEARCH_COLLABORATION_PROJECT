package com.research.view.expert;

import com.research.model.ResearchProject;
import com.research.model.Researcher;
import com.research.model.User;
import com.research.repository.ResearchProjectRepository;
import com.research.service.AuthService;
import com.research.service.CollaborationService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;

public class ResearcherCardUI extends HBox {

    public ResearcherCardUI(Researcher r, CollaborationService collaborationService,
            ResearchProjectRepository projectRepository) {
        setSpacing(16);
        setPadding(new Insets(14, 18, 14, 18));
        setStyle("-fx-background-color:#1a1f2e;-fx-background-radius:10px;" +
                "-fx-border-color:#68d39144;-fx-border-radius:10px;");
        setAlignment(Pos.CENTER_LEFT);

        Label userIcon = new Label("👤");
        userIcon.setFont(Font.font("System", 22));
        userIcon.setMinWidth(34);

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Text rName = new Text(r.getName());
        rName.setFont(Font.font("Georgia", FontWeight.BOLD, 15));
        rName.setFill(Color.web("#e2e8f0"));

        Label emailLbl = new Label(r.getEmail());
        emailLbl.setFont(Font.font("System", 11));
        emailLbl.setTextFill(Color.web("#8892a4"));

        Label interests = new Label(r.getResearchInterests() != null
                ? r.getResearchInterests()
                : "No interests listed");
        interests.setFont(Font.font("System", 11));
        interests.setTextFill(Color.web("#718096"));
        interests.setWrapText(true);

        Label roleBadge = new Label("RESEARCHER");
        roleBadge.setStyle("-fx-background-color:#68d39122;-fx-text-fill:#68d391;" +
                "-fx-padding:2 8;-fx-background-radius:10px;-fx-font-size:10px;");

        info.getChildren().addAll(rName, emailLbl, interests, roleBadge);

        VBox actions = new VBox(6);
        actions.setAlignment(Pos.CENTER);

        Label reqStatus = new Label("");
        reqStatus.setFont(Font.font("System", 10));

        Button requestBtn = new Button("🤝 Collaborate");
        requestBtn.setStyle("-fx-background-color:#68d391;-fx-text-fill:#1a1f2e;-fx-font-size:11px;" +
                "-fx-pref-width:110px;-fx-pref-height:32px;-fx-background-radius:6px;" +
                "-fx-cursor:hand;-fx-font-weight:bold;");
        requestBtn.setOnAction(e -> {
            User currentUser = AuthService.getCurrentUser();
            if (currentUser == null) {
                reqStatus.setTextFill(Color.web("#fc8181"));
                reqStatus.setText("Login first");
                return;
            }

            List<ResearchProject> myProjects = new ArrayList<>();
            try {
                if (currentUser instanceof Researcher) {
                    myProjects = projectRepository.findByOwner((Researcher) currentUser);
                }
            } catch (Exception ignored) {
            }

            if (myProjects.isEmpty()) {
                Alert noProj = new Alert(Alert.AlertType.CONFIRMATION);
                noProj.setTitle("No Projects");
                noProj.setHeaderText("You don't have any projects yet.");
                noProj.setContentText(
                        "Create a project in 'My Researches' first, or send a general collaboration request?");
                noProj.getButtonTypes().setAll(new ButtonType("Send General Request"), ButtonType.CANCEL);
                noProj.showAndWait().ifPresent(bt -> {
                    if (bt.getText().equals("Send General Request")) {
                        try {
                            collaborationService.sendRequest(
                                    currentUser.getUserId(), r.getUserId(), null,
                                    "Hi! I'd like to collaborate with you.");
                            reqStatus.setTextFill(Color.web("#68d391"));
                            reqStatus.setText("Request sent!");
                            requestBtn.setDisable(true);
                            requestBtn.setText("Sent \u2713");
                        } catch (Exception ex) {
                            reqStatus.setTextFill(Color.web("#fc8181"));
                            reqStatus.setText(ex.getMessage());
                        }
                    }
                });
            } else {
                ChoiceDialog<String> dialog = new ChoiceDialog<>();
                dialog.setTitle("Select Project");
                dialog.setHeaderText("Which project do you want to collaborate on with " + r.getName() + "?");
                dialog.setContentText("Project:");
                List<ResearchProject> finalProjects = myProjects;
                for (ResearchProject p : myProjects) {
                    dialog.getItems().add(p.getTopic() + " (ID:" + p.getProjectId() + ")");
                }
                if (!dialog.getItems().isEmpty())
                    dialog.setSelectedItem(dialog.getItems().get(0));
                dialog.showAndWait().ifPresent(selected -> {
                    try {
                        String idStr = selected.substring(selected.lastIndexOf("ID:") + 3, selected.length() - 1);
                        Long projectId = Long.parseLong(idStr);
                        ResearchProject proj = finalProjects.stream()
                                .filter(p -> p.getProjectId().equals(projectId))
                                .findFirst().orElse(null);
                        String msg = "Hi! I'd like to collaborate on '" +
                                (proj != null ? proj.getTopic() : "a project") + "' with you.";
                        collaborationService.sendRequest(
                                currentUser.getUserId(), r.getUserId(), projectId, msg);
                        reqStatus.setTextFill(Color.web("#68d391"));
                        reqStatus.setText("Request sent!");
                        requestBtn.setDisable(true);
                        requestBtn.setText("Sent \u2713");
                    } catch (Exception ex) {
                        reqStatus.setTextFill(Color.web("#fc8181"));
                        reqStatus.setText(ex.getMessage());
                    }
                });
            }
        });

        actions.getChildren().addAll(requestBtn, reqStatus);
        getChildren().addAll(userIcon, info, actions);
    }
}
