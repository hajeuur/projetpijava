package edu.connection3a36.controllers;

import edu.connection3a36.entities.Parcours;
import edu.connection3a36.entities.Projet;
import edu.connection3a36.services.ProjetService;
import edu.connection3a36.services.WikipediaService;
import edu.connection3a36.services.VoiceRecorderService;
import edu.connection3a36.tools.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class AfficherProjetsController implements Initializable {

    @FXML
    private ListView<Projet> listProjets;
    @FXML
    private Button btnAjouter;
    @FXML
    private Button btnSupprimer;
    @FXML
    private Button btnEnregistrer;

    @FXML
    private Label lblFormTitle;
    @FXML
    private Label lblTabRessources;
    @FXML
    private Label lblErreur;
    @FXML
    private Label errTitre, errType, errDescription, errTechnologies, errDateDebut, errDateFin;

    @FXML
    private TextField txtTitre;
    @FXML
    private TextField txtType;
    @FXML
    private TextArea taDescription;
    @FXML
    private TextField txtTechnologies;
    @FXML
    private DatePicker dpDateDebut;
    @FXML
    private DatePicker dpDateFin;
    @FXML
    private ComboBox<Parcours> cbParcours;

    private final ProjetService projetService = new ProjetService();
    private final edu.connection3a36.services.ParcoursService parcoursService = new edu.connection3a36.services.ParcoursService();
    private Parcours parcoursActuel;
    private ObservableList<Projet> projetsData = FXCollections.observableArrayList();
    private Projet selectedProjet = null; // null = mode ajout, non-null = mode édition

    @FXML
    private VBox vboxChat, paneChat;
    @FXML
    private TextField txtChatInput;
    @FXML
    private ScrollPane scrollChat;
    @FXML
    private Button btnMic;
    @FXML
    private javafx.scene.shape.SVGPath micIcon;

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
            // Start recording
            voiceService.startRecording();
            micIcon.setFill(javafx.scene.paint.Color.web("#ef4444"));
            micIcon.setStroke(javafx.scene.paint.Color.web("#ef4444"));
            btnMic.setStyle("-fx-background-color: #fee2e2; -fx-background-radius: 50; -fx-min-width: 40; -fx-min-height: 40;");
            ajouterMessageBot("J'écoute... Cliquez à nouveau pour arrêter.");
        } else {
            // Stop and transcribe
            micIcon.setFill(javafx.scene.paint.Color.web("#64748b"));
            micIcon.setStroke(javafx.scene.paint.Color.web("#64748b"));
            btnMic.setDisable(true);
            java.io.File audioFile = voiceService.stopRecording();

            if (audioFile != null) {
                voiceService.transcribe(audioFile).thenAccept(text -> {
                    javafx.application.Platform.runLater(() -> {
                        btnMic.setDisable(false);
                        btnMic.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 50; -fx-min-width: 40; -fx-min-height: 40;");
                        
                        if (text != null && !text.startsWith("Erreur")) {
                            txtChatInput.setText(text);
                        } else {
                            ajouterMessageBot("Désolé, je n'ai pas pu transcrire votre message : " + text);
                        }
                    });
                });
            } else {
                btnMic.setDisable(false);
                btnMic.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 50; -fx-min-width: 40; -fx-min-height: 40;");
            }
        }
    }

    @FXML
    private void envoyerMessageChat() {
        String msg = txtChatInput.getText().trim();
        if (msg.isEmpty())
            return;

        ajouterMessageUser(msg);
        txtChatInput.clear();

        boolean isPropose = msg.toLowerCase().contains("propose");

        edu.connection3a36.services.GroqService.getResponse(msg, isPropose)
                .thenAccept(response -> {
                    javafx.application.Platform.runLater(() -> {
                        String fullResponse = response;
                        if (fullResponse.contains("[JSON]")) {
                            try {
                                int start = fullResponse.indexOf("[JSON]") + 6;
                                int end = fullResponse.indexOf("[/JSON]");
                                String jsonStr = fullResponse.substring(start, end).trim();

                                String displayMsg = fullResponse.replace("[JSON]", "").replace("[/JSON]", "")
                                        .replace(jsonStr, "").trim();
                                ajouterMessageBot(displayMsg);

                                org.json.JSONObject suggestion = new org.json.JSONObject(jsonStr);
                                proposerRemplissage(suggestion);
                            } catch (Exception e) {
                                ajouterMessageBot(fullResponse);
                            }
                        } else {
                            ajouterMessageBot(fullResponse);
                        }
                    });
                });

    }


    private void ajouterMessageUser(String text) {
        Label lbl = new Label(text);
        lbl.setWrapText(true);
        lbl.setStyle("-fx-background-color: #6366f1; -fx-text-fill: white; -fx-padding: 10; -fx-background-radius: 15 15 0 15;");
        HBox container = new HBox(lbl);
        container.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        vboxChat.getChildren().add(container);
        scrollChat.setVvalue(1.0);
    }

    private void ajouterMessageBot(String text) {
        javafx.scene.text.TextFlow flow = new javafx.scene.text.TextFlow();
        flow.setMaxWidth(300); 
        
        String[] words = text.split("(?<=\\s)|(?=\\s)"); 
        for (String word : words) {
            javafx.scene.text.Text textNode = new javafx.scene.text.Text(word);
            textNode.setFill(javafx.scene.paint.Color.web("#1e293b"));
            
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

            flow.getChildren().add(textNode);
        }

        flow.setStyle("-fx-background-color: #f1f5f9; -fx-padding: 10; -fx-background-radius: 15 15 15 0;");
        
        HBox container = new HBox(flow);
        container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        vboxChat.getChildren().add(container);
        scrollChat.setVvalue(1.0);
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

    private void proposerRemplissage(org.json.JSONObject suggestion) {
        ajouterMessageBot("Voici une idée de projet ! 💡\n\n" +
                "TITRE : " + suggestion.getString("titre") + "\n" +
                "Voulez-vous remplir le formulaire ?");

        Button btnValider = new Button("✅ Remplir le formulaire");
        btnValider.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;");
        btnValider.setOnAction(e -> {
            txtTitre.setText(suggestion.getString("titre"));
            taDescription.setText(suggestion.getString("description"));
            txtTechnologies.setText(suggestion.getString("technologies"));
            txtType.setText("Personnel");
            ajouterMessageBot("Formulaire rempli ! 😊");
        });
        vboxChat.getChildren().add(btnValider);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        listProjets.setCellFactory(new Callback<ListView<Projet>, ListCell<Projet>>() {
            @Override
            public ListCell<Projet> call(ListView<Projet> param) {
                return new ListCell<Projet>() {
                    @Override
                    protected void updateItem(Projet item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setStyle("-fx-background-color: transparent;");
                        } else {
                            setText(item.getTitre() + "\n" + (item.getType() != null ? item.getType() : "Autre"));
                            setStyle("-fx-padding: 10; -fx-border-color: transparent transparent #eee transparent; -fx-border-width: 1; -fx-cursor: hand;");
                            if (isSelected()) {
                                setStyle(getStyle() + "-fx-background-color: #f0f4f8; -fx-text-fill: #102c59; -fx-font-weight: bold;");
                            }
                        }
                    }
                };
            }
        });

        listProjets.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                remplirFormulaire(newSel);
            }
        });

        ajouterMessageBot("Bonjour ! 👋 Je suis votre assistant MentorAI. Comment puis-je vous aider dans vos projets aujourd'hui ?");
    }

    public void initData(Parcours parcours) {
        this.parcoursActuel = parcours;
        chargerDonnees();
        preparerAjout(); 
    }

    public void selectProject(Projet p) {
        if (p == null) return;
        for (Projet item : listProjets.getItems()) {
            if (item.getId() == p.getId()) {
                listProjets.getSelectionModel().select(item);
                remplirFormulaire(item);
                break;
            }
        }
    }

    private void chargerDonnees() {
        try {
            List<Projet> liste;
            if (parcoursActuel != null) {
                liste = projetService.getByParcoursId(parcoursActuel.getId());
            } else {
                liste = projetService.getData(); 
            }
            projetsData = FXCollections.observableArrayList(liste);
            listProjets.setItems(projetsData);

            List<Parcours> allParcours = parcoursService.getData();
            cbParcours.setItems(FXCollections.observableArrayList(allParcours));

            cbParcours.setCellFactory(lv -> new ListCell<Parcours>() {
                @Override
                protected void updateItem(Parcours item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : item.getTitre());
                }
            });
            cbParcours.setButtonCell(new ListCell<Parcours>() {
                @Override
                protected void updateItem(Parcours item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : item.getTitre());
                }
            });

        } catch (SQLException e) {
            lblErreur.setText("Erreur de chargement: " + e.getMessage());
        }
    }

    @FXML
    private void preparerAjout() {
        selectedProjet = null;
        listProjets.getSelectionModel().clearSelection();
        lblFormTitle.setText("Créer un projet");
        btnEnregistrer.setText("💾 Enregistrer le projet");
        txtTitre.clear();
        txtType.clear();
        taDescription.clear();
        txtTechnologies.clear();
        dpDateDebut.setValue(null);
        dpDateFin.setValue(null);
        cbParcours.setValue(parcoursActuel); 
        lblErreur.setText("");
        lblTabRessources.setVisible(false);
    }

    private void remplirFormulaire(Projet projet) {
        selectedProjet = projet;
        lblFormTitle.setText("Détails du projet");
        btnEnregistrer.setText("✏️ Modifier le projet");
        txtTitre.setText(projet.getTitre());
        txtType.setText(projet.getType());
        taDescription.setText(projet.getDescription());
        txtTechnologies.setText(projet.getTechnologies());
        dpDateDebut.setValue(projet.getDateDebut());
        dpDateFin.setValue(projet.getDateFin());

        if (projet.getParcoursId() > 0) {
            cbParcours.getItems().stream()
                    .filter(p -> p.getId() == projet.getParcoursId())
                    .findFirst()
                    .ifPresent(cbParcours::setValue);
        } else {
            cbParcours.setValue(null);
        }

        lblErreur.setText("");
        lblTabRessources.setVisible(true);
    }

    @FXML
    private void enregistrerProjet() {
        String titre = txtTitre.getText().trim();
        String type = txtType.getText().trim();
        String desc = taDescription.getText().trim();
        String tech = txtTechnologies.getText().trim();
        LocalDate dd = dpDateDebut.getValue();
        LocalDate df = dpDateFin.getValue();

        hideAllErrors();
        boolean isValid = true;

        if (titre.isEmpty()) { showErr(errTitre, "• Le titre est obligatoire."); isValid = false; }
        if (type.isEmpty()) { showErr(errType, "• Le type est obligatoire."); isValid = false; }
        if (desc.isEmpty()) { showErr(errDescription, "• La description est obligatoire."); isValid = false; }
        if (dd == null) { showErr(errDateDebut, "• Date début obligatoire."); isValid = false; }

        if (!isValid) return;

        try {
            Parcours linkedP = cbParcours.getValue();
            if (selectedProjet == null) {
                Projet p = new Projet(titre, type, desc, tech, dd, df, linkedP != null ? linkedP.getId() : 0);
                if (SessionManager.getCurrentUser() != null) {
                    p.setUtilisateurId(SessionManager.getCurrentUser().getId());
                }
                projetService.addEntity(p);
                afficherInfo("Succès", "Le projet a été créé.");
            } else {
                selectedProjet.setTitre(titre);
                selectedProjet.setType(type);
                selectedProjet.setDescription(desc);
                selectedProjet.setTechnologies(tech);
                selectedProjet.setDateDebut(dd);
                selectedProjet.setDateFin(df);
                selectedProjet.setParcoursId(linkedP != null ? linkedP.getId() : 0);
                projetService.updateEntity(selectedProjet.getId(), selectedProjet);
                afficherInfo("Succès", "Le projet a été mis à jour.");
            }
            chargerDonnees();
            preparerAjout();
        } catch (SQLException e) {
            lblErreur.setText("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void supprimerProjet() {
        Projet selected = listProjets.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Optional<ButtonType> result = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ce projet ?", ButtonType.YES, ButtonType.NO).showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            try {
                projetService.deleteEntity(selected);
                chargerDonnees();
                preparerAjout();
            } catch (SQLException e) {
                lblErreur.setText("Erreur suppression: " + e.getMessage());
            }
        }
    }

    @FXML
    private void ouvrirRessources() {
        if (selectedProjet == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AfficherRessources.fxml"));
            Parent view = loader.load();
            AfficherRessourcesController controller = loader.getController();
            controller.initData(selectedProjet);
            MainController.getInstance().loadInContentArea(view);
        } catch (IOException e) {
            lblErreur.setText("Erreur: " + e.getMessage());
        }
    }

    @FXML
    private void retour() {
        MainController.getInstance().showParcours();
    }

    private void afficherInfo(String titre, String message) {
        new Alert(Alert.AlertType.INFORMATION, message).show();
    }

    private void hideAllErrors() {
        errTitre.setVisible(false); errType.setVisible(false);
        errDescription.setVisible(false); errTechnologies.setVisible(false);
        errDateDebut.setVisible(false); errDateFin.setVisible(false);
        lblErreur.setText("");
    }

    private void showErr(Label lbl, String msg) {
        lbl.setText(msg); lbl.setVisible(true); lbl.setManaged(true);
    }
}
