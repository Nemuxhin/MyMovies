package dal;

import be.Category;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) responsible for CRUD operations on Categories.
 * This class belongs to the DAL layer and should contain only database logic.
 */
public class CategoryDAO {

    private final ConnectionProvider cp = new ConnectionProvider();

    /**
     * Loads all categories from the database.
     */
    public List<Category> getAllCategories() {
        List<Category> allCategories = new ArrayList<>();
        String sql = "SELECT ID, Name FROM Categories";

        try (Connection conn = cp.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("ID");
                String name = rs.getString("Name");
                allCategories.add(new Category(id, name));
            }

        } catch (SQLException e) {
            // In a real project you would throw a custom exception or log properly
            e.printStackTrace();
        }

        return allCategories;
    }

    /**
     * Creates a new category in the database.
     * The database is responsible for generating the ID (IDENTITY / auto-increment).
     */
    public void createCategory(String name) {
        String sql = "INSERT INTO Categories (Name) VALUES (?)";

        try (Connection conn = cp.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Deletes a category and removes its relations to movies.
     * First we delete from the relation table, then from the Categories table.
     */
    public void deleteCategory(Category category) {
        String sqlRel = "DELETE FROM CategMovie WHERE CategID = ?";
        String sqlCat = "DELETE FROM Categories WHERE ID = ?";

        try (Connection conn = cp.getConnection()) {

            // Use a transaction to ensure both deletes succeed or both are rolled back
            conn.setAutoCommit(false);

            try (PreparedStatement psRel = conn.prepareStatement(sqlRel);
                 PreparedStatement psCat = conn.prepareStatement(sqlCat)) {

                psRel.setInt(1, category.getId());
                psRel.executeUpdate();

                psCat.setInt(1, category.getId());
                psCat.executeUpdate();

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
}
