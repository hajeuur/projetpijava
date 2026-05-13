package com.mentorai.controllers;

import com.mentorai.entities.Carnet;
import com.mentorai.entities.PlanningEtude;
import com.mentorai.services.CarnetService;
import com.mentorai.services.PlanningService;
import edu.connection3a36.controllers.MainController;
import edu.connection3a36.entities.Utilisateur;
import edu.connection3a36.tools.SessionManager;
import com.mentorai.utils.MyConnection;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.sql.*;
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
    private static final int COL_WIDTH  = 60;   // px per hour (horizontal axis)
    private static final int DAY_ROW_H  = 72;   // px per day row
    private static final int AXIS_W     = 90;   // px for left day-label column
    private static final int HEADER_H   = 36;   // px for top hour-header row

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

    @FXML private Button personalityTestBtn;

    // Transposed grid — built programmatically by buildWeekGrid()
    private final Pane[]  dayRows   = new Pane[7];
    private final Label[] dayLabels = new Label[7];

    // ── State ────────────────────────────────────────────────────────────
    private final PlanningService service    = new PlanningService();
    private final CarnetService   carnetSvc  = new CarnetService();
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
        buildWeekGrid();
        updateAll();
        startReminderPoller();
        refreshPersonalityStatus();

        // Auto-refresh: redraw week every 30 s so changes appear without navigation
        javafx.animation.Timeline autoRefresh = new javafx.animation.Timeline(
            new KeyFrame(Duration.seconds(30), e -> refreshWeek()));
        autoRefresh.setCycleCount(javafx.animation.Animation.INDEFINITE);
        autoRefresh.play();

        // Scroll to show 08h area on open
        Platform.runLater(() -> {
            double fullW = COL_WIDTH * 24.0;
            double viewW = weekScrollPane.getViewportBounds().getWidth();
            double target = (8.0 * COL_WIDTH - 8) / Math.max(1, fullW - viewW);
            weekScrollPane.setHvalue(Math.max(0, Math.min(1, target)));
        });
    }

    // ════════════════════════════════════════════════════════════════════
    // CALENDAR  (left panel)
    // ════════════════════════════════════════════════════════════════════

    @FXML private void onCalPrev()     { selectedDate = selectedDate.minusMonths(1); updateAll(); }
    @FXML private void onCalNext()     { selectedDate = selectedDate.plusMonths(1);  updateAll(); }
    @FXML private void onCalPrevYear() { selectedDate = selectedDate.minusYears(1);  updateAll(); }
    @FXML private void onCalNextYear() { selectedDate = selectedDate.plusYears(1);   updateAll(); }



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

    // ════════════════════════════════════════════════════════════════════
    // BUILD WEEK GRID  (transposed: days = rows, hours = columns)
    // ════════════════════════════════════════════════════════════════════

    private void buildWeekGrid() {
        weekGridPane.getChildren().clear();
        weekGridPane.getColumnConstraints().clear();
        weekGridPane.getRowConstraints().clear();

        // Col 0: left axis (day labels)
        ColumnConstraints axisCol = new ColumnConstraints(AXIS_W, AXIS_W, AXIS_W);
        weekGridPane.getColumnConstraints().add(axisCol);
        // Cols 1-24: one per hour
        for (int h = 0; h < 24; h++) {
            ColumnConstraints cc = new ColumnConstraints(COL_WIDTH, COL_WIDTH, COL_WIDTH);
            weekGridPane.getColumnConstraints().add(cc);
        }

        // Row 0: hour headers
        RowConstraints hdrRow = new RowConstraints(HEADER_H, HEADER_H, HEADER_H);
        weekGridPane.getRowConstraints().add(hdrRow);
        // Rows 1-7: one per day
        for (int d = 0; d < 7; d++) {
            RowConstraints rc = new RowConstraints(DAY_ROW_H, DAY_ROW_H, DAY_ROW_H);
            weekGridPane.getRowConstraints().add(rc);
        }

        // Corner cell
        Pane corner = new Pane();
        corner.getStyleClass().add("week-header-corner");
        weekGridPane.add(corner, 0, 0);

        // Hour header labels (row 0, cols 1-24)
        for (int h = 0; h < 24; h++) {
            Label lbl = new Label(String.format("%02dh", h));
            lbl.setMaxWidth(Double.MAX_VALUE);
            lbl.setMaxHeight(Double.MAX_VALUE);
            lbl.setAlignment(Pos.CENTER);
            lbl.getStyleClass().add("hour-header");
            weekGridPane.add(lbl, h + 1, 0);
        }

        // Day rows (rows 1-7): label in col 0, Pane spanning cols 1-24
        for (int d = 0; d < 7; d++) {
            dayLabels[d] = new Label();
            dayLabels[d].setMaxWidth(Double.MAX_VALUE);
            dayLabels[d].setMaxHeight(Double.MAX_VALUE);
            dayLabels[d].setAlignment(Pos.CENTER_LEFT);
            dayLabels[d].setPadding(new Insets(0, 8, 0, 8));
            dayLabels[d].getStyleClass().add("week-day-label");
            weekGridPane.add(dayLabels[d], 0, d + 1);

            dayRows[d] = new Pane();
            dayRows[d].getStyleClass().add("week-day-row");
            dayRows[d].setPrefWidth(COL_WIDTH * 24.0);
            dayRows[d].setMinWidth(COL_WIDTH * 24.0);
            dayRows[d].setPrefHeight(DAY_ROW_H);
            dayRows[d].setMinHeight(DAY_ROW_H);
            GridPane.setColumnSpan(dayRows[d], 24);
            weekGridPane.add(dayRows[d], 1, d + 1);

            final int idx = d;
            dayRows[d].setOnMouseClicked(e -> {
                if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY && e.getClickCount() == 2)
                    openCreateDialog(dayRows[idx], e.getX());
            });
            dayRows[d].setOnMousePressed(e -> setupDragDrop(dayRows[idx]));
        }
    }

    /** Update day-label texts and selected-day highlight. */
    private void renderWeekSkeleton() {
        LocalDate monday = weekStart(selectedDate);
        LocalDate sunday = monday.plusDays(6);
        weekRange.setText(monday.format(DISPLAY_FMT) + " → " + sunday.format(DISPLAY_FMT));

        for (int i = 0; i < 7; i++) {
            LocalDate d = monday.plusDays(i);
            if (dayLabels[i] == null) continue;
            dayLabels[i].setText(d.format(DAY_FMT));
            dayLabels[i].setUserData(d);
            dayLabels[i].getStyleClass().removeAll("week-day-label-selected");
            if (d.equals(selectedDate)) dayLabels[i].getStyleClass().add("week-day-label-selected");

            dayRows[i].setUserData(d);

            // click on label selects that day
            final LocalDate fd = d;
            dayLabels[i].setOnMouseClicked(e -> { selectedDate = fd; updateAll(); });
            dayLabels[i].setCursor(Cursor.HAND);
        }
    }

    /** Remove all activity cards from all day rows. */
    private void clearActivityCards() {
        for (Pane row : dayRows) {
            if (row != null) row.getChildren().removeIf(n -> "activity-card".equals(n.getUserData()));
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // PAINT ACTIVITIES
    // ════════════════════════════════════════════════════════════════════

    private void paintActivities(List<PlanningEtude> activities) {
        clearActivityCards();
        LocalDate monday = weekStart(selectedDate);
        List<PlanningEtude> rendered = splitForRender(activities, monday);

        Map<LocalDate, List<PlanningEtude>> byDay = new LinkedHashMap<>();
        for (LocalDate d = monday; !d.isAfter(monday.plusDays(6)); d = d.plusDays(1))
            byDay.put(d, new ArrayList<>());
        for (PlanningEtude a : rendered) {
            if (a.getDateSeance() != null && byDay.containsKey(a.getDateSeance()))
                if ("all".equals(activeTypeFilter) || activeTypeFilter.equals(a.getTypeActivite()))
                    byDay.get(a.getDateSeance()).add(a);
        }

        for (int i = 0; i < 7; i++) {
            LocalDate day = monday.plusDays(i);
            List<PlanningEtude> dayList = byDay.getOrDefault(day, Collections.emptyList());
            dayList.sort(Comparator.comparingInt(PlanningEtude::startMinutes));
            assignLanes(dayList);
            for (PlanningEtude a : dayList)
                dayRows[i].getChildren().add(buildActivityCard(a));
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

    /** Build a horizontal activity card positioned by time on X-axis, lanes on Y-axis. */
    private VBox buildActivityCard(PlanningEtude a) {
        VBox card = new VBox(1);
        card.setUserData("activity-card");
        card.getStyleClass().add("activity-card");

        if ("done".equals(a.getEtat()))    card.getStyleClass().add("activity-card-done");
        if ("skipped".equals(a.getEtat())) card.getStyleClass().add("activity-card-skipped");

        // Background color
        String hex = a.getCouleurActivite() != null ? a.getCouleurActivite() : "#dfe6e9";
        try {
            Color base = Color.web(hex);
            card.setBackground(new Background(new BackgroundFill(
                    base.deriveColor(0, 1, 1, 0.55), new CornerRadii(6), Insets.EMPTY)));
            card.setBorder(new Border(new BorderStroke(
                    base.deriveColor(0, 1, 0.8, 1), BorderStrokeStyle.SOLID,
                    new CornerRadii(6), new BorderWidths(2))));
        } catch (Exception ignored) {}

        // Horizontal position: X = startMinutes / 60 * COL_WIDTH
        int startMin = a.startMinutes();
        int duration = a.getDureePrevue() != null ? a.getDureePrevue() : 30;
        double leftPx  = (startMin  / 60.0) * COL_WIDTH;
        double widthPx = (duration  / 60.0) * COL_WIDTH;
        card.setLayoutX(leftPx);
        card.setPrefWidth(Math.max(widthPx - 3, 14));

        // Vertical lanes within the day row
        int lane  = laneOf(a);
        int total = laneTotalOf(a);
        double laneH = (DAY_ROW_H - 4.0) / Math.max(1, total);
        card.setLayoutY(lane * laneH + 2);
        card.setPrefHeight(Math.max(laneH - 4, 16));

        card.setPadding(new Insets(2, 4, 2, 4));

        Label titleLbl = new Label(a.getTitreP());
        titleLbl.getStyleClass().add("activity-title");
        titleLbl.setWrapText(false);          // horizontal card: clip rather than wrap
        titleLbl.setEllipsisString("…");
        if ("done".equals(a.getEtat())) titleLbl.setStyle("-fx-strikethrough: true;");

        Label metaLbl = new Label(
                (a.getHeureDebut() != null ? a.getHeureDebut().toString().substring(0, 5) : "")
                + "  " + a.durationLabel());
        metaLbl.getStyleClass().add("activity-meta");

        card.getChildren().addAll(titleLbl, metaLbl);
        card.setCursor(Cursor.HAND);
        card.setId("activity-" + a.getId());

        int activityId = a.getId();
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
    // DAY ROW EVENTS  (drag-drop target / xToTime helper)
    // ════════════════════════════════════════════════════════════════════

    /** Wire drag-over and drag-dropped on a day row Pane. */
    private void setupDragDrop(Pane row) {
        row.setOnDragOver(ev -> {
            if (ev.getDragboard().hasString()) ev.acceptTransferModes(TransferMode.MOVE);
            ev.consume();
        });
        row.setOnDragDropped(ev -> {
            Dragboard db = ev.getDragboard();
            if (db.hasString()) {
                int id = Integer.parseInt(db.getString());
                LocalDate targetDate = (LocalDate) row.getUserData();
                LocalTime targetTime = xToTime(ev.getX());
                moveActivity(id, targetDate, targetTime);
            }
            ev.setDropCompleted(true);
            ev.consume();
        });
    }

    /** Convert an X pixel offset within a day-row Pane to a LocalTime. */
    private LocalTime xToTime(double x) {
        int totalMinutes = (int) Math.max(0, Math.min(1439, (x / COL_WIDTH) * 60));
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

    /** Opens the "create activity" dialog — clickX is the X offset within the day-row Pane. */
    private void openCreateDialog(Pane row, double clickX) {
        LocalDate date = (LocalDate) row.getUserData();
        LocalTime time = xToTime(clickX);

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

    @FXML
    public void onRefaireTest() { openPersonalityTest(); }

    @FXML
    private void onPersonalityTest() { openPersonalityTest(); }

    private void refreshPersonalityStatus() {
        Utilisateur user = SessionManager.getCurrentUser();
        if (user == null) return;
        String typePers = null;
        try (Connection conn = MyConnection.getInstance();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT type_pers FROM profil_apprentissage WHERE utilisateur_id = ?")) {
            ps.setInt(1, user.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) typePers = rs.getString("type_pers");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        if (typePers != null && !typePers.isEmpty()) {
            personalityTestBtn.setText(typePers);
            personalityTestBtn.setStyle("-fx-background-color: #102c59; -fx-text-fill: white; -fx-font-weight: bold;");
        } else {
            personalityTestBtn.setText("Test de personnalité");
            personalityTestBtn.setStyle("");
        }
    }

    private void openPersonalityTest() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/personality.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("MentorAI - Test de Personnalité");
            stage.setScene(new Scene(root));
            stage.setOnHidden(e -> refreshPersonalityStatus());
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    /** Opens a unified info dialog on double-click: shows activity info + linked notes + actions. */
    private void openEditDialog(int activityId) {
        try {
            PlanningEtude existing = service.findById(activityId).orElse(null);
            if (existing == null) { showErrors("Activité introuvable."); return; }

            // ── Build the unified dialog ───────────────────────────────
            Dialog<Void> dlg = new Dialog<>();
            dlg.setTitle(existing.getTitreP());
            dlg.setHeaderText(null);

            // Add stylesheet
            try {
                dlg.getDialogPane().getStylesheets().add(
                    getClass().getResource("/css/planning.css").toExternalForm());
            } catch (Exception ignored) {}

            // Custom buttons
            ButtonType modifierBtn  = new ButtonType("✏️ Modifier",  ButtonBar.ButtonData.LEFT);
            ButtonType supprimerBtn = new ButtonType("🗑 Supprimer", ButtonBar.ButtonData.LEFT);
            ButtonType fermerBtn    = new ButtonType("Fermer",       ButtonBar.ButtonData.CANCEL_CLOSE);
            dlg.getDialogPane().getButtonTypes().setAll(modifierBtn, supprimerBtn, fermerBtn);

            // ── Content ───────────────────────────────────────────────
            VBox content = new VBox(12);
            content.setPadding(new Insets(16));
            content.setPrefWidth(420);

            // Activity info header
            String dateStr  = existing.getDateSeance() != null ? existing.getDateSeance().toString() : "—";
            String heureStr = existing.getHeureDebut() != null
                    ? existing.getHeureDebut().toString().substring(0, 5) : "—";
            String dureeStr = existing.durationLabel();
            Label infoLbl = new Label("📅 " + dateStr + "   🕐 " + heureStr
                    + (dureeStr.isEmpty() ? "" : "   ⏱ " + dureeStr));
            infoLbl.setStyle("-fx-font-size:13px; -fx-text-fill:#334155;");

            // Linked notes section
            Label notesHeader = new Label("📎 Notes liées");
            notesHeader.setStyle("-fx-font-weight:bold; -fx-font-size:13px;");

            ListView<Carnet> notesView = new ListView<>();
            notesView.setPrefHeight(130);
            notesView.getItems().setAll(carnetSvc.getNotesByPlanning(existing.getId()));
            notesView.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(Carnet c, boolean empty) {
                    super.updateItem(c, empty);
                    if (empty || c == null) { setText(null); setStyle(""); }
                    else {
                        setText(c.getTitre());
                        setStyle("-fx-cursor: hand;");
                        // Single click on a note → navigate to Carnet
                        setOnMouseClicked(ev -> {
                            if (ev.getClickCount() == 1 && c != null) {
                                dlg.close();
                                MainController main = MainController.getInstance();
                                if (main != null) {
                                    main.openCarnetAndSelect(c);
                                }
                            }
                        });
                    }
                }
            });

            Label noNotes = new Label("(aucune note liée)");
            noNotes.setStyle("-fx-text-fill:#94a3b8; -fx-font-style:italic;");
            noNotes.setVisible(notesView.getItems().isEmpty());
            noNotes.setManaged(notesView.getItems().isEmpty());
            notesView.setVisible(!notesView.getItems().isEmpty());
            notesView.setManaged(!notesView.getItems().isEmpty());

            content.getChildren().addAll(infoLbl, notesHeader, notesView, noNotes);
            dlg.getDialogPane().setContent(content);

            // ── Handle button clicks ───────────────────────────────────
            // We use the result converter to capture which button was pressed
            dlg.setResultConverter(btn -> null); // we handle via button lookup

            // Disable the default "consuming" of Modifier/Supprimer so the dialog closes first
            javafx.scene.control.ButtonBase modNode =
                (javafx.scene.control.ButtonBase) dlg.getDialogPane().lookupButton(modifierBtn);
            javafx.scene.control.ButtonBase supNode =
                (javafx.scene.control.ButtonBase) dlg.getDialogPane().lookupButton(supprimerBtn);

            final boolean[] doModifier  = {false};
            final boolean[] doSupprimer = {false};

            if (modNode != null) modNode.setOnAction(e -> { doModifier[0]  = true; dlg.close(); });
            if (supNode != null) supNode.setOnAction(e -> { doSupprimer[0] = true; dlg.close(); });

            dlg.showAndWait();

            if (doModifier[0])  openUpdateDialog(existing);
            if (doSupprimer[0]) confirmAndDelete(activityId);

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
        // Three-option dialog: delete notes / detach notes / cancel
        ButtonType deleteNotes  = new ButtonType("Supprimer les notes",  ButtonBar.ButtonData.LEFT);
        ButtonType detachNotes  = new ButtonType("Détacher les notes",   ButtonBar.ButtonData.LEFT);
        ButtonType cancelDelete = new ButtonType("Annuler",              ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer l'activité");
        confirm.setHeaderText("Que faire des notes liées ?");
        confirm.setContentText("Cette activité peut avoir des notes associées.");
        confirm.getButtonTypes().setAll(deleteNotes, detachNotes, cancelDelete);

        confirm.showAndWait().ifPresent(choice -> {
            if (choice == cancelDelete) return;
            try {
                if (choice == deleteNotes) {
                    carnetSvc.deleteNotesByPlanning(activityId);
                } else {
                    carnetSvc.detachNotesByPlanning(activityId);
                }
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
        form.add(errLbl,                                0, row, 2, 1); row++;

        // ── Notes liées (Planning ↔ Carnet) ─────────────────────────
        if (existing != null) {
            form.add(new Label(""), 0, row, 2, 1); row++; // spacer
            VBox notesLieesBox = buildLinkedNotesSection(existing.getId());
            form.add(notesLieesBox, 0, row, 2, 1);
        }

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

    // ════════════════════════════════════════════════════════════════════
    // PLANNING ↔ CARNET  —  linked-notes helpers
    // ════════════════════════════════════════════════════════════════════

    private VBox buildLinkedNotesSection(int planningId) {
        VBox box = new VBox(8);
        box.setStyle("-fx-background-color:#f1f5f9; -fx-background-radius:8; -fx-border-color:#cbd5e1; -fx-border-width:1; -fx-border-radius:8; -fx-padding:10;");

        Label header = new Label("📎 Notes liées");
        header.setStyle("-fx-font-weight:bold; -fx-font-size:13px;");

        ListView<com.mentorai.entities.Carnet> listView = new ListView<>();
        listView.setPrefHeight(100);
        listView.getItems().setAll(carnetSvc.getNotesByPlanning(planningId));
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(com.mentorai.entities.Carnet c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? null : c.getTitre());
            }
        });

        Button detachBtn = new Button("↩ Détacher la note sélectionnée");
        detachBtn.setStyle("-fx-font-size:11px;");
        detachBtn.setOnAction(e -> {
            com.mentorai.entities.Carnet sel = listView.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            carnetSvc.unlinkNoteFromPlanning(sel.getId(), planningId);
            listView.getItems().remove(sel);
        });

        Button attachBtn = new Button("➕ Ajouter une note existante");
        attachBtn.setStyle("-fx-font-size:11px;");
        attachBtn.setOnAction(e -> showAttachNoteDialog(planningId, listView));

        Button createBtn = new Button("📝 Créer une nouvelle note");
        createBtn.setStyle("-fx-font-size:11px;");
        createBtn.setOnAction(e -> showCreateLinkedNoteDialog(planningId, listView));

        HBox btnRow = new HBox(8, attachBtn, createBtn, detachBtn);
        box.getChildren().addAll(header, listView, btnRow);
        return box;
    }

    private void showAttachNoteDialog(int planningId,
                                      ListView<com.mentorai.entities.Carnet> listView) {
        // Notes already linked to this specific planning
        List<Integer> alreadyLinked = carnetSvc.getNotesByPlanning(planningId)
                .stream().map(com.mentorai.entities.Carnet::getId)
                .collect(java.util.stream.Collectors.toList());
        List<com.mentorai.entities.Carnet> free = carnetSvc.findAll().stream()
                .filter(n -> !alreadyLinked.contains(n.getId()))
                .collect(java.util.stream.Collectors.toList());

        if (free.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Aucune note disponible (toutes déjà liées).").showAndWait();
            return;
        }

        ChoiceDialog<com.mentorai.entities.Carnet> pick =
                new ChoiceDialog<>(free.get(0), free);
        pick.setTitle("Lier une note");
        pick.setHeaderText("Choisissez une note à lier à cette activité :");
        pick.showAndWait().ifPresent(chosen -> {
            carnetSvc.linkNoteToPlanning(chosen.getId(), planningId);
            if (!listView.getItems().contains(chosen)) listView.getItems().add(chosen);
        });
    }

    private void showCreateLinkedNoteDialog(int planningId,
                                            ListView<com.mentorai.entities.Carnet> listView) {
        Dialog<com.mentorai.entities.Carnet> dlg2 = new Dialog<>();
        dlg2.setTitle("Créer une note liée");
        dlg2.setHeaderText("Nouvelle note pour cette activité");

        ButtonType saveBtn = new ButtonType("Créer", ButtonBar.ButtonData.OK_DONE);
        dlg2.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        TextField titreF  = new TextField();
        titreF.setPromptText("Titre de la note *");
        TextArea contenuA = new TextArea();
        contenuA.setPromptText("Contenu…");
        contenuA.setPrefRowCount(4);

        VBox form2 = new VBox(8, new Label("Titre :"), titreF,
                              new Label("Contenu :"), contenuA);
        form2.setPadding(new Insets(12));
        dlg2.getDialogPane().setContent(form2);

        dlg2.setResultConverter(bt -> {
            if (bt != saveBtn) return null;
            String t = titreF.getText().trim();
            String c = contenuA.getText().trim();
            if (t.isEmpty() || c.length() < 5) return null;
            com.mentorai.entities.Carnet note = new com.mentorai.entities.Carnet(t, c);
            return note;
        });

        dlg2.showAndWait().ifPresent(note -> {
            if (carnetSvc.create(note)) {
                carnetSvc.linkNoteToPlanning(note.getId(), planningId);
                listView.getItems().add(note);
            }
        });
    }
}
