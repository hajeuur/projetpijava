package edu.mentorai.Controller;

import edu.mentorai.Main;
import edu.mentorai.entities.Objectif;
import edu.mentorai.entities.Statutobj;
import edu.mentorai.interfaces.ObjectifDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;

import java.util.List;

public class AdminDashboardController {

    @FXML private Label totalObjectifsLabel;
    @FXML private Label atteintsLabel;
    @FXML private Label enCoursLabel;
    @FXML private Label tauxLabel;
    @FXML private TableView<Objectif> recentTable;
    @FXML private TableColumn<Objectif, String> colId;
    @FXML private TableColumn<Objectif, String> colTitre;
    @FXML private TableColumn<Objectif, String> colStatut;
    @FXML private TableColumn<Objectif, String> colDebut;
    @FXML private TableColumn<Objectif, String> colFin;

    private final ObjectifDAO objectifDAO = new ObjectifDAO();

    @FXML
    public void initialize() {
        setupTable();
        loadStats();
    }

    private void setupTable() {
        colId.setCellValueFactory(d ->
                new SimpleStringProperty("PPD" + d.getValue().getId()));
        colTitre.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getTitre()));
        colStatut.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getStatut().getValue()));
        colDebut.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().getDatedebut() != null ?
                                d.getValue().getDatedebut().toString() : "-"));
        colFin.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().getDatefin() != null ?
                                d.getValue().getDatefin().toString() : "-"));

        // Style readonly — pas de selection
        recentTable.setSelectionModel(null);
    }

    private void loadStats() {
        try {
            List<Objectif> all = objectifDAO.findAll();

            long atteints = all.stream()
                    .filter(o -> o.getStatut() == Statutobj.Atteint).count();
            long enCours = all.stream()
                    .filter(o -> o.getStatut() == Statutobj.EnCours).count();
            int taux = all.isEmpty() ? 0 :
                    (int) Math.round((atteints * 100.0) / all.size());

            totalObjectifsLabel.setText(String.valueOf(all.size()));
            atteintsLabel.setText(String.valueOf(atteints));
            enCoursLabel.setText(String.valueOf(enCours));
            tauxLabel.setText(taux + "%");

            recentTable.setItems(FXCollections.observableArrayList(all));

        } catch (Exception e) {
            showAlert("Erreur", e.getMessage());
        }
    }

    @FXML
    private void handleObjectifs() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/admin_objectifs.fxml"));
            Main.primaryStage.setScene(new Scene(loader.load(), 1200, 750));
        } catch (Exception e) { showAlert("Erreur", e.getMessage()); }
    }

    @FXML
    private void handleTaches() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/admin_taches.fxml"));
            Main.primaryStage.setScene(new Scene(loader.load(), 1200, 750));
        } catch (Exception e) { showAlert("Erreur", e.getMessage()); }
    }

    @FXML
    private void handleVueUtilisateur() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/objectif_list.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 750);
            ObjectifListController ctrl = loader.getController();
            ctrl.setUtilisateurId(1);
            ctrl.loadData();
            Main.primaryStage.setScene(scene);
        } catch (Exception e) { showAlert("Erreur", e.getMessage()); }
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title); a.setContentText(msg); a.showAndWait();
    }
}