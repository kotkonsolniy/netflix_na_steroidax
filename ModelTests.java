package com.kinoflix.kotik;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ModelTests {

    @Test
    void testUserToString() {
        HelloApplication.User user = new HelloApplication.User(42, "Alice", "alice@example.com");
        assertEquals("42: Alice (alice@example.com)", user.toString());
    }

    @Test
    void testMovieAverageRatingEmpty() {
        HelloApplication.Movie movie = new HelloApplication.Movie("Inception", "Nolan", 2010, List.of("sci-fi"));
        assertEquals(0.0, movie.averageRating());
    }

    @Test
    void testMovieAverageRatingWithValues() {
        HelloApplication.Movie movie = new HelloApplication.Movie("Inception", "Nolan", 2010, List.of("sci-fi"));
        movie.ratings.addAll(List.of(5, 4, 3));
        assertEquals(4.0, movie.averageRating(), 0.001);
    }

    @Test
    void testMovieToStringNotFavorite() {
        HelloApplication.Movie movie = new HelloApplication.Movie("Inception", "Nolan", 2010, List.of("sci-fi", "thriller"));
        movie.ratings.add(5);
        String result = movie.toString();
        assertFalse(result.startsWith("★ "));
        assertTrue(result.contains("Inception"));
        assertTrue(result.contains("5.0"));
    }

    @Test
    void testMovieToStringFavorite() {
        HelloApplication.Movie movie = new HelloApplication.Movie("Inception", "Nolan", 2010, List.of("sci-fi", "thriller"));
        movie.ratings.add(5);
        movie.favorite = true;
        String result = movie.toString();
        assertTrue(result.startsWith("★ "));
    }

    @Test
    void testAddComment() {
        HelloApplication.Movie movie = new HelloApplication.Movie("Dune", "Villeneuve", 2021, List.of("sci-fi"));
        movie.comments.add("Great movie!");
        assertEquals(1, movie.comments.size());
        assertEquals("Great movie!", movie.comments.get(0));
    }
}
