package edu.mentorai.Controller;

import edu.mentorai.Main;
import edu.mentorai.entities.*;
import edu.mentorai.interfaces.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;

import java.util.List;

public class TacheFormController {

    @FXML private Label headerLabel;
    @FXML private TextField ordreField;
    @FXML private TextField titreField;
    @FXML private TextArea descriptionField;
    @FXML private ComboBox<String> etatCombo;
    @FXML private Label ordreError;
    @FXML private Label titreError;
    @FXML private Label etatError;
    @FXML private Button submitBtn;

    private Programme programme;
    private Objectif objectif;
    private Tache tacheToEdit;
    private int utilisateurId = 1;

    private final TacheDAO tacheDAO = new TacheDAO();
    private final ProgrammeDAO programmeDAO = new ProgrammeDAO();
    private final ObjectifDAO objectifDAO = new ObjectifDAO();
    private final MotivationDAO motivationDAO = new MotivationDAO();

    @FXML
    public void initialize() {
        etatCombo.getItems().addAll("Abandonner", "encours", "realisee");
        etatCombo.setValue("Abandonner");
    }

    public void setProgramme(Programme p) { this.programme = p; }
    public void setObjectif(Objectif o) { this.objectif = o; }
    public void setUtilisateurId(int id) { this.utilisateurId = id; }

    public void setTache(Tache tache) {
        this.tacheToEdit = tache;
        headerLabel.setText("Modifier la tâche");
        submitBtn.setText("Enregistrer");
        ordreField.setText(String.valueOf(tache.getOrdre()));
        titreField.setText(tache.getTitre());
        descriptionField.setText(tache.getDescription() != null ? tache.getDescription() : "");
        etatCombo.setValue(tache.getEtat().getValue());
    }

    @FXML
    private void handleSubmit() {
        ordreError.setText(""); titreError.setText(""); etatError.setText("");

        String ordreStr = ordreField.getText().trim();
        String titre = titreField.getText().trim();
        String description = descriptionField.getText().trim();
        String etatStr = etatCombo.getValue();

        boolean valid = true;

        if (ordreStr.isEmpty()) {
            ordreError.setText("L'ordre est obligatoire."); valid = false;
        } else {
            try {
                int ordre = Integer.parseInt(ordreStr);
                if (ordre <= 0) { ordreError.setText("L'ordre doit être positif."); valid = false; }
            } catch (NumberFormatException e) {
                ordreError.setText("L'ordre doit être un nombre."); valid = false;
            }
        }

        if (titre.isEmpty()) { titreError.setText("Le titre est obligatoire."); valid = false; }
        else if (titre.length() > 255) { titreError.setText("Max 255 caractères."); valid = false; }

        if (etatStr == null) { etatError.setText("L'état est obligatoire."); valid = false; }

        if (!valid) return;

        try {
            Etat etat = Etat.fromValue(etatStr);
            int ordre = Integer.parseInt(ordreStr);

            if (tacheToEdit == null) {
                Tache tache = new Tache();
                tache.setOrdre(ordre);
                tache.setTitre(titre);
                tache.setDescription(description.isEmpty() ? null : description);
                tache.setEtat(etat);
                tache.setProgrammeId(programme.getId());
                tacheDAO.save(tache);
            } else {
                tacheToEdit.setOrdre(ordre);
                tacheToEdit.setTitre(titre);
                tacheToEdit.setDescription(description.isEmpty() ? null : description);
                tacheToEdit.setEtat(etat);
                tacheDAO.update(tacheToEdit);
            }

            updateStats();
            goBackToProgramme();

        } catch (Exception e) { showAlert("Erreur", e.getMessage()); }
    }

    private void updateStats() throws Exception {
        List<Tache> taches = tacheDAO.findByProgramme(programme.getId());
        int total = taches.size();
        long realisees = taches.stream().filter(t -> t.getEtat() == Etat.realisee).count();
        int score = total > 0 ? (int) Math.round((realisees * 100.0) / total) : 0;

        Medaille medaille = null;
        if (score >= 90) medaille = Medaille.Or;
        else if (score >= 60) medaille = Medaille.Argent;
        else if (score >= 30) medaille = Medaille.Bronze;

        programmeDAO.updateScore(programme.getId(), score, medaille);

        Statutobj newStatut;
        if (score == 0) newStatut = Statutobj.Abandonner;
        else if (score == 100) newStatut = Statutobj.Atteint;
        else newStatut = Statutobj.EnCours;

        objectif.setStatut(newStatut);
        objectifDAO.update(objectif);

        String msg;
        if (score < 30) msg = "Continue, tu es sur la bonne voie !";
        else if (score < 60) msg = "Bravo ! Tu progresses bien. Garde ce rythme !";
        else if (score < 90) msg = "Excellent ! Tu es presque au bout !";
        else msg = "Incroyable ! " + score + "% de reussite !";

        Motivation motivation = new Motivation();
        motivation.setMessagemotivant(msg);
        motivation.setDategeneration(java.time.LocalDate.now());
        motivation.setProgrammeId(programme.getId());
        motivationDAO.save(motivation);
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