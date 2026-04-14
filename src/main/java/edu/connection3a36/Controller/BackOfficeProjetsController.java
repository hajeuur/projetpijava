package edu.connection3a36.Controller;

import edu.connection3a36.entities.Projet;
import edu.connection3a36.entities.Ressource;
import edu.connection3a36.services.ProjetService;
import edu.connection3a36.services.RessourceService;
import edu.connection3a36.Controller.DetailsProjetController;
import edu.connection3a36.Controller.AfficherProjetsController;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;

import java.io.IOException;
import java.io.File;
import java.net.URL;
import javafx.stage.FileChooser;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.FileOutputStream;

public class BackOfficeProjetsController implements Initializable {
    @FXML
    private TableView<Projet> tableProjets;
    @FXML
    private TableColumn<Projet, Integer> colIndex;
    @FXML
    private TableColumn<Projet, String> colTitre, colUtilisateur, colType, colRessources;
    @FXML
    private TableColumn<Projet, Void> colAction;
    @FXML
    private BarChart<String, Number> barChartUsers;
    @FXML
    private BarChart<String, Number> barChartResources;
    @FXML
    private PieChart pieChartTypes;
    @FXML
    private TableView<Projet> tableSummary;
    @FXML
    private TableColumn<Projet, String> colTopUser, colTopProjet, colProjResCount;
    @FXML
    private HBox paginationBoxProjets;
    @FXML
    private TextField filterInput;

    private final ProjetService projetService = new ProjetService();
    private final RessourceService ressourceService = new RessourceService();
    private ObservableList<Projet> allProjets = FXCollections.observableArrayList();
    private ObservableList<Projet> filteredProjets = FXCollections.observableArrayList();
    private int currentPage = 0;
    private final int itemsPerPage = 5;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupColumns();
        loadData();
    }

    private void setupColumns() {
        // Col #
        colIndex.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty)
                    setText(null);
                else
                    setText(String.valueOf(getIndex() + 1 + (currentPage * itemsPerPage)));
            }
        });

        // Col Titre
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colTitre.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Projet p = getTableView().getItems().get(getIndex());
                    VBox vb = new VBox(2);
                    Label title = new Label(item);
                    title.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b;");
                    Label date = new Label(p.getDateCreation() != null ? p.getDateCreation().toString() : "No date");
                    date.setStyle("-fx-font-size: 10; -fx-text-fill: #64748b;");
                    vb.getChildren().addAll(title, date);
                    setGraphic(vb);
                }
            }
        });

        // Col Utilisateur (Mock)
        colUtilisateur.setCellValueFactory(
                param -> new SimpleStringProperty("admin admin\nadmin@mentor.ai"));
        colUtilisateur.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null)
                    setGraphic(null);
                else {
                    Label l = new Label(item);
                    l.setStyle("-fx-font-size: 11; -fx-text-fill: #64748b;");
                    setGraphic(l);
                }
            }
        });

        // Col Type (Tags)
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colType.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null)
                    setGraphic(null);
                else {
                    Label tag = new Label(item.toUpperCase());
                    String color = item.toLowerCase().contains("web") ? "#06b6d4"
                            : (item.toLowerCase().contains("mobile") ? "#0ea5e9" : "#64748b");
                    tag.setStyle("-fx-background-color: " + color
                            + "; -fx-text-fill: white; -fx-padding: 3 10; -fx-background-radius: 15; -fx-font-size: 10; -fx-font-weight: bold;");
                    setGraphic(tag);
                }
            }
        });

        // Col Ressources
        colRessources.setCellValueFactory(param -> new SimpleStringProperty(""));
        colRessources.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty)
                    setGraphic(null);
                else {
                    Projet p = getTableView().getItems().get(getIndex());
                    try {
                        List<Ressource> res = ressourceService.getByProjetId(p.getId());
                        if (res.isEmpty()) {
                            setGraphic(new Label("Aucune ressource"));
                        } else {
                            VBox vb = new VBox(2);
                            for (Ressource r : res) {
                                Label l = new Label("📄 " + r.getNom() + " (" + r.getTypeRessource() + ")");
                                l.setStyle("-fx-font-size: 11; -fx-text-fill: #1e293b;");
                                vb.getChildren().add(l);
                            }
                            setGraphic(vb);
                        }
                    } catch (SQLException e) {
                        setGraphic(null);
                    }
                }
            }
        });

        // Col Action
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnVoir = new Button("👁");
            {
                btnVoir.setStyle(
                        "-fx-background-color: transparent; -fx-border-color: #06b6d4; -fx-text-fill: #06b6d4; -fx-border-radius: 5; -fx-cursor: hand;");
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
    }

    private void editProjet(Projet p) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AfficherProjets.fxml"));
            Parent view = loader.load();
            ((AfficherProjetsController) loader.getController()).initData(null);
            ((AfficherProjetsController) loader.getController()).selectProject(p);
            ((BorderPane) tableProjets.getScene().getRoot()).setCenter(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadData() {
        try {
            allProjets.setAll(projetService.getData());
            filteredProjets.setAll(allProjets);
            updateTable();
            updateStats();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateStats() {
        if (allProjets.isEmpty())
            return;

        // 1. Users Stats
        XYChart.Series<String, Number> userSeries = new XYChart.Series<>();
        userSeries.setName("Projets");
        userSeries.getData().add(new XYChart.Data<>("arsl arslen", 3));
        userSeries.getData().add(new XYChart.Data<>("Hejer Hejer", 1));
        barChartUsers.getData().setAll(userSeries);

        // 2. Project Resources Stats
        XYChart.Series<String, Number> resSeries = new XYChart.Series<>();
        resSeries.setName("Ressources");
        allProjets.stream()
                .sorted((p1, p2) -> {
                    try {
                        return Integer.compare(ressourceService.getByProjetId(p2.getId()).size(),
                                ressourceService.getByProjetId(p1.getId()).size());
                    } catch (SQLException e) {
                        return 0;
                    }
                })
                .limit(5)
                .forEach(p -> {
                    try {
                        int count = ressourceService.getByProjetId(p.getId()).size();
                        String label = p.getTitre().length() > 15 ? p.getTitre().substring(0, 12) + "..."
                                : p.getTitre();
                        resSeries.getData().add(new XYChart.Data<>(label, count));
                    } catch (SQLException ignored) {
                    }
                });
        barChartResources.getData().setAll(resSeries);

        // 3. Types Pie Chart (Donut-like)
        java.util.Map<String, Long> typeCounts = allProjets.stream()
                .collect(java.util.stream.Collectors.groupingBy(p -> p.getType() != null ? p.getType() : "Inconnu",
                        java.util.stream.Collectors.counting()));
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        typeCounts.forEach((k, v) -> pieData.add(new PieChart.Data(k, v)));
        pieChartTypes.setData(pieData);

        // 4. Summary Table
        tableSummary.setItems(FXCollections.observableArrayList(
                allProjets.stream().limit(Math.min(4, allProjets.size())).collect(Collectors.toList())));
        colTopUser.setCellValueFactory(p -> new SimpleStringProperty(
                p.getValue().getTitre().contains("chatbot") ? "arsl arslen" : "Hejer Hejer"));
        colTopProjet.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colProjResCount.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean e) {
                super.updateItem(s, e);
                if (e)
                    setGraphic(null);
                else {
                    Projet p = getTableView().getItems().get(getIndex());
                    try {
                        int count = ressourceService.getByProjetId(p.getId()).size();
                        Label tag = new Label(count + " docs");
                        tag.setStyle(
                                "-fx-background-color: #ecfdf5; -fx-text-fill: #059669; -fx-padding: 3 10; -fx-background-radius: 10; -fx-font-weight: bold;");
                        setGraphic(tag);
                    } catch (SQLException ex) {
                        setGraphic(null);
                    }
                }
            }
        });
    }

    private void updateTable() {
        int from = currentPage * itemsPerPage;
        int to = Math.min(from + itemsPerPage, filteredProjets.size());
        if (from >= filteredProjets.size() && !filteredProjets.isEmpty()) {
            currentPage = 0;
            from = 0;
            to = Math.min(itemsPerPage, filteredProjets.size());
        }
        tableProjets.setItems(FXCollections.observableArrayList(filteredProjets.subList(from, to)));
        updatePagination();
    }

    private void updatePagination() {
        if (paginationBoxProjets == null)
            return;
        paginationBoxProjets.getChildren().clear();
        int totalPages = (int) Math.ceil((double) filteredProjets.size() / itemsPerPage);

        for (int i = 0; i < totalPages; i++) {
            final int idx = i;
            Button b = new Button(String.valueOf(i + 1));
            b.setStyle(i == currentPage ? "-fx-background-color: #1e3a8a; -fx-text-fill: white; -fx-font-weight: bold;"
                    : "-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-padding: 5 10; -fx-cursor: hand;");
            b.setOnAction(e -> {
                currentPage = idx;
                updateTable();
            });
            paginationBoxProjets.getChildren().add(b);
        }
    }

    @FXML
    private void filtrar() {
        String q = filterInput.getText().toLowerCase();
        try {
            List<Projet> data = projetService.getData().stream()
                    .filter(p -> p.getTitre().toLowerCase().contains(q)
                            || p.getTechnologies().toLowerCase().contains(q))
                    .collect(Collectors.toList());
            filteredProjets.setAll(data);
            currentPage = 0;
            updateTable();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void genererPDF() {
        FileChooser fc = new FileChooser();
        fc.setInitialFileName("Rapport_MentorAI.pdf");
        File file = fc.showSaveDialog(tableProjets.getScene().getWindow());
        if (file == null)
            return;
        Document doc = new Document();
        try {
            PdfWriter.getInstance(doc, new FileOutputStream(file));
            doc.open();
            doc.add(new Paragraph("RAPPORT MENTOR AI\n\n"));
            for (Projet p : allProjets) {
                doc.add(new Paragraph(p.getTitre() + " [" + p.getType() + "]"));
                List<Ressource> res = ressourceService.getByProjetId(p.getId());
                for (Ressource r : res) {
                    doc.add(new Paragraph(" - " + r.getNom() + " (" + r.getTypeRessource() + ")"));
                }
                doc.add(new Paragraph("\n"));
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
            StackPane center = (StackPane) tableProjets.getScene()
                    .lookup("#centerContent");
            if (center != null)
                center.getChildren().setAll(view);
            else {
                // Fallback to BorderPane center if centerContent not found
                BorderPane root = (BorderPane) tableProjets.getScene().lookup("#mainContainer");
                if (root != null)
                    root.setCenter(view);
                else
                    ((BorderPane) tableProjets.getScene().getRoot()).setCenter(view);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
