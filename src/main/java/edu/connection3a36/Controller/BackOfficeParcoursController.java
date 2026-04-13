package edu.connection3a36.Controller;

import edu.connection3a36.entities.Parcours;
import edu.connection3a36.services.ParcoursService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class BackOfficeParcoursController implements Initializable {
    @FXML
    private TableView<Parcours> tableParcours;
    @FXML
    private TableColumn<Parcours, String> colTitre, colType, colEtablissement;
    @FXML
    private TableColumn<Parcours, Void> colAction;
    @FXML
    private TextField filterInput;

    private final ParcoursService parcoursService = new ParcoursService();
    private ObservableList<Parcours> allParcours = FXCollections.observableArrayList();
    private int currentPage = 0;
    private final int itemsPerPage = 3;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colType.setCellValueFactory(new PropertyValueFactory<>("typeParcours"));
        colEtablissement.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getEtablissement() != null ? cellData.getValue().getEtablissement()
                        : cellData.getValue().getEntreprise()));
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnVoir = new Button("👁");
            {
                btnVoir.setOnAction(event -> mostrarDetalles(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty)
                    setGraphic(null);
                else
                    setGraphic(btnVoir);
            }
        });
        loadData();
    }

    private void loadData() {
        try {
            allParcours.setAll(parcoursService.getData());
            updateTable();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateTable() {
        int from = currentPage * itemsPerPage;
        int to = Math.min(from + itemsPerPage, allParcours.size());
        tableParcours.setItems(FXCollections.observableArrayList(allParcours.subList(from, Math.max(from, to))));
    }

    @FXML
    private void nextPage() {
        if ((currentPage + 1) * itemsPerPage < allParcours.size()) {
            currentPage++;
            updateTable();
        }
    }

    @FXML
    private void prevPage() {
        if (currentPage > 0) {
            currentPage--;
            updateTable();
        }
    }

    @FXML
    private void filtrerParcours() {
        String q = filterInput.getText().toLowerCase();
        try {
            List<Parcours> filtered = parcoursService.getData().stream()
                    .filter(p -> p.getTitre().toLowerCase().contains(q))
                    .collect(Collectors.toList());
            allParcours.setAll(filtered);
            currentPage = 0;
            updateTable();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void mostrarDetalles(Parcours p) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/DetailsParcours.fxml"));
            Parent view = loader.load();
            ((DetailsParcoursController) loader.getController()).setParcours(p);
            ((BorderPane) tableParcours.getScene().getRoot()).setCenter(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
