package edu.connection3a36.Controller;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import javafx.application.HostServices;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;

import java.net.URL;
import java.util.ResourceBundle;

public class PricingController implements Initializable {

    @FXML
    private ToggleButton pricingToggle;
    @FXML
    private Label pricePlus;
    @FXML
    private Label priceBusiness;
    @FXML
    private Button btnBasique;
    @FXML
    private Button btnPlus;
    @FXML
    private Button btnBusiness;

    private static final String STRIPE_SECRET_KEY = "sk_test_51T3HLEAn7CRzwdrvGPToyqR7KSEjsMOzNYQVHCpR3Tk5TNsyiqBqyodGhoyPUyxhJYzJs1dMeeanM19nMJLepFwL00kIoimsOx";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Stripe.apiKey = STRIPE_SECRET_KEY;

        pricingToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                // Yearly
                pricePlus.setText("65");
                priceBusiness.setText("125");
            } else {
                // Monthly
                pricePlus.setText("75");
                priceBusiness.setText("135");
            }
        });
    }

    @FXML
    private void handleFreePlan() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Plan Basique");
        alert.setHeaderText(null);
        alert.setContentText("Vous avez choisi le plan Basique. Profitez de vos 5 conversations gratuites par jour !");
        alert.showAndWait();
    }

    @FXML
    private void handlePlusPlan() {
        createCheckoutSession("Plus", Long.parseLong(pricePlus.getText()) * 100);
    }

    @FXML
    private void handleBusinessPlan() {
        createCheckoutSession("Business", Long.parseLong(priceBusiness.getText()) * 100);
    }

    private void createCheckoutSession(String planName, long amount) {
        try {
            SessionCreateParams params = SessionCreateParams.builder()
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("https://example.com/success") // Dans un projet réel, ceci redirigerait vers une page de succès
                .setCancelUrl("https://example.com/cancel")
                .addLineItem(
                    SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(
                            SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("usd") // On garde usd pour Stripe test si nécessaire, ou tnd si supporté
                                .setUnitAmount(amount)
                                .setProductData(
                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Abonnement MentorAI - " + planName)
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .build();

            Session session = Session.create(params);
            
            // Ouvrir l'URL de paiement dans le navigateur
            openUrl(session.getUrl());

        } catch (StripeException e) {
            e.printStackTrace();
            showError("Erreur Stripe", "Impossible de créer la session de paiement : " + e.getMessage());
        }
    }

    private void openUrl(String url) {
        // Pour JavaFX, on utilise HostServices si possible, sinon java.awt.Desktop
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
