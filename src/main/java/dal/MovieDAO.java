package dal;

import be.Category;
import be.Movie;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MovieDAO {
    private final ConnectionProvider cp = new ConnectionProvider();

    public List<Movie> getAllMovies() {
        List<Movie> movies = new ArrayList<>();
        // Simple fetch. For performance, you might lazy-load categories,
        // but for a school project, we can fetch categories for each movie.
        String sql = "SELECT * FROM Movie";

        try (Connection conn = cp.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Movie m = new Movie(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getDouble("rating"),
                        rs.getDouble("imdb_rating"),
                        rs.getString("filelink"),
                        rs.getString("lastview")
                );
                // Populate categories for this movie
                m.getCategories().addAll(getCategoriesForMovie(m.getId(), conn));
                movies.add(m);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return movies;
    }

    // Helper method to fetch categories for a specific movie ID
    private List<Category> getCategoriesForMovie(int movieId, Connection conn) throws SQLException {
        List<Category> cats = new ArrayList<>();
        String sql = """
            SELECT c.id, c.name FROM Category c
            JOIN CatMovie cm ON c.id = cm.category_id
            WHERE cm.movie_id = ?
        """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, movieId);
            ResultSet rs = pstmt.executeQuery();
            while(rs.next()) {
                cats.add(new Category(rs.getInt("id"), rs.getString("name")));
            }
        }
        return cats;
    }

    public void createMovie(Movie movie, List<Category> selectedCategories) {
        String sql = "INSERT INTO Movie (title, rating, imdb_rating, filelink, lastview) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = cp.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, movie.getTitle());
            pstmt.setDouble(2, movie.getPersonalRating());
            pstmt.setDouble(3, movie.getImdbRating());
            pstmt.setString(4, movie.getFileLink());
            pstmt.setString(5, movie.getLastView());
            pstmt.executeUpdate();

            // 1. Get the generated Movie ID
            ResultSet rs = pstmt.getGeneratedKeys();
            int newMovieId = 0;
            if (rs.next()) {
                newMovieId = rs.getInt(1);
            }

            // 2. Insert relations into CatMovie
            if (newMovieId > 0 && selectedCategories != null) {
                String sqlRelation = "INSERT INTO CatMovie (movie_id, category_id) VALUES (?, ?)";
                try (PreparedStatement psCat = conn.prepareStatement(sqlRelation)) {
                    for (Category c : selectedCategories) {
                        psCat.setInt(1, newMovieId);
                        psCat.setInt(2, c.getId());
                        psCat.addBatch();
                    }
                    psCat.executeBatch();
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void updateLastView(int movieId) {
        // Logic to update lastview to current date
    }

    public void deleteMovie(Movie movie) {
        // 1. Delete the category links first
        String sqlRec = "DELETE FROM CatMovie WHERE movie_id = ?";
        // 2. Delete the movie itself
        String sqlMov = "DELETE FROM Movie WHERE id = ?";

        try (Connection conn = cp.getConnection()) {
            // ... (We delete relations first to avoid database errors) ...
            try (PreparedStatement stmtRec = conn.prepareStatement(sqlRec);
                 PreparedStatement stmtMov = conn.prepareStatement(sqlMov)) {

                stmtRec.setInt(1, movie.getId());
                stmtRec.executeUpdate();

                stmtMov.setInt(1, movie.getId());
                stmtMov.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}