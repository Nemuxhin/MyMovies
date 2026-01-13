package gui;

import be.Category;
import be.Movie;
import bll.MovieManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import java.awt.Desktop;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class MovieController implements Initializable {

    //FXML UI Elements
    @FXML private TableView<Movie> tblMovies;
    @FXML private TableColumn<Movie, String> colTitle;
    @FXML private TableColumn<Movie, Double> colImdb;
    @FXML private TableColumn<Movie, Double> colPersonal;
    @FXML private TableColumn<Movie, String> colCategory;
    @FXML private TableColumn<Movie, String> colLastView;

    @FXML private TextField txtTitle, txtImdb, txtPersonal, txtSearch;
    @FXML private TextField txtFile;

    @FXML private ListView<Category> lstCategories;

    @FXML private Button btnAddMovie, btnEditMovie, btnDeleteMovie, btnSave, btnCancel;
    @FXML private Button btnSearch, btnClearSearch;
    @FXML private Button btnBrowse;
    @FXML private Button btnAddCategory, btnDeleteCategory;

    //Logic
    private MovieManager manager = new MovieManager();
    private Movie movieInEditMode = null;

    private ObservableList<Movie> masterData = FXCollections.observableArrayList();
    private FilteredList<Movie> filteredData;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Test DB Connection on startup
        testDbConnection();

        setupTable();
        setupFilterAndSort();
        lstCategories.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        loadData();
        setupListeners();
        checkOldMovies();
    }

    //Test Connection Method
    private void testDbConnection() {
        try (java.sql.Connection conn = new dal.ConnectionProvider().getConnection()) {
            // If this passes, connection is good!
        } catch (Exception e) {
            showAlert("Database Error", "Could not connect to the database.\n" +
                    "Check your internet or VPN.\n\nError: " + e.getMessage());
        }
    }

    private void setupTable() {
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colImdb.setCellValueFactory(new PropertyValueFactory<>("imdbRating"));
        colPersonal.setCellValueFactory(new PropertyValueFactory<>("personalRating"));
        if (colLastView != null) colLastView.setCellValueFactory(new PropertyValueFactory<>("lastView"));
        colCategory.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCategoriesAsString()));
    }

    private void setupFilterAndSort() {
        filteredData = new FilteredList<>(masterData, p -> true);
        SortedList<Movie> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tblMovies.comparatorProperty());
        tblMovies.setItems(sortedData);
    }

    private void loadData() {
        try {
            masterData.setAll(manager.getAllMovies());
            lstCategories.getItems().setAll(manager.getAllCategories());
        } catch (Exception e) {
            showAlert("Data Error", "Failed to load data from database.");
            e.printStackTrace();
        }
    }

    private void setupListeners() {
        txtSearch.textProperty().addListener((obs, oldV, newV) -> {
            filteredData.setPredicate(movie -> {
                if (newV == null || newV.isEmpty()) return true;
                String lower = newV.toLowerCase();
                return movie.getTitle().toLowerCase().contains(lower) ||
                        movie.getCategoriesAsString().toLowerCase().contains(lower);
            });
        });

        if (btnClearSearch != null) btnClearSearch.setOnAction(e -> txtSearch.clear());

        btnSave.setOnAction(e -> handleSave());
        btnDeleteMovie.setOnAction(e -> handleDelete());
        btnAddMovie.setOnAction(e -> clearFields());
        btnEditMovie.setOnAction(e -> handleEdit());
        if (btnBrowse != null) btnBrowse.setOnAction(e -> handleBrowse());

        tblMovies.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && tblMovies.getSelectionModel().getSelectedItem() != null) {
                playMovie(tblMovies.getSelectionModel().getSelectedItem());
            }
        });
    }

    // Play Logic with File Check
    private void playMovie(Movie movie) {
        String path = movie.getFileLink();
        if (path == null || path.isEmpty()) {
            showAlert("No File", "No file path specified for this movie.");
            return;
        }

        try {
            if (path.toLowerCase().startsWith("http")) {
                Desktop.getDesktop().browse(new java.net.URI(path));
            } else {
                File file = new File(path);
                // --- CHECK: Does file exist? ---
                if (!file.exists()) {
                    showAlert("File Not Found", "Could not find the file:\n" + path);
                    return;
                }
                Desktop.getDesktop().open(file);
            }

            // "Last View"
            manager.updateLastView(movie.getId());
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
            movie.setLastView(sdf.format(new java.util.Date()));
            tblMovies.refresh();

        } catch (Exception e) {
            showAlert("Error", "Could not open media: " + e.getMessage());
        }
    }

    @FXML
    private void handleAddCategory() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Category");
        dialog.setHeaderText("Create a new category");
        dialog.setContentText("Name:");
        dialog.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                manager.createCategory(name);
                loadData();
            }
        });
    }

    @FXML
    private void handleDeleteCategory() {
        Category selected = lstCategories.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete category '" + selected.getName() + "'?", ButtonType.YES, ButtonType.NO);
            alert.showAndWait();
            if (alert.getResult() == ButtonType.YES) {
                manager.deleteCategory(selected);
                loadData();
            }
        } else {
            showAlert("No Selection", "Please select a category to delete.");
        }
    }

    private void handleBrowse() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Movie File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Video Files", "*.mp4", "*.mpeg4", "*.avi", "*.mkv"));
        File selectedFile = fileChooser.showOpenDialog(btnBrowse.getScene().getWindow());
        if (selectedFile != null) txtFile.setText(selectedFile.getAbsolutePath());
    }

    private void handleSave() {
        try {
            String title = txtTitle.getText();
            String imdbStr = txtImdb.getText();
            String personalStr = txtPersonal.getText();
            String filePath = txtFile.getText();
            List<Category> selectedCats = new ArrayList<>(lstCategories.getSelectionModel().getSelectedItems());

            if (title.isEmpty() || imdbStr.isEmpty() || personalStr.isEmpty() || selectedCats.isEmpty() || filePath.isEmpty()) {
                showAlert("Missing Info", "Please fill in all fields (Title, Ratings, Category, File).");
                return;
            }

            double imdb = Double.parseDouble(imdbStr);
            double personal = Double.parseDouble(personalStr);

            if (movieInEditMode != null) {
                movieInEditMode.setTitle(title);
                movieInEditMode.setImdbRating(imdb);
                movieInEditMode.setPersonalRating(personal);
                movieInEditMode.setFileLink(filePath);
                manager.updateMovie(movieInEditMode, selectedCats);
                movieInEditMode = null;
            } else {
                manager.createMovie(title, imdb, personal, filePath, selectedCats);
            }

            loadData();
            clearFields();

        } catch (NumberFormatException e) {
            showAlert("Invalid Number", "Ratings must be numbers (e.g., 8.5).");
        } catch (Exception e) {
            showAlert("Error", "Could not save movie.\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleEdit() {
        Movie selected = tblMovies.getSelectionModel().getSelectedItem();
        if (selected != null) {
            movieInEditMode = selected;
            txtTitle.setText(selected.getTitle());
            txtImdb.setText(String.valueOf(selected.getImdbRating()));
            txtPersonal.setText(String.valueOf(selected.getPersonalRating()));
            txtFile.setText(selected.getFileLink());

            lstCategories.getSelectionModel().clearSelection();
            for (Category listCat : lstCategories.getItems()) {
                for (Category movieCat : selected.getCategories()) {
                    if (listCat.getId() == movieCat.getId()) {
                        lstCategories.getSelectionModel().select(listCat);
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
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete movie '" + selected.getTitle() + "'?", ButtonType.YES, ButtonType.NO);
            alert.showAndWait();
            if (alert.getResult() == ButtonType.YES) {
                manager.deleteMovie(selected);
                loadData();
            }
        } else {
            showAlert("No Selection", "Please select a movie to delete.");
        }
    }

    private void clearFields() {
        txtTitle.clear();
        txtImdb.clear();
        txtPersonal.clear();
        txtFile.clear();
        lstCategories.getSelectionModel().clearSelection();
        tblMovies.getSelectionModel().clearSelection();
        movieInEditMode = null;
    }

    private void checkOldMovies() {
        List<Movie> warningList = new ArrayList<>();
        java.time.LocalDate twoYearsAgo = java.time.LocalDate.now().minusYears(2);

        for (Movie m : masterData) {
            if (m.getPersonalRating() < 6.0 && m.getLastView() != null && !m.getLastView().isEmpty()) {
                try {
                    java.time.LocalDate lastViewDate = java.time.LocalDate.parse(m.getLastView());
                    if (lastViewDate.isBefore(twoYearsAgo)) warningList.add(m);
                } catch (Exception e) { }
            }
        }

        if (!warningList.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Delete Old Movies");
            alert.setHeaderText("These movies are low rated (<6) and haven't been watched in 2 years:");
            StringBuilder sb = new StringBuilder();
            for (Movie m : warningList) {
                sb.append("• ").append(m.getTitle()).append(" (Last viewed: ").append(m.getLastView()).append(")\n");
            }
            TextArea area = new TextArea(sb.toString());
            area.setEditable(false);
            area.setWrapText(true);
            area.setMaxWidth(Double.MAX_VALUE);
            area.setMaxHeight(Double.MAX_VALUE);
            alert.getDialogPane().setContent(area);
            alert.showAndWait();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION); // Can change to ERROR or WARNING if preferred
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}