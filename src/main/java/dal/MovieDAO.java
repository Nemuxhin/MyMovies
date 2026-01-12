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
        String sql = "SELECT * FROM Movies";

        try (Connection conn = cp.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Movie m = new Movie(
                        rs.getInt("ID"),                    // Matches DB "ID"
                        rs.getString("Name"),               // Matches DB "Name"
                        rs.getDouble("Personal Rating"),    // Matches DB "Personal Rating"
                        rs.getDouble("Site Rating"),        // Matches DB "Site Rating"
                        rs.getString("File Link"),          // Matches DB "File Link"
                        rs.getString("Last View")           // Matches DB "Last View"
                );
                // Populate categories for this movie
                m.getCategories().addAll(getCategoriesForMovie(m.getId(), conn));
                movies.add(m);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return movies;
    }

    private List<Category> getCategoriesForMovie(int movieId, Connection conn) throws SQLException {
        List<Category> cats = new ArrayList<>();
        // Updated table and column names (CategID, MovieID)
        String sql = """
            SELECT c.ID, c.Name FROM Categories c
            JOIN CategMovie cm ON c.ID = cm.CategID
            WHERE cm.MovieID = ?
        """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, movieId);
            ResultSet rs = pstmt.executeQuery();
            while(rs.next()) {
                cats.add(new Category(rs.getInt("ID"), rs.getString("Name")));
            }
        }
        return cats;
    }

    public void createMovie(Movie movie, List<Category> selectedCategories) {
        String sql = "INSERT INTO Movies (Name, [Personal Rating], [Site Rating], [File Link], [Last View]) VALUES (?, ?, ?, ?, ?)";

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

            // 2. Insert relations into CategMovie
            if (newMovieId > 0 && selectedCategories != null) {
                String sqlRelation = "INSERT INTO CategMovie (MovieID, CategID) VALUES (?, ?)";
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
        String sql = "UPDATE Movies SET [Last View] = ? WHERE ID = ?";

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        String currentDate = sdf.format(new java.util.Date());

        try (Connection conn = cp.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, currentDate);
            pstmt.setInt(2, movieId);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void deleteMovie(Movie movie) {
        String sqlRec = "DELETE FROM CategMovie WHERE MovieID = ?";
        String sqlMov = "DELETE FROM Movies WHERE ID = ?";

        try (Connection conn = cp.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement stmtRec = conn.prepareStatement(sqlRec);
                 PreparedStatement stmtMov = conn.prepareStatement(sqlMov)) {

                stmtRec.setInt(1, movie.getId());
                stmtRec.executeUpdate();

                stmtMov.setInt(1, movie.getId());
                stmtMov.executeUpdate();

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}