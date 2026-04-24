package edu.mentorai.Controller;

import edu.mentorai.Main;
import edu.mentorai.entities.Objectif;
import edu.mentorai.entities.Programme;
import edu.mentorai.entities.Statutobj;
import edu.mentorai.services.ObjectifService;
import edu.mentorai.services.ProgrammeService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;

import java.time.LocalDate;

public class ObjectifFormController {

    @FXML private Label headerLabel;
    @FXML private TextField titreField;
    @FXML private TextArea descriptionField;
    @FXML private DatePicker datedebutField;
    @FXML private DatePicker datefinField;
    @FXML private Label titreError;
    @FXML private Label descriptionError;
    @FXML private Label datedebutError;
    @FXML private Label datefinError;
    @FXML private Button submitBtn;

    private int utilisateurId = 1;
    private Objectif objectifToEdit = null;
    private Runnable onSaved;

    private final ObjectifService objectifService = new ObjectifService();
    private final ProgrammeService programmeService = new ProgrammeService();

    public void setUtilisateurId(int id) { this.utilisateurId = id; }
    public void setOnSaved(Runnable r) { this.onSaved = r; }

    public void setObjectif(Objectif obj) {
        this.objectifToEdit = obj;
        headerLabel.setText("Modifier l'Objectif");
        submitBtn.setText("Enregistrer les modifications");
        titreField.setText(obj.getTitre());
        descriptionField.setText(obj.getDescription());
        datedebutField.setValue(obj.getDatedebut());
        datefinField.setValue(obj.getDatefin());
    }

    @FXML
    private void handleSubmit() {
        // Reset errors
        titreError.setText("");
        descriptionError.setText("");
        datedebutError.setText("");
        datefinError.setText("");

        String titre = titreField.getText().trim();
        String description = descriptionField.getText().trim();
        LocalDate datedebut = datedebutField.getValue();
        LocalDate datefin = datefinField.getValue();

        boolean valid = true;

        // Validation titre
        if (titre.isEmpty()) {
            titreError.setText("Le titre est obligatoire.");
            valid = false;
        } else if (titre.length() < 3) {
            titreError.setText("Le titre doit contenir au moins 3 caractères.");
            valid = false;
        } else if (titre.length() > 150) {
            titreError.setText("Le titre ne peut pas dépasser 150 caractères.");
            valid = false;
        }

        // Validation description
        if (description.isEmpty()) {
            descriptionError.setText("La description est obligatoire.");
            valid = false;
        }

        // Validation date début
        if (datedebut == null) {
            datedebutError.setText("La date de début est obligatoire.");
            valid = false;
        } else if (objectifToEdit == null && datedebut.isBefore(LocalDate.now())) {
            datedebutError.setText("La date de début ne peut pas être dans le passé.");
            valid = false;
        }

        // Validation date fin
        if (datefin == null) {
            datefinError.setText("La date de fin est obligatoire.");
            valid = false;
        } else if (datedebut != null && !datefin.isAfter(datedebut)) {
            datefinError.setText("La date de fin doit être après la date de début.");
            valid = false;
        }

        if (!valid) return;

        try {
            if (objectifToEdit == null) {
                // CREATE — créer programme d'abord
                Programme programme = new Programme();
                programme.setTitre(titre);
                programme.setDategeneration(LocalDate.now());
                programme.setScorePourcentage(0);
                programme = programmeService.save(programme);

                Objectif objectif = new Objectif();
                objectif.setTitre(titre);
                objectif.setDescription(description);
                objectif.setDatedebut(datedebut);
                objectif.setDatefin(datefin);
                objectif.setStatut(Statutobj.EnCours);
                objectif.setProgramme(programme);
                objectif.setUtilisateurId(utilisateurId);
                objectif = objectifService.save(objectif);
                showInfo("Succès", "Objectif créé avec succès !");
            } else {
                // UPDATE
                objectifToEdit.setTitre(titre);
                objectifToEdit.setDescription(description);
                objectifToEdit.setDatedebut(datedebut);
                objectifToEdit.setDatefin(datefin);
                objectifService.update(objectifToEdit);

                // Update programme titre aussi
                if (objectifToEdit.getProgramme() != null) {
                    objectifToEdit.getProgramme().setTitre(titre);
                }

                showInfo("Succès", "Objectif modifié avec succès !");
            }

            // Retour à la liste
            goBackToList();

        } catch (Exception e) {
            showAlert("Erreur", e.getMessage());
        }
    }

    @FXML
    private void handleRetour() {
        goBackToList();
    }

    private void goBackToList() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/objectif_list.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 750);
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

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setContentText(msg); a.showAndWait();
    }
}