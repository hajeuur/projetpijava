package edu.connection3a36.controllers;

import edu.connection3a36.entities.PlanActions;
import edu.connection3a36.enums.CategorieSortie;
import edu.connection3a36.enums.Statut;
import edu.connection3a36.services.PlanActionsService;
import edu.connection3a36.tools.AlertUtil;
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
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Contrôleur pour la vue liste des Plans d'Actions.
 * Gère : affichage, recherche, filtrage, tri, CRUD, statistiques.
 */
public class PlanActionsListController {

    @FXML private TableView<PlanActions> tablePlans;
    @FXML private TableColumn<PlanActions, String> colId;
    @FXML private TableColumn<PlanActions, String> colDecision;
    @FXML private TableColumn<PlanActions, String> colStatut;
    @FXML private TableColumn<PlanActions, String> colCategorie;
    @FXML private TableColumn<PlanActions, String> colDate;
    @FXML private TableColumn<PlanActions, Void> colActions;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statutFilter;
    @FXML private ComboBox<String> categorieFilter;

    @FXML private Label lblTotal;
    @FXML private Label lblEnAttente;
    @FXML private Label lblEnCours;
    @FXML private Label lblFini;
    @FXML private Label lblRejete;
    @FXML private Button btnNewPlan;

    private final PlanActionsService service = new PlanActionsService();
    private final ObservableList<PlanActions> plansList = FXCollections.observableArrayList();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        setupFilters();
        setupColumns();
        loadData();
        
        if ("ENSEIGNANT".equals(SessionManager.getCurrentUser().getRole()) || SessionManager.isFrontMode()) {
            if(btnNewPlan != null) {
                btnNewPlan.setVisible(false);
                btnNewPlan.setManaged(false);
            }
        }
    }

    // ======================== SETUP ========================

    private void setupFilters() {
        // Filtre statut
        ObservableList<String> statuts = FXCollections.observableArrayList("", "EN_ATTENTE", "EN_COURS", "FINI", "REJETE");
        statutFilter.setItems(statuts);

        // Filtre catégorie
        ObservableList<String> categories = FXCollections.observableArrayList("", "PEDAGOGIQUE", "STRATEGIQUE", "ADMINISTRATIVE");
        categorieFilter.setItems(categories);
    }

    private void setupColumns() {
        colId.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getId())));

        colDecision.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDecision()));

        // Statut avec badge coloré
        colStatut.setCellValueFactory(data -> {
            Statut s = data.getValue().getStatut();
            return new SimpleStringProperty(s != null ? s.getLabel() : "");
        });
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(item);
                    PlanActions plan = getTableView().getItems().get(getIndex());
                    String badgeClass = switch (plan.getStatut()) {
                        case EN_ATTENTE -> "badge-attente";
                        case EN_COURS -> "badge-encours";
                        case FINI -> "badge-fini";
                        case REJETE -> "badge-rejete";
                    };
                    badge.getStyleClass().add(badgeClass);
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        colCategorie.setCellValueFactory(data -> {
            CategorieSortie c = data.getValue().getCategorie();
            return new SimpleStringProperty(c != null ? c.getLabel() : "");
        });

        colDate.setCellValueFactory(data -> {
            var date = data.getValue().getDate();
            return new SimpleStringProperty(date != null ? date.format(DATE_FMT) : "");
        });

        // Colonne actions : Voir / Modifier / Supprimer
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnView = new Button("Voir");
            private final Button btnEdit = new Button("Éditer");
            private final Button btnDelete = new Button("Suppr.");
            private final HBox box = new HBox(5);

            {
                box.setAlignment(Pos.CENTER);
                btnView.getStyleClass().add("btn-primary");
                btnView.setStyle("-fx-padding: 4 8; -fx-font-size: 11px;");
                btnEdit.getStyleClass().add("btn-warning");
                btnEdit.setStyle("-fx-padding: 4 8; -fx-font-size: 11px;");
                btnDelete.getStyleClass().add("btn-danger");
                btnDelete.setStyle("-fx-padding: 4 8; -fx-font-size: 11px;");

                if ("ENSEIGNANT".equals(SessionManager.getCurrentUser().getRole()) || SessionManager.isFrontMode()) {
                    box.getChildren().add(btnView);
                } else {
                    box.getChildren().addAll(btnView, btnEdit, btnDelete);
                }

                btnView.setOnAction(e -> {
                    PlanActions plan = getTableView().getItems().get(getIndex());
                    handleView(plan);
                });
                btnEdit.setOnAction(e -> {
                    PlanActions plan = getTableView().getItems().get(getIndex());
                    handleEdit(plan);
                });
                btnDelete.setOnAction(e -> {
                    PlanActions plan = getTableView().getItems().get(getIndex());
                    handleDelete(plan);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tablePlans.setItems(plansList);
    }

    // ======================== DATA ========================

    private void loadData() {
        try {
            plansList.clear();
            plansList.addAll(service.getData());
            updateStats();
        } catch (SQLException e) {
            AlertUtil.showError("Erreur chargement des plans : " + e.getMessage());
        }
    }

    private void updateStats() {
        try {
            Map<String, Integer> stats = service.countByStatut();
            int total = plansList.size();
            lblTotal.setText("Total: " + total);
            lblEnAttente.setText("⏳ En attente: " + stats.getOrDefault("EN_ATTENTE", 0));
            lblEnCours.setText("🔄 En cours: " + stats.getOrDefault("EN_COURS", 0));
            lblFini.setText("✅ Fini: " + stats.getOrDefault("FINI", 0));
            lblRejete.setText("❌ Rejeté: " + stats.getOrDefault("REJETE", 0));
        } catch (SQLException e) {
            System.err.println("Erreur stats: " + e.getMessage());
        }
    }

    // ======================== ACTIONS ========================

    @FXML
    void handleSearch() {
        try {
            String search = searchField.getText();
            String statut = statutFilter.getValue();
            String categorie = categorieFilter.getValue();

            plansList.clear();
            plansList.addAll(service.searchPlans(search, statut, categorie, "date", "DESC"));
            updateStats();
        } catch (SQLException e) {
            AlertUtil.showError("Erreur recherche : " + e.getMessage());
        }
    }

    @FXML
    void handleReset() {
        searchField.clear();
        statutFilter.setValue(null);
        categorieFilter.setValue(null);
        loadData();
    }

    @FXML
    void handleAdd() {
        openForm(null);
    }

    void handleView(PlanActions plan) {
        // Afficher les détails dans une alerte pour l'instant
        String details = String.format(
                "📋 Plan d'Action #%d\n\n" +
                "Décision: %s\n\n" +
                "Description: %s\n\n" +
                "Statut: %s\n" +
                "Catégorie: %s\n" +
                "Date: %s\n" +
                "Feedback: %s",
                plan.getId(),
                plan.getDecision(),
                plan.getDescription(),
                plan.getStatut() != null ? plan.getStatut().getLabel() : "N/A",
                plan.getCategorie() != null ? plan.getCategorie().getLabel() : "N/A",
                plan.getDate() != null ? plan.getDate().format(DATE_FMT) : "N/A",
                plan.getFeedbackEnseignant() != null ? plan.getFeedbackEnseignant() : "Aucun"
        );
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails du Plan d'Action");
        alert.setHeaderText("Plan #" + plan.getId());
        alert.setContentText(details);
        alert.getDialogPane().setMinWidth(500);
        alert.showAndWait();
    }

    void handleEdit(PlanActions plan) {
        openForm(plan);
    }

    void handleDelete(PlanActions plan) {
        if (AlertUtil.showConfirmation("Voulez-vous vraiment supprimer le plan :\n\"" + plan.getDecision() + "\" ?")) {
            try {
                service.deleteEntity(plan);
                AlertUtil.showSuccess("Plan d'action supprimé avec succès !");
                loadData();
            } catch (SQLException e) {
                AlertUtil.showError("Erreur suppression : " + e.getMessage());
            }
        }
    }

    @FXML
    void handleExportPdf() {
        edu.connection3a36.tools.PdfExporter.exportPlanActionsList(tablePlans.getScene().getWindow(), plansList);
    }

    // ======================== FORMULAIRE ========================

    /**
     * Ouvre le formulaire d'ajout/modification dans une fenêtre modale.
     */
    private void openForm(PlanActions planToEdit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PlanActionsForm.fxml"));
            Parent root = loader.load();

            PlanActionsFormController controller = loader.getController();
            if (planToEdit != null) {
                controller.setPlanToEdit(planToEdit);
            }

            Stage stage = new Stage();
            stage.setTitle(planToEdit != null ? "Modifier Plan d'Action" : "Nouveau Plan d'Action");
            stage.initModality(Modality.APPLICATION_MODAL);
            Scene scene = new Scene(root, 600, 550);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();

            // Rafraîchir après fermeture
            loadData();
        } catch (IOException e) {
            AlertUtil.showError("Erreur ouverture formulaire : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
