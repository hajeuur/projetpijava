package edu.connection3a36.controllers;

import edu.connection3a36.entities.CategorieArticle;
import edu.connection3a36.entities.ReferenceArticle;
import edu.connection3a36.services.CategorieArticleService;
import edu.connection3a36.services.ReferenceArticleService;
import edu.connection3a36.tools.AlertUtil;
import edu.connection3a36.tools.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Contrôleur Articles de Référence.
 * Prend en charge la vue tableau (BACK) et la vue cartes (FRONT).
 */
public class ArticleListController {

    // ── Tableau (back) ────────────────────────────────────────────────────────
    @FXML private TableView<ReferenceArticle>              tableArticles;
    @FXML private TableColumn<ReferenceArticle, String>    colId;
    @FXML private TableColumn<ReferenceArticle, String>    colTitre;
    @FXML private TableColumn<ReferenceArticle, String>    colCategorie;
    @FXML private TableColumn<ReferenceArticle, String>    colPublished;
    @FXML private TableColumn<ReferenceArticle, String>    colDate;
    @FXML private TableColumn<ReferenceArticle, Void>      colActions;

    // ── Cartes (front) ────────────────────────────────────────────────────────
    @FXML private FlowPane cardsContainer;

    // ── Communs ───────────────────────────────────────────────────────────────
    @FXML private TextField          searchField;
    @FXML private ComboBox<String>   categorieFilter;
    @FXML private ComboBox<String>   publishedFilter;
    @FXML private Label              lblTotal;
    @FXML private Label              lblPublished;
    @FXML private Label              lblDraft;
    @FXML private Button             btnNewArticle;

    private final ReferenceArticleService articleService   = new ReferenceArticleService();
    private final CategorieArticleService categorieService = new CategorieArticleService();
    private final ObservableList<ReferenceArticle> articlesList = FXCollections.observableArrayList();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private boolean isFrontMode() {
        return cardsContainer != null;
    }

    @FXML
    public void initialize() {
        setupFilters();

        if (isFrontMode()) {
            // FRONT (Cartes)
            if (btnNewArticle != null) { btnNewArticle.setVisible(false); btnNewArticle.setManaged(false); }
            loadData();
        } else {
            // BACK (Tableau)
            setupColumns();
            loadData();
            boolean isEnseignant = "ENSEIGNANT".equals(
                    SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getRole() : "");
            if (isEnseignant && btnNewArticle != null) {
                btnNewArticle.setVisible(false);
                btnNewArticle.setManaged(false);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SETUP
    // ─────────────────────────────────────────────────────────────────────────

    private void setupFilters() {
        try {
            if (categorieFilter != null) {
                ObservableList<String> cats = FXCollections.observableArrayList("");
                for (CategorieArticle c : categorieService.getData()) {
                    cats.add(c.getId() + " - " + c.getNomCategorie());
                }
                categorieFilter.setItems(cats);
            }
        } catch (SQLException e) {
            System.err.println("Erreur chargement catégories: " + e.getMessage());
        }

        if (publishedFilter != null) {
            publishedFilter.setItems(FXCollections.observableArrayList("", "Publiés", "Brouillons"));
        }
    }

    private void setupColumns() {
        colId.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getId())));
        colTitre.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTitre()));
        colCategorie.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCategorieNom() != null ? d.getValue().getCategorieNom() : "—"));

        colPublished.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isPublished() ? "Publié" : "Brouillon"));
        colPublished.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(item);
                badge.getStyleClass().add(item.equals("Publié") ? "badge-published" : "badge-draft");
                setGraphic(badge); setText(null);
            }
        });

        colDate.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCreatedAt() != null ? d.getValue().getCreatedAt().format(DATE_FMT) : ""));

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnView   = new Button("Voir");
            private final Button btnEdit   = new Button("Éditer");
            private final Button btnDelete = new Button("Suppr.");
            private final Button btnToggle = new Button("Publier/Retirer");
            private final Button btnPdf    = new Button("📄 PDF");
            private final HBox   box       = new HBox(4);
            {
                box.setAlignment(Pos.CENTER);
                btnView.getStyleClass().add("btn-primary");
                btnView.setStyle("-fx-padding: 3 6; -fx-font-size: 10px;");
                btnEdit.getStyleClass().add("btn-warning");
                btnEdit.setStyle("-fx-padding: 3 6; -fx-font-size: 10px;");
                btnDelete.getStyleClass().add("btn-danger");
                btnDelete.setStyle("-fx-padding: 3 6; -fx-font-size: 10px;");
                btnToggle.getStyleClass().add("btn-success");
                btnToggle.setStyle("-fx-padding: 3 6; -fx-font-size: 10px;");
                btnPdf.getStyleClass().add("btn-secondary");
                btnPdf.setStyle("-fx-padding: 3 6; -fx-font-size: 10px; -fx-text-fill: #102c59;");

                boolean isEnseignant = "ENSEIGNANT".equals(
                        SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getRole() : "");

                if (isEnseignant) {
                    box.getChildren().addAll(btnView, btnPdf);
                } else {
                    Button btnPlan = new Button("📋 Créer Plan");
                    btnPlan.getStyleClass().add("btn-success");
                    btnPlan.setStyle("-fx-padding: 3 6; -fx-font-size: 10px;");
                    btnPlan.setOnAction(e -> handleCreateLinkedPlan(getTableView().getItems().get(getIndex())));
                    box.getChildren().addAll(btnView, btnPdf, btnEdit, btnToggle, btnDelete, btnPlan);
                }

                btnView.setOnAction(e   -> showArticleDetail(getTableView().getItems().get(getIndex())));
                btnPdf.setOnAction(e    -> edu.connection3a36.tools.PdfExporter.exportSingleArticle(
                        getTableView().getScene().getWindow(), getTableView().getItems().get(getIndex())));
                btnEdit.setOnAction(e   -> openForm(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> handleDelete(getTableView().getItems().get(getIndex())));
                btnToggle.setOnAction(e -> handleTogglePublish(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tableArticles.setItems(articlesList);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DATA
    // ─────────────────────────────────────────────────────────────────────────

    private void loadData() {
        try {
            articlesList.clear();
            articlesList.addAll(articleService.getData());
            updateStats();
            if (isFrontMode()) buildCards();
        } catch (SQLException e) {
            AlertUtil.showError("Erreur chargement articles : " + e.getMessage());
        }
    }

    private void updateStats() {
        try {
            int total = articlesList.size();
            int published = articleService.countPublished();
            if (lblTotal != null)     lblTotal.setText("Total: " + total);
            if (lblPublished != null) lblPublished.setText("📗 Publiés: " + published);
            if (lblDraft != null)     lblDraft.setText("📝 Brouillons: " + (total - published));
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

        int displayedCount = 0;
        for (ReferenceArticle article : articlesList) {
            if (!article.isPublished()) {
                continue;
            }
            cardsContainer.getChildren().add(buildArticleCard(article));
            displayedCount++;
        }

        if (displayedCount == 0) {
            Label empty = new Label("Aucun article publié disponible.");
            empty.setStyle("-fx-text-fill: #7a8fa5; -fx-font-style: italic; -fx-font-size: 14px;");
            cardsContainer.getChildren().add(empty);
        }
    }

    private VBox buildArticleCard(ReferenceArticle article) {
        VBox card = new VBox();
        card.getStyleClass().add("article-card");
        card.setPrefWidth(280);

        // Header carte
        VBox header = new VBox(4);
        header.getStyleClass().add("article-card-header");
        // Couleur différente si brouillon
        if (!article.isPublished()) {
            header.setStyle("-fx-background-color: #7a8fa5; -fx-background-radius: 12 12 0 0;");
        }

        Label titre = new Label(article.getTitre());
        titre.getStyleClass().add("article-card-title");

        Label cat = new Label(article.getCategorieNom() != null ? article.getCategorieNom() : "Sans catégorie");
        cat.getStyleClass().add("article-card-cat");
        if (!article.isPublished()) cat.setStyle("-fx-text-fill: white;");

        header.getChildren().addAll(titre, cat);

        // Body carte
        VBox body = new VBox(10);
        body.getStyleClass().add("article-card-body");

        String txt = article.getContenu() != null ? article.getContenu() : "";
        Label contenu = new Label(txt.length() > 100 ? txt.substring(0, 100) + "..." : txt);
        contenu.setWrapText(true);
        contenu.setMaxWidth(250);
        contenu.setStyle("-fx-font-size: 12px; -fx-text-fill: #5a7a90; -fx-pref-height: 50px;");

        HBox footer = new HBox(8);
        footer.setAlignment(Pos.CENTER_LEFT);

        Label dateLabel = new Label(article.getCreatedAt() != null ? article.getCreatedAt().format(DATE_FMT) : "");
        dateLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #9dbbce;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnVoir = new Button("Lire");
        btnVoir.getStyleClass().add("btn-primary");
        btnVoir.setStyle("-fx-padding: 4 12; -fx-font-size: 11px;");
        btnVoir.setOnAction(e -> showArticleDetail(article));

        Button btnPdf = new Button("PDF");
        btnPdf.getStyleClass().add("btn-secondary");
        btnPdf.setStyle("-fx-padding: 4 8; -fx-font-size: 11px;");
        btnPdf.setOnAction(e -> edu.connection3a36.tools.PdfExporter.exportSingleArticle(
                cardsContainer.getScene().getWindow(), article));

        Button btnPlan = new Button("📋 Plan");
        btnPlan.getStyleClass().add("btn-success");
        btnPlan.setStyle("-fx-padding: 4 8; -fx-font-size: 11px;");
        btnPlan.setOnAction(e -> handleCreateLinkedPlan(article));

        footer.getChildren().addAll(dateLabel, spacer, btnPdf, btnPlan, btnVoir);

        // Badge statut brouillon
        if (!article.isPublished()) {
            Label badgeDraft = new Label("Brouillon");
            badgeDraft.setStyle("-fx-background-color: #eef2f7; -fx-text-fill: #5a7a90; -fx-padding: 2 6; -fx-background-radius: 4; -fx-font-size: 10px;");
            body.getChildren().addAll(badgeDraft, contenu, footer);
        } else {
            body.getChildren().addAll(contenu, footer);
        }

        card.getChildren().addAll(header, body);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ACTIONS
    // ─────────────────────────────────────────────────────────────────────────

    @FXML void handleSearch() {
        try {
            String search = searchField != null ? searchField.getText() : "";
            Integer catId = null;
            if (categorieFilter != null) {
                String catVal = categorieFilter.getValue();
                if (catVal != null && !catVal.isEmpty() && catVal.contains(" - ")) {
                    catId = Integer.parseInt(catVal.split(" - ")[0]);
                }
            }
            Boolean published = null;
            if (publishedFilter != null) {
                String pubVal = publishedFilter.getValue();
                if ("Publiés".equals(pubVal)) published = true;
                else if ("Brouillons".equals(pubVal)) published = false;
            }

            articlesList.clear();
            articlesList.addAll(articleService.searchArticles(search, catId, published));
            updateStats();
            if (isFrontMode()) buildCards();
        } catch (SQLException e) {
            AlertUtil.showError("Erreur recherche : " + e.getMessage());
        }
    }

    @FXML void handleReset() {
        if (searchField != null) searchField.clear();
        if (categorieFilter != null) categorieFilter.setValue(null);
        if (publishedFilter != null) publishedFilter.setValue(null);
        loadData();
    }

    @FXML void handleAdd() { openForm(null); }

    void handleDelete(ReferenceArticle article) {
        if (AlertUtil.showConfirmation("Supprimer l'article \"" + article.getTitre() + "\" ?")) {
            try {
                articleService.deleteEntity(article);
                AlertUtil.showSuccess("Article supprimé !");
                loadData();
            } catch (SQLException e) {
                AlertUtil.showError(e.getMessage());
            }
        }
    }

    void handleTogglePublish(ReferenceArticle article) {
        try {
            articleService.togglePublish(article.getId());
            String status = article.isPublished() ? "dépublié" : "publié";
            AlertUtil.showSuccess("L'article a été " + status + " !");
            loadData();
        } catch (SQLException e) {
            AlertUtil.showError(e.getMessage());
        }
    }

    void showArticleDetail(ReferenceArticle article) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails de l'Article");
        dialog.setHeaderText(null);
        dialog.getDialogPane().setStyle("-fx-background-color: white;");

        VBox content = new VBox(20);
        content.setPadding(new Insets(25));
        content.setPrefWidth(600);
        content.setPrefHeight(500);

        // ── Custom Header ──
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-padding: 0 0 15 0; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 1 0;");
        
        Label icon = new Label("📖");
        icon.setStyle("-fx-font-size: 32px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        
        VBox titleBox = new VBox(4);
        Label title = new Label("Article #" + article.getId());
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        Label subtitle = new Label(article.getTitre().length() > 80 ? article.getTitre().substring(0, 80) + "..." : article.getTitre());
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
        titleBox.getChildren().addAll(title, subtitle);
        
        header.getChildren().addAll(icon, titleBox);

        // ── Tags (Statut, Catégorie, Date) ──
        HBox tagsBox = new HBox(10);
        
        Label lblStatut = new Label("Statut : " + (article.isPublished() ? "Publié" : "Brouillon"));
        lblStatut.setStyle("-fx-background-color: " + (article.isPublished() ? "#f0fdf4" : "#f1f5f9") + "; -fx-text-fill: " + (article.isPublished() ? "#166534" : "#475569") + "; -fx-padding: 6 12; -fx-background-radius: 12; -fx-font-size: 12px; -fx-font-weight: bold;");
        
        Label lblCat = new Label("Catégorie : " + (article.getCategorieNom() != null ? article.getCategorieNom() : "Sans catégorie"));
        lblCat.setStyle("-fx-background-color: #eff6ff; -fx-text-fill: #1d4ed8; -fx-padding: 6 12; -fx-background-radius: 12; -fx-font-size: 12px; -fx-font-weight: bold;");
        
        Label lblDate = new Label("📅 " + (article.getCreatedAt() != null ? article.getCreatedAt().format(DATE_FMT) : "N/A"));
        lblDate.setStyle("-fx-background-color: #f8fafc; -fx-text-fill: #94a3b8; -fx-padding: 6 12; -fx-background-radius: 12; -fx-font-size: 12px; -fx-border-color: #e2e8f0; -fx-border-radius: 12;");
        
        tagsBox.getChildren().addAll(lblStatut, lblCat, lblDate);

        // ── Body (Contenu Markdown) ──
        Label descTitle = new Label("Contenu de l'article :");
        descTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155; -fx-font-size: 14px;");
        
        ScrollPane scrollDesc = new ScrollPane();
        scrollDesc.setFitToWidth(true);
        scrollDesc.setStyle("-fx-background-color: transparent; -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-padding: 15;");
        
        VBox descBox = new VBox(10);
        descBox.setStyle("-fx-background-color: white;");
        
        // Rendu Markdown pour éliminer les astérisques et styliser !
        if (article.getContenu() != null && !article.getContenu().isBlank()) {
            edu.connection3a36.tools.MarkdownRenderer.render(article.getContenu(), descBox);
        } else {
            Label noDesc = new Label("Aucun contenu.");
            noDesc.setStyle("-fx-text-fill: #94a3b8; -fx-font-style: italic;");
            descBox.getChildren().add(noDesc);
        }
        scrollDesc.setContent(descBox);

        content.getChildren().addAll(header, tagsBox, descTitle, scrollDesc);
        VBox.setVgrow(scrollDesc, Priority.ALWAYS);
        
        dialog.getDialogPane().setContent(content);
        
        ButtonType btnClose = new ButtonType("Fermer la lecture", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(btnClose);
        
        javafx.scene.Node closeBtn = dialog.getDialogPane().lookupButton(btnClose);
        if (closeBtn != null) {
            closeBtn.setStyle("-fx-background-color: #475569; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 24; -fx-background-radius: 6; -fx-cursor: hand;");
        }

        dialog.showAndWait();
    }

    private void openForm(ReferenceArticle articleToEdit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ArticleForm.fxml"));
            Parent root = loader.load();
            ArticleFormController ctrl = loader.getController();
            if (articleToEdit != null) ctrl.setArticleToEdit(articleToEdit);
            Stage stage = new Stage();
            stage.setTitle(articleToEdit != null ? "Modifier Article" : "Nouvel Article");
            stage.initModality(Modality.APPLICATION_MODAL);
            Scene scene = new Scene(root, 650, 550);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();
            loadData();
        } catch (IOException e) {
            AlertUtil.showError("Erreur : " + e.getMessage());
        }
    }

    /**
     * Crée un Plan d'Action lié à cet article via l'IA (Point #5 réciproque).
     */
    void handleCreateLinkedPlan(ReferenceArticle article) {
        if (!AlertUtil.showConfirmation("Créer un plan d'action pédagogique basé sur l'article :\n'" + article.getTitre() + "' ?")) return;
        new Thread(() -> {
            try {
                edu.connection3a36.services.GroqService groq = new edu.connection3a36.services.GroqService();
                String prompt = "Génère un plan d'action pédagogique basé sur l'article suivant.\n"
                        + "Titre : " + article.getTitre() + "\n"
                        + "Extrait : " + (article.getContenu() != null ? article.getContenu().substring(0, Math.min(300, article.getContenu().length())) : "") + "\n\n"
                        + "Fournis une DÉCISION (1 phrase courte) et une DESCRIPTION (2-3 phrases) séparées par '|'. Exemple : Renforcer les TP|Organiser des sessions pratiques hebdomadaires...";
                String resp = groq.sendSimpleMessage(prompt, "ADMIN");
                String decision, description;
                if (resp.contains("|")) {
                    String[] parts = resp.split("\\|", 2);
                    decision = parts[0].replaceAll("[*#]", "").trim();
                    description = parts[1].trim();
                } else {
                    decision = "Plan lié à : " + article.getTitre();
                    description = resp.substring(0, Math.min(400, resp.length()));
                }
                edu.connection3a36.entities.PlanActions plan = new edu.connection3a36.entities.PlanActions();
                plan.setDecision(decision.length() > 200 ? decision.substring(0,200) : decision);
                plan.setDescription(description);
                plan.setStatut(edu.connection3a36.enums.Statut.EN_ATTENTE);
                plan.setCategorie(edu.connection3a36.enums.CategorieSortie.PEDAGOGIQUE);
                plan.setAuteurId(edu.connection3a36.tools.SessionManager.getCurrentUser() != null ? edu.connection3a36.tools.SessionManager.getCurrentUser().getId() : 1);
                edu.connection3a36.services.PlanActionsService ps = new edu.connection3a36.services.PlanActionsService();
                ps.addEntity(plan);
                if (plan.getId() > 0) ps.addArticleToPlan(plan.getId(), article.getId());
                javafx.application.Platform.runLater(() -> AlertUtil.showSuccess("Plan #" + plan.getId() + " créé et lié à l'article !"));
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> AlertUtil.showError("Erreur : " + ex.getMessage()));
            }
        }).start();
    }
}
