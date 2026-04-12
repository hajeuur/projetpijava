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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Contrôleur principal — gère la navigation et les RÔLES.
 */
public class MainController {

    @FXML private StackPane contentArea;
    @FXML private Label lblUser;

    // Sections
    @FXML private VBox boxAdmin;
    @FXML private VBox boxBackAdmin;
    @FXML private VBox boxFront;
    @FXML private VBox boxSwitcher;

    // Boutons
    @FXML private Button btnUtilisateurs;
    @FXML private Button btnCategories;
    @FXML private Button btnDashboardEnseignant;
    @FXML private Button btnDashboardAdmin;
    @FXML private Button btnPlanActions;
    @FXML private Button btnArticles;
    @FXML private Button btnAIPedagogique;
    @FXML private Button btnAIDecisionnel;

    private Button activeButton;

    @FXML
    public void initialize() {
        Utilisateur user = SessionManager.getCurrentUser();
        if (user == null) {
            // Sécurité de base, mais normalement on passe par Login
            return;
        }

        lblUser.setText("👤 " + user.getPrenom() + " (" + user.getRole() + ")");

        // Initialisation de la visibilité selon les rôles (Logique métier de votre demande)
        String r = user.getRole() != null ? user.getRole().toUpperCase() : "";

        if (r.equals("ADMINM") || r.contains("SUPERADMIN") || r.contains("SUPER_ADMIN")) {
            // L'ADMINM (Superadmin) ouvre implicitement le FRONT en premier
            boxAdmin.setVisible(true); boxAdmin.setManaged(true);
            boxBackAdmin.setVisible(true); boxBackAdmin.setManaged(true);
            boxFront.setVisible(true); boxFront.setManaged(true);
            if(boxSwitcher != null) { boxSwitcher.setVisible(true); boxSwitcher.setManaged(true); }
            
            btnDashboardEnseignant.setVisible(false); btnDashboardEnseignant.setManaged(false); // Utilise le dash admin
            btnAIPedagogique.setVisible(false); btnAIPedagogique.setManaged(false); // IA Pédagogique géré par les profs uniquement

            // La demande indique que le Dashboard décideur (IA Strate) doit s'ouvrir en premier sur le front
            showAIDecisionnel();

        } else if (r.contains("ADMIN")) {
            // Admin classique (pas ADMINM) : Uniquement gestion des users. 
            // Ici ça attrapte "ADMINISTRATEUR", car ADMINM a déjà été validé au premier if.
            boxAdmin.setVisible(true); boxAdmin.setManaged(true);
            boxBackAdmin.setVisible(false); boxBackAdmin.setManaged(false);
            boxFront.setVisible(false); boxFront.setManaged(false);
            if(boxSwitcher != null) { boxSwitcher.setVisible(false); boxSwitcher.setManaged(false); }

            showUtilisateurs();

        } else if (r.contains("ENSEIGNANT")) {
            // Enseignant : Uniquement front module (Dash prof, P.A, Articles, IA Pedago)
            boxAdmin.setVisible(false); boxAdmin.setManaged(false);
            boxBackAdmin.setVisible(false); boxBackAdmin.setManaged(false);
            boxFront.setVisible(true); boxFront.setManaged(true);
            if(boxSwitcher != null) { boxSwitcher.setVisible(false); boxSwitcher.setManaged(false); }

            btnDashboardAdmin.setVisible(false); btnDashboardAdmin.setManaged(false);
            btnAIDecisionnel.setVisible(false); btnAIDecisionnel.setManaged(false);

            showDashboardEnseignant();

        } else {
            // Cas par défaut (Étudiants / Visiteurs)
            boxAdmin.setVisible(false); boxAdmin.setManaged(false);
            boxBackAdmin.setVisible(false); boxBackAdmin.setManaged(false);
            boxFront.setVisible(false); boxFront.setManaged(false);
            if(boxSwitcher != null) { boxSwitcher.setVisible(false); boxSwitcher.setManaged(false); }
            contentArea.getChildren().clear();
        }
    }

    @FXML
    void handleLogout() {
        SessionManager.logout();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) lblUser.getScene().getWindow();
            Scene sc = new Scene(root, 1200, 750);
            sc.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            stage.setTitle("MentorAI — Connexion");
            stage.setScene(sc);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void showDashboardEnseignant() {
        loadView("/fxml/DashboardEnseignant.fxml");
        setActiveButton(btnDashboardEnseignant);
    }

    @FXML
    void showPlanActions() {
        loadView("/fxml/PlanActionsList.fxml");
        setActiveButton(btnPlanActions);
    }

    @FXML
    void showArticles() {
        loadView("/fxml/ArticleList.fxml");
        setActiveButton(btnArticles);
    }

    @FXML
    void showCategories() {
        loadView("/fxml/CategorieList.fxml");
        setActiveButton(btnCategories);
    }

    @FXML
    void showUtilisateurs() {
        loadView("/fxml/UtilisateurList.fxml");
        setActiveButton(btnUtilisateurs);
    }

    @FXML
    void showAIPedagogique() {
        loadView("/fxml/AIPedagogique.fxml");
        setActiveButton(btnAIPedagogique);
    }

    @FXML
    void showAIDecisionnel() {
        loadView("/fxml/AIDecisionnel.fxml");
        setActiveButton(btnAIDecisionnel);
    }

    /** BASCULES DEMANDEES PAR ADMINM **/
    @FXML
    void switchToFront() {
        SessionManager.setFrontMode(true);
        // En mode front, on masque le back et on affiche le front
        boxBackAdmin.setVisible(false); boxBackAdmin.setManaged(false);
        boxFront.setVisible(true); boxFront.setManaged(true);
        showAIDecisionnel();
    }

    @FXML
    void switchToBack() {
        SessionManager.setFrontMode(false);
        // En mode back, on masque le front et on montre les options d'administration et crud
        boxFront.setVisible(false); boxFront.setManaged(false);
        boxBackAdmin.setVisible(true); boxBackAdmin.setManaged(true);
        showCategories();
    }

    /**
     * Charge une vue FXML dans la zone de contenu principale.
     * Si la vue échoue (ex: MySQL non démarré), affiche un message d'erreur.
     */
    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement vue: " + fxmlPath);
            e.printStackTrace();

            // Afficher un message d'erreur dans l'interface au lieu de laisser vide
            VBox errorBox = new VBox(15);
            errorBox.setAlignment(Pos.CENTER);

            Label icon = new Label("⚠️");
            icon.setStyle("-fx-font-size: 48px;");

            Label title = new Label("Erreur de connexion à la base de données");
            title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");

            Label message = new Label("Vérifiez que MySQL est démarré dans XAMPP/WAMP\net que la base 'mentorai' existe.");
            message.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d; -fx-text-alignment: center;");
            message.setWrapText(true);

            Button retryBtn = new Button("🔄 Réessayer");
            retryBtn.getStyleClass().add("btn-primary");
            retryBtn.setOnAction(ev -> loadView(fxmlPath));

            errorBox.getChildren().addAll(icon, title, message, retryBtn);
            contentArea.getChildren().clear();
            contentArea.getChildren().add(errorBox);
        }
    }

    /**
     * Met à jour le style du bouton actif dans la sidebar.
     */
    private void setActiveButton(Button btn) {
        if (activeButton != null) {
            activeButton.getStyleClass().remove("sidebar-btn-active");
        }
        if (btn != null) {
            btn.getStyleClass().add("sidebar-btn-active");
            activeButton = btn;
        }
    }
}
