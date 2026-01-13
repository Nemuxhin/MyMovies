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

    // --- FXML UI Elements ---
    @FXML private TableView<Movie> tblMovies;
    @FXML private TableColumn<Movie, String> colTitle;
    @FXML private TableColumn<Movie, Double> colImdb;
    @FXML private TableColumn<Movie, Double> colPersonal;
    @FXML private TableColumn<Movie, String> colCategory;
    @FXML private TableColumn<Movie, String> colLastView;

    @FXML private TextField txtTitle, txtImdb, txtPersonal, txtSearch;
    @FXML private TextField txtFile;

    @FXML private ListView<Category> lstCategories;

    // Buttons
    @FXML private Button btnAddMovie, btnEditMovie, btnDeleteMovie, btnSave, btnCancel;
    @FXML private Button btnSearch, btnClearSearch;
    @FXML private Button btnBrowse;
    @FXML private Button btnAddCategory, btnDeleteCategory;

    // --- Logic ---
    private MovieManager manager = new MovieManager();
    private Movie movieInEditMode = null;

    // --- EXAM REQUIREMENT: Data Lists for Filtering & Sorting ---
    // We store data here instead of directly in the TableView to allow filtering.
    private ObservableList<Movie> masterData = FXCollections.observableArrayList();
    private FilteredList<Movie> filteredData;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();

        // 1. Setup Filter and Sorting Logic
        setupFilterAndSort();

        // 2. Enable Multi-Selection for Categories
        lstCategories.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // 3. Load Data and Listeners
        loadData();
        setupListeners();

        // 4. EXAM REQUIREMENT: Warning for old low-rated movies
        checkOldMovies();
    }

    private void setupTable() {
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colImdb.setCellValueFactory(new PropertyValueFactory<>("imdbRating"));
        colPersonal.setCellValueFactory(new PropertyValueFactory<>("personalRating"));

        if (colLastView != null) {
            colLastView.setCellValueFactory(new PropertyValueFactory<>("lastView"));
        }

        colCategory.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCategoriesAsString())
        );
    }

    // --- NEW: Setup FilteredList and SortedList ---
    private void setupFilterAndSort() {
        // A. Wrap the ObservableList in a FilteredList
        filteredData = new FilteredList<>(masterData, p -> true);

        // B. Wrap the FilteredList in a SortedList
        SortedList<Movie> sortedData = new SortedList<>(filteredData);

        // C. Bind the SortedList to the TableView
        // This makes clicking column headers actually sort the filtered data!
        sortedData.comparatorProperty().bind(tblMovies.comparatorProperty());

        // D. Add sorted data to the table
        tblMovies.setItems(sortedData);
    }

    private void loadData() {
        // Update the MASTER list. The TableView updates automatically because of the binding.
        masterData.setAll(manager.getAllMovies());
        lstCategories.getItems().setAll(manager.getAllCategories());
    }

    private void setupListeners() {
        // 1. Search Logic
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(movie -> {
                // If filter text is empty, display all movies
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase();

                // Check if filter matches Title OR Category
                if (movie.getTitle().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (movie.getCategoriesAsString().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }

                return false;
            });
        });

        if (btnClearSearch != null) {
            btnClearSearch.setOnAction(event -> {
                txtSearch.clear();
            });
        }

        // 2. Buttons
        btnSave.setOnAction(event -> handleSave());
        btnDeleteMovie.setOnAction(event -> handleDelete());
        btnAddMovie.setOnAction(event -> clearFields());
        btnEditMovie.setOnAction(event -> handleEdit());

        if (btnBrowse != null) {
            btnBrowse.setOnAction(event -> handleBrowse());
        }

        // 3. Double-click to Play
        tblMovies.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && tblMovies.getSelectionModel().getSelectedItem() != null) {
                playMovie(tblMovies.getSelectionModel().getSelectedItem());
            }
        });
    }

    // --- Category Management Logic ---
    @FXML
    private void handleAddCategory() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Category");
        dialog.setHeaderText("Create a new category");
        dialog.setContentText("Name:");

        dialog.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                manager.createCategory(name);
                loadData(); // Reloads categories
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
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Video Files", "*.mp4", "*.mpeg4")
        );
        File selectedFile = fileChooser.showOpenDialog(btnBrowse.getScene().getWindow());
        if (selectedFile != null) {
            txtFile.setText(selectedFile.getAbsolutePath());
        }
    }

    private void handleSave() {
        try {
            String title = txtTitle.getText();
            String imdbStr = txtImdb.getText();
            String personalStr = txtPersonal.getText();
            String filePath = txtFile.getText();
            List<Category> selectedCats = new ArrayList<>(lstCategories.getSelectionModel().getSelectedItems());

            // Validation
            if (title.isEmpty() || imdbStr.isEmpty() || personalStr.isEmpty() || selectedCats.isEmpty() || filePath.isEmpty()) {
                showAlert("Missing Info", "Please fill in all fields (Title, Ratings, Category, File).");
                return;
            }

            double imdb = Double.parseDouble(imdbStr);
            double personal = Double.parseDouble(personalStr);

            // Decide: Edit or Create
            if (movieInEditMode != null) {
                // UPDATE
                movieInEditMode.setTitle(title);
                movieInEditMode.setImdbRating(imdb);
                movieInEditMode.setPersonalRating(personal);
                movieInEditMode.setFileLink(filePath);

                // Pass selected categories to manager for update
                manager.updateMovie(movieInEditMode, selectedCats);
                movieInEditMode = null;
            } else {
                // CREATE
                manager.createMovie(title, imdb, personal, filePath, selectedCats);
            }

            loadData();
            clearFields();

        } catch (NumberFormatException e) {
            showAlert("Invalid Number", "Ratings must be numbers (e.g., 8.5).");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not save movie.");
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

            // Select the movie's categories in the list
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
        txtFile.clear();
        lstCategories.getSelectionModel().clearSelection();
        tblMovies.getSelectionModel().clearSelection();
        movieInEditMode = null;
    }

    private void playMovie(Movie movie) {
        String path = movie.getFileLink();
        try {
            if (path.toLowerCase().startsWith("http")) {
                Desktop.getDesktop().browse(new java.net.URI(path));
            } else {
                File file = new File(path);
                if (file.exists()) {
                    Desktop.getDesktop().open(file);
                } else {
                    showAlert("File Error", "Could not find file: " + path);
                    return;
                }
            }
            manager.updateLastView(movie.getId());

            // Refresh logic to show updated Last View immediately
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
            movie.setLastView(sdf.format(new java.util.Date()));

            // Refresh the table view so the new date shows up
            tblMovies.refresh();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not open media: " + e.getMessage());
        }
    }

    // --- EXAM REQUIREMENT: Robust Warning System ---
    private void checkOldMovies() {
        List<Movie> warningList = new ArrayList<>();
        java.time.LocalDate twoYearsAgo = java.time.LocalDate.now().minusYears(2);

        // Loop through MASTER data
        for (Movie m : masterData) {
            if (m.getPersonalRating() < 6.0 && m.getLastView() != null && !m.getLastView().isEmpty()) {
                try {
                    java.time.LocalDate lastViewDate = java.time.LocalDate.parse(m.getLastView());

                    if (lastViewDate.isBefore(twoYearsAgo)) {
                        warningList.add(m);
                    }
                } catch (Exception ignored) {
                }
            }
        }

        if (!warningList.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Delete Old Movies");
            alert.setHeaderText("These movies are low rated (<6) and haven't been watched in 2 years:");

            StringBuilder sb = new StringBuilder();
            for (Movie m : warningList) {
                sb.append("• ").append(m.getTitle())
                        .append(" (Last viewed: ").append(m.getLastView()).append(")\n");
            }

            // Use TextArea for scrollable content if list is long
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
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}