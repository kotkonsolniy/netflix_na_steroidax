package com.kinoflix.kotik;


import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.*;

public class HelloApplication extends Application {

    static class User {
        int id;
        String name, email;

        public User(int id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }

        @Override
        public String toString() {
            return id + ": " + name + " (" + email + ")";
        }
    }

    static class Movie {
        String title, director;
        int year;
        List<String> tags;
        List<Integer> ratings = new ArrayList<>();
        boolean favorite = false;               // добавлено
        List<String> comments = new ArrayList<>();  // добавлено

        public Movie(String title, String director, int year, List<String> tags) {
            this.title = title;
            this.director = director;
            this.year = year;
            this.tags = tags;
        }

        public double averageRating() {
            return ratings.isEmpty() ? 0.0 : ratings.stream().mapToInt(i -> i).average().orElse(0);
        }

        @Override
        public String toString() {
            String favMark = favorite ? "★ " : "";
            return favMark + title + " (" + year + "), " + director + " [" + String.join(", ", tags) + "] ★" + String.format("%.1f", averageRating());
        }
    }


    List<Movie> movies = new ArrayList<>();

    @Override
    public void start(Stage primaryStage) {
        DatabaseManager.connect();
        showAuthWindow(primaryStage);
    }

    // Окно выбора: Вход или Регистрация
    private void showAuthWindow(Stage stage) {
        Label welcomeLabel = new Label("Добро пожаловать в KinoFlix");
        Button loginBtn = new Button("Войти");
        Button registerBtn = new Button("Зарегистрироваться");

        // Загрузка изображения из ресурсов
        ImageView logoView = null;
        try {
            javafx.scene.image.Image logo = new Image(getClass().getResource("/images/auto.jpg").toExternalForm());


            logoView = new ImageView(logo);
            logoView.setFitWidth(1290);  // ширина картинки
            logoView.setPreserveRatio(true); // сохраняем пропорции
            logoView.setSmooth(true);
        } catch (Exception e) {
            System.err.println("Ошибка загрузки изображения логотипа: " + e.getMessage());
        }

        VBox root;
        if (logoView != null) {
            root = new VBox(15, logoView, welcomeLabel, loginBtn, registerBtn);
        } else {
            root = new VBox(15, welcomeLabel, loginBtn, registerBtn);
        }
        root.setStyle("-fx-padding: 20; -fx-alignment: center;");

        Scene scene = new Scene(root, 1290, 1340);
        stage.setScene(scene);
        stage.setTitle("Авторизация");
        stage.show();

        loginBtn.setOnAction(e -> showLoginWindow(stage));
        registerBtn.setOnAction(e -> showRegistrationWindow(stage));
    }

    // Окно регистрации
    private void showRegistrationWindow(Stage stage) {
        TextField nameField = new TextField();
        nameField.setPromptText("Имя");

        TextField emailField = new TextField();
        emailField.setPromptText("Почта");

        Button registerBtn = new Button("Зарегистрироваться");
        Button backBtn = new Button("Назад");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red");

        VBox vbox = new VBox(10,
                new Label("Регистрация"), nameField, emailField,
                registerBtn, backBtn, errorLabel);
        vbox.setStyle("-fx-padding: 20");

        StackPane root = new StackPane();

        try {
            Image backgroundImage = new Image(getClass().getResource("/images/log.jpeg").toExternalForm());
            BackgroundSize backgroundSize = new BackgroundSize(100, 100, true, true, true, false);
            BackgroundImage background = new BackgroundImage(
                    backgroundImage,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER,
                    backgroundSize
            );
            root.setBackground(new Background(background));
        } catch (Exception e) {
            System.err.println("Ошибка загрузки фонового изображения (окно регистрации): " + e.getMessage());
        }

        root.getChildren().add(vbox);

        Scene scene = new Scene(root, 300, 250);
        stage.setScene(scene);
        stage.setTitle("Регистрация");
        stage.show();

        registerBtn.setOnAction(e -> {
            // твоя логика регистрации
        });

        backBtn.setOnAction(e -> showAuthWindow(stage));
    }

    // Окно входа
    private void showLoginWindow(Stage stage) {
        TextField emailField = new TextField();
        emailField.setPromptText("Почта");

        Button loginBtn = new Button("Войти");
        Button backBtn = new Button("Назад");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red");

        VBox vbox = new VBox(10,
                new Label("Вход"), emailField,
                loginBtn, backBtn, errorLabel);
        vbox.setStyle("-fx-padding: 20");

        StackPane root = new StackPane();

        try {
            Image backgroundImage = new Image(getClass().getResource("/images/in.jpeg").toExternalForm());
            BackgroundSize backgroundSize = new BackgroundSize(100, 100, true, true, true, false);
            BackgroundImage background = new BackgroundImage(
                    backgroundImage,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER,
                    backgroundSize
            );
            root.setBackground(new Background(background));
        } catch (Exception e) {
            System.err.println("Ошибка загрузки фонового изображения (окно входа): " + e.getMessage());
        }

        root.getChildren().add(vbox);

        Scene scene = new Scene(root, 300, 200);
        stage.setScene(scene);
        stage.setTitle("Вход");
        stage.show();

        loginBtn.setOnAction(e -> {
            String email = emailField.getText().trim();

            if (email.isEmpty()) {
                errorLabel.setText("Пожалуйста, введите почту.");
                return;
            }

            User user = DatabaseManager.getUserByEmail(email);
            if (user == null) {
                errorLabel.setText("Пользователь с такой почтой не найден.");
                return;
            }

            showMainWindow(stage, user);
        });

        backBtn.setOnAction(e -> showAuthWindow(stage));
    }

    // Главное окно приложения с фильмами
    private void showMainWindow(Stage stage, User currentUser) {
        movies = DatabaseManager.loadMovies();

        ListView<Movie> movieListView = new ListView<>();

        TextField titleField = new TextField();
        titleField.setPromptText("Название");

        TextField directorField = new TextField();
        directorField.setPromptText("Режиссёр");

        TextField yearField = new TextField();
        yearField.setPromptText("Год");

        TextField tagsField = new TextField();
        tagsField.setPromptText("Теги через запятую");

        Button addMovieBtn = new Button("Добавить фильм");
        addMovieBtn.setOnAction(e -> {
            try {
                String title = titleField.getText().trim();
                String director = directorField.getText().trim();
                int year = Integer.parseInt(yearField.getText().trim());
                List<String> tags = Arrays.stream(tagsField.getText().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();

                if (title.isEmpty() || director.isEmpty() || tags.isEmpty()) {
                    return;
                }

                Movie movie = new Movie(title, director, year, tags);
                movies.add(movie);
                DatabaseManager.addMovie(movie);
                refreshMovies(movieListView);

                titleField.clear();
                directorField.clear();
                yearField.clear();
                tagsField.clear();
            } catch (NumberFormatException ex) {
                // Обработка ошибки ввода года
            }
        });

        Button delMovieBtn = new Button("Удалить выбранный");
        delMovieBtn.setOnAction(e -> {
            Movie selected = movieListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                movies.remove(selected);
                DatabaseManager.deleteMovie(selected);
                refreshMovies(movieListView);
            }
        });

        TextField ratingField = new TextField();
        ratingField.setPromptText("Оценка (1-5)");

        Button rateBtn = new Button("Поставить оценку");
        rateBtn.setOnAction(e -> {
            Movie selected = movieListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                try {
                    int rating = Integer.parseInt(ratingField.getText().trim());
                    if (rating < 1 || rating > 5) return;
                    selected.ratings.add(rating);
                    DatabaseManager.deleteMovie(selected);
                    DatabaseManager.addMovie(selected);
                    refreshMovies(movieListView);
                    ratingField.clear();
                } catch (NumberFormatException ignored) {
                }
            }
        });

        // Кнопка "Избранное"
        Button favoriteBtn = new Button("Добавить в избранное / Убрать из избранного");
        favoriteBtn.setOnAction(e -> {
            Movie selected = movieListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                selected.favorite = !selected.favorite;
                DatabaseManager.deleteMovie(selected);
                DatabaseManager.addMovie(selected);
                refreshMovies(movieListView);
            }
        });

        TextField searchField = new TextField();
        searchField.setPromptText("Поиск по названию/тегам");

        Button searchBtn = new Button("Поиск");
        searchBtn.setOnAction(e -> {
            String query = searchField.getText().toLowerCase();
            movieListView.getItems().setAll(
                    movies.stream().filter(m ->
                            m.title.toLowerCase().contains(query) ||
                                    m.director.toLowerCase().contains(query) ||
                                    m.tags.stream().anyMatch(tag -> tag.toLowerCase().contains(query))
                    ).toList()
            );
        });

        Button recommendBtn = new Button("Рекомендации (по рейтингу)");
        recommendBtn.setOnAction(e -> {
            movieListView.getItems().setAll(
                    movies.stream()
                            .sorted(Comparator.comparingDouble(Movie::averageRating).reversed())
                            .limit(5)
                            .toList()
            );
        });

        Label welcomeLabel = new Label("Привет, " + currentUser.name + "!");
        Button logoutBtn = new Button("Выйти");
        logoutBtn.setOnAction(e -> showAuthWindow(stage));

        VBox leftBox = new VBox(10, welcomeLabel, logoutBtn);
        leftBox.setMinWidth(150);
        leftBox.setStyle("-fx-padding: 10");

        VBox movieBox = new VBox(5,
                titleField, directorField, yearField, tagsField,
                addMovieBtn, delMovieBtn, favoriteBtn,
                ratingField, rateBtn,
                new Label("Фильмы:"), movieListView);
        movieBox.setMinWidth(400);

        VBox searchBox = new VBox(5, searchField, searchBtn, recommendBtn);
        searchBox.setMinWidth(150);

        // --- Комментарии ---
        Label commentsLabel = new Label("Комментарии:");
        ListView<String> commentsList = new ListView<>();
        commentsList.setPrefHeight(150);

        TextField commentField = new TextField();
        commentField.setPromptText("Добавить комментарий...");

        Button addCommentBtn = new Button("Добавить комментарий");
        addCommentBtn.setOnAction(e -> {
            Movie selected = movieListView.getSelectionModel().getSelectedItem();
            if (selected != null && !commentField.getText().trim().isEmpty()) {
                selected.comments.add(commentField.getText().trim());
                // DatabaseManager.addReview(selected.comments);
                commentField.clear();
                commentsList.getItems().setAll(selected.comments);
            }
        });

        VBox commentsBox = new VBox(5, commentsLabel, commentsList, commentField, addCommentBtn);
        commentsBox.setMinWidth(300);
        commentsBox.setStyle("-fx-padding: 10; -fx-border-color: gray; -fx-border-radius: 5;");

        movieListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                commentsList.getItems().setAll(newSelection.comments);
            } else {
                commentsList.getItems().clear();
            }
        });

        HBox contentBox = new HBox(15, leftBox, movieBox, searchBox, commentsBox);
        contentBox.setStyle("-fx-padding: 20;");

        // Полупрозрачный белый фон под контентом
        StackPane overlayPane = new StackPane(contentBox);
        overlayPane.setStyle("-fx-background-color: rgba(255,255,255,0.5); -fx-background-radius: 12; -fx-padding: 20;");

        // Корневой StackPane с фоном
        StackPane root = new StackPane();
        try {
            javafx.scene.image.Image backgroundImage = new javafx.scene.image.Image(
                    getClass().getResource("/images/z.jpg").toExternalForm()
            );
            BackgroundSize backgroundSize = new BackgroundSize(100, 100, true, true, true, false);
            BackgroundImage background = new BackgroundImage(backgroundImage,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER,
                    backgroundSize);
            root.setBackground(new Background(background));
        } catch (Exception e) {
            System.err.println("Ошибка загрузки фонового изображения (главное окно): " + e.getMessage());
        }

        root.getChildren().add(overlayPane);

        Scene scene = new Scene(root, 1200, 600);
        stage.setScene(scene);
        stage.setTitle("KinoFlix — Платформа для просмотра фильмов");
        stage.show();

        refreshMovies(movieListView);
    }


    private void refreshMovies(ListView<Movie> movieListView) {
        movieListView.getItems().setAll(movies);
    }

    public static void main(String[] args) {
        launch();
    }
}
