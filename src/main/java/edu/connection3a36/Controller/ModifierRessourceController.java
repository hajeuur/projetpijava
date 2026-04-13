package edu.connection3a36.Controller;

import edu.connection3a36.entities.Projet;
import edu.connection3a36.entities.Ressource;
import edu.connection3a36.services.RessourceService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

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

    private final RessourceService ressourceService = new RessourceService();
    private Ressource ressourceAModifier;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cbTypeRessource.setItems(FXCollections.observableArrayList(
                "PDF", "VIDEO", "LIEN", "ARTICLE", "AUTRE"));
        lblErreur.setText("");
    }

    public void initData(Ressource ressource, Projet projet) {
        this.ressourceAModifier = ressource;
        lblProjetNom.setText("Projet : " + projet.getTitre());
        txtNom.setText(ressource.getNom());
        cbTypeRessource.setValue(ressource.getTypeRessource());
        txtUrl.setText(ressource.getUrlRessource() != null ? ressource.getUrlRessource() : "");
        taDescription.setText(ressource.getDescription() != null ? ressource.getDescription() : "");
    }

    @FXML
    private void enregistrer() {
        lblErreur.setText("");

        if (txtNom.getText().trim().isEmpty()) {
            lblErreur.setText("❌ Le nom de la ressource est obligatoire.");
            return;
        }
        if (cbTypeRessource.getValue() == null) {
            lblErreur.setText("❌ Le type de ressource est obligatoire.");
            return;
        }
        String url2 = txtUrl.getText().trim();
        if (!url2.isEmpty() && !url2.startsWith("http://") && !url2.startsWith("https://")
                && !url2.startsWith("ftp://")) {
            lblErreur.setText("❌ L'URL doit commencer par http://, https:// ou ftp://");
            return;
        }

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
        Stage stage = (Stage) txtNom.getScene().getWindow();
        stage.close();
    }
}
