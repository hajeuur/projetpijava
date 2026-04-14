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

public class AdminObjectifsController {

    @FXML private TextField searchField;
    @FXML private TableView<Objectif> objectifsTable;
    @FXML private TableColumn<Objectif, String> colId;
    @FXML private TableColumn<Objectif, String> colTitre;
    @FXML private TableColumn<Objectif, String> colDescription;
    @FXML private TableColumn<Objectif, String> colStatut;
    @FXML private TableColumn<Objectif, String> colDebut;
    @FXML private TableColumn<Objectif, String> colFin;
    @FXML private Label countLabel;
    @FXML private Label atteintsLabel;
    @FXML private Label enCoursLabel;
    @FXML private Label abandonnesLabel;

    private final ObjectifDAO objectifDAO = new ObjectifDAO();
    private List<Objectif> allObjectifs;

    @FXML
    public void initialize() {
        setupColumns();
        loadData();
    }

    private void setupColumns() {
        colId.setCellValueFactory(d ->
                new SimpleStringProperty("PPD" + d.getValue().getId()));
        colTitre.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getTitre()));
        colDescription.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().getDescription() != null ?
                                d.getValue().getDescription() : "-"));
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

        // Colorer le statut
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    String color = switch (item) {
                        case "Atteint" -> "#198754";
                        case "EnCours" -> "#d97706";
                        default -> "#dc2626";
                    };
                    setStyle("-fx-text-fill: " + color +
                            "; -fx-font-weight: bold;");
                }
            }
        });

        // Readonly — pas de selection
        objectifsTable.setSelectionModel(null);
    }

    private void loadData() {
        try {
            allObjectifs = objectifDAO.findAll();
            updateTable(allObjectifs);
        } catch (Exception e) {
            showAlert("Erreur", e.getMessage());
        }
    }

    private void updateTable(List<Objectif> list) {
        objectifsTable.setItems(FXCollections.observableArrayList(list));

        long atteints = list.stream()
                .filter(o -> o.getStatut() == Statutobj.Atteint).count();
        long enCours = list.stream()
                .filter(o -> o.getStatut() == Statutobj.EnCours).count();
        long abandonnes = list.stream()
                .filter(o -> o.getStatut() == Statutobj.Abandonner).count();

        countLabel.setText(String.valueOf(list.size()));
        atteintsLabel.setText(String.valueOf(atteints));
        enCoursLabel.setText(String.valueOf(enCours));
        abandonnesLabel.setText(String.valueOf(abandonnes));
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().trim().toLowerCase();
        if (query.isEmpty()) {
            updateTable(allObjectifs);
        } else {
            List<Objectif> filtered = allObjectifs.stream()
                    .filter(o -> o.getTitre().toLowerCase().contains(query) ||
                            (o.getDescription() != null &&
                                    o.getDescription().toLowerCase().contains(query)))
                    .toList();
            updateTable(filtered);
        }
    }

    @FXML
    private void handleShowAll() {
        searchField.clear();
        updateTable(allObjectifs);
    }

    @FXML
    private void handleRetour() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/admin_dashboard.fxml"));
            Main.primaryStage.setScene(new Scene(loader.load(), 1100, 700));
        } catch (Exception e) { showAlert("Erreur", e.getMessage()); }
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title); a.setContentText(msg); a.showAndWait();
    }
}