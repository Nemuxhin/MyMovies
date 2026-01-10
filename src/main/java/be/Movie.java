package be;

import java.util.ArrayList;
import java.util.List;

public class Movie {
    private final int id;
    private final String title;
    private final double personalRating;
    private final double imdbRating;
    private final String fileLink;
    private final String lastView; // Stored as String (YYYY-MM-DD)

    // Many-to-Many representation
    private final List<Category> categories = new ArrayList<>();

    public Movie(int id, String title, double personalRating, double imdbRating, String fileLink, String lastView) {
        this.id = id;
        this.title = title;
        this.personalRating = personalRating;
        this.imdbRating = imdbRating;
        this.fileLink = fileLink;
        this.lastView = lastView;
    }

    // Getters and Setters
    public int getId() { return id; }
    public String getTitle() { return title; }
    public double getPersonalRating() { return personalRating; }
    public double getImdbRating() { return imdbRating; }
    public String getFileLink() { return fileLink; }
    public String getLastView() { return lastView; }

    public List<Category> getCategories() { return categories; }

    // Helper to display categories in a TableView cell (e.g., "Action, Drama")
    public String getCategoriesAsString() {
        StringBuilder sb = new StringBuilder();
        for (Category c : categories) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(c.getName());
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return title;
    }
}