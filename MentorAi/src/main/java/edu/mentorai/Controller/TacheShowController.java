package edu.mentorai.Controller;

import edu.mentorai.Main;
import edu.mentorai.entities.*;
import edu.mentorai.interfaces.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;

public class TacheShowController {

    @FXML private Label ordreLabel;
    @FXML private Label titreLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label etatLabel;

    private Tache tache;
    private Programme programme;
    private Objectif objectif;
    private int utilisateurId = 1;

    private final TacheDAO tacheDAO = new TacheDAO();
    private final ProgrammeDAO programmeDAO = new ProgrammeDAO();
    private final ObjectifDAO objectifDAO = new ObjectifDAO();

    public void setUtilisateurId(int id) { this.utilisateurId = id; }
    public void setProgramme(Programme p) { this.programme = p; }
    public void setObjectif(Objectif o) { this.objectif = o; }

    public void setTache(Tache t) {
        this.tache = t;
        ordreLabel.setText(String.valueOf(t.getOrdre()));
        titreLabel.setText(t.getTitre());
        descriptionLabel.setText(t.getDescription() != null ? t.getDescription() : "Aucune description");

        String color = switch (t.getEtat()) {
            case realisee -> "#198754";
            case encours -> "#ffc107";
            case Abandonner -> "#d52e28";
        };
        String text = switch (t.getEtat()) {
            case realisee -> "RÉALISÉE";
            case encours -> "EN COURS";
            case Abandonner -> "ABANDONNER";
        };
        etatLabel.setText(text);
        etatLabel.setStyle("-fx-background-color: " + color + "22; -fx-text-fill: " + color +
                "; -fx-font-weight: bold; -fx-background-radius: 50; -fx-padding: 5 15 5 15;");
    }

    @FXML
    private void handleEdit() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/tache_form.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 750);
            TacheFormController ctrl = loader.getController();
            ctrl.setProgramme(programme);
            ctrl.setObjectif(objectif);
            ctrl.setUtilisateurId(utilisateurId);
            ctrl.setTache(tache);
            Main.primaryStage.setScene(scene);
        } catch (Exception e) { showAlert("Erreur", e.getMessage()); }
    }

    @FXML
    private void handleDelete() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer");
        alert.setContentText("Supprimer cette tâche ?");
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    tacheDAO.delete(tache.getId());
                    goBackToProgramme();
                } catch (Exception e) { showAlert("Erreur", e.getMessage()); }
            }
        });
    }

    @FXML
    private void handleRetour() { goBackToProgramme(); }

    private void goBackToProgramme() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/programme_show.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 750);
            ProgrammeShowController ctrl = loader.getController();
            ctrl.setObjectif(objectif);
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