package edu.connection3a36.controllers;

import edu.connection3a36.entities.Projet;
import edu.connection3a36.entities.Ressource;
import edu.connection3a36.services.RessourceService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class ModifierRessourceController implements Initializable {

    @FXML
    private Label lblProjetNom;
    @FXML
    private TextField txtNom;
    @FXML
    private ComboBox<String> cbTypeRessource;
    @FXML
    private TextField txtUrl;
    @FXML
    private TextArea taDescription;
    @FXML
    private Label lblErreur;
    @FXML
    private Label errNom, errType, errUrl;

    private final RessourceService ressourceService = new RessourceService();
    private Ressource ressourceAModifier;
    private Projet projetActuel;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cbTypeRessource.setItems(FXCollections.observableArrayList(
                "PDF", "VIDEO", "LIEN", "ARTICLE", "AUTRE"));
        lblErreur.setText("");
    }

    public void initData(Ressource ressource, Projet projet) {
        this.ressourceAModifier = ressource;
        this.projetActuel = projet;
        lblProjetNom.setText("Projet : " + projet.getTitre());
        txtNom.setText(ressource.getNom());
        cbTypeRessource.setValue(ressource.getTypeRessource());
        txtUrl.setText(ressource.getUrlRessource() != null ? ressource.getUrlRessource() : "");
        taDescription.setText(ressource.getDescription() != null ? ressource.getDescription() : "");
    }

    @FXML
    private void enregistrer() {
        lblErreur.setText("");

        hideAllErrors();
        boolean isValid = true;

        if (txtNom.getText().trim().isEmpty()) {
            showErr(errNom, "• Le nom est obligatoire.");
            isValid = false;
        }
        if (cbTypeRessource.getValue() == null) {
            showErr(errType, "• Le type est obligatoire.");
            isValid = false;
        }
        String url2 = txtUrl.getText().trim();
        if (!url2.isEmpty() && !url2.startsWith("http://") && !url2.startsWith("https://")
                && !url2.startsWith("ftp://")) {
            showErr(errUrl, "• L'URL doit être valide (http...).");
            isValid = false;
        }

        if (!isValid)
            return;

        ressourceAModifier.setNom(txtNom.getText().trim());
        ressourceAModifier.setTypeRessource(cbTypeRessource.getValue());
        ressourceAModifier.setUrlRessource(url2.isEmpty() ? null : url2);
        ressourceAModifier
                .setDescription(taDescription.getText().trim().isEmpty() ? null : taDescription.getText().trim());
        ressourceAModifier.setDateModification(LocalDate.now());

        try {
            ressourceService.updateEntity(ressourceAModifier.getId(), ressourceAModifier);
            fermer();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Succès");
            alert.setContentText("Ressource modifiée avec succès !");
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
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/AfficherRessources.fxml"));
            javafx.scene.Parent view = loader.load();
            AfficherRessourcesController controller = loader.getController();
            controller.initData(projetActuel);
            MainController.getInstance().loadInContentArea(view);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private void hideAllErrors() {
        if (errNom != null) {
            errNom.setVisible(false);
            errNom.setManaged(false);
        }
        if (errType != null) {
            errType.setVisible(false);
            errType.setManaged(false);
        }
        if (errUrl != null) {
            errUrl.setVisible(false);
            errUrl.setManaged(false);
        }
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
