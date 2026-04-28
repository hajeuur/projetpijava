package edu.connection3a36.controllers;

import edu.connection3a36.entities.PlanActions;
import edu.connection3a36.enums.CategorieSortie;
import edu.connection3a36.enums.Statut;
import edu.connection3a36.services.EmailService;
import edu.connection3a36.services.PlanActionsService;
import edu.connection3a36.services.UserPreferencesService;
import edu.connection3a36.tools.AlertUtil;
import edu.connection3a36.tools.AIJsonParser;
import edu.connection3a36.tools.AIJsonSchemas;
import edu.connection3a36.tools.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

/**
 * Contrôleur Plans d'Actions.
 * - Mode BACK (tableau) : gestion complète CRUD
 * - Mode FRONT (cartes) : vue lecture + feedback enseignant
 */
public class PlanActionsListController {

    // ── Tableau (back) ────────────────────────────────────────────────────────
    @FXML private TableView<PlanActions>              tablePlans;
    @FXML private TableColumn<PlanActions, String>    colId;
    @FXML private TableColumn<PlanActions, String>    colDecision;
    @FXML private TableColumn<PlanActions, String>    colStatut;
    @FXML private TableColumn<PlanActions, String>    colCategorie;
    @FXML private TableColumn<PlanActions, String>    colDate;
    @FXML private TableColumn<PlanActions, Void>      colActions;

    // ── Cartes (front) ────────────────────────────────────────────────────────
    @FXML private FlowPane cardsContainer;

    // ── Communs ───────────────────────────────────────────────────────────────
    @FXML private TextField          searchField;
    @FXML private ComboBox<String>   statutFilter;
    @FXML private ComboBox<String>   categorieFilter;
    @FXML private Label              lblTotal;
    @FXML private Label              lblEnAttente;
    @FXML private Label              lblEnCours;
    @FXML private Label              lblFini;
    @FXML private Label              lblRejete;
    @FXML private Button             btnNewPlan;

    private final PlanActionsService           service   = new PlanActionsService();
    private final UserPreferencesService       prefsService = new UserPreferencesService();
    private final EmailService                 emailService = new EmailService();
    private final ObservableList<PlanActions>  plansList = FXCollections.observableArrayList();
    private static final DateTimeFormatter     DATE_FMT  = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private boolean isFrontMode() {
        return cardsContainer != null;
    }

    @FXML
    public void initialize() {
        setupFilters();

        if (isFrontMode()) {
            // Vue cartes (FRONT)
            if (btnNewPlan != null) { btnNewPlan.setVisible(false); btnNewPlan.setManaged(false); }
            loadData();
        } else {
            // Vue tableau (BACK)
            setupColumns();
            loadData();
            boolean isEnseignant = "ENSEIGNANT".equals(
                    SessionManager.getCurrentUser() != null
                            ? SessionManager.getCurrentUser().getRole() : "");
            if (isEnseignant && btnNewPlan != null) {
                btnNewPlan.setVisible(false);
                btnNewPlan.setManaged(false);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SETUP
    // ─────────────────────────────────────────────────────────────────────────

    private void setupFilters() {
        if (statutFilter != null) {
            statutFilter.setItems(FXCollections.observableArrayList("", "EN_ATTENTE", "EN_COURS", "FINI", "REJETE"));
        }
        if (categorieFilter != null) {
            categorieFilter.setItems(FXCollections.observableArrayList("", "PEDAGOGIQUE", "STRATEGIQUE", "ADMINISTRATIVE"));
        }
    }

    private void setupColumns() {
        colId.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getId())));
        colDecision.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDecision()));

        colStatut.setCellValueFactory(d -> {
            Statut s = d.getValue().getStatut();
            return new SimpleStringProperty(s != null ? s.getLabel() : "");
        });
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(item);
                PlanActions plan = getTableView().getItems().get(getIndex());
                badge.getStyleClass().add(switch (plan.getStatut()) {
                    case EN_ATTENTE -> "badge-attente";
                    case EN_COURS  -> "badge-encours";
                    case FINI      -> "badge-fini";
                    case REJETE    -> "badge-rejete";
                });
                setGraphic(badge); setText(null);
            }
        });

        colCategorie.setCellValueFactory(d -> {
            CategorieSortie c = d.getValue().getCategorie();
            return new SimpleStringProperty(c != null ? c.getLabel() : "");
        });

        colDate.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDate() != null ? d.getValue().getDate().format(DATE_FMT) : ""));

        // Colonne actions (Back)
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnView   = new Button("Voir");
            private final Button btnEdit   = new Button("Éditer");
            private final Button btnDelete = new Button("Suppr.");
            private final Button btnFB     = new Button("💬 Feedback");
            private final HBox   box       = new HBox(5);
            {
                box.setAlignment(Pos.CENTER);
                btnView.getStyleClass().add("btn-primary");
                btnView.setStyle("-fx-padding: 3 7; -fx-font-size: 11px;");
                btnEdit.getStyleClass().add("btn-warning");
                btnEdit.setStyle("-fx-padding: 3 7; -fx-font-size: 11px;");
                btnDelete.getStyleClass().add("btn-danger");
                btnDelete.setStyle("-fx-padding: 3 7; -fx-font-size: 11px;");
                btnFB.getStyleClass().add("btn-info");
                btnFB.setStyle("-fx-padding: 3 7; -fx-font-size: 11px;");

                boolean isEnseignant = "ENSEIGNANT".equals(
                        SessionManager.getCurrentUser() != null
                                ? SessionManager.getCurrentUser().getRole() : "");

                if (isEnseignant) {
                    box.getChildren().addAll(btnView, btnFB);
                } else {
                    box.getChildren().addAll(btnView, btnFB, btnEdit, btnDelete);
                }

                btnView.setOnAction(e   -> handleView(getTableView().getItems().get(getIndex())));
                btnEdit.setOnAction(e   -> handleEdit(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> handleDelete(getTableView().getItems().get(getIndex())));
                btnFB.setOnAction(e     -> handleFeedback(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tablePlans.setItems(plansList);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DATA
    // ─────────────────────────────────────────────────────────────────────────

    private void loadData() {
        try {
            plansList.clear();
            plansList.addAll(service.getData());
            updateStats();
            if (isFrontMode()) buildCards();
        } catch (SQLException e) {
            AlertUtil.showError("Erreur chargement des plans : " + e.getMessage());
        }
    }

    private void updateStats() {
        try {
            Map<String, Integer> stats = service.countByStatut();
            if (lblTotal != null)     lblTotal.setText("Total: " + plansList.size());
            if (lblEnAttente != null) lblEnAttente.setText("⏳ En attente: " + stats.getOrDefault("EN_ATTENTE", 0));
            if (lblEnCours != null)   lblEnCours.setText("🔄 En cours: " + stats.getOrDefault("EN_COURS", 0));
            if (lblFini != null)      lblFini.setText("✅ Terminé: " + stats.getOrDefault("FINI", 0));
            if (lblRejete != null)    lblRejete.setText("❌ Rejeté: " + stats.getOrDefault("REJETE", 0));
        } catch (SQLException e) {
            System.err.println("Erreur stats: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VUE CARTES (FRONT)
    // ─────────────────────────────────────────────────────────────────────────

    private void buildCards() {
        if (cardsContainer == null) return;
        cardsContainer.getChildren().clear();

        String userRole = SessionManager.getCurrentUser() != null
                ? SessionManager.getCurrentUser().getRole() : "";
        boolean isEnseignant = "ENSEIGNANT".equals(userRole);
        boolean darkMode = prefsService.load().darkMode;

        for (PlanActions plan : plansList) {
            cardsContainer.getChildren().add(buildPlanCard(plan, isEnseignant, darkMode));
        }

        if (plansList.isEmpty()) {
            Label empty = new Label("Aucun plan d'action disponible.");
            empty.setStyle("-fx-text-fill: #7a8fa5; -fx-font-style: italic; -fx-font-size: 14px;");
            cardsContainer.getChildren().add(empty);
        }
    }

    private VBox buildPlanCard(PlanActions plan, boolean isEnseignant, boolean darkMode) {
        VBox card = new VBox(10);
        card.getStyleClass().add("plan-card");
        card.setPrefWidth(320);

        // ── Bandeau couleur statut ──
        String borderColor = switch (plan.getStatut() != null ? plan.getStatut() : Statut.EN_ATTENTE) {
            case EN_ATTENTE -> "#e67e22";
            case EN_COURS   -> "#9dbbce";
            case FINI       -> "#27ae60";
            case REJETE     -> "#d52e28";
        };
        card.setStyle("-fx-border-left-color: " + borderColor + "; -fx-border-left-width: 4;"
                + "-fx-padding: 16; -fx-background-color: " + (darkMode ? "#161b22" : "white") + "; -fx-background-radius: 12;"
                + "-fx-border-color: " + borderColor + "; -fx-border-width: 0 0 0 4; -fx-border-radius: 12;"
                + "-fx-effect: dropshadow(gaussian, rgba(16,44,89,0.08), 10, 0, 0, 2);");

        // ── Titre + badge statut ──
        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label badge = new Label(plan.getStatut() != null ? plan.getStatut().getLabel() : "");
        badge.getStyleClass().add(switch (plan.getStatut() != null ? plan.getStatut() : Statut.EN_ATTENTE) {
            case EN_ATTENTE -> "badge-attente";
            case EN_COURS   -> "badge-encours";
            case FINI       -> "badge-fini";
            case REJETE     -> "badge-rejete";
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label catBadge = new Label(plan.getCategorie() != null ? plan.getCategorie().getLabel() : "");
        catBadge.setStyle("-fx-background-color: " + (darkMode ? "#30363d" : "rgba(16,44,89,0.08)") + "; -fx-text-fill: " + (darkMode ? "#e6edf3" : "#102c59") + ";"
                + "-fx-padding: 2 8; -fx-background-radius: 12; -fx-font-size: 10px;");

        titleRow.getChildren().addAll(badge, spacer, catBadge);

        // ── Décision ──
        Label decision = new Label(plan.getDecision() != null ? plan.getDecision() : "");
        decision.setWrapText(true);
        decision.setMaxWidth(290);
        decision.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + (darkMode ? "#e6edf3" : "#102c59") + ";");

        // ── Description courte ──
        String desc = plan.getDescription() != null ? plan.getDescription() : "";
        Label description = new Label(desc.length() > 120 ? desc.substring(0, 120) + "..." : desc);
        description.setWrapText(true);
        description.setMaxWidth(290);
        description.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (darkMode ? "#c9d1d9" : "#5a7a90") + ";");

        // ── Date ──
        Label dateLabel = new Label("📅 " + (plan.getDate() != null ? plan.getDate().format(DATE_FMT) : "—"));
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (darkMode ? "#8b949e" : "#9dbbce") + ";");

        // ── Feedback existant ──
        VBox feedbackBox = new VBox(4);
        if (plan.getFeedbackEnseignant() != null && !plan.getFeedbackEnseignant().isEmpty()) {
            Label fbTitle = new Label("💬 Feedback enseignant :");
            fbTitle.getStyleClass().add("feedback-title");
            Label fbText = new Label(plan.getFeedbackEnseignant());
            fbText.getStyleClass().add("feedback-text");
            fbText.setWrapText(true);
            fbText.setMaxWidth(280);
            feedbackBox.setStyle("-fx-background-color: " + (darkMode ? "#1f2937" : "#eef4f9") + "; -fx-padding: 8; -fx-background-radius: 8;");
            feedbackBox.getChildren().addAll(fbTitle, fbText);
        }

        // ── Boutons actions ──
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button btnVoir = new Button("👁 Voir détails");
        btnVoir.getStyleClass().add("btn-secondary");
        btnVoir.setStyle("-fx-padding: 5 12; -fx-font-size: 12px;");
        btnVoir.setOnAction(e -> handleView(plan));

        actions.getChildren().add(btnVoir);

        if (isEnseignant) {
            Button btnFeedback = new Button("💬 Ajouter Feedback");
            btnFeedback.getStyleClass().add("btn-primary");
            btnFeedback.setStyle("-fx-padding: 5 12; -fx-font-size: 12px;");
            btnFeedback.setOnAction(e -> handleFeedback(plan));
            actions.getChildren().add(btnFeedback);
        } else {
            // ADMIN / Superadmin peuvent générer un article
            Button btnGenArticle = new Button("📝 Générer Article");
            btnGenArticle.getStyleClass().add("btn-info");
            btnGenArticle.setStyle("-fx-padding: 5 12; -fx-font-size: 12px;");
            btnGenArticle.setOnAction(e -> handleGenerateArticle(plan));
            actions.getChildren().add(btnGenArticle);
        }

        card.getChildren().addAll(titleRow, decision, description, dateLabel);
        if (!feedbackBox.getChildren().isEmpty()) card.getChildren().add(feedbackBox);
        card.getChildren().add(actions);

        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ACTIONS
    // ─────────────────────────────────────────────────────────────────────────

    @FXML void handleSearch() {
        try {
            String search   = searchField != null ? searchField.getText() : "";
            String statut   = statutFilter != null ? statutFilter.getValue() : null;
            String categorie = categorieFilter != null ? categorieFilter.getValue() : null;
            plansList.clear();
            plansList.addAll(service.searchPlans(search, statut, categorie, "date", "DESC"));
            updateStats();
            if (isFrontMode()) buildCards();
        } catch (SQLException e) {
            AlertUtil.showError("Erreur recherche : " + e.getMessage());
        }
    }

    @FXML void handleReset() {
        if (searchField != null) searchField.clear();
        if (statutFilter != null) statutFilter.setValue(null);
        if (categorieFilter != null) categorieFilter.setValue(null);
        loadData();
    }

    @FXML void handleAdd() { openForm(null); }

    void handleView(PlanActions plan) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails du Plan d'Action");
        alert.setHeaderText("📋 Plan #" + plan.getId() + " — " + plan.getDecision());
        alert.setContentText(
                "Description :\n" + plan.getDescription() + "\n\n"
                + "Statut : " + (plan.getStatut() != null ? plan.getStatut().getLabel() : "N/A") + "\n"
                + "Catégorie : " + (plan.getCategorie() != null ? plan.getCategorie().getLabel() : "N/A") + "\n"
                + "Date : " + (plan.getDate() != null ? plan.getDate().format(DATE_FMT) : "N/A") + "\n\n"
                + "Feedback : " + (plan.getFeedbackEnseignant() != null ? plan.getFeedbackEnseignant() : "Aucun"));
        alert.getDialogPane().setMinWidth(500);
        alert.showAndWait();
    }

    void handleEdit(PlanActions plan) { openForm(plan); }

    void handleDelete(PlanActions plan) {
        if (AlertUtil.showConfirmation("Supprimer le plan :\n\"" + plan.getDecision() + "\" ?")) {
            try {
                service.deleteEntity(plan);
                AlertUtil.showSuccess("Plan supprimé !");
                loadData();
            } catch (SQLException e) {
                AlertUtil.showError("Erreur suppression : " + e.getMessage());
            }
        }
    }

    /** Feedback enseignant sur un plan d'action */
    void handleFeedback(PlanActions plan) {
        String userRole = SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getRole() : "";
        boolean isEnseignant = "ENSEIGNANT".equals(userRole);

        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle(isEnseignant ? "Feedback Enseignant" : "Gestion du Feedback (Admin)");
        dialog.setHeaderText(isEnseignant ? "Ajouter un feedback" : "Statut du feedback");

        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        content.setPrefWidth(440);

        TextArea taFeedback = new TextArea();
        taFeedback.setPromptText("Aucun feedback...");
        taFeedback.setPrefRowCount(4);
        taFeedback.setWrapText(true);
        if (plan.getFeedbackEnseignant() != null) {
            taFeedback.setText(plan.getFeedbackEnseignant());
        }

        if (isEnseignant) {
            // Mode Enseignant
            Label lblEval = new Label("Évaluation rapide :");
            HBox evalBox = new HBox(5);
            String[] evals = {"Insuffisant", "Passable", "Bien", "Excellent"};
            ToggleGroup tg = new ToggleGroup();
            for (String eval : evals) {
                ToggleButton tb = new ToggleButton(eval);
                tb.setToggleGroup(tg);
                tb.setStyle("-fx-font-size: 10px; -fx-padding: 3 6;");
                tb.selectedProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal) taFeedback.appendText(" [" + eval + "]");
                });
                evalBox.getChildren().add(tb);
            }
            content.getChildren().addAll(lblEval, evalBox, new Label("Votre retour :"), taFeedback);
        } else {
            // Mode Admin
            taFeedback.setEditable(false);
            taFeedback.setStyle("-fx-control-inner-background: #f4f4f4;");
            content.getChildren().addAll(new Label("Feedback enseignant (Lecture Seule) :"), taFeedback);
        }

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    if (isEnseignant) {
                        int userId = SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getId() : 0;
                        service.addFeedback(plan.getId(), taFeedback.getText().trim(), userId);
                        
                        // ─ Auto-notification pour le superadmin ──────────────────────
                        new Thread(() -> {
                            try {
                                edu.connection3a36.services.NotificationService ns =
                                        new edu.connection3a36.services.NotificationService();
                                String profName = SessionManager.getCurrentUser() != null 
                                        ? (SessionManager.getCurrentUser().getPrenom() + " " + SessionManager.getCurrentUser().getNom()).trim()
                                        : "Un enseignant";
                                ns.addFeedbackNotificationForAdmin(plan.getId(), plan.getDecision(), profName);
                                // ── Email au superadmin ──────────────────────────────────────
                                try {
                                    edu.connection3a36.services.UtilisateurService us =
                                            new edu.connection3a36.services.UtilisateurService();
                                    us.getData().stream()
                                        .filter(u -> "ADMIN".equals(u.getRole()) || "SUPERADMIN".equals(u.getRole()))
                                        .forEach(admin -> {
                                            try {
                                                emailService.sendNotification(
                                                    admin.getEmail(),
                                                    admin.getPrenom() + " " + admin.getNom(),
                                                    "Feedback enseignant sur Plan #" + plan.getId(),
                                                    profName + " a soumis un feedback sur le plan : \"" + plan.getDecision() + "\""
                                                );
                                            } catch (Exception ignored) {}
                                        });
                                } catch (Exception ignored) {}
                                // ─────────────────────────────────────────────────────────────
                        // Mettre à jour le badge dans MainController
                                int count = ns.countNonLues();
                                javafx.application.Platform.runLater(() -> {
                                    if (MainController.getInstance() != null)
                                        MainController.getInstance().updateNotificationBadge(count);
                                });
                            } catch (Exception ignored) {}
                        }).start();
                        // ─────────────────────────────────────────────────────────────
                    }
                    return true;
                } catch (SQLException e) {
                    AlertUtil.showError("Erreur : " + e.getMessage());
                }
            }
            return false;
        });

        Optional<Boolean> result = dialog.showAndWait();
        if (result.isPresent() && result.get()) {
            AlertUtil.showSuccess("Mise à jour enregistrée avec succès !");
            loadData();
        }
    }

    /**
     * Génère automatiquement un article lié à ce plan d'action (Requirement #5)
     */
    void handleGenerateArticle(PlanActions plan) {
        if (!AlertUtil.showConfirmation("Voulez-vous générer un article pédagogique basé sur ce plan d'action ?\nCela prendra quelques secondes.")) {
            return;
        }

        new Thread(() -> {
            try {
                edu.connection3a36.services.GroqService groq = new edu.connection3a36.services.GroqService();
                String prompt = "Rédige un article pédagogique professionnel basé sur le plan d'action suivant.\n"
                              + "Titre : " + plan.getDecision() + "\n"
                              + "Description : " + plan.getDescription();
                
                String raw = groq.sendSimpleJsonMessage(
                        prompt,
                        "ADMIN",
                        AIJsonSchemas.ARTICLE
                );
                org.json.JSONObject json = AIJsonParser.extractFirstJsonObject(raw);
                String contenu = AIJsonParser.extractMarkdownContent(raw);
                String aiTitle = json != null ? json.optString("title", "") : "";

                // 1. Créer l'article
                edu.connection3a36.entities.ReferenceArticle article = new edu.connection3a36.entities.ReferenceArticle();
                String finalTitle = !aiTitle.isBlank() ? aiTitle : ("Guide : " + plan.getDecision());
                article.setTitre(finalTitle.length() > 255 ? finalTitle.substring(0, 255) : finalTitle);
                article.setContenu(contenu);
                article.setPublished(true);
                article.setPlanActionsId(plan.getId());
                int fallbackCatId = 1;
                try {
                    edu.connection3a36.services.CategorieArticleService catServ = new edu.connection3a36.services.CategorieArticleService();
                    var cats = catServ.getData();
                    if (!cats.isEmpty()) fallbackCatId = cats.get(0).getId();
                } catch (Exception ignored) {}
                article.setCategorieId(fallbackCatId);
                article.setAuteurId(SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getId() : 1);

                edu.connection3a36.services.ReferenceArticleService articleService = new edu.connection3a36.services.ReferenceArticleService();
                articleService.addEntity(article);

                // 2. Lier l'article au plan d'action
                if (article.getId() > 0) {
                    service.addArticleToPlan(plan.getId(), article.getId());
                }

                javafx.application.Platform.runLater(() -> {
                    AlertUtil.showSuccess("L'article a été généré et lié au plan d'action avec succès !");
                });

                // ── Email notification plan d'action ─────────────────────────
                try {
                    edu.connection3a36.entities.Utilisateur currentUser = SessionManager.getCurrentUser();
                    if (currentUser != null && currentUser.getEmail() != null && !currentUser.getEmail().isBlank()) {
                        emailService.sendPlanCreatedNotification(
                            currentUser.getEmail(),
                            currentUser.getPrenom() + " " + currentUser.getNom(),
                            plan.getDecision(),
                            plan.getDescription() != null
                                ? (plan.getDescription().length() > 300 ? plan.getDescription().substring(0, 300) + "..." : plan.getDescription())
                                : ""
                        );
                        System.out.println("✅ Email plan d'action envoyé à " + currentUser.getEmail());
                    }
                } catch (Exception e) {
                    System.err.println("⚠️ Email plan non envoyé: " + e.getMessage());
                }
                // ─────────────────────────────────────────────────────────────

            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> {
                    AlertUtil.showError("Erreur lors de la génération de l'article : " + ex.getMessage());
                });
            }
        }).start();
    }

    @FXML
    void handleExportPdf() {
        edu.connection3a36.tools.PdfExporter.exportPlanActionsList(
                (tablePlans != null && tablePlans.getScene() != null)
                        ? tablePlans.getScene().getWindow()
                        : (cardsContainer != null ? cardsContainer.getScene().getWindow() : null),
                plansList);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FORMULAIRE
    // ─────────────────────────────────────────────────────────────────────────

    private void openForm(PlanActions planToEdit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PlanActionsForm.fxml"));
            Parent root = loader.load();
            PlanActionsFormController ctrl = loader.getController();
            if (planToEdit != null) ctrl.setPlanToEdit(planToEdit);
            Stage stage = new Stage();
            stage.setTitle(planToEdit != null ? "Modifier Plan d'Action" : "Nouveau Plan d'Action");
            stage.initModality(Modality.APPLICATION_MODAL);
            Scene scene = new Scene(root, 600, 550);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();
            loadData();
        } catch (IOException e) {
            AlertUtil.showError("Erreur ouverture formulaire : " + e.getMessage());
        }
    }
}
