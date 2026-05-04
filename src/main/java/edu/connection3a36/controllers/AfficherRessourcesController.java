package edu.connection3a36.controllers;

import edu.connection3a36.entities.Projet;
import edu.connection3a36.entities.Ressource;
import edu.connection3a36.services.RessourceService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class AfficherRessourcesController implements Initializable {

    @FXML private ListView<Ressource> listRessources;
    @FXML private Label lblProjetTitre;
    @FXML private Label lblDescription;
    @FXML private Label lblType;
    @FXML private Label lblUrl;

    private final RessourceService ressourceService = new RessourceService();
    private Projet projetActuel;
    private ObservableList<Ressource> ressourcesData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        listRessources.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) afficherDetails(newV);
        });
    }

    public void initData(Projet p) {
        this.projetActuel = p;
        lblProjetTitre.setText("Ressources : " + p.getTitre());
        chargerDonnees();
    }

    private void chargerDonnees() {
        try {
            List<Ressource> list = ressourceService.getByProjetId(projetActuel.getId());
            ressourcesData.setAll(list);
            listRessources.setItems(ressourcesData);
            if (!list.isEmpty()) listRessources.getSelectionModel().select(0);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void afficherDetails(Ressource r) {
        lblDescription.setText(r.getDescription());
        lblType.setText(r.getTypeRessource());
        lblUrl.setText(r.getUrlRessource());
    }

    @FXML
    private void ajouterRessource() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterRessource.fxml"));
            Parent root = loader.load();
            AjouterRessourceController ctrl = loader.getController();
            ctrl.initData(projetActuel);
            
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            chargerDonnees();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    private void modifierRessource() {
        Ressource selected = listRessources.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ModifierRessource.fxml"));
            Parent root = loader.load();
            ModifierRessourceController ctrl = loader.getController();
            ctrl.initData(selected, projetActuel);
            
            MainController.getInstance().loadInContentArea(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    private void supprimerRessource() {
        Ressource selected = listRessources.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        if (new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ?", ButtonType.YES, ButtonType.NO).showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try {
                ressourceService.deleteEntity(selected);
                chargerDonnees();
            } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    @FXML
    private void retour() {
        MainController.getInstance().showProjets();
    }
}
