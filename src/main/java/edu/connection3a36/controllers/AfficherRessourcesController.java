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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AfficherRessourcesController implements Initializable {

    @FXML private Label lblProjetNom;
    @FXML private ComboBox<String> cbFiltreType;
    @FXML private TableView<Ressource> tableRessources;
    @FXML private TableColumn<Ressource, Integer> colId;
    @FXML private TableColumn<Ressource, String> colNom;
    @FXML private TableColumn<Ressource, String> colType;
    @FXML private TableColumn<Ressource, String> colUrl;
    @FXML private TableColumn<Ressource, String> colDateCreation;
    @FXML private FlowPane flowPaneRessources;

    private final RessourceService ressourceService = new RessourceService();
    private Projet projetActuel;
    private ObservableList<Ressource> ressourcesData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        cbFiltreType.setItems(FXCollections.observableArrayList("Tous", "PDF", "VIDEO", "LIEN", "ARTICLE", "AUTRE"));
        cbFiltreType.setValue("Tous");
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colType.setCellValueFactory(new PropertyValueFactory<>("typeRessource"));
        colUrl.setCellValueFactory(new PropertyValueFactory<>("urlRessource"));
        colDateCreation.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));
    }

    public void initData(Projet p) {
        this.projetActuel = p;
        if (lblProjetNom != null) {
            lblProjetNom.setText("Ressources : " + p.getTitre());
        }
        chargerDonnees();
    }

    private void chargerDonnees() {
        try {
            List<Ressource> list = ressourceService.getByProjetId(projetActuel.getId());
            ressourcesData.setAll(list);
            tableRessources.setItems(ressourcesData);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void filtrerParType() {
        String type = cbFiltreType.getValue();
        if (type == null || type.equals("Tous")) {
            tableRessources.setItems(ressourcesData);
        } else {
            List<Ressource> filtered = ressourcesData.stream()
                    .filter(r -> r.getTypeRessource().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
            tableRessources.setItems(FXCollections.observableArrayList(filtered));
        }
    }

    @FXML
    private void handleTableClick() {
        // Optionnel : on pourrait faire quelque chose lors du clic
    }

    @FXML
    private void ouvrirLien() {
        Ressource selected = tableRessources.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getUrlRessource() == null) return;
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(selected.getUrlRessource()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void modifierRessource() {
        Ressource selected = tableRessources.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ModifierRessource.fxml"));
            Parent root = loader.load();
            ModifierRessourceController ctrl = loader.getController();
            ctrl.initData(selected, projetActuel);

            MainController.getInstance().loadInContentArea(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void supprimerRessource() {
        Ressource selected = tableRessources.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Voulez-vous vraiment supprimer cette ressource ?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText(null);
        
        if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try {
                ressourceService.deleteEntity(selected);
                chargerDonnees();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void retour() {
        MainController.getInstance().showProjets();
    }
}

