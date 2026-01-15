package be;

/**
 * Represents a movie category.
 * Each category has a unique identifier and a readable name.
 * This class belongs to the Business Entity (BE) layer.
 */
public class Category {

    // Unique identifier of the category (from the database)
    private final int id;

    // Human-readable name of the category (e.g. Drama, Action, Comedy)
    private String name;

    /**
     * Creates a new Category object.
     *
     * @param id   unique identifier of the category
     * @param name name of the category
     */
    public Category(int id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * Returns the unique ID of the category.
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the name of the category.
     */
    public String getName() {
        return name;
    }

    /**
     * Updates the category name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the category name instead of the default object representation.
     * This is especially useful when Category objects are shown in UI controls
     * such as ComboBox or ListView.
     */
    @Override
    public String toString() {
        return name;
    }
}
