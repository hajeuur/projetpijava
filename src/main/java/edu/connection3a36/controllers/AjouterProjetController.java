package edu.connection3a36.controllers;

import edu.connection3a36.entities.Parcours;
import edu.connection3a36.entities.Projet;
import edu.connection3a36.services.ParcoursService;
import edu.connection3a36.services.ProjetService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class AjouterProjetController implements Initializable {

    @FXML private TextField txtTitre;
    @FXML private TextField txtType;
    @FXML private TextArea taDescription;
    @FXML private TextField txtTechnologies;
    @FXML private DatePicker dpDateDebut;
    @FXML private DatePicker dpDateFin;
    @FXML private ComboBox<Parcours> cbParcours;
    @FXML private Label lblErreur;

    private final ProjetService projetService = new ProjetService();
    private final ParcoursService parcoursService = new ParcoursService();
    private int idToUpdate = -1;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            cbParcours.setItems(FXCollections.observableArrayList(parcoursService.getData()));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setProjet(Projet p) {
        if (p == null) return;
        this.idToUpdate = p.getId();
        txtTitre.setText(p.getTitre());
        txtType.setText(p.getType());
        taDescription.setText(p.getDescription());
        txtTechnologies.setText(p.getTechnologies());
        dpDateDebut.setValue(p.getDateDebut());
        dpDateFin.setValue(p.getDateFin());
        
        cbParcours.getItems().stream()
                .filter(pa -> pa.getId() == p.getParcoursId())
                .findFirst()
                .ifPresent(cbParcours::setValue);
    }

    @FXML
    private void enregistrer() {
        if (txtTitre.getText().trim().isEmpty()) {
            lblErreur.setText("Titre obligatoire");
            return;
        }

        Projet p = new Projet();
        p.setTitre(txtTitre.getText().trim());
        p.setType(txtType.getText().trim());
        p.setDescription(taDescription.getText().trim());
        p.setTechnologies(txtTechnologies.getText().trim());
        p.setDateDebut(dpDateDebut.getValue());
        p.setDateFin(dpDateFin.getValue());
        p.setParcoursId(cbParcours.getValue() != null ? cbParcours.getValue().getId() : 0);

        try {
            if (idToUpdate == -1) {
                projetService.addEntity(p);
            } else {
                p.setId(idToUpdate);
                projetService.updateEntity(idToUpdate, p);
            }
            fermer();
        } catch (SQLException e) {
            lblErreur.setText(e.getMessage());
        }
    }

    @FXML
    private void annuler() {
        fermer();
    }

    private void fermer() {
        Stage stage = (Stage) txtTitre.getScene().getWindow();
        stage.close();
    }
}
