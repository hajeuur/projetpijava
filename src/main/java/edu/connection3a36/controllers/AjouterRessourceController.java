package edu.connection3a36.controllers;

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
import java.util.ResourceBundle;

public class AjouterRessourceController implements Initializable {

    @FXML private TextField txtNom;
    @FXML private TextField txtUrl;
    @FXML private TextArea taDescription;
    @FXML private ComboBox<String> cbType;
    @FXML private Label lblErreur;

    private final RessourceService ressourceService = new RessourceService();
    private Projet projetActuel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cbType.setItems(FXCollections.observableArrayList("DOCUMENT", "VIDEO", "LIEN", "CODE"));
    }

    public void initData(Projet p) {
        this.projetActuel = p;
    }

    @FXML
    private void enregistrer() {
        if (txtNom.getText().trim().isEmpty()) { lblErreur.setText("Nom obligatoire"); return; }
        
        Ressource r = new Ressource();
        r.setNom(txtNom.getText().trim());
        r.setUrlRessource(txtUrl.getText().trim());
        r.setDescription(taDescription.getText().trim());
        r.setTypeRessource(cbType.getValue());
        r.setProjetId(projetActuel.getId());

        try {
            ressourceService.addEntity(r);
            fermer();
        } catch (SQLException e) { lblErreur.setText(e.getMessage()); }
    }

    @FXML private void annuler() { fermer(); }

    private void fermer() {
        Stage stage = (Stage) txtNom.getScene().getWindow();
        stage.close();
    }
}
