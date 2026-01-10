package dal;

import be.Category;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAO {
    private final ConnectionProvider cp = new ConnectionProvider();

    public List<Category> getAllCategories() {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT * FROM Category";

        try (Connection conn = cp.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                categories.add(new Category(rs.getInt("id"), rs.getString("name")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // If DB is empty, return defaults so the UI isn't broken
        if (categories.isEmpty()) {
            categories.add(new Category(-1, "Action"));
            categories.add(new Category(-1, "Drama"));
            categories.add(new Category(-1, "Comedy"));
        }
        return categories;
    }
}