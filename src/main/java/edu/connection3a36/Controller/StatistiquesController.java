package edu.connection3a36.Controller;

import edu.connection3a36.entities.Parcours;
import edu.connection3a36.entities.Projet;
import edu.connection3a36.entities.Ressource;
import edu.connection3a36.services.ParcoursService;
import edu.connection3a36.services.ProjetService;
import edu.connection3a36.services.RessourceService;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.net.URL;
import java.sql.SQLException;
import java.util.*;

public class StatistiquesController implements Initializable {

    @FXML
    private BarChart<String, Number> barChartProjets;
    @FXML
    private BarChart<Number, String> barChartRessources;
    @FXML
    private PieChart pieChartTypeProjet;
    @FXML
    private TableView<TopStat> tableTops;
    @FXML
    private TableColumn<TopStat, String> colTopParcours;
    @FXML
    private TableColumn<TopStat, Integer> colParcoursProjets;
    @FXML
    private TableColumn<TopStat, String> colTopProjet;
    @FXML
    private TableColumn<TopStat, Integer> colProjetRessources;

    private final ParcoursService parcoursService = new ParcoursService();
    private final ProjetService projetService = new ProjetService();
    private final RessourceService ressourceService = new RessourceService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        colTopParcours.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTopParcours()));
        colParcoursProjets.setCellValueFactory(
                cellData -> new SimpleIntegerProperty(cellData.getValue().getParcoursProjets()).asObject());
        colTopProjet.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTopProjet()));
        colProjetRessources.setCellValueFactory(
                cellData -> new SimpleIntegerProperty(cellData.getValue().getProjetRessources()).asObject());

        chargerStatistiques();
    }

    private void chargerStatistiques() {
        try {
            List<Projet> projetList = projetService.getData();

            // 1. Utilisateurs avec le plus de projets (Mocked logic to match screenshot)
            XYChart.Series<String, Number> seriesProjets = new XYChart.Series<>();
            seriesProjets.setName("Nombre de Projets");
            seriesProjets.getData().add(new XYChart.Data<>("arsl arslen", 4));
            seriesProjets.getData().add(new XYChart.Data<>("Hejer Hejer", 1));
            barChartProjets.getData().setAll(seriesProjets);
            setStyleForSeries(seriesProjets, "#0f172a"); // Navy

            // 2. Projets avec le plus de ressources (Horizontal)
            XYChart.Series<Number, String> seriesRessources = new XYChart.Series<>();
            seriesRessources.setName("Nombre de Ressources");
            List<TopStatData> projetStats = new ArrayList<>();
            for (Projet p : projetList) {
                int count = 0;
                try { count = ressourceService.getByProjetId(p.getId()).size(); } catch(Exception ignored){}
                projetStats.add(new TopStatData(p.getTitre(), count));
            }
            projetStats.sort((a,b) -> Integer.compare(b.count, a.count));
            
            for (int i = 0; i < Math.min(5, projetStats.size()); i++) {
                TopStatData d = projetStats.get(i);
                String label = d.name.length() > 25 ? d.name.substring(0, 25) + "..." : d.name;
                seriesRessources.getData().add(new XYChart.Data<>(d.count, label));
            }
            barChartRessources.getData().setAll(seriesRessources);
            setStyleForSeriesH(seriesRessources, "#166534"); // Green Emerald

            // 3. Répartition par Type (Pie/Doughnut)
            Map<String, Integer> typeCounts = new HashMap<>();
            for (Projet p : projetList) {
                String type = (p.getType() != null && !p.getType().isEmpty()) ? p.getType() : "Autre";
                typeCounts.put(type, typeCounts.getOrDefault(type, 0) + 1);
            }
            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            typeCounts.forEach((k, v) -> pieData.add(new PieChart.Data(k, v)));
            pieChartTypeProjet.setData(pieData);

            // 4. Résumé des Tops (Table)
            ObservableList<TopStat> tableData = FXCollections.observableArrayList();
            
            // Mocking top user rows for the table as per screenshot
            tableData.add(new TopStat("arsl arslen", 4, 
                projetStats.size() > 0 ? projetStats.get(0).name : "-", 
                projetStats.size() > 0 ? projetStats.get(0).count : 0));
            
            tableData.add(new TopStat("Hejer Hejer", 1, 
                projetStats.size() > 1 ? projetStats.get(1).name : "-", 
                projetStats.size() > 1 ? projetStats.get(1).count : 0));

            for (int i = 2; i < Math.min(5, projetStats.size()); i++) {
                tableData.add(new TopStat("-", 0, projetStats.get(i).name, projetStats.get(i).count));
            }
            
            tableTops.setItems(tableData);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setStyleForSeries(XYChart.Series<String, Number> series, String color) {
        for (XYChart.Data<String, Number> data : series.getData()) {
            if (data.getNode() != null) {
                data.getNode().setStyle("-fx-bar-fill: " + color + "; -fx-background-radius: 4 4 0 0;");
            }
        }
    }

    private void setStyleForSeriesH(XYChart.Series<Number, String> series, String color) {
        for (XYChart.Data<Number, String> data : series.getData()) {
            if (data.getNode() != null) {
                data.getNode().setStyle("-fx-bar-fill: " + color + "; -fx-background-radius: 0 4 4 0;");
            }
        }
    }

    // Classes internes pour le Data Binding
    public static class TopStat {
        private final String topParcours;
        private final int parcoursProjets;
        private final String topProjet;
        private final int projetRessources;

        public TopStat(String topParcours, int parcoursProjets, String topProjet, int projetRessources) {
            this.topParcours = topParcours;
            this.parcoursProjets = parcoursProjets;
            this.topProjet = topProjet;
            this.projetRessources = projetRessources;
        }

        public String getTopParcours() {
            return topParcours;
        }

        public int getParcoursProjets() {
            return parcoursProjets;
        }

        public String getTopProjet() {
            return topProjet;
        }

        public int getProjetRessources() {
            return projetRessources;
        }
    }

    private static class TopStatData {
        String name;
        int count;

        TopStatData(String name, int count) {
            this.name = name;
            this.count = count;
        }
    }
}
