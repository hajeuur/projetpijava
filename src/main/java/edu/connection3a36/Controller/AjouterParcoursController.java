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

    @FXML
    private void enregistrer() {
        boolean isValid = true;
        hideAllErrors();

        if (txtTitre.getText().trim().isEmpty()) {
            showErr(errTitre, "• Le titre est obligatoire.");
            isValid = false;
        } else if (txtTitre.getText().trim().length() < 3) {
            showErr(errTitre, "• Minimum 3 caractères.");
            isValid = false;
        }

        if (cbTypeParcours.getValue() == null) {
            showErr(errType, "• Le type est obligatoire.");
            isValid = false;
        }
        if (txtDiplome.getText().trim().isEmpty()) {
            showErr(errDiplome, "• Le diplôme est obligatoire.");
            isValid = false;
        }
        if (txtEtablissement.getText().trim().isEmpty()) {
            showErr(errEtablissement, "• L'établissement est obligatoire.");
            isValid = false;
        }
        if (dpDateDebut.getValue() == null) {
            showErr(errDateDebut, "• La date de début est obligatoire.");
            isValid = false;
        }
        if (taDescription.getText().trim().isEmpty()) {
            showErr(errDescription, "• La description est obligatoire.");
            isValid = false;
        }

        if (dpDateDebut.getValue() != null && dpDateFin.getValue() != null
                && dpDateFin.getValue().isBefore(dpDateDebut.getValue())) {
            showErr(errDateFin, "• La date de fin doit être après le début.");
            isValid = false;
        }

        if (!isValid)
            return;

        Parcours p = new Parcours();
        p.setTypeParcours(cbTypeParcours.getValue());
        p.setTitre(txtTitre.getText().trim());
        // ... (rest of fields set in original code)
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
            if (parcoursService.existsByTitreAndType(p.getTitre(), p.getTypeParcours())) {
                showErr(errTitre, "• Ce parcours existe déjà.");
                return;
            }
            parcoursService.addEntity(p);
            fermer();
            new Alert(Alert.AlertType.INFORMATION, "Parcours ajouté !").show();
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
