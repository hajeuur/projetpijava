package edu.mentorai.Controller;

import edu.mentorai.Main;
import edu.mentorai.entities.Objectif;
import edu.mentorai.entities.Programme;
import edu.mentorai.entities.Statutobj;
import edu.mentorai.interfaces.ObjectifDAO;
import edu.mentorai.interfaces.ProgrammeDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;

public class ObjectifShowController {

    @FXML private Label titreLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label datedebutLabel;
    @FXML private Label datefinLabel;
    @FXML private Label statutLabel;
    @FXML private Label programmeTitreLabel;
    @FXML private Label programmeDateLabel;

    private Objectif objectif;
    private int utilisateurId = 1;
    private final ObjectifDAO objectifDAO = new ObjectifDAO();
    private final ProgrammeDAO programmeDAO = new ProgrammeDAO();

    public void setUtilisateurId(int id) { this.utilisateurId = id; }

    public void setObjectif(Objectif obj) {
        this.objectif = obj;

        titreLabel.setText(obj.getTitre());
        descriptionLabel.setText(obj.getDescription());
        datedebutLabel.setText(obj.getDatedebut() != null ? obj.getDatedebut().toString() : "—");
        datefinLabel.setText(obj.getDatefin() != null ? obj.getDatefin().toString() : "—");

        // Statut badge
        String color = switch (obj.getStatut()) {
            case Atteint -> "#198754";
            case EnCours -> "#ffc107";
            case Abandonner -> "#d52e28";
        };
        statutLabel.setText(obj.getStatut().getValue());
        statutLabel.setStyle("-fx-background-color: " + color + "22; -fx-text-fill: " + color +
                "; -fx-font-weight: bold; -fx-background-radius: 50; -fx-padding: 5 15 5 15;");

        // Programme
        try {
            Programme programme = programmeDAO.findByObjectifId(obj.getId());
            if (programme != null) {
                programmeTitreLabel.setText(programme.getTitre());
                programmeDateLabel.setText(programme.getDategeneration() != null
                        ? programme.getDategeneration().toString() : "—");
            } else {
                programmeTitreLabel.setText("Aucun programme");
                programmeDateLabel.setText("—");
            }
        } catch (Exception e) {
            programmeTitreLabel.setText("Erreur");
        }
    }

    @FXML
    private void handleEdit() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/objectif_form.fxml"));
            Scene scene = new Scene(loader.load(), 700, 580);
            ObjectifFormController ctrl = loader.getController();
            ctrl.setUtilisateurId(utilisateurId);
            ctrl.setObjectif(objectif);
            ctrl.setOnSaved(this::goBackToList);
            Main.primaryStage.setScene(scene);
        } catch (Exception e) { showAlert("Erreur", e.getMessage()); }
    }

    @FXML
    private void handleDelete() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer");
        alert.setContentText("Supprimer cet objectif ? Action irréversible.");
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    objectifDAO.delete(objectif.getId());
                    goBackToList();
                } catch (Exception e) { showAlert("Erreur", e.getMessage()); }
            }
        });
    }

    @FXML
    private void handleRetour() { goBackToList(); }

    private void goBackToList() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/objectif_list.fxml"));
            Scene scene = new Scene(loader.load(), 1100, 700);
            ObjectifListController ctrl = loader.getController();
            ctrl.setUtilisateurId(utilisateurId);
            ctrl.loadData();
            Main.primaryStage.setScene(scene);
        } catch (Exception e) { showAlert("Erreur", e.getMessage()); }
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title); a.setContentText(msg); a.showAndWait();
    }
}