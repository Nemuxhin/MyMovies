package gui;

import be.Category;
import be.Movie;
import bll.MovieManager;
import javafx.beans.property.SimpleStringProperty;
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
import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Main controller for the Movie Manager view.
 * Handles UI events and delegates data operations to the BLL (MovieManager).
 */
public class MovieController implements Initializable {

    // ---------- TableView ----------
    @FXML private TableView<Movie> tblMovies;
    @FXML private TableColumn<Movie, String> colTitle;
    @FXML private TableColumn<Movie, Double> colImdb;
    @FXML private TableColumn<Movie, Double> colPersonal;
    @FXML private TableColumn<Movie, String> colCategory;
    @FXML private TableColumn<Movie, String> colLastView;

    // ---------- Inputs ----------
    @FXML private TextField txtTitle;
    @FXML private TextField txtImdb;
    @FXML private TextField txtPersonal;
    @FXML private TextField txtSearch;
    @FXML private TextField txtMinImdb;
    @FXML private TextField txtFile;

    // ---------- Categories ----------
    @FXML private ListView<Category> lstCategories;

    // ---------- Buttons ----------
    @FXML private Button btnAddMovie;
    @FXML private Button btnEditMovie;
    @FXML private Button btnDeleteMovie;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;
    @FXML private Button btnSearch;
    @FXML private Button btnClearSearch;
    @FXML private Button btnBrowse;
    @FXML private Button btnAddCategory;
    @FXML private Button btnDeleteCategory;

    // ---------- Bottom bar ----------
    @FXML private Label lblStatus;
    @FXML private Label lblCount;

    // ---------- Logic ----------
    private final MovieManager manager = new MovieManager();
    private Movie movieInEditMode = null;

    private final ObservableList<Movie> masterData = FXCollections.observableArrayList();
    private FilteredList<Movie> filteredData;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        // Quick startup check so the user immediately knows if DB is unreachable
        testDbConnection();

        setupTable();
        setupFilterAndSort();

        // A movie can have multiple categories
        lstCategories.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        loadData();
        setupListeners();

        // This warning must run AFTER data is loaded
        checkOldMovies();

        updateBottomBar();
    }

    /**
     * Attempts a connection at startup to show a friendly error early.
     */
    private void testDbConnection() {
        try (java.sql.Connection conn = new dal.ConnectionProvider().getConnection()) {
            // Connection OK
        } catch (Exception e) {
            showError("Database Error",
                    "Could not connect to the database.\n" +
                            "Check your internet/VPN.\n\nError: " + e.getMessage());
        }
    }

    /**
     * Defines how Movie properties are shown inside the table columns.
     */
    private void setupTable() {
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colImdb.setCellValueFactory(new PropertyValueFactory<>("imdbRating"));
        colPersonal.setCellValueFactory(new PropertyValueFactory<>("personalRating"));

        // LastView is stored as String in the entity (format: YYYY-MM-DD)
        colLastView.setCellValueFactory(new PropertyValueFactory<>("lastView"));

        // Categories are displayed as a comma-separated string
        colCategory.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getCategoriesAsString())
        );
    }

    /**
     * Enables sorting while filtering:
     * FilteredList controls visibility, SortedList applies column sorting on top.
     */
    private void setupFilterAndSort() {
        filteredData = new FilteredList<>(masterData, m -> true);

        SortedList<Movie> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tblMovies.comparatorProperty());

        tblMovies.setItems(sortedData);
    }

    /**
     * Loads movies and categories from the database.
     */
    private void loadData() {
        try {
            masterData.setAll(manager.getAllMovies());
            lstCategories.getItems().setAll(manager.getAllCategories());
            updateBottomBar();
        } catch (Exception e) {
            showError("Data Error", "Failed to load data from the database.");
            e.printStackTrace();
        }
    }

    /**
     * Registers all UI listeners and actions.
     */
    private void setupListeners() {

        // Live filtering (search + min imdb) happens whenever the user types
        txtSearch.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        txtMinImdb.textProperty().addListener((obs, oldV, newV) -> applyFilters());

        // Optional button: just re-applies current filters
        if (btnSearch != null) {
            btnSearch.setOnAction(e -> applyFilters());
        }

        if (btnClearSearch != null) {
            btnClearSearch.setOnAction(e -> {
                txtSearch.clear();
                txtMinImdb.clear();
                applyFilters();
            });
        }

        if (btnBrowse != null) {
            btnBrowse.setOnAction(e -> handleBrowse());
        }

        if (btnSave != null) {
            btnSave.setOnAction(e -> handleSave());
        }

        if (btnCancel != null) {
            btnCancel.setOnAction(e -> clearFields());
        }

        if (btnAddMovie != null) {
            btnAddMovie.setOnAction(e -> clearFields());
        }

        if (btnEditMovie != null) {
            btnEditMovie.setOnAction(e -> handleEdit());
        }

        if (btnDeleteMovie != null) {
            btnDeleteMovie.setOnAction(e -> handleDelete());
        }

        // Double click: play the movie and update Last View
        tblMovies.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Movie selected = tblMovies.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    playMovie(selected);
                }
            }
        });
    }

    /**
     * Applies all active filters:
     * - text search (title + category string)
     * - minimum IMDB rating
     */
    private void applyFilters() {

        String query = (txtSearch.getText() == null) ? "" : txtSearch.getText().trim().toLowerCase();
        String minText = (txtMinImdb.getText() == null) ? "" : txtMinImdb.getText().trim();

        Double minImdb = null;

        // Parse minimum IMDB rating (supports both "." and "," decimals)
        if (!minText.isBlank()) {
            try {
                minImdb = Double.parseDouble(minText.replace(",", "."));
            } catch (NumberFormatException ignored) {
                // Invalid input -> ignore min rating filter (keeps UI responsive)
                minImdb = null;
            }
        }

        Double finalMinImdb = minImdb;

        filteredData.setPredicate(movie -> {
            if (movie == null) return false;

            // 1) Text filter
            if (!query.isBlank()) {
                String title = (movie.getTitle() == null) ? "" : movie.getTitle().toLowerCase();
                String categ = movie.getCategoriesAsString().toLowerCase();

                if (!title.contains(query) && !categ.contains(query)) {
                    return false;
                }
            }

            // 2) Minimum IMDB filter
            if (finalMinImdb != null) {
                return movie.getImdbRating() >= finalMinImdb;
            }

            return true;
        });

        updateBottomBar();
    }

    /**
     * Opens a file chooser to pick a movie file.
     * Only MP4 and MPEG4 are allowed.
     */
    private void handleBrowse() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Movie File");

        // Only allowed formats (project requirement)
        fileChooser.getExtensionFilters().clear();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Video files (*.mp4, *.mpeg4)", "*.mp4", "*.mpeg4")
        );

        File selectedFile = fileChooser.showOpenDialog(btnBrowse.getScene().getWindow());
        if (selectedFile != null) {
            txtFile.setText(selectedFile.getAbsolutePath());
        }
    }

    /**
     * Plays a local movie file or opens a URL.
     * Updates Last View in DB and updates the UI model.
     */
    private void playMovie(Movie movie) {

        String path = movie.getFileLink();

        if (path == null || path.isBlank()) {
            showWarning("No File", "No file path specified for this movie.");
            return;
        }

        if (!Desktop.isDesktopSupported()) {
            showError("Unsupported", "Desktop integration is not supported on this system.");
            return;
        }

        try {
            if (path.toLowerCase().startsWith("http")) {
                Desktop.getDesktop().browse(new URI(path));
            } else {
                File file = new File(path);

                // Always verify the file exists before trying to open it
                if (!file.exists()) {
                    showWarning("File Not Found", "Could not find the file:\n" + path);
                    return;
                }

                Desktop.getDesktop().open(file);
            }

            // Update last view in database and in UI object
            manager.updateLastView(movie.getId());
            movie.setLastView(LocalDate.now().toString());
            tblMovies.refresh();

            updateBottomBar();

        } catch (Exception e) {
            showError("Error", "Could not open media:\n" + e.getMessage());
        }
    }

    // ---------- Category actions ----------

    @FXML
    private void handleAddCategory() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Category");
        dialog.setHeaderText("Create a new category");
        dialog.setContentText("Name:");

        dialog.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                manager.createCategory(name.trim());
                loadData();
            }
        });
    }

    @FXML
    private void handleDeleteCategory() {
        Category selected = lstCategories.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showWarning("No Selection", "Please select a category to delete.");
            return;
        }

        Alert alert = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Delete category '" + selected.getName() + "'?",
                ButtonType.YES, ButtonType.NO
        );

        alert.showAndWait();

        if (alert.getResult() == ButtonType.YES) {
            manager.deleteCategory(selected);
            loadData();
        }
    }

    // ---------- Movie actions ----------

    /**
     * Saves a movie:
     * - If in edit mode -> update
     * - Else -> create
     */
    private void handleSave() {
        try {
            String title = txtTitle.getText();
            String imdbStr = txtImdb.getText();
            String personalStr = txtPersonal.getText();
            String filePath = txtFile.getText();

            List<Category> selectedCats = new ArrayList<>(lstCategories.getSelectionModel().getSelectedItems());

            // Required fields validation
            if (title == null || title.isBlank()
                    || imdbStr == null || imdbStr.isBlank()
                    || personalStr == null || personalStr.isBlank()
                    || filePath == null || filePath.isBlank()
                    || selectedCats.isEmpty()) {
                showWarning("Missing Info", "Please fill in all fields (Title, Ratings, Category, File).");
                return;
            }

            // Hard file extension validation (user can paste paths manually)
            String lower = filePath.trim().toLowerCase();
            if (!(lower.endsWith(".mp4") || lower.endsWith(".mpeg4"))) {
                showWarning("Invalid file type", "Only .mp4 or .mpeg4 files are allowed.");
                return;
            }

            double imdb = Double.parseDouble(imdbStr.replace(",", "."));
            double personal = Double.parseDouble(personalStr.replace(",", "."));

            if (movieInEditMode != null) {
                // Update existing movie
                movieInEditMode.setTitle(title.trim());
                movieInEditMode.setImdbRating(imdb);
                movieInEditMode.setPersonalRating(personal);
                movieInEditMode.setFileLink(filePath.trim());

                manager.updateMovie(movieInEditMode, selectedCats);
                movieInEditMode = null;
                lblStatus.setText("Movie updated");
            } else {
                // Create new movie
                manager.createMovie(title.trim(), imdb, personal, filePath.trim(), selectedCats);
                lblStatus.setText("Movie created");
            }

            loadData();
            clearFields();

        } catch (NumberFormatException e) {
            showWarning("Invalid Number", "Ratings must be numbers (e.g., 8.5).");
        } catch (Exception e) {
            showError("Error", "Could not save movie.\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Loads the selected movie into the input fields so it can be edited.
     */
    private void handleEdit() {
        Movie selected = tblMovies.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showWarning("No Selection", "Please select a movie to edit.");
            return;
        }

        movieInEditMode = selected;

        txtTitle.setText(selected.getTitle());
        txtImdb.setText(String.valueOf(selected.getImdbRating()));
        txtPersonal.setText(String.valueOf(selected.getPersonalRating()));
        txtFile.setText(selected.getFileLink());

        // Select movie categories in the ListView
        lstCategories.getSelectionModel().clearSelection();

        for (Category listCat : lstCategories.getItems()) {
            for (Category movieCat : selected.getCategories()) {
                if (listCat.getId() == movieCat.getId()) {
                    lstCategories.getSelectionModel().select(listCat);
                }
            }
        }

        lblStatus.setText("Edit mode: " + selected.getTitle());
    }

    /**
     * Deletes the selected movie after confirmation.
     */
    private void handleDelete() {
        Movie selected = tblMovies.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showWarning("No Selection", "Please select a movie to delete.");
            return;
        }

        Alert alert = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Delete movie '" + selected.getTitle() + "'?",
                ButtonType.YES, ButtonType.NO
        );

        alert.showAndWait();

        if (alert.getResult() == ButtonType.YES) {
            manager.deleteMovie(selected);
            loadData();
            clearFields();
            lblStatus.setText("Movie deleted");
        }
    }

    /**
     * Resets all input fields and exits edit mode.
     */
    private void clearFields() {
        txtTitle.clear();
        txtImdb.clear();
        txtPersonal.clear();
        txtFile.clear();
        lstCategories.getSelectionModel().clearSelection();
        tblMovies.getSelectionModel().clearSelection();
        movieInEditMode = null;

        if (lblStatus != null) {
            lblStatus.setText("Ready");
        }
        updateBottomBar();
    }

    // ---------- Old movies warning ----------

    /**
     * Warns the user about movies that are:
     * - low rated (personal rating < 6)
     * - last viewed more than 2 years ago
     */
    private void checkOldMovies() {

        List<Movie> warningList = new ArrayList<>();
        LocalDate twoYearsAgo = LocalDate.now().minusYears(2);

        for (Movie m : masterData) {
            if (m.getPersonalRating() >= 6.0) continue;

            LocalDate lastViewDate = safeParseDate(m.getLastView());
            if (lastViewDate != null && lastViewDate.isBefore(twoYearsAgo)) {
                warningList.add(m);
            }
        }

        if (warningList.isEmpty()) return;

        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Delete Old Movies");
        alert.setHeaderText("These movies are low rated (<6) and haven't been watched in 2 years:");

        StringBuilder sb = new StringBuilder();
        for (Movie m : warningList) {
            sb.append("• ").append(m.getTitle())
                    .append(" (Last viewed: ").append(m.getLastView()).append(")\n");
        }

        TextArea area = new TextArea(sb.toString());
        area.setEditable(false);
        area.setWrapText(true);
        area.setMaxWidth(Double.MAX_VALUE);
        area.setMaxHeight(Double.MAX_VALUE);

        alert.getDialogPane().setContent(area);
        alert.showAndWait();
    }

    /**
     * Safe date parsing for values like:
     * - "YYYY-MM-DD"
     * - "YYYY-MM-DD HH:mm:ss"
     */
    private LocalDate safeParseDate(String raw) {
        if (raw == null) return null;

        String s = raw.trim();
        if (s.isBlank()) return null;

        if (s.length() >= 10) {
            s = s.substring(0, 10);
        }

        try {
            return LocalDate.parse(s);
        } catch (Exception e) {
            return null;
        }
    }

    // ---------- Bottom bar ----------

    /**
     * Updates the bottom bar information:
     * - total movies
     * - visible movies after filtering
     */
    private void updateBottomBar() {
        if (lblCount == null || filteredData == null) return;

        int total = masterData.size();
        int visible = filteredData.size();

        lblCount.setText("Showing " + visible + " / " + total);

        if (lblStatus != null && (txtSearch.getText() != null || txtMinImdb.getText() != null)) {
            // Light feedback only, no need to spam status
        }
    }

    // ---------- Alerts ----------

    private void showWarning(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
