package edu.connection3a36.controllers;

import edu.connection3a36.entities.Utilisateur;
import edu.connection3a36.services.*;
import edu.connection3a36.tools.AlertUtil;
import edu.connection3a36.entities.Objectif;
import edu.connection3a36.entities.Programme;
import edu.connection3a36.entities.Statutobj;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Contrôleur de la vue admin : affiche les objectifs d'un utilisateur spécifique.
 * L'admin peut consulter les programmes et tâches en mode lecture seule (readOnly).
 * Accessible depuis DashboardObjectifsAdminController via le bouton "Voir les objectifs".
 */
public class AdminObjectifsUserController {

    // ── Composants FXML ───────────────────────────────────────────────────────
    @FXML private Label lblUserTitre;    // nom et prénom de l'utilisateur
    @FXML private Label lblUserSub;      // email et rôle de l'utilisateur
    @FXML private VBox vboxObjectifs;    // conteneur des cartes d'objectifs
    @FXML private Label lblStats;        // résumé statistique (total, atteints, etc.)

    /** L'utilisateur dont on affiche les objectifs */
    private Utilisateur utilisateur;

    // ── Services utilisés ─────────────────────────────────────────────────────
    private final ObjectifService objectifService = new ObjectifService();
    private final ProgrammeService programmeService = new ProgrammeService();

    /**
     * Point d'entrée : reçoit l'utilisateur sélectionné depuis le dashboard admin.
     * Affiche son nom/email dans l'en-tête et charge ses objectifs.
     */
    public void setUtilisateur(Utilisateur u) {
        this.utilisateur = u;
        lblUserTitre.setText(u.getPrenom() + " " + u.getNom());
        lblUserSub.setText(u.getEmail() + "  |  " + (u.getRole() != null ? u.getRole() : "—"));
        charger(); // charger les objectifs de cet utilisateur
    }

    /**
     * Charge les objectifs de l'utilisateur depuis la BDD.
     * Calcule les statistiques (atteints, en cours, abandonnés) et affiche les cartes.
     */
    private void charger() {
        try {
            List<Objectif> objectifs = objectifService.getByUtilisateur(utilisateur.getId());
            // Calculer les statistiques par statut
            long atteints   = objectifs.stream().filter(o -> o.getStatut() == Statutobj.Atteint).count();
            long encours    = objectifs.stream().filter(o -> o.getStatut() == Statutobj.EnCours).count();
            long abandonnes = objectifs.stream().filter(o -> o.getStatut() == Statutobj.Abandonner).count();
            lblStats.setText(objectifs.size() + " objectif(s)  |  "
                    + atteints + " atteint(s)  |  "
                    + encours + " en cours  |  "
                    + abandonnes + " abandonne(s)");

            vboxObjectifs.getChildren().clear();
            if (objectifs.isEmpty()) {
                Label lbl = new Label("Cet utilisateur n a aucun objectif.");
                lbl.setStyle("-fx-text-fill: #888; -fx-font-size: 13px;");
                vboxObjectifs.getChildren().add(lbl);
                return;
            }
            // Construire une carte pour chaque objectif
            for (Objectif o : objectifs) vboxObjectifs.getChildren().add(buildCard(o));
        } catch (Exception e) {
            AlertUtil.showError("Erreur chargement : " + e.getMessage());
        }
    }

    /**
     * Construit dynamiquement une carte JavaFX pour un objectif.
     * Affiche : titre, statut, description, dates, barre de progression, médaille.
     * Contient un bouton pour ouvrir le programme en mode lecture seule.
     */
    private VBox buildCard(Objectif o) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 20 25 20 25; "
                + "-fx-effect: dropshadow(gaussian, rgba(16,44,89,0.08), 10, 0, 0, 3);");

        // En-tête : titre de l'objectif + badge de statut coloré
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label lblTitre = new Label(o.getTitre());
        lblTitre.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #102c59;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label lblStatut = new Label(o.getStatut() != null ? o.getStatut().getValue() : "—");
        lblStatut.setStyle("-fx-background-color: " + couleurStatut(o.getStatut())
                + "; -fx-text-fill: white; -fx-background-radius: 50; -fx-padding: 4 12 4 12; "
                + "-fx-font-size: 11px; -fx-font-weight: bold;");
        header.getChildren().addAll(lblTitre, spacer, lblStatut);

        // Description (affichée seulement si elle existe)
        if (o.getDescription() != null && !o.getDescription().isBlank()) {
            Label lblDesc = new Label(o.getDescription());
            lblDesc.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
            lblDesc.setWrapText(true);
            card.getChildren().add(lblDesc);
        }

        // Dates de début et deadline
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String debut = o.getDatedebut() != null ? o.getDatedebut().format(fmt) : "—";
        String fin   = o.getDatefin()   != null ? o.getDatefin().format(fmt)   : "—";
        Label lblDates = new Label("Debut : " + debut + "   |   Deadline : " + fin);
        lblDates.setStyle("-fx-text-fill: #aaa; -fx-font-size: 11px;");

        // Barre de progression + score + médaille
        HBox scoreBox = new HBox(10);
        scoreBox.setAlignment(Pos.CENTER_LEFT);
        try {
            Programme prog = o.getProgramme() != null && o.getProgramme().getId() > 0
                    ? programmeService.getById(o.getProgramme().getId()) : null;
            if (prog != null) {
                ProgressBar pb = new ProgressBar(prog.getScorePourcentage() / 100.0);
                pb.setPrefWidth(160);
                // La couleur de la barre dépend de la médaille obtenue
                pb.setStyle("-fx-accent: " + ScoreService.couleurMedaille(prog.getMeilleureMedaille()) + ";");
                Label lblScore = new Label(prog.getScorePourcentage() + "%");
                lblScore.setStyle("-fx-font-weight: bold; -fx-text-fill: #102c59;");
                Label lblMedaille = new Label(ScoreService.emojiMedaille(prog.getMeilleureMedaille()));
                scoreBox.getChildren().addAll(pb, lblScore, lblMedaille);
            }
        } catch (Exception ignored) {}

        // Bouton pour ouvrir le programme en mode lecture seule (admin ne peut pas modifier)
        HBox btnBox = new HBox();
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        Button btnProg = new Button("Voir le programme et les taches");
        btnProg.setStyle("-fx-background-color: #102c59; -fx-text-fill: white; -fx-font-weight: bold; "
                + "-fx-background-radius: 50; -fx-padding: 9 20 9 20; -fx-cursor: hand;");
        btnProg.setOnAction(e -> ouvrirProgramme(o));
        btnBox.getChildren().add(btnProg);

        card.getChildren().addAll(header, lblDates, scoreBox, new Separator(), btnBox);
        return card;
    }

    /**
     * Ouvre ProgrammeDetail en mode readOnly=true pour l'admin.
     * L'admin peut voir les tâches mais ne peut pas les modifier.
     */
    private void ouvrirProgramme(Objectif o) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ProgrammeDetail.fxml"));
            Parent view = loader.load();
            ProgrammeDetailController ctrl = loader.getController();
            ctrl.setObjectif(o, true); // true = mode lecture seule pour l'admin
            MainController.getInstance().loadInContentArea(view);
        } catch (Exception e) {
            AlertUtil.showError("Erreur ouverture programme : " + e.getMessage());
        }
    }

    /** Retourne au dashboard admin des objectifs */
    @FXML
    void handleRetour() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/DashboardObjectifsAdmin.fxml"));
            Parent view = loader.load();
            MainController.getInstance().loadInContentArea(view);
        } catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Retourne la couleur CSS du badge de statut.
     *   EnCours    → orange
     *   Atteint    → vert
     *   Abandonner → rouge
     */
    private String couleurStatut(Statutobj s) {
        if (s == null) return "#888";
        return switch (s) {
            case EnCours    -> "#ffc107";
            case Atteint    -> "#198754";
            case Abandonner -> "#d52e28";
        };
    }
}
