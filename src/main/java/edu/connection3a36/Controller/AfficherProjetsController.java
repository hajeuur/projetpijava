package edu.connection3a36.Controller;

import edu.connection3a36.entities.Parcours;
import edu.connection3a36.entities.Projet;
import edu.connection3a36.services.ProjetService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class AfficherProjetsController implements Initializable {

    @FXML
    private Label lblParcoursNom;
    @FXML
    private TableView<Projet> tableProjets;
    @FXML
    private TableColumn<Projet, Integer> colId;
    @FXML
    private TableColumn<Projet, String> colTitre;
    @FXML
    private TableColumn<Projet, String> colType;
    @FXML
    private TableColumn<Projet, String> colTechnologies;
    @FXML
    private TableColumn<Projet, String> colDateDebut;
    @FXML
    private TableColumn<Projet, String> colDateFin;
    @FXML
    private TextField txtRecherche;
    @FXML
    private Button btnAjouter;
    @FXML
    private Button btnModifier;
    @FXML
    private Button btnSupprimer;

    private final ProjetService projetService = new ProjetService();
    private Parcours parcoursActuel;
    private ObservableList<Projet> projetsData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colTechnologies.setCellValueFactory(new PropertyValueFactory<>("technologies"));
        colDateDebut.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getDateDebut() != null ? cellData.getValue().getDateDebut().toString() : ""));
        colDateFin.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getDateFin() != null ? cellData.getValue().getDateFin().toString() : ""));

        // Gestion affichage/masquage CRUD selon si Front (User/Etudiant) ou Back
        // (Admin)
        if (edu.connection3a36.tools.SessionManager.getInstance().getCurrentUser() != null) {
            String role = edu.connection3a36.tools.SessionManager.getInstance().getCurrentUser().getRole();
            if ("ROLE_ADMIN".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role)) {
                if (btnAjouter != null) {
                    btnAjouter.setVisible(false);
                    btnAjouter.setManaged(false);
                }
                if (btnModifier != null) {
                    btnModifier.setVisible(false);
                    btnModifier.setManaged(false);
                }
                if (btnSupprimer != null) {
                    btnSupprimer.setVisible(false);
                    btnSupprimer.setManaged(false);
                }
            }
        }
    }

    public void initData(Parcours parcours) {
        this.parcoursActuel = parcours;
        lblParcoursNom.setText("Projets du parcours : " + parcours.getTitre());
        chargerDonnees();
    }

    private void chargerDonnees() {
        try {
            List<Projet> liste = projetService.getByParcoursId(parcoursActuel.getId());
            projetsData = FXCollections.observableArrayList(liste);
            tableProjets.setItems(projetsData);
        } catch (SQLException e) {
            afficherErreur("Erreur de chargement", e.getMessage());
        }
    }

    @FXML
    private void rechercherProjet() {
        String keyword = txtRecherche.getText().trim();
        try {
            List<Projet> liste;
            if (keyword.isEmpty()) {
                liste = projetService.getByParcoursId(parcoursActuel.getId());
            } else {
                liste = projetService.searchByTitre(keyword);
            }
            projetsData = FXCollections.observableArrayList(liste);
            tableProjets.setItems(projetsData);
        } catch (SQLException e) {
            afficherErreur("Erreur de recherche", e.getMessage());
        }
    }

    @FXML
    private void ajouterProjet() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterProjet.fxml"));
            Parent root = loader.load();
            AjouterProjetController controller = loader.getController();
            controller.initData(parcoursActuel);
            Stage stage = new Stage();
            stage.setTitle("Ajouter un Projet");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.centerOnScreen();
            stage.setOnHidden(e -> chargerDonnees());
            stage.show();
        } catch (IOException e) {
            afficherErreur("Erreur d'ouverture", e.getMessage());
        }
    }

    @FXML
    private void modifierProjet() {
        Projet selected = tableProjets.getSelectionModel().getSelectedItem();
        if (selected == null) {
            afficherAvertissement("Sélection requise", "Veuillez sélectionner un projet à modifier.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ModifierProjet.fxml"));
            Parent root = loader.load();
            ModifierProjetController controller = loader.getController();
            controller.initData(selected, parcoursActuel);
            Stage stage = new Stage();
            stage.setTitle("Modifier le Projet");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.centerOnScreen();
            stage.setOnHidden(e -> chargerDonnees());
            stage.show();
        } catch (IOException e) {
            afficherErreur("Erreur d'ouverture", e.getMessage());
        }
    }

    @FXML
    private void supprimerProjet() {
        Projet selected = tableProjets.getSelectionModel().getSelectedItem();
        if (selected == null) {
            afficherAvertissement("Sélection requise", "Veuillez sélectionner un projet à supprimer.");
            return;
        }
        Optional<ButtonType> result = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer le projet \"" + selected.getTitre() + "\" et toutes ses ressources ?",
                ButtonType.YES, ButtonType.NO).showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            try {
                projetService.deleteEntity(selected);
                chargerDonnees();
                afficherInfo("Succès", "Projet supprimé avec succès.");
            } catch (SQLException e) {
                afficherErreur("Erreur de suppression", e.getMessage());
            }
        }
    }

    @FXML
    private void voirRessources() {
        Projet selected = tableProjets.getSelectionModel().getSelectedItem();
        if (selected == null) {
            afficherAvertissement("Sélection requise", "Veuillez sélectionner un projet pour voir ses ressources.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AfficherRessources.fxml"));
            Parent root = loader.load();
            AfficherRessourcesController controller = loader.getController();
            controller.initData(selected);
            Stage stage = new Stage();
            stage.setTitle("Ressources du projet : " + selected.getTitre());
            stage.setScene(new Scene(root, 850, 550));
            stage.setResizable(false);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            afficherErreur("Erreur d'ouverture", e.getMessage());
        }
    }

    private void afficherErreur(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titre);
        alert.setContentText(message);
        alert.show();
    }

    private void afficherAvertissement(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titre);
        alert.setContentText(message);
        alert.show();
    }

    private void afficherInfo(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titre);
        alert.setContentText(message);
        alert.show();
    }
}
