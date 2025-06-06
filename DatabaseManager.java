package com.kinoflix.kotik;

import java.sql.*;
import java.util.*;

public class DatabaseManager {

    private static Connection conn;

    // Подключение к базе и создание таблиц
    public static void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:kinoflix.db");
            createTablesIfNotExist();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Создание таблиц users, movies и reviews (если не существуют)
    private static void createTablesIfNotExist() throws SQLException {
        String userTable = """
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY,
                    name TEXT NOT NULL,
                    email TEXT NOT NULL UNIQUE
                );
                """;

        String movieTable = """
                CREATE TABLE IF NOT EXISTS movies (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    director TEXT NOT NULL,
                    year INTEGER NOT NULL,
                    tags TEXT,
                    ratings TEXT
                );
                """;

        String reviewTable = """
                CREATE TABLE IF NOT EXISTS reviews (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    movie_id INTEGER NOT NULL,
                    user_id INTEGER NOT NULL,
                    comment TEXT NOT NULL,
                    FOREIGN KEY(movie_id) REFERENCES movies(id),
                    FOREIGN KEY(user_id) REFERENCES users(id)
                );
                """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(userTable);
            stmt.execute(movieTable);
            stmt.execute(reviewTable);
        }
    }

    // Загрузка всех пользователей из БД
    public static List<HelloApplication.User> loadUsers() {
        List<HelloApplication.User> users = new ArrayList<>();
        String sql = "SELECT id, name, email FROM users";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(new HelloApplication.User(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    // Получить id фильма по его уникальным полям
    private static Integer getMovieId(HelloApplication.Movie movie) {
        String sql = "SELECT id FROM movies WHERE title = ? AND director = ? AND year = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, movie.title);
            ps.setString(2, movie.director);
            ps.setInt(3, movie.year);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    // Загрузка всех фильмов из БД (с рейтингами)
    public static List<HelloApplication.Movie> loadMovies() {
        List<HelloApplication.Movie> movies = new ArrayList<>();
        String sql = "SELECT id, title, director, year, tags, ratings FROM movies";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String title = rs.getString("title");
                String director = rs.getString("director");
                int year = rs.getInt("year");

                List<String> tags = Optional.ofNullable(rs.getString("tags"))
                        .map(t -> Arrays.stream(t.split(","))
                                .map(String::trim)
                                .toList())
                        .orElse(new ArrayList<>());

                List<Integer> ratings = new ArrayList<>();
                String ratingsStr = rs.getString("ratings");
                if (ratingsStr != null && !ratingsStr.isEmpty()) {
                    for (String r : ratingsStr.split(",")) {
                        try {
                            ratings.add(Integer.parseInt(r));
                        } catch (NumberFormatException ignored) {}
                    }
                }

                HelloApplication.Movie movie = new HelloApplication.Movie(title, director, year, tags);
                movie.ratings = ratings;
                movies.add(movie);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return movies;
    }

    // Загрузка отзывов для конкретного фильма
    public static List<String> loadReviews(HelloApplication.Movie movie) {
        List<String> comments = new ArrayList<>();
        Integer movieId = getMovieId(movie);
        if (movieId == null) return comments;

        String sql = """
            SELECT u.name, r.comment FROM reviews r
            JOIN users u ON r.user_id = u.id
            WHERE r.movie_id = ?
            ORDER BY r.id DESC
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String userName = rs.getString("name");
                    String comment = rs.getString("comment");
                    comments.add(userName + ": " + comment);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return comments;
    }

    // Добавление пользователя в БД
    public static void addUser(HelloApplication.User user) {
        String sql = "INSERT INTO users (id, name, email) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, user.id);
            ps.setString(2, user.name);
            ps.setString(3, user.email);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Добавление фильма в БД
    public static void addMovie(HelloApplication.Movie movie) {
        String sql = "INSERT INTO movies (title, director, year, tags, ratings) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, movie.title);
            ps.setString(2, movie.director);
            ps.setInt(3, movie.year);
            ps.setString(4, String.join(",", movie.tags));
            ps.setString(5, movie.ratings.isEmpty() ? "" : String.join(",", movie.ratings.stream().map(String::valueOf).toList()));
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Удаление фильма из БД по уникальным полям (title, director, year)
    public static void deleteMovie(HelloApplication.Movie movie) {
        Integer movieId = getMovieId(movie);
        if (movieId != null) {
            // Сначала удалить отзывы к этому фильму
            String deleteReviewsSql = "DELETE FROM reviews WHERE movie_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(deleteReviewsSql)) {
                ps.setInt(1, movieId);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        String sql = "DELETE FROM movies WHERE title = ? AND director = ? AND year = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, movie.title);
            ps.setString(2, movie.director);
            ps.setInt(3, movie.year);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Обновление рейтингов фильма (удаляем и заново добавляем фильм — можно оптимизировать)
    public static void updateMovieRatings(HelloApplication.Movie movie) {
        Integer movieId = getMovieId(movie);
        if (movieId == null) return;

        String sql = "UPDATE movies SET ratings = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, movie.ratings.isEmpty() ? "" : String.join(",", movie.ratings.stream().map(String::valueOf).toList()));
            ps.setInt(2, movieId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Добавление отзыва к фильму
    public static void addReview(HelloApplication.Movie movie, HelloApplication.User user, String comment) {
        Integer movieId = getMovieId(movie);
        if (movieId == null) return;
        String sql = "INSERT INTO reviews (movie_id, user_id, comment) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            ps.setInt(2, user.id);
            ps.setString(3, comment);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    // Получить максимальный ID пользователя для генерации нового
    public static int getMaxUserId() {
        String sql = "SELECT MAX(id) AS max_id FROM users";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("max_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // Проверить, есть ли пользователь с таким email
    public static boolean isEmailExists(String email) {
        String sql = "SELECT COUNT(*) AS cnt FROM users WHERE email = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("cnt") > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Получить пользователя по email
    public static HelloApplication.User getUserByEmail(String email) {
        String sql = "SELECT id, name, email FROM users WHERE email = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new HelloApplication.User(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("email")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
