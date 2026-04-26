package edu.connection3a36.Controller;

import edu.connection3a36.entities.Parcours;
import edu.connection3a36.services.ParcoursService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class AjouterParcoursController implements Initializable {

    @FXML
    private ComboBox<String> cbTypeParcours;
    @FXML
    private TextField txtTitre;
    @FXML
    private DatePicker dpDateDebut;
    @FXML
    private DatePicker dpDateFin;
    @FXML
    private TextArea taDescription;
    @FXML
    private TextField txtEtablissement;
    @FXML
    private TextField txtDiplome;
    @FXML
    private TextField txtSpecialite;
    @FXML
    private TextField txtEntreprise;
    @FXML
    private TextField txtPoste;
    @FXML
    private TextField txtTypeContrat;
    @FXML
    private Label lblErreur;
    @FXML
    private Label errTitre, errType, errDiplome, errEtablissement, errDateDebut, errDateFin, errDescription;

    private final ParcoursService parcoursService = new ParcoursService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cbTypeParcours.setItems(FXCollections.observableArrayList(
                "Formation", "Stage", "Alternance", "Emploi", "Personnel"));
        lblErreur.setText("");
    }

    private int idToUpdate = -1;

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

        if (txtTitre.getText().trim().isEmpty()) {
            showErr(errTitre, "• Le titre est obligatoire.");
            isValid = false;
        }

        if (cbTypeParcours.getValue() == null) {
            showErr(errType, "• Le type est obligatoire.");
            isValid = false;
        }
        
        if (dpDateDebut.getValue() == null) {
            showErr(errDateDebut, "• La date de début est obligatoire.");
            isValid = false;
        }

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
                // Ajout
                if (parcoursService.existsByTitre(p.getTitre())) {
                    showErr(errTitre, "• Ce titre existe déjà.");
                    return;
                }
                parcoursService.addEntity(p);
                new Alert(Alert.AlertType.INFORMATION, "Parcours ajouté !").show();
            } else {
                // Modification
                p.setId(idToUpdate);
                parcoursService.updateEntity(idToUpdate, p);
                new Alert(Alert.AlertType.INFORMATION, "Parcours mis à jour !").show();
            }
            fermer();
        } catch (SQLException e) {
            lblErreur.setText("❌ " + e.getMessage());
        }
    }

    private void hideAllErrors() {
        errTitre.setVisible(false);
        errTitre.setManaged(false);
        errType.setVisible(false);
        errType.setManaged(false);
        errDiplome.setVisible(false);
        errDiplome.setManaged(false);
        errEtablissement.setVisible(false);
        errEtablissement.setManaged(false);
        errDateDebut.setVisible(false);
        errDateDebut.setManaged(false);
        errDateFin.setVisible(false);
        errDateFin.setManaged(false);
        errDescription.setVisible(false);
        errDescription.setManaged(false);
        lblErreur.setText("");
    }

    private void showErr(Label lbl, String msg) {
        lbl.setText(msg);
        lbl.setVisible(true);
        lbl.setManaged(true);
    }

    @FXML
    private void annuler() {
        fermer();
    }

    private void fermer() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AfficherParcours.fxml"));
            Parent view = loader.load();
            ((BorderPane) txtTitre.getScene().getRoot()).setCenter(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
