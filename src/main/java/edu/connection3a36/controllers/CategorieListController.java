package edu.connection3a36.controllers;

import edu.connection3a36.entities.CategorieArticle;
import edu.connection3a36.services.CategorieArticleService;
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

/**
 * Contrôleur pour la vue liste des Catégories d'Articles.
 */
public class CategorieListController {

    @FXML private TableView<CategorieArticle> tableCategories;
    @FXML private TableColumn<CategorieArticle, String> colId;
    @FXML private TableColumn<CategorieArticle, String> colNom;
    @FXML private TableColumn<CategorieArticle, String> colDescription;
    @FXML private TableColumn<CategorieArticle, String> colNbArticles;
    @FXML private TableColumn<CategorieArticle, String> colDate;
    @FXML private TableColumn<CategorieArticle, Void> colActions;

    @FXML private TextField searchField;
    @FXML private Label lblTotal;

    private final CategorieArticleService service = new CategorieArticleService();
    private final ObservableList<CategorieArticle> categoriesList = FXCollections.observableArrayList();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        setupColumns();
        loadData();
    }

    private void setupColumns() {
        colId.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getId())));
        colNom.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNomCategorie()));
        colDescription.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDescription() != null ? d.getValue().getDescription() : "—"));

        // Nombre d'articles liés
        colNbArticles.setCellValueFactory(d -> {
            try {
                int count = service.countArticlesByCategorie(d.getValue().getId());
                return new SimpleStringProperty(String.valueOf(count));
            } catch (SQLException e) {
                return new SimpleStringProperty("?");
            }
        });

        colDate.setCellValueFactory(d -> {
            var date = d.getValue().getCreatedAt();
            return new SimpleStringProperty(date != null ? date.format(DATE_FMT) : "");
        });

        // Actions
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("Éditer");
            private final Button btnDelete = new Button("Suppr.");
            private final HBox box = new HBox(5, btnEdit, btnDelete);
            {
                box.setAlignment(Pos.CENTER);
                btnEdit.getStyleClass().add("btn-warning");
                btnEdit.setStyle("-fx-padding: 4 8; -fx-font-size: 11px;");
                btnDelete.getStyleClass().add("btn-danger");
                btnDelete.setStyle("-fx-padding: 4 8; -fx-font-size: 11px;");

                btnEdit.setOnAction(e -> {
                    CategorieArticle cat = getTableView().getItems().get(getIndex());
                    openForm(cat);
                });
                btnDelete.setOnAction(e -> {
                    CategorieArticle cat = getTableView().getItems().get(getIndex());
                    handleDelete(cat);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tableCategories.setItems(categoriesList);
    }

    private void loadData() {
        try {
            categoriesList.clear();
            categoriesList.addAll(service.getData());
            lblTotal.setText("Total: " + categoriesList.size());
        } catch (SQLException e) {
            AlertUtil.showError("Erreur chargement catégories : " + e.getMessage());
        }
    }

    @FXML
    void handleSearch() {
        try {
            String keyword = searchField.getText();
            categoriesList.clear();
            if (keyword.isEmpty()) {
                categoriesList.addAll(service.getData());
            } else {
                categoriesList.addAll(service.search(keyword));
            }
            lblTotal.setText("Total: " + categoriesList.size());
        } catch (SQLException e) {
            AlertUtil.showError("Erreur recherche : " + e.getMessage());
        }
    }

    @FXML
    void handleReset() {
        searchField.clear();
        loadData();
    }

    @FXML
    void handleAdd() {
        openForm(null);
    }

    void handleDelete(CategorieArticle cat) {
        if (AlertUtil.showConfirmation("Supprimer la catégorie \"" + cat.getNomCategorie() + "\" ?")) {
            try {
                service.deleteEntity(cat);
                AlertUtil.showSuccess("Catégorie supprimée !");
                loadData();
            } catch (SQLException e) {
                AlertUtil.showError(e.getMessage());
            }
        }
    }

    private void openForm(CategorieArticle catToEdit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CategorieForm.fxml"));
            Parent root = loader.load();

            CategorieFormController controller = loader.getController();
            if (catToEdit != null) {
                controller.setCategorieToEdit(catToEdit);
            }

            Stage stage = new Stage();
            stage.setTitle(catToEdit != null ? "Modifier Catégorie" : "Nouvelle Catégorie");
            stage.initModality(Modality.APPLICATION_MODAL);
            Scene scene = new Scene(root, 500, 350);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();
            loadData();
        } catch (IOException e) {
            AlertUtil.showError("Erreur ouverture formulaire : " + e.getMessage());
        }
    }
}
