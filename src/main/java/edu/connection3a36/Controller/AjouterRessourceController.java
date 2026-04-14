package edu.connection3a36.Controller;

import edu.connection3a36.entities.Projet;
import edu.connection3a36.entities.Ressource;
import edu.connection3a36.services.RessourceService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class AjouterRessourceController implements Initializable {

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
    private Projet projetActuel;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cbTypeRessource.setItems(FXCollections.observableArrayList(
                "PDF", "VIDEO", "LIEN", "ARTICLE", "AUTRE"));
        cbTypeRessource.setValue("LIEN");
        lblErreur.setText("");
    }

    public void initData(Projet projet) {
        this.projetActuel = projet;
        lblProjetNom.setText("Projet : " + projet.getTitre());
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
        // Validation URL si fournie
        String url2 = txtUrl.getText().trim();
        if (!url2.isEmpty() && !url2.startsWith("http://") && !url2.startsWith("https://")
                && !url2.startsWith("ftp://")) {
            showErr(errUrl, "• L'URL doit être valide (http...).");
            isValid = false;
        }

        if (!isValid)
            return;

        Ressource r = new Ressource();
        r.setNom(txtNom.getText().trim());
        r.setTypeRessource(cbTypeRessource.getValue());
        r.setUrlRessource(url2.isEmpty() ? null : url2);
        r.setDescription(taDescription.getText().trim().isEmpty() ? null : taDescription.getText().trim());
        r.setDateCreation(LocalDate.now());
        r.setProjetId(projetActuel.getId());

        try {
            ressourceService.addEntity(r);
            fermer();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Succès");
            alert.setContentText("Ressource ajoutée avec succès !");
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
            ((BorderPane) txtNom.getScene().getRoot()).setCenter(view);
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
