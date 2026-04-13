package edu.connection3a36.controllers;

import edu.connection3a36.entities.Utilisateur;
import edu.connection3a36.tools.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Contrôleur principal — gère la navigation, les RÔLES et le mode FRONT/BACK.
 *
 * Architecture de layout :
 * - Mode FRONT  → Top Header (frontHeader visible, backSidebar masquée)
 * - Mode BACK   → Sidebar gauche (backSidebar visible, frontHeader masquée)
 */
public class MainController {

    // ── ContentArea ──────────────────────────────────────────────────────────
    @FXML private StackPane contentArea;

    // ── Top Header (FRONT) ────────────────────────────────────────────────────
    @FXML private HBox frontHeader;
    @FXML private HBox frontNavBox;
    @FXML private Label lblUser;

    // Boutons header FRONT
    @FXML private Button btnDashboardEnseignant;
    @FXML private Button btnDashboardAdmin;
    @FXML private Button btnPlanActions;
    @FXML private Button btnArticles;
    @FXML private Button btnAIPedagogique;
    @FXML private Button btnAIDecisionnel;
    @FXML private Button btnSwitchBack;
    @FXML private Button btnNotifications;

    // ── Sidebar (BACK) ────────────────────────────────────────────────────────
    @FXML private VBox backSidebar;
    @FXML private VBox boxAdmin;
    @FXML private VBox boxBackAdmin;
    @FXML private VBox boxSwitcher;
    @FXML private Button btnUtilisateurs;
    @FXML private Button btnCategories;
    @FXML private Label lblUserBack;

    // ── État interne ──────────────────────────────────────────────────────────
    private Button activeHeaderBtn;
    private Button activeSidebarBtn;
    private String userRole = "";

    // ─────────────────────────────────────────────────────────────────────────
    // INITIALISATION
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        Utilisateur user = SessionManager.getCurrentUser();
        if (user == null) return;

        String displayName = user.getPrenom() + " (" + user.getRole() + ")";
        if (lblUser != null)     lblUser.setText(displayName);
        if (lblUserBack != null) lblUserBack.setText(displayName);

        userRole = user.getRole() != null ? user.getRole().toUpperCase() : "";

        configureByRole();
    }

    /**
     * Configure la visibilité des sections et l'écran de démarrage selon le rôle.
     */
    private void configureByRole() {
        if (isSuperAdmin()) {
            // SUPERADMIN / ADMINM → FRONT en premier (dashboard stratégique)
            showFrontMode();
            show(btnSwitchBack);       // peut basculer vers back-office
            hide(btnDashboardEnseignant);
            show(btnDashboardAdmin);
            hide(btnAIPedagogique);
            show(btnAIDecisionnel);
            show(btnNotifications);
            
            updateNotifications();
            
            // Sidebar back : tout activé
            show(boxAdmin);
            show(boxBackAdmin);
            show(boxSwitcher);

            showDashboardAdmin();

        } else if (isAdmin()) {
            // Admin classique -> Accès Frontend Stratégique + Backoffice
            showFrontMode();
            show(btnSwitchBack);
            hide(btnDashboardEnseignant);
            show(btnDashboardAdmin);
            hide(btnAIPedagogique);
            show(btnAIDecisionnel);
            show(btnNotifications);
            updateNotifications();

            // Partage admin
            show(boxAdmin);
            hide(boxBackAdmin);
            show(boxSwitcher);

            showDashboardAdmin();

        } else if (isEnseignant()) {
            // Enseignant → FRONT uniquement (dashboard pédagogique + IA)
            showFrontMode();
            hide(btnSwitchBack);
            show(btnDashboardEnseignant);
            hide(btnDashboardAdmin);
            show(btnAIPedagogique);
            hide(btnAIDecisionnel);
            // Sidebar masquée
            hide(boxAdmin);
            hide(boxBackAdmin);
            hide(boxSwitcher);

            showDashboardEnseignant();

        } else {
            // Visiteur / étudiant → contenu vide
            showFrontMode();
            hide(btnSwitchBack);
            hide(btnDashboardEnseignant); hide(btnDashboardAdmin);
            hide(btnPlanActions);
            hide(btnArticles);
            hide(btnAIPedagogique); hide(btnAIDecisionnel);
            contentArea.getChildren().clear();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BASCULEURS FRONT / BACK
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    void switchToFront() {
        SessionManager.setFrontMode(true);
        showFrontMode();
        showAIDecisionnel();
    }

    @FXML
    void switchToBack() {
        SessionManager.setFrontMode(false);
        showBackMode();
        if (isSuperAdmin()) {
            show(boxAdmin);
            show(boxBackAdmin);
        }
        showCategories();
    }

    /** Affiche le header TOP, masque la sidebar. */
    private void showFrontMode() {
        show(frontHeader);
        hide(backSidebar);
    }

    /** Affiche la sidebar LEFT, masque le header. */
    private void showBackMode() {
        hide(frontHeader);
        show(backSidebar);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NAVIGATION
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    void showDashboardAdmin() {
        loadView("/fxml/DashboardAdmin.fxml");
        setActiveBtn(btnDashboardAdmin);
    }

    @FXML
    void showDashboardEnseignant() {
        loadView("/fxml/DashboardEnseignant.fxml");
        setActiveBtn(btnDashboardEnseignant);
    }

    @FXML
    void showPlanActions() {
        // Vue FRONT = cartes, vue BACK = tableau
        if (frontHeader != null && frontHeader.isVisible()) {
            loadView("/fxml/PlanActionsListFront.fxml");
        } else {
            loadView("/fxml/PlanActionsList.fxml");
        }
        setActiveBtn(btnPlanActions);
    }

    @FXML
    void showArticles() {
        if (frontHeader != null && frontHeader.isVisible()) {
            loadView("/fxml/ArticleListFront.fxml");
        } else {
            loadView("/fxml/ArticleList.fxml");
        }
        setActiveBtn(btnArticles);
    }

    @FXML
    void showPreferences() {
        loadView("/fxml/Preferences.fxml");
    }

    private void updateNotifications() {
        if (btnNotifications == null) return;
        try {
            edu.connection3a36.services.PlanActionsService ps = new edu.connection3a36.services.PlanActionsService();
            int count = ps.getRecentFeedbacks().size();
            btnNotifications.setText("🔔 (" + count + ")");
            if (count > 0) {
                btnNotifications.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            } else {
                btnNotifications.setStyle("");
            }
        } catch (Exception e) {}
    }

    @FXML
    void handleNotifications() {
        try {
            edu.connection3a36.services.PlanActionsService ps = new edu.connection3a36.services.PlanActionsService();
            int count = ps.getRecentFeedbacks().size();
            if (count > 0) {
                edu.connection3a36.tools.AlertUtil.showSuccess("Vous avez " + count + " nouveaux feedbacks à traiter dans votre dashboard.");
                if (isSuperAdmin()) {
                    showDashboardAdmin();
                }
            } else {
                edu.connection3a36.tools.AlertUtil.showSuccess("Aucune nouvelle notification.");
            }
        } catch (Exception e) {}
    }

    @FXML
    void showCategories() {
        loadView("/fxml/CategorieList.fxml");
        setActiveBtn(btnCategories);
    }

    @FXML
    void showUtilisateurs() {
        loadView("/fxml/UtilisateurList.fxml");
        setActiveBtn(btnUtilisateurs);
    }

    @FXML
    void showAIPedagogique() {
        loadView("/fxml/AIPedagogique.fxml");
        setActiveBtn(btnAIPedagogique);
    }

    @FXML
    void showAIDecisionnel() {
        loadView("/fxml/AIDecisionnel.fxml");
        setActiveBtn(btnAIDecisionnel);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DÉCONNEXION
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    void handleLogout() {
        SessionManager.logout();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) contentArea.getScene().getWindow();
            Scene sc = new Scene(root, 1200, 750);
            sc.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            stage.setTitle("MentorAI — Connexion");
            stage.setScene(sc);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CHARGEMENT DE VUE
    // ─────────────────────────────────────────────────────────────────────────

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement vue: " + fxmlPath);
            e.printStackTrace();
            showErrorView(fxmlPath, e);
        }
    }

    private void showErrorView(String fxmlPath, Exception e) {
        VBox errorBox = new VBox(15);
        errorBox.setAlignment(Pos.CENTER);

        Label icon  = new Label("⚠️");
        icon.setStyle("-fx-font-size: 52px;");

        Label title = new Label("Erreur de connexion à la base de données");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");

        Label message = new Label("Vérifiez que MySQL est démarré dans XAMPP/WAMP\net que la base 'mentorai' existe.");
        message.setStyle("-fx-font-size: 14px; -fx-text-fill: #8e92a9; -fx-text-alignment: center;");
        message.setWrapText(true);

        Label detail = new Label("Détail : " + e.getMessage());
        detail.setStyle("-fx-font-size: 11px; -fx-text-fill: #bdc3c7;");
        detail.setWrapText(true);

        Button retryBtn = new Button("🔄 Réessayer");
        retryBtn.getStyleClass().add("btn-primary");
        retryBtn.setOnAction(ev -> loadView(fxmlPath));

        errorBox.getChildren().addAll(icon, title, message, detail, retryBtn);
        contentArea.getChildren().setAll(errorBox);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS UI
    // ─────────────────────────────────────────────────────────────────────────

    /** Gère le style actif pour les boutons du header FRONT. */
    private void setActiveBtn(Button btn) {
        // Header buttons
        Button[] headerBtns = {
            btnDashboardEnseignant, btnDashboardAdmin,
            btnPlanActions, btnArticles,
            btnAIPedagogique, btnAIDecisionnel
        };
        for (Button b : headerBtns) {
            if (b != null) {
                b.getStyleClass().remove("header-nav-btn-active");
                b.getStyleClass().remove("sidebar-btn-active");
            }
        }

        // Sidebar buttons
        Button[] sidebarBtns = {btnUtilisateurs, btnCategories};
        for (Button b : sidebarBtns) {
            if (b != null) {
                b.getStyleClass().remove("sidebar-btn-active");
            }
        }

        if (btn != null) {
            if (frontHeader != null && frontHeader.isVisible()) {
                btn.getStyleClass().add("header-nav-btn-active");
            } else {
                btn.getStyleClass().add("sidebar-btn-active");
            }
        }
        activeHeaderBtn = btn;
    }

    // ── Role checks ───────────────────────────────────────────────────────────
    private boolean isSuperAdmin() {
        return userRole.equals("ADMINM")
            || userRole.contains("SUPERADMIN")
            || userRole.contains("SUPER_ADMIN");
    }

    private boolean isAdmin() {
        return userRole.contains("ADMIN") && !isSuperAdmin();
    }

    private boolean isEnseignant() {
        return userRole.contains("ENSEIGNANT");
    }

    // ── Visibility helpers ────────────────────────────────────────────────────
    private static void show(javafx.scene.Node n) {
        if (n != null) { n.setVisible(true);  n.setManaged(true);  }
    }

    private static void hide(javafx.scene.Node n) {
        if (n != null) { n.setVisible(false); n.setManaged(false); }
    }
}
