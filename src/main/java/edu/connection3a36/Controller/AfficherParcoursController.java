package edu.connection3a36.Controller;

import edu.connection3a36.entities.Parcours;
import edu.connection3a36.services.ParcoursService;
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

public class AfficherParcoursController implements Initializable {

    @FXML
    private TableView<Parcours> tableParcours;
    @FXML
    private TableColumn<Parcours, Integer> colId;
    @FXML
    private TableColumn<Parcours, String> colType;
    @FXML
    private TableColumn<Parcours, String> colTitre;
    @FXML
    private TableColumn<Parcours, String> colEtablissement;
    @FXML
    private TableColumn<Parcours, String> colDiplome;
    @FXML
    private TableColumn<Parcours, String> colDateDebut;
    @FXML
    private TableColumn<Parcours, String> colDateFin;
    @FXML
    private TextField txtRecherche;
    @FXML
    private ComboBox<String> cbFiltreType;
    @FXML
    private Label lblStats;

    @FXML
    private Button btnAjouter;
    @FXML
    private Button btnModifier;
    @FXML
    private Button btnSupprimer;

    private final ParcoursService parcoursService = new ParcoursService();
    private ObservableList<Parcours> parcoursData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (colId != null)
            colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        if (colType != null)
            colType.setCellValueFactory(new PropertyValueFactory<>("typeParcours"));
        if (colTitre != null)
            colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        if (colEtablissement != null)
            colEtablissement.setCellValueFactory(new PropertyValueFactory<>("etablissement"));
        if (colDiplome != null)
            colDiplome.setCellValueFactory(new PropertyValueFactory<>("diplome"));

        // Affichage des dates sous forme de String
        if (colDateDebut != null)
            colDateDebut.setCellValueFactory(cellData -> {
                Parcours p = cellData.getValue();
                return new javafx.beans.property.SimpleStringProperty(
                        p.getDateDebut() != null ? p.getDateDebut().toString() : "");
            });
        if (colDateFin != null)
            colDateFin.setCellValueFactory(cellData -> {
                Parcours p = cellData.getValue();
                return new javafx.beans.property.SimpleStringProperty(
                        p.getDateFin() != null ? p.getDateFin().toString() : "");
            });

        cbFiltreType.setItems(FXCollections.observableArrayList(
                "Tous", "Formation", "Stage", "Alternance", "Emploi", "Personnel"));
        cbFiltreType.setValue("Tous");

        // Gestion affichage/masquage CRUD selon si Front (User/Etudiant) ou Back
        // (Admin)
        if (edu.connection3a36.tools.SessionManager.getInstance().getCurrentUser() != null) {
            String role = edu.connection3a36.tools.SessionManager.getInstance().getCurrentUser().getRole();
            if ("ROLE_ADMIN".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role)) {
                // "juste laffichage dans le back" -> Admin ne voit pas les boutons CRUD
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

        chargerDonnees();
    }

    private void chargerDonnees() {
        try {
            List<Parcours> liste = parcoursService.getData();
            parcoursData = FXCollections.observableArrayList(liste);
            tableParcours.setItems(parcoursData);
            mettreAJourStats();
        } catch (SQLException e) {
            afficherErreur("Erreur de chargement", e.getMessage());
        }
    }

    @FXML
    private void rechercherParcours() {
        String keyword = txtRecherche.getText().trim();
        try {
            List<Parcours> liste;
            if (keyword.isEmpty()) {
                liste = parcoursService.getData();
            } else {
                liste = parcoursService.searchByTitre(keyword);
            }
            parcoursData = FXCollections.observableArrayList(liste);
            tableParcours.setItems(parcoursData);
        } catch (SQLException e) {
            afficherErreur("Erreur de recherche", e.getMessage());
        }
    }

    @FXML
    private void filtrerParType() {
        String type = cbFiltreType.getValue();
        try {
            List<Parcours> liste;
            if (type == null || type.equals("Tous")) {
                liste = parcoursService.getData();
            } else {
                liste = parcoursService.filterByType(type);
            }
            parcoursData = FXCollections.observableArrayList(liste);
            tableParcours.setItems(parcoursData);
        } catch (SQLException e) {
            afficherErreur("Erreur de filtrage", e.getMessage());
        }
    }

    @FXML
    private void ajouterParcours() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterParcours.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Ajouter un Parcours");
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
    private void modifierParcours() {
        Parcours selected = tableParcours.getSelectionModel().getSelectedItem();
        if (selected == null) {
            afficherAvertissement("Sélection requise", "Veuillez sélectionner un parcours à modifier.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ModifierParcours.fxml"));
            Parent root = loader.load();
            ModifierParcoursController controller = loader.getController();
            controller.initData(selected);
            Stage stage = new Stage();
            stage.setTitle("Modifier le Parcours");
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
    private void supprimerParcours() {
        Parcours selected = tableParcours.getSelectionModel().getSelectedItem();
        if (selected == null) {
            afficherAvertissement("Sélection requise", "Veuillez sélectionner un parcours à supprimer.");
            return;
        }
        Optional<ButtonType> result = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer le parcours \"" + selected.getTitre() + "\" et tous ses projets ?",
                ButtonType.YES, ButtonType.NO).showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            try {
                parcoursService.deleteEntity(selected);
                chargerDonnees();
                afficherInfo("Succès", "Parcours supprimé avec succès.");
            } catch (SQLException e) {
                afficherErreur("Erreur de suppression", e.getMessage());
            }
        }
    }

    @FXML
    private void voirProjets() {
        Parcours selected = tableParcours.getSelectionModel().getSelectedItem();
        if (selected == null) {
            afficherAvertissement("Sélection requise", "Veuillez sélectionner un parcours pour voir ses projets.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AfficherProjets.fxml"));
            Parent root = loader.load();
            AfficherProjetsController controller = loader.getController();
            controller.initData(selected);
            Stage stage = new Stage();
            stage.setTitle("Projets du parcours : " + selected.getTitre());
            stage.setScene(new Scene(root, 950, 650));
            stage.setResizable(false);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            afficherErreur("Erreur d'ouverture", e.getMessage());
        }
    }

    private void mettreAJourStats() {
        try {
            int total = parcoursService.getData().size();
            int formations = parcoursService.countByType("Formation");
            int stages = parcoursService.countByType("Stage");
            lblStats.setText(String.format("Total: %d | Formations: %d | Stages: %d", total, formations, stages));
        } catch (SQLException e) {
            lblStats.setText("Statistiques non disponibles");
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
