package be;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Movie entity in the system.
 * This class belongs to the Business Entity (BE) layer and contains
 * only data, not application logic.
 */
public class Movie {

    // Unique identifier of the movie (primary key in the database)
    private int id;

    // Movie title
    private String title;

    // Personal rating given by the user
    private double personalRating;

    // Official IMDB rating
    private double imdbRating;

    // Path to the movie file on disk
    private String fileLink;

    // Date of last view (stored as String, parsed later when needed)
    private String lastView;

    // List of categories associated with this movie (Action, Horror, etc.)
    private List<Category> categories = new ArrayList<>();

    /**
     * Creates a new Movie object.
     *
     * @param id             movie identifier
     * @param title          movie title
     * @param personalRating user rating
     * @param imdbRating     IMDB rating
     * @param fileLink       path to the video file
     * @param lastView       date when the movie was last viewed
     */
    public Movie(int id,
                 String title,
                 double personalRating,
                 double imdbRating,
                 String fileLink,
                 String lastView) {

        this.id = id;
        this.title = title;
        this.personalRating = personalRating;
        this.imdbRating = imdbRating;
        this.fileLink = fileLink;
        this.lastView = lastView;
    }

    // ---------- GETTERS ----------

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public double getPersonalRating() {
        return personalRating;
    }

    public double getImdbRating() {
        return imdbRating;
    }

    public String getFileLink() {
        return fileLink;
    }

    /**
     * Returns the last view date as a String.
     * The conversion to LocalDate is handled outside the BE layer.
     */
    public String getLastView() {
        return lastView;
    }

    public List<Category> getCategories() {
        return categories;
    }

    // ---------- SETTERS ----------

    public void setId(int id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setPersonalRating(double personalRating) {
        this.personalRating = personalRating;
    }

    public void setImdbRating(double imdbRating) {
        this.imdbRating = imdbRating;
    }

    public void setFileLink(String fileLink) {
        this.fileLink = fileLink;
    }

    /**
     * Updates the last view date.
     */
    public void setLastView(String lastView) {
        this.lastView = lastView;
    }

    /**
     * Replaces the list of categories associated with this movie.
     */
    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }

    /**
     * Returns all category names as a single comma-separated string.
     * This method is mainly used for displaying categories in the UI
     * (e.g. inside a TableView column).
     */
    public String getCategoriesAsString() {
        if (categories == null || categories.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (Category c : categories) {
            sb.append(c.getName()).append(", ");
        }

        // Remove the last comma and space
        sb.setLength(sb.length() - 2);

        return sb.toString();
    }
}
