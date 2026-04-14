package edu.connection3a36.Controller;

import edu.connection3a36.entities.Projet;
import edu.connection3a36.entities.Ressource;
import edu.connection3a36.services.RessourceService;
import javafx.collections.FXCollections;
import javafx.scene.Parent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

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
        String end = p.getDateFin() != null ? p.getDateFin().toString() : "Présent";
        lblPeriode.setText(p.getDateDebut() + " - " + end);
        lblDescription.setText(p.getDescription());

        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colType.setCellValueFactory(new PropertyValueFactory<>("typeRessource"));
        try {
            List<Ressource> resources = rs.getByProjetId(p.getId());
            lblRessourcesCount.setText(resources.size() + " document(s) disponible(s)");
            tableRessources.setItems(FXCollections.observableArrayList(resources));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void retour() {
        try {
            Parent view = new FXMLLoader(getClass().getResource("/BackOfficeProjets.fxml")).load();
            javafx.scene.layout.StackPane center = (javafx.scene.layout.StackPane) lblTitre.getScene()
                    .lookup("#centerContent");
            if (center != null)
                center.getChildren().setAll(view);
            else
                ((BorderPane) lblTitre.getScene().getRoot()).setCenter(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
