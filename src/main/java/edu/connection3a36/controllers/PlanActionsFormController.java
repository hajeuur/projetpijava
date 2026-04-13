package edu.connection3a36.controllers;

import edu.connection3a36.entities.PlanActions;
import edu.connection3a36.enums.CategorieSortie;
import edu.connection3a36.enums.Statut;
import edu.connection3a36.services.PlanActionsService;
import edu.connection3a36.tools.AlertUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;

/**
 * Contrôleur formulaire Plan d'Action — ajout et modification.
 * Validations en temps réel avec messages d'erreur clairs.
 */
public class PlanActionsFormController {

    @FXML private Label formTitle;
    @FXML private TextField tfDecision;
    @FXML private TextArea taDescription;
    @FXML private ComboBox<Statut> cbStatut;
    @FXML private ComboBox<CategorieSortie> cbCategorie;
    @FXML private Label errDecision;
    @FXML private Label errDescription;
    @FXML private Label errStatut;
    @FXML private Label errCategorie;

    private final PlanActionsService service = new PlanActionsService();
    private PlanActions planToEdit = null;

    @FXML
    public void initialize() {
        // Remplir les ComboBox avec les valeurs des enums
        cbStatut.setItems(FXCollections.observableArrayList(Statut.values()));
        cbCategorie.setItems(FXCollections.observableArrayList(CategorieSortie.values()));
    }

    /**
     * Pré-remplit le formulaire pour la modification.
     */
    public void setPlanToEdit(PlanActions plan) {
        this.planToEdit = plan;
        formTitle.setText("Modifier Plan d'Action #" + plan.getId());
        tfDecision.setText(plan.getDecision());
        taDescription.setText(plan.getDescription());
        cbStatut.setValue(plan.getStatut());
        cbCategorie.setValue(plan.getCategorie());
    }

    @FXML
    void handleSave() {
        // Réinitialiser les erreurs
        clearErrors();

        // Construire l'objet
        PlanActions plan = planToEdit != null ? planToEdit : new PlanActions();
        plan.setDecision(tfDecision.getText());
        plan.setDescription(taDescription.getText());
        plan.setStatut(cbStatut.getValue());
        plan.setCategorie(cbCategorie.getValue());

        // Validation
        List<String> errors = service.validate(plan);
        if (!errors.isEmpty()) {
            displayErrors(errors);
            return;
        }

        // Check Uniqueness
        try {
            boolean exists = (planToEdit != null)
                ? service.existsByDecisionExcluding(plan.getDecision(), planToEdit.getId())
                : service.existsByDecision(plan.getDecision());
            if (exists) {
                showError(errDecision, "Un Plan d'Action avec cette décision existe déjà.");
                return;
            }
        } catch(SQLException ex) {
            showError(errDecision, "Erreur BdD lors de la vérification de l'unicité.");
            return;
        }

        try {
            if (planToEdit != null) {
                // Modification
                service.updateEntity(planToEdit.getId(), plan);
                AlertUtil.showSuccess("Le plan d'action a été modifié avec succès !");
            } else {
                // Création
                service.addEntity(plan);
                AlertUtil.showSuccess("Le plan d'action a été créé avec succès !");
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

    // ======================== VALIDATION UI ========================

    private void displayErrors(List<String> errors) {
        for (String error : errors) {
            String lower = error.toLowerCase();
            if (lower.contains("décision")) {
                showError(errDecision, error);
            } else if (lower.contains("description")) {
                showError(errDescription, error);
            } else if (lower.contains("statut")) {
                showError(errStatut, error);
            } else if (lower.contains("catégorie")) {
                showError(errCategorie, error);
            }
        }
    }

    private void showError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void clearErrors() {
        Label[] labels = {errDecision, errDescription, errStatut, errCategorie};
        for (Label lbl : labels) {
            lbl.setText("");
            lbl.setVisible(false);
            lbl.setManaged(false);
        }
    }

    private void closeWindow() {
        Stage stage = (Stage) tfDecision.getScene().getWindow();
        stage.close();
    }
}
