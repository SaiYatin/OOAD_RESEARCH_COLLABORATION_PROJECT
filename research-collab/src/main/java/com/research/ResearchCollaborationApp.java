package com.research;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import com.research.view.dashboard.DashboardView;

/**
 * Main entry point - bridges Spring Boot context with JavaFX lifecycle.
 * Design Pattern: Facade (AppContext provides single entry to Spring beans from JavaFX)
 *
 * App boots directly into Visitor mode (browse without login).
 * Users can login/signup from the top bar to unlock full features.
 */
@SpringBootApplication
public class ResearchCollaborationApp extends Application {

    private static ConfigurableApplicationContext springContext;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() {
        springContext = SpringApplication.run(ResearchCollaborationApp.class);
    }

    @Override
    public void start(Stage primaryStage) {
        // Boot directly into visitor dashboard (no login required)
        DashboardView dashboardView = springContext.getBean(DashboardView.class);
        dashboardView.show(primaryStage, null); // null = visitor mode
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
