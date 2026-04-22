package com.mentorai.controllers;

import com.mentorai.models.Feedback;
import com.mentorai.models.Traitement;
import com.mentorai.services.FeedbackService;
import com.mentorai.services.PdfExportService;
import com.mentorai.services.TraitementService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.mentorai.services.ExcelExportService;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AdminFeedbackController implements Initializable {

    @FXML private TableView<Feedback> tableNonTraites;
    @FXML private TableView<Feedback> tableTraites;

    @FXML private TableColumn<Feedback, Integer> colIdNT;
    @FXML private TableColumn<Feedback, String>  colTypeNT;
    @FXML private TableColumn<Feedback, String>  colDateNT;
    @FXML private TableColumn<Feedback, Integer> colNoteNT;
    @FXML private TableColumn<Feedback, String>  colMessageNT;
    @FXML private TableColumn<Feedback, Void>    colActionNT;

    @FXML private TableColumn<Feedback, Integer> colIdT;
    @FXML private TableColumn<Feedback, String>  colTypeT;
    @FXML private TableColumn<Feedback, String>  colDateT;
    @FXML private TableColumn<Feedback, Integer> colNoteT;
    @FXML private TableColumn<Feedback, String>  colMessageT;
    @FXML private TableColumn<Feedback, Void>    colTraitementT;
    @FXML private TableColumn<Feedback, Void>    colActionT;

    @FXML private Label labelEnAttente;
    @FXML private Label labelTraites;
    @FXML private TextField champRecherche;
    @FXML private ComboBox<String> comboFiltreType;
    @FXML private ComboBox<String> comboTri;

    private List<Feedback> tousLesFeedbacks;
    private FeedbackService feedbackService = new FeedbackService();
    private TraitementService traitementService = new TraitementService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        comboFiltreType.setItems(FXCollections.observableArrayList(
                "Tous les types", "probleme", "satisfaction", "suggestion"
        ));
        comboFiltreType.setValue("Tous les types");

        comboTri.setItems(FXCollections.observableArrayList(
                "Plus récent d'abord", "Plus ancien d'abord",
                "Note croissante", "Note décroissante"
        ));
        comboTri.setValue("Plus récent d'abord");

        tableNonTraites.setPlaceholder(new Label("Aucun feedback en attente."));
        tableTraites.setPlaceholder(new Label("Aucun feedback traité."));
        tableNonTraites.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableTraites.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        configurerColonnes();
        chargerDonnees();

        // ✅ RECHERCHE DYNAMIQUE
        champRecherche.textProperty().addListener((obs, oldVal, newVal) -> filtrer());
        comboFiltreType.valueProperty().addListener((obs, oldVal, newVal) -> filtrer());
        comboTri.valueProperty().addListener((obs, oldVal, newVal) -> filtrer());
    }

    private void configurerColonnes() {

        colIdNT.setCellValueFactory(new PropertyValueFactory<>("id"));
        colDateNT.setCellValueFactory(new PropertyValueFactory<>("datefeedback"));
        colNoteNT.setCellValueFactory(new PropertyValueFactory<>("note"));
        colMessageNT.setCellValueFactory(new PropertyValueFactory<>("contenu"));
        colTypeNT.setCellFactory(col -> badgeType());

        colActionNT.setCellFactory(col -> new TableCell<>() {
            final Button btnVoir    = new Button("Voir");
            final Button btnTraiter = new Button("Traiter");
            final HBox   box        = new HBox(6, btnVoir, btnTraiter);
            {
                btnVoir.setStyle(
                        "-fx-background-color: #9dbbce; -fx-text-fill: white;" +
                                "-fx-background-radius: 5; -fx-cursor: hand; -fx-font-size: 11px;" +
                                "-fx-padding: 5 10 5 10; -fx-font-weight: bold;"
                );
                btnTraiter.setStyle(
                        "-fx-background-color: #102c59; -fx-text-fill: white;" +
                                "-fx-background-radius: 5; -fx-cursor: hand; -fx-font-size: 11px;" +
                                "-fx-padding: 5 10 5 10; -fx-font-weight: bold;"
                );
                btnVoir.setOnAction(e -> ouvrirVue(getTableView().getItems().get(getIndex())));
                btnTraiter.setOnAction(e -> ouvrirFormulaireTraitement(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null); return;
                }
                setGraphic(box); setText(null);
            }
        });

        colIdT.setCellValueFactory(new PropertyValueFactory<>("id"));
        colDateT.setCellValueFactory(new PropertyValueFactory<>("datefeedback"));
        colNoteT.setCellValueFactory(new PropertyValueFactory<>("note"));
        colMessageT.setCellValueFactory(new PropertyValueFactory<>("contenu"));
        colTypeT.setCellFactory(col -> badgeType());

        colTraitementT.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null); return;
                }
                Feedback f = getTableView().getItems().get(getIndex());
                if (f.getTraitementId() != 0) {
                    Traitement t = traitementService.getById(f.getTraitementId());
                    if (t != null) {
                        Label badge = new Label(t.getTypetraitement());
                        badge.setStyle(
                                "-fx-background-color: #102c59; -fx-text-fill: white;" +
                                        "-fx-padding: 3 8 3 8; -fx-background-radius: 10; -fx-font-size: 11px;"
                        );
                        setGraphic(badge);
                    } else { setGraphic(null); }
                } else { setGraphic(null); }
                setText(null);
            }
        });

        colActionT.setCellFactory(col -> new TableCell<>() {
            final Button btnVoir     = new Button("Voir");
            final Button btnModifier = new Button("Modifier");
            final Button btnSupp     = new Button("Supprimer");
            final HBox   box         = new HBox(5, btnVoir, btnModifier, btnSupp);
            {
                btnVoir.setStyle(
                        "-fx-background-color: #9dbbce; -fx-text-fill: white;" +
                                "-fx-background-radius: 5; -fx-cursor: hand; -fx-font-size: 11px;" +
                                "-fx-padding: 5 10 5 10; -fx-font-weight: bold;"
                );
                btnModifier.setStyle(
                        "-fx-background-color: #f0a500; -fx-text-fill: white;" +
                                "-fx-background-radius: 5; -fx-cursor: hand; -fx-font-size: 11px;" +
                                "-fx-padding: 5 10 5 10; -fx-font-weight: bold;"
                );
                btnSupp.setStyle(
                        "-fx-background-color: #d52e28; -fx-text-fill: white;" +
                                "-fx-background-radius: 5; -fx-cursor: hand; -fx-font-size: 11px;" +
                                "-fx-padding: 5 10 5 10; -fx-font-weight: bold;"
                );
                btnVoir.setOnAction(e -> ouvrirVue(getTableView().getItems().get(getIndex())));
                btnModifier.setOnAction(e -> ouvrirModifierTraitement(getTableView().getItems().get(getIndex())));
                btnSupp.setOnAction(e -> supprimerFeedback(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null); return;
                }
                setGraphic(box); setText(null);
            }
        });
    }

    private TableCell<Feedback, String> badgeType() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null); return;
                }
                Feedback f = getTableView().getItems().get(getIndex());
                String couleur = switch (f.getTypefeedback()) {
                    case "probleme"     -> "#d52e28";
                    case "satisfaction" -> "#28a745";
                    case "suggestion"   -> "#9dbbce";
                    default             -> "#888";
                };
                Label badge = new Label(f.getTypefeedback());
                badge.setStyle(
                        "-fx-background-color: " + couleur + "; -fx-text-fill: white;" +
                                "-fx-padding: 3 8 3 8; -fx-background-radius: 10;" +
                                "-fx-font-size: 11px; -fx-font-weight: bold;"
                );
                setGraphic(badge); setText(null);
            }
        };
    }

    public void chargerDonnees() {
        tousLesFeedbacks = feedbackService.getAll();
        appliquerFiltres(tousLesFeedbacks);
    }

    private void appliquerFiltres(List<Feedback> source) {
        List<Feedback> nonTraites = source.stream()
                .filter(f -> f.getEtatfeedback().equals("en_attente"))
                .collect(Collectors.toList());
        List<Feedback> traites = source.stream()
                .filter(f -> f.getEtatfeedback().equals("traite"))
                .collect(Collectors.toList());
        tableNonTraites.setItems(FXCollections.observableArrayList(nonTraites));
        tableTraites.setItems(FXCollections.observableArrayList(traites));
        labelEnAttente.setText(nonTraites.size() + " en attente");
        labelTraites.setText(traites.size() + " traités");
    }

    @FXML
    private void filtrer() {
        if (tousLesFeedbacks == null) return;
        String motCle = champRecherche.getText().trim().toLowerCase();
        String type   = comboFiltreType.getValue();
        String tri    = comboTri.getValue();
        List<Feedback> filtre = tousLesFeedbacks.stream()
                .filter(f -> motCle.isEmpty() || f.getContenu().toLowerCase().contains(motCle))
                .filter(f -> type == null || type.equals("Tous les types") || f.getTypefeedback().equals(type))
                .collect(Collectors.toList());
        if (tri != null) {
            switch (tri) {
                case "Plus récent d'abord" -> filtre.sort((a, b) -> b.getDatefeedback().compareTo(a.getDatefeedback()));
                case "Plus ancien d'abord" -> filtre.sort((a, b) -> a.getDatefeedback().compareTo(b.getDatefeedback()));
                case "Note croissante"     -> filtre.sort((a, b) -> Integer.compare(a.getNote(), b.getNote()));
                case "Note décroissante"   -> filtre.sort((a, b) -> Integer.compare(b.getNote(), a.getNote()));
            }
        }
        appliquerFiltres(filtre);
    }

    @FXML
    private void reinitialiser() {
        champRecherche.clear();
        comboFiltreType.setValue("Tous les types");
        comboTri.setValue("Plus récent d'abord");
        chargerDonnees();
    }

    // ✅ EXPORT PDF
    @FXML
    private void exporterPDF() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le PDF");
        fileChooser.setInitialFileName("feedbacks_" + java.time.LocalDate.now() + ".pdf");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );
        Stage stage = (Stage) champRecherche.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            PdfExportService pdfService = new PdfExportService();
            pdfService.exporterFeedbacks(tousLesFeedbacks, file.getAbsolutePath());
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Export PDF");
            alert.setContentText("✅ PDF exporté avec succès !\n" + file.getAbsolutePath());
            alert.showAndWait();
        }
    }

    // ✅ EXPORT EXCEL
    @FXML
    private void exporterExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le fichier Excel");
        fileChooser.setInitialFileName("feedbacks_" + java.time.LocalDate.now() + ".xlsx");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx")
        );
        Stage stage = (Stage) champRecherche.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            ExcelExportService excelService = new ExcelExportService();
            excelService.exporterFeedbacks(tousLesFeedbacks, file.getAbsolutePath());
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Export Excel");
            alert.setContentText("✅ Excel exporté avec succès !\n" + file.getAbsolutePath());
            alert.showAndWait();
        }
    }

    private void ouvrirVue(Feedback feedback) {
        Traitement t = feedback.getTraitementId() != 0
                ? traitementService.getById(feedback.getTraitementId()) : null;
        String reponse = (t != null)
                ? "Type : " + t.getTypetraitement() + "\nRéponse : " + t.getDescription() + "\nDate : " + t.getDatetraitement()
                : "Pas encore de traitement.";
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails #" + feedback.getId());
        alert.setHeaderText(
                "Type : " + feedback.getTypefeedback() +
                        " | Note : " + feedback.getNote() + "/5" +
                        " | État : " + feedback.getEtatfeedback() +
                        " | Date : " + feedback.getDatefeedback()
        );
        alert.setContentText("Message :\n" + feedback.getContenu() + "\n\n── Traitement ──\n" + reponse);
        alert.showAndWait();
    }

    private void ouvrirFormulaireTraitement(Feedback feedback) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TraiterFeedback.fxml"));
            VBox root = loader.load();
            TraiterFeedbackController ctrl = loader.getController();
            ctrl.setFeedback(feedback, this);
            Stage popup = new Stage();
            popup.setTitle("Traiter #" + feedback.getId());
            popup.setScene(new Scene(root));
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.showAndWait();
        } catch (Exception e) {
            System.out.println("❌ Erreur traiter : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void ouvrirModifierTraitement(Feedback feedback) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ModifierTraitement.fxml"));
            VBox root = loader.load();
            ModifierTraitementController ctrl = loader.getController();
            ctrl.setFeedback(feedback, this);
            Stage popup = new Stage();
            popup.setTitle("Modifier traitement #" + feedback.getId());
            popup.setScene(new Scene(root));
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.showAndWait();
        } catch (Exception e) {
            System.out.println("❌ Erreur modifier : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void supprimerFeedback(Feedback feedback) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setContentText("Supprimer ce feedback ?");
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                feedbackService.delete(feedback.getId());
                chargerDonnees();
            }
        });
    }
}