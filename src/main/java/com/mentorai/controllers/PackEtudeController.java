package com.mentorai.controllers;

import com.mentorai.services.PackEtudeService;
import edu.connection3a36.tools.SessionManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URI;
import java.awt.Desktop;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.Paragraph;

public class PackEtudeController implements Initializable {

    @FXML private VBox formSection;
    @FXML private TextField topicField;
    @FXML private ComboBox<String> languageComboBox;
    @FXML private Label fileNameLabel;
    @FXML private Label errorLabel;

    @FXML private VBox loadingSection;
    @FXML private Label step1Icon, step1Text;
    @FXML private Label step2Icon, step2Text;
    @FXML private Label step3Icon, step3Text;

    @FXML private VBox resultsSection;
    @FXML private Label explanationLabel;
    @FXML private TabPane resultsTabPane;

    @FXML private Label resumeContent;
    @FXML private VBox slidesContainer;
    @FXML private VBox videosContainer;
    @FXML private VBox articlesContainer;

    @FXML private Label quizQuestionLabel;
    @FXML private Label quizAnswerLabel;
    @FXML private Button quizRevealBtn;
    @FXML private Label quizProgressLabel;

    @FXML private StackPane flashcardPane;
    @FXML private Label flashcardFront;
    @FXML private Label flashcardBack;
    @FXML private Label flashcardProgressLabel;

    private File selectedFile;
    private final PackEtudeService service = new PackEtudeService();
    private Timeline loadingTimeline;

    // State for Quiz and Flashcards
    private JSONArray quizArray;
    private int currentQuizIndex = 0;
    private JSONArray flashcardArray;
    private int currentFlashcardIndex = 0;
    private boolean isFlashcardFlipped = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initial state
        formSection.setVisible(true);
        loadingSection.setVisible(false);
        resultsSection.setVisible(false);
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/Apprentissage.fxml"));
            formSection.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleFileUpload(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner un fichier");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Documents", "*.pdf", "*.docx", "*.jpg", "*.png")
        );
        File file = fileChooser.showOpenDialog(formSection.getScene().getWindow());
        if (file != null) {
            long fileSizeInMB = file.length() / (1024 * 1024);
            if (fileSizeInMB > 5) {
                showError("Le fichier dépasse la limite de 5MB.");
                selectedFile = null;
                fileNameLabel.setText("Aucun fichier sélectionné");
            } else {
                selectedFile = file;
                fileNameLabel.setText(file.getName());
                errorLabel.setVisible(false);
            }
        }
    }

    @FXML
    private void handleGenerate(ActionEvent event) {
        String topic = topicField.getText().trim();
        String language = languageComboBox.getValue();

        if (topic.isEmpty()) {
            showError("Veuillez entrer un sujet.");
            return;
        }

        int userId = 1; // Fallback pour les tests
        try {
            if (SessionManager.getCurrentUser() != null) {
                userId = SessionManager.getCurrentUser().getId();
            }
        } catch (Exception e) {
            // Ignorer si SessionManager n'est pas initialisé ou a un problème
        }

        // Show loading
        formSection.setVisible(false);
        resultsSection.setVisible(false);
        loadingSection.setVisible(true);
        errorLabel.setVisible(false);

        startLoadingAnimation();

        service.generatePack(userId, topic, language, selectedFile)
                .thenAccept(result -> {
                    Platform.runLater(() -> {
                        stopLoadingAnimation();
                        displayResults(result);
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        stopLoadingAnimation();
                        loadingSection.setVisible(false);
                        formSection.setVisible(true);
                        
                        String message = ex.getMessage();
                        if (ex.getCause() != null) {
                            message = ex.getCause().getMessage();
                        }
                        showError("Erreur : " + message);
                    });
                    return null;
                });
    }

    private void startLoadingAnimation() {
        step1Icon.setText("⏳"); step1Text.setStyle("-fx-text-fill: #8892B0;");
        step2Icon.setText("⏳"); step2Text.setStyle("-fx-text-fill: #8892B0;");
        step3Icon.setText("⏳"); step3Text.setStyle("-fx-text-fill: #8892B0;");

        loadingTimeline = new Timeline(
                new KeyFrame(Duration.seconds(2), e -> {
                    step1Icon.setText("✓");
                    step1Icon.setStyle("-fx-text-fill: #64FFDA;");
                    step1Text.setStyle("-fx-text-fill: white;");
                }),
                new KeyFrame(Duration.seconds(5), e -> {
                    step2Icon.setText("✓");
                    step2Icon.setStyle("-fx-text-fill: #64FFDA;");
                    step2Text.setStyle("-fx-text-fill: white;");
                }),
                new KeyFrame(Duration.seconds(8), e -> {
                    step3Icon.setText("✓");
                    step3Icon.setStyle("-fx-text-fill: #64FFDA;");
                    step3Text.setStyle("-fx-text-fill: white;");
                })
        );
        loadingTimeline.play();
    }

    private void stopLoadingAnimation() {
        if (loadingTimeline != null) {
            loadingTimeline.stop();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void displayResults(JSONObject result) {
        System.out.println("DISPLAY RESULTS: " + result.toString());
        
        loadingSection.setVisible(false);
        resultsSection.setVisible(true);

        explanationLabel.setText(result.optString("computed_explanation", ""));

        // Résumé
        resumeContent.setText(result.optString("summary", "Aucun résumé disponible."));

        // Slides
        slidesContainer.getChildren().clear();
        JSONArray slides = result.optJSONArray("slides");
        if (slides != null && slides.length() > 0) {
            for (int i = 0; i < slides.length(); i++) {
                JSONObject slide = slides.getJSONObject(i);
                VBox slideBox = new VBox(5);
                slideBox.getStyleClass().add("link-box"); // Reusing style for container
                Label titre = new Label(slide.optString("title"));
                titre.getStyleClass().add("link-title");
                slideBox.getChildren().add(titre);

                JSONArray points = slide.optJSONArray("bullets");
                if (points != null) {
                    for (int j = 0; j < points.length(); j++) {
                        Label point = new Label("• " + points.getString(j));
                        point.setStyle("-fx-text-fill: white;");
                        slideBox.getChildren().add(point);
                    }
                }
                slidesContainer.getChildren().add(slideBox);
            }
            
            Button exportPdfBtn = new Button("Exporter en PDF");
            exportPdfBtn.getStyleClass().add("primary-btn");
            exportPdfBtn.setOnAction(e -> exportSlidesToPdf(slides));
            slidesContainer.getChildren().add(exportPdfBtn);
        }

        // Vidéos
        videosContainer.getChildren().clear();
        JSONArray videos = result.optJSONArray("videos");
        if (videos != null && videos.length() > 0) {
            for (int i = 0; i < videos.length(); i++) {
                JSONObject video = videos.getJSONObject(i);
                HBox vidBox = new HBox(15);
                vidBox.getStyleClass().add("link-box");
                
                String urlStr = video.optString("url");
                String videoId = extractYoutubeId(urlStr);
                
                ImageView thumbnail = new ImageView();
                thumbnail.setFitWidth(120);
                thumbnail.setFitHeight(90);
                if (videoId != null && !videoId.isEmpty()) {
                    thumbnail.setImage(new Image("https://img.youtube.com/vi/" + videoId + "/mqdefault.jpg", true));
                }
                
                VBox infoBox = new VBox(5);
                Label titre = new Label(video.optString("title"));
                titre.getStyleClass().add("link-title");
                titre.setWrapText(true);
                
                Button openBtn = new Button("▶ Ouvrir");
                openBtn.getStyleClass().add("primary-btn");
                openBtn.setOnAction(e -> openUrl(urlStr));
                
                infoBox.getChildren().addAll(titre, openBtn);
                vidBox.getChildren().addAll(thumbnail, infoBox);
                
                videosContainer.getChildren().add(vidBox);
            }
        }

        // Articles
        articlesContainer.getChildren().clear();
        JSONArray articles = result.optJSONArray("articles");
        if (articles != null && articles.length() > 0) {
            for (int i = 0; i < articles.length(); i++) {
                JSONObject article = articles.getJSONObject(i);
                VBox artBox = new VBox(5);
                artBox.getStyleClass().add("link-box");
                
                Label titre = new Label(article.optString("title"));
                titre.getStyleClass().add("link-title");
                titre.setWrapText(true);
                
                String urlStr = article.optString("url");
                if (urlStr == null || urlStr.isEmpty()) urlStr = article.optString("link");
                
                Button openBtn = new Button("🔗 Ouvrir");
                openBtn.getStyleClass().add("primary-btn");
                final String finalUrl = urlStr;
                openBtn.setOnAction(e -> openUrl(finalUrl));
                
                artBox.getChildren().addAll(titre, openBtn);
                articlesContainer.getChildren().add(artBox);
            }
        }

        // Quiz
        quizArray = result.optJSONArray("quiz");
        currentQuizIndex = 0;
        updateQuizView();

        // Flashcards
        flashcardArray = result.optJSONArray("flashcards");
        currentFlashcardIndex = 0;
        isFlashcardFlipped = false;
        updateFlashcardView();
    }

    // --- Quiz Logic ---
    private void updateQuizView() {
        if (quizArray == null || quizArray.length() == 0) {
            quizQuestionLabel.setText("Aucun quiz disponible.");
            quizAnswerLabel.setVisible(false);
            quizRevealBtn.setVisible(false);
            quizProgressLabel.setText("0/0");
            return;
        }
        JSONObject q = quizArray.getJSONObject(currentQuizIndex);
        quizQuestionLabel.setText(q.optString("question"));
        quizAnswerLabel.setText(q.optString("answer"));
        quizAnswerLabel.setVisible(false);
        quizRevealBtn.setVisible(true);
        quizProgressLabel.setText((currentQuizIndex + 1) + "/" + quizArray.length());
    }

    @FXML
    private void handleRevealQuiz(ActionEvent event) {
        quizAnswerLabel.setVisible(true);
        quizRevealBtn.setVisible(false);
    }

    @FXML
    private void handlePrevQuiz(ActionEvent event) {
        if (quizArray != null && currentQuizIndex > 0) {
            currentQuizIndex--;
            updateQuizView();
        }
    }

    @FXML
    private void handleNextQuiz(ActionEvent event) {
        if (quizArray != null && currentQuizIndex < quizArray.length() - 1) {
            currentQuizIndex++;
            updateQuizView();
        }
    }

    // --- Flashcard Logic ---
    private void updateFlashcardView() {
        if (flashcardArray == null || flashcardArray.length() == 0) {
            flashcardFront.setText("Aucune flashcard.");
            flashcardBack.setText("");
            flashcardProgressLabel.setText("0/0");
            return;
        }
        JSONObject f = flashcardArray.getJSONObject(currentFlashcardIndex);
        flashcardFront.setText(f.optString("front"));
        flashcardBack.setText(f.optString("back"));
        
        isFlashcardFlipped = false;
        flashcardFront.setVisible(true);
        flashcardBack.setVisible(false);
        
        flashcardProgressLabel.setText((currentFlashcardIndex + 1) + "/" + flashcardArray.length());
    }

    @FXML
    private void handleFlipCard(MouseEvent event) {
        if (flashcardArray == null || flashcardArray.length() == 0) return;
        
        isFlashcardFlipped = !isFlashcardFlipped;
        flashcardFront.setVisible(!isFlashcardFlipped);
        flashcardBack.setVisible(isFlashcardFlipped);
    }

    @FXML
    private void handlePrevCard(ActionEvent event) {
        if (flashcardArray != null && currentFlashcardIndex > 0) {
            currentFlashcardIndex--;
            updateFlashcardView();
        }
    }

    @FXML
    private void handleNextCard(ActionEvent event) {
        if (flashcardArray != null && currentFlashcardIndex < flashcardArray.length() - 1) {
            currentFlashcardIndex++;
            updateFlashcardView();
        }
    }

    private void exportSlidesToPdf(JSONArray slides) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Sauvegarder les slides en PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
        File file = fc.showSaveDialog(formSection.getScene().getWindow());
        if (file != null) {
            try {
                Document doc = new Document();
                PdfWriter.getInstance(doc, new FileOutputStream(file));
                doc.open();
                for (int i = 0; i < slides.length(); i++) {
                    JSONObject slide = slides.getJSONObject(i);
                    doc.add(new Paragraph(slide.optString("title")));
                    doc.add(new Paragraph("\n"));
                    JSONArray points = slide.optJSONArray("bullets");
                    if (points != null) {
                        for (int j = 0; j < points.length(); j++) {
                            doc.add(new Paragraph("• " + points.getString(j)));
                        }
                    }
                    if (i < slides.length() - 1) {
                        doc.newPage();
                    }
                }
                doc.close();
            } catch (Exception ex) {
                ex.printStackTrace();
                showError("Erreur lors de l'exportation: " + ex.getMessage());
            }
        }
    }

    private String extractYoutubeId(String url) {
        if (url == null) return null;
        if (url.contains("v=")) {
            int idx = url.indexOf("v=");
            int endIdx = url.indexOf("&", idx);
            if (endIdx == -1) endIdx = url.length();
            return url.substring(idx + 2, endIdx);
        }
        if (url.contains("youtu.be/")) {
            int idx = url.indexOf("youtu.be/");
            int endIdx = url.indexOf("?", idx);
            if (endIdx == -1) endIdx = url.length();
            return url.substring(idx + 9, endIdx);
        }
        return null;
    }

    private void openUrl(String url) {
        if (url == null || url.isEmpty()) return;
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Impossible d'ouvrir l'URL.");
        }
    }
}
