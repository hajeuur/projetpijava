package edu.connection3a36.Controller;

import edu.connection3a36.entities.Parcours;
import edu.connection3a36.services.ParcoursService;
import edu.connection3a36.services.CVService;
import edu.connection3a36.services.ProjetService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AfficherParcoursController implements Initializable {

    @FXML private FlowPane flowPaneParcours;
    @FXML private TextField txtRecherche;
    @FXML private ComboBox<String> cbFiltreType;
    @FXML private Label lblStats;

    private final ParcoursService parcoursService = new ParcoursService();
    private List<Parcours> allParcours = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cbFiltreType.setItems(FXCollections.observableArrayList("Tous", "Formation", "Stage", "Alternance", "Emploi", "Personnel"));
        cbFiltreType.setValue("Tous");
        
        chargerDonnees();
        
        txtRecherche.textProperty().addListener((obs, oldVal, newVal) -> appliquerFiltres());
    }

    private void chargerDonnees() {
        try {
            allParcours = parcoursService.getData();
            appliquerFiltres();
            lblStats.setText("Nombre de parcours : " + allParcours.size());
        } catch (SQLException e) {
            afficherErreur("Erreur", "Impossible de charger les données : " + e.getMessage());
        }
    }

    @FXML
    private void filtrerParType() {
        appliquerFiltres();
    }

    private void appliquerFiltres() {
        String query = txtRecherche.getText().toLowerCase();
        String typeFilter = cbFiltreType.getValue();

        List<Parcours> filtered = allParcours.stream()
                .filter(p -> (typeFilter.equals("Tous") || p.getTypeParcours().equals(typeFilter)))
                .filter(p -> (p.getTitre().toLowerCase().contains(query) || 
                             p.getEtablissement().toLowerCase().contains(query)))
                .collect(Collectors.toList());

        afficherCartes(filtered);
    }

    private void afficherCartes(List<Parcours> list) {
        flowPaneParcours.getChildren().clear();
        for (Parcours p : list) {
            VBox card = creerCard(p);
            flowPaneParcours.getChildren().add(card);
        }
    }

    private VBox creerCard(Parcours p) {
        VBox card = new VBox(10);
        card.setPrefWidth(300);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 20; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 4);");

        Label lblType = new Label(p.getTypeParcours().toUpperCase());
        lblType.setStyle("-fx-background-color: #e0f2fe; -fx-text-fill: #0369a1; -fx-padding: 2 10; -fx-background-radius: 10; -fx-font-size: 10px; -fx-font-weight: bold;");

        Label lblTitre = new Label(p.getTitre());
        lblTitre.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #102c59;");
        lblTitre.setWrapText(true);

        Label lblEcole = new Label("📍 " + p.getEtablissement());
        lblEcole.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");

        Label lblDates = new Label("📅 " + p.getDateDebut() + " - " + (p.getDateFin() != null ? p.getDateFin() : "Présent"));
        lblDates.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button btnEdit = new Button("✏");
        Button btnDelete = new Button("🗑");
        btnEdit.setStyle("-fx-background-color: #f1f5f9; -fx-cursor: hand;");
        btnDelete.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #ef4444; -fx-cursor: hand;");

        btnEdit.setOnAction(e -> modifierParcours(p));
        btnDelete.setOnAction(e -> supprimerParcours(p));

        actions.getChildren().addAll(btnEdit, btnDelete);
        card.getChildren().addAll(lblType, lblTitre, lblEcole, lblDates, spacer, actions);
        return card;
    }

    @FXML
    private void ajouterParcours() {
        ouvrirFormulaire(null);
    }

    public void modifierParcours(Parcours p) {
        ouvrirFormulaire(p);
    }

    public void supprimerParcours(Parcours p) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ce parcours ?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                try {
                    parcoursService.deleteEntity(p);
                    chargerDonnees();
                } catch (SQLException e) {
                    afficherErreur("Erreur", e.getMessage());
                }
            }
        });
    }

    public void voirProjets(Parcours p) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AfficherProjets.fxml"));
            Parent view = loader.load();
            AfficherProjetsController controller = loader.getController();
            controller.initData(p);
            
            Scene scene = flowPaneParcours.getScene();
            if (scene != null) {
                BorderPane mainLayout = (BorderPane) scene.lookup("#mainContainer");
                if (mainLayout != null) mainLayout.setCenter(view);
                else if (scene.getRoot() instanceof BorderPane) ((BorderPane) scene.getRoot()).setCenter(view);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void ouvrirFormulaire(Parcours p) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterParcours.fxml"));
            Parent root = loader.load();
            if (p != null) {
                AjouterParcoursController ctrl = loader.getController();
                ctrl.setParcours(p);
            }
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            chargerDonnees();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    private void genererCV() {
        try {
            ProjetService ps = new ProjetService();
            CVService.genererEtOuvrirCV(parcoursService.getData(), ps.getData());
        } catch (Exception e) { afficherErreur("Erreur PDF", e.getMessage()); }
    }

    @FXML
    private void ouvrirAnalyseCV() {
        naviguer("/AnalyseCV.fxml");
    }

    @FXML
    private void ouvrirRecommandationsIA() {
        naviguer("/RecommendationIA.fxml");
    }

    private void naviguer(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent view = loader.load();
            
            // Tentative de récupération depuis n'importe quel élément de la scène actuelle
            Scene scene = flowPaneParcours.getScene();
            if (scene != null) {
                BorderPane mainLayout = (BorderPane) scene.lookup("#mainContainer");
                if (mainLayout != null) {
                    mainLayout.setCenter(view);
                } else {
                    // Si lookup échoue, on tente de remonter le root
                    if (scene.getRoot() instanceof BorderPane) {
                        ((BorderPane) scene.getRoot()).setCenter(view);
                    }
                }
            }
        } catch (IOException e) {
            afficherErreur("Erreur", "Navigation impossible vers " + fxml);
            e.printStackTrace();
        }
    }

    private void afficherErreur(String titre, String message) {
        new Alert(Alert.AlertType.ERROR, message).show();
    }
}
