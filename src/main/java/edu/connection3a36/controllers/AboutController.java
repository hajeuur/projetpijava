package edu.connection3a36.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import java.net.InetAddress;
import java.net.URL;
import java.util.ResourceBundle;

public class AboutController implements Initializable {

    @FXML private ImageView qrCodeImageView;
    @FXML private Label ipLabel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            String ip = InetAddress.getLocalHost().getHostAddress();
            ipLabel.setText("Serveur local : " + ip + ":8081");
            
            // On génère un vrai QR code avec une API externe pour le look premium
            qrCodeImageView.setImage(new Image("https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=http://" + ip + ":8081"));
        } catch (Exception e) {
            ipLabel.setText("Erreur lors de la récupération de l'IP");
        }
    }
}
