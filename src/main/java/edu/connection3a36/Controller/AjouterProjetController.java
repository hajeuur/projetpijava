package edu.connection3a36.Controller;

import edu.connection3a36.entities.Parcours;
import edu.connection3a36.entities.Projet;
import edu.connection3a36.services.ProjetService;
import edu.connection3a36.services.VoiceRecorderService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import javafx.geometry.Insets;
import java.io.IOException;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class AjouterProjetController implements Initializable {

    @FXML
    private Label lblParcoursNom;
    @FXML
    private TextField txtTitre;
    @FXML
    private ComboBox<String> cbType;
    @FXML
    private TextArea taDescription;
    @FXML
    private TextField txtTechnologies;
    @FXML
    private DatePicker dpDateDebut;
    @FXML
    private DatePicker dpDateFin;
    @FXML
    private Label lblErreur;
    @FXML
    private Label errTitre, errType, errTechnologies, errDateDebut, errDateFin;

    private final ProjetService projetService = new ProjetService();
    private Parcours parcoursActuel;

    @FXML private VBox vboxChat, paneChat;
    @FXML private TextField txtChatInput;
    @FXML private ScrollPane scrollChat;
    @FXML private Button btnFloatingChat;
    @FXML private Button btnMic;
    @FXML private javafx.scene.shape.SVGPath micIcon;

    private final VoiceRecorderService voiceService = new VoiceRecorderService();

    @FXML
    private void toggleChat() {
        boolean isVisible = paneChat.isVisible();
        paneChat.setVisible(!isVisible);
        paneChat.setManaged(!isVisible);
        if (!isVisible) {
            txtChatInput.requestFocus();
        }
    }

    @FXML
    private void handleMicAction() {
        if (!voiceService.isRecording()) {
            voiceService.startRecording();
            micIcon.setFill(javafx.scene.paint.Color.web("#ef4444"));
            micIcon.setStroke(javafx.scene.paint.Color.web("#ef4444"));
            btnMic.setStyle("-fx-background-color: #fee2e2; -fx-background-radius: 50; -fx-min-width: 35; -fx-min-height: 35;");
            ajouterMessageBot("J'écoute...");
        } else {
            micIcon.setFill(javafx.scene.paint.Color.web("#64748b"));
            micIcon.setStroke(javafx.scene.paint.Color.web("#64748b"));
            btnMic.setDisable(true);
            java.io.File audioFile = voiceService.stopRecording();

            if (audioFile != null) {
                voiceService.transcribe(audioFile).thenAccept(text -> {
                    javafx.application.Platform.runLater(() -> {
                        btnMic.setDisable(false);
                        btnMic.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 50; -fx-min-width: 35; -fx-min-height: 35;");
                        
                        if (text != null && !text.startsWith("Erreur")) {
                            txtChatInput.setText(text);
                        } else {
                            ajouterMessageBot("Erreur transcription: " + text);
                        }
                    });
                });
            } else {
                btnMic.setDisable(false);
                btnMic.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 50; -fx-min-width: 35; -fx-min-height: 35;");
            }
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cbType.setItems(FXCollections.observableArrayList(
                "Personnel", "Académique", "Professionnel", "Open Source", "Compétition"));
        
        // Message de bienvenue du bot
        ajouterMessageBot("Salut ! 👋 Je suis ton assistant IA. Que cherches-tu à créer comme projet aujourd'hui ? Je peux te proposer des idées et t'aider à remplir le formulaire.");
        
        // Petit bouton "propose" rapide
        Button btnQuick = new Button("propose");
        btnQuick.setStyle("-fx-background-color: #cbd5e1; -fx-text-fill: #1e293b; -fx-background-radius: 10; -fx-font-size: 11px; -fx-cursor: hand;");
        btnQuick.setOnAction(e -> {
            txtChatInput.setText("propose");
            envoyerMessage();
        });
        HBox container = new HBox(btnQuick);
        container.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        container.setPadding(new Insets(0, 10, 0, 0));
        vboxChat.getChildren().add(container);
    }

    @FXML
    private void envoyerMessage() {
        String msg = txtChatInput.getText().trim();
        if (msg.isEmpty()) return;

        ajouterMessageUser(msg);
        txtChatInput.clear();

        boolean isPropose = msg.toLowerCase().contains("propose");
        
        edu.connection3a36.services.GroqService.getResponse(msg, isPropose)
            .thenAccept(response -> {
                Platform.runLater(() -> {
                    if (isPropose) {
                        try {
                            // On tente de parser le JSON si c'est une proposition
                            String jsonStr = response.substring(response.indexOf("{"), response.lastIndexOf("}") + 1);
                            org.json.JSONObject suggestion = new org.json.JSONObject(jsonStr);
                            proposerRemplissage(suggestion);
                        } catch (Exception e) {
                            ajouterMessageBot(response); // Fallback texte simple
                        }
                    } else {
                        ajouterMessageBot(response);
                    }
                });
            });
    }

    private void ajouterMessageUser(String text) {
        Label lbl = new Label(text);
        lbl.setWrapText(true);
        lbl.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-padding: 10; -fx-background-radius: 15 15 0 15;");
        HBox container = new HBox(lbl);
        container.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        vboxChat.getChildren().add(container);
        scrollChat.setVvalue(1.0);
    }

    private void ajouterMessageBot(String text) {
        Label lbl = new Label(text);
        lbl.setWrapText(true);
        lbl.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #1e293b; -fx-padding: 10; -fx-background-radius: 15 15 15 0;");
        HBox container = new HBox(lbl);
        container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        vboxChat.getChildren().add(container);
        scrollChat.setVvalue(1.0);
    }

    private void proposerRemplissage(org.json.JSONObject suggestion) {
        ajouterMessageBot("Voici une idée géniale ! 💡\n\n" +
                "TITRE : " + suggestion.getString("titre") + "\n" +
                "Voulez-vous remplir le formulaire avec ces détails ?");
        
        Button btnValider = new Button("✅ Remplir le formulaire");
        btnValider.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-background-radius: 10;");
        btnValider.setOnAction(e -> {
            txtTitre.setText(suggestion.getString("titre"));
            taDescription.setText(suggestion.getString("description"));
            txtTechnologies.setText(suggestion.getString("technologies"));
            cbType.setValue("Personnel"); // Valeur par défaut
            ajouterMessageBot("Formulaire rempli ! Vous n'avez plus qu'à ajuster les dates. 😊");
        });
        vboxChat.getChildren().add(btnValider);
    }

    public void initData(Parcours parcours) {
        this.parcoursActuel = parcours;
        lblParcoursNom.setText("Parcours : " + parcours.getTitre());
    }

    @FXML
    private void enregistrer() {
        boolean isValid = true;
        hideAllErrors();

        if (txtTitre.getText().trim().isEmpty()) {
            showErr(errTitre, "• Le titre est obligatoire.");
            isValid = false;
        }

        if (cbType.getValue() == null) {
            showErr(errType, "• Le type est obligatoire.");
            isValid = false;
        }
        if (txtTechnologies.getText().trim().isEmpty()) {
            showErr(errTechnologies, "• Techs obligatoires.");
            isValid = false;
        }
        if (dpDateDebut.getValue() == null) {
            showErr(errDateDebut, "• Date début obligatoire.");
            isValid = false;
        }

        if (dpDateDebut.getValue() != null && dpDateFin.getValue() != null
                && dpDateFin.getValue().isBefore(dpDateDebut.getValue())) {
            showErr(errDateFin, "• La date de fin doit être après le début.");
            isValid = false;
        }

        if (!isValid) return;

        Projet projet = new Projet();
        projet.setTitre(txtTitre.getText().trim());
        projet.setType(cbType.getValue());
        projet.setDescription(taDescription.getText());
        projet.setTechnologies(txtTechnologies.getText().trim());
        projet.setDateDebut(dpDateDebut.getValue());
        projet.setDateFin(dpDateFin.getValue());
        projet.setParcoursId(parcoursActuel.getId());

        try {
            if (projetService.existsByTitreAndParcours(projet.getTitre(), projet.getParcoursId())) {
                showErr(errTitre, "• Ce titre existe déjà.");
                return;
            }
            projetService.addEntity(projet);
            fermer();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Erreur Save: " + e.getMessage()).show();
        }
    }

    private void hideAllErrors() {
        errTitre.setVisible(false); errTitre.setManaged(false);
        errType.setVisible(false); errType.setManaged(false);
        errTechnologies.setVisible(false); errTechnologies.setManaged(false);
        errDateDebut.setVisible(false); errDateDebut.setManaged(false);
        errDateFin.setVisible(false); errDateFin.setManaged(false);
    }

    private void showErr(Label lbl, String msg) {
        lbl.setText(msg); lbl.setVisible(true); lbl.setManaged(true);
    }

    @FXML private void annuler() { fermer(); }

    private void fermer() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AfficherProjets.fxml"));
            Parent view = loader.load();
            AfficherProjetsController controller = loader.getController();
            controller.initData(parcoursActuel);
            
            // On remonte au BorderPane principal
            Scene scene = txtTitre.getScene();
            BorderPane mainLayout = (BorderPane) scene.lookup("#mainContainer");
            if (mainLayout != null) mainLayout.setCenter(view);
            else ((BorderPane) scene.getRoot()).setCenter(view);
            
        } catch (IOException e) { e.printStackTrace(); }
    }
}
