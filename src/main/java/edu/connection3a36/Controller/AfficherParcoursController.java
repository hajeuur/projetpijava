package edu.connection3a36.Controller;

import edu.connection3a36.entities.Parcours;
import edu.connection3a36.services.ParcoursService;
import edu.connection3a36.tools.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AfficherParcoursController implements Initializable {

    @FXML
    private FlowPane flowPaneParcours;
    @FXML
    private TextField txtRecherche;
    @FXML
    private ComboBox<String> cbFiltreType;
    @FXML
    private Label lblStats;
    @FXML
    private Button btnAjouter;

    private final ParcoursService parcoursService = new ParcoursService();
    private ObservableList<Parcours> allParcours = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cbFiltreType.setItems(FXCollections.observableArrayList(
                "Tous", "Formation", "Stage", "Alternance", "Emploi", "Personnel"));
        cbFiltreType.setValue("Tous");

        txtRecherche.textProperty().addListener((observable, oldValue, newValue) -> filterData());

        /* Enabling CRUD in Front Office as requested */
        /*
         * if (SessionManager.getInstance().getCurrentUser() != null) {
         * String role = SessionManager.getInstance().getCurrentUser().getRole();
         * if (!("ROLE_ADMIN".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role)))
         * {
         * btnAjouter.setVisible(false);
         * btnAjouter.setManaged(false);
         * }
         * }
         */

        chargerDonnees();
    }

    public void chargerDonnees() {
        try {
            List<Parcours> list = parcoursService.getData();
            allParcours = FXCollections.observableArrayList(list);
            filterData();
            updateStats();
        } catch (SQLException e) {
            afficherErreur("Erreur de chargement", e.getMessage());
        }
    }

    private void filterData() {
        String searchText = txtRecherche.getText() == null ? "" : txtRecherche.getText().toLowerCase().trim();
        String selectedType = cbFiltreType.getValue();

        List<Parcours> filtered = allParcours.stream()
                .filter(p -> {
                    boolean matchesSearch = p.getTitre().toLowerCase().contains(searchText) ||
                            p.getEtablissement().toLowerCase().contains(searchText) ||
                            p.getDiplome().toLowerCase().contains(searchText);
                    boolean matchesType = selectedType.equals("Tous") ||
                            (p.getTypeParcours() != null && p.getTypeParcours().equalsIgnoreCase(selectedType));
                    return matchesSearch && matchesType;
                })
                .collect(Collectors.toList());

        displayCards(filtered);
    }

    private void displayCards(List<Parcours> list) {
        flowPaneParcours.getChildren().clear();
        for (Parcours p : list) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/ParcoursCard.fxml"));
                loader.setController(new ParcoursCardController());
                Parent card = loader.load();
                ParcoursCardController controller = loader.getController();
                controller.setData(p, this);
                flowPaneParcours.getChildren().add(card);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateStats() {
        int count = allParcours.size();
        lblStats.setText("Nombre total de parcours : " + count);
    }

    @FXML
    private void ajouterParcours() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterParcours.fxml"));
            Parent view = loader.load();
            ((BorderPane) flowPaneParcours.getScene().getRoot()).setCenter(view);
        } catch (IOException e) {
            afficherErreur("Erreur", e.getMessage());
        }
    }

    public void modifierParcoursSpecific(Parcours p) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ModifierParcours.fxml"));
            Parent view = loader.load();
            ModifierParcoursController controller = loader.getController();
            controller.initData(p);
            ((BorderPane) flowPaneParcours.getScene().getRoot()).setCenter(view);
        } catch (IOException e) {
            afficherErreur("Erreur", e.getMessage());
        }
    }

    public void supprimerParcoursSpecific(Parcours p) {
        Optional<ButtonType> result = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer le parcours \"" + p.getTitre() + "\" ?",
                ButtonType.YES, ButtonType.NO).showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            try {
                parcoursService.deleteEntity(p);
                chargerDonnees();
            } catch (SQLException e) {
                afficherErreur("Erreur suppression", e.getMessage());
            }
        }
    }

    public void voirProjetsSpecific(Parcours p) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AfficherProjets.fxml"));
            Parent view = loader.load();
            AfficherProjetsController controller = loader.getController();
            controller.initData(p);
            ((BorderPane) flowPaneParcours.getScene().getRoot()).setCenter(view);
        } catch (IOException e) {
            afficherErreur("Erreur", e.getMessage());
        }
    }

    @FXML
    private void filtrerParType() {
        filterData();
    }

    @FXML
    private void ouvrirRecommandationsIA() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/RecommendationIA.fxml"));
            Parent view = loader.load();
            
            // Trouver le conteneur principal (BorderPane) dans la scène actuelle
            // On cherche par ID "#mainContainer" qui est défini dans FrontLayout.fxml
            BorderPane mainLayout = (BorderPane) txtRecherche.getScene().lookup("#mainContainer");
            
            if (mainLayout != null) {
                mainLayout.setCenter(view);
            } else {
                // Secours : si pas trouvé par ID, on essaie de remonter les parents
                Parent parent = txtRecherche.getParent();
                while (parent != null && !(parent instanceof BorderPane)) {
                    parent = parent.getParent();
                }
                if (parent instanceof BorderPane) {
                    ((BorderPane) parent).setCenter(view);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void afficherErreur(String titre, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titre);
        alert.setContentText(msg);
        alert.show();
    }
}
