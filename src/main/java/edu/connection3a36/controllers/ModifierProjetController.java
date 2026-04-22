package edu.connection3a36.controllers;

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

public class ModifierProjetController implements Initializable {

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
    private Projet projetAModifier;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cbType.setItems(FXCollections.observableArrayList(
                "Personnel", "Académique", "Professionnel", "Open Source", "Compétition"));
        lblErreur.setText("");
    }

    public void initData(Projet projet, Parcours parcours) {
        this.projetAModifier = projet;
        lblParcoursNom.setText("Parcours : " + parcours.getTitre());
        txtTitre.setText(projet.getTitre());
        cbType.setValue(projet.getType());
        taDescription.setText(projet.getDescription() != null ? projet.getDescription() : "");
        txtTechnologies.setText(projet.getTechnologies());
        dpDateDebut.setValue(projet.getDateDebut());
        dpDateFin.setValue(projet.getDateFin());
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

        projetAModifier.setTitre(txtTitre.getText().trim());
        projetAModifier.setType(cbType.getValue());
        projetAModifier
                .setDescription(taDescription.getText().trim().isEmpty() ? null : taDescription.getText().trim());
        projetAModifier.setTechnologies(txtTechnologies.getText().trim());
        projetAModifier.setDateDebut(dpDateDebut.getValue());
        projetAModifier.setDateFin(dpDateFin.getValue());
        projetAModifier.setDateModification(LocalDate.now());

        try {
            projetService.updateEntity(projetAModifier.getId(), projetAModifier);
            fermer();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Succès");
            alert.setContentText("Projet modifié avec succès !");
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
