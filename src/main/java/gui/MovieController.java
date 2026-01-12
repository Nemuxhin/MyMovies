package gui;

import be.Category;
import be.Movie;
import bll.MovieManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
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
    @FXML private TextField txtFile; // <--- NEW: File Path Box
    @FXML private ComboBox<Category> cbCategory;

    // Buttons
    @FXML private Button btnAddMovie, btnEditMovie, btnDeleteMovie, btnSave, btnCancel;
    @FXML private Button btnSearch, btnClearSearch;
    @FXML private Button btnBrowse; // <--- NEW: Browse Button

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
        tblMovies.getItems().setAll(manager.getAllMovies());
        cbCategory.getItems().setAll(manager.getAllCategories());
    }

    private void setupListeners() {
        // 1. Search Logic
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) {
                tblMovies.getItems().setAll(manager.getAllMovies());
            } else {
                tblMovies.getItems().setAll(manager.searchMovies(newValue));
            }
        });

        if (btnClearSearch != null) {
            btnClearSearch.setOnAction(event -> {
                txtSearch.clear();
                tblMovies.getItems().setAll(manager.getAllMovies());
            });
        }

        // 2. Main Action Buttons
        btnSave.setOnAction(event -> handleSave());
        btnDeleteMovie.setOnAction(event -> handleDelete());
        btnAddMovie.setOnAction(event -> clearFields());
        btnEditMovie.setOnAction(event -> handleEdit());

        // 3. NEW: Browse Button Action
        if (btnBrowse != null) {
            btnBrowse.setOnAction(event -> handleBrowse());
        }

        // 4. Double-click to Play
        tblMovies.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && tblMovies.getSelectionModel().getSelectedItem() != null) {
                playMovie(tblMovies.getSelectionModel().getSelectedItem());
            }
        });
    }

    // Handle File Browsing ---
    private void handleBrowse() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Movie File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Video Files", "*.mp4", "*.mpeg4")
        );

        // Open dialog
        File selectedFile = fileChooser.showOpenDialog(btnBrowse.getScene().getWindow());

        if (selectedFile != null) {
            txtFile.setText(selectedFile.getAbsolutePath());
        }
    }

    private void handleSave() {
        System.out.println("--- Save Button Clicked ---"); // Debug 1

        try {
            String title = txtTitle.getText();
            String imdbStr = txtImdb.getText();
            String personalStr = txtPersonal.getText();
            Category selectedCat = cbCategory.getSelectionModel().getSelectedItem();
            String filePath = txtFile.getText();

            System.out.println("Title: " + title);       // Debug 2
            System.out.println("File: " + filePath);     // Debug 3
            System.out.println("Category: " + selectedCat); // Debug 4

            // 1. Check if numbers are valid
            if (imdbStr.isEmpty() || personalStr.isEmpty()) {
                System.out.println("Error: Ratings are empty");
                showAlert("Missing Info", "Please enter ratings.");
                return;
            }

            double imdb = Double.parseDouble(imdbStr);
            double personal = Double.parseDouble(personalStr);

            // 2. Check Validation
            if (selectedCat != null && !title.isEmpty() && !filePath.isEmpty()) {
                System.out.println("Validation Passed. Sending to Manager..."); // Debug 5

                manager.createMovie(title, imdb, personal, filePath, selectedCat);

                System.out.println("Manager finished. Reloading data..."); // Debug 6
                loadData();
                clearFields();
            } else {
                System.out.println("Error: Validation Failed (Null category or empty fields)");
                showAlert("Missing Info", "Please enter a Title, select a Category, and choose a File.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Error: Number format exception");
            e.printStackTrace();
            showAlert("Invalid Number", "Ratings must be numbers (e.g., 8.5).");
        } catch (Exception e) {
            System.out.println("CRITICAL ERROR IN SAVE:");
            e.printStackTrace(); // This will print the real database error if there is one
        }
    }

    private void handleEdit() {
        Movie selected = tblMovies.getSelectionModel().getSelectedItem();
        if (selected != null) {
            txtTitle.setText(selected.getTitle());
            txtImdb.setText(String.valueOf(selected.getImdbRating()));
            txtPersonal.setText(String.valueOf(selected.getPersonalRating()));
            txtFile.setText(selected.getFileLink()); // <--- Load file path

            if (!selected.getCategories().isEmpty()) {
                for (Category c : cbCategory.getItems()) {
                    if (c.getId() == selected.getCategories().get(0).getId()) {
                        cbCategory.getSelectionModel().select(c);
                        break;
                    }
                }
            }
        } else {
            showAlert("No Selection", "Please select a movie to edit.");
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
        txtFile.clear(); // <--- Clear file path
        cbCategory.getSelectionModel().clearSelection();
        tblMovies.getSelectionModel().clearSelection();
    }

    private void playMovie(Movie movie) {
        String path = movie.getFileLink();

        try {
            // 1. Check if it is a Website Link (HTTP / HTTPS)
            if (path.toLowerCase().startsWith("http://") || path.toLowerCase().startsWith("https://")) {
                // Open in default Web Browser (Chrome, Edge, Safari)
                Desktop.getDesktop().browse(new java.net.URI(path));
            }
            // 2. Assume it is a Local File
            else {
                File file = new File(path);
                if (file.exists()) {
                    // Open in default Media Player (VLC, Windows Media Player)
                    Desktop.getDesktop().open(file);
                } else {
                    showAlert("File Error", "Could not find file: " + path);
                    return; // Stop here so we don't update "Last View" for a broken file
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not open media: " + e.getMessage());
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
