package edu.connection3a36.controllers;

import edu.connection3a36.entities.Utilisateur;
import edu.connection3a36.services.AccessControlService;
import edu.connection3a36.services.UserPreferencesService;
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
import javafx.scene.layout.Priority;
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

    // ── Boutons header FRONT
    @FXML private Button btnDashboardEnseignant;
    @FXML private Button btnDashboardAdmin;
    @FXML private Button btnPlanActions;
    @FXML private Button btnArticles;
    @FXML private Button btnAIPedagogique;
    @FXML private Button btnAIDecisionnel;
    @FXML private Button btnSwitchBack;
    @FXML private Button btnNotifications;
    @FXML private Button btnParcours;
    @FXML private Button btnProjets;
    @FXML private Button btnGamesHub;
    @FXML private Button btnIoT;
    @FXML private Button btnAtRisk;
    @FXML private Button btnBackParcours;
    @FXML private Button btnBackProjets;
    @FXML private Button btnNotifSidebar; // Bouton clochette dans la sidebar
    @FXML private Button btnBackFeedbacks;
    @FXML private Button btnMesFeedbacks;
    @FXML private Button btnObjectifs;
    @FXML private Button btnDashboardObjectifs;

    // ── MentorAI features (sidebar BACK) ──────────────────────────────────────
    @FXML private VBox    boxMentorAI;
    @FXML private Button  btnHumeur;
    @FXML private Button  btnPlanning;
    @FXML private Button  btnCarnet;

    // ── MentorAI features (header FRONT) ──────────────────────────────────────
    @FXML private Button  btnHeaderHumeur;
    @FXML private Button  btnHeaderPlanning;
    @FXML private Button  btnHeaderCarnet;

    // ── Sidebar (BACK) ────────────────────────────────────────────────────────
    @FXML private VBox backSidebar;
    @FXML private VBox boxAdmin;
    @FXML private VBox boxBackAdmin;
    @FXML private VBox boxSwitcher;
    @FXML private Button btnUtilisateurs;
    @FXML private Button btnCategories;
    @FXML private Button btnGestionUtilisateursHejer;
    @FXML private Button btnMonProfil;
    @FXML private Label lblUserBack;

    // ── État interne ──────────────────────────────────────────────────────────
    private static MainController instance;
    private Button activeHeaderBtn;
    private Button activeSidebarBtn;
    private String userRole = "";
    private final UserPreferencesService prefsService = new UserPreferencesService();
    private final AccessControlService acl = new AccessControlService();

    public static MainController getInstance() {
        return instance;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INITIALISATION
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        instance = this;
        Utilisateur user = SessionManager.getCurrentUser();
        if (user == null) return;

        String displayName = user.getPrenom() + " (" + user.getRole() + ")";
        if (lblUser != null)     lblUser.setText(displayName);
        if (lblUserBack != null) lblUserBack.setText(displayName);

        userRole = user.getRole() != null ? user.getRole().toUpperCase() : "";

        configureByRole();
        // Charger le badge de notifications en arrière-plan
        updateNotifications();
        applyGlobalPreferences();
    }

    /**
     * Configure la visibilité des sections et l'écran de démarrage selon le rôle.
     */
    private void configureByRole() {
        if (isSuperAdmin()) {
            // SUPERADMIN (admin@esprit.tn) → BACK-Office complet au démarrage + accès FRONT
            showBackMode();
            show(boxAdmin);
            show(boxBackAdmin);
            show(boxSwitcher); // ADMINM peut basculer vers le FRONT
            show(btnNotifSidebar); // Bouton notifications visible

            // Tous les boutons CRUD visibles
            for (javafx.scene.Node n : boxBackAdmin.getChildren()) show(n);

            showCategories();

        } else if (isAdmin()) {
            // ADMIN SIMPLE (admin@gmail.com) → Back-Office Parcours + Projets uniquement
            showBackMode();
            hide(boxAdmin);
            show(boxBackAdmin);
            hide(boxSwitcher);

            // Cacher tout dans boxBackAdmin sauf Parcours et Projets
            for (javafx.scene.Node n : boxBackAdmin.getChildren()) {
                if (n instanceof Button b) {
                    if (b == btnBackParcours || b == btnBackProjets || b == btnBackFeedbacks || b == btnGestionUtilisateursHejer) show(b);
                    else hide(b);
                } else {
                    hide(n); // Séparateurs, labels
                }
            }

            // MentorAI visible en sidebar pour admin
            if (boxMentorAI != null) show(boxMentorAI);

            showBackParcours();

        } else if (isEnseignant()) {
            // Enseignant → FRONT uniquement (dashboard pédagogique + IA + Plans + Articles)
            showFrontMode();
            hide(btnDashboardAdmin);
            hide(btnSwitchBack);
            hide(btnAIDecisionnel);
            hide(btnParcours);
            hide(btnProjets);
            hide(btnGamesHub); // Cacher Hub Intervention
            hide(boxAdmin);
            hide(boxBackAdmin);
            hide(boxSwitcher);
            show(btnDashboardEnseignant);
            show(btnAIPedagogique);
            show(btnPlanActions);
            show(btnArticles);
            if (btnMonProfil != null) show(btnMonProfil);
            
            // MentorAI dans le header FRONT
            if (btnHeaderHumeur != null) show(btnHeaderHumeur);
            if (btnHeaderPlanning != null) show(btnHeaderPlanning);
            if (btnHeaderCarnet != null) show(btnHeaderCarnet);

            showDashboardEnseignant();

        } else if (isEtudiant()) {
            // Étudiant → FRONT uniquement (Parcours + Projets)
            showFrontMode();
            hide(btnSwitchBack);
            hide(btnDashboardEnseignant); hide(btnDashboardAdmin);
            hide(btnPlanActions);
            hide(btnArticles);
            hide(btnAIPedagogique); hide(btnAIDecisionnel);
            
            show(btnParcours);
            show(btnProjets);
            show(btnGamesHub);
            if (btnMesFeedbacks != null) show(btnMesFeedbacks);
            if (btnObjectifs != null) show(btnObjectifs);
            if (btnHeaderCarnet != null) show(btnHeaderCarnet);
            if (btnMonProfil != null) show(btnMonProfil);
            
            showParcours();

        } else {
            // Visiteur → contenu vide
            showFrontMode();
            hide(btnSwitchBack);
            hide(btnDashboardEnseignant); hide(btnDashboardAdmin);
            hide(btnPlanActions);
            hide(btnArticles);
            hide(btnAIPedagogique); hide(btnAIDecisionnel);
            hide(btnParcours); hide(btnProjets); hide(btnGamesHub);
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
        configureFrontButtons();
    }

    /** Configure les boutons visibles dans le header FRONT selon le rôle. */
    private void configureFrontButtons() {
        // Cacher tout d'abord
        hide(btnDashboardEnseignant); hide(btnDashboardAdmin);
        hide(btnPlanActions); hide(btnArticles);
        hide(btnAIPedagogique); hide(btnAIDecisionnel);
        hide(btnParcours); hide(btnProjets); hide(btnGamesHub);
        hide(btnSwitchBack); hide(btnNotifications);

        if (isSuperAdmin()) {
            // ADMINM front : Dashboard Stratégique + Plans + Articles + IA Décisionnelle + switcher back
            show(btnDashboardAdmin);
            show(btnPlanActions);
            show(btnArticles);
            show(btnAIDecisionnel);
            show(btnSwitchBack);
            show(btnNotifications);
            hide(btnGamesHub); // Cacher Hub Intervention pour ADMINM
            showDashboardAdmin();
        } else if (isEnseignant()) {
            show(btnDashboardEnseignant);
            show(btnPlanActions);
            show(btnArticles);
            show(btnAIPedagogique);
            hide(btnGamesHub); // Cacher Hub Intervention pour enseignant
            showDashboardEnseignant();
        }
    }

    @FXML
    void switchToBack() {
        SessionManager.setFrontMode(false);
        showBackMode();

        // Gestion visibilité sidebar selon le niveau d'admin
        if (isSuperAdmin()) {
            show(boxAdmin);
            show(boxBackAdmin);
            show(boxSwitcher);
            for (javafx.scene.Node n : boxBackAdmin.getChildren()) show(n);
        } else if (isAdmin()) {
            hide(boxAdmin);
            show(boxBackAdmin);
            show(boxSwitcher);
        }

        // Par défaut au switch back
        if (isAdmin()) showIoT();
        else showCategories();
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
        if (!acl.canAccess(AccessControlService.Module.DASHBOARD_ADMIN)) return;
        loadView("/fxml/DashboardAdmin.fxml");
        setActiveBtn(btnDashboardAdmin);
    }

    @FXML
    void showDashboardEnseignant() {
        if (!acl.canAccess(AccessControlService.Module.DASHBOARD_ENSEIGNANT)) return;
        loadView("/fxml/DashboardEnseignant.fxml");
        setActiveBtn(btnDashboardEnseignant);
    }

    @FXML
    void showPlanActions() {
        if (!acl.canAccess(AccessControlService.Module.PLAN_ACTIONS)) return;
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
        if (!acl.canAccess(AccessControlService.Module.ARTICLES)) return;
        if (frontHeader != null && frontHeader.isVisible()) {
            loadView("/fxml/ArticleListFront.fxml");
        } else {
            loadView("/fxml/ArticleList.fxml");
        }
        setActiveBtn(btnArticles);
    }

    @FXML
    void showParcours() {
        if (!acl.canAccess(AccessControlService.Module.PARCOURS_FRONT)) return;
        System.out.println("🎓 Loading Parcours View...");
        loadView("/AfficherParcours.fxml");
        setActiveBtn(btnParcours);
    }

    @FXML
    void showProjets() {
        if (!acl.canAccess(AccessControlService.Module.PROJETS_FRONT)) return;
        System.out.println("📂 Loading Projects View...");
        loadView("/AfficherProjetsGlobal.fxml");
        setActiveBtn(btnProjets);
    }

    @FXML
    void showGamesHub() {
        if (!acl.canAccess(AccessControlService.Module.GAMES_HUB)) return;
        System.out.println("🎮 Loading Games Hub View...");
        loadView("/fxml/GamesHub.fxml");
        setActiveBtn(btnGamesHub);
    }

    @FXML
    void showIoT() {
        if (!acl.canAccess(AccessControlService.Module.IOT)) return;
        System.out.println("📡 Loading IoT View...");
        loadView("/fxml/IoTClustering.fxml");
        setActiveBtn(btnIoT);
    }

    @FXML
    void showPreferences() {
        loadView("/fxml/Preferences.fxml");
    }

    @FXML
    void showAtRisk() {
        if (!acl.canAccess(AccessControlService.Module.AT_RISK)) return;
        System.out.println("🚨 Loading At-Risk Scenario...");
        loadView("/fxml/AtRiskScenario.fxml");
        setActiveBtn(btnAtRisk);
    }

    private void updateNotifications() {
        // Mise à jour du badge notifications depuis le NotificationService
        if (!isSuperAdmin()) return;
        new Thread(() -> {
            try {
                edu.connection3a36.services.NotificationService ns =
                        new edu.connection3a36.services.NotificationService();
                int count = ns.countNonLues();
                javafx.application.Platform.runLater(() -> updateNotificationBadge(count));
            } catch (Exception ignored) {}
        }).start();
    }

    /** Met à jour le badge de notification dans la sidebar et le header. */
    public void updateNotificationBadge(int count) {
        String label = count > 0 ? "🔔 (" + count + ")" : "🔔";
        String style = count > 0
                ? "-fx-text-fill: #e74c3c; -fx-font-weight: bold;"
                : "";
        if (btnNotifications != null) {
            btnNotifications.setText(label);
            btnNotifications.setStyle(style);
        }
        if (btnNotifSidebar != null) {
            btnNotifSidebar.setText(label);
            btnNotifSidebar.setStyle(style);
        }
    }

    @FXML
    void handleNotifications() {
        if (!isSuperAdmin()) return;
        try {
            NotificationController nc = new NotificationController();
            Stage stage = (Stage) contentArea.getScene().getWindow();
            nc.openNotificationsPanel(stage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void showCategories() {
        if (!acl.canAccess(AccessControlService.Module.CATEGORIES)) return;
        loadView("/fxml/CategorieList.fxml");
        setActiveBtn(btnCategories);
    }

    @FXML
    void showUtilisateurs() {
        if (!acl.canAccess(AccessControlService.Module.UTILISATEURS)) return;
        loadView("/fxml/UtilisateurList.fxml");
        setActiveBtn(btnUtilisateurs);
    }

    @FXML
    public void handleGestionUtilisateursHejer() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/esprit/views/BackOffice.fxml"));
            javafx.scene.layout.BorderPane bp = loader.load();
            // Supprimer la sidebar bleue — elle est redondante dans MainView
            bp.setLeft(null);
            contentArea.getChildren().setAll(bp);
            setActiveBtn(btnGestionUtilisateursHejer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void showMonProfil() {
        try {
            // Construire un com.esprit.models.Utilisateur depuis la session courante
            edu.connection3a36.entities.Utilisateur session = SessionManager.getCurrentUser();
            if (session == null) return;

            com.esprit.models.Utilisateur u = new com.esprit.models.Utilisateur();
            u.setId(session.getId());
            u.setNom(session.getNom());
            u.setPrenom(session.getPrenom());
            u.setEmail(session.getEmail());
            u.setMdp(session.getMdp());
            u.setRole(session.getRole());
            u.setStatus(session.getStatus());

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/esprit/views/FrontOffice.fxml"));
            javafx.scene.layout.BorderPane bp = loader.load();
            com.esprit.controllers.FrontOfficeController ctrl = loader.getController();
            ctrl.setUtilisateur(u);
            // Supprimer la navbar du FrontOffice — on est déjà dans MainView
            bp.setTop(null);
            contentArea.getChildren().setAll(bp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void showAIPedagogique() {
        if (!acl.canAccess(AccessControlService.Module.IA_PEDAGOGIQUE)) return;
        loadView("/fxml/AIPedagogique.fxml");
        setActiveBtn(btnAIPedagogique);
    }

    @FXML
    void showAIDecisionnel() {
        if (!acl.canAccess(AccessControlService.Module.IA_DECISIONNELLE)) return;
        loadView("/fxml/AIDecisionnel.fxml");
        setActiveBtn(btnAIDecisionnel);
    }

    @FXML
    void showBackParcours() {
        if (!acl.canAccess(AccessControlService.Module.BACK_PARCOURS)) return;
        loadView("/BackOfficeParcours.fxml");
        setActiveBtn(btnBackParcours);
    }

    @FXML
    void showBackProjets() {
        if (!acl.canAccess(AccessControlService.Module.BACK_PROJETS)) return;
        loadView("/BackOfficeProjets.fxml");
        setActiveBtn(btnBackProjets);
    }

    @FXML
    void showBackFeedbacks() {
        loadView("/fxml/AdminFeedback.fxml");
        setActiveBtn(btnBackFeedbacks);
    }

    @FXML
    void showMesFeedbacks() {
        loadView("/fxml/MesFeedbacks.fxml");
        setActiveBtn(btnMesFeedbacks);
    }

    @FXML
    void showObjectifs() {
        loadView("/fxml/ObjectifList.fxml");
        setActiveBtn(btnObjectifs);
    }

    @FXML
    void showDashboardObjectifs() {
        loadView("/fxml/DashboardObjectifsAdmin.fxml");
        setActiveBtn(btnDashboardObjectifs);
    }

    @FXML
    void showHumeur() {
        loadView("/views/humeur.fxml");
        setActiveBtn(btnHumeur);
    }

    @FXML
    void showPlanning() {
        loadView("/views/planning.fxml");
        setActiveBtn(btnPlanning);
    }

    @FXML
    void showCarnet() {
        loadView("/views/carnet.fxml");
        setActiveBtn(btnCarnet);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DÉCONNEXION
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    public void handleLogout() {
        try {
            SessionManager.logout();
            Stage stage = (Stage) contentArea.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/com/esprit/views/Login.fxml"));
            Scene scene = new Scene(root, 1200, 750);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Permet aux sous-contrôleurs de charger une vue sans casser le MainController.
     */
    public void loadInContentArea(String fxmlPath) {
        loadView(fxmlPath);
    }

    public void loadInContentArea(Parent view) {
        contentArea.getChildren().setAll(view);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CHARGEMENT DE VUE
    // ─────────────────────────────────────────────────────────────────────────

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
            applyGlobalPreferences();
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
        applyGlobalPreferences();
    }

    public void applyGlobalPreferences() {
        if (contentArea == null || contentArea.getScene() == null || contentArea.getScene().getRoot() == null) return;
        prefsService.applyToRoot((Parent) contentArea.getScene().getRoot(), prefsService.load());
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
            btnAIPedagogique, btnAIDecisionnel,
            btnParcours, btnProjets,
            btnBackParcours, btnBackProjets
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
        Utilisateur user = SessionManager.getCurrentUser();
        if (user == null) return false;
        
        String email = user.getEmail() != null ? user.getEmail().toLowerCase() : "";
        return email.equals("admin@esprit.tn") || userRole.equals("ADMINM");
    }

    private boolean isAdmin() {
        Utilisateur user = SessionManager.getCurrentUser();
        if (user == null) return false;
        
        String email = user.getEmail() != null ? user.getEmail().toLowerCase() : "";
        // admin@gmail.com est un admin mais PAS le superadmin
        return (email.equals("admin@gmail.com") || userRole.contains("ADMIN")) && !isSuperAdmin();
    }

    private boolean isEnseignant() {
        return userRole.contains("ENSEIGNANT");
    }

    private boolean isEtudiant() {
        return userRole.contains("ETUDIANT") || userRole.equals("STUDENT");
    }

    // ── Visibility helpers ────────────────────────────────────────────────────
    private static void show(javafx.scene.Node n) {
        if (n != null) { n.setVisible(true);  n.setManaged(true);  }
    }

    private static void hide(javafx.scene.Node n) {
        if (n != null) { n.setVisible(false); n.setManaged(false); }
    }
}
