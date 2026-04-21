package edu.mentorai.Controller;

import edu.mentorai.Main;
import edu.mentorai.entities.*;
import edu.mentorai.interfaces.*;
import edu.mentorai.services.MotivationService;
import edu.mentorai.services.ObjectifService;
import edu.mentorai.services.ProgrammeService;
import edu.mentorai.services.TacheService;
import edu.mentorai.tools.CircleChart;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class ProgrammeShowController {

    @FXML private Label objectifTitreLabel;
    @FXML private Label dateGenerationLabel;
    @FXML private Label motivationLabel;
    @FXML private Label scoreLabel;
    @FXML private ProgressBar progressBar;
    @FXML private Label tachesCountLabel;
    @FXML private Label totalTachesLabel;
    @FXML private Label realiséesLabel;
    @FXML private Label enCoursLabel;
    @FXML private Label abandonnesLabel;
    @FXML private Label medailleLabel;
    @FXML private Label medailleTextLabel;
    @FXML private VBox tachesContainer;
    @FXML private StackPane tacheChartPane;

    private Objectif objectif;
    private Programme programme;
    private int utilisateurId = 1;

    private final ProgrammeService programmeService = new ProgrammeService();
    private final TacheService tacheService = new TacheService();
    private final MotivationService motivationService = new MotivationService();
    private final ObjectifService objectifService = new ObjectifService();

    public void setObjectif(Objectif obj) { this.objectif = obj; }
    public void setUtilisateurId(int id) { this.utilisateurId = id; }

    public void loadData() {
        try {
            // Load programme
            programme = programmeService.findByObjectifId(objectif.getId());
            if (programme == null) return;

            // Header
            objectifTitreLabel.setText(objectif.getTitre());
            dateGenerationLabel.setText("Généré le " +
                    (programme.getDategeneration() != null ? programme.getDategeneration().toString() : "—") +
                    " pour votre réussite.");

            // Motivation
            Motivation motivation = motivationService.findLatestByProgramme(programme.getId());
            if (motivation != null && motivation.getMessagemotivant() != null) {
                motivationLabel.setText(motivation.getMessagemotivant());
            }

            // Taches stats
            List<Tache> taches = tacheService.findByProgramme(programme.getId());
            programme.setTaches(taches);

            int total = taches.size();
            long realisees = taches.stream().filter(t -> t.getEtat() == Etat.realisee).count();
            long encours = taches.stream().filter(t -> t.getEtat() == Etat.encours).count();
            long abandonnes = taches.stream().filter(t -> t.getEtat() == Etat.Abandonner).count();

            int score = total > 0 ? (int) Math.round((realisees * 100.0) / total) : 0;

            scoreLabel.setText(score + "%");
            progressBar.setProgress(score / 100.0);
            tachesCountLabel.setText(realisees + " tâches sur " + total + " validées");
            totalTachesLabel.setText(String.valueOf(total));
            realiséesLabel.setText(realisees + " Réalisées");
            enCoursLabel.setText(encours + " En cours");
            abandonnesLabel.setText(abandonnes + " Abandonnées");
            // Cercle taches
            tacheChartPane.getChildren().clear();
            tacheChartPane.getChildren().add(
                    CircleChart.createDonut(
                            realisees, encours, abandonnes,
                            total,
                            String.valueOf(total),
                            "Taches"
                    )
            );

            // Medaille
            String medaille = programme.getMeilleureMedaille() != null
                    ? programme.getMeilleureMedaille().getValue() : null;
            if (medaille != null) {
                medailleLabel.setText(switch (medaille) {
                    case "Or" -> "🥇";
                    case "Argent" -> "🥈";
                    case "Bronze" -> "🥉";
                    default -> "🏆";
                });
                medailleTextLabel.setText("Médaille " + medaille + " !");
            } else {
                medailleLabel.setText("🏆");
                medailleTextLabel.setText("Validez vos tâches pour gagner une médaille");
            }

            // Display taches
            afficherTaches(taches);

        } catch (Exception e) {
            showAlert("Erreur", e.getMessage());
        }
    }

    private void afficherTaches(List<Tache> taches) {
        tachesContainer.getChildren().clear();

        if (taches.isEmpty()) {
            Label empty = new Label("Aucune tâche. Cliquez sur '+ Ajouter une tâche'.");
            empty.setStyle("-fx-text-fill: #888; -fx-font-size: 14px; -fx-padding: 20;");
            tachesContainer.getChildren().add(empty);
            return;
        }

        for (Tache tache : taches) {
            tachesContainer.getChildren().add(createTacheRow(tache));
        }
    }

    private HBox createTacheRow(Tache tache) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(15, 20, 15, 20));
        row.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(16,44,89,0.05), 8, 0, 0, 2);");

        // Numero
        Label numLabel = new Label(String.valueOf(tache.getOrdre()));
        numLabel.setStyle("-fx-background-color: #102c59; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 50; -fx-min-width: 32; -fx-min-height: 32; " +
                "-fx-alignment: center; -fx-font-size: 13px;");
        numLabel.setMinSize(32, 32);
        numLabel.setAlignment(Pos.CENTER);

        // Titre + desc
        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label titreLabel = new Label(tache.getTitre());
        titreLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #102c59; -fx-font-size: 14px;");
        Label descLabel = new Label(tache.getDescription() != null ? tache.getDescription() : "");
        descLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");
        info.getChildren().addAll(titreLabel, descLabel);

        // Etat badge
        String etatColor = switch (tache.getEtat()) {
            case realisee -> "#198754";
            case encours -> "#ffc107";
            case Abandonner -> "#d52e28";
        };
        String etatText = switch (tache.getEtat()) {
            case realisee -> "RÉALISÉE";
            case encours -> "EN COURS";
            case Abandonner -> "ABANDONNER";
        };
        Label etatBadge = new Label(etatText);
        etatBadge.setStyle("-fx-background-color: " + etatColor + "22; -fx-text-fill: " + etatColor +
                "; -fx-font-weight: bold; -fx-font-size: 10px; -fx-background-radius: 50; -fx-padding: 4 10 4 10;");

        // Actions
        Button btnVoir = createTacheBtn("👁", "#f0fdf4", "#198754");
        Button btnEdit = createTacheBtn("✏", "#fffbeb", "#d97706");
        Button btnDel = createTacheBtn("🗑", "#fef2f2", "#dc2626");

        btnVoir.setOnAction(e -> handleVoirTache(tache));
        btnEdit.setOnAction(e -> handleEditTache(tache));
        btnDel.setOnAction(e -> handleDeleteTache(tache));

        row.getChildren().addAll(numLabel, info, etatBadge, btnVoir, btnEdit, btnDel);
        return row;
    }

    private Button createTacheBtn(String text, String bg, String fg) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg +
                "; -fx-background-radius: 8; -fx-padding: 8 12 8 12; -fx-cursor: hand; -fx-font-size: 13px;");
        return btn;
    }

    @FXML
    private void handleAddTache() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/tache_form.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 750);
            TacheFormController ctrl = loader.getController();
            ctrl.setProgramme(programme);
            ctrl.setUtilisateurId(utilisateurId);
            ctrl.setObjectif(objectif);
            Main.primaryStage.setScene(scene);
        } catch (Exception e) { showAlert("Erreur", e.getMessage()); }
    }

    private void handleVoirTache(Tache tache) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/tache_show.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 750);
            TacheShowController ctrl = loader.getController();
            ctrl.setTache(tache);
            ctrl.setProgramme(programme);
            ctrl.setObjectif(objectif);
            ctrl.setUtilisateurId(utilisateurId);
            Main.primaryStage.setScene(scene);
        } catch (Exception e) { showAlert("Erreur", e.getMessage()); }
    }

    private void handleEditTache(Tache tache) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/tache_form.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 750);
            TacheFormController ctrl = loader.getController();
            ctrl.setProgramme(programme);
            ctrl.setUtilisateurId(utilisateurId);
            ctrl.setObjectif(objectif);
            ctrl.setTache(tache);
            Main.primaryStage.setScene(scene);
        } catch (Exception e) { showAlert("Erreur", e.getMessage()); }
    }

    private void handleDeleteTache(Tache tache) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer");
        alert.setContentText("Supprimer cette tâche ?");
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    tacheService.delete(tache.getId());
                    updateStats();
                    loadData();
                } catch (Exception e) { showAlert("Erreur", e.getMessage()); }
            }
        });
    }

    private void updateStats() throws Exception {
        List<Tache> taches = tacheService.findByProgramme(programme.getId());
        int total = taches.size();
        long realisees = taches.stream().filter(t -> t.getEtat() == Etat.realisee).count();
        int score = total > 0 ? (int) Math.round((realisees * 100.0) / total) : 0;

        Medaille medaille = null;
        if (score >= 90) medaille = Medaille.Or;
        else if (score >= 60) medaille = Medaille.Argent;
        else if (score >= 30) medaille = Medaille.Bronze;

        programmeService.updateScore(programme.getId(), score, medaille);

        // Update objectif statut
        Statutobj newStatut = score == 0 ? Statutobj.Abandonner
                : score == 100 ? Statutobj.Atteint : Statutobj.EnCours;
        objectif.setStatut(newStatut);
        objectifService.update(objectif);
    }

    @FXML
    private void handleRetour() {
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

}