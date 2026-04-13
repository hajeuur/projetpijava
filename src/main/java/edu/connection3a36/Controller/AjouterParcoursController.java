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

    private final ParcoursService parcoursService = new ParcoursService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cbTypeParcours.setItems(FXCollections.observableArrayList(
                "Formation", "Stage", "Alternance", "Emploi", "Personnel"));
        lblErreur.setText("");
    }

    @FXML
    private void enregistrer() {
        lblErreur.setText("");

        // Validation des champs obligatoires
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
        // Validation date fin >= date debut
        if (dpDateFin.getValue() != null && dpDateFin.getValue().isBefore(dpDateDebut.getValue())) {
            lblErreur.setText("❌ La date de fin ne peut pas être antérieure à la date de début.");
            return;
        }

        Parcours p = new Parcours();
        p.setTypeParcours(cbTypeParcours.getValue());
        p.setTitre(txtTitre.getText().trim());
        p.setDateDebut(dpDateDebut.getValue());
        p.setDateFin(dpDateFin.getValue());
        p.setDescription(taDescription.getText().trim());
        p.setEtablissement(txtEtablissement.getText().trim());
        p.setDiplome(txtDiplome.getText().trim());
        p.setSpecialite(txtSpecialite.getText().trim().isEmpty() ? null : txtSpecialite.getText().trim());
        p.setEntreprise(txtEntreprise.getText().trim().isEmpty() ? null : txtEntreprise.getText().trim());
        p.setPoste(txtPoste.getText().trim().isEmpty() ? null : txtPoste.getText().trim());
        p.setTypeContrat(txtTypeContrat.getText().trim().isEmpty() ? null : txtTypeContrat.getText().trim());
        p.setDateCreation(LocalDate.now());

        try {
            parcoursService.addEntity(p);
            fermer();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Succès");
            alert.setContentText("Parcours ajouté avec succès !");
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
