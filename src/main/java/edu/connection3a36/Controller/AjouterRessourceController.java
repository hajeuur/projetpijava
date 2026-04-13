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

        if (txtNom.getText().trim().isEmpty()) {
            lblErreur.setText("❌ Le nom de la ressource est obligatoire.");
            return;
        }
        if (cbTypeRessource.getValue() == null) {
            lblErreur.setText("❌ Le type de ressource est obligatoire.");
            return;
        }
        // Validation URL si fournie
        String url2 = txtUrl.getText().trim();
        if (!url2.isEmpty() && !url2.startsWith("http://") && !url2.startsWith("https://")
                && !url2.startsWith("ftp://")) {
            lblErreur.setText("❌ L'URL doit commencer par http://, https:// ou ftp://");
            return;
        }

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
        Stage stage = (Stage) txtNom.getScene().getWindow();
        stage.close();
    }
}
