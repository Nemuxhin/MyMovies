package gui;

import be.Category;
import be.Movie;
import bll.MovieManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.awt.Desktop;
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class MovieController implements Initializable {

    // --- FXML UI Elements ---
    @FXML private TableView<Movie> tblMovies;
    @FXML private TableColumn<Movie, String> colTitle;
    @FXML private TableColumn<Movie, Double> colImdb;
    @FXML private TableColumn<Movie, Double> colPersonal;
    @FXML private TableColumn<Movie, String> colCategory;

    @FXML private TextField txtTitle, txtImdb, txtPersonal, txtSearch;
    @FXML private ComboBox<Category> cbCategory;
    @FXML private Button btnAddMovie, btnEditMovie, btnDeleteMovie, btnSave, btnCancel;

    // --- Logic ---
    private MovieManager manager = new MovieManager();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        loadData();
        setupListeners();
    }

    private void setupTable() {
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colImdb.setCellValueFactory(new PropertyValueFactory<>("imdbRating"));
        colPersonal.setCellValueFactory(new PropertyValueFactory<>("personalRating"));
        colCategory.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCategoriesAsString())
        );
    }

    private void loadData() {
        // 1. Populate Table
        tblMovies.getItems().setAll(manager.getAllMovies());

        // 2. Populate Categories Dropdown
        cbCategory.getItems().setAll(manager.getAllCategories());
    }

    private void setupListeners() {
        // BUTTON: Save (The Disk Icon)
        btnSave.setOnAction(event -> handleSave());

        // BUTTON: Delete (The Trash Icon)
        btnDeleteMovie.setOnAction(event -> handleDelete());

        // BUTTON: Add (+) - Just clears the fields for typing
        btnAddMovie.setOnAction(event -> clearFields());

        // BUTTON: Play (Double click table row)
        tblMovies.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && tblMovies.getSelectionModel().getSelectedItem() != null) {
                playMovie(tblMovies.getSelectionModel().getSelectedItem());
            }
        });
    }

    private void handleSave() {
        try {
            String title = txtTitle.getText();
            double imdb = Double.parseDouble(txtImdb.getText());
            double personal = Double.parseDouble(txtPersonal.getText());
            Category selectedCat = cbCategory.getSelectionModel().getSelectedItem();

            // Temporary: Use a dummy file path for now since there is no file chooser yet
            String filePath = "data/dummy.mp4";

            if (selectedCat != null && !title.isEmpty()) {
                // Send to BLL
                manager.createMovie(title, imdb, personal, filePath, selectedCat);

                // Refresh and Clean up
                loadData();
                clearFields();
                System.out.println("Movie Saved!");
            } else {
                showAlert("Missing Info", "Please enter a Title and select a Category.");
            }
        } catch (NumberFormatException e) {
            showAlert("Invalid Number", "Ratings must be numbers (e.g., 8.5).");
        }
    }

    private void handleDelete() {
        Movie selected = tblMovies.getSelectionModel().getSelectedItem();
        if (selected != null) {
            manager.deleteMovie(selected);
            loadData();
        } else {
            showAlert("No Selection", "Please select a movie to delete.");
        }
    }

    private void clearFields() {
        txtTitle.clear();
        txtImdb.clear();
        txtPersonal.clear();
        cbCategory.getSelectionModel().clearSelection();
        tblMovies.getSelectionModel().clearSelection();
    }

    private void playMovie(Movie movie) {
        try {
            File file = new File(movie.getFileLink());
            // Check if file exists, if not, try opening a default one for testing
            if (file.exists()) {
                Desktop.getDesktop().open(file);
            } else {
                showAlert("File Error", "Could not find file: " + movie.getFileLink());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}