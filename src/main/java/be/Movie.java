package be;

import java.util.ArrayList;
import java.util.List;

public class Movie {
    private int id;
    private String title;
    private double personalRating;
    private double imdbRating;
    private String fileLink;
    private String lastView;

    // This list holds the categories (Action, Horror, etc.)
    private List<Category> categories = new ArrayList<>();

    public Movie(int id, String title, double personalRating, double imdbRating, String fileLink, String lastView) {
        this.id = id;
        this.title = title;
        this.personalRating = personalRating;
        this.imdbRating = imdbRating;
        this.fileLink = fileLink;
        this.lastView = lastView;
    }

    // --- GETTERS ---
    public int getId() { return id; }
    public String getTitle() { return title; }
    public double getPersonalRating() { return personalRating; }
    public double getImdbRating() { return imdbRating; }
    public String getFileLink() { return fileLink; }
    public String getLastView() { return lastView; }
    public List<Category> getCategories() { return categories; }

    public void setId(int id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setPersonalRating(double personalRating) { this.personalRating = personalRating; }
    public void setImdbRating(double imdbRating) { this.imdbRating = imdbRating; }
    public void setFileLink(String fileLink) { this.fileLink = fileLink; }
    public void setLastView(String lastView) { this.lastView = lastView; }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }

    public String getCategoriesAsString() {
        if (categories.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Category c : categories) {
            sb.append(c.getName()).append(", ");
        }
        if (sb.length() > 0) sb.setLength(sb.length() - 2);
        return sb.toString();
    }
}