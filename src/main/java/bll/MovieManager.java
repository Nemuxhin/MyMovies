package bll;

import be.Category;
import be.Movie;
import dal.CategoryDAO;
import dal.MovieDAO;
import java.util.List;

public class MovieManager {

    private MovieDAO movieDao = new MovieDAO();
    private CategoryDAO categoryDao = new CategoryDAO();

    public List<Movie> getAllMovies() {
        return movieDao.getAllMovies();
    }

    public List<Category> getAllCategories() {
        return categoryDao.getAllCategories();
    }

    // --- 1. CREATE MOVIE
    public void createMovie(String title, double imdb, double personal, String fileLink, List<Category> categories) {
        System.out.println("MANAGER: Received request to create '" + title + "' with " + categories.size() + " categories.");

        Movie newMovie = new Movie(-1, title, personal, imdb, fileLink, "");

        System.out.println("MANAGER: Passing to DAO now...");

        // Pass the list directly to the DAO
        movieDao.createMovie(newMovie, categories);

        System.out.println("MANAGER: DAO finished.");
    }

    // --- 2. UPDATE MOVIE ---
    public void updateMovie(Movie movie, List<Category> categories) {
        movieDao.updateMovie(movie, categories);
    }

    // --- 3. DELETE MOVIE ---
    public void deleteMovie(Movie movie) {
        movieDao.deleteMovie(movie);
    }

    // --- 4. CATEGORY HANDLING ---
    public void createCategory(String name) {
        categoryDao.createCategory(name);
    }

    public void deleteCategory(Category category) {
        categoryDao.deleteCategory(category);
    }

    // --- 5. SEARCH ---
    public List<Movie> searchMovies(String query) {
        List<Movie> allMovies = getAllMovies();
        List<Movie> searchResult = new java.util.ArrayList<>();

        for (Movie m : allMovies) {
            if (m.getTitle().toLowerCase().contains(query.toLowerCase())) {
                searchResult.add(m);
            }
        }
        return searchResult;
    }

    public void updateLastView(int id) {
        movieDao.updateLastView(id);
    }
}