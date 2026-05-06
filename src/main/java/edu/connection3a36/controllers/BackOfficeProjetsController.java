package edu.connection3a36.controllers;

import edu.connection3a36.entities.Projet;
import edu.connection3a36.entities.Ressource;
import edu.connection3a36.services.ProjetService;
import edu.connection3a36.services.RessourceService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.stage.FileChooser;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Font;
import com.itextpdf.text.Element;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPCell;

public class BackOfficeProjetsController implements Initializable {
    @FXML private TableView<Projet> tableProjets;
    @FXML private TableColumn<Projet, Integer> colIndex;
    @FXML private TableColumn<Projet, String> colTitre;
    @FXML private TableColumn<Projet, String> colUtilisateur;
    @FXML private TableColumn<Projet, String> colType;
    @FXML private TableColumn<Projet, String> colRessources;
    @FXML private TableColumn<Projet, Void> colAction;
    @FXML private PieChart pieChartTypes;
    @FXML private Label labelTotalProjects;
    @FXML private Label labelTotalResources;
    @FXML private Label labelActiveUsers;

    @FXML private BarChart<String, Number> barChartUsers;
    @FXML private BarChart<Number, String> barChartRessources;
    @FXML private TableView<TopStat> tableTops;
    @FXML private TableColumn<TopStat, String> colTopParcours;
    @FXML private TableColumn<TopStat, Integer> colParcoursProjets;
    @FXML private TableColumn<TopStat, String> colTopProjet;
    @FXML private TableColumn<TopStat, Integer> colProjetRessources;
    @FXML private HBox paginationBoxProjets;
    @FXML private TextField filterInput;

    private final ProjetService projetService = new ProjetService();
    private final RessourceService ressourceService = new RessourceService();
    private ObservableList<Projet> allProjets = FXCollections.observableArrayList();
    private ObservableList<Projet> filteredProjets = FXCollections.observableArrayList();
    private int currentPage = 0;
    private final int itemsPerPage = 5;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            setupColumns();
            setupTopsColumns();
            Platform.runLater(this::loadData);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void setupTopsColumns() {
        colTopParcours.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTopParcours()));
        colParcoursProjets.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getParcoursProjets()));
        colTopProjet.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTopProjet()));
        colProjetRessources.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getProjetRessources()));
    }

    public static class TopStat {
        private final String topParcours;
        private final int parcoursProjets;
        private final String topProjet;
        private final int projetRessources;
        public TopStat(String u, int p, String tp, int pr) {
            this.topParcours = u; this.parcoursProjets = p; 
            this.topProjet = tp; this.projetRessources = pr;
        }
        public String getTopParcours() { return topParcours; }
        public int getParcoursProjets() { return parcoursProjets; }
        public String getTopProjet() { return topProjet; }
        public int getProjetRessources() { return projetRessources; }
    }

    private void setupColumns() {
        // Ajout de CellValueFactory pour forcer la création des cellules
        colIndex.setCellValueFactory(p -> new javafx.beans.property.SimpleObjectProperty<>(1));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colUtilisateur.setCellValueFactory(param -> new SimpleStringProperty("admin admin\nadmin@mentor.ai"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colRessources.setCellValueFactory(param -> new SimpleStringProperty(""));
        colAction.setCellValueFactory(p -> new javafx.beans.property.SimpleObjectProperty<>(null));

        colIndex.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setText(null);
                else setText(String.valueOf(getIndex() + 1 + (currentPage * itemsPerPage)));
            }
        });

        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colTitre.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setGraphic(null);
                else {
                    int myIdx = getIndex();
                    if (myIdx >= 0 && myIdx < getTableView().getItems().size()) {
                        Projet p = getTableView().getItems().get(myIdx);
                        VBox vb = new VBox(2);
                        Label title = new Label(item);
                        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b;");
                        Label date = new Label(p.getDateCreation() != null ? p.getDateCreation().toString() : "No date");
                        date.setStyle("-fx-font-size: 10; -fx-text-fill: #64748b;");
                        vb.getChildren().addAll(title, date);
                        setGraphic(vb);
                    } else setGraphic(null);
                }
            }
        });

        colUtilisateur.setCellValueFactory(param -> new SimpleStringProperty("admin admin\nadmin@mentor.ai"));
        colUtilisateur.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setGraphic(null);
                else {
                    Label l = new Label(item);
                    l.setStyle("-fx-font-size: 11; -fx-text-fill: #64748b;");
                    setGraphic(l);
                }
            }
        });

        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colType.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setGraphic(null);
                else {
                    Label tag = new Label(item.toUpperCase());
                    String color = item.toLowerCase().contains("web") ? "#06b6d4" : (item.toLowerCase().contains("mobile") ? "#0ea5e9" : "#64748b");
                    tag.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-padding: 3 10; -fx-background-radius: 15; -fx-font-size: 10; -fx-font-weight: bold;");
                    setGraphic(tag);
                }
            }
        });

        colRessources.setCellValueFactory(param -> new SimpleStringProperty(""));
        colRessources.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else {
                    int myIdx = getIndex();
                    if (myIdx >= 0 && myIdx < getTableView().getItems().size()) {
                        Projet p = getTableView().getItems().get(myIdx);
                        try {
                            List<Ressource> res = ressourceService.getByProjetId(p.getId());
                            if (res.isEmpty()) setGraphic(new Label("Aucune ressource"));
                            else {
                                VBox vb = new VBox(2);
                                for (Ressource r : res) {
                                    Label l = new Label("📄 " + r.getNom() + " (" + r.getTypeRessource() + ")");
                                    l.setStyle("-fx-font-size: 11; -fx-text-fill: #1e293b;");
                                    vb.getChildren().add(l);
                                }
                                setGraphic(vb);
                            }
                        } catch (SQLException e) { setGraphic(null); }
                    } else setGraphic(null);
                }
            }
        });

        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnVoir = new Button("Voir");
            {
                btnVoir.setStyle("-fx-background-color: transparent; -fx-border-color: #06b6d4; -fx-text-fill: #06b6d4; -fx-border-radius: 5; -fx-cursor: hand;");
                btnVoir.setOnAction(event -> {
                    if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                        mostrarDetalles(getTableView().getItems().get(getIndex()));
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(btnVoir);
            }
        });
    }

    @FXML
    public void loadData() {
        try {
            List<Projet> data = projetService.getData();
            allProjets.setAll(data != null ? data : new ArrayList<>());
            filteredProjets.setAll(allProjets);
            updateCharts();
            updateTable();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateCharts() {
        try {
            labelTotalProjects.setText(String.valueOf(allProjets.size()));
            int totalRessources = 0;
            Set<String> activeDesigners = new HashSet<>();
            for (Projet p : allProjets) {
                if (p.getTechnologies() != null) activeDesigners.add(p.getTechnologies());
                try { totalRessources += ressourceService.getByProjetId(p.getId()).size(); } catch(Exception ignored) {}
            }
            labelTotalResources.setText(String.valueOf(totalRessources));
            labelActiveUsers.setText(String.valueOf(activeDesigners.size()));

            XYChart.Series<String, Number> userSeries = new XYChart.Series<>();
            userSeries.setName("Nombre de Projets");
            Map<String, Integer> userCounts = new HashMap<>();
            userCounts.put("arsl arslen", 4);
            userCounts.put("Hejer Hejer", 2);
            for (Map.Entry<String, Integer> entry : userCounts.entrySet()) {
                userSeries.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            }
            barChartUsers.getData().clear();
            barChartUsers.getData().add(userSeries);

            XYChart.Series<Number, String> resSeries = new XYChart.Series<>();
            resSeries.setName("Ressources");
            List<Projet> sortedByRes = new ArrayList<>(allProjets);
            final Map<Integer, Integer> resCountMap = new HashMap<>();
            for (Projet p : sortedByRes) {
                try { resCountMap.put(p.getId(), ressourceService.getByProjetId(p.getId()).size()); } catch(Exception ignored) {}
            }
            sortedByRes.sort((a,b) -> Integer.compare(resCountMap.getOrDefault(b.getId(), 0), resCountMap.getOrDefault(a.getId(), 0)));

            for (int i = 0; i < Math.min(5, sortedByRes.size()); i++) {
                Projet p = sortedByRes.get(i);
                String titleLabel = p.getTitre().length() > 20 ? p.getTitre().substring(0, 20) + "..." : p.getTitre();
                resSeries.getData().add(new XYChart.Data<>(resCountMap.getOrDefault(p.getId(), 0), titleLabel));
            }
            barChartRessources.getData().clear();
            barChartRessources.getData().add(resSeries);

            Map<String, Long> typeDistribution = allProjets.stream()
                .collect(Collectors.groupingBy(p -> (p.getType() != null && !p.getType().isEmpty()) ? p.getType() : "Autre", Collectors.counting()));
            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            for (Map.Entry<String, Long> entry : typeDistribution.entrySet()) {
                String type = entry.getKey();
                String label = type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase();
                pieData.add(new PieChart.Data(label + " (" + entry.getValue() + ")", entry.getValue()));
            }
            pieChartTypes.setData(pieData);
            pieChartTypes.setLegendVisible(true);
            pieChartTypes.setLabelsVisible(true);

            List<TopStat> topStats = new ArrayList<>();
            List<String> userKeys = new ArrayList<>(userCounts.keySet());
            for (int i = 0; i < Math.max(userKeys.size(), Math.min(5, sortedByRes.size())); i++) {
                String uName = i < userKeys.size() ? userKeys.get(i) : "-";
                int uProj = userCounts.getOrDefault(uName, 0);
                String pTitle = i < sortedByRes.size() ? sortedByRes.get(i).getTitre() : "-";
                int pRes = i < sortedByRes.size() ? resCountMap.getOrDefault(sortedByRes.get(i).getId(), 0) : 0;
                topStats.add(new TopStat(uName, uProj, pTitle, pRes));
            }
            tableTops.setItems(FXCollections.observableArrayList(topStats));

        } catch (Exception e) { e.printStackTrace(); }
    }

    private void updateTable() {
        int from = currentPage * itemsPerPage;
        int to = Math.min(from + itemsPerPage, filteredProjets.size());
        if (from >= filteredProjets.size() && !filteredProjets.isEmpty()) { currentPage = 0; from = 0; to = Math.min(itemsPerPage, filteredProjets.size()); }
        tableProjets.setItems(FXCollections.observableArrayList(filteredProjets.subList(from, to)));
        updatePagination();
    }

    private void updatePagination() {
        if (paginationBoxProjets == null) return;
        paginationBoxProjets.getChildren().clear();
        int totalPages = (int) Math.ceil((double) filteredProjets.size() / itemsPerPage);
        for (int i = 0; i < totalPages; i++) {
            final int idx = i;
            Button b = new Button(String.valueOf(i + 1));
            b.setStyle(i == currentPage ? "-fx-background-color: #1e3a8a; -fx-text-fill: white; -fx-font-weight: bold;" : "-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-padding: 5 10; -fx-cursor: hand;");
            b.setOnAction(e -> { currentPage = idx; updateTable(); });
            paginationBoxProjets.getChildren().add(b);
        }
    }

    @FXML
    private void filtrar() {
        String q = filterInput.getText() != null ? filterInput.getText().toLowerCase() : "";
        filteredProjets.setAll(allProjets.stream()
                .filter(p -> (p.getTitre() != null && p.getTitre().toLowerCase().contains(q)) || (p.getTechnologies() != null && p.getTechnologies().toLowerCase().contains(q)))
                .collect(Collectors.toList()));
        currentPage = 0;
        updateTable();
    }

    @FXML
    private void genererPDF() {
        FileChooser fc = new FileChooser();
        fc.setInitialFileName("Rapport_MentorAI_" + java.time.LocalDate.now() + ".pdf");
        File file = fc.showSaveDialog(tableProjets.getScene().getWindow());
        if (file == null) return;

        Document doc = new Document(PageSize.A4);
        try {
            PdfWriter.getInstance(doc, new FileOutputStream(file));
            doc.open();
            BaseColor navy = new BaseColor(16, 44, 89);
            BaseColor grey = new BaseColor(100, 116, 139);
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD, navy);
            Font subTitleFont = new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC, grey);
            Font sectionFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, navy);
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.WHITE);
            Font bodyFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.BLACK);

            doc.add(new Paragraph("RAPPORT D'ACTIVITÉ MENTOR AI", titleFont));
            doc.add(new Paragraph("Généré le : " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), subTitleFont));
            doc.add(new Paragraph(" "));
            
            PdfPTable statsTable = new PdfPTable(3);
            statsTable.setWidthPercentage(100);
            int totalRessources = 0;
            for (Projet p : allProjets) {
                try { totalRessources += ressourceService.getByProjetId(p.getId()).size(); } catch(Exception ignored) {}
            }
            addStatCell(statsTable, "PROJETS TOTAUX", String.valueOf(allProjets.size()), navy);
            addStatCell(statsTable, "RESSOURCES TOTALES", String.valueOf(totalRessources), navy);
            addStatCell(statsTable, "MOYENNE ENGAGEMENT", String.format("%.1f", allProjets.isEmpty() ? 0 : (double)totalRessources/allProjets.size()), navy);
            doc.add(statsTable);

            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("DÉTAILS DES PROJETS", sectionFont));
            doc.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            addHeaderCell(table, "PROJET", headerFont, navy);
            addHeaderCell(table, "TYPE", headerFont, navy);
            addHeaderCell(table, "DÉBUT", headerFont, navy);
            addHeaderCell(table, "STATUS", headerFont, navy);

            for (Projet p : allProjets) {
                table.addCell(createStyledCell(p.getTitre(), bodyFont, false));
                table.addCell(createStyledCell(p.getType(), bodyFont, true));
                table.addCell(createStyledCell(p.getDateDebut() != null ? p.getDateDebut().toString() : "--", bodyFont, true));
                int resCount = 0;
                try { resCount = ressourceService.getByProjetId(p.getId()).size(); } catch(Exception ignored){}
                table.addCell(createStyledCell(resCount > 2 ? "Actif (High)" : (resCount > 0 ? "Actif" : "En attente"), bodyFont, true));
            }
            doc.add(table);
            doc.close();
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Le rapport PDF premium a été généré !");
            alert.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void addHeaderCell(PdfPTable table, String text, Font font, BaseColor color) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(color);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(10);
        table.addCell(cell);
    }

    private void addStatCell(PdfPTable table, String label, String value, BaseColor color) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(15);
        cell.setBackgroundColor(new BaseColor(248, 250, 252));
        cell.addElement(new Paragraph(label, new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, new BaseColor(100, 116, 139))));
        cell.addElement(new Paragraph(value, new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, color)));
        table.addCell(cell);
    }

    private PdfPCell createStyledCell(String text, Font font, boolean center) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(8);
        if (center) cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private void mostrarDetalles(Projet p) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/DetailsProjet.fxml"));
            Parent view = loader.load();
            ((DetailsProjetController) loader.getController()).setProjet(p);
            MainController.getInstance().loadInContentArea(view);
        } catch (IOException e) { e.printStackTrace(); }
    }
}
