package edu.connection3a36.Controller;

import edu.connection3a36.services.NewsService;
import edu.connection3a36.tools.SessionManager;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class FrontLayoutController implements Initializable {

    @FXML
    private BorderPane mainContainer;
    @FXML
    private Label lblUserName;
    @FXML
    private HBox newsTicker;
    @FXML
    private Pane tickerClipPane;

    private final NewsService newsService = new NewsService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (SessionManager.getInstance().getCurrentUser() != null) {
            String email = SessionManager.getInstance().getCurrentUser().getEmail();
            lblUserName.setText(email.split("@")[0]); // Afficher le début de l'email
        }

        // Configuration du News Ticker
        setupNewsTicker();

        // Charger la vue principale du Front-Office par défaut
        loadView("/AfficherParcours.fxml");
    }

    private void setupNewsTicker() {
        // Appliquer un clip pour empêcher le débordement
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(tickerClipPane.widthProperty());
        clip.heightProperty().bind(tickerClipPane.heightProperty());
        tickerClipPane.setClip(clip);

        // Charger les news de manière asynchrone
        new Thread(() -> {
            List<NewsService.NewsItem> news = newsService.getLatestTechNews();
            Platform.runLater(() -> {
                if (news.isEmpty()) {
                    Label fallback = new Label("⚠️  Aucune actualité disponible pour le moment — vérifiez votre connexion réseau.");
                    fallback.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13px;");
                    newsTicker.getChildren().add(fallback);
                } else {
                    for (NewsService.NewsItem item : news) {
                        // Séparateur visuel entre les articles
                        Label sep = new Label("   ●   ");
                        sep.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14px; -fx-font-weight: bold;");
                        newsTicker.getChildren().add(sep);

                        Label label = new Label(item.getTitle());
                        label.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 13px; -fx-font-weight: 500; -fx-cursor: hand;");

                        // Interaction au clic : ouvrir l'article dans le navigateur
                        label.setOnMouseClicked(event -> openBrowser(item.getUrl()));

                        // Effet de survol (hover)
                        label.setOnMouseEntered(e -> label.setStyle(
                                "-fx-text-fill: #e74c3c; -fx-font-size: 13px; -fx-font-weight: 700; -fx-underline: true; -fx-cursor: hand;"));
                        label.setOnMouseExited(e -> label.setStyle(
                                "-fx-text-fill: #2c3e50; -fx-font-size: 13px; -fx-font-weight: 500; -fx-cursor: hand;"));

                        newsTicker.getChildren().add(label);
                    }
                }

                // Démarrer l'animation une fois le layout calculé
                newsTicker.layoutBoundsProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal.getWidth() > 0 && newsTicker.getTranslateX() == 0) {
                        // Départ depuis la droite (hors champ)
                        newsTicker.setTranslateX(tickerClipPane.getWidth());
                        startTickerAnimation();
                    }
                });
            });
        }).start();
    }

    private void openBrowser(String url) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Timeline tickerTimeline;

    private void startTickerAnimation() {
        if (tickerTimeline != null) tickerTimeline.stop();

        tickerTimeline = new Timeline(new KeyFrame(Duration.millis(16), event -> {
            double currentX = newsTicker.getTranslateX() - 1.2;
            newsTicker.setTranslateX(currentX);

            // Réinitialiser une fois que tout le contenu est sorti à gauche
            double contentWidth = newsTicker.getBoundsInLocal().getWidth();
            if (currentX < -contentWidth) {
                newsTicker.setTranslateX(tickerClipPane.getWidth());
            }
        }));

        tickerTimeline.setCycleCount(Animation.INDEFINITE);
        tickerTimeline.play();
    }

    @FXML
    private void loadParcours() {
        loadView("/AfficherParcours.fxml");
    }

    @FXML
    private void loadProjets() {
        loadView("/AfficherProjetsGlobal.fxml");
    }

    @FXML
    private void loadSkillGap() {
        loadView("/SkillGap.fxml");
    }

    @FXML
    private void loadPricing() {
        loadView("/Pricing.fxml");
    }

    @FXML
    private void loadAbout() {
        loadView("/About.fxml");
    }

    @FXML
    private void logout() {
        SessionManager.getInstance().logout();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Connexion.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) mainContainer.getScene().getWindow();
            stage.setTitle("MentorAI - Connexion");

            // On s'assure que la connexion a une taille correcte
            stage.setScene(new Scene(root, 600, 500));
            stage.setResizable(false);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadView(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent view = loader.load();
            mainContainer.setCenter(view);
        } catch (IOException e) {
            System.err.println("Erreur de chargement de la vue Front : " + fxml);
            e.printStackTrace();
            mainContainer.setCenter(new Label("Erreur 404 : Vue introuvable."));
        }
    }
}
