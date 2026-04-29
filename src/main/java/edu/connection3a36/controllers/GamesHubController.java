package edu.connection3a36.controllers;

import edu.connection3a36.services.WikipediaService;
import edu.connection3a36.tools.AlertUtil;
import edu.connection3a36.tools.MarkdownRenderer;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import javax.sound.sampled.*;
import java.awt.image.BufferedImage;
import java.util.Timer;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class GamesHubController {

    // ── Tab buttons ───────────────────────────────────────────────
    @FXML private Button tab1,tab2,tab3,tab4,tab5,tab6,tab7;
    @FXML private VBox pane1,pane2,pane3,pane4,pane5,pane6,pane7;

    // ── Tab 7 : Wikipedia ─────────────────────────────────────────
    @FXML private TextField txtWikiSearch;
    @FXML private Label lblWikiTitle;
    @FXML private VBox wikiContentArea;

    private final WikipediaService wikiService = new WikipediaService();

    // ── Tab 1 : Breathing ─────────────────────────────────────────
    @FXML private Circle breathingCircle;
    @FXML private Label lblBreathingText, lblBreathPhase, lblBreathCycles;
    @FXML private ProgressBar breathProgressBar;
    @FXML private Button btnBreathe;
    private Timeline breathTimeline;
    private int breathCycles = 0;

    // ── Tab 2 : Wave ──────────────────────────────────────────────
    @FXML private Canvas waveCanvas;
    @FXML private Label lblMeditationTimer, lblMeditationStatus;
    @FXML private Button btnWaveStart;
    private AnimationTimer waveAnimator;
    private Timer meditationTimer;
    private int meditationSeconds = 5 * 60;
    private boolean waveRunning = false;
    private double waveOffset = 0;

    // ── Tab 3 : Dyslexia ──────────────────────────────────────────
    @FXML private Button btnDyslexia, btnDyslexiaApply;
    @FXML private Slider fontSizeSlider, letterSpacingSlider;
    @FXML private VBox previewBox;
    @FXML private Label previewText1, previewText2, previewText3;
    private boolean dyslexiaActive = false;
    private String currentBg = "#fdf6e3";

    // ── Tab 4 : Son ───────────────────────────────────────────────
    @FXML private Label lblSoundSymbol, lblSoundFeedback, lblSoundScore;
    @FXML private HBox soundChoicesBox;
    @FXML private Button btnPlaySound;
    private final String[][] SOUNDS = {{"A","B","C","D"},{"MA","BA","LA","SA"},{"PA","TA","KA","RA"}};
    private int soundIdx = 0, soundRound = 0, soundCorrect = 0, soundTotal = 0;

    // ── Tab 5 : Pomodoro ──────────────────────────────────────────
    @FXML private Label lblPomodoroTime, lblPomodoroMode, lblPomodoroSessions, lblPomodoroMotivation;
    @FXML private Button btnPomodoro;
    @FXML private HBox pomodoroDotsBox;
    private Timer pomodoroTimer;
    private int pomodoroSeconds = 25 * 60;
    private boolean pomodoroWork = true, pomodoroRunning = false;
    private int pomodoroSessionsDone = 0;
    private static final String[] QUOTES = {
        "\"La concentration est la racine de toute capacité.\"",
        "\"Un esprit focalisé peut déplacer des montagnes.\"",
        "\"Chaque Pomodoro est une victoire sur la procrastination.\"",
        "\"Le succès est une série de petites décisions.\"" };

    // ── Tab 6 : Quiz + Camera ─────────────────────────────────────
    @FXML private Label lblQuizQuestion, lblQuizFeedback, lblQuizScore, lblQuizTimer, lblCameraStatus, lblGestureDetected;
    @FXML private ProgressBar quizTimerBar;
    @FXML private Button btnA, btnB, btnC, btnD, btnQuizStart;
    @FXML private HBox quizAnswersBox, quizAnswers4Box;
    @FXML private ImageView webcamView;

    private int quizScore = 0, quizQIdx = 0, quizTimerSecs = 10;
    private Timer quizTimer;
    private final AtomicBoolean cameraRunning = new AtomicBoolean(false);
    private Thread cameraThread;

    // Quiz data: {question, answerA, answerB, correct (0=A,1=B)}
    private static final Object[][] QUESTIONS = {
        {"Le Pomodoro dure 25 minutes.", "VRAI", "FAUX", 0},
        {"La lecture active améliore la rétention.", "VRAI", "FAUX", 0},
        {"Le sommeil n'affecte pas la mémoire.", "VRAI", "FAUX", 1},
        {"L'espacement des révisions est efficace.", "VRAI", "FAUX", 0},
        {"Multitâcher améliore la productivité.", "VRAI", "FAUX", 1},
        {"L'exercice améliore les capacités cognitives.", "VRAI", "FAUX", 0},
    };

    @FXML public void initialize() {
        showPane(pane1); updateTimerLabels();
        updateSoundUI(); updatePomodoroDots();
        
        // Configuration du Shift+Clic sur les mots (Wikipedia Lookup)
        MarkdownRenderer.lookupAction = this::handleWikiSearchDirect;
    }

    /** Version directe de recherche pour le Shift+Clic */
    private void handleWikiSearchDirect(String word) {
        txtWikiSearch.setText(word);
        handleWikiSearch();
    }

    // ── TABS ──────────────────────────────────────────────────────
    @FXML void showTab1() { showPane(pane1); activateTab(tab1); }
    @FXML void showTab2() { showPane(pane2); activateTab(tab2); }
    @FXML void showTab3() { showPane(pane3); activateTab(tab3); }
    @FXML void showTab4() { showPane(pane4); activateTab(tab4); }
    @FXML void showTab5() { showPane(pane5); activateTab(tab5); }
    @FXML void showTab6() { showPane(pane6); activateTab(tab6); }
    @FXML void showTab7() { showPane(pane7); activateTab(tab7); }

    private void showPane(VBox active) {
        for (VBox p : new VBox[]{pane1,pane2,pane3,pane4,pane5,pane6,pane7}) {
            if (p != null) {
                p.setVisible(p == active); p.setManaged(p == active);
            }
        }
    }
    private void activateTab(Button active) {
        String on  = "-fx-background-color:#3b82f6;-fx-text-fill:white;-fx-background-radius:20;-fx-padding:6 16;-fx-cursor:hand;";
        String off = "-fx-background-color:#e2e8f0;-fx-background-radius:20;-fx-padding:6 16;-fx-cursor:hand;";
        for (Button b : new Button[]{tab1,tab2,tab3,tab4,tab5,tab6,tab7}) {
            if (b != null) b.setStyle(b==active?on:off);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // TAB 1 : RESPIRATION 4-7-8
    // ══════════════════════════════════════════════════════════════
    @FXML void startBreathing() {
        if (breathTimeline != null && breathTimeline.getStatus() == Animation.Status.RUNNING) {
            breathTimeline.stop(); breathingCircle.setRadius(40);
            lblBreathingText.setText("Prêt ?"); lblBreathPhase.setText(""); breathProgressBar.setProgress(0);
            btnBreathe.setText("▶  Démarrer l'exercice"); return;
        }
        breathCycles = 0; btnBreathe.setText("⏹  Arrêter");
        breathTimeline = new Timeline();
        double total = 19.0;
        // INSPIRE 0-4s
        breathTimeline.getKeyFrames().addAll(
            new KeyFrame(Duration.ZERO,
                e -> { lblBreathPhase.setText("🌬 Inspirez..."); lblBreathingText.setText("4s"); },
                new KeyValue(breathingCircle.radiusProperty(), 40),
                new KeyValue(breathProgressBar.progressProperty(), 0.0)),
            new KeyFrame(Duration.seconds(4),
                e -> { lblBreathPhase.setText("🤐 Retenez..."); lblBreathingText.setText("7s"); },
                new KeyValue(breathingCircle.radiusProperty(), 100),
                new KeyValue(breathProgressBar.progressProperty(), 4/total)),
            new KeyFrame(Duration.seconds(11),
                e -> { lblBreathPhase.setText("💨 Expirez..."); lblBreathingText.setText("8s"); },
                new KeyValue(breathingCircle.radiusProperty(), 100),
                new KeyValue(breathProgressBar.progressProperty(), 11/total)),
            new KeyFrame(Duration.seconds(19),
                e -> { breathCycles++; lblBreathCycles.setText("Cycles complétés : " + breathCycles);
                       lblBreathingText.setText(""); },
                new KeyValue(breathingCircle.radiusProperty(), 40),
                new KeyValue(breathProgressBar.progressProperty(), 1.0))
        );
        breathTimeline.setCycleCount(Animation.INDEFINITE);
        breathTimeline.play();
    }

    // ══════════════════════════════════════════════════════════════
    // TAB 2 : VAGUE DE CALME
    // ══════════════════════════════════════════════════════════════
    @FXML void startWave() {
        if (waveRunning) { stopWave(); return; }
        waveRunning = true; btnWaveStart.setText("⏹ Arrêter");
        lblMeditationStatus.setText("Respirez doucement et observez la vague...");
        meditationSeconds = 5 * 60;
        // Canvas animation
        GraphicsContext gc = waveCanvas.getGraphicsContext2D();
        waveAnimator = new AnimationTimer() {
            @Override public void handle(long now) {
                drawWave(gc); waveOffset += 0.04;
            }
        };
        waveAnimator.start();
        // Countdown
        meditationTimer = new Timer();
        meditationTimer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                Platform.runLater(() -> {
                    if (meditationSeconds > 0) {
                        meditationSeconds--;
                        int m = meditationSeconds/60, s = meditationSeconds%60;
                        lblMeditationTimer.setText(String.format("%02d:%02d", m, s));
                    } else {
                        stopWave();
                        lblMeditationStatus.setText("✅ Méditation terminée. Bravo !");
                    }
                });
            }
        }, 1000, 1000);
    }

    private void drawWave(GraphicsContext gc) {
        double w = waveCanvas.getWidth(), h = waveCanvas.getHeight();
        gc.clearRect(0,0,w,h);
        gc.setFill(Color.web("#e0f2fe")); gc.fillRect(0,0,w,h);
        for (int layer = 0; layer < 3; layer++) {
            double amp = 18 - layer*4, speed = 1 + layer*0.5, alpha = 0.6 - layer*0.15;
            gc.setStroke(Color.color(0.06, 0.45, 0.72, alpha)); gc.setLineWidth(2.5 - layer*0.5);
            gc.beginPath();
            gc.moveTo(0, h/2);
            for (double x = 0; x <= w; x += 2) {
                double y = h/2 + amp * Math.sin((x/60) + waveOffset * speed + layer * Math.PI/3);
                gc.lineTo(x, y);
            }
            gc.stroke();
            gc.setFill(Color.color(0.06, 0.45, 0.72, alpha*0.3));
            gc.lineTo(w, h); gc.lineTo(0, h); gc.closePath(); gc.fill();
        }
    }

    @FXML void resetWave() {
        stopWave(); meditationSeconds = 5*60;
        lblMeditationTimer.setText("05:00");
        GraphicsContext gc = waveCanvas.getGraphicsContext2D();
        gc.clearRect(0,0,waveCanvas.getWidth(),waveCanvas.getHeight());
    }

    private void stopWave() {
        waveRunning = false; btnWaveStart.setText("🌊 Démarrer");
        if (waveAnimator != null) waveAnimator.stop();
        if (meditationTimer != null) meditationTimer.cancel();
    }

    // ══════════════════════════════════════════════════════════════
    // TAB 3 : DYSLEXIE
    // ══════════════════════════════════════════════════════════════
    @FXML void setBgCream()  { currentBg="#fdf6e3"; applyPreview(); }
    @FXML void setBgGreen()  { currentBg="#e8f5e9"; applyPreview(); }
    @FXML void setBgBlue()   { currentBg="#e3f2fd"; applyPreview(); }
    @FXML void setBgNormal() { currentBg="white";   applyPreview(); }

    @FXML void adjustFontSize()      { applyPreview(); }
    @FXML void adjustLetterSpacing() { applyPreview(); }

    private void applyPreview() {
        double fs = fontSizeSlider.getValue();
        double ls = letterSpacingSlider.getValue();
        String style = String.format("-fx-font-size:%.0fpx; -fx-letter-spacing:%.1f;", fs, ls);
        previewBox.setStyle("-fx-background-color:"+currentBg+";-fx-padding:16;-fx-background-radius:10;-fx-border-color:#e2e8f0;-fx-border-radius:10;");
        for (Label l : new Label[]{previewText1,previewText2,previewText3}) l.setStyle(style);
    }

    @FXML void toggleDyslexiaMode() {
        dyslexiaActive = !dyslexiaActive;
        if (dyslexiaActive) {
            double fs = fontSizeSlider.getValue();
            btnDyslexiaApply.setText("✅ Désactiver");
            btnDyslexia.setText("📖 Désactiver");
            btnDyslexia.getScene().getRoot().setStyle(
                "-fx-font-family:'Arial'; -fx-font-size:"+fs+"px; -fx-background-color:"+currentBg+";");
        } else {
            btnDyslexiaApply.setText("✅ Appliquer");
            btnDyslexia.setText("📖 Mode Dyslexie");
            btnDyslexia.getScene().getRoot().setStyle("");
        }
    }
    @FXML void resetDyslexia() {
        fontSizeSlider.setValue(15); letterSpacingSlider.setValue(2);
        currentBg="#fdf6e3"; applyPreview();
        if (dyslexiaActive) { dyslexiaActive=true; toggleDyslexiaMode(); }
    }

    // ══════════════════════════════════════════════════════════════
    // TAB 4 : CHASSE AU SON
    // ══════════════════════════════════════════════════════════════
    @FXML void playCurrentSound() {
        if (soundChoicesBox.getChildren().isEmpty()) buildSoundRound();
        String sym = SOUNDS[soundRound % SOUNDS.length][soundIdx];
        playBeepFor(sym);
        lblSoundFeedback.setText("Quel son entends-tu ?");
        lblSoundFeedback.setStyle("-fx-text-fill:#7c3aed;");
    }

    private void buildSoundRound() {
        soundChoicesBox.getChildren().clear();
        String[] set = SOUNDS[soundRound % SOUNDS.length];
        soundIdx = new Random().nextInt(set.length);
        List<String> opts = new ArrayList<>(Arrays.asList(set));
        Collections.shuffle(opts);
        for (String opt : opts) {
            Button b = new Button(opt);
            b.setStyle("-fx-background-color:#7c3aed;-fx-text-fill:white;-fx-font-size:18px;-fx-font-weight:bold;-fx-padding:12 22;-fx-background-radius:12;-fx-cursor:hand;");
            b.setOnAction(e -> checkSoundAnswer(opt, set[soundIdx]));
            soundChoicesBox.getChildren().add(b);
        }
        lblSoundSymbol.setText("?");
    }

    private void checkSoundAnswer(String chosen, String correct) {
        soundTotal++;
        if (chosen.equals(correct)) {
            soundCorrect++; lblSoundSymbol.setText("✅ " + correct);
            lblSoundFeedback.setText("Bravo ! C'était bien \"" + correct + "\" !");
            lblSoundFeedback.setStyle("-fx-text-fill:#059669; -fx-font-weight:bold;");
        } else {
            lblSoundSymbol.setText("❌ " + correct);
            lblSoundFeedback.setText("Raté ! C'était \"" + correct + "\", pas \"" + chosen + "\".");
            lblSoundFeedback.setStyle("-fx-text-fill:#dc2626; -fx-font-weight:bold;");
        }
        lblSoundScore.setText("Score : " + soundCorrect + " / " + soundTotal);
        soundRound++;
        new Timer().schedule(new TimerTask() {
            public void run() { Platform.runLater(() -> buildSoundRound()); }
        }, 1500);
    }

    private void updateSoundUI() { lblSoundScore.setText("Score : 0 / 0"); }

    /** Génère un son simple (bip) dont la fréquence encode la lettre. */
    private void playBeepFor(String symbol) {
        new Thread(() -> {
            try {
                int freq = 440 + symbol.hashCode() % 400;
                AudioFormat fmt = new AudioFormat(44100, 8, 1, true, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(fmt); line.start();
                int dur = 600; byte[] buf = new byte[(int)(fmt.getSampleRate() * dur / 1000)];
                for (int i = 0; i < buf.length; i++)
                    buf[i] = (byte)(80 * Math.sin(2 * Math.PI * freq * i / fmt.getSampleRate()));
                line.write(buf, 0, buf.length);
                line.drain(); line.close();
                Platform.runLater(() -> lblSoundSymbol.setText(symbol));
            } catch (Exception e) {
                Platform.runLater(() -> lblSoundSymbol.setText(symbol));
            }
        }).start();
    }

    // ══════════════════════════════════════════════════════════════
    // TAB 5 : POMODORO
    // ══════════════════════════════════════════════════════════════
    @FXML void togglePomodoro() {
        if (pomodoroRunning) {
            pomodoroTimer.cancel(); pomodoroRunning = false; btnPomodoro.setText("▶ Reprendre"); return;
        }
        pomodoroRunning = true; btnPomodoro.setText("⏸ Pause");
        pomodoroTimer = new Timer();
        pomodoroTimer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                Platform.runLater(() -> {
                    if (pomodoroSeconds > 0) { pomodoroSeconds--; updateTimerLabels(); }
                    else {
                        pomodoroTimer.cancel(); pomodoroRunning = false; btnPomodoro.setText("▶ Démarrer");
                        if (pomodoroWork) {
                            pomodoroSessionsDone++; updatePomodoroDots();
                            pomodoroWork = false; pomodoroSeconds = 5 * 60;
                            lblPomodoroMode.setText("MODE PAUSE ☕");
                            lblPomodoroMode.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#10b981;");
                            AlertUtil.showSuccess("🍅 Pomodoro terminé ! Pause 5 minutes.");
                        } else {
                            pomodoroWork = true; pomodoroSeconds = 25 * 60;
                            lblPomodoroMode.setText("MODE TRAVAIL");
                            lblPomodoroMode.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#dc2626;");
                            lblPomodoroMotivation.setText(QUOTES[pomodoroSessionsDone % QUOTES.length]);
                            AlertUtil.showSuccess("☕ Pause terminée ! Retour au travail.");
                        }
                    }
                });
            }
        }, 1000, 1000);
    }

    @FXML void resetPomodoro() {
        if (pomodoroTimer != null) pomodoroTimer.cancel();
        pomodoroRunning = false; pomodoroWork = true; pomodoroSeconds = 25*60;
        btnPomodoro.setText("▶ Démarrer"); updateTimerLabels();
        lblPomodoroMode.setText("MODE TRAVAIL");
    }

    private void updateTimerLabels() {
        int m = pomodoroSeconds/60, s = pomodoroSeconds%60;
        lblPomodoroTime.setText(String.format("%02d:%02d", m, s));
        lblPomodoroSessions.setText("Sessions: " + pomodoroSessionsDone);
    }

    private void updatePomodoroDots() {
        pomodoroDotsBox.getChildren().clear();
        for (int i = 0; i < Math.max(4, pomodoroSessionsDone + 1); i++) {
            Label dot = new Label(i < pomodoroSessionsDone ? "🍅" : "⬜");
            pomodoroDotsBox.getChildren().add(dot);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // TAB 6 : SPEED QUIZ
    // ══════════════════════════════════════════════════════════════
    @FXML void startQuiz() {
        quizScore = 0; quizQIdx = 0; lblQuizScore.setText("⭐ 0 pts");
        btnQuizStart.setDisable(true); quizAnswersBox.setDisable(false);
        lblQuizFeedback.setText("");
        loadQuestion();
    }

    private void loadQuestion() {
        if (quizQIdx >= QUESTIONS.length) { endQuiz(); return; }
        Object[] q = QUESTIONS[quizQIdx];
        lblQuizQuestion.setText((quizQIdx+1) + "/" + QUESTIONS.length + " — " + q[0]);
        btnA.setText("A : " + q[1]); btnB.setText("B : " + q[2]);
        startQuizTimer();
    }

    private void startQuizTimer() {
        quizTimerSecs = 10;
        if (quizTimer != null) quizTimer.cancel();
        quizTimer = new Timer();
        quizTimer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                Platform.runLater(() -> {
                    quizTimerSecs--;
                    lblQuizTimer.setText("⏱ " + quizTimerSecs + "s");
                    quizTimerBar.setProgress(quizTimerSecs / 10.0);
                    if (quizTimerSecs <= 0) {
                        quizTimer.cancel();
                        showFeedback(false, "⏰ Temps écoulé !");
                        nextAfterDelay();
                    }
                });
            }
        }, 1000, 1000);
    }

    @FXML void handleAnswerA() { checkAnswer(0); }
    @FXML void handleAnswerB() { checkAnswer(1); }
    @FXML void handleAnswerC() { checkAnswer(2); }
    @FXML void handleAnswerD() { checkAnswer(3); }

    private void checkAnswer(int chosen) {
        if (quizTimer != null) quizTimer.cancel();
        boolean correct = (chosen == (int) QUESTIONS[quizQIdx][3]);
        if (correct) {
            int pts = Math.max(1, quizTimerSecs) * 10;
            quizScore += pts;
            lblQuizScore.setText("⭐ " + quizScore + " pts");
            showFeedback(true, "✅ Correct ! +" + pts + " pts");
        } else {
            showFeedback(false, "❌ Incorrect ! La bonne réponse était : " + QUESTIONS[quizQIdx][3+((int)QUESTIONS[quizQIdx][3])]);
        }
        nextAfterDelay();
    }

    private void showFeedback(boolean ok, String msg) {
        lblQuizFeedback.setText(msg);
        lblQuizFeedback.setStyle(ok ? "-fx-text-fill:#059669;-fx-font-weight:bold;" : "-fx-text-fill:#dc2626;-fx-font-weight:bold;");
    }

    private void nextAfterDelay() {
        quizAnswersBox.setDisable(true);
        new Timer().schedule(new TimerTask() {
            public void run() { Platform.runLater(() -> { quizQIdx++; quizAnswersBox.setDisable(false); loadQuestion(); }); }
        }, 1500);
    }

    private void endQuiz() {
        quizAnswersBox.setDisable(true); btnQuizStart.setDisable(false);
        int pct = quizScore * 100 / (QUESTIONS.length * 100);
        String emoji = pct >= 80 ? "🏆" : pct >= 50 ? "👍" : "💪";
        lblQuizQuestion.setText(emoji + " Quiz terminé ! Score : " + quizScore + " pts sur " + (QUESTIONS.length*100));
        lblQuizFeedback.setText("");
    }

    // ── Gestes manuels (boutons 👍/👎) ───────────────────────────
    @FXML void gestureTrueManual()  { lblGestureDetected.setText("👍 VRAI"); if (!quizAnswersBox.isDisable()) checkAnswer(0); }
    @FXML void gestureFalseManual() { lblGestureDetected.setText("👎 FAUX"); if (!quizAnswersBox.isDisable()) checkAnswer(1); }

    // ── Caméra (Webcam-Capture) ───────────────────────────────────
    @FXML void startCamera() {
        if (cameraRunning.get()) return;
        cameraRunning.set(true);
        lblCameraStatus.setText("Initialisation de la caméra...");
        
        cameraThread = new Thread(() -> {
            try {
                com.github.sarxos.webcam.Webcam cam = com.github.sarxos.webcam.Webcam.getDefault();
                if (cam == null) {
                    Platform.runLater(() -> {
                        lblCameraStatus.setText("❌ Aucune caméra détectée.");
                        AlertUtil.showError("Impossible de trouver une webcam sur ce PC.");
                    });
                    cameraRunning.set(false); return;
                }
                
                // Set explicit size
                java.awt.Dimension[] sizes = cam.getViewSizes();
                if(sizes != null && sizes.length > 0) {
                    cam.setViewSize(sizes[0]);
                }
                
                if(!cam.isOpen()) {
                    cam.open();
                }
                
                Platform.runLater(() -> lblCameraStatus.setText("✅ Caméra active : " + cam.getName()));
                while (cameraRunning.get() && cam.isOpen()) {
                    BufferedImage img = cam.getImage();
                    if (img != null) {
                        WritableImage fxImg = bufferedToFx(img);
                        Platform.runLater(() -> webcamView.setImage(fxImg));
                    }
                    Thread.sleep(60); // ~15 fps
                }
                cam.close();
                Platform.runLater(() -> { webcamView.setImage(null); lblCameraStatus.setText("Caméra arrêtée."); });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblCameraStatus.setText("Erreur caméra : " + e.getMessage());
                    AlertUtil.showError("Vérifiez vos permissions ou si la caméra est utilisée par une autre app.");
                });
                cameraRunning.set(false);
            }
        });
        cameraThread.setDaemon(true);
        cameraThread.start();
    }

    @FXML void stopCamera() {
        cameraRunning.set(false);
    }

    private WritableImage bufferedToFx(BufferedImage bImg) {
        int w = bImg.getWidth(), h = bImg.getHeight();
        WritableImage img = new WritableImage(w, h);
        PixelWriter pw = img.getPixelWriter();
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int rgb = bImg.getRGB(x, y);
                pw.setArgb(x, y, rgb);
            }
        return img;
    }

    // ══════════════════════════════════════════════════════════════
    // TAB 7 : WIKIPEDIA NOTIONS
    // ══════════════════════════════════════════════════════════════
    @FXML void handleWikiSearch() {
        String notion = txtWikiSearch.getText().trim();
        if (notion.isEmpty()) {
            AlertUtil.showError("Veuillez entrer une notion à rechercher.");
            return;
        }

        lblWikiTitle.setText("🔍 Recherche : " + notion + "...");
        wikiContentArea.getChildren().clear();
        Label lblLoading = new Label("Récupération de l'explication en cours...");
        lblLoading.setStyle("-fx-text-fill: #64748b; -fx-font-style: italic;");
        wikiContentArea.getChildren().add(lblLoading);

        new Thread(() -> {
            try {
                String summary = wikiService.getSummary(notion);
                Platform.runLater(() -> {
                    lblWikiTitle.setText("📚 " + notion);
                    wikiContentArea.getChildren().clear();
                    MarkdownRenderer.render(summary, wikiContentArea);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblWikiTitle.setText("❌ Erreur");
                    wikiContentArea.getChildren().clear();
                    wikiContentArea.getChildren().add(new Label("Une erreur est survenue : " + e.getMessage()));
                });
            }
        }).start();
    }
}
