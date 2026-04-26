package edu.connection3a36.Controller;

import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentIntentCollection;
import com.stripe.param.PaymentIntentListParams;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class PricingBackController implements Initializable {

    @FXML private Label lblTotalCount, lblTotalVolume, lblSuccessCount, lblFailedCount;
    @FXML private TableView<TransactionRecord> tblTransactions;
    @FXML private TableColumn<TransactionRecord, String> colId, colClient, colDesc, colAmount, colStatus;

    private final String STRIPE_SK = "sk_test_51T3HLEAn7CRzwdrvGPToyqR7KSEjsMOzNYQVHCpR3Tk5TNsyiqBqyodGhoyPUyxhJYzJs1dMeeanM19nMJLepFwL00kIoimsOx";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Stripe.apiKey = STRIPE_SK;

        colId.setCellValueFactory(cellData -> cellData.getValue().idProperty());
        colClient.setCellValueFactory(cellData -> cellData.getValue().clientProperty());
        colDesc.setCellValueFactory(cellData -> cellData.getValue().descProperty());
        colAmount.setCellValueFactory(cellData -> cellData.getValue().amountProperty());
        colStatus.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        refreshTransactions();
    }

    @FXML
    private void refreshTransactions() {
        new Thread(() -> {
            try {
                PaymentIntentListParams params = PaymentIntentListParams.builder().setLimit(50L).build();
                PaymentIntentCollection paymentIntents = PaymentIntent.list(params);

                ObservableList<TransactionRecord> data = FXCollections.observableArrayList();
                long totalCount = 0;
                double totalVolume = 0;
                long successCount = 0;
                long failedCount = 0;

                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");

                for (PaymentIntent pi : paymentIntents.getData()) {
                    totalCount++;
                    double amount = pi.getAmount() / 100.0;
                    String status = pi.getStatus();
                    
                    if ("succeeded".equals(status)) {
                        successCount++;
                        totalVolume += amount;
                    } else if ("requires_payment_method".equals(status) || "canceled".equals(status)) {
                        failedCount++;
                    }

                    // Récupération du nom ou email
                    String clientDisplay = "Client inconnu";
                    if (pi.getReceiptEmail() != null) clientDisplay = pi.getReceiptEmail();
                    if (pi.getMetadata() != null && pi.getMetadata().containsKey("customer_name")) {
                        clientDisplay = pi.getMetadata().get("customer_name");
                    }

                    // Formatage de la date
                    String dateFormatted = sdf.format(new java.util.Date(pi.getCreated() * 1000L));

                    data.add(new TransactionRecord(
                            pi.getId(),
                            clientDisplay,
                            dateFormatted, // La date va dans la colonne Description
                            String.format("%.2f %s", amount, pi.getCurrency().toUpperCase()),
                            status.toUpperCase()
                    ));
                }

                final long fTotal = totalCount;
                final double fVolume = totalVolume;
                final long fSuccess = successCount;
                final long fFailed = failedCount;

                Platform.runLater(() -> {
                    tblTransactions.setItems(data);
                    lblTotalCount.setText(String.valueOf(fTotal));
                    lblTotalVolume.setText(String.format("%.2f EUR", fVolume));
                    lblSuccessCount.setText(String.valueOf(fSuccess));
                    lblFailedCount.setText(String.valueOf(fFailed));
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Erreur Stripe");
                    alert.setHeaderText("Impossible de récupérer les transactions");
                    alert.setContentText(e.getMessage());
                    alert.showAndWait();
                });
            }
        }).start();
    }

    @FXML
    private void openStripeDashboard() {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI("https://dashboard.stripe.com/test/payments"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class TransactionRecord {
        private final SimpleStringProperty id, client, desc, amount, status;

        public TransactionRecord(String id, String client, String desc, String amount, String status) {
            this.id = new SimpleStringProperty(id);
            this.client = new SimpleStringProperty(client);
            this.desc = new SimpleStringProperty(desc);
            this.amount = new SimpleStringProperty(amount);
            this.status = new SimpleStringProperty(status);
        }

        public SimpleStringProperty idProperty() { return id; }
        public SimpleStringProperty clientProperty() { return client; }
        public SimpleStringProperty descProperty() { return desc; }
        public SimpleStringProperty amountProperty() { return amount; }
        public SimpleStringProperty statusProperty() { return status; }
    }
}
