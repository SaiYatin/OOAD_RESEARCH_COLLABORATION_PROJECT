package com.research.view.expert;

import com.research.model.Expert;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class ExpertCardUI extends HBox {

    public ExpertCardUI(Expert expert, int rank, String query) {
        setSpacing(25);
        setPadding(new Insets(15, 20, 15, 20));
        setStyle("-fx-background-color:#141722;-fx-background-radius:10px;" +
                 "-fx-border-color:#212738;-fx-border-radius:10px;");
        setAlignment(Pos.CENTER_LEFT);

        // 1. Profile Image Box
        VBox imageBox = new VBox();
        imageBox.setAlignment(Pos.CENTER);
        imageBox.setStyle("-fx-background-color:#1a1f2e;-fx-background-radius:10px;-fx-border-color:#2d3748;-fx-border-radius:10px;-fx-padding:10;");
        imageBox.setPrefWidth(120);
        imageBox.setPrefHeight(150);
        
        String url = expert.getProfileUrl();
        if (url != null && !url.isBlank() && !url.trim().equalsIgnoreCase("nan") && !url.trim().equalsIgnoreCase("not listed")) {
            try {
                ImageView iv = new ImageView(new Image(url, 100, 130, true, true));
                imageBox.getChildren().add(iv);
            } catch (Exception e) {
                Label noImg = new Label("👤"); noImg.setFont(Font.font(40)); noImg.setTextFill(Color.web("#4a5568"));
                imageBox.getChildren().add(noImg);
            }
        } else {
            Label noImg = new Label("👤"); noImg.setFont(Font.font(40)); noImg.setTextFill(Color.web("#4a5568"));
            imageBox.getChildren().add(noImg);
        }

        // 2. Info Box (Name, Teaching Department, Campus, Email, Phone)
        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);

        TextField name = selectableTextField(expert.getName(), 22, FontWeight.BOLD, "#ffffff");
        VBox.setMargin(name, new Insets(0, 0, 4, -7)); 

        String deptStr = (expert.getDepartment() != null && !expert.getDepartment().equalsIgnoreCase("nan")) ? expert.getDepartment() : "Not listed";
        TextField teaching = selectableTextField("Teaching — " + deptStr, 13, FontWeight.BOLD, "#ffffff");

        TextField campusField = selectableTextField("Campus: " + (expert.getCampus() != null && !expert.getCampus().equalsIgnoreCase("nan") ? expert.getCampus() : "PES University"), 11, FontWeight.BOLD, "#8892a4");
        VBox.setMargin(campusField, new Insets(0, 0, 8, 0));

        TextField email = selectableTextField("Email: " + (expert.getEmail() != null && !expert.getEmail().equalsIgnoreCase("nan") ? expert.getEmail() : "Not listed"), 12, FontWeight.NORMAL, "#6c9bff");

        TextField phone = selectableTextField("Phone: " + (expert.getPhone() != null && !expert.getPhone().equalsIgnoreCase("nan") ? expert.getPhone() : "Not listed"), 12, FontWeight.NORMAL, "#ffffff");

        info.getChildren().addAll(name, teaching, campusField, email, phone);

        // 3. Actions Button (Details)
        VBox actions = new VBox(6);
        actions.setAlignment(Pos.CENTER);
        Button contactBtn = new Button("📄 Details");
        contactBtn.setStyle("-fx-background-color:#6c9bff;-fx-text-fill:white;-fx-font-size:11px;" +
                            "-fx-pref-width:95px;-fx-pref-height:32px;-fx-background-radius:6px;-fx-cursor:hand;");
        contactBtn.setOnAction(e -> {
            new ExpertProfileDialogBuilder(expert).build().showAndWait();
        });
        actions.getChildren().addAll(contactBtn);

        // Put it all together (Image -> Info -> Button)
        getChildren().addAll(imageBox, info, actions);
    }

    private TextField selectableTextField(String text, int size, FontWeight weight, String hexColor) {
        TextField tf = new TextField(text);
        tf.setEditable(false);
        tf.setFont(Font.font("Segoe UI", weight, size));
        tf.setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-text-fill: " + hexColor + "; " +
                    "-fx-background-insets: 0; -fx-background-radius: 0; -fx-border-width: 0;");
        // Ensure it doesn't look like a text box when focused
        tf.focusedProperty().addListener((obs, oldV, newV) -> tf.setStyle(tf.getStyle() + "-fx-faint-focus-color: transparent; -fx-focus-color: transparent;"));
        return tf;
    }
}
