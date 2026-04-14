package edu.mentorai.Controller;

import edu.mentorai.Main;
import edu.mentorai.entities.Etat;
import edu.mentorai.entities.Tache;
import edu.mentorai.tools.DatabaseConnection;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdminTachesController {

    @FXML private TableView<Tache> tachesTable;
    @FXML private TableColumn<Tache, String> colId;
    @FXML private TableColumn<Tache, String> colOrdre;
    @FXML private TableColumn<Tache, String> colTitre;
    @FXML private TableColumn<Tache, String> colDescription;
    @FXML private TableColumn<Tache, String> colEtat;
    @FXML private TableColumn<Tache, String> colProgramme;
    @FXML private Label totalLabel;
    @FXML private Label realiséesLabel;
    @FXML private Label enCoursLabel;
    @FXML private Label abandonnesLabel;

    @FXML
    public void initialize() {
        setupColumns();
        loadData();
    }

    private void setupColumns() {
        colId.setCellValueFactory(d ->
                new SimpleStringProperty(String.valueOf(d.getValue().getId())));
        colOrdre.setCellValueFactory(d ->
                new SimpleStringProperty(String.valueOf(d.getValue().getOrdre())));
        colTitre.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getTitre()));
        colDescription.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().getDescription() != null ?
                                d.getValue().getDescription() : "-"));
        colEtat.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getEtat().getValue()));
        colProgramme.setCellValueFactory(d ->
                new SimpleStringProperty(
                        "Prog-" + d.getValue().getProgrammeId()));

        // Colorer l'etat
        colEtat.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setStyle("");
                } else {
                    setText(item);
                    String color = switch (item) {
                        case "realisee" -> "#198754";
                        case "encours" -> "#d97706";
                        default -> "#dc2626";
                    };
                    setStyle("-fx-text-fill: " + color +
                            "; -fx-font-weight: bold;");
                }
            }
        });

        // Readonly
        tachesTable.setSelectionModel(null);
    }

    private void loadData() {
        try {
            List<Tache> all = loadAllTaches();
            tachesTable.setItems(FXCollections.observableArrayList(all));

            long realisees = all.stream()
                    .filter(t -> t.getEtat() == Etat.realisee).count();
            long encours = all.stream()
                    .filter(t -> t.getEtat() == Etat.encours).count();
            long abandonnes = all.stream()
                    .filter(t -> t.getEtat() == Etat.Abandonner).count();

            totalLabel.setText(String.valueOf(all.size()));
            realiséesLabel.setText(String.valueOf(realisees));
            enCoursLabel.setText(String.valueOf(encours));
            abandonnesLabel.setText(String.valueOf(abandonnes));

        } catch (Exception e) {
            showAlert("Erreur", e.getMessage());
        }
    }

    private List<Tache> loadAllTaches() throws SQLException {
        List<Tache> list = new ArrayList<>();
        String sql = "SELECT * FROM tache ORDER BY programme_id, ordre ASC";
        try (Statement stmt = DatabaseConnection.getInstance().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Tache t = new Tache();
                t.setId(rs.getInt("id"));
                t.setOrdre(rs.getInt("ordre"));
                t.setTitre(rs.getString("titre"));
                t.setDescription(rs.getString("description"));
                t.setEtat(Etat.fromValue(rs.getString("etat")));
                t.setProgrammeId(rs.getInt("programme_id"));
                list.add(t);
            }
        }
        return list;
    }

    @FXML
    private void handleRetour() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/admin_dashboard.fxml"));
            Main.primaryStage.setScene(new Scene(loader.load(), 1200, 750));
        } catch (Exception e) { showAlert("Erreur", e.getMessage()); }
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title); a.setContentText(msg); a.showAndWait();
    }
}