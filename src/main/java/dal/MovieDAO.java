package dal;

import be.Category;
import be.Movie;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) responsible for database operations on Movies
 * and the Movie-Category relationship.
 *
 * This class belongs to the DAL layer and contains only database logic.
 */
public class MovieDAO {

    private final ConnectionProvider cp = new ConnectionProvider();

    // ---------- 1) GET ALL MOVIES ----------

    /**
     * Loads all movies and their categories.
     * The categories are loaded using the SAME connection to avoid opening a new connection per movie.
     */
    public List<Movie> getAllMovies() {
        List<Movie> movies = new ArrayList<>();

        // Avoid SELECT * and always use bracketed column names when they contain spaces.
        String sql = """
                SELECT ID,
                       Name,
                       [Site Rating],
                       [Personal Rating],
                       [File Link],
                       [Last View]
                FROM Movies
                """;

        try (Connection conn = cp.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                int id = rs.getInt("ID");
                String title = rs.getString("Name");

                double imdbRating = rs.getDouble("Site Rating");        // JDBC label matches the column alias/name
                double personalRating = rs.getDouble("Personal Rating");
                String fileLink = rs.getString("File Link");

                // Read last view as DATE when possible (safer than parsing random datetime strings)
                // If the DB column is DATETIME, getDate() still works and returns only the date part.
                Date lastViewDate = rs.getDate("Last View");
                String lastView = (lastViewDate != null) ? lastViewDate.toString() : "";

                Movie movie = new Movie(id, title, personalRating, imdbRating, fileLink, lastView);

                // Load categories for this movie using the same connection
                movie.setCategories(getCategoriesForMovie(conn, id));

                movies.add(movie);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return movies;
    }

    /**
     * Loads categories for a single movie.
     * Uses the same open connection to keep the operation efficient.
     */
    private List<Category> getCategoriesForMovie(Connection conn, int movieId) throws SQLException {
        List<Category> categories = new ArrayList<>();

        String sql = """
                SELECT c.ID, c.Name
                FROM Categories c
                INNER JOIN CategMovie cm ON c.ID = cm.CategID
                WHERE cm.MovieID = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, movieId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    categories.add(new Category(rs.getInt("ID"), rs.getString("Name")));
                }
            }
        }

        return categories;
    }

    // ---------- 2) CREATE MOVIE ----------

    /**
     * Creates a new movie and its category relationships.
     * A transaction is used so we never end up with a movie without categories (or vice versa).
     */
    public Movie createMovie(Movie movie, List<Category> categories) {

        String sqlInsertMovie = """
                INSERT INTO Movies (Name, [Site Rating], [Personal Rating], [File Link], [Last View])
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection conn = cp.getConnection()) {

            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(sqlInsertMovie, Statement.RETURN_GENERATED_KEYS)) {

                ps.setString(1, movie.getTitle());
                ps.setDouble(2, movie.getImdbRating());
                ps.setDouble(3, movie.getPersonalRating());
                ps.setString(4, movie.getFileLink());

                // Store last view as DATE if present, otherwise NULL.
                // This avoids parsing issues later and keeps DB values clean.
                setNullableDate(ps, 5, movie.getLastView());

                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        int newId = keys.getInt(1);
                        movie.setId(newId);

                        // Save relationships (MovieID <-> CategoryID)
                        addCategoriesToMovie(conn, newId, categories);

                        conn.commit();
                        return movie;
                    } else {
                        // No generated key means the Movies.ID is not identity or key retrieval failed
                        conn.rollback();
                        throw new SQLException("Movie created but no generated ID was returned.");
                    }
                }

            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return movie;
        }
    }

    /**
     * Inserts rows into the relationship table (CategMovie).
     * Uses the same connection and does not open a new one.
     */
    private void addCategoriesToMovie(Connection conn, int movieId, List<Category> categories) throws SQLException {

        if (categories == null || categories.isEmpty()) {
            return; // No categories selected, nothing to insert
        }

        String sql = "INSERT INTO CategMovie (MovieID, CategID) VALUES (?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Category cat : categories) {
                ps.setInt(1, movieId);
                ps.setInt(2, cat.getId());
                ps.addBatch();
            }
            ps.executeBatch(); // Batch insert is faster and cleaner
        }
    }

    // ---------- 3) DELETE MOVIE ----------

    /**
     * Deletes a movie and its relationships.
     * A transaction is used to keep the database consistent.
     */
    public void deleteMovie(Movie movie) {

        String sqlRel = "DELETE FROM CategMovie WHERE MovieID = ?";
        String sqlMov = "DELETE FROM Movies WHERE ID = ?";

        try (Connection conn = cp.getConnection()) {

            conn.setAutoCommit(false);

            try (PreparedStatement psRel = conn.prepareStatement(sqlRel);
                 PreparedStatement psMov = conn.prepareStatement(sqlMov)) {

                psRel.setInt(1, movie.getId());
                psRel.executeUpdate();

                psMov.setInt(1, movie.getId());
                psMov.executeUpdate();

                conn.commit();

            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ---------- 4) UPDATE MOVIE ----------

    /**
     * Updates movie data and replaces its category relations.
     * We delete old relations and insert the new ones (simple and reliable).
     */
    public void updateMovie(Movie movie, List<Category> categories) {

        String sqlUpdate = """
                UPDATE Movies
                SET Name = ?,
                    [Site Rating] = ?,
                    [Personal Rating] = ?,
                    [File Link] = ?,
                    [Last View] = ?
                WHERE ID = ?
                """;

        String sqlDeleteLinks = "DELETE FROM CategMovie WHERE MovieID = ?";

        try (Connection conn = cp.getConnection()) {

            conn.setAutoCommit(false);

            try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate);
                 PreparedStatement psDelete = conn.prepareStatement(sqlDeleteLinks)) {

                psUpdate.setString(1, movie.getTitle());
                psUpdate.setDouble(2, movie.getImdbRating());
                psUpdate.setDouble(3, movie.getPersonalRating());
                psUpdate.setString(4, movie.getFileLink());

                // Store as DATE / NULL (no GETDATE string parsing problems later)
                setNullableDate(psUpdate, 5, movie.getLastView());

                psUpdate.setInt(6, movie.getId());
                psUpdate.executeUpdate();

                // Replace relationships
                psDelete.setInt(1, movie.getId());
                psDelete.executeUpdate();

                addCategoriesToMovie(conn, movie.getId(), categories);

                conn.commit();

            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ---------- 5) UPDATE LAST VIEW ----------

    /**
     * Updates the "Last View" column to today's date.
     * Using CAST(GETDATE() AS DATE) avoids time values and makes comparisons easier.
     */
    public void updateLastView(int movieId) {

        String sql = "UPDATE Movies SET [Last View] = CAST(GETDATE() AS DATE) WHERE ID = ?";

        try (Connection conn = cp.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, movieId);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ---------- Helper methods ----------

    /**
     * Sets a DATE parameter from a String in format YYYY-MM-DD.
     * If the string is null/blank/invalid, it stores NULL instead.
     */
    private void setNullableDate(PreparedStatement ps, int index, String dateString) throws SQLException {

        if (dateString == null || dateString.isBlank()) {
            ps.setNull(index, Types.DATE);
            return;
        }

        String value = dateString.trim();

        // If a datetime string sneaks in (YYYY-MM-DD HH:mm:ss), keep only the date part.
        if (value.length() >= 10) {
            value = value.substring(0, 10);
        }

        try {
            ps.setDate(index, Date.valueOf(value));
        } catch (IllegalArgumentException e) {
            // Invalid format -> store NULL instead of crashing
            ps.setNull(index, Types.DATE);
        }
    }
}
