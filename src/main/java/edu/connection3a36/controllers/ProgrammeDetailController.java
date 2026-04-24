package edu.connection3a36.controllers;

import edu.connection3a36.services.*;
import edu.connection3a36.tools.AlertUtil;
import edu.connection3a36.tools.ExportUtil;
import edu.mentorai.entities.*;
import javafx.application.Platform;
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
import java.util.List;

public class ProgrammeDetailController {

    @FXML private Label lblObjectifTitre;
    @FXML private Label lblDeadline;
    @FXML private Label lblScore;
    @FXML private ProgressBar progressBar;
    @FXML private Label tachesCountLabel;
    @FXML private StackPane tacheChartPane;
    @FXML private Label realiséesLabel;
    @FXML private Label enCoursLabel;
    @FXML private Label abandonnesLabel;
    @FXML private Label lblMedaille;
    @FXML private Label medailleTextLabel;
    @FXML private Label lblMotivation;
    @FXML private Button btnRefreshMotivation;
    @FXML private ProgressIndicator progressMotivation;
    @FXML private Label lblAlerte;
    @FXML private VBox vboxTaches;
    @FXML private TextField tfRecherche;
    @FXML private ComboBox<String> cbTriEtat;

    // Formulaire ajout / modification tâche
    @FXML private VBox panneauFormTache;
    @FXML private Label lblFormTacheTitre;
    @FXML private TextField tfTacheTitre;
    @FXML private TextArea taTacheDescription;
    @FXML private Button btnSauvegarderTache;

    // Boutons à masquer en mode readOnly
    @FXML private Button btnGenererTaches;
    @FXML private Button btnAjouterTache;
    @FXML private Button btnReinitialiser;
    @FXML private Button btnSupprimerToutesTaches;

    // Tâche en cours de modification (null = ajout)
    private Tache tacheEnEdition = null;

    private Objectif objectif;
    private Programme programme;
    private List<Tache> taches;
    private int scorePrecedent = 0;
    // Mode lecture seule pour l'admin
    private boolean readOnly = false;

    private final ProgrammeService programmeService = new ProgrammeService();
    private final TacheService tacheService = new TacheService();
    private final MotivationService motivationService = new MotivationService();
    private final ScoreService scoreService = new ScoreService();
    private final OllamaService ollamaService = new OllamaService();
    private final NotificationService notifService = new NotificationService();

    @FXML
    public void initialize() {
        cbTriEtat.getItems().addAll("Ordre", "En cours d abord", "Realisees d abord", "Abandonnees d abord");
        cbTriEtat.setValue("Ordre");
    }

    public void setObjectif(Objectif o) { setObjectif(o, false); }

    public void setObjectif(Objectif o, boolean readOnly) {
        this.objectif = o;
        this.readOnly = readOnly;

        // Masquer tous les contrôles de modification pour l'admin
        if (readOnly) {
            hide(btnGenererTaches);
            hide(btnAjouterTache);
            hide(btnReinitialiser);
            hide(btnSupprimerToutesTaches);
            hide(panneauFormTache);
            hide(btnRefreshMotivation);
        }

        try {
            // Récupérer le programme via programme_id stocké dans l'objectif
            if (o.getProgramme() != null && o.getProgramme().getId() > 0) {
                programme = programmeService.getById(o.getProgramme().getId());
            }
            if (programme == null) {
                programme = programmeService.getByObjectifId(o.getId());
            }
            if (programme == null && !readOnly) {
                programme = new Programme("Programme — " + o.getTitre(), LocalDate.now());
                programmeService.addForObjectif(programme, o.getId());
            }
            if (programme == null) {
                AlertUtil.showError("Aucun programme trouve pour cet objectif.");
                return;
            }
            scorePrecedent = programme.getScorePourcentage();
            charger();
        } catch (Exception e) {
            AlertUtil.showError("Erreur chargement programme : " + e.getMessage());
        }
    }

    private void charger() throws Exception {
        taches = tacheService.getByProgramme(programme.getId());
        lblObjectifTitre.setText(objectif.getTitre());
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        lblDeadline.setText(objectif.getDatefin() != null
                ? "Deadline : " + objectif.getDatefin().format(fmt) : "Pas de deadline definie");

        rafraichirScore();

        String alerte = notifService.verifierDeadline(objectif, programme.getId());
        if (alerte != null) { lblAlerte.setText(alerte); lblAlerte.setVisible(true); lblAlerte.setManaged(true); }
        else { lblAlerte.setVisible(false); lblAlerte.setManaged(false); }

        Motivation lastMotiv = motivationService.getLatestByProgramme(programme.getId());
        lblMotivation.setText(lastMotiv != null ? lastMotiv.getMessagemotivant()
                : "Cliquez sur 'Rafraichir le message' pour generer un message motivant.");

        afficherTaches();
    }

    private void rafraichirScore() {
        try {
            programme = programmeService.getById(programme.getId());
            int score = programme.getScorePourcentage();
            Medaille medaille = programme.getMeilleureMedaille();

            lblScore.setText(score + "%");
            progressBar.setProgress(score / 100.0);
            progressBar.setStyle("-fx-accent: " + ScoreService.couleurMedaille(medaille) + ";");

            long realisees  = taches.stream().filter(t -> t.getEtat() == Etat.realisee).count();
            long encours    = taches.stream().filter(t -> t.getEtat() == Etat.encours).count();
            long abandonnes = taches.stream().filter(t -> t.getEtat() == Etat.Abandonner).count();
            int total = taches.size();

            tachesCountLabel.setText(realisees + " taches sur " + total + " validees");
            realiséesLabel.setText(realisees + " Realisees");
            enCoursLabel.setText(encours + " En cours");
            abandonnesLabel.setText(abandonnes + " Abandonnees");

            tacheChartPane.getChildren().clear();
            if (total > 0) {
                Canvas canvas = new Canvas(130, 130);
                GraphicsContext gc = canvas.getGraphicsContext2D();
                double[] vals = {realisees, encours, abandonnes};
                Color[] colors = {Color.web("#198754"), Color.web("#ffc107"), Color.web("#d52e28")};
                double startAngle = -90;
                for (int i = 0; i < vals.length; i++) {
                    if (vals[i] == 0) continue;
                    double arc = vals[i] / total * 360;
                    gc.setFill(colors[i]);
                    gc.fillArc(5, 5, 120, 120, startAngle, arc, javafx.scene.shape.ArcType.ROUND);
                    startAngle += arc;
                }
                gc.setFill(Color.WHITE);
                gc.fillOval(30, 30, 70, 70);
                gc.setFill(Color.web("#102c59"));
                gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 16));
                gc.fillText(String.valueOf(total), 58, 72);
                tacheChartPane.getChildren().add(canvas);
            }

            if (medaille != null) {
                lblMedaille.setText(switch (medaille) { case Bronze -> "🥉"; case Argent -> "🥈"; case Or -> "🥇"; });
                medailleTextLabel.setText(ScoreService.emojiMedaille(medaille));
                medailleTextLabel.setStyle("-fx-text-fill: " + ScoreService.couleurMedaille(medaille) + "; -fx-font-weight: bold;");
            } else {
                lblMedaille.setText("?");
                medailleTextLabel.setText("Validez vos taches pour gagner une medaille");
                medailleTextLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");
            }
        } catch (Exception e) { lblScore.setText("—"); }
    }

    private void afficherTaches() {
        if (taches == null) return;
        String recherche = tfRecherche.getText() != null ? tfRecherche.getText().toLowerCase().trim() : "";
        String tri = cbTriEtat.getValue();
        List<Tache> filtrees = taches.stream()
                .filter(t -> recherche.isEmpty() || t.getTitre().toLowerCase().contains(recherche)
                        || (t.getDescription() != null && t.getDescription().toLowerCase().contains(recherche)))
                .sorted((a, b) -> switch (tri != null ? tri : "Ordre") {
                    case "En cours d abord"    -> etatOrdre(a.getEtat()) - etatOrdre(b.getEtat());
                    case "Realisees d abord"   -> etatOrdreRealise(a.getEtat()) - etatOrdreRealise(b.getEtat());
                    case "Abandonnees d abord" -> etatOrdreAbandon(a.getEtat()) - etatOrdreAbandon(b.getEtat());
                    default -> Integer.compare(a.getOrdre(), b.getOrdre());
                }).toList();

        vboxTaches.getChildren().clear();
        for (Tache t : filtrees) vboxTaches.getChildren().add(buildTacheRow(t));
    }

    private HBox buildTacheRow(Tache t) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 18, 12, 18));
        row.setStyle("-fx-background-color: " + bgEtat(t.getEtat())
                + "; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(16,44,89,0.05), 6, 0, 0, 2);");

        Label lblOrdre = new Label(t.getOrdre() + ".");
        lblOrdre.setStyle("-fx-font-weight: bold; -fx-text-fill: #aaa; -fx-min-width: 28;");

        VBox info = new VBox(3);
        Label lblTitre = new Label(t.getTitre());
        String titreStyle = "-fx-font-weight: bold; -fx-text-fill: #102c59;";
        if (t.getEtat() == Etat.realisee) titreStyle += " -fx-strikethrough: true; -fx-text-fill: #aaa;";
        lblTitre.setStyle(titreStyle);
        info.getChildren().add(lblTitre);
        if (t.getDescription() != null && !t.getDescription().isBlank()) {
            Label lblDesc = new Label(t.getDescription());
            lblDesc.setStyle("-fx-text-fill: #888; -fx-font-size: 11px;");
            lblDesc.setWrapText(true);
            info.getChildren().add(lblDesc);
        }
        HBox.setHgrow(info, Priority.ALWAYS);

        Label badge = new Label(labelEtat(t.getEtat()));
        badge.setStyle("-fx-background-color: " + couleurEtat(t.getEtat())
                + "; -fx-text-fill: white; -fx-background-radius: 50; -fx-padding: 3 10 3 10; -fx-font-size: 10px; -fx-font-weight: bold;");

        row.getChildren().addAll(lblOrdre, info, badge);

        if (!readOnly) {
            ComboBox<String> cbEtat = new ComboBox<>();
            cbEtat.getItems().addAll("encours", "realisee", "Abandonner");
            cbEtat.setValue(t.getEtat() != null ? t.getEtat().getValue() : "encours");
            cbEtat.setStyle("-fx-font-size: 11px; -fx-background-radius: 8;");
            cbEtat.setOnAction(e -> changerEtat(t, cbEtat.getValue()));

            Button btnEdit = new Button("✏️");
            btnEdit.setStyle("-fx-background-color: transparent; -fx-text-fill: #102c59; -fx-cursor: hand; -fx-font-size: 13px;");
            btnEdit.setTooltip(new Tooltip("Modifier cette tache"));
            btnEdit.setOnAction(e -> ouvrirFormModification(t));

            Button btnDel = new Button("✕");
            btnDel.setStyle("-fx-background-color: transparent; -fx-text-fill: #d52e28; -fx-cursor: hand; -fx-font-weight: bold;");
            btnDel.setOnAction(e -> supprimerTache(t));
            row.getChildren().addAll(cbEtat, btnEdit, btnDel);
        }
        return row;
    }

    private void changerEtat(Tache t, String nouvelEtat) {
        try {
            tacheService.updateEtat(t.getId(), nouvelEtat);
            t.setEtat(Etat.fromValue(nouvelEtat));
            scorePrecedent = programme.getScorePourcentage();
            scoreService.recalculerEtSauvegarder(programme.getId());
            rafraichirScore();
            afficherTaches();
        } catch (Exception e) { AlertUtil.showError("Erreur mise a jour etat : " + e.getMessage()); }
    }

    private void supprimerTache(Tache t) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer la tache \"" + t.getTitre() + "\" ?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    tacheService.deleteEntity(t); taches.remove(t);
                    scoreService.recalculerEtSauvegarder(programme.getId());
                    rafraichirScore(); afficherTaches();
                } catch (Exception e) { AlertUtil.showError("Erreur suppression : " + e.getMessage()); }
            }
        });
    }

    @FXML void handleGenerateTaches() {
        if (readOnly) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Generer les taches avec l IA ?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                Thread thread = new Thread(() -> {
                    try {
                        List<String[]> generated = ollamaService.genererTaches(objectif.getTitre(), objectif.getDescription());
                        int startOrdre = taches.size() + 1;
                        for (int i = 0; i < generated.size(); i++) {
                            Tache t = new Tache(startOrdre + i, generated.get(i)[0], generated.get(i)[1], Etat.Abandonner, programme.getId());
                            tacheService.addEntity(t); taches.add(t);
                        }
                        scoreService.recalculerEtSauvegarder(programme.getId());
                        Platform.runLater(() -> { try { rafraichirScore(); } catch (Exception ignored) {} afficherTaches(); AlertUtil.showSuccess(generated.size() + " taches generees !"); });
                    } catch (Exception e) { Platform.runLater(() -> AlertUtil.showError("Erreur IA : " + e.getMessage())); }
                });
                thread.setDaemon(true); thread.start();
            }
        });
    }

    @FXML void handleAddTache() {
        if (readOnly) return;
        // Ouvrir le formulaire en mode ajout
        tacheEnEdition = null;
        lblFormTacheTitre.setText("Nouvelle tache");
        tfTacheTitre.clear();
        taTacheDescription.clear();
        btnSauvegarderTache.setText("Ajouter");
        panneauFormTache.setVisible(true);
        panneauFormTache.setManaged(true);
        tfTacheTitre.requestFocus();
    }

    /** Ouvre le formulaire en mode modification pour une tâche existante. */
    private void ouvrirFormModification(Tache t) {
        tacheEnEdition = t;
        lblFormTacheTitre.setText("Modifier la tache");
        tfTacheTitre.setText(t.getTitre());
        taTacheDescription.setText(t.getDescription() != null ? t.getDescription() : "");
        btnSauvegarderTache.setText("Modifier");
        panneauFormTache.setVisible(true);
        panneauFormTache.setManaged(true);
        tfTacheTitre.requestFocus();
    }

    @FXML void handleSauvegarderTache() {
        String titre = tfTacheTitre.getText().trim();
        if (titre.isEmpty()) {
            AlertUtil.showError("Le titre de la tache est obligatoire.");
            return;
        }
        String description = taTacheDescription.getText().trim();

        try {
            if (tacheEnEdition == null) {
                // Ajout — état par défaut : Abandonner
                Tache t = new Tache(taches.size() + 1, titre, description, Etat.Abandonner, programme.getId());
                tacheService.addEntity(t);
                taches.add(t);
                AlertUtil.showSuccess("Tache ajoutee avec succes !");
            } else {
                // Modification
                tacheEnEdition.setTitre(titre);
                tacheEnEdition.setDescription(description);
                tacheService.updateEntity(tacheEnEdition.getId(), tacheEnEdition);
                AlertUtil.showSuccess("Tache modifiee avec succes !");
            }
            scoreService.recalculerEtSauvegarder(programme.getId());
            fermerFormTache();
            rafraichirScore();
            afficherTaches();
        } catch (Exception e) {
            AlertUtil.showError("Erreur sauvegarde tache : " + e.getMessage());
        }
    }

    @FXML void handleAnnulerFormTache() {
        fermerFormTache();
    }

    private void fermerFormTache() {
        tacheEnEdition = null;
        tfTacheTitre.clear();
        taTacheDescription.clear();
        panneauFormTache.setVisible(false);
        panneauFormTache.setManaged(false);
    }

    @FXML void handleFiltrerTaches() { afficherTaches(); }

    @FXML void handleReinitialiser() {
        if (readOnly) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Reinitialiser toutes les taches ?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    for (Tache t : taches) { tacheService.updateEtat(t.getId(), Etat.encours.getValue()); t.setEtat(Etat.encours); }
                    scoreService.recalculerEtSauvegarder(programme.getId());
                    rafraichirScore(); afficherTaches(); AlertUtil.showSuccess("Taches reinitialises.");
                } catch (Exception e) { AlertUtil.showError("Erreur : " + e.getMessage()); }
            }
        });
    }

    @FXML void handleSupprimerToutesTaches() {
        if (readOnly) return;
        if (taches == null || taches.isEmpty()) { AlertUtil.showError("Aucune tache a supprimer."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer TOUTES les taches (" + taches.size() + ") ? Cette action est irreversible.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmation suppression");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    tacheService.deleteByProgramme(programme.getId());
                    taches.clear();
                    scoreService.recalculerEtSauvegarder(programme.getId());
                    rafraichirScore();
                    afficherTaches();
                    AlertUtil.showSuccess("Toutes les taches ont ete supprimees.");
                } catch (Exception e) { AlertUtil.showError("Erreur suppression : " + e.getMessage()); }
            }
        });
    }

    @FXML void handleRefreshMotivation() {
        progressMotivation.setVisible(true); progressMotivation.setManaged(true);
        btnRefreshMotivation.setDisable(true);
        lblMotivation.setText("Generation en cours...");
        Thread thread = new Thread(() -> {
            try {
                String message = ollamaService.genererMessageMotivant(objectif.getTitre(),
                        programme.getScorePourcentage(), scorePrecedent, objectif.getDatefin());
                Motivation m = new Motivation();
                m.setMessagemotivant(message); m.setDategeneration(LocalDate.now()); m.setProgrammeId(programme.getId());
                motivationService.addEntity(m);
                Platform.runLater(() -> { lblMotivation.setText(message); progressMotivation.setVisible(false); progressMotivation.setManaged(false); btnRefreshMotivation.setDisable(false); });
            } catch (Exception e) {
                Platform.runLater(() -> { lblMotivation.setText("Ollama indisponible. Verifiez que le service est demarre."); progressMotivation.setVisible(false); progressMotivation.setManaged(false); btnRefreshMotivation.setDisable(false); });
            }
        });
        thread.setDaemon(true); thread.start();
    }

    @FXML void handleExportExcel() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter en Excel");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (.xlsx)", "*.xlsx"));
        fc.setInitialFileName(objectif.getTitre().replaceAll("[^a-zA-Z0-9]", "_") + ".xlsx");
        File file = fc.showSaveDialog(vboxTaches.getScene().getWindow());
        if (file != null) {
            try { ExportUtil.exporterExcel(file.getAbsolutePath(), objectif, programme, taches); AlertUtil.showSuccess("Export Excel reussi."); }
            catch (Exception e) { AlertUtil.showError("Erreur export Excel : " + e.getMessage()); }
        }
    }

    @FXML void handleExportWord() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter en Word");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Word (.docx)", "*.docx"));
        fc.setInitialFileName(objectif.getTitre().replaceAll("[^a-zA-Z0-9]", "_") + ".docx");
        File file = fc.showSaveDialog(vboxTaches.getScene().getWindow());
        if (file != null) {
            try { ExportUtil.exporterWord(file.getAbsolutePath(), objectif, programme, taches); AlertUtil.showSuccess("Export Word reussi."); }
            catch (Exception e) { AlertUtil.showError("Erreur export Word : " + e.getMessage()); }
        }
    }

    @FXML void handleRetour() {
        try {
            // Retour vers la liste admin ou utilisateur selon le contexte
            String fxml = readOnly ? "/fxml/DashboardObjectifsAdmin.fxml" : "/fxml/ObjectifList.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent view = loader.load();
            MainController.getInstance().loadInContentArea(view);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private int etatOrdre(Etat e) { if (e == null) return 1; return switch (e) { case encours -> 0; case realisee -> 1; case Abandonner -> 2; }; }
    private int etatOrdreRealise(Etat e) { if (e == null) return 1; return switch (e) { case realisee -> 0; case encours -> 1; case Abandonner -> 2; }; }
    private int etatOrdreAbandon(Etat e) { if (e == null) return 1; return switch (e) { case Abandonner -> 0; case encours -> 1; case realisee -> 2; }; }
    private String bgEtat(Etat e) { if (e == null) return "white"; return switch (e) { case realisee -> "#f0fdf4"; case Abandonner -> "#fff5f5"; case encours -> "white"; }; }
    private String couleurEtat(Etat e) { if (e == null) return "#888"; return switch (e) { case realisee -> "#198754"; case Abandonner -> "#d52e28"; case encours -> "#ffc107"; }; }
    private String labelEtat(Etat e) { if (e == null) return "En cours"; return switch (e) { case realisee -> "Realisee"; case Abandonner -> "Abandonnee"; case encours -> "En cours"; }; }

    private static void hide(javafx.scene.Node n) {
        if (n != null) { n.setVisible(false); n.setManaged(false); }
    }
}
