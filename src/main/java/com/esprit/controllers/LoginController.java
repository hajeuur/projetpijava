package com.esprit.controllers;

import com.esprit.models.Utilisateur;
import com.esprit.services.UtilisateurService;
import com.esprit.utils.GoogleAuthService;
import com.esprit.utils.GoogleUserInfo;
import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
    @FXML private Button googleLoginButton;
    @FXML private VBox captchaContainer;

    private final UtilisateurService service = new UtilisateurService();
    private boolean captchaVerified = false;
    private WebEngine webEngine;
    private HttpServer captchaServer;

    public class JavaBridge {
        public void captchaVerified(String token) {
            System.out.println(">>> hCaptcha OK !");
            captchaVerified = true;
            Platform.runLater(() -> errorLabel.setText(""));
        }
        public void captchaExpired() { captchaVerified = false; }
    }

    private int findFreePort() {
        try (ServerSocket s = new ServerSocket(0)) { return s.getLocalPort(); }
        catch (Exception e) { return 7777; }
    }

    @FXML
    public void initialize() {
        if (captchaContainer != null) {
            try {
                int port = findFreePort();
                captchaServer = HttpServer.create(new InetSocketAddress("localhost", port), 0);
                captchaServer.createContext("/hcaptcha.html", exchange -> {
                    InputStream in = getClass().getResourceAsStream("/com/esprit/views/hcaptcha.html");
                    byte[] bytes = in.readAllBytes();
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                    exchange.sendResponseHeaders(200, bytes.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(bytes);
                    os.close();
                });
                captchaServer.start();

                WebView webView = new WebView();
                webView.setPrefWidth(320);
                webView.setPrefHeight(120);
                webView.setMinHeight(120);
                webView.setMaxHeight(120);
                webEngine = webView.getEngine();

                webEngine.getLoadWorker().stateProperty().addListener((obs, old, newState) -> {
                    if (newState == Worker.State.SUCCEEDED) {
                        try {
                            JSObject window = (JSObject) webEngine.executeScript("window");
                            window.setMember("javabridge", new JavaBridge());
                        } catch (Exception e) {
                            System.out.println(">>> Erreur bridge: " + e.getMessage());
                        }
                    }
                });

                webEngine.load("http://localhost:" + port + "/hcaptcha.html");
                captchaContainer.getChildren().add(webView);
            } catch (Exception e) {
                System.out.println(">>> Erreur serveur captcha: " + e.getMessage());
            }
        }
    }

    @FXML
    public void handleLogin() {
        String email = emailField.getText().trim();
        String mdp   = passwordField.getText().trim();

        if (email.isEmpty() || mdp.isEmpty()) {
            errorLabel.setText("Veuillez remplir tous les champs !"); return;
        }

        Utilisateur u = service.login(email, mdp);

        if (u == null) {
            errorLabel.setText("Email ou mot de passe incorrect !");
            captchaVerified = false;
            if (webEngine != null)
                Platform.runLater(() -> { try { webEngine.executeScript("hcaptcha.reset()"); } catch (Exception ignored) {} });
            return;
        }

        if (u.getStatus().equals("desactiver")) {
            errorLabel.setText("Votre compte est désactivé. Contactez l'administrateur !"); return;
        }

        if (captchaServer != null) captchaServer.stop(0);
        redirectByRole(u);
    }

    @FXML
    public void handleGoogleLogin() {
        errorLabel.setText("Ouverture du navigateur Google...");
        if (googleLoginButton != null) googleLoginButton.setDisable(true);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                GoogleUserInfo googleUser = GoogleAuthService.authenticate();
                if (googleUser == null || googleUser.getEmail() == null) {
                    Platform.runLater(() -> errorLabel.setText("Authentification Google échouée.")); return;
                }
                Utilisateur existing = service.findByEmail(googleUser.getEmail());
                if (existing != null) {
                    if (existing.getStatus().equals("desactiver")) {
                        Platform.runLater(() -> errorLabel.setText("Votre compte est désactivé.")); return;
                    }
                    if (captchaServer != null) captchaServer.stop(0);
                    Platform.runLater(() -> redirectByRole(existing));
                } else {
                    Utilisateur nouveau = new Utilisateur();
                    nouveau.setEmail(googleUser.getEmail());
                    nouveau.setNom(googleUser.getNom()    != null ? googleUser.getNom()    : "");
                    nouveau.setPrenom(googleUser.getPrenom() != null ? googleUser.getPrenom() : "");
                    nouveau.setMdp("");
                    nouveau.setRole("etudiant");
                    nouveau.setStatus("activer");
                    nouveau.setTrustScore(0.0);
                    nouveau.setRiskLevel("low");
                    service.ajouter(nouveau);
                    Utilisateur created = service.findByEmail(googleUser.getEmail());
                    if (captchaServer != null) captchaServer.stop(0);
                    Platform.runLater(() -> redirectByRole(created));
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> errorLabel.setText(e.getClass().getSimpleName() + ": " + e.getMessage()));
            } finally {
                Platform.runLater(() -> { if (googleLoginButton != null) googleLoginButton.setDisable(false); });
                executor.shutdown();
            }
        });
    }

    @FXML
    public void handleInscription() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/Inscription.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Inscription");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) { errorLabel.setText("Erreur : " + e.getMessage()); }
    }

    @FXML
    public void handleForgetPassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/views/ForgetPassword.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) { errorLabel.setText("Erreur : " + e.getMessage()); }
    }

    private void redirectByRole(Utilisateur u) {
        try {
            if (u == null) { errorLabel.setText("Erreur : utilisateur introuvable."); return; }
            String role = u.getRole();
            String fxmlPath = role.equals("admin")
                    ? "/com/esprit/views/BackOffice.fxml"
                    : "/com/esprit/views/FrontOffice.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            if (!role.equals("admin")) {
                FrontOfficeController controller = loader.getController();
                controller.setUtilisateur(u);
            }
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Erreur navigation : " + e.getMessage());
        }
    }
}