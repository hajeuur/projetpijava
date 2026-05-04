package edu.connection3a36.controllers;

import edu.connection3a36.entities.Parcours;
import edu.connection3a36.services.ParcoursService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class AjouterParcoursController implements Initializable {

    @FXML private ComboBox<String> cbTypeParcours;
    @FXML private TextField txtTitre;
    @FXML private DatePicker dpDateDebut;
    @FXML private DatePicker dpDateFin;
    @FXML private TextArea taDescription;
    @FXML private TextField txtEtablissement;
    @FXML private TextField txtDiplome;
    @FXML private TextField txtSpecialite;
    @FXML private TextField txtEntreprise;
    @FXML private TextField txtPoste;
    @FXML private TextField txtTypeContrat;
    @FXML private Label lblErreur;
    @FXML private Label errTitre, errType, errDateDebut;

    private final ParcoursService parcoursService = new ParcoursService();
    private int idToUpdate = -1;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cbTypeParcours.setItems(FXCollections.observableArrayList(
                "Formation", "Stage", "Alternance", "Emploi", "Personnel"));
        lblErreur.setText("");
    }

    public void setParcours(Parcours p) {
        if (p == null) return;
        this.idToUpdate = p.getId();
        cbTypeParcours.setValue(p.getTypeParcours());
        txtTitre.setText(p.getTitre());
        dpDateDebut.setValue(p.getDateDebut());
        dpDateFin.setValue(p.getDateFin());
        taDescription.setText(p.getDescription());
        txtEtablissement.setText(p.getEtablissement());
        txtDiplome.setText(p.getDiplome());
        txtSpecialite.setText(p.getSpecialite());
        txtEntreprise.setText(p.getEntreprise());
        txtPoste.setText(p.getPoste());
        txtTypeContrat.setText(p.getTypeContrat());
    }

    @FXML
    private void enregistrer() {
        boolean isValid = true;
        hideAllErrors();

        if (txtTitre.getText().trim().isEmpty()) { showErr(errTitre, "• Obligatoire"); isValid = false; }
        if (cbTypeParcours.getValue() == null) { showErr(errType, "• Obligatoire"); isValid = false; }
        if (dpDateDebut.getValue() == null) { showErr(errDateDebut, "• Obligatoire"); isValid = false; }

        if (!isValid) return;

        Parcours p = new Parcours();
        p.setTypeParcours(cbTypeParcours.getValue());
        p.setTitre(txtTitre.getText().trim());
        p.setDateDebut(dpDateDebut.getValue());
        p.setDateFin(dpDateFin.getValue());
        p.setDescription(taDescription.getText().trim());
        p.setEtablissement(txtEtablissement.getText().trim());
        p.setDiplome(txtDiplome.getText().trim());
        p.setSpecialite(txtSpecialite.getText().trim());
        p.setEntreprise(txtEntreprise.getText().trim());
        p.setPoste(txtPoste.getText().trim());
        p.setTypeContrat(txtTypeContrat.getText().trim());

        try {
            if (idToUpdate == -1) {
                if (parcoursService.existsByTitre(p.getTitre())) {
                    showErr(errTitre, "• Ce titre existe déjà.");
                    return;
                }
                parcoursService.addEntity(p);
            } else {
                p.setId(idToUpdate);
                parcoursService.updateEntity(idToUpdate, p);
            }
            fermer();
        } catch (SQLException e) {
            lblErreur.setText("❌ " + e.getMessage());
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

    private void hideAllErrors() {
        if (errTitre != null) errTitre.setVisible(false);
        if (errType != null) errType.setVisible(false);
        if (errDateDebut != null) errDateDebut.setVisible(false);
        lblErreur.setText("");
    }

    private void showErr(Label lbl, String msg) {
        if (lbl != null) {
            lbl.setText(msg);
            lbl.setVisible(true);
            lbl.setManaged(true);
        }
    }
}
