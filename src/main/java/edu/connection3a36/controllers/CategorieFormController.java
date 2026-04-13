package edu.connection3a36.controllers;

import edu.connection3a36.entities.CategorieArticle;
import edu.connection3a36.services.CategorieArticleService;
import edu.connection3a36.tools.AlertUtil;
import edu.connection3a36.tools.ValidationUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;

/**
 * Contrôleur formulaire Catégorie — ajout et modification avec contrôle d'unicité.
 */
public class CategorieFormController {

    @FXML private Label formTitle;
    @FXML private TextField tfNom;
    @FXML private TextArea taDescription;
    @FXML private Label errNom;
    @FXML private Label errDescription;

    private final CategorieArticleService service = new CategorieArticleService();
    private CategorieArticle categorieToEdit = null;

    public void setCategorieToEdit(CategorieArticle cat) {
        this.categorieToEdit = cat;
        formTitle.setText("Modifier Catégorie #" + cat.getId());
        tfNom.setText(cat.getNomCategorie());
        taDescription.setText(cat.getDescription());
    }

    @FXML
    void handleSave() {
        clearErrors();
        boolean hasError = false;

        String nom = tfNom.getText();
        String desc = taDescription.getText();

        // Validation nom
        String nomErr = ValidationUtil.validateTextField(nom, "Le nom de catégorie", true, 3, 100);
        if (nomErr != null) {
            showError(errNom, nomErr);
            hasError = true;
        }

        // Validation description (optionnelle, max 1000)
        if (desc != null && !desc.isEmpty() && desc.length() > 1000) {
            showError(errDescription, "La description ne peut pas dépasser 1000 caractères");
            hasError = true;
        }

        if (hasError) return;

        // Check Uniqueness
        try {
            boolean exists = (categorieToEdit != null)
                ? service.existsByNomExcluding(nom.trim(), categorieToEdit.getId())
                : service.existsByNom(nom.trim());
            if (exists) {
                showError(errNom, "Une catégorie avec ce nom existe déjà.");
                return;
            }
        } catch(SQLException ex) {
            showError(errNom, "Erreur BdD lors de la vérification de l'unicité.");
            return;
        }

        try {
            CategorieArticle cat = categorieToEdit != null ? categorieToEdit : new CategorieArticle();
            cat.setNomCategorie(nom.trim());
            cat.setDescription(desc != null ? desc.trim() : null);

            if (categorieToEdit != null) {
                service.updateEntity(categorieToEdit.getId(), cat);
                AlertUtil.showSuccess("Catégorie modifiée avec succès !");
            } else {
                cat.setAuteurId(1); // TODO: récupérer l'utilisateur connecté
                service.addEntity(cat);
                AlertUtil.showSuccess("Catégorie créée avec succès !");
            }
            closeWindow();
        } catch (SQLException e) {
            // Erreur d'unicité remonte ici
            showError(errNom, e.getMessage());
        }
    }

    @FXML
    void handleCancel() {
        closeWindow();
    }

    private void showError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void clearErrors() {
        for (Label lbl : new Label[]{errNom, errDescription}) {
            lbl.setText("");
            lbl.setVisible(false);
            lbl.setManaged(false);
        }
    }

    private void closeWindow() {
        Stage stage = (Stage) tfNom.getScene().getWindow();
        stage.close();
    }
}
