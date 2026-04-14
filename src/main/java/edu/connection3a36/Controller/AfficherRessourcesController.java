package edu.connection3a36.Controller;

import edu.connection3a36.entities.Projet;
import edu.connection3a36.entities.Ressource;
import edu.connection3a36.services.RessourceService;
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
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class AfficherRessourcesController implements Initializable {

    @FXML
    private Label lblProjetNom;
    @FXML
    private TableView<Ressource> tableRessources;
    @FXML
    private TableColumn<Ressource, Integer> colId;
    @FXML
    private TableColumn<Ressource, String> colNom;
    @FXML
    private TableColumn<Ressource, String> colType;
    @FXML
    private TableColumn<Ressource, String> colUrl;
    @FXML
    private TableColumn<Ressource, String> colDateCreation;
    @FXML
    private ComboBox<String> cbFiltreType;

    @FXML
    private Button btnAjouter;
    @FXML
    private Button btnModifier;
    @FXML
    private Button btnSupprimer;

    private final RessourceService ressourceService = new RessourceService();
    private Projet projetActuel;
    private ObservableList<Ressource> ressourcesData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colType.setCellValueFactory(new PropertyValueFactory<>("typeRessource"));
        colUrl.setCellValueFactory(new PropertyValueFactory<>("urlRessource"));
        colDateCreation.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getDateCreation() != null ? cellData.getValue().getDateCreation().toString() : ""));

        cbFiltreType.setItems(FXCollections.observableArrayList(
                "Tous", "PDF", "VIDEO", "LIEN", "ARTICLE", "AUTRE"));
        cbFiltreType.setValue("Tous");

        /* Enabling CRUD in Front as requested */
        /*
         * if (edu.connection3a36.tools.SessionManager.getInstance().getCurrentUser() !=
         * null) {
         * String role =
         * edu.connection3a36.tools.SessionManager.getInstance().getCurrentUser().
         * getRole();
         * if ("ROLE_ADMIN".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role)) {
         * if (btnAjouter != null) {
         * btnAjouter.setVisible(false);
         * btnAjouter.setManaged(false);
         * }
         * if (btnModifier != null) {
         * btnModifier.setVisible(false);
         * btnModifier.setManaged(false);
         * }
         * if (btnSupprimer != null) {
         * btnSupprimer.setVisible(false);
         * btnSupprimer.setManaged(false);
         * }
         * }
         * }
         */
    }

    public void initData(Projet projet) {
        this.projetActuel = projet;
        lblProjetNom.setText("Ressources du projet : " + projet.getTitre());
        chargerDonnees();
    }

    private void chargerDonnees() {
        try {
            List<Ressource> liste = ressourceService.getByProjetId(projetActuel.getId());
            ressourcesData = FXCollections.observableArrayList(liste);
            tableRessources.setItems(ressourcesData);
        } catch (SQLException e) {
            afficherErreur("Erreur de chargement", e.getMessage());
        }
    }

    @FXML
    private void filtrerParType() {
        String type = cbFiltreType.getValue();
        try {
            List<Ressource> liste;
            if (type == null || type.equals("Tous")) {
                liste = ressourceService.getByProjetId(projetActuel.getId());
            } else {
                liste = ressourceService.filterByType(type);
            }
            ressourcesData = FXCollections.observableArrayList(liste);
            tableRessources.setItems(ressourcesData);
        } catch (SQLException e) {
            afficherErreur("Erreur de filtrage", e.getMessage());
        }
    }

    @FXML
    private void ajouterRessource() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterRessource.fxml"));
            Parent view = loader.load();
            AjouterRessourceController controller = loader.getController();
            controller.initData(projetActuel);
            ((BorderPane) tableRessources.getScene().getRoot()).setCenter(view);
        } catch (IOException e) {
            afficherErreur("Erreur d'ouverture", e.getMessage());
        }
    }

    @FXML
    private void modifierRessource() {
        Ressource selected = tableRessources.getSelectionModel().getSelectedItem();
        if (selected == null) {
            afficherAvertissement("Sélection requise", "Veuillez sélectionner une ressource à modifier.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ModifierRessource.fxml"));
            Parent view = loader.load();
            ModifierRessourceController controller = loader.getController();
            controller.initData(selected, projetActuel);
            ((BorderPane) tableRessources.getScene().getRoot()).setCenter(view);
        } catch (IOException e) {
            afficherErreur("Erreur d'ouverture", e.getMessage());
        }
    }

    @FXML
    private void ouvrirLien() {
        Ressource selected = tableRessources.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getUrlRessource() != null && !selected.getUrlRessource().isEmpty()) {
            try {
                // Utilisation de HostServices (via MainFx ou singleton si possible) ou
                // java.awt.Desktop
                String url = selected.getUrlRessource().trim();
                if (!url.startsWith("http"))
                    url = "http://" + url;

                java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
            } catch (Exception e) {
                afficherErreur("Erreur", "Impossible d'ouvrir le lien : " + e.getMessage());
            }
        } else {
            afficherErreur("Attention", "Aucun lien URL disponible pour cette ressource.");
        }
    }

    @FXML
    private void handleTableClick(javafx.scene.input.MouseEvent event) {
        if (event.getClickCount() == 2) {
            ouvrirLien();
        }
    }

    @FXML
    private void retour() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AfficherProjets.fxml"));
            Parent view = loader.load();
            AfficherProjetsController controller = loader.getController();

            // Recharger les projets du même parcours
            edu.connection3a36.services.ParcoursService ps = new edu.connection3a36.services.ParcoursService();
            edu.connection3a36.entities.Parcours parc = ps.getById(projetActuel.getParcoursId());
            controller.initData(parc);

            ((BorderPane) tableRessources.getScene().getRoot()).setCenter(view);
        } catch (Exception e) {
            e.printStackTrace();
            afficherErreur("Erreur", "Impossible de retourner aux projets : " + e.getMessage());
        }
    }

    @FXML
    private void supprimerRessource() {
        Ressource selected = tableRessources.getSelectionModel().getSelectedItem();
        if (selected == null) {
            afficherAvertissement("Sélection requise", "Veuillez sélectionner une ressource à supprimer.");
            return;
        }
        Optional<ButtonType> result = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer la ressource \"" + selected.getNom() + "\" ?",
                ButtonType.YES, ButtonType.NO).showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            try {
                ressourceService.deleteEntity(selected);
                chargerDonnees();
                afficherInfo("Succès", "Ressource supprimée avec succès.");
            } catch (SQLException e) {
                afficherErreur("Erreur de suppression", e.getMessage());
            }
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
