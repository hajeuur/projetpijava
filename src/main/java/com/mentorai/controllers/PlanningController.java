package com.mentorai.controllers;

import com.mentorai.entities.PlanningEtude;
import com.mentorai.services.PlanningService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.net.URL;
import java.sql.SQLException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * JavaFX controller for planning.fxml.
 * Exact feature-parity with PlanningEtudeController.php.
 */
public class PlanningController implements Initializable {

    // ── Constants ────────────────────────────────────────────────────────
    private static final int ROW_HEIGHT = 56;          // px per hour
    private static final int TOTAL_GRID_HEIGHT = ROW_HEIGHT * 24; // 1344

    // ── FXML injections ──────────────────────────────────────────────────

    // Page-level
    @FXML private HBox  personalityBanner;
    @FXML private Label personalityLabel;
    @FXML private HBox  personalityTestBar;

    // Left panel – calendar
    @FXML private Label    calendarTitle;
    @FXML private Label    selectedDateLabel;
    @FXML private GridPane calendarGrid;
    @FXML private FlowPane typeFilter;

    // Right panel – week grid
    @FXML private Label      weekRange;
    @FXML private Label      planningErrors;
    @FXML private Label      planningReminders;
    @FXML private ScrollPane weekScrollPane;
    @FXML private GridPane   weekGridPane;
    @FXML private VBox       weekAxis;

    // Day-header labels
    @FXML private Label dayHeader0, dayHeader1, dayHeader2, dayHeader3,
            dayHeader4, dayHeader5, dayHeader6;

    // Day column Panes
    @FXML private Pane dayCol0, dayCol1, dayCol2, dayCol3,
            dayCol4, dayCol5, dayCol6;

    // ── State ────────────────────────────────────────────────────────────
    private final PlanningService service = new PlanningService();
    private LocalDate selectedDate = LocalDate.now();
    private String activeTypeFilter = "all";
    private List<PlanningEtude> cachedActivities = new ArrayList<>();
    private final Set<String> reminderCache = new HashSet<>();
    private ScheduledExecutorService reminderExecutor;

    // Drag state
    private int dragActivityId = -1;
    private double dragOffsetY  = 0;

    // ── Formatters ───────────────────────────────────────────────────────
    private static final DateTimeFormatter DAY_FMT =
            DateTimeFormatter.ofPattern("EEE dd/MM", Locale.FRENCH);
    private static final DateTimeFormatter MONTH_FMT =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.FRENCH);

    // ════════════════════════════════════════════════════════════════════
    // INIT
    // ════════════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        updateAll();
        startReminderPoller();
    }

    // ════════════════════════════════════════════════════════════════════
    // CALENDAR  (left panel)
    // ════════════════════════════════════════════════════════════════════

    @FXML private void onCalPrev()     { selectedDate = selectedDate.minusMonths(1); updateAll(); }
    @FXML private void onCalNext()     { selectedDate = selectedDate.plusMonths(1);  updateAll(); }
    @FXML private void onCalPrevYear() { selectedDate = selectedDate.minusYears(1);  updateAll(); }
    @FXML private void onCalNextYear() { selectedDate = selectedDate.plusYears(1);   updateAll(); }

    @FXML private void onPersonalityTest() { /* navigate to personality test */ }
    @FXML private void onRefaireTest()     { /* navigate to personality test */ }

    private void renderCalendar() {
        LocalDate today = LocalDate.now();
        int year  = selectedDate.getYear();
        int month = selectedDate.getMonthValue();

        // Title
        String monthLabel = selectedDate.format(MONTH_FMT);
        calendarTitle.setText(capitalize(monthLabel));
        selectedDateLabel.setText("Date sélectionnée : " + selectedDate.format(DISPLAY_FMT));

        calendarGrid.getChildren().clear();
        calendarGrid.getColumnConstraints().clear();
        calendarGrid.getRowConstraints().clear();

        // 7 equal columns
        for (int c = 0; c < 7; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 7);
            calendarGrid.getColumnConstraints().add(cc);
        }

        // Day-of-week headers
        String[] headers = {"L", "M", "M", "J", "V", "S", "D"};
        for (int i = 0; i < 7; i++) {
            Label h = new Label(headers[i]);
            h.setMaxWidth(Double.MAX_VALUE);
            h.setAlignment(Pos.CENTER);
            h.getStyleClass().add("calendar-cell");
            h.getStyleClass().add("calendar-cell-muted");
            calendarGrid.add(h, i, 0);
        }

        // Grid cells
        LocalDate firstOfMonth = LocalDate.of(year, month, 1);
        int startDayOffset = (firstOfMonth.getDayOfWeek().getValue() - 1); // 0=Mon
        LocalDate gridStart = firstOfMonth.minusDays(startDayOffset);

        for (int i = 0; i < 42; i++) {
            LocalDate cellDate = gridStart.plusDays(i);
            Label cell = new Label(String.valueOf(cellDate.getDayOfMonth()));
            cell.setMaxWidth(Double.MAX_VALUE);
            cell.setAlignment(Pos.CENTER);
            cell.getStyleClass().add("calendar-cell");
            cell.setPadding(new Insets(6, 0, 6, 0));

            if (cellDate.getMonthValue() != month) {
                cell.getStyleClass().add("calendar-cell-muted");
            }
            if (cellDate.equals(selectedDate)) {
                cell.getStyleClass().add("calendar-cell-selected");
            }
            if (cellDate.equals(today)) {
                cell.getStyleClass().add("calendar-cell-today");
            }

            final LocalDate clickDate = cellDate;
            cell.setOnMouseClicked(e -> {
                selectedDate = clickDate;
                updateAll();
            });
            cell.setCursor(Cursor.HAND);
            calendarGrid.add(cell, i % 7, (i / 7) + 1);
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // WEEK GRID  (right panel)
    // ════════════════════════════════════════════════════════════════════

    private void renderWeekSkeleton() {
        LocalDate monday = weekStart(selectedDate);
        Label[] dayHeaders = {dayHeader0, dayHeader1, dayHeader2, dayHeader3,
                dayHeader4, dayHeader5, dayHeader6};
        Pane[]  dayCols   = {dayCol0,    dayCol1,    dayCol2,    dayCol3,
                dayCol4,    dayCol5,    dayCol6};

        for (int i = 0; i < 7; i++) {
            LocalDate d = monday.plusDays(i);
            dayHeaders[i].setText(d.format(DAY_FMT));
            dayHeaders[i].setUserData(d);            // store date for click handler
            dayHeaders[i].getStyleClass().removeAll("week-day-header-selected");
            if (d.equals(selectedDate)) {
                dayHeaders[i].getStyleClass().add("week-day-header-selected");
            }
            // click on header selects that day
            dayHeaders[i].setOnMouseClicked(e -> {
                selectedDate = d;
                updateAll();
            });
            dayHeaders[i].setCursor(Cursor.HAND);

            // Tag column with date string
            dayCols[i].setUserData(d);
            dayCols[i].setPrefHeight(TOTAL_GRID_HEIGHT);
            dayCols[i].setMinHeight(TOTAL_GRID_HEIGHT);
        }

        // Week range label
        LocalDate sunday = monday.plusDays(6);
        weekRange.setText(monday.format(DISPLAY_FMT) + " → " + sunday.format(DISPLAY_FMT));
    }

    /** Remove all activity cards from all day columns. */
    private void clearActivityCards() {
        Pane[] cols = {dayCol0, dayCol1, dayCol2, dayCol3, dayCol4, dayCol5, dayCol6};
        for (Pane col : cols) {
            col.getChildren().removeIf(n -> "activity-card".equals(n.getUserData()));
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // PAINT ACTIVITIES
    // ════════════════════════════════════════════════════════════════════

    private void paintActivities(List<PlanningEtude> activities) {
        clearActivityCards();
        LocalDate monday = weekStart(selectedDate);

        // Split multi-day activities then group by day for lane assignment
        List<PlanningEtude> rendered = splitForRender(activities, monday);
        Map<LocalDate, List<PlanningEtude>> byDay = new LinkedHashMap<>();
        for (LocalDate d = monday; !d.isAfter(monday.plusDays(6)); d = d.plusDays(1)) {
            byDay.put(d, new ArrayList<>());
        }
        for (PlanningEtude a : rendered) {
            if (a.getDateSeance() != null && byDay.containsKey(a.getDateSeance())) {
                if ("all".equals(activeTypeFilter) ||
                        activeTypeFilter.equals(a.getTypeActivite())) {
                    byDay.get(a.getDateSeance()).add(a);
                }
            }
        }

        // Render per day
        Pane[] cols = {dayCol0, dayCol1, dayCol2, dayCol3, dayCol4, dayCol5, dayCol6};
        for (int i = 0; i < 7; i++) {
            LocalDate day = monday.plusDays(i);
            List<PlanningEtude> dayList = byDay.getOrDefault(day, Collections.emptyList());
            dayList.sort(Comparator.comparingInt(PlanningEtude::startMinutes));
            assignLanes(dayList);
            for (PlanningEtude a : dayList) {
                VBox card = buildActivityCard(a, cols[i]);
                cols[i].getChildren().add(card);
            }
        }
    }

    /**
     * Split activities that span midnight into multiple single-day chunks.
     * Mirrors splitActivitiesForRender() in JS.
     */
    private List<PlanningEtude> splitForRender(List<PlanningEtude> activities, LocalDate monday) {
        List<PlanningEtude> result = new ArrayList<>();
        LocalDate sunday = monday.plusDays(6);

        for (PlanningEtude a : activities) {
            if (a.getDateSeance() == null || a.getHeureDebut() == null
                    || a.getDureePrevue() == null || a.getDureePrevue() <= 0) continue;

            int remaining   = a.getDureePrevue();
            LocalDate curDate  = a.getDateSeance();
            LocalTime curStart = a.getHeureDebut();

            while (remaining > 0) {
                int startMin     = curStart.getHour() * 60 + curStart.getMinute();
                int leftInDay    = 1440 - startMin;
                int chunk        = Math.min(remaining, leftInDay);

                if (!curDate.isBefore(monday) && !curDate.isAfter(sunday)) {
                    PlanningEtude clone = cloneWithRender(a, curDate, curStart, chunk);
                    result.add(clone);
                }

                remaining -= chunk;
                if (remaining > 0) {
                    curDate = curDate.plusDays(1);
                    curStart = LocalTime.MIDNIGHT;
                }
            }
        }
        return result;
    }

    private PlanningEtude cloneWithRender(PlanningEtude src, LocalDate date, LocalTime start, int duration) {
        PlanningEtude c = new PlanningEtude();
        c.setId(src.getId());
        c.setTitreP(src.getTitreP());
        c.setDateSeance(date);
        c.setHeureDebut(start);
        c.setDureePrevue(duration);
        c.setTypeActivite(src.getTypeActivite());
        c.setCouleurActivite(src.getCouleurActivite());
        c.setEtat(src.getEtat());
        c.setNotesPers(src.getNotesPers());
        c.setMatiere(src.getMatiere());
        // Keep reference to original for edit
        c.setHeureFin(src.getHeureDebut()); // repurposed: stores original start
        c.setDureeReelle(src.getDureePrevue()); // stores original duration
        return c;
    }

    /** Assign _lane and _laneCount to activities in a single day (overlap columns). */
    private void assignLanes(List<PlanningEtude> dayActivities) {
        // Each activity gets a "lane" index for side-by-side rendering
        // Uses a simple greedy column algorithm
        List<Integer> laneEnds = new ArrayList<>(); // laneEnds[i] = end minute of last activity in lane i

        for (PlanningEtude a : dayActivities) {
            int start = a.startMinutes();
            int end   = start + (a.getDureePrevue() != null ? a.getDureePrevue() : 0);

            // Find a free lane
            int lane = -1;
            for (int i = 0; i < laneEnds.size(); i++) {
                if (laneEnds.get(i) <= start) { lane = i; laneEnds.set(i, end); break; }
            }
            if (lane == -1) { lane = laneEnds.size(); laneEnds.add(end); }

            // Store lane in unused field (we use matiere field as scratch)
            a.setMatiere("lane=" + lane + ";total=" + laneEnds.size());
        }
        // Second pass: fix total (all in same group share same total)
        int total = laneEnds.size();
        for (PlanningEtude a : dayActivities) {
            String m = a.getMatiere();
            if (m != null && m.startsWith("lane=")) {
                int lane = Integer.parseInt(m.split(";")[0].split("=")[1]);
                a.setMatiere("lane=" + lane + ";total=" + total);
            }
        }
    }

    private int laneOf(PlanningEtude a) {
        try {
            String m = a.getMatiere();
            if (m != null && m.startsWith("lane="))
                return Integer.parseInt(m.split(";")[0].split("=")[1]);
        } catch (Exception ignored) {}
        return 0;
    }

    private int laneTotalOf(PlanningEtude a) {
        try {
            String m = a.getMatiere();
            if (m != null && m.contains(";total="))
                return Integer.parseInt(m.split(";total=")[1]);
        } catch (Exception ignored) {}
        return 1;
    }

    // ════════════════════════════════════════════════════════════════════
    // ACTIVITY CARD BUILDER
    // ════════════════════════════════════════════════════════════════════

    private VBox buildActivityCard(PlanningEtude a, Pane column) {
        VBox card = new VBox(2);
        card.setUserData("activity-card");
        card.getStyleClass().add("activity-card");

        if ("done".equals(a.getEtat()))    card.getStyleClass().add("activity-card-done");
        if ("skipped".equals(a.getEtat())) card.getStyleClass().add("activity-card-skipped");

        // Background color
        String hex = a.getCouleurActivite() != null ? a.getCouleurActivite() : "#dfe6e9";
        try {
            Color base = Color.web(hex);
            card.setBackground(new Background(new BackgroundFill(
                    base.deriveColor(0, 1, 1, 0.55), new CornerRadii(8), Insets.EMPTY)));
            card.setBorder(new Border(new BorderStroke(
                    base.deriveColor(0, 1, 0.8, 1), BorderStrokeStyle.SOLID,
                    new CornerRadii(8), new BorderWidths(2))));
        } catch (Exception ignored) {}

        // Position & size
        int startMin  = a.startMinutes();
        int duration  = a.getDureePrevue() != null ? a.getDureePrevue() : 30;
        double topPx  = (startMin / 60.0) * ROW_HEIGHT;
        double heightPx = (duration / 60.0) * ROW_HEIGHT;

        card.setLayoutY(topPx);
        card.setPrefHeight(Math.max(heightPx, 20));

        int lane  = laneOf(a);
        int total = laneTotalOf(a);
        double pct = 100.0 / total;
        double gap = 8.0;
        // setLayoutX and prefWidth set after column width is known → bind to column width
        column.widthProperty().addListener((obs, ov, nv) -> {
            double w = nv.doubleValue();
            card.setLayoutX(pct / 100.0 * lane * w + gap / 2.0);
            card.setPrefWidth(pct / 100.0 * w - gap);
        });
        // Initial set
        double colW = column.getWidth() > 0 ? column.getWidth() : 160;
        card.setLayoutX(pct / 100.0 * lane * colW + gap / 2.0);
        card.setPrefWidth(pct / 100.0 * colW - gap);

        card.setPadding(new Insets(4, 6, 4, 6));

        Label titleLbl = new Label(a.getTitreP());
        titleLbl.getStyleClass().add("activity-title");
        titleLbl.setWrapText(true);
        if ("done".equals(a.getEtat())) titleLbl.setStyle("-fx-strikethrough: true;");

        Label metaLbl = new Label(a.durationLabel() + "  " +
                (a.getHeureDebut() != null ? a.getHeureDebut().toString().substring(0, 5) : ""));
        metaLbl.getStyleClass().add("activity-meta");

        card.getChildren().addAll(titleLbl, metaLbl);
        card.setCursor(Cursor.HAND);

        // Store activity id for event handlers
        card.setId("activity-" + a.getId());
        int activityId = a.getId();

        // Single click → toggle done
        card.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 1) {
                if (!"skipped".equals(a.getEtat())) toggleActivity(activityId, card);
                e.consume();
            }
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                openEditDialog(activityId);
                e.consume();
            }
        });

        // Drag & drop (source)
        card.setOnDragDetected(e -> {
            dragActivityId = activityId;
            dragOffsetY    = e.getY();
            Dragboard db = card.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent cc = new ClipboardContent();
            cc.putString(String.valueOf(activityId));
            db.setContent(cc);
            e.consume();
        });

        return card;
    }

    // ════════════════════════════════════════════════════════════════════
    // DAY COLUMN EVENTS  (double-click create / drag-drop target)
    // ════════════════════════════════════════════════════════════════════

    @FXML private void onDayColClick(javafx.scene.input.MouseEvent e) {
        if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
            Pane col = (Pane) e.getSource();
            openCreateDialog(col, e.getY());
        }
    }

    @FXML private void onDayColMousePressed(javafx.scene.input.MouseEvent e) {
        // Accept drag-over
        Pane col = (Pane) e.getSource();
        col.setOnDragOver(ev -> {
            if (ev.getDragboard().hasString()) ev.acceptTransferModes(TransferMode.MOVE);
            ev.consume();
        });
        col.setOnDragDropped(ev -> {
            Dragboard db = ev.getDragboard();
            if (db.hasString()) {
                int id = Integer.parseInt(db.getString());
                LocalDate targetDate = (LocalDate) col.getUserData();
                LocalTime targetTime = yToTime(ev.getY());
                moveActivity(id, targetDate, targetTime);
            }
            ev.setDropCompleted(true);
            ev.consume();
        });
    }

    private LocalTime yToTime(double y) {
        int totalMinutes = (int) Math.max(0, Math.min(1439, (y / ROW_HEIGHT) * 60));
        return LocalTime.of(totalMinutes / 60, totalMinutes % 60);
    }

    // ════════════════════════════════════════════════════════════════════
    // TYPE FILTER
    // ════════════════════════════════════════════════════════════════════

    private void renderTypeFilter(List<String[]> types) {
        typeFilter.getChildren().clear();

        Button allBtn = pillButton("Tous", "all", null);
        if ("all".equals(activeTypeFilter)) {
            allBtn.getStyleClass().add("type-pill-active");
        }
        typeFilter.getChildren().add(allBtn);

        for (String[] t : types) {
            Button btn = pillButton(t[0], t[0], t[1]);
            if (t[0].equals(activeTypeFilter)) btn.getStyleClass().add("type-pill-active");
            typeFilter.getChildren().add(btn);
        }
    }

    private Button pillButton(String label, String filterValue, String colorHex) {
        Button btn = new Button(label);
        btn.getStyleClass().add("type-pill");
        if (colorHex != null) {
            try {
                Color c = Color.web(colorHex);
                btn.setBorder(new Border(new BorderStroke(c,
                        BorderStrokeStyle.SOLID, new CornerRadii(999), new BorderWidths(1.5))));
            } catch (Exception ignored) {}
        }
        btn.setOnAction(e -> {
            activeTypeFilter = filterValue;
            typeFilter.getChildren().forEach(n -> n.getStyleClass().remove("type-pill-active"));
            btn.getStyleClass().add("type-pill-active");
            paintActivities(cachedActivities);
        });
        return btn;
    }

    // ════════════════════════════════════════════════════════════════════
    // DIALOGS  (Create / Edit / Delete)
    // ════════════════════════════════════════════════════════════════════

    /** Opens the "create activity" dialog. */
    private void openCreateDialog(Pane col, double clickY) {
        LocalDate date = (LocalDate) col.getUserData();
        LocalTime time = yToTime(clickY);

        Dialog<PlanningEtude> dlg = buildActivityDialog(null, date, time);
        dlg.setTitle("Ajouter une activité");
        dlg.showAndWait().ifPresent(p -> {
            try {
                service.create(p);
                refreshWeek();
            } catch (IllegalArgumentException ex) {
                showErrors(ex.getMessage());
            } catch (SQLException ex) {
                showErrors("Erreur base de données : " + ex.getMessage());
            }
        });
    }

    /** Opens the "edit / delete" dialog for an existing activity. */
    private void openEditDialog(int activityId) {
        try {
            PlanningEtude existing = service.findById(activityId).orElse(null);
            if (existing == null) { showErrors("Activité introuvable."); return; }

            // Action picker
            ChoiceDialog<String> actionDlg = new ChoiceDialog<>("Modifier", "Modifier", "Supprimer");
            actionDlg.setTitle("Activité : " + existing.getTitreP());
            actionDlg.setHeaderText("Date : " + existing.getDateSeance() +
                    " | Début : " + (existing.getHeureDebut() != null ?
                    existing.getHeureDebut().toString().substring(0, 5) : "—"));
            actionDlg.setContentText("Action :");
            actionDlg.showAndWait().ifPresent(action -> {
                if ("Supprimer".equals(action)) {
                    confirmAndDelete(activityId);
                } else {
                    openUpdateDialog(existing);
                }
            });
        } catch (SQLException ex) {
            showErrors("Erreur : " + ex.getMessage());
        }
    }

    private void openUpdateDialog(PlanningEtude existing) {
        Dialog<PlanningEtude> dlg = buildActivityDialog(existing, existing.getDateSeance(), existing.getHeureDebut());
        dlg.setTitle("Modifier l'activité");
        dlg.showAndWait().ifPresent(p -> {
            try {
                service.update(p);
                refreshWeek();
            } catch (IllegalArgumentException ex) {
                showErrors(ex.getMessage());
            } catch (SQLException ex) {
                showErrors("Erreur base de données : " + ex.getMessage());
            }
        });
    }

    private void confirmAndDelete(int activityId) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Êtes-vous sûr de vouloir supprimer cette activité ?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Supprimer");
        confirm.showAndWait().filter(r -> r == ButtonType.YES).ifPresent(r -> {
            try {
                service.delete(activityId);
                refreshWeek();
            } catch (SQLException ex) {
                showErrors("Erreur lors de la suppression : " + ex.getMessage());
            }
        });
    }

    private Label buildErrorLabel() {
        Label lbl = new Label();
        lbl.setStyle("-fx-text-fill: #b91c1c;");
        lbl.setVisible(false);
        lbl.setManaged(false);
        return lbl;
    }

    private void showFieldError(Label label, Control field, String msg) {
        label.setText(msg);
        label.setVisible(true);
        label.setManaged(true);
        field.setStyle("-fx-border-color: #b91c1c; -fx-border-width: 1px; -fx-border-radius: 3px; -fx-background-radius: 3px;");
    }

    private void clearFieldError(Label label, Control field) {
        label.setVisible(false);
        label.setManaged(false);
        field.setStyle("");
    }

    /**
     * Builds a reusable Dialog<PlanningEtude> for create or edit.
     * Pre-fills fields when {@code existing} is not null.
     */
    private Dialog<PlanningEtude> buildActivityDialog(
            PlanningEtude existing, LocalDate date, LocalTime time) {

        Dialog<PlanningEtude> dlg = new Dialog<>();
        dlg.setHeaderText(existing == null ? "Nouvelle activité" : "Modifier : " + existing.getTitreP());

        ButtonType saveType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        // ── CSS ──────────────────────────────────────────────────────
        dlg.getDialogPane().getStylesheets().add(
            getClass().getResource("/css/planning.css").toExternalForm()
        );

        // ── Form ────────────────────────────────────────────────────
        GridPane form = new GridPane();
        form.setHgap(10); form.setVgap(10);
        form.setPadding(new Insets(16));
        form.getStyleClass().add("dialog-form");

        TextField titreField = new TextField();
        titreField.setPromptText("Titre *");
        titreField.setPrefWidth(320);
        titreField.getStyleClass().add("input-field");

        DatePicker datePicker = new DatePicker(date);
        datePicker.getStyleClass().add("input-field");

        TextField heureField = new TextField(time != null ? time.toString().substring(0, 5) : "");
        heureField.setPromptText("HH:mm *");
        heureField.getStyleClass().add("input-field");

        TextField heuresField   = new TextField();
        heuresField.setPromptText("HH");
        heuresField.setPrefWidth(60);
        heuresField.getStyleClass().add("input-field");
        TextField minutesField  = new TextField();
        minutesField.setPromptText("MM");
        minutesField.setPrefWidth(60);
        minutesField.getStyleClass().add("input-field");

        // Type: combo + free text
        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.setPromptText("Type existant");
        typeCombo.setEditable(false);
        typeCombo.getStyleClass().add("input-field");
        TextField typeField = new TextField();
        typeField.setPromptText("Ou nouveau type *");
        typeField.getStyleClass().add("input-field");

        ColorPicker colorPicker = new ColorPicker(Color.web("#fcd34d"));
        colorPicker.getStyleClass().add("input-field");

        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Notes personnelles…");
        notesArea.setPrefRowCount(3);
        notesArea.getStyleClass().add("input-field");

        Label titreError = buildErrorLabel();
        titreError.getStyleClass().add("error-text");
        Label heureError = buildErrorLabel();
        heureError.getStyleClass().add("error-text");
        Label dureeError = buildErrorLabel();
        dureeError.getStyleClass().add("error-text");
        Label typeError  = buildErrorLabel();
        typeError.getStyleClass().add("error-text");

        // Populate type combo
        try {
            List<String[]> types = service.findDistinctTypesWithColor();
            types.forEach(t -> typeCombo.getItems().add(t[0]));
            // auto-fill color on combo change
            typeCombo.setOnAction(e -> {
                String sel = typeCombo.getValue();
                if (sel == null) return;
                types.stream().filter(t -> t[0].equals(sel)).findFirst()
                        .ifPresent(t -> {
                            if (t[1] != null) try { colorPicker.setValue(Color.web(t[1])); } catch (Exception ignored) {}
                        });

                // Clear type error when combo is selected
                if (!sel.trim().isEmpty()) clearFieldError(typeError, typeField);
            });
        } catch (SQLException ex) { /* ignore – combo stays empty */ }

        // ── Real-time validations ────────────────────────────────────
        titreField.textProperty().addListener((obs, old, val) -> {
            if (val.trim().isEmpty()) showFieldError(titreError, titreField, "Titre obligatoire");
            else clearFieldError(titreError, titreField);
        });

        heureField.textProperty().addListener((obs, old, val) -> {
            if (val.trim().isEmpty()) {
                clearFieldError(heureError, heureField);
            } else {
                try {
                    LocalTime.parse(val.trim(), DateTimeFormatter.ofPattern("H:mm"));
                    clearFieldError(heureError, heureField);
                } catch (Exception e) {
                    showFieldError(heureError, heureField, "Format HH:mm invalide");
                }
            }
        });

        javafx.beans.value.ChangeListener<String> dureeListener = (obs, old, val) -> {
            String hStr = heuresField.getText().trim();
            String mStr = minutesField.getText().trim();
            if (hStr.isEmpty() && mStr.isEmpty()) {
                clearFieldError(dureeError, heuresField);
                clearFieldError(dureeError, minutesField);
                return;
            }
            try {
                int h = hStr.isEmpty() ? 0 : Integer.parseInt(hStr);
                int m = mStr.isEmpty() ? 0 : Integer.parseInt(mStr);
                if (h < 0 || m < 0 || m > 59 || (h == 0 && m == 0)) {
                    showFieldError(dureeError, heuresField, "Durée invalide");
                    showFieldError(dureeError, minutesField, "Durée invalide");
                } else {
                    clearFieldError(dureeError, heuresField);
                    clearFieldError(dureeError, minutesField);
                }
            } catch (NumberFormatException e) {
                showFieldError(dureeError, heuresField, "Durée invalide");
                showFieldError(dureeError, minutesField, "Durée invalide");
            }
        };
        heuresField.textProperty().addListener(dureeListener);
        minutesField.textProperty().addListener(dureeListener);

        typeField.textProperty().addListener((obs, old, val) -> {
            String t1 = typeCombo.getValue() != null ? typeCombo.getValue() : "";
            if (val.trim().isEmpty() && t1.isEmpty()) showFieldError(typeError, typeField, "Type obligatoire");
            else clearFieldError(typeError, typeField);
        });

        // Suggestion: fill time+duration when title typed
        titreField.textProperty().addListener((obs, ov, nv) -> {
            if (nv.length() < 2) return;
            try {
                service.findLastByTitlePrefix(nv).ifPresent(sug -> {
                    if (heureField.getText().isBlank() && sug.getHeureDebut() != null)
                        heureField.setText(sug.getHeureDebut().toString().substring(0, 5));
                    if (heuresField.getText().isBlank() && minutesField.getText().isBlank()
                            && sug.getDureePrevue() != null) {
                        heuresField.setText(String.valueOf(sug.getDureePrevue() / 60));
                        minutesField.setText(String.format("%02d", sug.getDureePrevue() % 60));
                    }
                });
            } catch (SQLException ignored) {}
        });

        // Pre-fill for edit
        if (existing != null) {
            titreField.setText(existing.getTitreP() != null ? existing.getTitreP() : "");
            datePicker.setValue(existing.getDateSeance());
            if (existing.getHeureDebut() != null)
                heureField.setText(existing.getHeureDebut().toString().substring(0, 5));
            if (existing.getDureePrevue() != null) {
                heuresField.setText(String.valueOf(existing.getDureePrevue() / 60));
                minutesField.setText(String.format("%02d", existing.getDureePrevue() % 60));
            }
            if (existing.getTypeActivite() != null) {
                if (typeCombo.getItems().contains(existing.getTypeActivite()))
                    typeCombo.setValue(existing.getTypeActivite());
                else
                    typeField.setText(existing.getTypeActivite());
            }
            if (existing.getCouleurActivite() != null)
                try { colorPicker.setValue(Color.web(existing.getCouleurActivite())); } catch (Exception ignored) {}
            if (existing.getNotesPers() != null) notesArea.setText(existing.getNotesPers());
        }

        // Error label inside dialog
        Label errLbl = new Label();
        errLbl.setStyle("-fx-text-fill: #b91c1c; -fx-wrap-text: true;");
        errLbl.setWrapText(true);

        int row = 0;
        VBox titreBox = new VBox(2, titreField, titreError);
        VBox heureBox = new VBox(2, heureField, heureError);
        HBox durBoxInner = new HBox(4, heuresField, new Label(":"), minutesField);
        VBox dureeBox = new VBox(2, durBoxInner, dureeError);
        VBox typeBox  = new VBox(2, typeField, typeError);

        form.add(new Label("Titre *"),                  0, row); form.add(titreBox,       1, row++);
        form.add(new Label("Date *"),                   0, row); form.add(datePicker,     1, row++);
        form.add(new Label("Heure début *"),            0, row); form.add(heureBox,       1, row++);
        form.add(new Label("Durée (HH : MM) *"),        0, row); form.add(dureeBox,       1, row++);
        form.add(new Label("Type (sélection)"),         0, row); form.add(typeCombo,      1, row++);
        form.add(new Label("Ou nouveau type"),          0, row); form.add(typeBox,        1, row++);
        form.add(new Label("Couleur"),                  0, row); form.add(colorPicker,    1, row++);
        form.add(new Label("Notes"),                    0, row); form.add(notesArea,      1, row++);
        form.add(errLbl,                                0, row, 2, 1);

        dlg.getDialogPane().setContent(form);

        // ── Result converter ────────────────────────────────────────
        dlg.setResultConverter(btn -> {
            if (btn != saveType) return null;
            errLbl.setText("");

            // Collect fields
            String titre   = titreField.getText().trim();
            LocalDate d    = datePicker.getValue();
            String heureStr = heureField.getText().trim();
            String hStr    = heuresField.getText().trim();
            String mStr    = minutesField.getText().trim();
            String type    = typeField.getText().trim().isEmpty()
                    ? (typeCombo.getValue() != null ? typeCombo.getValue() : "")
                    : typeField.getText().trim();
            String color   = toHex(colorPicker.getValue());
            String notes   = notesArea.getText().trim();

            // Parse time
            LocalTime heure = null;
            if (!heureStr.isEmpty()) {
                try { heure = LocalTime.parse(heureStr, DateTimeFormatter.ofPattern("H:mm")); }
                catch (Exception ex) { errLbl.setText("Heure invalide (HH:mm)"); return null; }
            }

            // Parse duration
            Integer duree = null;
            if (!hStr.isEmpty() || !mStr.isEmpty()) {
                try {
                    int h = hStr.isEmpty() ? 0 : Integer.parseInt(hStr);
                    int min = mStr.isEmpty() ? 0 : Integer.parseInt(mStr);
                    if (h < 0 || min < 0 || min > 59) throw new NumberFormatException();
                    duree = h * 60 + min;
                    if (duree <= 0) throw new NumberFormatException();
                } catch (NumberFormatException ex) {
                    errLbl.setText("Durée invalide"); return null;
                }
            }

            PlanningEtude p = existing != null ? existing : new PlanningEtude();
            p.setTitreP(titre);
            p.setDateSeance(d);
            p.setHeureDebut(heure);
            p.setDureePrevue(duree);
            p.setTypeActivite(type);
            p.setCouleurActivite(color);
            p.setNotesPers(notes.isEmpty() ? null : notes);
            if (existing == null) p.setEtat("to do");

            // Local pre-validate (service will do full validation too)
            List<String> errs = new ArrayList<>();
            if (titre.isEmpty())   errs.add("Le titre est obligatoire.");
            if (d == null)         errs.add("La date est obligatoire.");
            if (heure == null)     errs.add("L'heure est obligatoire.");
            if (duree == null)     errs.add("La durée est obligatoire.");
            if (type.isEmpty())    errs.add("Le type est obligatoire.");
            if (!errs.isEmpty()) {
                errLbl.setText(String.join("\n", errs));
                return null;
            }
            return p;
        });

        return dlg;
    }

    // ════════════════════════════════════════════════════════════════════
    // TOGGLE
    // ════════════════════════════════════════════════════════════════════

    private void toggleActivity(int id, VBox card) {
        try {
            String newEtat = service.toggle(id);
            // Update card style immediately without full repaint
            card.getStyleClass().removeAll("activity-card-done");
            if ("done".equals(newEtat)) {
                card.getStyleClass().add("activity-card-done");
                card.getChildren().stream()
                        .filter(n -> n instanceof Label && ((Label) n).getStyleClass().contains("activity-title"))
                        .forEach(n -> ((Label) n).setStyle("-fx-strikethrough: true;"));
            } else {
                card.getChildren().stream()
                        .filter(n -> n instanceof Label && ((Label) n).getStyleClass().contains("activity-title"))
                        .forEach(n -> ((Label) n).setStyle(""));
            }
            // update cached list
            cachedActivities.stream()
                    .filter(a -> a.getId() == id)
                    .forEach(a -> a.setEtat(newEtat));
        } catch (Exception ex) {
            showErrors("Impossible de mettre à jour le statut : " + ex.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // MOVE  (drag & drop)
    // ════════════════════════════════════════════════════════════════════

    private void moveActivity(int id, LocalDate newDate, LocalTime newTime) {
        try {
            service.move(id, newDate, newTime);
            refreshWeek();
        } catch (IllegalArgumentException ex) {
            showErrors(ex.getMessage());
        } catch (SQLException ex) {
            showErrors("Erreur lors du déplacement : " + ex.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // REMINDERS POLLER
    // ════════════════════════════════════════════════════════════════════

    private void startReminderPoller() {
        reminderExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "reminder-poller");
            t.setDaemon(true);
            return t;
        });
        reminderExecutor.scheduleAtFixedRate(() -> {
            try {
                Map<String, List<String[]>> reminders = service.getReminders();
                List<String> messages = new ArrayList<>();

                for (String[] item : reminders.getOrDefault("upcoming", Collections.emptyList())) {
                    String key = "upcoming-" + item[0];
                    if (reminderCache.add(key)) messages.add(item[1]);
                }
                for (String[] item : reminders.getOrDefault("in_progress", Collections.emptyList())) {
                    String key = "inprogress-" + item[0];
                    if (reminderCache.add(key)) messages.add(item[1]);
                }

                if (!messages.isEmpty()) {
                    String text = String.join("\n", messages);
                    Platform.runLater(() -> showReminders(text));
                }
            } catch (Exception ignored) {}
        }, 0, 60, TimeUnit.SECONDS);
    }

    private void showReminders(String text) {
        planningReminders.setText(text);
        planningReminders.setManaged(true);
        planningReminders.setVisible(true);
        // Auto-hide after 90 seconds
        Timeline hide = new Timeline(new KeyFrame(Duration.seconds(90), e -> {
            planningReminders.setVisible(false);
            planningReminders.setManaged(false);
            planningReminders.setText("");
        }));
        hide.play();
    }

    // ════════════════════════════════════════════════════════════════════
    // REFRESH
    // ════════════════════════════════════════════════════════════════════

    private void updateAll() {
        renderCalendar();
        renderWeekSkeleton();
        refreshWeek();
    }

    private void refreshWeek() {
        clearErrors();
        try {
            cachedActivities = service.findByWeek(selectedDate);
            List<String[]> types = service.findDistinctTypesWithColor();
            renderTypeFilter(types);
            paintActivities(cachedActivities);
        } catch (SQLException ex) {
            showErrors("Impossible de charger le planning : " + ex.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // ERROR HELPERS
    // ════════════════════════════════════════════════════════════════════

    private void showErrors(String msg) {
        planningErrors.setText(msg);
        planningErrors.setManaged(true);
        planningErrors.setVisible(true);
    }



    private void clearErrors() {
        planningErrors.setText("");
        planningErrors.setManaged(false);
        planningErrors.setVisible(false);
    }

    // ════════════════════════════════════════════════════════════════════
    // UTILITIES
    // ════════════════════════════════════════════════════════════════════

    private static LocalDate weekStart(LocalDate d) {
        return d.with(DayOfWeek.MONDAY);
    }
    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String toHex(Color c) {
        return String.format("#%02X%02X%02X",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }

    // ════════════════════════════════════════════════════════════════════
    // GUIDE
    // ════════════════════════════════════════════════════════════════════

    @FXML
    private void handleGuide() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Guide d'utilisation - Planning");
        alert.setHeaderText("Comment utiliser le planning MentorAI");

        VBox content = new VBox(12);
        content.setPadding(new Insets(10));
        content.setPrefWidth(460);

        content.getChildren().addAll(
            createGuideSection("🟦 Création d'une activité :",
                "• Double-cliquez sur une cellule du planning\n• Remplissez les informations (titre, heure, durée, type)\n• Cliquez sur « Enregistrer »"),
            createGuideSection("🟩 Modification :",
                "• Double-cliquez sur une activité existante\n• Choisissez « Modifier » dans le menu\n• Modifiez les informations et enregistrez"),
            createGuideSection("🟥 Suppression :",
                "• Double-cliquez sur une activité\n• Choisissez « Supprimer » dans le menu\n• Confirmez la suppression"),
            createGuideSection("🟨 Drag & Drop :",
                "• Cliquez et maintenez une activité\n• Glissez-la vers un autre créneau ou un autre jour\n• Relâchez pour changer l'horaire"),
            createGuideSection("🟪 Statut d'une activité :",
                "• Cliquez une fois sur une activité pour alterner entre\n  « to do » et « done » (coché / barré)"),
            createGuideSection("🟫 Navigation dans le calendrier :",
                "• Utilisez les boutons ‹ › pour changer de mois\n• Utilisez « -1 an » / « +1 an » pour changer d'année\n• Cliquez sur un jour pour l'afficher dans le planning"),
            createGuideSection("🟧 Conseils :",
                "• Respectez le format HH:mm pour les heures\n• Ne laissez pas les champs obligatoires (*) vides\n• Utilisez le filtre de type pour n'afficher qu'une catégorie")
        );

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(420);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        alert.getDialogPane().setContent(scrollPane);
        alert.getDialogPane().setPrefWidth(520);
        alert.showAndWait();
    }

    private VBox createGuideSection(String title, String text) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #102c59;");
        Label textLabel = new Label(text);
        textLabel.setWrapText(true);
        textLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #334155;");
        VBox section = new VBox(4, titleLabel, textLabel);
        section.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-padding: 10;");
        return section;
    }
}