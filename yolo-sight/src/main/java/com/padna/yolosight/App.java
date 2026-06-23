package com.padna.yolosight;

import com.padna.yolosight.gui.MainWindow;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * JavaFX application entry point.
 */
public final class App extends Application {

    @Override
    public void start(Stage stage) {
        MainWindow mainWindow = new MainWindow(stage);
        Scene scene = new Scene(mainWindow.getRoot(),
                com.padna.yolosight.config.AppConfig.WINDOW_WIDTH,
                com.padna.yolosight.config.AppConfig.WINDOW_HEIGHT);

        // Dark theme CSS
        scene.getStylesheets().add(
                App.class.getResource("/css/dark-theme.css").toExternalForm());

        stage.setTitle(com.padna.yolosight.config.AppConfig.APP_NAME);
        stage.setScene(scene);
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
