package edu.connection3a36.controllers;

import edu.connection3a36.entities.Projet;
import edu.connection3a36.entities.Ressource;
import edu.connection3a36.services.RessourceService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.sql.SQLException;
import java.util.List;

public class DetailsProjetController {
    @FXML private Label lblTitre, lblTypeBadge, lblTechnologies, lblPeriode, lblDescription, lblRessourcesCount;
    @FXML private TableView<Ressource> tableRessources;
    @FXML private TableColumn<Ressource, String> colNom, colType;
    private final RessourceService rs = new RessourceService();

    public void setProjet(Projet p) {
        lblTitre.setText(p.getTitre());
        lblTypeBadge.setText(p.getType());
        lblTechnologies.setText(p.getTechnologies());
        String end = p.getDateFin() != null ? p.getDateFin().toString() : "Présent";
        lblPeriode.setText(p.getDateDebut() + " - " + end);
        lblDescription.setText(p.getDescription());

        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colType.setCellValueFactory(new PropertyValueFactory<>("typeRessource"));
        try {
            List<Ressource> resources = rs.getByProjetId(p.getId());
            lblRessourcesCount.setText(resources.size() + " document(s) disponible(s)");
            tableRessources.setItems(FXCollections.observableArrayList(resources));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    private void retour() {
        MainController.getInstance().showBackProjets();
    }
}
