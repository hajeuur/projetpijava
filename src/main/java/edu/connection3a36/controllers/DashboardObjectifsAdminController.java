package edu.connection3a36.controllers;

import edu.connection3a36.entities.Utilisateur;
import edu.connection3a36.services.*;
import edu.connection3a36.tools.AlertUtil;
import edu.mentorai.entities.Objectif;
import edu.mentorai.entities.Programme;
import edu.mentorai.entities.Statutobj;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class DashboardObjectifsAdminController {

    // ── Stats globales ────────────────────────────────────────────────────────
    @FXML private Label totalObjectifsLabel;
    @FXML private Label atteintsLabel;
    @FXML private Label enCoursLabel;
    @FXML private Label tauxLabel;

    // ── Liste utilisateurs ────────────────────────────────────────────────────
    @FXML private VBox vboxUtilisateurs;
    @FXML private TextField tfRechercheUser;

    private final ObjectifService objectifService = new ObjectifService();
    private final ProgrammeService programmeService = new ProgrammeService();
    private final UtilisateurService utilisateurService = new UtilisateurService();

    private List<Utilisateur> tousUtilisateurs;

    @FXML
    public void initialize() {
        charger();
    }

    @FXML
    void handleRefresh() { charger(); }

    private void charger() {
        try {
            // Stats globales
            List<Objectif> tousObjectifs = objectifService.getData();
            long atteints = tousObjectifs.stream().filter(o -> o.getStatut() == Statutobj.Atteint).count();
            long encours  = tousObjectifs.stream().filter(o -> o.getStatut() == Statutobj.EnCours).count();
            int total = tousObjectifs.size();
            double taux = total > 0 ? (double) atteints / total * 100 : 0;

            totalObjectifsLabel.setText(String.valueOf(total));
            atteintsLabel.setText(String.valueOf(atteints));
            enCoursLabel.setText(String.valueOf(encours));
            tauxLabel.setText(String.format("%.0f%%", taux));

            // Liste utilisateurs
            tousUtilisateurs = utilisateurService.getData();
            afficherUtilisateurs(tousUtilisateurs);

        } catch (Exception e) {
            AlertUtil.showError("Erreur chargement dashboard : " + e.getMessage());
        }
    }

    @FXML
    void handleRechercheUser() {
        if (tousUtilisateurs == null) return;
        String q = tfRechercheUser.getText().toLowerCase().trim();
        List<Utilisateur> filtres = tousUtilisateurs.stream()
                .filter(u -> q.isEmpty()
                        || u.getNom().toLowerCase().contains(q)
                        || u.getPrenom().toLowerCase().contains(q)
                        || u.getEmail().toLowerCase().contains(q))
                .toList();
        afficherUtilisateurs(filtres);
    }

    private void afficherUtilisateurs(List<Utilisateur> utilisateurs) {
        vboxUtilisateurs.getChildren().clear();
        if (utilisateurs.isEmpty()) {
            Label lbl = new Label("Aucun utilisateur trouve.");
            lbl.setStyle("-fx-text-fill: #888;");
            vboxUtilisateurs.getChildren().add(lbl);
            return;
        }
        for (Utilisateur u : utilisateurs) {
            vboxUtilisateurs.getChildren().add(buildUserRow(u));
        }
    }

    private HBox buildUserRow(Utilisateur u) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 20, 14, 20));
        row.setStyle("-fx-background-color: white; -fx-background-radius: 12; "
                + "-fx-effect: dropshadow(gaussian, rgba(16,44,89,0.07), 8, 0, 0, 2);");

        // Avatar
        Label avatar = new Label(u.getPrenom().substring(0, 1).toUpperCase());
        avatar.setStyle("-fx-background-color: #102c59; -fx-text-fill: white; -fx-font-weight: bold; "
                + "-fx-font-size: 16px; -fx-min-width: 42; -fx-min-height: 42; "
                + "-fx-background-radius: 21; -fx-alignment: CENTER;");

        // Infos utilisateur
        VBox infos = new VBox(3);
        Label lblNom = new Label(u.getPrenom() + " " + u.getNom());
        lblNom.setStyle("-fx-font-weight: bold; -fx-text-fill: #102c59; -fx-font-size: 14px;");
        Label lblEmail = new Label(u.getEmail());
        lblEmail.setStyle("-fx-text-fill: #888; -fx-font-size: 11px;");
        Label lblRole = new Label(u.getRole() != null ? u.getRole() : "—");
        lblRole.setStyle("-fx-background-color: #f0f4f8; -fx-text-fill: #102c59; "
                + "-fx-background-radius: 50; -fx-padding: 2 8 2 8; -fx-font-size: 10px; -fx-font-weight: bold;");
        infos.getChildren().addAll(lblNom, lblEmail, lblRole);
        HBox.setHgrow(infos, Priority.ALWAYS);

        // Compteur objectifs
        VBox stats = new VBox(3);
        stats.setAlignment(Pos.CENTER);
        try {
            List<Objectif> objUser = objectifService.getByUtilisateur(u.getId());
            long atteints = objUser.stream().filter(o -> o.getStatut() == Statutobj.Atteint).count();
            Label lblCount = new Label(String.valueOf(objUser.size()));
            lblCount.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #102c59;");
            Label lblCountLabel = new Label("objectif(s)");
            lblCountLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 10px;");
            Label lblAtteints = new Label(atteints + " atteint(s)");
            lblAtteints.setStyle("-fx-text-fill: #198754; -fx-font-size: 10px; -fx-font-weight: bold;");
            stats.getChildren().addAll(lblCount, lblCountLabel, lblAtteints);
        } catch (Exception ignored) {
            stats.getChildren().add(new Label("—"));
        }

        // Bouton voir objectifs
        Button btnVoir = new Button("Voir les objectifs");
        btnVoir.setStyle("-fx-background-color: #102c59; -fx-text-fill: white; -fx-font-weight: bold; "
                + "-fx-background-radius: 50; -fx-padding: 9 20 9 20; -fx-cursor: hand;");
        btnVoir.setOnAction(e -> ouvrirObjectifsUtilisateur(u));

        row.getChildren().addAll(avatar, infos, stats, btnVoir);
        return row;
    }

    private void ouvrirObjectifsUtilisateur(Utilisateur u) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AdminObjectifsUser.fxml"));
            Parent view = loader.load();
            AdminObjectifsUserController ctrl = loader.getController();
            ctrl.setUtilisateur(u);
            MainController.getInstance().loadInContentArea(view);
        } catch (Exception e) {
            AlertUtil.showError("Erreur ouverture objectifs : " + e.getMessage());
        }
    }
}
