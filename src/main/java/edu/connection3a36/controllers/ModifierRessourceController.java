package edu.connection3a36.controllers;

import edu.connection3a36.entities.Projet;
import edu.connection3a36.entities.Ressource;
import edu.connection3a36.services.RessourceService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class ModifierRessourceController implements Initializable {

    @FXML private TextField txtNom;
    @FXML private TextField txtUrl;
    @FXML private TextArea taDescription;
    @FXML private ComboBox<String> cbTypeRessource;
    @FXML private Label lblErreur;
    @FXML private Label lblProjetNom;

    private final RessourceService ressourceService = new RessourceService();
    private Ressource ressourceAModifier;
    private Projet projetActuel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cbTypeRessource.setItems(FXCollections.observableArrayList("PDF", "VIDEO", "LIEN", "ARTICLE", "AUTRE"));
    }

    public void initData(Ressource r, Projet p) {
        this.ressourceAModifier = r;
        this.projetActuel = p;
        if (lblProjetNom != null) lblProjetNom.setText("Projet : " + p.getTitre());
        txtNom.setText(r.getNom());
        txtUrl.setText(r.getUrlRessource());
        taDescription.setText(r.getDescription());
        cbTypeRessource.setValue(r.getTypeRessource());
    }

    @FXML
    private void enregistrer() {
        if (txtNom.getText().trim().isEmpty()) { lblErreur.setText("Nom obligatoire"); return; }

        ressourceAModifier.setNom(txtNom.getText().trim());
        ressourceAModifier.setUrlRessource(txtUrl.getText().trim());
        ressourceAModifier.setDescription(taDescription.getText().trim());
        ressourceAModifier.setTypeRessource(cbTypeRessource.getValue());

        try {
            ressourceService.updateEntity(ressourceAModifier.getId(), ressourceAModifier);
            retour();
        } catch (SQLException e) { lblErreur.setText(e.getMessage()); }
    }

    @FXML
    private void annuler() { retour(); }

    private void retour() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/AfficherRessources.fxml"));
            javafx.scene.Parent view = loader.load();
            ((AfficherRessourcesController) loader.getController()).initData(projetActuel);
            MainController.getInstance().loadInContentArea(view);
        } catch (Exception e) { e.printStackTrace(); }
    }
}