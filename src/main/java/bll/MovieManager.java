package bll;

import be.Movie;
import dal.MovieDAO;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import be.Category;
import dal.CategoryDAO;

public class MovieManager {
    private MovieDAO movieDAO = new MovieDAO();
    private CategoryDAO categoryDAO = new CategoryDAO();

    public List<Movie> getAllMovies() {
        return movieDAO.getAllMovies();
    }

    public List<Category> getAllCategories() {
        return categoryDAO.getAllCategories();
    }

    /**
     * Checks for "Bad" movies according to assignment requirements
     */
    public List<Movie> getMoviesToDelete() {
        List<Movie> all = getAllMovies();
        List<Movie> warnings = new ArrayList<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Movie m : all) {
            // 1. Check Rating
            if (m.getPersonalRating() < 6.0) {
                // 2. Check Date
                try {
                    if (m.getLastView() != null && !m.getLastView().isEmpty()) {
                        LocalDate lastViewDate = LocalDate.parse(m.getLastView(), formatter);
                        long yearsBetween = ChronoUnit.YEARS.between(lastViewDate, today);

                        if (yearsBetween >= 2) {
                            warnings.add(m);
                        }
                    }
                } catch (Exception e) {
                    // Handle date parsing errors (ignore or log)
                }
            }
        }
        return warnings;
    }
    public void createMovie(String title, double imdb, double personal, String fileLink, Category category) {
        Movie newMovie = new Movie(-1, title, personal, imdb, fileLink, "");
        // We pass a LIST of categories because the DB supports multiple
        List<Category> cats = new ArrayList<>();
        cats.add(category);

        movieDAO.createMovie(newMovie, cats);
    }

    public void deleteMovie(Movie movie) {
        movieDAO.deleteMovie(movie);
    }

    public List<Movie> searchMovies(String query) {
        List<Movie> allMovies = getAllMovies();
        List<Movie> searchResult = new ArrayList<>();

        for (Movie movie : allMovies) {
            // Check if title contains the query (case-insensitive)
            if (movie.getTitle().toLowerCase().contains(query.toLowerCase())) {
                searchResult.add(movie);
            }
        }
        return searchResult;
    }
}

