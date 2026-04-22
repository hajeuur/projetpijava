package edu.connection3a36.controllers;

import edu.connection3a36.entities.Parcours;
import edu.connection3a36.entities.Projet;
import edu.connection3a36.services.ProjetService;
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

public class DetailsParcoursController {
    @FXML
    private Label lblTitre, lblTypeBadge, lblEtablissement, lblDiplome, lblSpecialite, lblPeriode, lblDescription,
            lblProjetsCount;
    @FXML
    private TableView<Projet> tableProjets;
    @FXML
    private TableColumn<Projet, String> colProjet, colType;
    private final ProjetService ps = new ProjetService();

    public void setParcours(Parcours p) {
        lblTitre.setText(p.getTitre());
        lblTypeBadge.setText(p.getTypeParcours());
        lblEtablissement.setText(p.getEtablissement() != null ? p.getEtablissement() : p.getEntreprise());
        lblDiplome.setText(p.getDiplome());
        lblSpecialite.setText(p.getSpecialite());
        lblPeriode.setText(p.getDateDebut() + " - " + p.getDateFin());
        lblDescription.setText(p.getDescription());

        colProjet.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        try {
            List<Projet> projects = ps.getByParcoursId(p.getId());
            lblProjetsCount.setText(String.valueOf(projects.size()));
            tableProjets.setItems(FXCollections.observableArrayList(projects));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void retour() {
        try {
            Parent view = new FXMLLoader(getClass().getResource("/BackOfficeParcours.fxml")).load();
            javafx.scene.layout.StackPane center = (javafx.scene.layout.StackPane) lblTitre.getScene()
                    .lookup("#centerContent");
            if (center != null)
                center.getChildren().setAll(view);
            else
                MainController.getInstance().loadInContentArea(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
