package edu.connection3a36.controllers;

import edu.connection3a36.services.*;
import edu.connection3a36.tools.AlertUtil;
import edu.connection3a36.tools.ExportUtil;
import edu.connection3a36.tools.ToastNotification;
import edu.connection3a36.entities.*;
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

    // ── Resume IA ─────────────────────────────────────────────────────────────
    @FXML private VBox    cardResume;
    @FXML private Label   lblResumeTexte;
    @FXML private Label   lblResumePointPositif;
    @FXML private Label   lblResumeConseil;
    @FXML private Button  btnGenererRapport;
    @FXML private ProgressIndicator progressResume;

    // Tâche en cours de modification (null = ajout)
    private Tache tacheEnEdition = null;

    private Objectif objectif;
    private Programme programme;
    private List<Tache> taches;
    private int scorePrecedent = 0;
    private boolean readOnly = false;
    // Titres des taches a risque (pour badge 🚨)
    private List<String> tachesARisque = new java.util.ArrayList<>();

    private final ProgrammeService programmeService = new ProgrammeService();
    private final TacheService tacheService = new TacheService();
    private final MotivationService motivationService = new MotivationService();
    private final ScoreService scoreService = new ScoreService();
    private final OllamaService ollamaService = new OllamaService();
    private final DeadlineNotificationService notifService = new DeadlineNotificationService();
    private final ResumeService resumeService = new ResumeService();
    private final RisqueAbandonService risqueService = new RisqueAbandonService();

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
                ToastNotification.showError("Programme introuvable", "Aucun programme trouvé pour cet objectif.");
                return;
            }
            scorePrecedent = programme.getScorePourcentage();
            charger();
        } catch (Exception e) {
            ToastNotification.showError("Chargement programme", e.getMessage());
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
        // Analyse risque automatique en arriere-plan
        analyserRisqueAbandon();
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

    /**
     * Construit dynamiquement une carte JavaFX pour une tâche.
     * Chaque carte contient :
     *   - Une ligne principale (numéro, titre, flèche, badge état, boutons)
     *   - Un panneau description caché (tiroir) qui s'ouvre au clic
     *
     * @param t La tâche à afficher
     * @return Un VBox représentant la carte complète de la tâche
     */
    private VBox buildTacheRow(Tache t) {

        // Carte principale : fond coloré selon l'état de la tâche
        // bgEtat() retourne : vert clair si réalisée, rouge clair si abandonnée, blanc si en cours
        VBox card = new VBox(0);
        card.setStyle("-fx-background-color: " + bgEtat(t.getEtat())
                + "; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(16,44,89,0.05), 6, 0, 0, 2);");
        card.setCursor(javafx.scene.Cursor.HAND); // curseur main pour indiquer que c'est cliquable

        // ── Ligne principale (toujours visible) ───────────────────────────────
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 20, 14, 20));

        // Numéro d'ordre de la tâche (1. 2. 3. ...)
        Label lblOrdre = new Label(t.getOrdre() + ".");
        lblOrdre.setStyle("-fx-font-weight: bold; -fx-text-fill: #aaa; -fx-min-width: 28; -fx-font-size: 14px;");

        // Titre de la tâche — barré si réalisée (strikethrough)
        Label lblTitre = new Label(t.getTitre());
        String titreStyle = "-fx-font-weight: bold; -fx-text-fill: #102c59; -fx-font-size: 13px;";
        if (t.getEtat() == Etat.realisee)
            // Tâche réalisée → texte gris barré pour indiquer qu'elle est terminée
            titreStyle = "-fx-font-weight: bold; -fx-text-fill: #aaa; -fx-font-size: 13px; -fx-strikethrough: true;";
        lblTitre.setStyle(titreStyle);
        lblTitre.setWrapText(true);
        HBox.setHgrow(lblTitre, Priority.ALWAYS); // le titre prend tout l'espace disponible

        // Flèche indicatrice du tiroir : ▼ = fermé, ▲ = ouvert
        Label lblArrow = new Label("▼");
        lblArrow.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10px;");

        // Badge coloré indiquant l'état : "🔄 En cours", "✅ Realisee", "⛔ Abandonnee"
        Label badge = new Label(labelEtat(t.getEtat()));
        badge.setStyle("-fx-background-color: " + couleurEtat(t.getEtat())
                + "; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 4 12 4 12;"
                + " -fx-font-size: 11px; -fx-font-weight: bold; -fx-min-width: 100; -fx-alignment: CENTER;");

        // Ajouter numéro + titre + flèche dans la ligne
        row.getChildren().addAll(lblOrdre, lblTitre, lblArrow);

        // Badge risque 🚨 — affiché uniquement si la tâche est abandonnée ET identifiée
        // comme dangereuse par RisqueAbandonService (liste tachesARisque)
        if (t.getEtat() == Etat.Abandonner && tachesARisque.contains(t.getTitre())) {
            Label badgeRisque = new Label("🚨");
            badgeRisque.setStyle(
                    "-fx-background-color: #fee2e2; -fx-text-fill: #dc2626;"
                    + " -fx-background-radius: 6; -fx-padding: 3 8 3 8; -fx-font-size: 13px;");
            badgeRisque.setTooltip(new Tooltip("Cette tache met en danger votre objectif"));
            row.getChildren().add(badgeRisque);
        }

        if (!readOnly) {
            // Mode étudiant : afficher les contrôles de modification

            // Spacer invisible qui pousse badge + ComboBox + boutons vers la droite
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            // ComboBox pour changer l'état de la tâche directement dans la liste
            // Quand l'utilisateur change la valeur → changerEtat() est appelé
            ComboBox<String> cbEtat = new ComboBox<>();
            cbEtat.getItems().addAll("encours", "realisee", "Abandonner");
            cbEtat.setValue(t.getEtat() != null ? t.getEtat().getValue() : "encours");
            cbEtat.setPrefWidth(115);
            cbEtat.setStyle("-fx-font-size: 11px; -fx-background-radius: 8;");
            cbEtat.setOnAction(e -> changerEtat(t, cbEtat.getValue())); // déclenche recalcul score

            // Bouton modifier ✎ → ouvre le formulaire inline en mode modification
            Button btnEdit = new Button("✎");
            btnEdit.setStyle(
                    "-fx-background-color: transparent;"
                    + " -fx-text-fill: #f59e0b;"
                    + " -fx-cursor: hand;"
                    + " -fx-background-radius: 0;"
                    + " -fx-padding: 4 8 4 8;"
                    + " -fx-font-size: 16px;"
                    + " -fx-border-color: transparent;");
            btnEdit.setTooltip(new Tooltip("Modifier"));
            btnEdit.setOnAction(e -> ouvrirFormModification(t));

            // Bouton supprimer 🗑 → demande confirmation puis supprime la tâche
            Button btnDel = new Button("🗑");
            btnDel.setStyle(
                    "-fx-background-color: transparent;"
                    + " -fx-text-fill: #dc2626;"
                    + " -fx-cursor: hand;"
                    + " -fx-background-radius: 0;"
                    + " -fx-padding: 4 8 4 8;"
                    + " -fx-font-size: 16px;"
                    + " -fx-border-color: transparent;");
            btnDel.setTooltip(new Tooltip("Supprimer"));
            btnDel.setOnAction(e -> supprimerTache(t));

            row.getChildren().addAll(spacer, badge, cbEtat, btnEdit, btnDel);
        } else {
            // Mode admin (readOnly) : afficher seulement le badge, pas les boutons
            HBox.setHgrow(lblTitre, Priority.ALWAYS);
            row.getChildren().add(badge);
        }

        // ── Panneau description (tiroir, masqué par défaut) ───────────────────
        // Ce panneau est caché au départ et s'affiche quand l'utilisateur clique sur la carte.
        // C'est le principe du "tiroir" ou "accordion" en UI design.
        VBox descPane = new VBox(6);

        // setVisible(false) → invisible
        // setManaged(false) → ne prend PAS de place dans le layout (important !)
        // Sans setManaged(false), il y aurait un espace vide entre les cartes même quand fermé
        descPane.setVisible(false);
        descPane.setManaged(false);
        descPane.setPadding(new Insets(0, 20, 14, 56)); // décalé à droite pour aligner avec le titre
        descPane.setStyle("-fx-border-color: transparent transparent transparent #e2e8f0;"
                + " -fx-border-width: 0 0 0 3;"); // bordure gauche grise (effet visuel de tiroir)

        // Petit label "Description" en gris au-dessus du texte
        Label lblDescLabel = new Label("Description");
        lblDescLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #aaa;");

        // Texte de la description
        // Si la description est null ou vide → afficher un message par défaut
        Label lblDesc = new Label(t.getDescription() != null && !t.getDescription().isBlank()
                ? t.getDescription()
                : "Aucune description pour cette tache.");
        lblDesc.setStyle("-fx-text-fill: #555; -fx-font-size: 12px; -fx-line-spacing: 2;");
        lblDesc.setWrapText(true); // retour à la ligne automatique si le texte est long

        descPane.getChildren().addAll(lblDescLabel, lblDesc);

        // La carte contient : ligne principale (row) + tiroir description (descPane)
        // descPane est invisible au départ, il apparaît sous row au clic
        card.getChildren().addAll(row, descPane);

        // ── Clic sur la carte = toggle tiroir ─────────────────────────────────
        // setOnMouseClicked : écoute les clics sur toute la surface de la carte
        card.setOnMouseClicked(e -> {

            // Ne pas ouvrir le tiroir si l'utilisateur a cliqué sur un bouton ou le ComboBox
            // (sinon le tiroir s'ouvrirait en même temps qu'on clique sur "Modifier" ou "Supprimer")
            if (e.getTarget() instanceof Button || e.getTarget() instanceof ComboBox
                    || e.getTarget() instanceof javafx.scene.control.skin.ComboBoxListViewSkin) return;

            // Lire l'état actuel du tiroir
            boolean ouvert = descPane.isVisible();

            // Toggle : inverser l'état
            // Si ouvert → fermer (visible=false, managed=false)
            // Si fermé  → ouvrir (visible=true, managed=true)
            descPane.setVisible(!ouvert);
            descPane.setManaged(!ouvert);

            // Changer la flèche selon l'état
            // ouvert=true (on ferme) → mettre ▼ (flèche vers le bas = fermé)
            // ouvert=false (on ouvre) → mettre ▲ (flèche vers le haut = ouvert)
            lblArrow.setText(ouvert ? "▼" : "▲");
            lblArrow.setStyle(ouvert
                    ? "-fx-text-fill: #aaa; -fx-font-size: 10px;"                          // fermé → gris
                    : "-fx-text-fill: #102c59; -fx-font-size: 10px; -fx-font-weight: bold;"); // ouvert → bleu gras
        });

        return card; // retourner la carte complète pour l'ajouter dans vboxTaches
    }

    private void changerEtat(Tache t, String nouvelEtat) {
        try {
            tacheService.updateEtat(t.getId(), nouvelEtat);
            t.setEtat(Etat.fromValue(nouvelEtat));
            scorePrecedent = programme.getScorePourcentage();
            int nouveauScore = scoreService.recalculerEtSauvegarder(programme.getId());
            rafraichirScore();
            afficherTaches();

            // Toast selon le nouvel état
            if ("realisee".equals(nouvelEtat)) {
                programme = programmeService.getById(programme.getId());
                Medaille m = programme.getMeilleureMedaille();
                if (m != null && nouveauScore >= 80) {
                    ToastNotification.showMedal(ScoreService.emojiMedaille(m) + " — Score : " + nouveauScore + "%");
                } else if (nouveauScore == 100) {
                    ToastNotification.showMedal("Objectif atteint à 100% ! Félicitations !");
                } else {
                    ToastNotification.showSuccess("Tâche réalisée ✅", "Score mis à jour : " + nouveauScore + "%");
                }
            } else if ("Abandonner".equals(nouvelEtat)) {
                ToastNotification.showWarning("Tâche abandonnée", "\"" + t.getTitre() + "\" marquée comme abandonnée.");
            }

            // Regénérer automatiquement le message de motivation si le score a changé
            if (nouveauScore != scorePrecedent && !readOnly) {
                genererEtAfficherMotivation(true); // toast à chaque nouveau message
            }

        } catch (Exception e) { ToastNotification.showError("Mise à jour état", e.getMessage()); }
    }

    private void supprimerTache(Tache t) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer la tache \"" + t.getTitre() + "\" ?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    int scoreAvant = programme.getScorePourcentage();
                    tacheService.deleteEntity(t); taches.remove(t);
                    int scoreApres = scoreService.recalculerEtSauvegarder(programme.getId());
                    rafraichirScore(); afficherTaches();
                    ToastNotification.showInfo("Tâche supprimée", "\"" + t.getTitre() + "\" a été supprimée.");
                    // Regénérer la motivation si le score a changé
                    if (scoreApres != scoreAvant) {
                        scorePrecedent = scoreAvant;
                        genererEtAfficherMotivation(true);
                    }
                } catch (Exception e) { ToastNotification.showError("Erreur suppression", e.getMessage()); }
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
                            Tache t = new Tache(startOrdre + i, generated.get(i)[0], generated.get(i)[1], Etat.encours, programme.getId());
                            tacheService.addEntity(t); taches.add(t);
                        }
                        scoreService.recalculerEtSauvegarder(programme.getId());
                        Platform.runLater(() -> {
                            try { rafraichirScore(); } catch (Exception ignored) {}
                            afficherTaches();
                            ToastNotification.showSuccess("IA Ollama", generated.size() + " tâches générées et ajoutées !");
                        });
                    } catch (Exception e) { Platform.runLater(() -> ToastNotification.showError("Erreur IA", e.getMessage())); }
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
            ToastNotification.showWarning("Champ requis", "Le titre de la tâche est obligatoire.");
            return;
        }
        String description = taTacheDescription.getText().trim();

        try {
            if (tacheEnEdition == null) {
                Tache t = new Tache(taches.size() + 1, titre, description, Etat.encours, programme.getId());
                tacheService.addEntity(t);
                taches.add(t);
                ToastNotification.showSuccess("Tâche ajoutée", "\"" + titre + "\" ajoutée au programme.");
            } else {
                tacheEnEdition.setTitre(titre);
                tacheEnEdition.setDescription(description);
                tacheService.updateEntity(tacheEnEdition.getId(), tacheEnEdition);
                ToastNotification.showSuccess("Tâche modifiée", "\"" + titre + "\" mise à jour.");
            }
            scoreService.recalculerEtSauvegarder(programme.getId());
            fermerFormTache();
            rafraichirScore();
            afficherTaches();
        } catch (Exception e) {
            ToastNotification.showError("Erreur sauvegarde tâche", e.getMessage());
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
                    int scoreAvant = programme.getScorePourcentage();
                    for (Tache t : taches) { tacheService.updateEtat(t.getId(), Etat.encours.getValue()); t.setEtat(Etat.encours); }
                    int scoreApres = scoreService.recalculerEtSauvegarder(programme.getId());
                    rafraichirScore(); afficherTaches();
                    ToastNotification.showInfo("Réinitialisation", "Toutes les tâches sont remises en cours.");
                    if (scoreApres != scoreAvant) {
                        scorePrecedent = scoreAvant;
                        genererEtAfficherMotivation(true);
                    }
                } catch (Exception e) { ToastNotification.showError("Erreur réinitialisation", e.getMessage()); }
            }
        });
    }

    @FXML void handleSupprimerToutesTaches() {
        if (readOnly) return;
        if (taches == null || taches.isEmpty()) { ToastNotification.showWarning("Aucune tâche", "Il n'y a aucune tâche à supprimer."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer TOUTES les taches (" + taches.size() + ") ? Cette action est irreversible.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmation suppression");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    int scoreAvant = programme.getScorePourcentage();
                    tacheService.deleteByProgramme(programme.getId());
                    taches.clear();
                    int scoreApres = scoreService.recalculerEtSauvegarder(programme.getId());
                    rafraichirScore();
                    afficherTaches();
                    ToastNotification.showInfo("Tâches supprimées", "Toutes les tâches ont été supprimées.");
                    if (scoreApres != scoreAvant) {
                        scorePrecedent = scoreAvant;
                        genererEtAfficherMotivation(true);
                    }
                } catch (Exception e) { ToastNotification.showError("Erreur suppression", e.getMessage()); }
            }
        });
    }

    @FXML void handleRefreshMotivation() {
        // Appel manuel → affiche le toast de confirmation
        genererEtAfficherMotivation(true);
    }

    /**
     * Génère un message de motivation via Ollama et l'affiche dans lblMotivation.
     * Sauvegarde le message en BDD via MotivationService.
     *
     * @param avecToast true = affiche un toast INFO après génération (appel manuel),
     *                  false = silencieux (appel automatique après changement de score)
     */
    private void genererEtAfficherMotivation(boolean avecToast) {
        if (readOnly) return;

        // Afficher le spinner et désactiver le bouton pendant la génération
        if (progressMotivation != null) { progressMotivation.setVisible(true); progressMotivation.setManaged(true); }
        if (btnRefreshMotivation != null) btnRefreshMotivation.setDisable(true);

        if (avecToast) {
            lblMotivation.setText("Génération en cours...");
        } else {
            // Mise à jour silencieuse : rendre le message semi-transparent
            // pour indiquer qu'une mise à jour est en cours sans effacer le texte
            lblMotivation.setOpacity(0.5);
        }

        // ════════════════════════════════════════════════════════════════════
        // POURQUOI CAPTURER CES VALEURS AVANT LE THREAD ?
        //
        // Le thread s'exécute en arrière-plan, APRÈS que la méthode
        // genererEtAfficherMotivation() a terminé. À ce moment-là,
        // les variables "objectif", "programme", etc. pourraient avoir
        // changé (si l'utilisateur navigue vers un autre objectif).
        //
        // En capturant les valeurs dans des variables "final" AVANT de
        // démarrer le thread, on garantit que le thread travaille avec
        // les bonnes données, même si l'état du contrôleur change.
        // ════════════════════════════════════════════════════════════════════
        final String titreObjectif = objectif.getTitre();
        final int    scoreCourant  = programme.getScorePourcentage();
        final int    scorePrec     = scorePrecedent;
        final LocalDate deadline   = objectif.getDatefin();
        final int    programmeId   = programme.getId();

        Thread thread = new Thread(() -> {
            // ════════════════════════════════════════════════════════════════
            // CE CODE S'EXÉCUTE EN ARRIÈRE-PLAN (pas sur le thread UI).
            // Pourquoi ? Ollama peut prendre 10-30 secondes pour répondre.
            // Si on l'appelait sur le thread UI, l'application se figerait
            // complètement pendant ce temps (plus de clics, plus d'animations).
            // ════════════════════════════════════════════════════════════════
            try {
                // Appel à Ollama (peut prendre plusieurs secondes)
                String message = ollamaService.genererMessageMotivant(
                        titreObjectif, scoreCourant, scorePrec, deadline);

                // Sauvegarder le message en BDD
                Motivation m = new Motivation();
                m.setMessagemotivant(message);
                m.setDategeneration(LocalDate.now());
                m.setProgrammeId(programmeId);
                motivationService.addEntity(m);

                // ════════════════════════════════════════════════════════════
                // Platform.runLater() : remet l'exécution sur le thread UI.
                // OBLIGATOIRE pour modifier des composants JavaFX (Label, etc.)
                // depuis un thread background. Sans ça → exception au runtime.
                //
                // Le lambda passé à runLater() sera exécuté dès que le thread
                // UI sera disponible (généralement dans la prochaine frame).
                // ════════════════════════════════════════════════════════════
                Platform.runLater(() -> {
                    lblMotivation.setText(message);   // ← modification UI → doit être sur thread UI
                    lblMotivation.setOpacity(1.0);    // remettre l'opacité normale
                    if (progressMotivation != null) { progressMotivation.setVisible(false); progressMotivation.setManaged(false); }
                    if (btnRefreshMotivation != null) btnRefreshMotivation.setDisable(false);
                    if (avecToast) {
                        ToastNotification.showInfo("💬 Message motivant", "Nouveau message généré par l'IA !");
                    }
                });

            } catch (Exception e) {
                // Ollama indisponible → afficher un message d'erreur sur le thread UI
                Platform.runLater(() -> {
                    lblMotivation.setOpacity(1.0);
                    if (avecToast) {
                        lblMotivation.setText("Ollama indisponible. Vérifiez que le service est démarré.");
                        ToastNotification.showWarning("Ollama indisponible", "Vérifiez que le service est démarré.");
                    }
                    if (progressMotivation != null) { progressMotivation.setVisible(false); progressMotivation.setManaged(false); }
                    if (btnRefreshMotivation != null) btnRefreshMotivation.setDisable(false);
                });
            }
        });
        thread.setDaemon(true); // daemon = le thread s'arrête si l'application se ferme
        thread.start();         // démarrer le thread en arrière-plan
    }

    @FXML void handleExportExcel() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter en Excel");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (.xlsx)", "*.xlsx"));
        fc.setInitialFileName(objectif.getTitre().replaceAll("[^a-zA-Z0-9]", "_") + ".xlsx");
        File file = fc.showSaveDialog(vboxTaches.getScene().getWindow());
        if (file != null) {
            try {
                ExportUtil.exporterExcel(file.getAbsolutePath(), objectif, programme, taches);
                ToastNotification.showSuccess("Export Excel réussi", file.getName() + " sauvegardé.");
                ExportUtil.ouvrirFichier(file.getAbsolutePath()); // ouvrir automatiquement
            }
            catch (Exception e) { ToastNotification.showError("Erreur export Excel", e.getMessage()); }
        }
    }

    @FXML void handleExportWord() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter en Word");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Word (.docx)", "*.docx"));
        fc.setInitialFileName(objectif.getTitre().replaceAll("[^a-zA-Z0-9]", "_") + ".docx");
        File file = fc.showSaveDialog(vboxTaches.getScene().getWindow());
        if (file != null) {
            try {
                ExportUtil.exporterWord(file.getAbsolutePath(), objectif, programme, taches);
                ToastNotification.showSuccess("Export Word réussi", file.getName() + " sauvegardé.");
                ExportUtil.ouvrirFichier(file.getAbsolutePath()); // ouvrir automatiquement
            }
            catch (Exception e) { ToastNotification.showError("Erreur export Word", e.getMessage()); }
        }
    }

    // ── Resume IA ─────────────────────────────────────────────────────────────

    @FXML void handleGenererRapport() {
        if (cardResume == null) return;
        if (progressResume != null) { progressResume.setVisible(true); progressResume.setManaged(true); }
        if (btnGenererRapport != null) btnGenererRapport.setDisable(true);
        if (lblResumeTexte != null) lblResumeTexte.setText("Generation en cours...");

        final String titre = objectif.getTitre();
        final int score = programme.getScorePourcentage();
        final List<Tache> snapshot = List.copyOf(taches);

        Task<ResumeService.ResumeResultat> task = new Task<>() {
            @Override
            protected ResumeService.ResumeResultat call() {
                return resumeService.genererResume(titre, score, snapshot);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            afficherResume(task.getValue());
            if (progressResume != null) { progressResume.setVisible(false); progressResume.setManaged(false); }
            if (btnGenererRapport != null) btnGenererRapport.setDisable(false);
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            if (lblResumeTexte != null) lblResumeTexte.setText("Ollama indisponible.");
            if (progressResume != null) { progressResume.setVisible(false); progressResume.setManaged(false); }
            if (btnGenererRapport != null) btnGenererRapport.setDisable(false);
            ToastNotification.showWarning("Ollama indisponible", "Impossible de générer le rapport IA.");
        }));
        new Thread(task).start();
    }

    private void afficherResume(ResumeService.ResumeResultat r) {
        if (cardResume == null) return;
        if (lblResumeTexte != null)        lblResumeTexte.setText(r.resume);
        if (lblResumePointPositif != null) lblResumePointPositif.setText("✅ " + r.pointPositif);
        if (lblResumeConseil != null)      lblResumeConseil.setText("💡 " + r.conseil);
        cardResume.setVisible(true);
        cardResume.setManaged(true);
    }

    // ── Risque Abandon ────────────────────────────────────────────────────────

    private void analyserRisqueAbandon() {
        if (taches == null || taches.isEmpty()) return;
        final List<Tache> snapshot = List.copyOf(taches);
        final int score = programme.getScorePourcentage();
        final LocalDate datefin = objectif.getDatefin();

        Task<RisqueAbandonService.RisqueResultat> task = new Task<>() {
            @Override
            protected RisqueAbandonService.RisqueResultat call() {
                return risqueService.analyserRisque(snapshot, score, datefin);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            RisqueAbandonService.RisqueResultat r = task.getValue();
            tachesARisque.clear();
            tachesARisque.addAll(r.tachesARelancer);
            afficherTaches();
            if (r.risque && !r.tachesARelancer.isEmpty()) {
                int nb = r.tachesARelancer.size();
                lblAlerte.setText("⚠️ " + nb + " tache(s) abandonnee(s) mettent en danger votre objectif. " + r.conseil);
                lblAlerte.setVisible(true);
                lblAlerte.setManaged(true);
                ToastNotification.showDeadline(nb + " tâche(s) abandonnée(s) menacent votre objectif !");
            }
        }));
        new Thread(task).start();
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
    private String bgEtat(Etat e) {
        if (e == null) return "white";
        return switch (e) {
            case realisee   -> "#f0fdf4";
            case Abandonner -> "#fff5f5";
            case encours    -> "#fafbff";
        };
    }

    private String couleurEtat(Etat e) {
        if (e == null) return "#888";
        return switch (e) {
            case realisee   -> "#198754";
            case Abandonner -> "#dc2626";
            case encours    -> "#f59e0b";
        };
    }

    private String labelEtat(Etat e) {
        if (e == null) return "En cours";
        return switch (e) {
            case realisee   -> "✅ Realisee";
            case Abandonner -> "⛔ Abandonnee";
            case encours    -> "🔄 En cours";
        };
    }

    private static void hide(javafx.scene.Node n) {
        if (n != null) { n.setVisible(false); n.setManaged(false); }
    }
}
