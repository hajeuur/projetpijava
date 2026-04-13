package edu.connection3a36.Controller;

import edu.connection3a36.entities.Parcours;
import edu.connection3a36.entities.Projet;
import edu.connection3a36.services.ProjetService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class AjouterProjetController implements Initializable {

    @FXML
    private Label lblParcoursNom;
    @FXML
    private TextField txtTitre;
    @FXML
    private ComboBox<String> cbType;
    @FXML
    private TextArea taDescription;
    @FXML
    private TextField txtTechnologies;
    @FXML
    private DatePicker dpDateDebut;
    @FXML
    private DatePicker dpDateFin;
    @FXML
    private Label lblErreur;

    private final ProjetService projetService = new ProjetService();
    private Parcours parcoursActuel;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cbType.setItems(FXCollections.observableArrayList(
                "Personnel", "Académique", "Professionnel", "Open Source", "Compétition"));
        lblErreur.setText("");
    }

    public void initData(Parcours parcours) {
        this.parcoursActuel = parcours;
        lblParcoursNom.setText("Parcours : " + parcours.getTitre());
    }

    @FXML
    private void enregistrer() {
        lblErreur.setText("");

        if (txtTitre.getText().trim().isEmpty()) {
            lblErreur.setText("❌ Le titre du projet est obligatoire.");
            return;
        }
        if (txtTitre.getText().trim().length() < 3) {
            lblErreur.setText("❌ Le titre doit contenir au moins 3 caractères.");
            return;
        }
        if (cbType.getValue() == null || cbType.getValue().isEmpty()) {
            lblErreur.setText("❌ Le type de projet est requis.");
            return;
        }
        if (txtTechnologies.getText().trim().isEmpty()) {
            lblErreur.setText("❌ Veuillez indiquer les technologies utilisées.");
            return;
        }
        if (dpDateDebut.getValue() == null) {
            lblErreur.setText("❌ Veuillez indiquer une date de début.");
            return;
        }
        if (dpDateFin.getValue() != null && dpDateFin.getValue().isBefore(dpDateDebut.getValue())) {
            lblErreur.setText("❌ La date de fin ne peut pas être antérieure à la date de début.");
            return;
        }

        Projet projet = new Projet();
        projet.setTitre(txtTitre.getText().trim());
        projet.setType(cbType.getValue());
        projet.setDescription(taDescription.getText().trim().isEmpty() ? null : taDescription.getText().trim());
        projet.setTechnologies(txtTechnologies.getText().trim());
        projet.setDateDebut(dpDateDebut.getValue());
        projet.setDateFin(dpDateFin.getValue());
        projet.setDateCreation(LocalDate.now());
        projet.setParcoursId(parcoursActuel.getId());

        try {
            projetService.addEntity(projet);
            fermer();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Succès");
            alert.setContentText("Projet ajouté avec succès !");
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
