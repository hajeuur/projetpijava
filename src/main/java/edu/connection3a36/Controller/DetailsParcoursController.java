package edu.connection3a36.Controller;

import edu.connection3a36.entities.Parcours;
import edu.connection3a36.entities.Projet;
import edu.connection3a36.services.ProjetService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import java.io.IOException;
import java.sql.SQLException;

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
            tableProjets.setItems(FXCollections.observableArrayList(ps.getByParcoursId(p.getId())));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void retour() {
        try {
            ((BorderPane) lblTitre.getScene().getRoot())
                    .setCenter(new FXMLLoader(getClass().getResource("/BackOfficeParcours.fxml")).load());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
