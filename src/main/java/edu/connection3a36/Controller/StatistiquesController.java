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
            List<Parcours> parcoursList = parcoursService.getData();
            List<Projet> projetList = projetService.getData();

            // 1. BarChart: Projets par Parcours
            XYChart.Series<String, Number> seriesProjets = new XYChart.Series<>();
            seriesProjets.setName("Nombre de Projets");
            List<TopStatData> parcoursStats = new ArrayList<>();
            for (Parcours p : parcoursList) {
                int count = projetService.getByParcoursId(p.getId()).size();
                String label = p.getTitre().length() > 15 ? p.getTitre().substring(0, 15) + "..." : p.getTitre();
                seriesProjets.getData().add(new XYChart.Data<>(label, count));
                parcoursStats.add(new TopStatData(p.getTitre(), count));
            }
            barChartProjets.getData().add(seriesProjets);
            // Appliquer la couleur bleu foncée (#102c59)
            setStyleForSeries(seriesProjets, "#102c59");

            // 2. BarChart Horizontal: Ressources par Projet
            XYChart.Series<Number, String> seriesRessources = new XYChart.Series<>();
            seriesRessources.setName("Nombre de Ressources");
            List<TopStatData> projetStats = new ArrayList<>();
            for (Projet p : projetList) {
                int count = ressourceService.getByProjetId(p.getId()).size();
                String label = p.getTitre().length() > 20 ? p.getTitre().substring(0, 20) + "..." : p.getTitre();
                seriesRessources.getData().add(new XYChart.Data<>(count, label));
                projetStats.add(new TopStatData(p.getTitre(), count));
            }
            barChartRessources.getData().add(seriesRessources);
            // Appliquer la couleur vert émeraude (#1f8c50)
            setStyleForSeriesH(seriesRessources, "#1f8c50");

            // 3. PieChart: Type de Projets
            Map<String, Integer> typeCounts = new HashMap<>();
            for (Projet p : projetList) {
                String type = p.getType() != null ? p.getType() : "Autre";
                typeCounts.put(type, typeCounts.getOrDefault(type, 0) + 1);
            }
            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            for (Map.Entry<String, Integer> entry : typeCounts.entrySet()) {
                pieData.add(new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue()));
            }
            pieChartTypeProjet.setData(pieData);

            // 4. Résumé des Tops (Table)
            parcoursStats.sort((o1, o2) -> Integer.compare(o2.count, o1.count)); // Descending
            projetStats.sort((o1, o2) -> Integer.compare(o2.count, o1.count)); // Descending

            ObservableList<TopStat> tableData = FXCollections.observableArrayList();
            int maxRows = Math.max(parcoursStats.size(), projetStats.size());
            for (int i = 0; i < Math.min(maxRows, 5); i++) { // Afficher top 5 max
                String pLabel = i < parcoursStats.size() ? parcoursStats.get(i).name : "-";
                int pCount = i < parcoursStats.size() ? parcoursStats.get(i).count : 0;

                String prLabel = i < projetStats.size() ? projetStats.get(i).name : "-";
                int prCount = i < projetStats.size() ? projetStats.get(i).count : 0;

                tableData.add(new TopStat(pLabel, pCount, prLabel, prCount));
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
