package edu.connection3a36.tools;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.*;
import javafx.util.Duration;

/**
 * ============================================================
 * ToastNotification — Notifications style Windows 11
 * ============================================================
 * Affiche des toasts non-bloquants dans le coin inférieur droit
 * de l'écran, empilés verticalement comme les notifications Windows.
 *
 * TYPES DISPONIBLES :
 *  SUCCESS  → vert   — opération réussie
 *  ERROR    → rouge  — erreur critique
 *  WARNING  → orange — avertissement
 *  INFO     → bleu   — information neutre
 *  MEDAL    → doré   — récompense gamification
 *  DEADLINE → orange foncé — alerte deadline / risque abandon
 *
 * UTILISATION :
 *  ToastNotification.showSuccess("Objectif créé !");
 *  ToastNotification.showSuccess("Titre", "Message détaillé");
 *  ToastNotification.showError("Erreur de connexion");
 *  ToastNotification.showWarning("Deadline dans 2 jours !");
 *  ToastNotification.showMedal("🥇 Médaille Or obtenue !");
 *  ToastNotification.show(Type.INFO, "Titre", "Message", 4000);
 *
 * COMPORTEMENT :
 *  - Apparaît en glissant depuis la droite (slide-in via AnimationTimer)
 *  - Barre de progression indique le temps restant
 *  - Disparaît automatiquement après N millisecondes
 *  - Cliquable pour fermer immédiatement
 *  - Max 4 toasts simultanés (les anciens sont supprimés)
 *
 * NOTE TECHNIQUE :
 *  Les propriétés x/y/opacity d'un Stage sont ReadOnlyDoubleProperty.
 *  On utilise des DoubleProperty intermédiaires liées par listener
 *  pour contourner cette limitation de JavaFX.
 * ============================================================
 */
public class ToastNotification {

    // ── Types de notification ─────────────────────────────────────────────────
    public enum Type {
        SUCCESS, ERROR, WARNING, INFO, MEDAL, DEADLINE
    }

    // ── Constantes de design ──────────────────────────────────────────────────
    private static final int TOAST_WIDTH    = 360;
    private static final int TOAST_HEIGHT   = 80;
    private static final int MARGIN_RIGHT   = 20;
    private static final int MARGIN_BOTTOM  = 20;
    private static final int SPACING        = 12;
    private static final int MAX_TOASTS     = 4;
    private static final int DEFAULT_DURATION = 4000; // ms

    /** File des toasts actifs pour l'empilement */
    private static final java.util.Deque<Stage> activeToasts = new java.util.ArrayDeque<>();

    // ── API publique ──────────────────────────────────────────────────────────

    public static void showSuccess(String message) {
        show(Type.SUCCESS, "Succès", message, DEFAULT_DURATION);
    }

    public static void showSuccess(String title, String message) {
        show(Type.SUCCESS, title, message, DEFAULT_DURATION);
    }

    public static void showError(String message) {
        show(Type.ERROR, "Erreur", message, 6000);
    }

    public static void showError(String title, String message) {
        show(Type.ERROR, title, message, 6000);
    }

    public static void showWarning(String message) {
        show(Type.WARNING, "Attention", message, 5000);
    }

    public static void showWarning(String title, String message) {
        show(Type.WARNING, title, message, 5000);
    }

    public static void showInfo(String message) {
        show(Type.INFO, "Information", message, DEFAULT_DURATION);
    }

    public static void showInfo(String title, String message) {
        show(Type.INFO, title, message, DEFAULT_DURATION);
    }

    public static void showMedal(String message) {
        show(Type.MEDAL, "🏆 Récompense", message, 6000);
    }

    public static void showDeadline(String message) {
        show(Type.DEADLINE, "⏰ Deadline", message, 7000);
    }

    // ── Méthode principale ────────────────────────────────────────────────────

    /**
     * Affiche un toast de notification.
     * Thread-safe : peut être appelé depuis n'importe quel thread.
     *
     * @param type     Type de notification
     * @param title    Titre affiché en gras
     * @param message  Message détaillé
     * @param duration Durée d'affichage en millisecondes
     */
    public static void show(Type type, String title, String message, int duration) {

        // ════════════════════════════════════════════════════════════════════
        // THREAD SAFETY : JavaFX interdit de modifier l'interface depuis
        // un thread autre que le thread UI (JavaFX Application Thread).
        // Si cette méthode est appelée depuis un Thread background (ex: après
        // un appel Ollama), Platform.runLater() remet l'exécution sur le
        // thread UI de façon asynchrone et sécurisée.
        // ════════════════════════════════════════════════════════════════════
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> show(type, title, message, duration));
            return;
        }

        // Si on a déjà MAX_TOASTS toasts affichés, supprimer le plus ancien
        while (activeToasts.size() >= MAX_TOASTS) {
            Stage oldest = activeToasts.pollFirst(); // retirer le premier (le plus ancien)
            if (oldest != null) oldest.close();
        }

        // Créer et enregistrer le toast dans la file
        Stage toastStage = buildToastStage(type, title, message, duration);
        activeToasts.addLast(toastStage); // ajouter à la fin de la file

        // Repositionner les toasts existants (pour faire de la place)
        repositionnerToasts();

        // Afficher puis animer l'entrée (slide depuis la droite)
        toastStage.show();
        animerEntree(toastStage);

        // PauseTransition : timer JavaFX qui attend N ms puis exécute une action
        // C'est l'équivalent de Thread.sleep() mais non-bloquant pour l'UI
        PauseTransition pause = new PauseTransition(Duration.millis(duration));
        pause.setOnFinished(e -> fermerToast(toastStage)); // fermer après la durée
        pause.play();
    }

    // ── Construction du toast ─────────────────────────────────────────────────

    private static Stage buildToastStage(Type type, String title, String message, int duration) {
        Stage stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);
        stage.setResizable(false);
        stage.initModality(Modality.NONE);

        // ── Conteneur principal ───────────────────────────────────────────────
        VBox root = new VBox(0);
        root.setPrefWidth(TOAST_WIDTH);
        root.setMinWidth(TOAST_WIDTH);
        root.setMaxWidth(TOAST_WIDTH);
        root.setStyle(
                "-fx-background-color: " + bgColor(type) + ";" +
                "-fx-background-radius: 12;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 20, 0, 0, 6);"
        );

        // ── Ligne principale : accent | icône | texte ─────────────────────────
        HBox content = new HBox(0);
        content.setAlignment(Pos.CENTER_LEFT);

        // Barre verticale colorée à gauche (style Windows)
        Rectangle accentBar = new Rectangle(4, TOAST_HEIGHT);
        accentBar.setFill(Color.web(accentColor(type)));
        accentBar.setArcWidth(4);
        accentBar.setArcHeight(4);

        // Zone icône
        StackPane iconPane = new StackPane();
        iconPane.setMinWidth(52);
        iconPane.setMinHeight(TOAST_HEIGHT);
        iconPane.setAlignment(Pos.CENTER);
        iconPane.setStyle("-fx-background-color: " + iconBgColor(type) + ";");
        Label iconLabel = new Label(icon(type));
        iconLabel.setStyle("-fx-font-size: 22px;");
        iconPane.getChildren().add(iconLabel);

        // Zone texte
        VBox textBox = new VBox(4);
        textBox.setPadding(new Insets(14, 14, 10, 12));
        textBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        // Titre + bouton fermer
        HBox titleRow = new HBox(6);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label lblTitle = new Label(title);
        lblTitle.setStyle(
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + titleColor(type) + ";"
        );
        lblTitle.setWrapText(false);
        HBox.setHgrow(lblTitle, Priority.ALWAYS);

        // Bouton ×
        Label btnClose = new Label("×");
        final String closeBaseStyle =
                "-fx-font-size: 16px;" +
                "-fx-text-fill: " + closeColor(type) + ";" +
                "-fx-cursor: hand;" +
                "-fx-padding: 0 4 2 4;" +
                "-fx-background-radius: 4;";
        btnClose.setStyle(closeBaseStyle);
        btnClose.setOnMouseEntered(e ->
                btnClose.setStyle(closeBaseStyle + "-fx-background-color: rgba(0,0,0,0.1);"));
        btnClose.setOnMouseExited(e ->
                btnClose.setStyle(closeBaseStyle));

        titleRow.getChildren().addAll(lblTitle, btnClose);

        // Message
        Label lblMessage = new Label(message);
        lblMessage.setStyle(
                "-fx-font-size: 11.5px;" +
                "-fx-text-fill: " + messageColor(type) + ";"
        );
        lblMessage.setWrapText(true);
        lblMessage.setMaxWidth(TOAST_WIDTH - 90);

        textBox.getChildren().addAll(titleRow, lblMessage);
        content.getChildren().addAll(accentBar, iconPane, textBox);

        // ── Barre de progression (timer visuel) ───────────────────────────────
        ProgressBar progressBar = new ProgressBar(1.0);
        progressBar.setPrefWidth(TOAST_WIDTH);
        progressBar.setPrefHeight(3);
        progressBar.setStyle(
                "-fx-accent: " + accentColor(type) + ";" +
                "-fx-background-color: rgba(0,0,0,0.08);" +
                "-fx-background-radius: 0 0 12 12;" +
                "-fx-padding: 0;"
        );

        // Animer la barre de progression (progressProperty est WritableProperty → OK)
        Timeline progressAnim = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(progressBar.progressProperty(), 1.0, Interpolator.LINEAR)),
                new KeyFrame(Duration.millis(duration),
                        new KeyValue(progressBar.progressProperty(), 0.0, Interpolator.LINEAR))
        );
        progressAnim.play();

        root.getChildren().addAll(content, progressBar);

        // ── Scène transparente ────────────────────────────────────────────────
        Scene scene = new Scene(root, TOAST_WIDTH, TOAST_HEIGHT + 3);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);

        // Position initiale hors écran (sera animée dans animerEntree)
        javafx.geometry.Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        stage.setX(screen.getMaxX() + TOAST_WIDTH);
        stage.setY(screen.getMaxY() - MARGIN_BOTTOM - TOAST_HEIGHT - 3);

        // ── Interactions ──────────────────────────────────────────────────────
        btnClose.setOnMouseClicked(e -> {
            progressAnim.stop();
            fermerToast(stage);
        });
        root.setOnMouseClicked(e -> {
            progressAnim.stop();
            fermerToast(stage);
        });
        root.setOnMouseEntered(e -> root.setCursor(javafx.scene.Cursor.HAND));
        root.setOnMouseExited(e  -> root.setCursor(javafx.scene.Cursor.DEFAULT));

        return stage;
    }

    // ── Animations (via DoubleProperty intermédiaires) ────────────────────────

    /**
     * Animation d'entrée : slide depuis la droite + fade-in.
     *
     * TECHNIQUE : Stage.xProperty() / opacityProperty() sont ReadOnlyDoubleProperty.
     * On crée des SimpleDoubleProperty animables et on les lie au Stage via listener.
     */
    private static void animerEntree(Stage stage) {
        javafx.geometry.Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double targetX = screen.getMaxX() - TOAST_WIDTH - MARGIN_RIGHT;
        double startX  = screen.getMaxX() + 10; // départ hors écran à droite

        stage.setX(startX);
        stage.setOpacity(0.0); // invisible au départ

        // ════════════════════════════════════════════════════════════════════
        // PROBLÈME : Stage.xProperty() et Stage.opacityProperty() sont des
        // ReadOnlyDoubleProperty → on NE PEUT PAS les passer à KeyValue
        // car KeyValue exige une WritableValue (propriété modifiable).
        //
        // SOLUTION : on crée des SimpleDoubleProperty (qui sont modifiables),
        // on les anime avec Timeline/KeyValue, et on les connecte au Stage
        // via des listeners (addListener).
        //
        // Fonctionnement du listener :
        //   Chaque fois que xProp change de valeur (à chaque frame d'animation),
        //   le lambda (obs, oldV, newV) -> stage.setX(newV) est appelé
        //   automatiquement, ce qui déplace le Stage.
        // ════════════════════════════════════════════════════════════════════

        // Propriétés intermédiaires animables (WritableDoubleProperty)
        DoubleProperty xProp       = new SimpleDoubleProperty(startX);
        DoubleProperty opacityProp = new SimpleDoubleProperty(0.0);

        // Connecter les propriétés au Stage via des listeners
        // Chaque changement de valeur → appel automatique de stage.setX() / stage.setOpacity()
        xProp.addListener((obs, oldV, newV)       -> stage.setX(newV.doubleValue()));
        opacityProp.addListener((obs, oldV, newV) -> stage.setOpacity(newV.doubleValue()));

        // Timeline : définit l'animation image par image
        // KeyFrame(Duration.ZERO)       → état au début (t=0ms)
        // KeyFrame(Duration.millis(280)) → état à la fin (t=280ms)
        // KeyValue : quelle propriété animer, vers quelle valeur, avec quelle courbe
        // Interpolator.EASE_OUT : démarre vite, ralentit à la fin (effet naturel)
        Timeline slideIn = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(xProp,       startX,  Interpolator.EASE_IN),
                        new KeyValue(opacityProp, 0.0,     Interpolator.EASE_IN)),
                new KeyFrame(Duration.millis(280),
                        new KeyValue(xProp,       targetX, Interpolator.EASE_OUT),
                        new KeyValue(opacityProp, 1.0,     Interpolator.EASE_OUT))
        );
        slideIn.play(); // démarrer l'animation
    }

    /**
     * Animation de sortie : slide vers la droite + fade-out.
     */
    private static void animerSortie(Stage stage, Runnable onFinished) {
        javafx.geometry.Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double startX  = stage.getX();
        double targetX = screen.getMaxX() + 10;

        DoubleProperty xProp       = new SimpleDoubleProperty(startX);
        DoubleProperty opacityProp = new SimpleDoubleProperty(stage.getOpacity());

        xProp.addListener((obs, oldV, newV)       -> stage.setX(newV.doubleValue()));
        opacityProp.addListener((obs, oldV, newV) -> stage.setOpacity(newV.doubleValue()));

        Timeline slideOut = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(xProp,       startX,  Interpolator.EASE_IN),
                        new KeyValue(opacityProp, stage.getOpacity(), Interpolator.EASE_IN)),
                new KeyFrame(Duration.millis(220),
                        new KeyValue(xProp,       targetX, Interpolator.EASE_IN),
                        new KeyValue(opacityProp, 0.0,     Interpolator.EASE_IN))
        );
        slideOut.setOnFinished(e -> onFinished.run());
        slideOut.play();
    }

    /**
     * Animation de repositionnement vertical (quand un toast se ferme).
     */
    private static void animerReposition(Stage stage, double targetY) {
        double startY = stage.getY();

        DoubleProperty yProp = new SimpleDoubleProperty(startY);
        yProp.addListener((obs, oldV, newV) -> stage.setY(newV.doubleValue()));

        Timeline move = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(yProp, startY,  Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(180),
                        new KeyValue(yProp, targetY, Interpolator.EASE_BOTH))
        );
        move.play();
    }

    // ── Gestion de la file ────────────────────────────────────────────────────

    /** Ferme un toast avec animation de sortie puis repositionne les autres. */
    private static void fermerToast(Stage stage) {
        if (!stage.isShowing()) return;
        animerSortie(stage, () -> {
            stage.close();
            activeToasts.remove(stage);
            repositionnerToasts();
        });
    }

    /**
     * Repositionne tous les toasts actifs en les empilant depuis le bas.
     * Appelé après chaque ajout ou suppression de toast.
     */
    private static void repositionnerToasts() {
        javafx.geometry.Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double targetX = screen.getMaxX() - TOAST_WIDTH - MARGIN_RIGHT;

        int index = 0;
        for (Stage s : activeToasts) {
            if (!s.isShowing()) continue;
            double targetY = screen.getMaxY() - MARGIN_BOTTOM
                    - (TOAST_HEIGHT + 3 + SPACING) * (index + 1);
            // Repositionner X directement (déjà à la bonne position ou en cours d'animation)
            s.setX(targetX);
            // Animer Y
            animerReposition(s, targetY);
            index++;
        }
    }

    // ── Couleurs et icônes par type ───────────────────────────────────────────

    private static String bgColor(Type t) {
        return switch (t) {
            case SUCCESS  -> "#f0fdf4";
            case ERROR    -> "#fff5f5";
            case WARNING  -> "#fffbeb";
            case INFO     -> "#eff6ff";
            case MEDAL    -> "#fefce8";
            case DEADLINE -> "#fff7ed";
        };
    }

    private static String accentColor(Type t) {
        return switch (t) {
            case SUCCESS  -> "#22c55e";
            case ERROR    -> "#ef4444";
            case WARNING  -> "#f59e0b";
            case INFO     -> "#3b82f6";
            case MEDAL    -> "#eab308";
            case DEADLINE -> "#f97316";
        };
    }

    private static String iconBgColor(Type t) {
        return switch (t) {
            case SUCCESS  -> "#dcfce7";
            case ERROR    -> "#fee2e2";
            case WARNING  -> "#fef3c7";
            case INFO     -> "#dbeafe";
            case MEDAL    -> "#fef9c3";
            case DEADLINE -> "#ffedd5";
        };
    }

    private static String titleColor(Type t) {
        return switch (t) {
            case SUCCESS  -> "#15803d";
            case ERROR    -> "#b91c1c";
            case WARNING  -> "#b45309";
            case INFO     -> "#1d4ed8";
            case MEDAL    -> "#a16207";
            case DEADLINE -> "#c2410c";
        };
    }

    private static String messageColor(Type t) {
        return switch (t) {
            case SUCCESS  -> "#166534";
            case ERROR    -> "#991b1b";
            case WARNING  -> "#92400e";
            case INFO     -> "#1e40af";
            case MEDAL    -> "#854d0e";
            case DEADLINE -> "#9a3412";
        };
    }

    private static String closeColor(Type t) {
        return switch (t) {
            case SUCCESS  -> "#4ade80";
            case ERROR    -> "#f87171";
            case WARNING  -> "#fbbf24";
            case INFO     -> "#60a5fa";
            case MEDAL    -> "#facc15";
            case DEADLINE -> "#fb923c";
        };
    }

    private static String icon(Type t) {
        return switch (t) {
            case SUCCESS  -> "✅";
            case ERROR    -> "❌";
            case WARNING  -> "⚠️";
            case INFO     -> "ℹ️";
            case MEDAL    -> "🏆";
            case DEADLINE -> "⏰";
        };
    }
}
