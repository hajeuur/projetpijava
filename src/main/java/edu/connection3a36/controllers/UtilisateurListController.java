package edu.connection3a36.controllers;

import edu.connection3a36.entities.Utilisateur;
import edu.connection3a36.services.UtilisateurService;
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

public class UtilisateurListController {

    @FXML private TableView<Utilisateur> tableUtilisateurs;
    @FXML private TableColumn<Utilisateur, String> colId;
    @FXML private TableColumn<Utilisateur, String> colNom;
    @FXML private TableColumn<Utilisateur, String> colEmail;
    @FXML private TableColumn<Utilisateur, String> colRole;
    @FXML private TableColumn<Utilisateur, String> colStatus;
    @FXML private TableColumn<Utilisateur, Void> colActions;

    @FXML private ComboBox<String> roleFilter;
    @FXML private Label lblTotal;

    private final UtilisateurService service = new UtilisateurService();
    private final ObservableList<Utilisateur> userList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        roleFilter.setItems(FXCollections.observableArrayList("Tous", "ADMINISTRATEUR", "ENSEIGNANT", "ETUDIANT"));
        roleFilter.setValue("Tous");

        setupColumns();
        loadData();
    }

    private void setupColumns() {
        colId.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getId())));
        colNom.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPrenom() + " " + d.getValue().getNom()));
        colEmail.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEmail()));
        
        colRole.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRole()));
        colRole.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add(item.equals("ADMINISTRATEUR") ? "badge-danger" : 
                            (item.equals("ENSEIGNANT") ? "badge-warning" : "badge-success"));
                    badge.setStyle("-fx-padding: 3 6; -fx-background-radius: 10px; -fx-text-fill: white; -fx-font-size: 10px;");
                    // Astuce couleurs si les classes CSS n'existent pas encore
                    if(item.equals("ADMINISTRATEUR")) badge.setStyle(badge.getStyle() + "-fx-background-color: #e74c3c;");
                    if(item.equals("ENSEIGNANT")) badge.setStyle(badge.getStyle() + "-fx-background-color: #f39c12;");
                    if(item.equals("ETUDIANT")) badge.setStyle(badge.getStyle() + "-fx-background-color: #27ae60;");
                    
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));

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
                    Utilisateur u = getTableView().getItems().get(getIndex());
                    openForm(u);
                });
                btnDelete.setOnAction(e -> {
                    Utilisateur u = getTableView().getItems().get(getIndex());
                    handleDelete(u);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tableUtilisateurs.setItems(userList);
    }

    private void loadData() {
        try {
            userList.clear();
            if ("Tous".equals(roleFilter.getValue())) {
                userList.addAll(service.getData());
            } else {
                userList.addAll(service.searchByRole(roleFilter.getValue()));
            }
            lblTotal.setText("Total: " + userList.size());
        } catch (SQLException e) {
            AlertUtil.showError("Erreur : " + e.getMessage());
        }
    }

    @FXML
    void handleFilter() {
        loadData();
    }

    @FXML
    void handleReset() {
        roleFilter.setValue("Tous");
        loadData();
    }

    @FXML
    void handleAdd() {
        openForm(null);
    }

    void handleDelete(Utilisateur u) {
        if (AlertUtil.showConfirmation("Supprimer l'utilisateur " + u.getPrenom() + " " + u.getNom() + " ?")) {
            try {
                service.deleteEntity(u);
                AlertUtil.showSuccess("Utilisateur supprimé !");
                loadData();
            } catch (SQLException e) {
                AlertUtil.showError(e.getMessage());
            }
        }
    }

    private void openForm(Utilisateur userToEdit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UtilisateurForm.fxml"));
            Parent root = loader.load();

            UtilisateurFormController controller = loader.getController();
            if (userToEdit != null) {
                controller.setUserToEdit(userToEdit);
            }

            Stage stage = new Stage();
            stage.setTitle(userToEdit != null ? "Modifier Utilisateur" : "Nouvel Utilisateur");
            stage.initModality(Modality.APPLICATION_MODAL);
            Scene scene = new Scene(root, 500, 450);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();
            loadData();
        } catch (IOException e) {
            AlertUtil.showError("Erreur affichage formulaire : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
