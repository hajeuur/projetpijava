package edu.connection3a36.Controller;

import edu.connection3a36.services.WikipediaService;
import edu.connection3a36.services.GroqService;
import edu.connection3a36.services.VoiceRecorderService;
import edu.connection3a36.tools.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class EntretienIAController implements Initializable {

    @FXML private Label lblStatus, lblScore, lblFeedback, lblTip, lblLoadingText;
    @FXML private TextFlow flowQuestion;
    @FXML private TextArea txtAnswer;
    @FXML private Button btnStart, btnMic, btnNext;
    @FXML private VBox paneEvaluation, paneLoading;
    @FXML private ProgressBar progressScore;
    @FXML private SVGPath micIcon;

    private final VoiceRecorderService voiceService = new VoiceRecorderService();
    private String currentQuestion = "";
    private int questionCount = 0;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initial state
        paneEvaluation.setVisible(false);
        paneEvaluation.setManaged(false);
    }

    @FXML
    private void demarrerEntretien() {
        btnStart.setVisible(false);
        btnStart.setManaged(false);
        btnMic.setVisible(true);
        btnMic.setManaged(true);
        questionCount = 1;
        fetchNewQuestion();
    }

    private void fetchNewQuestion() {
        showLoading("MentorAI prépare une question pertinente...");
        
        String context = "Étudiant en informatique"; // Default
        if (SessionManager.getInstance().getCurrentUser() != null) {
            context = "Étudiant (" + SessionManager.getInstance().getCurrentUser().getEmail() + ")";
        }

        GroqService.getInterviewQuestion(context).thenAccept(question -> {
            Platform.runLater(() -> {
                hideLoading();
                currentQuestion = question;
                updateQuestionFlow(question);
                lblStatus.setText("Question " + questionCount);
                txtAnswer.clear();
                paneEvaluation.setVisible(false);
                paneEvaluation.setManaged(false);
                btnNext.setVisible(false);
                btnNext.setManaged(false);
                btnMic.setDisable(false);
            });
        });
    }

    private void updateQuestionFlow(String text) {
        flowQuestion.getChildren().clear();
        String[] words = text.split("(?<=\\s)|(?=\\s)");
        for (String word : words) {
            Text textNode = new Text(word);
            textNode.setFill(Color.web("#1e293b"));
            textNode.setStyle("-fx-font-size: 18px;");

            textNode.setOnMouseClicked(e -> {
                if (e.isShiftDown()) {
                    String cleanWord = word.trim().replaceAll("[^a-zA-ZÀ-ÿ0-9]", "");
                    if (!cleanWord.isEmpty()) {
                        handleWikiLookup(cleanWord);
                    }
                }
            });

            textNode.setOnMouseEntered(e -> {
                if (e.isShiftDown()) {
                    textNode.setUnderline(true);
                    textNode.setCursor(javafx.scene.Cursor.HAND);
                }
            });
            textNode.setOnMouseExited(e -> {
                textNode.setUnderline(false);
                textNode.setCursor(javafx.scene.Cursor.DEFAULT);
            });

            flowQuestion.getChildren().add(textNode);
        }
    }

    private void handleWikiLookup(String word) {
        WikipediaService.getSummary(word).thenAccept(summary -> {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Wikipedia : " + word);
                alert.setHeaderText("Définition de " + word);
                Label lbl = new Label(summary);
                lbl.setWrapText(true);
                lbl.setPrefWidth(400);
                alert.getDialogPane().setContent(lbl);
                alert.showAndWait();
            });
        });
    }

    @FXML
    private void handleMicAction() {
        if (!voiceService.isRecording()) {
            voiceService.startRecording();
            micIcon.setFill(Color.web("#ef4444"));
            micIcon.setStroke(Color.web("#ef4444"));
            lblStatus.setText("Enregistrement en cours...");
        } else {
            micIcon.setFill(Color.web("#64748b"));
            micIcon.setStroke(Color.web("#64748b"));
            btnMic.setDisable(true);
            lblStatus.setText("Transcription...");
            
            File audioFile = voiceService.stopRecording();
            if (audioFile != null) {
                voiceService.transcribe(audioFile).thenAccept(text -> {
                    Platform.runLater(() -> {
                        if (text != null && !text.startsWith("Erreur")) {
                            txtAnswer.setText(text);
                            evaluerReponse(text);
                        } else {
                            lblStatus.setText("Erreur transcription");
                            btnMic.setDisable(false);
                        }
                    });
                });
            }
        }
    }

    private void evaluerReponse(String answer) {
        showLoading("Analyse de votre réponse par l'IA...");
        GroqService.evaluateAnswer(currentQuestion, answer).thenAccept(jsonStr -> {
            Platform.runLater(() -> {
                hideLoading();
                try {
                    JSONObject eval = new JSONObject(jsonStr);
                    int score = eval.getInt("score");
                    lblScore.setText(score + "/10");
                    progressScore.setProgress(score / 10.0);
                    lblFeedback.setText(eval.getString("feedback"));
                    lblTip.setText(eval.getString("tip"));
                    
                    paneEvaluation.setVisible(true);
                    paneEvaluation.setManaged(true);
                    btnNext.setVisible(true);
                    btnNext.setManaged(true);
                    lblStatus.setText("Analyse terminée");
                } catch (Exception e) {
                    lblFeedback.setText("Désolé, l'analyse a échoué. Passez à la suite.");
                    btnNext.setVisible(true);
                    btnNext.setManaged(true);
                }
            });
        });
    }

    @FXML
    private void zoomQuestion() {
        if (currentQuestion == null || currentQuestion.isEmpty()) return;
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Question de l'entretien");
        alert.setHeaderText("MentorAI vous demande :");
        
        // Custom styling for the alert content to make it "grossier" (large)
        Label content = new Label(currentQuestion);
        content.setWrapText(true);
        content.setPrefWidth(500);
        content.setStyle("-fx-font-size: 16px; -fx-text-fill: #1e293b; -fx-font-weight: bold;");
        
        alert.getDialogPane().setContent(content);
        alert.showAndWait();
    }

    @FXML
    private void prochaineQuestion() {
        questionCount++;
        fetchNewQuestion();
    }

    @FXML
    private void quitter() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/CareerDashboard.fxml"));
            Parent view = loader.load();
            
            // On cherche le BorderPane principal pour changer le centre
            BorderPane mainLayout = (BorderPane) flowQuestion.getScene().lookup("#mainContainer");
            if (mainLayout == null) {
                mainLayout = (BorderPane) flowQuestion.getScene().getRoot();
            }
            mainLayout.setCenter(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showLoading(String text) {
        lblLoadingText.setText(text);
        paneLoading.setVisible(true);
    }

    private void hideLoading() {
        paneLoading.setVisible(false);
    }
}
