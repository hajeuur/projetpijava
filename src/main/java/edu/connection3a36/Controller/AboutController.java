package edu.connection3a36.Controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.awt.image.BufferedImage;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;
import java.net.URL;
import java.util.ResourceBundle;

public class AboutController implements Initializable {

    @FXML
    private ImageView qrCodeImageView;
    @FXML
    private Label ipLabel;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        new Thread(() -> {
            try {
                String ipAddress = getLocalIpAddress();
                // Utilisation du port 8000 par défaut pour Symfony
                String appUrl = "http://" + ipAddress + ":8000/home"; 

                Platform.runLater(() -> {
                    ipLabel.setText("Lien généré : " + appUrl);
                    System.out.println("QR Code URL: " + appUrl);
                });

                Image qrImage = generateQRCodeImage(appUrl);
                Platform.runLater(() -> qrCodeImageView.setImage(qrImage));

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> ipLabel.setText("Erreur lors de la génération du QR Code."));
            }
        }).start();
    }

    private Image generateQRCodeImage(String text) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 200, 200);

        BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
        return SwingFXUtils.toFXImage(bufferedImage, null);
    }

    private String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface iface : Collections.list(interfaces)) {
                if (iface.isLoopback() || !iface.isUp()) continue;
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                for (InetAddress addr : Collections.list(addresses)) {
                    if (addr instanceof java.net.Inet4Address) {
                        String ip = addr.getHostAddress();
                        if (ip.startsWith("192.") || ip.startsWith("172.") || ip.startsWith("10.")) {
                            return ip;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "127.0.0.1";
    }
}
