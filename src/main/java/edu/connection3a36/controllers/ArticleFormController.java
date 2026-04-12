package edu.connection3a36.controllers;

import edu.connection3a36.entities.CategorieArticle;
import edu.connection3a36.entities.ReferenceArticle;
import edu.connection3a36.services.CategorieArticleService;
import edu.connection3a36.services.ReferenceArticleService;
import edu.connection3a36.tools.AlertUtil;
import edu.connection3a36.tools.ValidationUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;

/**
 * Contrôleur formulaire Article — ajout et modification avec validation.
 */
public class ArticleFormController {

    @FXML private Label formTitle;
    @FXML private TextField tfTitre;
    @FXML private TextArea taContenu;
    @FXML private ComboBox<CategorieArticle> cbCategorie;
    @FXML private CheckBox chkPublished;
    @FXML private Label errTitre;
    @FXML private Label errContenu;
    @FXML private Label errCategorie;

    private final ReferenceArticleService articleService = new ReferenceArticleService();
    private final CategorieArticleService categorieService = new CategorieArticleService();
    private ReferenceArticle articleToEdit = null;

    @FXML
    public void initialize() {
        // Charger les catégories dans le ComboBox
        try {
            List<CategorieArticle> cats = categorieService.getData();
            cbCategorie.setItems(FXCollections.observableArrayList(cats));
        } catch (SQLException e) {
            System.err.println("Erreur chargement catégories: " + e.getMessage());
        }
    }

    public void setArticleToEdit(ReferenceArticle article) {
        this.articleToEdit = article;
        formTitle.setText("Modifier Article #" + article.getId());
        tfTitre.setText(article.getTitre());
        taContenu.setText(article.getContenu());
        chkPublished.setSelected(article.isPublished());

        // Sélectionner la catégorie
        for (CategorieArticle cat : cbCategorie.getItems()) {
            if (cat.getId() == article.getCategorieId()) {
                cbCategorie.setValue(cat);
                break;
            }
        }
    }

    @FXML
    void handleSave() {
        clearErrors();
        boolean hasError = false;

        // Validation titre
        String titreErr = ValidationUtil.validateTextField(tfTitre.getText(), "Le titre", true, 5, 255);
        if (titreErr != null) { showError(errTitre, titreErr); hasError = true; }

        // Validation contenu
        String contenuErr = ValidationUtil.validateTextField(taContenu.getText(), "Le contenu", true, 20, Integer.MAX_VALUE);
        if (contenuErr != null) { showError(errContenu, contenuErr); hasError = true; }

        // Validation catégorie
        if (cbCategorie.getValue() == null) {
            showError(errCategorie, "La catégorie est obligatoire");
            hasError = true;
        }

        if (hasError) return;

        try {
            ReferenceArticle article = articleToEdit != null ? articleToEdit : new ReferenceArticle();
            article.setTitre(tfTitre.getText().trim());
            article.setContenu(taContenu.getText().trim());
            article.setCategorieId(cbCategorie.getValue().getId());
            article.setPublished(chkPublished.isSelected());

            if (articleToEdit != null) {
                articleService.updateEntity(articleToEdit.getId(), article);
                AlertUtil.showSuccess("Article modifié avec succès !");
            } else {
                article.setAuteurId(1); // TODO: utilisateur connecté
                articleService.addEntity(article);
                AlertUtil.showSuccess("Article créé avec succès !");
            }
            closeWindow();
        } catch (SQLException e) {
            AlertUtil.showError("Erreur : " + e.getMessage());
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
        for (Label lbl : new Label[]{errTitre, errContenu, errCategorie}) {
            lbl.setText(""); lbl.setVisible(false); lbl.setManaged(false);
        }
    }

    private void closeWindow() {
        Stage stage = (Stage) tfTitre.getScene().getWindow();
        stage.close();
    }
}
