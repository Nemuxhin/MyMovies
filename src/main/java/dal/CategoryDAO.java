package dal;

import be.Category;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAO {

    private ConnectionProvider cp = new ConnectionProvider();

    public List<Category> getAllCategories() {
        List<Category> allCategories = new ArrayList<>();
        String sql = "SELECT * FROM Categories";

        try (Connection conn = cp.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("ID");
                String name = rs.getString("Name");
                allCategories.add(new Category(id, name));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return allCategories;
    }

    public void createCategory(String name) {
        int nextId = 1;
        try (Connection conn = cp.getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT MAX(ID) FROM Categories");
            if (rs.next()) {
                nextId = rs.getInt(1) + 1;
            }
        } catch (SQLException e) { e.printStackTrace(); }

        String sql = "INSERT INTO Categories (ID, Name) VALUES (?, ?)";
        try (Connection conn = cp.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, nextId);
            pstmt.setString(2, name);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteCategory(Category category) {
        // FIX: Table name updated to 'CategMovie'
        String sqlRel = "DELETE FROM CategMovie WHERE CategID = ?";
        String sqlCat = "DELETE FROM Categories WHERE ID = ?";

        try (Connection conn = cp.getConnection()) {
            try (PreparedStatement psRel = conn.prepareStatement(sqlRel)) {
                psRel.setInt(1, category.getId());
                psRel.executeUpdate();
            }
            try (PreparedStatement psCat = conn.prepareStatement(sqlCat)) {
                psCat.setInt(1, category.getId());
                psCat.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}