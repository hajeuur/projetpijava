package edu.connection3a36.Controller;

import edu.connection3a36.entities.Projet;
import edu.connection3a36.entities.Ressource;
import edu.connection3a36.services.RessourceService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import java.io.IOException;
import java.sql.SQLException;

public class DetailsProjetController {
    @FXML
    private Label lblTitre, lblTypeBadge, lblTechnologies, lblPeriode, lblDescription, lblRessourcesCount;
    @FXML
    private TableView<Ressource> tableRessources;
    @FXML
    private TableColumn<Ressource, String> colNom, colType;
    private final RessourceService rs = new RessourceService();

    public void setProjet(Projet p) {
        lblTitre.setText(p.getTitre());
        lblTypeBadge.setText(p.getType());
        lblTechnologies.setText(p.getTechnologies());
        lblPeriode.setText(p.getDateDebut() + " - " + p.getDateFin());
        lblDescription.setText(p.getDescription());

        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colType.setCellValueFactory(new PropertyValueFactory<>("typeRessource"));
        try {
            tableRessources.setItems(FXCollections.observableArrayList(rs.getByProjetId(p.getId())));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void retour() {
        try {
            ((BorderPane) lblTitre.getScene().getRoot())
                    .setCenter(new FXMLLoader(getClass().getResource("/BackOfficeProjets.fxml")).load());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
