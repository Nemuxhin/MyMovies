package gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * JavaFX application entry point.
 * This class loads the main FXML view and shows the primary stage.
 */
public class MovieApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {

        // Load the main UI from FXML (keeps UI layout separate from Java code)
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/MovieManagerView.fxml"));

        // If the FXML path is wrong, loader.load() will throw an exception
        Parent root = loader.load();

        // Create the scene and attach it to the main window (stage)
        Scene scene = new Scene(root);

        stage.setTitle("My Movie Collection");
        stage.setScene(scene);

        // Optional: prevent the window from being resized too small
        stage.setMinWidth(900);
        stage.setMinHeight(600);

        stage.show();
    }

    /**
     * Launches the JavaFX application.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
