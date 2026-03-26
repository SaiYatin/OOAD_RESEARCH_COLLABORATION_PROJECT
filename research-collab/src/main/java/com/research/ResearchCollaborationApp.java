package com.research;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import com.research.view.auth.LoginView;

/**
 * Main entry point - bridges Spring Boot context with JavaFX lifecycle.
 * Design Pattern: Facade (AppContext provides single entry to Spring beans from JavaFX)
 */
@SpringBootApplication
public class ResearchCollaborationApp extends Application {

    private static ConfigurableApplicationContext springContext;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() {
        // Start Spring Boot context before JavaFX stage opens
        springContext = SpringApplication.run(ResearchCollaborationApp.class);
    }

    @Override
    public void start(Stage primaryStage) {
        // Retrieve the LoginView bean (Spring-managed JavaFX view)
        LoginView loginView = springContext.getBean(LoginView.class);
        loginView.show(primaryStage);
    }

    @Override
    public void stop() {
        springContext.close();
        Platform.exit();
    }

    public static ConfigurableApplicationContext getSpringContext() {
        return springContext;
    }
}
