package edu.connection3a36.tools;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/**
 * Utilitaire de rendu Markdown → JavaFX Nodes.
 * Version stable (Retour à la base après annulation du clic par mot).
 */
public class MarkdownRenderer {

    private static final String COLOR_TEXT   = "#1a2340";
    private static final String COLOR_TITLE  = "#102c59";
    private static final String COLOR_BULLET = "#3b82f6";
    private static final String COLOR_NUMBER = "#6366f1";

    public static void render(String markdown, Pane target) {
        if (markdown == null || markdown.isBlank()) return;

        String[] parts = markdown.split("```");
        for (int i = 0; i < parts.length; i++) {
            if (i % 2 == 0) {
                renderTextBlock(parts[i], target);
            } else {
                String block = parts[i].trim();
                if (block.toLowerCase().startsWith("json")) continue;
                
                String codeContent = block;
                if (codeContent.contains("\n")) {
                    codeContent = codeContent.substring(codeContent.indexOf("\n") + 1);
                }
                if (!codeContent.isBlank()) {
                    Label codeLabel = new Label(codeContent.trim());
                    codeLabel.setWrapText(true);
                    codeLabel.setStyle("-fx-background-color: #f0f4ff; -fx-text-fill: #334155; "
                            + "-fx-padding: 10 14; -fx-background-radius: 8; "
                            + "-fx-font-family: 'Courier New'; -fx-font-size: 12px; "
                            + "-fx-border-color: #c7d2fe; -fx-border-radius: 8; -fx-border-width: 1;");
                    target.getChildren().add(codeLabel);
                }
            }
        }
    }

    private static void renderTextBlock(String block, Pane target) {
        if (block.isBlank()) return;
        String[] lines = block.split("\n");
        StringBuilder paragraph = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#")) {
                flushParagraph(paragraph, target);
                String titleText = stripBold(trimmed.replaceFirst("^#+\\s*", ""));
                Label lbl = new Label(titleText);
                lbl.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + COLOR_TITLE + "; -fx-padding: 5 0;");
                lbl.setWrapText(true);
                target.getChildren().add(lbl);
            } else if (trimmed.matches("^[-•*]\\s+.+")) {
                flushParagraph(paragraph, target);
                renderBullet(stripBold(trimmed.replaceFirst("^[-•*]\\s+", "")), target, false);
            } else if (trimmed.isBlank()) {
                flushParagraph(paragraph, target);
            } else {
                if (paragraph.length() > 0) paragraph.append(" ");
                paragraph.append(trimmed);
            }
        }
        flushParagraph(paragraph, target);
    }

    private static void flushParagraph(StringBuilder buf, Pane target) {
        if (buf.length() == 0) return;
        String text = stripBold(buf.toString().trim());
        if (!text.isBlank()) {
            Label lbl = new Label(text);
            lbl.setWrapText(true);
            lbl.setStyle("-fx-font-size: 13.5px; -fx-text-fill: " + COLOR_TEXT + "; -fx-line-spacing: 2;");
            target.getChildren().add(lbl);
        }
        buf.setLength(0);
    }

    private static void renderBullet(String content, Pane target, boolean numbered) {
        HBox row = new HBox(6);
        row.setPadding(new Insets(1, 0, 1, 10));
        Label bullet = new Label(numbered ? "1." : "•");
        bullet.setStyle("-fx-font-weight: bold; -fx-text-fill: " + COLOR_BULLET + ";");
        Label text = new Label(content);
        text.setWrapText(true);
        text.setStyle("-fx-font-size: 13px; -fx-text-fill: " + COLOR_TEXT + ";");
        row.getChildren().addAll(bullet, text);
        target.getChildren().add(row);
    }

    public static String stripBold(String text) {
        if (text == null) return "";
        return text.replaceAll("\\*\\*(.+?)\\*\\*", "$1").replaceAll("_(.+?)_", "$1");
    }
}
