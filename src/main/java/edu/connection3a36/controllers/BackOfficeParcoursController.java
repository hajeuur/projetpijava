package edu.connection3a36.controllers;

import edu.connection3a36.entities.Parcours;
import edu.connection3a36.services.ParcoursService;
import edu.connection3a36.services.ProjetService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.chart.*;
import javafx.scene.layout.*;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class BackOfficeParcoursController implements Initializable {

    @FXML
    private TableView<Parcours> tableParcours;
    @FXML
    private TableColumn<Parcours, Integer> colIndex;
    @FXML
    private TableColumn<Parcours, String> colTitreInstitution, colTypeDiplome, colPeriode, colProjetsAssocies;
    @FXML
    private TableColumn<Parcours, Void> colAction;

    @FXML
    private TextField filterInput;
    @FXML
    private ComboBox<String> comboType;
    @FXML
    private HBox paginationBox;
    @FXML
    private Label lblTotalParcours, lblTotalEtab, lblTotalProjets;
    @FXML
    private ProgressBar progressProjets;

    @FXML
    private PieChart pieChartParcours;
    @FXML
    private BarChart<String, Number> barChartProjets;

    private final ParcoursService parcoursService = new ParcoursService();
    private final ProjetService projetService = new ProjetService();
    private ObservableList<Parcours> allData = FXCollections.observableArrayList();
    private ObservableList<Parcours> filteredData = FXCollections.observableArrayList();

    private int currentPage = 0;
    private final int itemsPerPage = 5;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumns();
        setupFilters();
        loadData();
    }

    private void setupColumns() {
        colIndex.setCellValueFactory(p -> new javafx.beans.property.SimpleObjectProperty<>(1));
        colIndex.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Integer i, boolean e) {
                super.updateItem(i, e);
                if (e) setText(null);
                else setText(String.valueOf(getIndex() + 1 + (currentPage * itemsPerPage)));
            }
        });

        colTitreInstitution.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean e) {
                super.updateItem(s, e);
                if (e) setGraphic(null);
                else {
                    Parcours p = getTableView().getItems().get(getIndex());
                    VBox v = new VBox(2);
                    Label t = new Label(p.getTitre());
                    t.setStyle("-fx-font-weight: bold;");
                    String inst = p.getEtablissement() != null ? p.getEtablissement() : p.getEntreprise();
                    Label sub = new Label(inst != null ? inst : "---");
                    sub.setStyle("-fx-font-size: 11; -fx-text-fill: #64748b;");
                    v.getChildren().addAll(t, sub);
                    setGraphic(v);
                }
            }
        });

        colTypeDiplome.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean e) {
                super.updateItem(s, e);
                if (e) setGraphic(null);
                else {
                    Parcours p = getTableView().getItems().get(getIndex());
                    VBox v = new VBox(5);
                    Label tag = new Label(p.getTypeParcours().toUpperCase());
                    String color = p.getTypeParcours().equalsIgnoreCase("formation") ? "#3b82f6" : "#0ea5e9";
                    tag.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-padding: 2 8; -fx-background-radius: 10; -fx-font-size: 9;");
                    Label d = new Label(p.getDiplome() != null ? p.getDiplome() : p.getPoste());
                    d.setStyle("-fx-font-size: 11;");
                    v.getChildren().addAll(tag, d);
                    setGraphic(v);
                }
            }
        });

        colPeriode.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean e) {
                super.updateItem(s, e);
                if (e) setGraphic(null);
                else {
                    Parcours p = getTableView().getItems().get(getIndex());
                    String start = p.getDateDebut() != null ? p.getDateDebut().toString() : "...";
                    String end = p.getDateFin() != null ? p.getDateFin().toString() : "Présent";
                    Label l = new Label("📅 " + start + " - " + end);
                    l.setStyle("-fx-font-size: 11;");
                    setGraphic(l);
                }
            }
        });

        colProjetsAssocies.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean e) {
                super.updateItem(s, e);
                if (e) setGraphic(null);
                else {
                    Parcours p = getTableView().getItems().get(getIndex());
                    try {
                        int count = projetService.getByParcoursId(p.getId()).size();
                        Label l = new Label(count + " Projets");
                        l.setStyle("-fx-background-color: #f1f5f9; -fx-padding: 3 10; -fx-background-radius: 5; -fx-font-size: 11;");
                        setGraphic(l);
                    } catch (SQLException ex) {
                        setGraphic(new Label("0 Projets"));
                    }
                }
            }
        });

        colAction.setCellFactory(c -> new TableCell<>() {
            private final Button btn = new Button("Voir");
            {
                btn.setStyle("-fx-background-color: #ffffff; -fx-border-color: #3b82f6; -fx-text-fill: #3b82f6; -fx-border-radius: 5; -fx-cursor: hand;");
                btn.setOnAction(e -> mostrarDetalles(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void v, boolean e) {
                super.updateItem(v, e);
                if (e) setGraphic(null);
                else setGraphic(btn);
            }
        });
    }

    private void setupFilters() {
        comboType.setItems(FXCollections.observableArrayList("Tous", "Formation", "Stage", "Alternance", "Emploi", "Personnel"));
        comboType.setOnAction(e -> filtrerParcours());
    }

    @FXML
    public void loadData() {
        try {
            List<Parcours> data = parcoursService.getData();
            allData.setAll(data);
            filteredData.setAll(data);
            calculateStats();
            updateCharts();
            updateTable();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void calculateStats() {
        lblTotalParcours.setText(String.valueOf(allData.size()));
        long estabs = allData.stream().map(p -> p.getEtablissement() != null ? p.getEtablissement() : p.getEntreprise())
                .filter(s -> s != null && !s.isEmpty()).distinct().count();
        lblTotalEtab.setText(String.valueOf(estabs));
        int projects = 0;
        for (Parcours p : allData) {
            try {
                projects += projetService.getByParcoursId(p.getId()).size();
            } catch (SQLException ignored) {}
        }
        lblTotalProjets.setText(String.valueOf(projects));

        if (progressProjets != null && !allData.isEmpty()) {
            double ratio = (double) projects / (allData.size() * 2.0);
            progressProjets.setProgress(Math.min(1.0, ratio));
        }
    }

    private void updateCharts() {
        Map<String, Long> counts = allData.stream()
                .collect(Collectors.groupingBy(Parcours::getTypeParcours, Collectors.counting()));
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        counts.forEach((type, count) -> pieData.add(new PieChart.Data(type, count)));
        pieChartParcours.setData(pieData);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Projets");

        allData.stream()
                .sorted((p1, p2) -> {
                    try {
                        return Integer.compare(projetService.getByParcoursId(p2.getId()).size(),
                                projetService.getByParcoursId(p1.getId()).size());
                    } catch (SQLException e) { return 0; }
                })
                .limit(5)
                .forEach(p -> {
                    try {
                        int count = projetService.getByParcoursId(p.getId()).size();
                        String label = p.getTitre().length() > 20 ? p.getTitre().substring(0, 17) + "..." : p.getTitre();
                        series.getData().add(new XYChart.Data<>(label, count));
                    } catch (SQLException ignored) {}
                });

        barChartProjets.getData().setAll(series);
    }

    private void updateTable() {
        int from = currentPage * itemsPerPage;
        int to = Math.min(from + itemsPerPage, filteredData.size());
        if (from >= filteredData.size()) {
            currentPage = 0; from = 0;
            to = Math.min(itemsPerPage, filteredData.size());
        }
        tableParcours.getItems().setAll(filteredData.subList(from, to));
        updatePagination();
    }

    private void updatePagination() {
        paginationBox.getChildren().clear();
        int totalPages = (int) Math.ceil((double) filteredData.size() / itemsPerPage);

        Button prev = new Button("Précédent");
        prev.setDisable(currentPage == 0);
        prev.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 5; -fx-padding: 5 15; -fx-cursor: hand;");
        prev.setOnAction(e -> { currentPage--; updateTable(); });
        paginationBox.getChildren().add(prev);

        for (int i = 0; i < totalPages; i++) {
            final int idx = i;
            Button b = new Button(String.valueOf(i + 1));
            if (i == currentPage) b.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 5; -fx-font-weight: bold; -fx-min-width: 35;");
            else b.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 5; -fx-text-fill: #64748b; -fx-cursor: hand; -fx-min-width: 35;");
            b.setOnAction(e -> { currentPage = idx; updateTable(); });
            paginationBox.getChildren().add(b);
        }

        Button next = new Button("Suivant");
        next.setDisable(currentPage >= totalPages - 1 || totalPages == 0);
        next.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 5; -fx-padding: 5 15; -fx-cursor: hand;");
        next.setOnAction(e -> { currentPage++; updateTable(); });
        paginationBox.getChildren().add(next);
    }

    @FXML
    private void filtrerParcours() {
        String query = filterInput.getText().toLowerCase();
        String type = comboType.getValue();
        List<Parcours> filtered = allData.stream()
                .filter(p -> (query.isEmpty() || p.getTitre().toLowerCase().contains(query)
                        || (p.getEtablissement() != null && p.getEtablissement().toLowerCase().contains(query))))
                .filter(p -> (type == null || type.equals("Tous") || p.getTypeParcours().equalsIgnoreCase(type)))
                .collect(Collectors.toList());
        filteredData.setAll(filtered);
        currentPage = 0;
        updateTable();
    }

    @FXML
    private void nouveauParcours() {
        loadView("/AjouterParcours.fxml");
    }

    private void mostrarDetalles(Parcours p) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/DetailsParcours.fxml"));
            Parent view = loader.load();
            ((DetailsParcoursController) loader.getController()).setParcours(p);
            MainController.getInstance().loadInContentArea(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadView(String fxml) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxml));
            MainController.getInstance().loadInContentArea(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
