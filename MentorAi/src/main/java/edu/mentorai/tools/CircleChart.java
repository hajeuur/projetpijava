package edu.mentorai.tools;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class CircleChart {

    public static Canvas createDonut(double atteints, double enCours,
                                     double abandonnes, double total,
                                     String centerText, String centerSubText) {
        Canvas canvas = new Canvas(150, 150);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        double cx = 75, cy = 75, r = 60, strokeW = 18;

        if (total == 0) {
            // Cercle gris vide
            gc.setStroke(Color.web("#e0e0e0"));
            gc.setLineWidth(strokeW);
            gc.strokeOval(cx - r, cy - r, r * 2, r * 2);
        } else {
            double startAngle = 90;

            // Atteints — vert
            double arcAtteints = (atteints / total) * 360;
            // EnCours — jaune
            double arcEnCours = (enCours / total) * 360;
            // Abandonnes — rouge
            double arcAbandonnes = (abandonnes / total) * 360;

            if (arcAtteints > 0) {
                gc.setStroke(Color.web("#198754"));
                gc.setLineWidth(strokeW);
                gc.strokeArc(cx - r, cy - r, r * 2, r * 2,
                        startAngle, -arcAtteints, javafx.scene.shape.ArcType.OPEN);
                startAngle -= arcAtteints;
            }

            if (arcEnCours > 0) {
                gc.setStroke(Color.web("#ffc107"));
                gc.setLineWidth(strokeW);
                gc.strokeArc(cx - r, cy - r, r * 2, r * 2,
                        startAngle, -arcEnCours, javafx.scene.shape.ArcType.OPEN);
                startAngle -= arcEnCours;
            }

            if (arcAbandonnes > 0) {
                gc.setStroke(Color.web("#d52e28"));
                gc.setLineWidth(strokeW);
                gc.strokeArc(cx - r, cy - r, r * 2, r * 2,
                        startAngle, -arcAbandonnes, javafx.scene.shape.ArcType.OPEN);
            }
        }

        // Texte centre
        gc.setFill(Color.web("#102c59"));
        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 22));
        double textW = centerText.length() * 13;
        gc.fillText(centerText, cx - textW / 2, cy + 7);

        // Sous-texte
        gc.setFill(Color.web("#888888"));
        gc.setFont(javafx.scene.text.Font.font("Arial", 11));
        double subW = centerSubText.length() * 6;
        gc.fillText(centerSubText, cx - subW / 2, cy + 22);

        return canvas;
    }
}