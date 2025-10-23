package com.warsaw.transport;

import com.warsaw.transport.ui.MainWindow;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main application class for Warsaw Transport Interactive Map
 */
public class WarsawTransportApp extends Application {
    
    private static final Logger logger = LoggerFactory.getLogger(WarsawTransportApp.class);
    private MainWindow mainWindow;
    
    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Starting Warsaw Transport Map Application");
            
            mainWindow = new MainWindow();
            Scene scene = new Scene(mainWindow.getRoot(), 1200, 800);
            
            primaryStage.setTitle("Warsaw Public Transport - Interactive Map");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            
            // Handle window close event properly
            primaryStage.setOnCloseRequest(event -> {
                logger.info("Window close requested - shutting down application");
                shutdown();
                Platform.exit();
                System.exit(0);
            });
            
            primaryStage.show();
            
            // Initialize the application components
            mainWindow.initialize();
            
            logger.info("Application started successfully");
            
        } catch (Exception e) {
            logger.error("Failed to start application", e);
            throw new RuntimeException("Application startup failed", e);
        }
    }
    
    @Override
    public void stop() {
        logger.info("Shutting down Warsaw Transport Map Application");
        shutdown();
    }
    
    private void shutdown() {
        if (mainWindow != null) {
            try {
                mainWindow.shutdown();
            } catch (Exception e) {
                logger.error("Error during mainWindow shutdown", e);
            }
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
