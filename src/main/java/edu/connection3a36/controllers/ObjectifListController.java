package edu.connection3a36.controllers;

import edu.connection3a36.services.*;
import edu.connection3a36.tools.AlertUtil;
import edu.connection3a36.tools.ExportUtil;
import edu.connection3a36.tools.SessionManager;
import edu.connection3a36.tools.ToastNotification;
import edu.connection3a36.entities.Objectif;
import edu.connection3a36.entities.Programme;
import edu.connection3a36.entities.Statutobj;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

public class ObjectifListController {

    @FXML private VBox objectifContainer;
    @FXML private TextField tfRecherche;
    @FXML private ComboBox<String> cbStatut;
    @FXML private Label lblAlertes;
    @FXML private StackPane objectifChartPane;
    @FXML private Label atteintsLabel;
    @FXML private Label enCoursLabel;
    @FXML private Label abandonnesLabel;

    // Calendrier
    @FXML private Label lblCalMoisAnnee;
    @FXML private Label lblCalJour;
    @FXML private Label lblCalJourSemaine;
    @FXML private VBox vboxCalGrille;

    // ── Plan du jour ──────────────────────────────────────────────────────────
    @FXML private VBox cardPlanDuJour;
    @FXML private VBox vboxPlanTimeline;
    @FXML private Label lblPlanConseil;
    @FXML private Button btnPlanDuJour;
    @FXML private ProgressIndicator progressPlan;

    private final ObjectifService objectifService = new ObjectifService();
    private final ProgrammeService programmeService = new ProgrammeService();
    private final DeadlineNotificationService notifService = new DeadlineNotificationService();
    private final PlanificateurService planificateurService = new PlanificateurService();

    private List<Objectif> tousObjectifs;

    @FXML
    public void initialize() {
        cbStatut.getItems().addAll("Tous", "EnCours", "Atteint", "Abandonner");
        cbStatut.setValue("Tous");
        afficherCalendrier();
        charger();
    }

    private void charger() {
        try {
            int userId = SessionManager.getCurrentUser().getId();
            tousObjectifs = objectifService.getByUtilisateur(userId);

            afficherAlertes();
            mettreAJourStats();
            filtrer();
        } catch (Exception e) {
            ToastNotification.showError("Chargement impossible", e.getMessage());
        }
    }

    private void afficherCalendrier() {
        LocalDate today = LocalDate.now();
        Locale fr = Locale.FRENCH;

        // Bandeau mois/année
        String moisAnnee = today.getMonth().getDisplayName(TextStyle.FULL, fr).toUpperCase()
                + " " + today.getYear();
        lblCalMoisAnnee.setText(moisAnnee);

        // Numéro du jour
        lblCalJour.setText(String.valueOf(today.getDayOfMonth()));

        // Jour de la semaine
        lblCalJourSemaine.setText(today.getDayOfWeek().getDisplayName(TextStyle.FULL, fr).toUpperCase());

        // Grille des jours du mois
        vboxCalGrille.getChildren().clear();

        // En-têtes jours de la semaine
        String[] joursSemaine = {"L", "M", "M", "J", "V", "S", "D"};
        HBox headerRow = new HBox(4);
        headerRow.setAlignment(Pos.CENTER);
        for (String j : joursSemaine) {
            Label lbl = new Label(j);
            lbl.setMinWidth(24);
            lbl.setAlignment(Pos.CENTER);
            lbl.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #888;");
            headerRow.getChildren().add(lbl);
        }
        vboxCalGrille.getChildren().add(headerRow);

        // Premier jour du mois (1 = lundi, 7 = dimanche)
        LocalDate premierJour = today.withDayOfMonth(1);
        int debutSemaine = premierJour.getDayOfWeek().getValue(); // 1=lun, 7=dim
        int nbJours = today.lengthOfMonth();

        HBox semaine = new HBox(4);
        semaine.setAlignment(Pos.CENTER);

        // Cases vides avant le 1er
        for (int i = 1; i < debutSemaine; i++) {
            Label vide = new Label("");
            vide.setMinWidth(24);
            semaine.getChildren().add(vide);
        }

        int colonne = debutSemaine;
        for (int jour = 1; jour <= nbJours; jour++) {
            Label lblJour = new Label(String.valueOf(jour));
            lblJour.setMinWidth(24);
            lblJour.setMinHeight(22);
            lblJour.setAlignment(Pos.CENTER);

            if (jour == today.getDayOfMonth()) {
                // Jour actuel mis en évidence
                lblJour.setStyle("-fx-background-color: #d52e28; -fx-text-fill: white; "
                        + "-fx-background-radius: 50; -fx-font-weight: bold; -fx-font-size: 10px;");
            } else {
                lblJour.setStyle("-fx-font-size: 10px; -fx-text-fill: #555;");
            }

            semaine.getChildren().add(lblJour);
            colonne++;

            // Nouvelle ligne tous les 7 jours
            if (colonne > 7) {
                vboxCalGrille.getChildren().add(semaine);
                semaine = new HBox(4);
                semaine.setAlignment(Pos.CENTER);
                colonne = 1;
            }
        }
        // Dernière ligne si non vide
        if (!semaine.getChildren().isEmpty()) {
            vboxCalGrille.getChildren().add(semaine);
        }
    }

    private void mettreAJourStats() {
        if (tousObjectifs == null) return;
        long atteints   = tousObjectifs.stream().filter(o -> o.getStatut() == Statutobj.Atteint).count();
        long encours    = tousObjectifs.stream().filter(o -> o.getStatut() == Statutobj.EnCours).count();
        long abandonnes = tousObjectifs.stream().filter(o -> o.getStatut() == Statutobj.Abandonner).count();
        int total = tousObjectifs.size();

        atteintsLabel.setText(String.valueOf(atteints));
        enCoursLabel.setText(String.valueOf(encours));
        abandonnesLabel.setText(String.valueOf(abandonnes));

        objectifChartPane.getChildren().clear();
        if (total > 0) {
            Canvas canvas = new Canvas(140, 140);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            double[] vals = {atteints, encours, abandonnes};
            Color[] colors = {Color.web("#198754"), Color.web("#ffc107"), Color.web("#d52e28")};
            double startAngle = -90;
            for (int i = 0; i < vals.length; i++) {
                if (vals[i] == 0) continue;
                double arc = vals[i] / total * 360;
                gc.setFill(colors[i]);
                gc.fillArc(10, 10, 120, 120, startAngle, arc, javafx.scene.shape.ArcType.ROUND);
                startAngle += arc;
            }
            gc.setFill(Color.WHITE);
            gc.fillOval(35, 35, 70, 70);
            gc.setFill(Color.web("#102c59"));
            gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 18));
            gc.fillText(String.valueOf(total), 62, 76);
            objectifChartPane.getChildren().add(canvas);
        } else {
            Label lbl = new Label("Aucun objectif");
            lbl.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");
            objectifChartPane.getChildren().add(lbl);
        }
    }

    @FXML void handleNouvelObjectif() { ouvrirFormulaire(null); }

    /** Déclenché par le bouton "Filtrer" */
    @FXML void handleFiltrer() { filtrer(); }

    private void filtrer() {
        if (tousObjectifs == null) return;
        String recherche = tfRecherche.getText() != null ? tfRecherche.getText().toLowerCase().trim() : "";
        String statut = cbStatut.getValue();
        List<Objectif> filtres = tousObjectifs.stream()
                .filter(o -> recherche.isEmpty()
                        || o.getTitre().toLowerCase().contains(recherche)
                        || (o.getDescription() != null && o.getDescription().toLowerCase().contains(recherche)))
                .filter(o -> "Tous".equals(statut) || (o.getStatut() != null && o.getStatut().getValue().equals(statut)))
                .toList();
        afficherObjectifs(filtres);
    }

    private void afficherObjectifs(List<Objectif> objectifs) {
        objectifContainer.getChildren().clear();
        if (objectifs.isEmpty()) {
            VBox empty = new VBox(12);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(40));
            empty.setStyle("-fx-background-color: white; -fx-background-radius: 15; "
                    + "-fx-effect: dropshadow(gaussian, rgba(16,44,89,0.06), 8, 0, 0, 2);");
            Label icon = new Label("🎯");
            icon.setStyle("-fx-font-size: 40px;");
            Label lbl = new Label("Aucun objectif trouve.\nCliquez sur '+ Nouvel Objectif' pour commencer.");
            lbl.setStyle("-fx-text-fill: #888; -fx-font-size: 13px; -fx-text-alignment: center;");
            lbl.setWrapText(true);
            empty.getChildren().addAll(icon, lbl);
            objectifContainer.getChildren().add(empty);
            return;
        }
        for (Objectif o : objectifs) objectifContainer.getChildren().add(buildCard(o));
    }

    private VBox buildCard(Objectif o) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 20 25 20 25; "
                + "-fx-effect: dropshadow(gaussian, rgba(16,44,89,0.08), 10, 0, 0, 3);");

        // Titre + badge statut
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label lblTitre = new Label(o.getTitre());
        lblTitre.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #102c59;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label lblStatut = new Label(o.getStatut() != null ? o.getStatut().getValue() : "—");
        lblStatut.setStyle("-fx-background-color: " + couleurStatut(o.getStatut())
                + "; -fx-text-fill: white; -fx-background-radius: 50; -fx-padding: 4 12 4 12; -fx-font-size: 11px; -fx-font-weight: bold;");
        header.getChildren().addAll(lblTitre, spacer, lblStatut);

        // Description
        if (o.getDescription() != null && !o.getDescription().isBlank()) {
            Label lblDesc = new Label(o.getDescription());
            lblDesc.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
            lblDesc.setWrapText(true);
            card.getChildren().add(lblDesc);
        }

        // Dates
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String debut = o.getDatedebut() != null ? o.getDatedebut().format(fmt) : "—";
        String fin   = o.getDatefin()   != null ? o.getDatefin().format(fmt)   : "—";
        Label lblDates = new Label("Debut : " + debut + "   |   Deadline : " + fin);
        lblDates.setStyle("-fx-text-fill: #aaa; -fx-font-size: 11px;");

        // Score + barre
        HBox scoreBox = new HBox(10);
        scoreBox.setAlignment(Pos.CENTER_LEFT);
        try {
            Programme prog = o.getProgramme() != null && o.getProgramme().getId() > 0
                    ? programmeService.getById(o.getProgramme().getId()) : null;
            if (prog != null) {
                ProgressBar pb = new ProgressBar(prog.getScorePourcentage() / 100.0);
                pb.setPrefWidth(180);
                pb.setStyle("-fx-accent: " + ScoreService.couleurMedaille(prog.getMeilleureMedaille()) + ";");
                Label lblScore = new Label(prog.getScorePourcentage() + "%");
                lblScore.setStyle("-fx-font-weight: bold; -fx-text-fill: #102c59;");
                Label lblMedaille = new Label(ScoreService.emojiMedaille(prog.getMeilleureMedaille()));
                scoreBox.getChildren().addAll(pb, lblScore, lblMedaille);
            }
        } catch (Exception ignored) {}

        // Boutons
        HBox btnBox = new HBox(8);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        Button btnDetail = new Button("Voir le programme");
        btnDetail.setStyle("-fx-background-color: #102c59; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 50; -fx-padding: 8 18 8 18; -fx-cursor: hand;");
        btnDetail.setOnAction(e -> ouvrirProgramme(o));

        Button btnModifier = new Button("Modifier");
        btnModifier.setStyle("-fx-background-color: #f0f4f8; -fx-text-fill: #102c59; -fx-font-weight: bold; -fx-background-radius: 50; -fx-padding: 8 18 8 18; -fx-cursor: hand;");
        btnModifier.setOnAction(e -> ouvrirFormulaire(o));

        Button btnSupprimer = new Button("Supprimer");
        btnSupprimer.setStyle("-fx-background-color: transparent; -fx-text-fill: #d52e28; -fx-border-color: #d52e28; -fx-border-radius: 50; -fx-background-radius: 50; -fx-font-weight: bold; -fx-padding: 8 18 8 18; -fx-cursor: hand;");
        btnSupprimer.setOnAction(e -> supprimer(o));

        btnBox.getChildren().addAll(btnDetail, btnModifier, btnSupprimer);
        card.getChildren().addAll(header, lblDates, scoreBox, new Separator(), btnBox);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ALERTES
    // ─────────────────────────────────────────────────────────────────────────

    private void afficherAlertes() {
        List<String> alertes = notifService.verifierToutesDeadlines(tousObjectifs);
        if (alertes.isEmpty()) {
            lblAlertes.setVisible(false);
            lblAlertes.setManaged(false);
        } else {
            lblAlertes.setText("⚠️  " + String.join("\n⚠️  ", alertes));
            lblAlertes.setVisible(true);
            lblAlertes.setManaged(true);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NAVIGATION
    // ─────────────────────────────────────────────────────────────────────────

    private void ouvrirFormulaire(Objectif objectif) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ObjectifForm.fxml"));
            Parent view = loader.load();
            ObjectifFormController ctrl = loader.getController();
            ctrl.setObjectif(objectif);
            ctrl.setOnSaved(this::charger);
            MainController.getInstance().loadInContentArea(view);
        } catch (Exception e) { ToastNotification.showError("Ouverture formulaire", e.getMessage()); }
    }

    private void ouvrirProgramme(Objectif objectif) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ProgrammeDetail.fxml"));
            Parent view = loader.load();
            ProgrammeDetailController ctrl = loader.getController();
            ctrl.setObjectif(objectif);
            MainController.getInstance().loadInContentArea(view);
        } catch (Exception e) { ToastNotification.showError("Ouverture programme", e.getMessage()); }
    }

    private void supprimer(Objectif o) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer l objectif \"" + o.getTitre() + "\" et tout son programme ?",
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    objectifService.deleteEntity(o);
                    charger();
                    ToastNotification.showSuccess("Objectif supprimé", "\"" + o.getTitre() + "\" a été supprimé.");
                }
                catch (Exception e) { ToastNotification.showError("Erreur suppression", e.getMessage()); }
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EXPORTS
    // ─────────────────────────────────────────────────────────────────────────

    @FXML void handleExportExcel() {
        if (tousObjectifs == null || tousObjectifs.isEmpty()) { ToastNotification.showWarning("Export Excel", "Aucun objectif à exporter."); return; }
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter en Excel");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (.xlsx)", "*.xlsx"));
        fc.setInitialFileName("objectifs.xlsx");
        File file = fc.showSaveDialog(objectifContainer.getScene().getWindow());
        if (file != null) {
            try {
                for (Objectif o : tousObjectifs) {
                    Programme prog = o.getProgramme() != null ? programmeService.getById(o.getProgramme().getId()) : null;
                    if (prog != null) {
                        ExportUtil.exporterExcel(file.getAbsolutePath(), o, prog, new TacheService().getByProgramme(prog.getId()));
                        break;
                    }
                }
                ToastNotification.showSuccess("Export Excel réussi", file.getName() + " sauvegardé.");
            } catch (Exception e) { ToastNotification.showError("Erreur export Excel", e.getMessage()); }
        }
    }

    @FXML void handleExportWord() {
        if (tousObjectifs == null || tousObjectifs.isEmpty()) { ToastNotification.showWarning("Export Word", "Aucun objectif à exporter."); return; }
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter en Word");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Word (.docx)", "*.docx"));
        fc.setInitialFileName("objectifs.docx");
        File file = fc.showSaveDialog(objectifContainer.getScene().getWindow());
        if (file != null) {
            try {
                for (Objectif o : tousObjectifs) {
                    Programme prog = o.getProgramme() != null ? programmeService.getById(o.getProgramme().getId()) : null;
                    if (prog != null) {
                        ExportUtil.exporterWord(file.getAbsolutePath(), o, prog, new TacheService().getByProgramme(prog.getId()));
                        break;
                    }
                }
                ToastNotification.showSuccess("Export Word réussi", file.getName() + " sauvegardé.");
            } catch (Exception e) { ToastNotification.showError("Erreur export Word", e.getMessage()); }
        }
    }

    @FXML void handleReinitialiserTout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer TOUS vos objectifs ? Action irreversible.", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    for (Objectif o : tousObjectifs) objectifService.deleteEntity(o);
                    charger();
                    ToastNotification.showInfo("Réinitialisation", "Toutes les données ont été supprimées.");
                }
                catch (Exception e) { ToastNotification.showError("Erreur réinitialisation", e.getMessage()); }
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PLAN DU JOUR
    // ─────────────────────────────────────────────────────────────────────────

    @FXML void handlePlanDuJour() {
        if (cardPlanDuJour == null) return;
        if (progressPlan != null) { progressPlan.setVisible(true); progressPlan.setManaged(true); }
        if (btnPlanDuJour != null) btnPlanDuJour.setDisable(true);

        final List<Objectif> snapshot = tousObjectifs != null ? List.copyOf(tousObjectifs) : List.of();

        Task<PlanificateurService.PlanResultat> task = new Task<>() {
            @Override
            protected PlanificateurService.PlanResultat call() {
                return planificateurService.genererPlan(snapshot);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            afficherPlan(task.getValue());
            if (progressPlan != null) { progressPlan.setVisible(false); progressPlan.setManaged(false); }
            if (btnPlanDuJour != null) btnPlanDuJour.setDisable(false);
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            if (progressPlan != null) { progressPlan.setVisible(false); progressPlan.setManaged(false); }
            if (btnPlanDuJour != null) btnPlanDuJour.setDisable(false);
        }));
        new Thread(task).start();
    }

    private void afficherPlan(PlanificateurService.PlanResultat r) {
        if (cardPlanDuJour == null) return;
        if (vboxPlanTimeline != null) vboxPlanTimeline.getChildren().clear();

        if (r.plan.isEmpty()) {
            if (lblPlanConseil != null) lblPlanConseil.setText(r.conseil);
            cardPlanDuJour.setVisible(true);
            cardPlanDuJour.setManaged(true);
            return;
        }

        for (PlanificateurService.CreneauPlan creneau : r.plan) {
            HBox ligne = new HBox(0);
            ligne.setAlignment(Pos.CENTER_LEFT);

            // Heure
            Label lblHeure = new Label(creneau.heure);
            lblHeure.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #102c59;"
                    + " -fx-min-width: 80; -fx-padding: 10 12 10 0;");

            // Ligne verticale timeline
            VBox timeline = new VBox();
            timeline.setAlignment(Pos.CENTER);
            timeline.setMinWidth(20);
            Label dot = new Label("●");
            dot.setStyle("-fx-text-fill: #102c59; -fx-font-size: 8px;");
            timeline.getChildren().add(dot);

            // Contenu
            VBox contenu = new VBox(2);
            contenu.setPadding(new Insets(8, 12, 8, 12));
            contenu.setStyle("-fx-background-color: #f8faff; -fx-background-radius: 8;"
                    + " -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-border-width: 1;");
            HBox.setHgrow(contenu, Priority.ALWAYS);

            Label lblTache = new Label(creneau.tache);
            lblTache.setStyle("-fx-font-weight: bold; -fx-text-fill: #102c59; -fx-font-size: 12px;");
            lblTache.setWrapText(true);

            Label lblObj = new Label("📌 " + creneau.objectif);
            lblObj.setStyle("-fx-text-fill: #888; -fx-font-size: 11px;");

            contenu.getChildren().addAll(lblTache, lblObj);
            ligne.getChildren().addAll(lblHeure, timeline, contenu);

            if (vboxPlanTimeline != null) vboxPlanTimeline.getChildren().add(ligne);
        }

        if (lblPlanConseil != null) lblPlanConseil.setText("💡 " + r.conseil);
        cardPlanDuJour.setVisible(true);
        cardPlanDuJour.setManaged(true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS COULEURS / ICÔNES
    // ─────────────────────────────────────────────────────────────────────────

    private String couleurStatut(Statutobj s) {
        if (s == null) return "#94a3b8";
        return switch (s) { case EnCours -> "#f59e0b"; case Atteint -> "#22c55e"; case Abandonner -> "#ef4444"; };
    }

    private String couleurStatutLight(Statutobj s) {
        if (s == null) return "#f8fafc";
        return switch (s) { case EnCours -> "#fffbeb"; case Atteint -> "#f0fdf4"; case Abandonner -> "#fff5f5"; };
    }

    private String couleurStatutText(Statutobj s) {
        if (s == null) return "#64748b";
        return switch (s) { case EnCours -> "#d97706"; case Atteint -> "#16a34a"; case Abandonner -> "#dc2626"; };
    }

    private String iconStatut(Statutobj s) {
        if (s == null) return "○";
        return switch (s) { case EnCours -> "🔄"; case Atteint -> "✅"; case Abandonner -> "❌"; };
    }
}
