package com.mentorai.controllers;

import com.mentorai.models.Feedback;
import com.mentorai.models.Traitement;
import com.mentorai.services.ExcelExportService;
import com.mentorai.services.FeedbackService;
import com.mentorai.services.PdfExportService;
import com.mentorai.services.PrioriteService;
import com.mentorai.services.TraitementService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.time.*;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

public class AdminFeedbackController implements Initializable {

    @FXML private TableView<Feedback> tableNonTraites;
    @FXML private TableView<Feedback> tableTraites;

    @FXML private TableColumn<Feedback, Integer> colIdNT;
    @FXML private TableColumn<Feedback, Void>    colPrioriteNT;
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

    @FXML private Label            labelEnAttente;
    @FXML private Label            labelTraites;
    @FXML private TextField        champRecherche;
    @FXML private ComboBox<String> comboFiltreType;
    @FXML private ComboBox<String> comboTri;

    @FXML private GridPane grilleCalendrier;
    @FXML private GridPane headerJours;
    @FXML private Label    labelMoisAnnee;
    @FXML private Button   btnVueMois;
    @FXML private Button   btnVueSemaine;

    private YearMonth moisActuel      = YearMonth.now();
    private LocalDate semaineActuelle = LocalDate.now();
    private boolean   vueMois         = true;

    private List<Feedback>    tousLesFeedbacks;
    private FeedbackService   feedbackService   = new FeedbackService();
    private TraitementService traitementService = new TraitementService();
    private PrioriteService   prioriteService   = new PrioriteService();

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

        champRecherche.textProperty().addListener((obs, o, n) -> filtrer());
        comboFiltreType.valueProperty().addListener((obs, o, n) -> filtrer());
        comboTri.valueProperty().addListener((obs, o, n) -> filtrer());
    }

    // ============================================================
    // ✅ CALENDRIER
    // ============================================================

    private void afficherCalendrier() {
        if (vueMois) afficherMois();
        else afficherSemaine();
    }

    private void afficherJoursHeader(List<String> jours) {
        headerJours.getChildren().clear();
        String[] nomsJours = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
        for (int i = 0; i < nomsJours.length; i++) {
            Label lbl = new Label(nomsJours[i]);
            lbl.setMaxWidth(Double.MAX_VALUE);
            lbl.setAlignment(Pos.CENTER);
            lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #102c59;" +
                    "-fx-font-size: 12px; -fx-padding: 4 0 4 0;");
            headerJours.add(lbl, i, 0);
        }
    }

    private void afficherMois() {
        afficherJoursHeader(null);
        grilleCalendrier.getChildren().clear();

        String moisNom = moisActuel.getMonth()
                .getDisplayName(TextStyle.FULL, Locale.FRENCH);
        labelMoisAnnee.setText(
                moisNom.substring(0,1).toUpperCase() + moisNom.substring(1) +
                        " " + moisActuel.getYear()
        );

        Map<LocalDate, List<Feedback>> parDate = tousLesFeedbacks.stream()
                .collect(Collectors.groupingBy(Feedback::getDatefeedback));

        LocalDate premier  = moisActuel.atDay(1);
        int decalage = premier.getDayOfWeek().getValue() - 1;
        int nbJours  = moisActuel.lengthOfMonth();
        int col = decalage, row = 0;

        for (int jour = 1; jour <= nbJours; jour++) {
            LocalDate date = moisActuel.atDay(jour);
            List<Feedback> feedbacksDuJour = parDate.getOrDefault(date, new ArrayList<>());
            VBox cellule = creerCelluleJour(jour, date, feedbacksDuJour, false);
            grilleCalendrier.add(cellule, col, row);
            col++;
            if (col == 7) { col = 0; row++; }
        }
    }

    private void afficherSemaine() {
        afficherJoursHeader(null);
        grilleCalendrier.getChildren().clear();

        LocalDate lundi = semaineActuelle.with(
                java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
        );
        LocalDate dimanche = lundi.plusDays(6);

        labelMoisAnnee.setText(
                "Semaine du " + lundi.getDayOfMonth() + " au " +
                        dimanche.getDayOfMonth() + " " +
                        dimanche.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH) +
                        " " + dimanche.getYear()
        );

        Map<LocalDate, List<Feedback>> parDate = tousLesFeedbacks.stream()
                .collect(Collectors.groupingBy(Feedback::getDatefeedback));

        for (int i = 0; i < 7; i++) {
            LocalDate date = lundi.plusDays(i);
            List<Feedback> feedbacksDuJour = parDate.getOrDefault(date, new ArrayList<>());
            VBox cellule = creerCelluleJour(date.getDayOfMonth(), date, feedbacksDuJour, true);
            grilleCalendrier.add(cellule, i, 0);
        }
    }

    private VBox creerCelluleJour(int numero, LocalDate date,
                                  List<Feedback> feedbacks, boolean grandFormat) {
        VBox cell = new VBox(2);
        cell.setPadding(new Insets(4));
        // ✅ CELLULES COMPACTES
        cell.setMinHeight(grandFormat ? 90 : 45);
        cell.setMaxHeight(grandFormat ? 90 : 45);
        cell.setMaxWidth(Double.MAX_VALUE);

        boolean estAujourdhui = date.equals(LocalDate.now());

        if (estAujourdhui) {
            cell.setStyle("-fx-background-color: #e8f0fe; -fx-background-radius: 6;" +
                    "-fx-border-color: #102c59; -fx-border-radius: 6; -fx-border-width: 2;");
        } else {
            cell.setStyle("-fx-background-color: #fafafa; -fx-background-radius: 6;" +
                    "-fx-border-color: #e0e0e0; -fx-border-radius: 6; -fx-border-width: 1;");
        }

        Label lblNum = new Label(String.valueOf(numero));
        lblNum.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;" +
                "-fx-text-fill: " + (estAujourdhui ? "#102c59" : "#555") + ";");
        cell.getChildren().add(lblNum);

        if (!feedbacks.isEmpty()) {
            long nbProbleme     = feedbacks.stream().filter(f -> f.getTypefeedback().equals("probleme")).count();
            long nbSatisfaction = feedbacks.stream().filter(f -> f.getTypefeedback().equals("satisfaction")).count();
            long nbSuggestion   = feedbacks.stream().filter(f -> f.getTypefeedback().equals("suggestion")).count();

            FlowPane badges = new FlowPane(2, 2);
            if (nbProbleme > 0)     badges.getChildren().add(creerBadge(nbProbleme + "P", "#d52e28"));
            if (nbSatisfaction > 0) badges.getChildren().add(creerBadge(nbSatisfaction + "S", "#28a745"));
            if (nbSuggestion > 0)   badges.getChildren().add(creerBadge(nbSuggestion + "Su", "#f0a500"));
            cell.getChildren().add(badges);

            cell.setStyle(cell.getStyle() + "-fx-cursor: hand;");
            cell.setOnMouseClicked(e -> ouvrirPopupJour(date, feedbacks));

            if (grandFormat) {
                for (int i = 0; i < Math.min(feedbacks.size(), 3); i++) {
                    Feedback f = feedbacks.get(i);
                    String couleur = switch (f.getTypefeedback()) {
                        case "probleme"     -> "#d52e28";
                        case "satisfaction" -> "#28a745";
                        default             -> "#f0a500";
                    };
                    String msg = f.getContenu().length() > 25
                            ? f.getContenu().substring(0, 25) + "..."
                            : f.getContenu();
                    Label lMsg = new Label(msg);
                    lMsg.setStyle("-fx-font-size: 10px; -fx-text-fill: white;" +
                            "-fx-background-color: " + couleur + ";" +
                            "-fx-background-radius: 3; -fx-padding: 2 4 2 4;");
                    lMsg.setMaxWidth(Double.MAX_VALUE);
                    lMsg.setWrapText(true);
                    cell.getChildren().add(lMsg);
                }
                if (feedbacks.size() > 3) {
                    Label plus = new Label("+" + (feedbacks.size()-3) + " autres");
                    plus.setStyle("-fx-font-size: 10px; -fx-text-fill: #888;");
                    cell.getChildren().add(plus);
                }
            }
        }

        return cell;
    }

    private Label creerBadge(String texte, String couleur) {
        Label badge = new Label(texte);
        badge.setStyle("-fx-background-color: " + couleur + "; -fx-text-fill: white;" +
                "-fx-font-size: 9px; -fx-font-weight: bold;" +
                "-fx-padding: 1 4 1 4; -fx-background-radius: 8;");
        return badge;
    }

    private void ouvrirPopupJour(LocalDate date, List<Feedback> feedbacks) {
        Stage popup = new Stage();
        popup.setTitle("Feedbacks du " + date);
        popup.initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(10);
        root.setPadding(new Insets(0, 0, 10, 0));
        root.setStyle("-fx-background-color: #f5f7fa;");

        HBox header = new HBox();
        header.setStyle("-fx-background-color: #102c59; -fx-padding: 12 20 12 20;");
        header.setAlignment(Pos.CENTER_LEFT);
        Label titrePopup = new Label(
                "Feedbacks du " + date.getDayOfMonth() + " " +
                        date.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH) +
                        " " + date.getYear() + "  (" + feedbacks.size() + " feedback(s))"
        );
        titrePopup.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        header.getChildren().add(titrePopup);
        root.getChildren().add(header);

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        VBox cardsContainer = new VBox(10);
        cardsContainer.setPadding(new Insets(10, 15, 10, 15));

        for (Feedback f : feedbacks) {
            VBox card = new VBox(8);
            card.setStyle("-fx-background-color: white; -fx-background-radius: 8;" +
                    "-fx-padding: 12;" +
                    "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.08),4,0,0,2);");

            String couleurType = switch (f.getTypefeedback()) {
                case "probleme"     -> "#d52e28";
                case "satisfaction" -> "#28a745";
                default             -> "#f0a500";
            };

            HBox topRow = new HBox(8);
            topRow.setAlignment(Pos.CENTER_LEFT);

            Label lId = new Label("#" + f.getId());
            lId.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");

            Label lType = new Label(f.getTypefeedback());
            lType.setStyle("-fx-background-color: " + couleurType + "; -fx-text-fill: white;" +
                    "-fx-padding: 3 8 3 8; -fx-background-radius: 10;" +
                    "-fx-font-size: 11px; -fx-font-weight: bold;");

            boolean traite = f.getEtatfeedback().equals("traite");
            Label lEtat = new Label(traite ? "Traité" : "En attente");
            lEtat.setStyle("-fx-background-color: " + (traite ? "#28a745" : "#f0a500") + ";" +
                    "-fx-text-fill: white; -fx-padding: 3 8 3 8;" +
                    "-fx-background-radius: 10; -fx-font-size: 11px; -fx-font-weight: bold;");

            Label lNote = new Label("Note : " + f.getNote() + "/5");
            lNote.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");

            topRow.getChildren().addAll(lId, lType, lEtat, lNote);
            card.getChildren().add(topRow);

            Label lMsg = new Label("Message : " + f.getContenu());
            lMsg.setWrapText(true);
            lMsg.setStyle("-fx-font-size: 12px; -fx-text-fill: #333;");
            card.getChildren().add(lMsg);

            if (traite && f.getTraitementId() != 0) {
                Traitement t = traitementService.getById(f.getTraitementId());
                if (t != null) {
                    VBox reponseBox = new VBox(4);
                    reponseBox.setStyle("-fx-background-color: #eef2ff; -fx-background-radius: 6;" +
                            "-fx-padding: 8;");

                    Label lTitre = new Label("Réponse Admin :");
                    lTitre.setStyle("-fx-font-weight: bold; -fx-text-fill: #102c59; -fx-font-size: 11px;");

                    Label lTypeT = new Label("Type : " + t.getTypetraitement());
                    lTypeT.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");

                    Label lDesc = new Label(t.getDescription());
                    lDesc.setWrapText(true);
                    lDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #333; -fx-font-style: italic;");

                    Label lDate = new Label("Date : " + t.getDatetraitement());
                    lDate.setStyle("-fx-font-size: 10px; -fx-text-fill: #888;");

                    reponseBox.getChildren().addAll(lTitre, lTypeT, lDesc, lDate);
                    card.getChildren().add(reponseBox);
                }
            }

            cardsContainer.getChildren().add(card);
        }

        scroll.setContent(cardsContainer);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        root.getChildren().add(scroll);

        Button btnFermer = new Button("Fermer");
        btnFermer.setStyle("-fx-background-color: #102c59; -fx-text-fill: white;" +
                "-fx-font-weight: bold; -fx-background-radius: 6;" +
                "-fx-padding: 8 24 8 24; -fx-cursor: hand;");
        btnFermer.setOnAction(e -> popup.close());
        HBox footer = new HBox(btnFermer);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(0, 15, 0, 0));
        root.getChildren().add(footer);

        popup.setScene(new Scene(root, 600, 500));
        popup.showAndWait();
    }

    @FXML
    private void moisPrecedent() {
        if (vueMois) moisActuel = moisActuel.minusMonths(1);
        else semaineActuelle = semaineActuelle.minusWeeks(1);
        afficherCalendrier();
    }

    @FXML
    private void moisSuivant() {
        if (vueMois) moisActuel = moisActuel.plusMonths(1);
        else semaineActuelle = semaineActuelle.plusWeeks(1);
        afficherCalendrier();
    }

    @FXML
    private void switchVueMois() {
        vueMois = true;
        btnVueMois.setStyle(
                "-fx-background-color: white; -fx-text-fill: #102c59;" +
                        "-fx-font-weight: bold; -fx-background-radius: 5;" +
                        "-fx-padding: 4 12 4 12; -fx-cursor: hand; -fx-font-size: 11px;"
        );
        btnVueSemaine.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: white;" +
                        "-fx-font-weight: bold; -fx-background-radius: 5;" +
                        "-fx-border-color: white; -fx-border-radius: 5;" +
                        "-fx-padding: 4 12 4 12; -fx-cursor: hand; -fx-font-size: 11px;"
        );
        afficherCalendrier();
    }

    @FXML
    private void switchVueSemaine() {
        vueMois = false;
        btnVueSemaine.setStyle(
                "-fx-background-color: white; -fx-text-fill: #102c59;" +
                        "-fx-font-weight: bold; -fx-background-radius: 5;" +
                        "-fx-padding: 4 12 4 12; -fx-cursor: hand; -fx-font-size: 11px;"
        );
        btnVueMois.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: white;" +
                        "-fx-font-weight: bold; -fx-background-radius: 5;" +
                        "-fx-border-color: white; -fx-border-radius: 5;" +
                        "-fx-padding: 4 12 4 12; -fx-cursor: hand; -fx-font-size: 11px;"
        );
        afficherCalendrier();
    }

    // ============================================================
    // CRUD + FILTRES
    // ============================================================

    private void configurerColonnes() {
        colIdNT.setCellValueFactory(new PropertyValueFactory<>("id"));
        colDateNT.setCellValueFactory(new PropertyValueFactory<>("datefeedback"));
        colNoteNT.setCellValueFactory(new PropertyValueFactory<>("note"));
        colMessageNT.setCellValueFactory(new PropertyValueFactory<>("contenu"));
        colTypeNT.setCellFactory(col -> badgeType());

        colPrioriteNT.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null); return;
                }
                Feedback f = getTableView().getItems().get(getIndex());
                String priorite = prioriteService.calculerPriorite(f);
                String couleur  = prioriteService.getCouleurPriorite(priorite);
                Label badge = new Label(priorite);
                badge.setStyle(
                        "-fx-background-color: " + couleur + "; -fx-text-fill: white;" +
                                "-fx-padding: 3 8 3 8; -fx-background-radius: 10;" +
                                "-fx-font-size: 11px; -fx-font-weight: bold;"
                );
                setGraphic(badge); setText(null);
            }
        });

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
        tousLesFeedbacks = prioriteService.trierParPriorite(tousLesFeedbacks);
        appliquerFiltres(tousLesFeedbacks);
        afficherCalendrier();
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

    @FXML
    private void exporterPDF() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer le PDF");
        fc.setInitialFileName("feedbacks_" + java.time.LocalDate.now() + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fc.showSaveDialog((Stage) champRecherche.getScene().getWindow());
        if (file != null) {
            new PdfExportService().exporterFeedbacks(tousLesFeedbacks, file.getAbsolutePath());
            new Alert(Alert.AlertType.INFORMATION, "PDF exporté !\n" + file.getAbsolutePath()).showAndWait();
        }
    }

    @FXML
    private void exporterExcel() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer Excel");
        fc.setInitialFileName("feedbacks_" + java.time.LocalDate.now() + ".xlsx");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        File file = fc.showSaveDialog((Stage) champRecherche.getScene().getWindow());
        if (file != null) {
            new ExcelExportService().exporterFeedbacks(tousLesFeedbacks, file.getAbsolutePath());
            new Alert(Alert.AlertType.INFORMATION, "Excel exporté !\n" + file.getAbsolutePath()).showAndWait();
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
        alert.setHeaderText("Type : " + feedback.getTypefeedback() +
                " | Note : " + feedback.getNote() + "/5" +
                " | État : " + feedback.getEtatfeedback() +
                " | Date : " + feedback.getDatefeedback());
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
            System.out.println("Erreur traiter : " + e.getMessage());
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
            System.out.println("Erreur modifier : " + e.getMessage());
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