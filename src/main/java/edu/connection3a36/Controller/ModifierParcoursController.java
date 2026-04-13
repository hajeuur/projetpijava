package edu.connection3a36.Controller;

import edu.connection3a36.entities.Parcours;
import edu.connection3a36.services.ParcoursService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class ModifierParcoursController implements Initializable {

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

    private final ParcoursService parcoursService = new ParcoursService();
    private Parcours parcoursAModifier;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cbTypeParcours.setItems(FXCollections.observableArrayList(
                "Formation", "Stage", "Alternance", "Emploi", "Personnel"));
        lblErreur.setText("");
    }

    public void initData(Parcours parcours) {
        this.parcoursAModifier = parcours;
        cbTypeParcours.setValue(parcours.getTypeParcours());
        txtTitre.setText(parcours.getTitre());
        dpDateDebut.setValue(parcours.getDateDebut());
        dpDateFin.setValue(parcours.getDateFin());
        taDescription.setText(parcours.getDescription());
        txtEtablissement.setText(parcours.getEtablissement());
        txtDiplome.setText(parcours.getDiplome());
        txtSpecialite.setText(parcours.getSpecialite() != null ? parcours.getSpecialite() : "");
        txtEntreprise.setText(parcours.getEntreprise() != null ? parcours.getEntreprise() : "");
        txtPoste.setText(parcours.getPoste() != null ? parcours.getPoste() : "");
        txtTypeContrat.setText(parcours.getTypeContrat() != null ? parcours.getTypeContrat() : "");
    }

    @FXML
    private void enregistrer() {
        lblErreur.setText("");

        if (cbTypeParcours.getValue() == null || cbTypeParcours.getValue().isEmpty()) {
            lblErreur.setText("❌ Le type de parcours est obligatoire.");
            return;
        }
        if (txtTitre.getText().trim().isEmpty()) {
            lblErreur.setText("❌ Le titre est obligatoire.");
            return;
        }
        if (txtTitre.getText().trim().length() < 3) {
            lblErreur.setText("❌ Le titre doit contenir au moins 3 caractères.");
            return;
        }
        if (dpDateDebut.getValue() == null) {
            lblErreur.setText("❌ La date de début est obligatoire.");
            return;
        }
        if (taDescription.getText().trim().isEmpty()) {
            lblErreur.setText("❌ La description est obligatoire.");
            return;
        }
        if (txtEtablissement.getText().trim().isEmpty()) {
            lblErreur.setText("❌ L'établissement est obligatoire.");
            return;
        }
        if (txtDiplome.getText().trim().isEmpty()) {
            lblErreur.setText("❌ Le diplôme est obligatoire.");
            return;
        }
        if (dpDateFin.getValue() != null && dpDateFin.getValue().isBefore(dpDateDebut.getValue())) {
            lblErreur.setText("❌ La date de fin ne peut pas être antérieure à la date de début.");
            return;
        }

        parcoursAModifier.setTypeParcours(cbTypeParcours.getValue());
        parcoursAModifier.setTitre(txtTitre.getText().trim());
        parcoursAModifier.setDateDebut(dpDateDebut.getValue());
        parcoursAModifier.setDateFin(dpDateFin.getValue());
        parcoursAModifier.setDescription(taDescription.getText().trim());
        parcoursAModifier.setEtablissement(txtEtablissement.getText().trim());
        parcoursAModifier.setDiplome(txtDiplome.getText().trim());
        parcoursAModifier
                .setSpecialite(txtSpecialite.getText().trim().isEmpty() ? null : txtSpecialite.getText().trim());
        parcoursAModifier
                .setEntreprise(txtEntreprise.getText().trim().isEmpty() ? null : txtEntreprise.getText().trim());
        parcoursAModifier.setPoste(txtPoste.getText().trim().isEmpty() ? null : txtPoste.getText().trim());
        parcoursAModifier
                .setTypeContrat(txtTypeContrat.getText().trim().isEmpty() ? null : txtTypeContrat.getText().trim());
        parcoursAModifier.setDateModification(LocalDate.now());

        try {
            parcoursService.updateEntity(parcoursAModifier.getId(), parcoursAModifier);
            fermer();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Succès");
            alert.setContentText("Parcours modifié avec succès !");
            alert.show();
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
}
