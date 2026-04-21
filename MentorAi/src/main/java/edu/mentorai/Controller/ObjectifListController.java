package edu.mentorai.Controller;

import edu.mentorai.Main;
import edu.mentorai.entities.Objectif;
import edu.mentorai.entities.Statutobj;
import edu.mentorai.services.ObjectifService;
import edu.mentorai.tools.CircleChart;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ObjectifListController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private VBox objectifContainer;
    @FXML private Label totalLabel;
    @FXML private Label atteintsLabel;
    @FXML private Label enCoursLabel;
    @FXML private Label abandonnesLabel;
    @FXML private StackPane objectifChartPane;

    private int utilisateurId = 1;
    private final ObjectifService objectifService = new ObjectifService();
    private List<Objectif> currentList;

    @FXML
    public void initialize() {
        sortCombo.getItems().addAll("Date de debut", "Date de fin", "Titre", "Statut");
        sortCombo.setValue("Date de debut");
        sortCombo.setOnAction(e -> applySortAndDisplay());
    }

    public void setUtilisateurId(int id) { this.utilisateurId = id; }

    public void loadData() {
        try {
            currentList = objectifService.findByUtilisateur(utilisateurId);
            applySortAndDisplay();
        } catch (Exception e) {
            showAlert("Erreur", e.getMessage());
        }
    }

    private void applySortAndDisplay() {
        if (currentList == null) return;
        String sort = sortCombo.getValue();
        List<Objectif> sorted = new ArrayList<>(currentList);
        switch (sort) {
            case "Titre" -> sorted.sort(Comparator.comparing(Objectif::getTitre));
            case "Date de fin" -> sorted.sort(Comparator.comparing(
                    o -> o.getDatefin() != null ? o.getDatefin() : LocalDate.MIN));
            case "Statut" -> sorted.sort(Comparator.comparing(
                    o -> o.getStatut().getValue()));
            default -> sorted.sort(Comparator.comparing(
                    o -> o.getDatedebut() != null ? o.getDatedebut() : LocalDate.MIN));
        }
        afficherObjectifs(sorted);
    }

    private void afficherObjectifs(List<Objectif> objectifs) {
        objectifContainer.getChildren().clear();

        long atteints = objectifs.stream()
                .filter(o -> o.getStatut() == Statutobj.Atteint).count();
        long enCours = objectifs.stream()
                .filter(o -> o.getStatut() == Statutobj.EnCours).count();
        long abandonnes = objectifs.stream()
                .filter(o -> o.getStatut() == Statutobj.Abandonner).count();

        totalLabel.setText(String.valueOf(objectifs.size()));
        atteintsLabel.setText(String.valueOf(atteints));
        enCoursLabel.setText(String.valueOf(enCours));
        abandonnesLabel.setText(String.valueOf(abandonnes));

        // Cercle
        objectifChartPane.getChildren().clear();
        objectifChartPane.getChildren().add(
                CircleChart.createDonut(
                        atteints, enCours, abandonnes,
                        objectifs.size(),
                        String.valueOf(objectifs.size()),
                        "Total"
                )
        );

        if (objectifs.isEmpty()) {
            Label empty = new Label("Aucun objectif trouve. Creez votre premier objectif !");
            empty.setStyle("-fx-font-size: 15px; -fx-text-fill: #888; -fx-padding: 40;");
            objectifContainer.getChildren().add(empty);
            return;
        }

        for (Objectif obj : objectifs) {
            objectifContainer.getChildren().add(createCard(obj));
        }
    }

    private VBox createCard(Objectif obj) {
        VBox card = new VBox();
        card.setStyle("-fx-background-color: white; -fx-background-radius: 20; " +
                "-fx-effect: dropshadow(gaussian, rgba(16,44,89,0.08), 15, 0, 0, 5); " +
                "-fx-border-color: rgba(157,187,206,0.2); -fx-border-radius: 20;");

        VBox body = new VBox(8);
        body.setPadding(new Insets(20, 25, 20, 25));

        String statusColor = switch (obj.getStatut()) {
            case Atteint -> "#198754";
            case EnCours -> "#ffc107";
            case Abandonner -> "#d52e28";
        };

        Label badge = new Label(obj.getStatut().getValue().toUpperCase());
        badge.setStyle("-fx-background-color: " + statusColor + "22; -fx-text-fill: " + statusColor +
                "; -fx-font-weight: bold; -fx-font-size: 11px; " +
                "-fx-background-radius: 50; -fx-padding: 4 12 4 12;");

        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label idLabel = new Label("ID: PPD" + obj.getId());
        idLabel.setStyle("-fx-text-fill: #102c59; -fx-font-size: 11px; " +
                "-fx-background-color: #f0f4f8; -fx-background-radius: 50; -fx-padding: 4 10 4 10;");
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topRow.getChildren().addAll(badge, spacer, idLabel);

        Label titre = new Label(obj.getTitre());
        titre.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #102c59;");
        titre.setWrapText(true);

        Label desc = new Label(obj.getDescription());
        desc.setStyle("-fx-text-fill: #888; -fx-font-size: 13px;");
        desc.setWrapText(true);

        HBox dates = new HBox(20);
        Label debut = new Label("Debut : " + (obj.getDatedebut() != null ? obj.getDatedebut() : "-"));
        debut.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        Label fin = new Label("Echeance : " + (obj.getDatefin() != null ? obj.getDatefin() : "-"));
        fin.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        dates.getChildren().addAll(debut, fin);

        body.getChildren().addAll(topRow, titre, desc, dates);

        HBox actions = new HBox(10);
        actions.setPadding(new Insets(12, 20, 12, 20));
        actions.setStyle("-fx-background-color: #fcfdfe; -fx-background-radius: 0 0 20 20; " +
                "-fx-border-color: #f0f4f8; -fx-border-width: 1 0 0 0;");

        Button btnProgramme = createBtn("Mon Programme", "#eef2f7", "#102c59");
        Button btnDetails   = createBtn("Details", "#f0fdf4", "#198754");
        Button btnModifier  = createBtn("Modifier", "#fffbeb", "#d97706");
        Button btnSupprimer = createBtn("Supprimer", "#fef2f2", "#dc2626");

        btnProgramme.setOnAction(e -> handleProgramme(obj));
        btnDetails.setOnAction(e -> handleDetails(obj));
        btnModifier.setOnAction(e -> handleEdit(obj));
        btnSupprimer.setOnAction(e -> handleDelete(obj));

        for (Button b : new Button[]{btnProgramme, btnDetails, btnModifier, btnSupprimer}) {
            HBox.setHgrow(b, Priority.ALWAYS);
        }

        actions.getChildren().addAll(btnProgramme, btnDetails, btnModifier, btnSupprimer);
        card.getChildren().addAll(body, actions);
        return card;
    }

    private Button createBtn(String text, String bg, String fg) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg +
                "; -fx-font-weight: bold; -fx-background-radius: 10; " +
                "-fx-padding: 10 10 10 10; -fx-cursor: hand; -fx-font-size: 12px;");
        return btn;
    }

    @FXML
    private void handleNew() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/objectif_form.fxml"));
            Scene scene = new Scene(loader.load(), 700, 580);
            ObjectifFormController ctrl = loader.getController();
            ctrl.setUtilisateurId(utilisateurId);
            ctrl.setOnSaved(this::loadData);
            Main.primaryStage.setScene(scene);
        } catch (Exception e) { showAlert("Erreur", e.getMessage()); }
    }

    private void handleEdit(Objectif obj) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/objectif_form.fxml"));
            Scene scene = new Scene(loader.load(), 700, 580);
            ObjectifFormController ctrl = loader.getController();
            ctrl.setUtilisateurId(utilisateurId);
            ctrl.setObjectif(obj);
            ctrl.setOnSaved(this::loadData);
            Main.primaryStage.setScene(scene);
        } catch (Exception e) { showAlert("Erreur", e.getMessage()); }
    }

    private void handleDetails(Objectif obj) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/objectif_show.fxml"));
            Scene scene = new Scene(loader.load(), 900, 600);
            ObjectifShowController ctrl = loader.getController();
            ctrl.setObjectif(obj);
            ctrl.setUtilisateurId(utilisateurId);
            Main.primaryStage.setScene(scene);
        } catch (Exception e) { showAlert("Erreur", e.getMessage()); }
    }

    private void handleProgramme(Objectif obj) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/programme_show.fxml"));
            Scene scene = new Scene(loader.load(), 1100, 700);
            ProgrammeShowController ctrl = loader.getController();
            ctrl.setObjectif(obj);
            ctrl.setUtilisateurId(utilisateurId);
            ctrl.loadData();
            Main.primaryStage.setScene(scene);
        } catch (Exception e) { showAlert("Erreur", e.getMessage()); }
    }

    private void handleDelete(Objectif obj) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer");
        alert.setContentText("Supprimer cet objectif ? Action irreversible.");
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    objectifService.delete(obj.getId());
                    loadData();
                } catch (Exception e) { showAlert("Erreur", e.getMessage()); }
            }
        });
    }

    @FXML
    private void handleSearch() {
        try {
            String query = searchField.getText().trim();
            currentList = query.isEmpty()
                    ? objectifService.findByUtilisateur(utilisateurId)
                    : objectifService.searchByTitre(query);
            applySortAndDisplay();
        } catch (Exception e) { showAlert("Erreur", e.getMessage()); }
    }

    @FXML
    private void handleReset() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Reinitialiser");
        alert.setContentText("Supprimer TOUS vos objectifs ?");
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    List<Objectif> all = objectifService.findByUtilisateur(utilisateurId);
                    for (Objectif o : all) objectifService.delete(o.getId());
                    loadData();
                } catch (Exception e) { showAlert("Erreur", e.getMessage()); }
            }
        });
    }

    @FXML
    private void handleExportExcel() {
        try {
            FileChooser fc = new FileChooser();
            fc.setInitialFileName("objectifs.csv");
            fc.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("CSV", "*.csv"));
            File file = fc.showSaveDialog(Main.primaryStage);
            if (file == null) return;
            List<Objectif> all = objectifService.findByUtilisateur(utilisateurId);
            try (PrintWriter pw = new PrintWriter(new FileOutputStream(file))) {
                pw.println("N,ID,Titre,Date debut,Date fin,Statut");
                int i = 1;
                for (Objectif o : all) {
                    pw.println(i++ + ",PPD" + o.getId() + ",\"" + o.getTitre()
                            + "\"," + o.getDatedebut() + "," + o.getDatefin()
                            + "," + o.getStatut().getValue());
                }
            }
            showInfo("Export reussi", "Fichier sauvegarde !");
        } catch (Exception e) { showAlert("Erreur", e.getMessage()); }
    }

    @FXML
    private void handleExportWord() {
        try {
            FileChooser fc = new FileChooser();
            fc.setInitialFileName("objectifs.txt");
            fc.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Text", "*.txt"));
            File file = fc.showSaveDialog(Main.primaryStage);
            if (file == null) return;
            List<Objectif> all = objectifService.findByUtilisateur(utilisateurId);
            try (PrintWriter pw = new PrintWriter(new FileOutputStream(file))) {
                pw.println("LISTE DES OBJECTIFS - MentorAI");
                pw.println("================================");
                int i = 1;
                for (Objectif o : all) {
                    pw.println(i++ + ". [PPD" + o.getId() + "] " + o.getTitre());
                    pw.println("   Statut: " + o.getStatut().getValue());
                    pw.println("   Debut: " + o.getDatedebut()
                            + " | Fin: " + o.getDatefin());
                    pw.println();
                }
            }
            showInfo("Export reussi", "Fichier sauvegarde !");
        } catch (Exception e) { showAlert("Erreur", e.getMessage()); }
    }

    @FXML
    private void handleAdmin() {
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

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setContentText(msg); a.showAndWait();
    }
}