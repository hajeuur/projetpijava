package edu.connection3a36.Controller;

import edu.connection3a36.entities.Projet;
import edu.connection3a36.entities.Ressource;
import edu.connection3a36.services.ProjetService;
import edu.connection3a36.services.RessourceService;
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

public class BackOfficeProjetsController implements Initializable {
    @FXML
    private TableView<Projet> tableProjets;
    @FXML
    private TableColumn<Projet, String> colTitre, colType, colTechno;
    @FXML
    private TableColumn<Projet, Void> colAction;
    @FXML
    private TextField filterInput;

    private final ProjetService projetService = new ProjetService();
    private final RessourceService ressourceService = new RessourceService();
    private ObservableList<Projet> allProjets = FXCollections.observableArrayList();
    private int currentPage = 0;
    private final int itemsPerPage = 3;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colTechno.setCellValueFactory(new PropertyValueFactory<>("technologies"));
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
            allProjets.setAll(projetService.getData());
            updateTable();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateTable() {
        int from = currentPage * itemsPerPage;
        int to = Math.min(from + itemsPerPage, allProjets.size());
        tableProjets.setItems(FXCollections.observableArrayList(allProjets.subList(from, Math.max(from, to))));
    }

    @FXML
    private void nextPage() {
        if ((currentPage + 1) * itemsPerPage < allProjets.size()) {
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
    private void filtrar() {
        String q = filterInput.getText().toLowerCase();
        try {
            List<Projet> filtered = projetService.getData().stream()
                    .filter(p -> p.getTitre().toLowerCase().contains(q)
                            || p.getTechnologies().toLowerCase().contains(q))
                    .collect(Collectors.toList());
            allProjets.setAll(filtered);
            currentPage = 0;
            updateTable();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void genererPDF() {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setInitialFileName("Rapport_MentorAI.pdf");
        java.io.File file = fc.showSaveDialog(tableProjets.getScene().getWindow());
        if (file == null)
            return;
        com.itextpdf.text.Document doc = new com.itextpdf.text.Document();
        try {
            com.itextpdf.text.pdf.PdfWriter.getInstance(doc, new java.io.FileOutputStream(file));
            doc.open();
            doc.add(new com.itextpdf.text.Paragraph("RAPPORT MENTOR AI\n\n"));
            for (Projet p : allProjets) {
                doc.add(new com.itextpdf.text.Paragraph(p.getTitre() + " [" + p.getType() + "]"));
                List<Ressource> res = ressourceService.getByProjetId(p.getId());
                for (Ressource r : res) {
                    doc.add(new com.itextpdf.text.Paragraph(" - " + r.getNom() + " (" + r.getTypeRessource() + ")"));
                }
                doc.add(new com.itextpdf.text.Paragraph("\n"));
            }
            doc.close();
            new Alert(Alert.AlertType.INFORMATION, "PDF généré !").show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void mostrarDetalles(Projet p) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/DetailsProjet.fxml"));
            Parent view = loader.load();
            ((DetailsProjetController) loader.getController()).setProjet(p);
            ((BorderPane) tableProjets.getScene().getRoot()).setCenter(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
