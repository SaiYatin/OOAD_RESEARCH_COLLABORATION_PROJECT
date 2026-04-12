package com.research;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import com.research.view.auth.LoginView;
import io.github.cdimascio.dotenv.Dotenv;

/**
 * Main entry point - bridges Spring Boot context with JavaFX lifecycle.
 * Design Pattern: Facade (AppContext provides single entry to Spring beans from JavaFX)
 */
@SpringBootApplication
public class ResearchCollaborationApp extends Application {

    private static ConfigurableApplicationContext springContext;

    public static void main(String[] args) {
        System.out.println(">>> [App] main() started - loading environment variables.");
        // Load .env variables into system properties for Spring Boot
        Dotenv dotenv = Dotenv.configure()
                .directory("./")
                .ignoreIfMissing()
                .load();
        dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
        System.out.println(">>> [App] Environment variables loaded. Launching JavaFX application.");
        
        launch(args);
    }

    @Override
    public void init() {
        System.out.println(">>> [App] init() called - starting Spring Boot context.");
        // Start Spring Boot context before JavaFX stage opens
        springContext = SpringApplication.run(ResearchCollaborationApp.class);
        System.out.println(">>> [App] Spring Boot context started successfully.");
    }

    @Override
    public void start(Stage primaryStage) {
        System.out.println(">>> [App] start() called - showing LoginView.");
        // Retrieve the LoginView bean (Spring-managed JavaFX view)
        LoginView loginView = springContext.getBean(LoginView.class);
        loginView.show(primaryStage);
    }

    @Override
    public void stop() {
        System.out.println(">>> [App] stop() called - shutting down Spring Context and terminating.");
        springContext.close();
        Platform.exit();
    }

    public static ConfigurableApplicationContext getSpringContext() {
        return springContext;
    }
}
