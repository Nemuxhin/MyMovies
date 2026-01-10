package dal;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class ConnectionProvider {

    private static final String URL = "jdbc:sqlite:moviecollection.db"; // Changed DB name

    public ConnectionProvider() {
        createTables();
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    private void createTables() {
        // 1. Movie Table (Added ratings and lastview)
        String sqlMovie = """
            CREATE TABLE IF NOT EXISTS Movie (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT,
                rating REAL,
                imdb_rating REAL,
                filelink TEXT,
                lastview TEXT
            );
        """;

        // 2. Category Table
        String sqlCategory = """
            CREATE TABLE IF NOT EXISTS Category (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT
            );
        """;

        // 3. CatMovie Junction Table (The Many-to-Many Link)
        String sqlCatMovie = """
            CREATE TABLE IF NOT EXISTS CatMovie (
                movie_id INTEGER,
                category_id INTEGER,
                FOREIGN KEY (movie_id) REFERENCES Movie(id),
                FOREIGN KEY (category_id) REFERENCES Category(id)
            );
        """;

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sqlMovie);
            stmt.execute(sqlCategory);
            stmt.execute(sqlCatMovie);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}