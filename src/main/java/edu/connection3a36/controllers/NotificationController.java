package edu.connection3a36.controllers;

import edu.connection3a36.entities.Notification;
import edu.connection3a36.services.NotificationService;
import edu.connection3a36.services.UserPreferencesService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Contrôleur du panneau de notifications pour le superadmin (ADMINM).
 * Accessible via le bouton 🔔 dans la sidebar back-office.
 */
public class NotificationController {

    private final NotificationService notifService = new NotificationService();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    // ─────────────────────────────────────────────────────────────────────────
    // OUVERTURE DU PANNEAU
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Ouvre le panneau de notifications dans une nouvelle fenêtre.
     * Appelé depuis MainController.
     */
    public void openNotificationsPanel(Stage owner) {
        Stage stage = new Stage();
        stage.setTitle("🔔 Notifications");
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(owner);

        boolean darkMode = owner != null && owner.getScene() != null
                && owner.getScene().getRoot().getStyleClass().contains("dark-mode");

        VBox root = buildPanel(stage, darkMode);

        Scene scene = new Scene(root, 560, 650);
        // Charger le CSS si disponible
        try {
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        } catch (Exception ignored) {}
        try {
            new UserPreferencesService().applyToRoot(root, new UserPreferencesService().load());
        } catch (Exception ignored) {}

        stage.setScene(scene);
        stage.setResizable(true);
        stage.show();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONSTRUCTION DU PANNEAU
    // ─────────────────────────────────────────────────────────────────────────

    private VBox buildPanel(Stage stage, boolean darkMode) {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: " + (darkMode ? "#161b22" : "#f8fafc") + ";");

        // ── Header ────────────────────────────────────────────────────────────
        HBox header = new HBox(10);
        header.setStyle("-fx-background-color: #102c59; -fx-padding: 18 20;");
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("🔔 Notifications");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        int nonLues = notifService.countNonLues();
        Label badge = new Label(nonLues > 0 ? nonLues + " non lues" : "Tout lu ✅");
        badge.setStyle("-fx-background-color: " + (nonLues > 0 ? "#e74c3c" : "#27ae60")
                + "; -fx-text-fill: white; -fx-padding: 4 10; -fx-background-radius: 12; "
                + "-fx-font-size: 12px; -fx-font-weight: bold;");

        header.getChildren().addAll(title, spacer, badge);

        // ── Barre d'actions ───────────────────────────────────────────────────
        HBox actionsBar = new HBox(8);
        actionsBar.setStyle("-fx-padding: 10 16; -fx-background-color: " + (darkMode ? "#161b22" : "white") + "; "
                + "-fx-border-color: " + (darkMode ? "#30363d" : "#e2e8f0") + "; -fx-border-width: 0 0 1 0;");
        actionsBar.setAlignment(Pos.CENTER_LEFT);

        Button btnToutLire = new Button("✅ Tout marquer comme lu");
        btnToutLire.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; "
                + "-fx-padding: 6 14; -fx-background-radius: 6; -fx-font-size: 12px; "
                + "-fx-cursor: hand;");

        Button btnNettoyerDones = new Button("🗑️ Nettoyer terminées");
        btnNettoyerDones.setStyle("-fx-background-color: #64748b; -fx-text-fill: white; "
                + "-fx-padding: 6 14; -fx-background-radius: 6; -fx-font-size: 12px; "
                + "-fx-cursor: hand;");

        // Conteneur scrollable pour la liste
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: #f8fafc;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        VBox listBox = new VBox(8);
        listBox.setPadding(new Insets(12));
        listBox.setFillWidth(true);

        scrollPane.setContent(listBox);

        // Chargement initial
        reloadList(listBox, stage, badge, scrollPane, darkMode);

        // Actions sur la barre
        btnToutLire.setOnAction(e -> {
            notifService.marquerToutesLues();
            reloadList(listBox, stage, badge, scrollPane, darkMode);
        });
        btnNettoyerDones.setOnAction(e -> {
            notifService.supprimerDones();
            reloadList(listBox, stage, badge, scrollPane, darkMode);
        });

        actionsBar.getChildren().addAll(btnToutLire, btnNettoyerDones);

        root.getChildren().addAll(header, actionsBar, scrollPane);
        return root;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CHARGEMENT DE LA LISTE
    // ─────────────────────────────────────────────────────────────────────────

    private void reloadList(VBox listBox, Stage stage, Label badge, ScrollPane scroll, boolean darkMode) {
        listBox.getChildren().clear();

        List<Notification> notifications = notifService.getAll();

        if (notifications.isEmpty()) {
            VBox empty = new VBox(10);
            empty.setAlignment(Pos.CENTER);
            empty.setStyle("-fx-padding: 50;");
            Label emptyIcon = new Label("🔔");
            emptyIcon.setStyle("-fx-font-size: 40px;");
            Label emptyText = new Label("Aucune notification pour le moment");
            emptyText.setStyle("-fx-text-fill: " + (darkMode ? "#8b949e" : "#94a3b8") + "; -fx-font-size: 14px;");
            empty.getChildren().addAll(emptyIcon, emptyText);
            listBox.getChildren().add(empty);
        } else {
            for (Notification n : notifications) {
                listBox.getChildren().add(buildNotifCard(n, listBox, stage, badge, scroll, darkMode));
            }
        }

        // Mettre à jour le badge
        int nonLues = notifService.countNonLues();
        badge.setText(nonLues > 0 ? nonLues + " non lues" : "Tout lu ✅");
        badge.setStyle("-fx-background-color: " + (nonLues > 0 ? "#e74c3c" : "#27ae60")
                + "; -fx-text-fill: white; -fx-padding: 4 10; -fx-background-radius: 12; "
                + "-fx-font-size: 12px; -fx-font-weight: bold;");

        // Mettre à jour le badge dans la sidebar si MainController existe
        updateMainControllerBadge(nonLues);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CARTE DE NOTIFICATION
    // ─────────────────────────────────────────────────────────────────────────

    private VBox buildNotifCard(Notification n, VBox listBox, Stage stage, Label badge, ScrollPane scroll, boolean darkMode) {
        VBox card = new VBox(6);
        String bgColor = n.isDone() ? (darkMode ? "#1f2937" : "#f1f5f9") : (n.isLu() ? (darkMode ? "#161b22" : "white") : n.getTypeBgColor());
        String borderColor = n.isDone() ? (darkMode ? "#30363d" : "#e2e8f0") : (n.isLu() ? (darkMode ? "#30363d" : "#e2e8f0") : getBorderColor(n.getType()));
        String opacity = n.isDone() ? "0.65" : "1.0";

        card.setStyle("-fx-background-color: " + bgColor + "; "
                + "-fx-border-color: " + borderColor + "; "
                + "-fx-border-width: 0 0 0 4; "
                + "-fx-border-radius: 8; -fx-background-radius: 8; "
                + "-fx-padding: 12 14; "
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 4, 0, 0, 1); "
                + "-fx-opacity: " + opacity + ";");

        // ── Ligne haute : emoji + titre + date ────────────────────────────────
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label typeEmoji = new Label(n.getTypeEmoji());
        typeEmoji.setStyle("-fx-font-size: 16px;");

        Label titleLbl = new Label(n.getTitre());
        titleLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13.5px; "
                + "-fx-text-fill: " + n.getTypeTextColor() + ";");
        titleLbl.setWrapText(true);
        HBox.setHgrow(titleLbl, Priority.ALWAYS);

        String dateStr = n.getDateCreation() != null ? n.getDateCreation().format(FMT) : "";
        Label dateLbl = new Label(dateStr);
        dateLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        if (!n.isLu()) {
            Label unreadDot = new Label("●");
            unreadDot.setStyle("-fx-text-fill: #3b82f6; -fx-font-size: 10px;");
            topRow.getChildren().add(unreadDot);
        }

        topRow.getChildren().addAll(typeEmoji, titleLbl, dateLbl);

        // ── Message ───────────────────────────────────────────────────────────
        Label messageLbl = new Label(n.getMessage());
        messageLbl.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #475569; -fx-padding: 0 0 0 24;");
        messageLbl.setWrapText(true);

        // ── Statuts badges ────────────────────────────────────────────────────
        HBox statusRow = new HBox(6);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        statusRow.setStyle("-fx-padding: 2 0 0 24;");

        if (n.isLu()) {
            Label luBadge = new Label("Lu");
            luBadge.setStyle("-fx-background-color: #dbeafe; -fx-text-fill: #1d4ed8; "
                    + "-fx-padding: 2 8; -fx-background-radius: 10; -fx-font-size: 10px;");
            statusRow.getChildren().add(luBadge);
        }
        if (n.isDone()) {
            Label doneBadge = new Label("✅ Terminé");
            doneBadge.setStyle("-fx-background-color: #d1fae5; -fx-text-fill: #065f46; "
                    + "-fx-padding: 2 8; -fx-background-radius: 10; -fx-font-size: 10px;");
            statusRow.getChildren().add(doneBadge);
        }

        // ── Boutons d'action ──────────────────────────────────────────────────
        HBox actionsRow = new HBox(6);
        actionsRow.setAlignment(Pos.CENTER_RIGHT);

        if (!n.isLu()) {
            Button btnLire = new Button("👁️ Marquer lu");
            styleSmallBtn(btnLire, "#3b82f6");
            btnLire.setOnAction(e -> {
                notifService.marquerCommeLue(n.getId());
                reloadList(listBox, stage, badge, scroll, darkMode);
            });
            actionsRow.getChildren().add(btnLire);
        }

        if (!n.isDone()) {
            Button btnDone = new Button("✅ Marquer terminé");
            styleSmallBtn(btnDone, "#10b981");
            btnDone.setOnAction(e -> {
                notifService.marquerCommeDone(n.getId());
                reloadList(listBox, stage, badge, scroll, darkMode);
            });
            actionsRow.getChildren().add(btnDone);
        }

        Button btnSuppr = new Button("🗑️");
        styleSmallBtn(btnSuppr, "#ef4444");
        btnSuppr.setOnAction(e -> {
            notifService.supprimer(n.getId());
            reloadList(listBox, stage, badge, scroll, darkMode);
        });
        actionsRow.getChildren().add(btnSuppr);

        card.getChildren().addAll(topRow, messageLbl, statusRow, actionsRow);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private void styleSmallBtn(Button btn, String color) {
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; "
                + "-fx-padding: 4 10; -fx-background-radius: 6; -fx-font-size: 11px; "
                + "-fx-cursor: hand;");
    }

    private String getBorderColor(String type) {
        return switch (type) {
            case "WARNING" -> "#f59e0b";
            case "SUCCESS" -> "#10b981";
            case "ERROR"   -> "#ef4444";
            default        -> "#3b82f6";
        };
    }

    private void updateMainControllerBadge(int nonLues) {
        Platform.runLater(() -> {
            MainController mc = MainController.getInstance();
            if (mc != null) {
                mc.updateNotificationBadge(nonLues);
            }
        });
    }
}
