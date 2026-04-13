package edu.connection3a36.Controller;

import edu.connection3a36.entities.Projet;
import edu.connection3a36.services.ProjetService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AfficherProjetsGlobalController implements Initializable {

    @FXML
    private FlowPane flowPaneProjets;
    @FXML
    private TextField txtRecherche;
    @FXML
    private Button btnAjouter;

    private final ProjetService projetService = new ProjetService();
    private ObservableList<Projet> allProjets = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        txtRecherche.textProperty().addListener((obs, oldV, newV) -> filterData());
        chargerDonnees();
    }

    public void chargerDonnees() {
        try {
            List<Projet> list = projetService.getData();
            allProjets = FXCollections.observableArrayList(list);
            filterData();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void filterData() {
        String search = txtRecherche.getText() == null ? "" : txtRecherche.getText().toLowerCase().trim();
        List<Projet> filtered = allProjets.stream()
                .filter(p -> p.getTitre().toLowerCase().contains(search) ||
                        (p.getTechnologies() != null && p.getTechnologies().toLowerCase().contains(search)))
                .collect(Collectors.toList());
        displayCards(filtered);
    }

    private void displayCards(List<Projet> list) {
        flowPaneProjets.getChildren().clear();
        for (Projet p : list) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/ProjetCard.fxml"));
                loader.setController(new ProjetCardController());
                Parent card = loader.load();
                ProjetCardController ctrl = loader.getController();
                ctrl.setData(p, this);
                flowPaneProjets.getChildren().add(card);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void ajouterProjet() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AfficherProjets.fxml"));
            Parent root = loader.load();
            AfficherProjetsController controller = loader.getController();
            controller.initData(null); // Global mode (null = no pre-selected parcours)

            Stage stage = new Stage();
            stage.setTitle("Gestion Manuelle des Projets");
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.setOnHidden(e -> chargerDonnees());
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void modifierProjetSpecific(Projet p) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AfficherProjets.fxml"));
            Parent root = loader.load();
            AfficherProjetsController controller = loader.getController();
            controller.initData(null); // Load all to allow switching
            // We'll need a way to select the project in the list
            // For now, it will load everything and user can pick it, or we add a select
            // method

            Stage stage = new Stage();
            stage.setTitle("Modifier : " + p.getTitre());
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.setOnHidden(e -> chargerDonnees());
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void supprimerProjetSpecific(Projet p) {
        Optional<ButtonType> result = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer le projet ?", ButtonType.YES,
                ButtonType.NO).showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            try {
                projetService.deleteEntity(p);
                chargerDonnees();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void afficherInfo(String titre, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titre);
        alert.setContentText(msg);
        alert.show();
    }
}
