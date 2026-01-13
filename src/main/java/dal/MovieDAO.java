package dal;

import be.Category;
import be.Movie;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MovieDAO {

    private ConnectionProvider cp = new ConnectionProvider();

    // --- 1. GET ALL MOVIES ---
    public List<Movie> getAllMovies() {
        List<Movie> allMovies = new ArrayList<>();
        String sql = "SELECT * FROM Movies";

        try (Connection conn = cp.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("ID");
                String title = rs.getString("Name");
                double imdbRating = rs.getDouble("Site Rating");
                double personalRating = rs.getDouble("Personal Rating");
                String fileLink = rs.getString("File Link");
                String lastView = rs.getString("Last View");

                Movie movie = new Movie(id, title, personalRating, imdbRating, fileLink, lastView);

                // Load categories for this movie
                movie.setCategories(getCategoriesForMovie(id));

                allMovies.add(movie);
            }
        } catch (SQLException e) {
            System.out.println("Error getting movies: " + e.getMessage());
            e.printStackTrace();
        }
        return allMovies;
    }

    // --- Helper: Get Categories ---
    private List<Category> getCategoriesForMovie(int movieId) {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT c.ID, c.Name FROM Categories c " +
                "JOIN CategMovie cm ON c.ID = cm.CategID " +
                "WHERE cm.MovieID = ?";

        try (Connection conn = cp.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, movieId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    categories.add(new Category(rs.getInt("ID"), rs.getString("Name")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return categories;
    }

    // --- 2. CREATE MOVIE ---
    public Movie createMovie(Movie movie, List<Category> categories) {
        String sql = "INSERT INTO Movies (Name, [Site Rating], [Personal Rating], [File Link], [Last View]) VALUES (?,?,?,?,?)";

        try (Connection conn = cp.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, movie.getTitle());
            pstmt.setDouble(2, movie.getImdbRating());
            pstmt.setDouble(3, movie.getPersonalRating());
            pstmt.setString(4, movie.getFileLink());
            pstmt.setString(5, movie.getLastView());
            pstmt.executeUpdate();

            // Get the new Movie ID
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int newMovieId = generatedKeys.getInt(1);
                    movie.setId(newMovieId);

                    System.out.println("DEBUG: Movie Created with ID: " + newMovieId);
                    System.out.println("DEBUG: Saving " + categories.size() + " categories...");

                    // SAVE CATEGORIES
                    addCategoriesToMovie(newMovieId, categories);
                } else {
                    System.out.println("DEBUG: Movie created, but NO ID was generated! Check 'Movies' table Identity settings.");
                }
            }
        } catch (SQLException e) {
            System.out.println("DEBUG: Error creating movie: " + e.getMessage());
            e.printStackTrace();
        }
        return movie;
    }

    private void addCategoriesToMovie(int movieId, List<Category> categories) {

        String sqlInsert = "INSERT INTO CategMovie (MovieID, CategID) VALUES (?, ?)";

        try (Connection conn = cp.getConnection()) {
            for (Category cat : categories) {
                try (PreparedStatement pstmt = conn.prepareStatement(sqlInsert)) {
                    pstmt.setInt(1, movieId);
                    pstmt.setInt(2, cat.getId());
                    pstmt.executeUpdate();
                    System.out.println("DEBUG: Saved Link -> Movie " + movieId + " + Cat " + cat.getId());
                } catch (SQLException e) {
                    System.err.println("!!! FAILED TO SAVE CATEGORY !!!");
                    System.err.println("Error: " + e.getMessage());

                    if (e.getMessage().contains("ID") && e.getMessage().contains("null")) {
                        System.out.println("DEBUG: Trying Plan B (Manual ID)...");
                        saveCategoryManualID(conn, movieId, cat.getId());
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void saveCategoryManualID(Connection conn, int movieId, int catId) {
        try {
            // 1. Get next ID
            int nextId = 1;
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT MAX(ID) FROM CategMovie")) {
                if (rs.next()) nextId = rs.getInt(1) + 1;
            }

            // 2. Insert with ID
            String sql = "INSERT INTO CategMovie (ID, MovieID, CategID) VALUES (?, ?, ?)";
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setInt(1, nextId);
                pst.setInt(2, movieId);
                pst.setInt(3, catId);
                pst.executeUpdate();
                System.out.println("DEBUG: Plan B Success!");
            }
        } catch (SQLException ex) {
            System.err.println("DEBUG: Plan B Failed too: " + ex.getMessage());
        }
    }

    // --- 3. DELETE MOVIE ---
    public void deleteMovie(Movie movie) {
        String sqlRel = "DELETE FROM CategMovie WHERE MovieID = ?";
        String sqlMov = "DELETE FROM Movies WHERE ID = ?";
        try (Connection conn = cp.getConnection()) {
            try (PreparedStatement psRel = conn.prepareStatement(sqlRel)) {
                psRel.setInt(1, movie.getId());
                psRel.executeUpdate();
            }
            try (PreparedStatement psMov = conn.prepareStatement(sqlMov)) {
                psMov.setInt(1, movie.getId());
                psMov.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- 4. UPDATE MOVIE ---
    public void updateMovie(Movie movie, List<Category> categories) {
        String sql = "UPDATE Movies SET Name=?, [Site Rating]=?, [Personal Rating]=?, [File Link]=?, [Last View]=? WHERE ID=?";

        try (Connection conn = cp.getConnection()) {
            // A. Update basic info
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, movie.getTitle());
                pstmt.setDouble(2, movie.getImdbRating());
                pstmt.setDouble(3, movie.getPersonalRating());
                pstmt.setString(4, movie.getFileLink());
                pstmt.setString(5, movie.getLastView());
                pstmt.setInt(6, movie.getId());
                pstmt.executeUpdate();
            }

            // B. Update Categories (Delete OLD -> Insert NEW)
            System.out.println("DEBUG: Updating categories for Movie ID " + movie.getId());

            // 1. Delete old links
            String sqlDelete = "DELETE FROM CategMovie WHERE MovieID = ?";
            try (PreparedStatement psDel = conn.prepareStatement(sqlDelete)) {
                psDel.setInt(1, movie.getId());
                psDel.executeUpdate();
            }

            // 2. Add new links
            addCategoriesToMovie(movie.getId(), categories);

        } catch (SQLException e) {
            System.out.println("Error updating movie: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- 5. UPDATE LAST VIEW ---
    public void updateLastView(int movieId) {
        String sql = "UPDATE Movies SET [Last View] = GETDATE() WHERE ID = ?";
        try (Connection conn = cp.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, movieId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}