package com.mentorai.controllers;

import com.mentorai.models.Feedback;
import com.mentorai.models.Traitement;
import com.mentorai.services.FeedbackService;
import com.mentorai.services.TraitementService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class MesFeedbacksController implements Initializable {

    @FXML private TableView<Feedback> tableFeedbacks;
    @FXML private TableColumn<Feedback, Integer> colId;
    @FXML private TableColumn<Feedback, String>  colType;
    @FXML private TableColumn<Feedback, String>  colDate;
    @FXML private TableColumn<Feedback, Integer> colNote;
    @FXML private TableColumn<Feedback, String>  colMessage;
    @FXML private TableColumn<Feedback, String>  colEtat;
    @FXML private TableColumn<Feedback, Void>    colReponse;
    @FXML private TableColumn<Feedback, Void>    colActions;

    @FXML private TextField champRecherche;
    @FXML private ComboBox<String> comboTri;
    @FXML private Label labelTotal;

    private int utilisateurId = 11;
    private List<Feedback> tousLesFeedbacks;

    private FeedbackService feedbackService = new FeedbackService();
    private TraitementService traitementService = new TraitementService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        comboTri.setItems(FXCollections.observableArrayList(
                "Plus récent d'abord",
                "Plus ancien d'abord",
                "Note croissante",
                "Note décroissante"
        ));
        comboTri.setValue("Plus récent d'abord");

        configurerColonnes();

        // ✅ CORRECTION : message quand table vide
        tableFeedbacks.setPlaceholder(new Label("Aucun feedback trouvé."));
        
        // ✅ CHARGER LES DONNÉES
        chargerDonnees();
    }

    public void setUtilisateurId(int id) {
        this.utilisateurId = id;
        chargerDonnees();
    }

    private void configurerColonnes() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("datefeedback"));
        colNote.setCellValueFactory(new PropertyValueFactory<>("note"));
        colMessage.setCellValueFactory(new PropertyValueFactory<>("contenu"));

        // ===== TYPE badge coloré =====
        colType.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null); return;
                }
                Feedback f = getTableView().getItems().get(getIndex());
                String type = f.getTypefeedback();
                String couleur = switch (type) {
                    case "probleme"     -> "#d52e28";
                    case "satisfaction" -> "#28a745";
                    case "suggestion"   -> "#9dbbce";
                    default             -> "#888";
                };
                Label badge = new Label(type);
                badge.setStyle(
                        "-fx-background-color: " + couleur + "; -fx-text-fill: white;" +
                                "-fx-padding: 3 8 3 8; -fx-background-radius: 10;" +
                                "-fx-font-size: 11px; -fx-font-weight: bold;"
                );
                setGraphic(badge);
                setText(null);
            }
        });

        // ===== ÉTAT badge =====
        colEtat.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null); return;
                }
                Feedback f = getTableView().getItems().get(getIndex());
                boolean traite = f.getEtatfeedback().equals("traite");
                Label badge = new Label(traite ? "✅ Traité" : "⏳ En attente");
                badge.setStyle(
                        "-fx-background-color: " + (traite ? "#28a745" : "#f0a500") + ";" +
                                "-fx-text-fill: white; -fx-padding: 4 10 4 10;" +
                                "-fx-background-radius: 10; -fx-font-size: 11px; -fx-font-weight: bold;"
                );
                setGraphic(badge);
                setText(null);
            }
        });

        // ===== RÉPONSE ADMIN =====
        colReponse.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null); return;
                }
                Feedback f = getTableView().getItems().get(getIndex());

                if (f.getTraitementId() != 0) {
                    Traitement t = traitementService.getById(f.getTraitementId());
                    if (t != null) {
                        VBox box = new VBox(3);
                        box.setStyle(
                                "-fx-background-color: #f0f4ff; -fx-padding: 6;" +
                                        "-fx-background-radius: 6;"
                        );
                        Label lType = new Label("🔖 " + t.getTypetraitement());
                        lType.setStyle(
                                "-fx-font-size: 10px; -fx-text-fill: #102c59; -fx-font-weight: bold;"
                        );
                        String desc = t.getDescription().length() > 40
                                ? t.getDescription().substring(0, 40) + "..."
                                : t.getDescription();
                        Label lDesc = new Label(desc);
                        lDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
                        box.getChildren().addAll(lType, lDesc);
                        setGraphic(box);
                    } else {
                        setGraphic(null);
                    }
                } else {
                    Label lAttente = new Label("En attente de réponse");
                    lAttente.setStyle("-fx-text-fill: #aaa; -fx-font-size: 11px;");
                    setGraphic(lAttente);
                }
                setText(null);
            }
        });

        // ===== ACTIONS =====
        colActions.setCellFactory(col -> new TableCell<>() {
            final Button btnModifier  = new Button("✏️ Modifier");
            final Button btnSupprimer = new Button("🗑️ Supprimer");
            final Label  lblNonModif  = new Label("🔒 Non modifiable");
            final HBox   boiteActions = new HBox(6, btnModifier, btnSupprimer);

            {
                btnModifier.setStyle(
                        "-fx-background-color: #f0a500; -fx-text-fill: white;" +
                                "-fx-background-radius: 5; -fx-cursor: hand; -fx-font-size: 11px;"
                );
                btnSupprimer.setStyle(
                        "-fx-background-color: #d52e28; -fx-text-fill: white;" +
                                "-fx-background-radius: 5; -fx-cursor: hand; -fx-font-size: 11px;"
                );
                lblNonModif.setStyle(
                        "-fx-background-color: #ccc; -fx-text-fill: white;" +
                                "-fx-padding: 5 10 5 10; -fx-background-radius: 5; -fx-font-size: 11px;"
                );
                btnModifier.setOnAction(e -> {
                    Feedback f = getTableView().getItems().get(getIndex());
                    ouvrirModification(f);
                });
                btnSupprimer.setOnAction(e -> {
                    Feedback f = getTableView().getItems().get(getIndex());
                    supprimerFeedback(f);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null); return;
                }
                Feedback f = getTableView().getItems().get(getIndex());
                if (f.getEtatfeedback().equals("traite")) {
                    setGraphic(lblNonModif);
                } else {
                    setGraphic(boiteActions);
                }
                setText(null);
            }
        });
    }

    private void chargerDonnees() {
        tousLesFeedbacks = feedbackService.getByUtilisateur(utilisateurId);
        afficherDonnees(tousLesFeedbacks);
    }

    private void afficherDonnees(List<Feedback> liste) {
        tableFeedbacks.setItems(FXCollections.observableArrayList(liste));
        labelTotal.setText("ⓘ  Total : " + liste.size() + " feedback(s)");
    }

    @FXML
    private void filtrer() {
        String motCle = champRecherche.getText().trim().toLowerCase();
        String tri = comboTri.getValue();

        List<Feedback> filtre = tousLesFeedbacks.stream()
                .filter(f -> motCle.isEmpty() ||
                        f.getContenu().toLowerCase().contains(motCle))
                .collect(Collectors.toList());

        switch (tri) {
            case "Plus récent d'abord" ->
                    filtre.sort((a, b) -> b.getDatefeedback().compareTo(a.getDatefeedback()));
            case "Plus ancien d'abord" ->
                    filtre.sort((a, b) -> a.getDatefeedback().compareTo(b.getDatefeedback()));
            case "Note croissante" ->
                    filtre.sort((a, b) -> Integer.compare(a.getNote(), b.getNote()));
            case "Note décroissante" ->
                    filtre.sort((a, b) -> Integer.compare(b.getNote(), a.getNote()));
        }

        afficherDonnees(filtre);
    }

    private void ouvrirModification(Feedback feedback) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/ModifierFeedback.fxml")
            );
            VBox root = loader.load();
            ModifierFeedbackController ctrl = loader.getController();
            ctrl.setFeedback(feedback, this);

            Stage stage = (Stage) tableFeedbacks.getScene().getWindow();
            stage.setTitle("MentorAI - Modifier mon feedback");
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            System.out.println("❌ Erreur modification : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void supprimerFeedback(Feedback feedback) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer ce feedback ?");
        alert.setContentText("Cette action est irréversible.");
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                feedbackService.delete(feedback.getId());
                chargerDonnees();
            }
        });
    }

    @FXML
    private void nouveauFeedback() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/AjouterFeedback.fxml")
            );
            VBox root = loader.load();
            Stage stage = (Stage) tableFeedbacks.getScene().getWindow();
            stage.setTitle("MentorAI - Donner un feedback");
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            System.out.println("❌ Erreur : " + e.getMessage());
        }
    }

    public void rafraichir() {
        chargerDonnees();
    }
}