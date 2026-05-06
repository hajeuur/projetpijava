package edu.connection3a36.tools;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

/**
 * Utilitaire pour les alertes JavaFX réutilisables.
 */
public class AlertUtil {

    /**
     * Affiche une alerte de succès
     */
    public static void showSuccess(String message) {
        showCustomAlert("Succès", "Opération réussie", message, "✅", "#10b981", false);
    }

    /**
     * Affiche une alerte d'erreur
     */
    public static void showError(String message) {
        showCustomAlert("Erreur", "Une erreur est survenue", message, "❌", "#ef4444", false);
    }

    /**
     * Affiche un avertissement
     */
    public static void showWarning(String message) {
        showCustomAlert("Attention", "Veuillez vérifier", message, "⚠️", "#f59e0b", false);
    }

    /**
     * Affiche une boîte de confirmation (Oui / Non)
     * @return true si l'utilisateur confirme
     */
    public static boolean showConfirmation(String message) {
        return showCustomAlert("Confirmation", "Êtes-vous sûr(e) ?", message, "❓", "#3b82f6", true);
    }

    private static boolean showCustomAlert(String windowTitle, String titleText, String message, String iconEmoji, String colorHex, boolean isConfirm) {
        javafx.scene.control.Dialog<javafx.scene.control.ButtonType> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle(windowTitle);
        dialog.setHeaderText(null);
        dialog.getDialogPane().setStyle("-fx-background-color: white;");

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(15);
        content.setPadding(new javafx.geometry.Insets(25));
        content.setPrefWidth(420);

        // Header
        javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(15);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.setStyle("-fx-padding: 0 0 15 0; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 1 0;");
        
        javafx.scene.control.Label icon = new javafx.scene.control.Label(iconEmoji);
        icon.setStyle("-fx-font-size: 32px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        
        javafx.scene.control.Label title = new javafx.scene.control.Label(titleText);
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        
        header.getChildren().addAll(icon, title);

        // Body message
        javafx.scene.control.Label msgLabel = new javafx.scene.control.Label(message);
        msgLabel.setWrapText(true);
        msgLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #475569; -fx-line-spacing: 4;");
        
        content.getChildren().addAll(header, msgLabel);
        dialog.getDialogPane().setContent(content);

        if (isConfirm) {
            javafx.scene.control.ButtonType btnYes = new javafx.scene.control.ButtonType("Oui, confirmer", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
            javafx.scene.control.ButtonType btnNo = new javafx.scene.control.ButtonType("Annuler", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().addAll(btnYes, btnNo);

            javafx.scene.Node yesNode = dialog.getDialogPane().lookupButton(btnYes);
            if (yesNode != null) {
                yesNode.setStyle("-fx-background-color: " + colorHex + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 24; -fx-background-radius: 6; -fx-cursor: hand;");
            }
            
            javafx.scene.Node noNode = dialog.getDialogPane().lookupButton(btnNo);
            if (noNode != null) {
                noNode.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-font-weight: bold; -fx-padding: 10 24; -fx-cursor: hand;");
            }

            Optional<javafx.scene.control.ButtonType> result = dialog.showAndWait();
            return result.isPresent() && result.get() == btnYes;
        } else {
            javafx.scene.control.ButtonType btnOk = new javafx.scene.control.ButtonType("OK", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().add(btnOk);

            javafx.scene.Node okNode = dialog.getDialogPane().lookupButton(btnOk);
            if (okNode != null) {
                okNode.setStyle("-fx-background-color: " + colorHex + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 24; -fx-background-radius: 6; -fx-cursor: hand;");
            }

            dialog.showAndWait();
            return true;
        }
    }
}
