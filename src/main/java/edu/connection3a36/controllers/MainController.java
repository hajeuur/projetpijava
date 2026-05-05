package edu.connection3a36.controllers;

import edu.connection3a36.entities.Utilisateur;
import edu.connection3a36.services.AccessControlService;
import edu.connection3a36.services.UserPreferencesService;
import edu.connection3a36.services.NewsService;
import edu.connection3a36.tools.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.control.Hyperlink;
import javafx.stage.Stage;

import javafx.scene.layout.Pane;
import javafx.animation.TranslateTransition;
import javafx.animation.Interpolator;
import javafx.animation.Animation;
import javafx.util.Duration;
import java.util.List;
import javafx.application.Platform;
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
    @FXML private MenuButton menuPlus;
    // ── Sidebar (BACK) ────────────────────────────────────────────────────────
    @FXML private VBox backSidebar;
    @FXML private VBox boxSwitcher;
    @FXML private VBox boxGestions;

    // ── Boutons Sidebar (BACK) ────────────────────────────────────────────────
    @FXML private Button btnUtilisateurs;
    @FXML private Button btnCategories;
    @FXML private Button btnBackParcours;
    @FXML private Button btnBackProjets;
    @FXML private Button btnBackFeedbacks;
    @FXML private Button btnBackObjectifs;
    @FXML private Button btnDashboardObjectifs;
    @FXML private javafx.scene.control.Separator sepObjectifs;
    @FXML private Label lblObjectifsSection;
    @FXML private Button btnPlanActionsBack;
    @FXML private Button btnArticlesBack;
    @FXML private Button btnIoT;
    @FXML private Button btnAtRisk;
    @FXML private Button btnHumeur;
    @FXML private Button btnPlanning;
    @FXML private Button btnCarnet;
    @FXML private Button btnNotifSidebar;
    @FXML private Label  lblUserBack;
    
    // Labels/Separators Sidebar (pour contrôle fin)
    @FXML private Label lblAdminSection;
    @FXML private Label lblPedagoSection;
    @FXML private Label lblIASection;
    @FXML private Label lblMentorSection;
    @FXML private Separator sepPedago;
    @FXML private Separator sepIA;
    @FXML private Separator sepMentor;
    @FXML private Separator sepSwitcher;

    // ── MentorAI features (header FRONT) ──────────────────────────────────────
    @FXML private Button  btnHeaderHumeur;
    @FXML private Button  btnHeaderPlanning;
    @FXML private Button  btnHeaderCarnet;
    @FXML private Button  btnMesFeedbacks;
    @FXML private Button  btnObjectifs;
    @FXML private MenuButton menuCarriere;
    @FXML private MenuButton menuIA;
    @FXML private HBox newsBar;
    @FXML private HBox hBoxNews;

    // ── État interne ──────────────────────────────────────────────────────────
    private static MainController instance;
    private Button activeHeaderBtn;
    private Button activeSidebarBtn;
    private String userRole = "";
    private final UserPreferencesService prefsService = new UserPreferencesService();
    private final AccessControlService acl = new AccessControlService();
    private final NewsService newsService = new NewsService();

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
        startNewsAnimation();

        // Rendre en plein écran / maximiser

        // Rendre en plein écran / maximiser
        javafx.application.Platform.runLater(() -> {
            if (contentArea.getScene() != null) {
                Stage stage = (Stage) contentArea.getScene().getWindow();
                stage.setMaximized(true);
            }
        });
    }

    /**
     * Configure la visibilité des sections et l'écran de démarrage selon le rôle.
     */
    private void configureByRole() {
        if (isSuperAdmin()) {
            showBackMode();
            show(boxSwitcher);
            show(btnNotifSidebar);

            if (boxGestions != null) {
                show(boxGestions);
                for (javafx.scene.Node n : boxGestions.getChildren()) show(n);
            }

            // Masquer la section Objectifs étudiants pour le SuperAdmin
            // (cette section est réservée à l'admin simple admin@gmail.com)
            hide(btnDashboardObjectifs);
            hide(btnBackParcours);
            hide(btnBackProjets);
            hide(btnBackFeedbacks);
            hide(btnBackObjectifs);
            if (sepObjectifs != null) hide(sepObjectifs);
            if (lblObjectifsSection != null) hide(lblObjectifsSection);

            showCategories();

        } else if (isAdmin()) {
            // admin@gmail.com
            showBackMode();
            hide(boxSwitcher);
            hide(sepSwitcher);

            // Cacher tout dans boxGestions sauf Parcours, Projets, Feedbacks et Dashboard Objectifs
            if (boxGestions != null) {
                show(boxGestions);
                for (javafx.scene.Node n : boxGestions.getChildren()) {
                    if (n instanceof Button b) {
                        if (b == btnBackParcours || b == btnBackProjets || b == btnBackFeedbacks
                                || b == btnDashboardObjectifs) show(b);
                        else hide(b);
                    } else {
                        hide(n); // Séparateurs, labels
                    }
                }
            }
            showBackParcours();

        } else if (isEnseignant()) {
            // Enseignant → FRONT uniquement (dashboard pédagogique + IA + Plans + Articles)
            showFrontMode();
            hide(btnDashboardAdmin);
            hide(btnSwitchBack);
            hide(btnAIDecisionnel);
            hide(btnParcours);
            hide(btnProjets);
            hide(menuCarriere);
            hide(menuIA);
            hide(newsBar);
            hide(btnGamesHub); // Cacher Hub Intervention
            hide(boxGestions);
            hide(boxSwitcher);
            show(btnDashboardEnseignant);
            show(btnAIPedagogique);
            show(btnPlanActions);
            show(btnArticles);
            
            // MentorAI dans le header FRONT
            if (btnHeaderHumeur != null) hide(btnHeaderHumeur);
            if (btnHeaderPlanning != null) hide(btnHeaderPlanning);
            if (btnHeaderCarnet != null) hide(btnHeaderCarnet);

            showDashboardEnseignant();

        } else if (isEtudiant()) {
            // Étudiant → FRONT uniquement (Parcours + Projets)
            showFrontMode();
            hide(btnSwitchBack);
            hide(btnDashboardEnseignant); hide(btnDashboardAdmin);
            hide(btnPlanActions);
            hide(btnArticles);
            hide(btnAIPedagogique); 
            hide(btnAIDecisionnel);
            show(menuIA);
            show(newsBar);
            show(menuCarriere);
            hide(btnGamesHub);
            if (btnMesFeedbacks != null) show(btnMesFeedbacks);
            if (btnObjectifs != null) show(btnObjectifs);
            if (btnHeaderCarnet != null) show(btnHeaderCarnet);
            if (menuPlus != null) show(menuPlus);
            
            showParcours();

        } else {
            // Visiteur → contenu vide
            showFrontMode();
            hide(btnSwitchBack);
            hide(btnDashboardEnseignant); hide(btnDashboardAdmin);
            hide(btnPlanActions);
            hide(btnArticles);
            hide(btnAIPedagogique); hide(btnAIDecisionnel);
            hide(btnParcours); hide(btnProjets); hide(menuCarriere); hide(menuIA); hide(newsBar); hide(btnGamesHub);
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
        hide(btnParcours); hide(btnProjets); hide(menuCarriere); hide(menuIA); hide(newsBar); hide(btnGamesHub);
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
            show(menuIA);
            show(newsBar);
            hide(btnGamesHub); 
            showDashboardEnseignant();
        } else if (isEtudiant()) {
            // Étudiant : Parcours + Projets + Outils IA (Skill Gap, etc.)
            show(menuCarriere);
            show(menuIA);
            show(newsBar);
            show(menuPlus);
            hide(btnGamesHub);
            showParcours(); // Vue par défaut
        }
    }

    @FXML
    void switchToBack() {
        SessionManager.setFrontMode(false);
        showBackMode();

        // Gestion visibilité sidebar selon le niveau d'admin
        if (isSuperAdmin()) {
            show(boxSwitcher);
            show(sepSwitcher);
            if (boxGestions != null) {
                show(boxGestions);
                for (javafx.scene.Node n : boxGestions.getChildren()) show(n);
            }
            // Masquer la section Objectifs étudiants pour le SuperAdmin
            hide(btnDashboardObjectifs);
            hide(btnBackParcours);
            hide(btnBackProjets);
            hide(btnBackFeedbacks);
            hide(btnBackObjectifs);
            if (sepObjectifs != null) hide(sepObjectifs);
            if (lblObjectifsSection != null) hide(lblObjectifsSection);
        } else if (isAdmin()) {
            hide(boxSwitcher);
            hide(sepSwitcher);
            if (boxGestions != null) {
                show(boxGestions);
                for (javafx.scene.Node n : boxGestions.getChildren()) {
                    if (n instanceof Button b) {
                        if (b == btnBackParcours || b == btnBackProjets || b == btnBackFeedbacks || b == btnHumeur || b == btnPlanning || b == btnCarnet) show(b);
                        else hide(b);
                    } else hide(n);
                }
            }
        }

        // Par défaut au switch back
        if (isAdmin()) showIoT();
        else showCategories();
    }

    /** Affiche le header TOP, masque la sidebar. */
    private void showFrontMode() {
        show(frontHeader);
        show(newsBar);
        hide(backSidebar);
    }

    /** Affiche la sidebar LEFT, masque le header. */
    private void showBackMode() {
        hide(frontHeader);
        hide(newsBar);
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
    void showSkillGap() {
        loadView("/SkillGap.fxml");
        setActiveBtn(null);
    }

    @FXML
    void showCareerPredictor() {
        loadView("/CareerDashboard.fxml");
        setActiveBtn(null);
    }

    @FXML
    void showAnalyseCV() {
        loadView("/AnalyseCV.fxml");
        setActiveBtn(null);
    }

    @FXML
    void showEntretienIA() {
        loadView("/EntretienIA.fxml");
        setActiveBtn(null);
    }

    @FXML
    void showPricing() {
        loadView("/Pricing.fxml");
    }

    @FXML
    void showAbout() {
        loadView("/About.fxml");
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

    @FXML
    void toggleFullScreen() {
        Stage stage = (Stage) contentArea.getScene().getWindow();
        stage.setFullScreen(!stage.isFullScreen());
    }

    private void startNewsAnimation() {
        if (hBoxNews == null) return;
        
        new Thread(() -> {
            try {
                List<NewsService.NewsItem> news = newsService.getLatestTechNews();
                if (news.isEmpty()) return;

                Platform.runLater(() -> {
                    hBoxNews.getChildren().clear();
                    for (NewsService.NewsItem item : news) {
                        Hyperlink link = new Hyperlink(item.getTitle());
                        link.setStyle("-fx-text-fill: white; -fx-font-size: 11px; -fx-underline: false;");
                        link.setOnAction(e -> {
                            try {
                                java.awt.Desktop.getDesktop().browse(new java.net.URI(item.getUrl()));
                            } catch (Exception ex) { ex.printStackTrace(); }
                        });
                        hBoxNews.getChildren().add(link);
                        
                        // Separator
                        Label sep = new Label("|");
                        sep.setStyle("-fx-text-fill: rgba(255,255,255,0.3);");
                        hBoxNews.getChildren().add(sep);
                    }
                    
                    double textWidth = hBoxNews.getChildren().size() * 300; // Estimation large car width pas encore calculée
                    
                    javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(
                        javafx.util.Duration.seconds(40), hBoxNews);
                    
                    tt.setFromX(1200);
                    tt.setToX(-3000); // Défilement vers la gauche
                    tt.setCycleCount(javafx.animation.Animation.INDEFINITE);
                    tt.setInterpolator(javafx.animation.Interpolator.LINEAR);
                    tt.play();
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DÉCONNEXION
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    public void handleLogout() {
        try {
            SessionManager.logout();
            Stage stage = (Stage) contentArea.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/Login.fxml"));
            Scene scene = new Scene(root, 1200, 750);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.setMaximized(true);
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

    /** Gère le style actif pour les boutons du header FRONT et de la sidebar BACK. */
    private void setActiveBtn(Button btn) {
        // Liste de TOUS les boutons pouvant être actifs
        Button[] allBtns = {
            btnDashboardEnseignant, btnDashboardAdmin, btnPlanActions, btnArticles,
            btnAIPedagogique, btnAIDecisionnel, btnParcours, btnProjets,
            btnBackParcours, btnBackProjets, btnBackFeedbacks, btnBackObjectifs,
            btnDashboardObjectifs, btnUtilisateurs, btnCategories, btnIoT,
            btnAtRisk, btnPlanActionsBack, btnArticlesBack, btnHumeur,
            btnPlanning, btnCarnet, btnGamesHub
        };

        for (Button b : allBtns) {
            if (b != null) {
                b.getStyleClass().remove("header-nav-btn-active");
                b.getStyleClass().remove("sidebar-btn-active");
            }
        }

        if (btn != null) {
            // Appliquer le style selon le mode actuel
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
