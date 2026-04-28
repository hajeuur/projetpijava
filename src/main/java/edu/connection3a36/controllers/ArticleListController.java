package edu.connection3a36.controllers;

import edu.connection3a36.entities.CategorieArticle;
import edu.connection3a36.entities.ReferenceArticle;
import edu.connection3a36.services.CategorieArticleService;
import edu.connection3a36.services.ReferenceArticleService;
import edu.connection3a36.tools.AlertUtil;
import edu.connection3a36.tools.AIJsonParser;
import edu.connection3a36.tools.AIJsonSchemas;
import edu.connection3a36.tools.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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

        for (ReferenceArticle article : articlesList) {
            // En front, on peut choisir d'afficher que les publiés ou tous
            // Ici on affiche tout mais on différencie visuellement
            cardsContainer.getChildren().add(buildArticleCard(article));
        }

        if (articlesList.isEmpty()) {
            Label empty = new Label("Aucun article disponible.");
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

        String txt = article.getContenu() != null ? edu.connection3a36.tools.AIJsonParser.extractMarkdownContent(article.getContenu()) : "";
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
            if (article.getPlanActionsId() != null && article.getPlanActionsId() > 0) {
                Label badgeLink = new Label("🔗 Plan #" + article.getPlanActionsId());
                badgeLink.setStyle("-fx-background-color: #e0f2fe; -fx-text-fill: #0c4a6e; -fx-padding: 2 6; -fx-background-radius: 4; -fx-font-size: 10px;");
                body.getChildren().addAll(badgeDraft, badgeLink, contenu, footer);
            } else {
                body.getChildren().addAll(badgeDraft, contenu, footer);
            }
        } else {
            if (article.getPlanActionsId() != null && article.getPlanActionsId() > 0) {
                Label badgeLink = new Label("🔗 Plan #" + article.getPlanActionsId());
                badgeLink.setStyle("-fx-background-color: #e0f2fe; -fx-text-fill: #0c4a6e; -fx-padding: 2 6; -fx-background-radius: 4; -fx-font-size: 10px;");
                body.getChildren().addAll(badgeLink, contenu, footer);
            } else {
                body.getChildren().addAll(contenu, footer);
            }
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
        String details = String.format(
                "📝 Article #%d\n\nTitre: %s\n\nContenu:\n%s\n\nCatégorie: %s\nStatut: %s\nCréé le: %s",
                article.getId(), article.getTitre(),
                article.getContenu() != null ? edu.connection3a36.tools.AIJsonParser.extractMarkdownContent(article.getContenu()) : "",
                article.getCategorieNom() != null ? article.getCategorieNom() : "N/A",
                article.isPublished() ? "Publié" : "Brouillon",
                article.getCreatedAt() != null ? article.getCreatedAt().format(DATE_FMT) : "N/A"
        );
        if (article.getPlanActionsId() != null && article.getPlanActionsId() > 0) {
            details += "\nPlan lié: #" + article.getPlanActionsId();
        }
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails de l'Article");
        alert.setHeaderText(article.getTitre());
        alert.setContentText(details);

        // Rendre l'alerte scrollable
        TextArea area = new TextArea(details);
        area.setWrapText(true);
        area.setEditable(false);
        area.setPrefSize(600, 400);
        alert.getDialogPane().setContent(area);
        alert.getDialogPane().setMinWidth(650);

        alert.showAndWait();
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
                        + "Extrait : " + (article.getContenu() != null ? edu.connection3a36.tools.AIJsonParser.extractMarkdownContent(article.getContenu()).substring(0, Math.min(300, edu.connection3a36.tools.AIJsonParser.extractMarkdownContent(article.getContenu()).length())) : "");
                String resp = groq.sendSimpleJsonMessage(
                        prompt,
                        "ADMIN",
                        AIJsonSchemas.PLAN
                );
                String decision, description;
                edu.connection3a36.enums.CategorieSortie iaCategorie = edu.connection3a36.enums.CategorieSortie.PEDAGOGIQUE;
                org.json.JSONObject json = AIJsonParser.extractFirstJsonObject(resp);
                if (json != null && json.has("decision") && json.has("description")) {
                    decision = json.optString("decision", "").replaceAll("[*#]", "").trim();
                    description = json.optString("description", "").trim();
                    String cat = json.optString("categorie", "PEDAGOGIQUE").trim().toUpperCase();
                    try {
                        iaCategorie = edu.connection3a36.enums.CategorieSortie.valueOf(cat);
                    } catch (Exception ignored) {
                        iaCategorie = edu.connection3a36.enums.CategorieSortie.PEDAGOGIQUE;
                    }
                } else if (resp.contains("|")) {
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
                plan.setCategorie(iaCategorie);
                plan.setAuteurId(edu.connection3a36.tools.SessionManager.getCurrentUser() != null ? edu.connection3a36.tools.SessionManager.getCurrentUser().getId() : 1);
                edu.connection3a36.services.PlanActionsService ps = new edu.connection3a36.services.PlanActionsService();
                ps.addEntity(plan);
                if (plan.getId() > 0) {
                    ps.addArticleToPlan(plan.getId(), article.getId());
                    article.setPlanActionsId(plan.getId());
                    edu.connection3a36.services.ReferenceArticleService ras = new edu.connection3a36.services.ReferenceArticleService();
                    ras.updateEntity(article.getId(), article);
                }
                javafx.application.Platform.runLater(() -> AlertUtil.showSuccess("Plan #" + plan.getId() + " créé et lié à l'article !"));
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> AlertUtil.showError("Erreur : " + ex.getMessage()));
            }
        }).start();
    }
}
