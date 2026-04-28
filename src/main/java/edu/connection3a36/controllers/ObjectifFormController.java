package edu.connection3a36.controllers;

import edu.connection3a36.services.*;
import edu.connection3a36.tools.AlertUtil;
import edu.connection3a36.tools.SessionManager;
import edu.connection3a36.tools.ToastNotification;
import edu.connection3a36.entities.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;

public class ObjectifFormController {

    @FXML private Label lblTitre;
    @FXML private TextField tfTitre;
    @FXML private TextArea taDescription;
    @FXML private DatePicker dpDebut;
    @FXML private DatePicker dpFin;
    @FXML private Button btnGenererTaches;
    @FXML private VBox vboxTachesPreview;
    @FXML private Label lblGenerationStatus;
    @FXML private ProgressIndicator progressIA;

    private Objectif objectif;
    private Runnable onSaved;
    private List<String[]> tachesGenerees;

    private final ObjectifService objectifService = new ObjectifService();
    private final TacheService tacheService = new TacheService();
    private final OllamaService ollamaService = new OllamaService();

    @FXML
    public void initialize() {
        dpDebut.setValue(LocalDate.now());
        dpFin.setValue(LocalDate.now().plusMonths(1));
        progressIA.setVisible(false);
        progressIA.setManaged(false);
        lblGenerationStatus.setVisible(false);
        lblGenerationStatus.setManaged(false);
    }

    public void setObjectif(Objectif o) {
        this.objectif = o;
        if (o != null) {
            lblTitre.setText("Modifier l objectif");
            tfTitre.setText(o.getTitre());
            taDescription.setText(o.getDescription());
            dpDebut.setValue(o.getDatedebut());
            dpFin.setValue(o.getDatefin());
            // Pas de statut — géré automatiquement
            btnGenererTaches.setVisible(false);
            btnGenererTaches.setManaged(false);
        } else {
            lblTitre.setText("Nouvel objectif");
        }
    }

    public void setOnSaved(Runnable callback) { this.onSaved = callback; }

    @FXML
    void handleGenererTaches() {
        String titre = tfTitre.getText().trim();
        if (titre.isEmpty()) { ToastNotification.showWarning("Champ requis", "Saisissez d'abord un titre."); return; }
        progressIA.setVisible(true); progressIA.setManaged(true);
        lblGenerationStatus.setText("Generation en cours...");
        lblGenerationStatus.setStyle("-fx-text-fill: #888; -fx-font-weight: bold;");
        lblGenerationStatus.setVisible(true); lblGenerationStatus.setManaged(true);
        btnGenererTaches.setDisable(true);
        vboxTachesPreview.getChildren().clear();

        Thread thread = new Thread(() -> {
            try {
                List<String[]> taches = ollamaService.genererTaches(titre, taDescription.getText().trim());
                tachesGenerees = taches;
                Platform.runLater(() -> {
                    afficherPreview(taches);
                    progressIA.setVisible(false); progressIA.setManaged(false);
                    lblGenerationStatus.setText(taches.size() + " taches generees !");
                    lblGenerationStatus.setStyle("-fx-text-fill: #198754; -fx-font-weight: bold;");
                    btnGenererTaches.setDisable(false);
                    ToastNotification.showSuccess("IA Ollama", taches.size() + " tâches générées avec succès !");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    progressIA.setVisible(false); progressIA.setManaged(false);
                    lblGenerationStatus.setText("Ollama indisponible — taches par defaut utilisees.");
                    lblGenerationStatus.setStyle("-fx-text-fill: #ffc107; -fx-font-weight: bold;");
                    btnGenererTaches.setDisable(false);
                    ToastNotification.showWarning("Ollama indisponible", "Tâches par défaut utilisées.");
                    try { tachesGenerees = ollamaService.genererTaches("", ""); afficherPreview(tachesGenerees); } catch (Exception ignored) {}
                });
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void afficherPreview(List<String[]> taches) {
        vboxTachesPreview.getChildren().clear();
        for (int i = 0; i < taches.size(); i++) {
            Label lbl = new Label((i + 1) + ".  " + taches.get(i)[0]);
            lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #102c59;");
            vboxTachesPreview.getChildren().add(lbl);
            if (taches.get(i)[1] != null && !taches.get(i)[1].isBlank()) {
                Label desc = new Label("     " + taches.get(i)[1]);
                desc.setStyle("-fx-text-fill: #888; -fx-font-size: 11px;");
                desc.setWrapText(true);
                vboxTachesPreview.getChildren().add(desc);
            }
        }
    }

    @FXML
    void handleSauvegarder() {
        String titre = tfTitre.getText().trim();
        if (titre.isEmpty()) { ToastNotification.showWarning("Champ requis", "Le titre est obligatoire."); return; }
        if (dpDebut.getValue() == null) { ToastNotification.showWarning("Champ requis", "La date de début est obligatoire."); return; }
        if (dpFin.getValue() == null) { ToastNotification.showWarning("Champ requis", "La date de fin est obligatoire."); return; }
        if (dpFin.getValue().isBefore(dpDebut.getValue())) {
            ToastNotification.showWarning("Dates invalides", "La date de fin doit être après la date de début."); return;
        }

        try {
            if (objectif == null) {
                Objectif o = new Objectif(titre, taDescription.getText().trim(),
                        dpDebut.getValue(), dpFin.getValue(), SessionManager.getCurrentUser().getId());
                o.setStatut(Statutobj.EnCours);
                objectifService.addEntity(o);

                if (tachesGenerees != null && !tachesGenerees.isEmpty() && o.getProgramme() != null) {
                    for (int i = 0; i < tachesGenerees.size(); i++) {
                        tacheService.addEntity(new Tache(i + 1, tachesGenerees.get(i)[0],
                                tachesGenerees.get(i)[1], Etat.encours, o.getProgramme().getId()));
                    }
                }
                ToastNotification.showSuccess("Objectif créé !", "\"" + titre + "\" a été ajouté à vos objectifs.");
            } else {
                objectif.setTitre(titre);
                objectif.setDescription(taDescription.getText().trim());
                objectif.setDatedebut(dpDebut.getValue());
                objectif.setDatefin(dpFin.getValue());
                objectifService.updateEntity(objectif.getId(), objectif);
                ToastNotification.showSuccess("Objectif modifié", "\"" + titre + "\" a été mis à jour.");
            }
            if (onSaved != null) onSaved.run();
            handleAnnuler();
        } catch (Exception e) { ToastNotification.showError("Erreur sauvegarde", e.getMessage()); }
    }

    @FXML
    void handleAnnuler() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ObjectifList.fxml"));
            Parent view = loader.load();
            MainController.getInstance().loadInContentArea(view);
        } catch (Exception e) { e.printStackTrace(); }
    }
}
