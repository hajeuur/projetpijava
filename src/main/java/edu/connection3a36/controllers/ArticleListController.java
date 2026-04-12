package edu.connection3a36.controllers;

import edu.connection3a36.entities.CategorieArticle;
import edu.connection3a36.entities.ReferenceArticle;
import edu.connection3a36.services.CategorieArticleService;
import edu.connection3a36.services.ReferenceArticleService;
import edu.connection3a36.tools.AlertUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import edu.connection3a36.tools.SessionManager;

/**
 * Contrôleur pour la vue liste des Articles de Référence.
 */
public class ArticleListController {

    @FXML private TableView<ReferenceArticle> tableArticles;
    @FXML private TableColumn<ReferenceArticle, String> colId;
    @FXML private TableColumn<ReferenceArticle, String> colTitre;
    @FXML private TableColumn<ReferenceArticle, String> colCategorie;
    @FXML private TableColumn<ReferenceArticle, String> colPublished;
    @FXML private TableColumn<ReferenceArticle, String> colDate;
    @FXML private TableColumn<ReferenceArticle, Void> colActions;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categorieFilter;
    @FXML private ComboBox<String> publishedFilter;

    @FXML private Label lblTotal;
    @FXML private Label lblPublished;
    @FXML private Label lblDraft;
    @FXML private Button btnNewArticle;

    private final ReferenceArticleService articleService = new ReferenceArticleService();
    private final CategorieArticleService categorieService = new CategorieArticleService();
    private final ObservableList<ReferenceArticle> articlesList = FXCollections.observableArrayList();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        setupFilters();
        setupColumns();
        loadData();
        
        if ("ENSEIGNANT".equals(SessionManager.getCurrentUser().getRole()) || SessionManager.isFrontMode()) {
            if(btnNewArticle != null) {
                btnNewArticle.setVisible(false);
                btnNewArticle.setManaged(false);
            }
        }
    }

    private void setupFilters() {
        // Filtre catégorie
        try {
            ObservableList<String> cats = FXCollections.observableArrayList("");
            for (CategorieArticle c : categorieService.getData()) {
                cats.add(c.getId() + " - " + c.getNomCategorie());
            }
            categorieFilter.setItems(cats);
        } catch (SQLException e) {
            System.err.println("Erreur chargement catégories: " + e.getMessage());
        }

        // Filtre publication
        publishedFilter.setItems(FXCollections.observableArrayList("", "Publiés", "Brouillons"));
    }

    private void setupColumns() {
        colId.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getId())));
        colTitre.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTitre()));
        colCategorie.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCategorieNom() != null ? d.getValue().getCategorieNom() : "—"));

        // Statut publication avec badge
        colPublished.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isPublished() ? "Publié" : "Brouillon"));
        colPublished.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add(item.equals("Publié") ? "badge-published" : "badge-draft");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        colDate.setCellValueFactory(d -> {
            var date = d.getValue().getCreatedAt();
            return new SimpleStringProperty(date != null ? date.format(DATE_FMT) : "");
        });

        // Actions: Voir / Modifier / Supprimer / Toggle Publish / PDF
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnView = new Button("Voir");
            private final Button btnEdit = new Button("Éditer");
            private final Button btnDelete = new Button("Suppr.");
            private final Button btnToggle = new Button("Publier/Retirer");
            private final Button btnPdf = new Button("📄 PDF");
            private final HBox box = new HBox(4);
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
                btnPdf.setStyle("-fx-padding: 3 6; -fx-font-size: 10px; -fx-background-color: #8e44ad; -fx-text-fill: white;");

                if ("ENSEIGNANT".equals(SessionManager.getCurrentUser().getRole()) || SessionManager.isFrontMode()) {
                    box.getChildren().addAll(btnView, btnPdf);
                } else {
                    box.getChildren().addAll(btnView, btnPdf, btnEdit, btnToggle, btnDelete);
                }

                btnView.setOnAction(e -> {
                    ReferenceArticle a = getTableView().getItems().get(getIndex());
                    showArticleDetail(a);
                });
                btnPdf.setOnAction(e -> {
                    ReferenceArticle a = getTableView().getItems().get(getIndex());
                    edu.connection3a36.tools.PdfExporter.exportSingleArticle(getTableView().getScene().getWindow(), a);
                });
                btnEdit.setOnAction(e -> {
                    ReferenceArticle a = getTableView().getItems().get(getIndex());
                    openForm(a);
                });
                btnDelete.setOnAction(e -> {
                    ReferenceArticle a = getTableView().getItems().get(getIndex());
                    handleDelete(a);
                });
                btnToggle.setOnAction(e -> {
                    ReferenceArticle a = getTableView().getItems().get(getIndex());
                    handleTogglePublish(a);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tableArticles.setItems(articlesList);
    }

    private void loadData() {
        try {
            articlesList.clear();
            articlesList.addAll(articleService.getData());
            updateStats();
        } catch (SQLException e) {
            AlertUtil.showError("Erreur chargement articles : " + e.getMessage());
        }
    }

    private void updateStats() {
        try {
            int total = articlesList.size();
            int published = articleService.countPublished();
            lblTotal.setText("Total: " + total);
            lblPublished.setText("📗 Publiés: " + published);
            lblDraft.setText("📝 Brouillons: " + (total - published));
        } catch (SQLException e) {
            System.err.println("Erreur stats: " + e.getMessage());
        }
    }

    @FXML void handleSearch() {
        try {
            String search = searchField.getText();
            Integer catId = null;
            String catVal = categorieFilter.getValue();
            if (catVal != null && !catVal.isEmpty() && catVal.contains(" - ")) {
                catId = Integer.parseInt(catVal.split(" - ")[0]);
            }
            Boolean published = null;
            String pubVal = publishedFilter.getValue();
            if ("Publiés".equals(pubVal)) published = true;
            else if ("Brouillons".equals(pubVal)) published = false;

            articlesList.clear();
            articlesList.addAll(articleService.searchArticles(search, catId, published));
            updateStats();
        } catch (SQLException e) {
            AlertUtil.showError("Erreur recherche : " + e.getMessage());
        }
    }

    @FXML void handleReset() {
        searchField.clear();
        categorieFilter.setValue(null);
        publishedFilter.setValue(null);
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
                article.getContenu() != null ? article.getContenu().substring(0, Math.min(500, article.getContenu().length())) : "",
                article.getCategorieNom() != null ? article.getCategorieNom() : "N/A",
                article.isPublished() ? "Publié" : "Brouillon",
                article.getCreatedAt() != null ? article.getCreatedAt().format(DATE_FMT) : "N/A"
        );
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails de l'Article");
        alert.setHeaderText(article.getTitre());
        alert.setContentText(details);
        alert.getDialogPane().setMinWidth(500);
        alert.showAndWait();
    }

    private void openForm(ReferenceArticle articleToEdit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ArticleForm.fxml"));
            Parent root = loader.load();

            ArticleFormController controller = loader.getController();
            if (articleToEdit != null) {
                controller.setArticleToEdit(articleToEdit);
            }

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
}
