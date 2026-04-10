package com.esprit.controllers;

import com.esprit.dao.UtilisateurDAO;
import com.esprit.models.Utilisateur;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class BackOfficeController implements Initializable {

    @FXML private TableView<Utilisateur> tableView;
    @FXML private TableColumn<Utilisateur, Integer> colId;
    @FXML private TableColumn<Utilisateur, String> colNom;
    @FXML private TableColumn<Utilisateur, String> colPrenom;
    @FXML private TableColumn<Utilisateur, String> colEmail;
    @FXML private TableColumn<Utilisateur, String> colRole;
    @FXML private TableColumn<Utilisateur, String> colStatus;
    @FXML private TableColumn<Utilisateur, Void> colActions;
    @FXML private TextField searchField;

    private UtilisateurDAO dao = new UtilisateurDAO();
    private ObservableList<Utilisateur> data = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        ajouterColonneActions();
        chargerDonnees();
    }

    private void chargerDonnees() {
        data.clear();
        data.addAll(dao.getAll());
        tableView.setItems(data);
    }

    private void ajouterColonneActions() {
        colActions.setCellFactory(col -> new TableCell<>() {
            Button btnModifier = new Button("Modifier");
            Button btnSupprimer = new Button("Désactiver");
            Button btnVoir = new Button("Voir");
            HBox hbox = new HBox(5, btnVoir, btnModifier, btnSupprimer);

            {
                btnModifier.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 4;");
                btnSupprimer.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 4;");
                btnVoir.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-background-radius: 4;");

                btnModifier.setOnAction(e -> handleModifier(getTableView().getItems().get(getIndex())));
                btnSupprimer.setOnAction(e -> handleDesactiver(getTableView().getItems().get(getIndex())));
                btnVoir.setOnAction(e -> handleVoir(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : hbox);
            }
        });
    }

    private void handleModifier(Utilisateur u) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/ModifierUser.fxml"));
            Parent root = loader.load();
            ModifierUserController controller = loader.getController();
            controller.setUtilisateur(u, this);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Modifier utilisateur");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleVoir(Utilisateur u) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/ProfilUser.fxml"));
            Parent root = loader.load();
            ProfilUserController controller = loader.getController();
            controller.setUtilisateur(u);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Profil utilisateur");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleDesactiver(Utilisateur u) {
        u.setStatus(u.getStatus().equals("actif") ? "desactiver" : "actif");
        dao.modifier(u);
        chargerDonnees();
    }

    @FXML
    public void handleAjouter() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/Inscription.fxml"));
            Parent root = loader.load();
            InscriptionController controller = loader.getController();
            controller.setBackOfficeController(this);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Ajouter utilisateur");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleSearch() {
        String keyword = searchField.getText().toLowerCase();
        List<Utilisateur> tous = dao.getAll();
        data.clear();
        for (Utilisateur u : tous) {
            if (u.getNom().toLowerCase().contains(keyword) ||
                    u.getEmail().toLowerCase().contains(keyword) ||
                    u.getRole().toLowerCase().contains(keyword)) {
                data.add(u);
            }
        }
        tableView.setItems(data);
    }

    @FXML
    public void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) tableView.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void refreshTable() {
        chargerDonnees();
    }
}