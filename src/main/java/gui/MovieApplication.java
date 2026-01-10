package gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class MovieApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // UPDATED: Pointing to the new FXML file name
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/MovieManagerView.fxml"));

        Parent root = fxmlLoader.load();

        // Pass the Manager to the Controller (Dependency Injection)
        MovieController controller = fxmlLoader.getController();
        // controller.setManager(new MovieManager()); // Uncomment this when Manager is ready

        Scene scene = new Scene(root);
        stage.setTitle("My Movie Collection");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}