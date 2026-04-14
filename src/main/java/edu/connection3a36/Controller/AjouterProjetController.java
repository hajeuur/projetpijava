package edu.connection3a36.Controller;

import edu.connection3a36.entities.Parcours;
import edu.connection3a36.entities.Projet;
import edu.connection3a36.services.ProjetService;
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
    @FXML
    private Label errTitre, errType, errTechnologies, errDateDebut, errDateFin;

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
        boolean isValid = true;
        hideAllErrors();

        if (txtTitre.getText().trim().isEmpty()) {
            showErr(errTitre, "• Le titre est obligatoire.");
            isValid = false;
        } else if (txtTitre.getText().trim().length() < 3) {
            showErr(errTitre, "• Minimum 3 caractères.");
            isValid = false;
        }

        if (cbType.getValue() == null) {
            showErr(errType, "• Le type est obligatoire.");
            isValid = false;
        }
        if (txtTechnologies.getText().trim().isEmpty()) {
            showErr(errTechnologies, "• Techs obligatoires.");
            isValid = false;
        }
        if (dpDateDebut.getValue() == null) {
            showErr(errDateDebut, "• Date début obligatoire.");
            isValid = false;
        }

        if (dpDateDebut.getValue() != null && dpDateFin.getValue() != null
                && dpDateFin.getValue().isBefore(dpDateDebut.getValue())) {
            showErr(errDateFin, "• La date de fin doit être après le début.");
            isValid = false;
        }

        if (!isValid)
            return;

        Projet projet = new Projet();
        projet.setTitre(txtTitre.getText().trim());
        projet.setType(cbType.getValue());
        projet.setDescription(taDescription.getText());
        projet.setTechnologies(txtTechnologies.getText().trim());
        projet.setDateDebut(dpDateDebut.getValue());
        projet.setDateFin(dpDateFin.getValue());
        projet.setParcoursId(parcoursActuel.getId());

        try {
            if (projetService.existsByTitreAndParcours(projet.getTitre(), projet.getParcoursId())) {
                showErr(errTitre, "• Ce titre existe déjà dans ce parcours.");
                return;
            }
            projetService.addEntity(projet);
            fermer();
            new Alert(Alert.AlertType.INFORMATION, "Projet ajouté !").show();
        } catch (SQLException e) {
            lblErreur.setText("❌ " + e.getMessage());
        }
    }

    private void hideAllErrors() {
        errTitre.setVisible(false);
        errTitre.setManaged(false);
        errType.setVisible(false);
        errType.setManaged(false);
        errTechnologies.setVisible(false);
        errTechnologies.setManaged(false);
        errDateDebut.setVisible(false);
        errDateDebut.setManaged(false);
        errDateFin.setVisible(false);
        errDateFin.setManaged(false);
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AfficherProjets.fxml"));
            Parent view = loader.load();
            AfficherProjetsController controller = loader.getController();
            controller.initData(parcoursActuel);
            ((BorderPane) txtTitre.getScene().getRoot()).setCenter(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
