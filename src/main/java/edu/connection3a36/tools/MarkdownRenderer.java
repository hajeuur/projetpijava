package edu.connection3a36.tools;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * Utilitaire de rendu Markdown → JavaFX Nodes.
 * Supporte : **gras**, # titres, - / • listes, paragraphes.
 * Supprime silencieusement les blocs JSON et code.
 */
public class MarkdownRenderer {

    /** Couleur texte principal */
    private static final String COLOR_TEXT   = "#1a2340";
    /** Couleur titre */
    private static final String COLOR_TITLE  = "#102c59";
    /** Couleur bullet */
    private static final String COLOR_BULLET = "#3b82f6";
    /** Couleur numéro liste */
    private static final String COLOR_NUMBER = "#6366f1";

    /**
     * Rend le texte markdown dans le conteneur cible.
     * Les blocs ```json``` sont ignorés (pas affichés).
     * Les autres blocs ``` sont affichés comme info-box légère.
     */
    public static void render(String markdown, Pane target) {
        if (markdown == null || markdown.isBlank()) return;

        // Séparer les blocs de code (``` ... ```)
        String[] parts = markdown.split("```");
        for (int i = 0; i < parts.length; i++) {
            if (i % 2 == 0) {
                // Texte normal → parser ligne par ligne
                renderTextBlock(parts[i], target);
            } else {
                // Bloc de code
                String block = parts[i].trim();
                // Ignorer silencieusement les blocs JSON
                if (block.toLowerCase().startsWith("json")) {
                    continue; // Ne pas afficher le JSON brut
                }
                // Autres blocs de code → info-box minimaliste
                String codeContent = block;
                // Retirer l'étiquette de langage (python, java, etc.)
                if (codeContent.contains("\n")) {
                    String firstLine = codeContent.substring(0, codeContent.indexOf("\n")).trim();
                    if (!firstLine.contains(" ") && firstLine.length() < 15) {
                        codeContent = codeContent.substring(codeContent.indexOf("\n") + 1);
                    }
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

    /**
     * Parse un bloc de texte markdown (pas de code) ligne par ligne.
     */
    private static void renderTextBlock(String block, Pane target) {
        if (block.isBlank()) return;

        String[] lines = block.split("\n");
        StringBuilder paragraph = new StringBuilder();

        for (String rawLine : lines) {
            String line = rawLine.stripTrailing();

            // Titre H1 (# ou ## ou ###)
            if (line.startsWith("### ") || line.startsWith("## ") || line.startsWith("# ")) {
                // Vider le paragraphe en cours
                flushParagraph(paragraph, target);
                String titleText = line.replaceFirst("^#{1,3}\\s+", "").trim();
                titleText = stripBold(titleText);
                Label lbl = new Label(titleText);
                int level = line.startsWith("### ") ? 3 : line.startsWith("## ") ? 2 : 1;
                double size = level == 1 ? 16.0 : level == 2 ? 14.5 : 13.5;
                lbl.setStyle("-fx-font-size: " + size + "px; -fx-font-weight: bold; "
                        + "-fx-text-fill: " + COLOR_TITLE + "; -fx-padding: 4 0 2 0;");
                lbl.setWrapText(true);
                target.getChildren().add(lbl);
                continue;
            }

            // Ligne de séparation ---
            if (line.matches("^[-*_]{3,}$")) {
                flushParagraph(paragraph, target);
                javafx.scene.control.Separator sep = new javafx.scene.control.Separator();
                sep.setStyle("-fx-padding: 4 0;");
                target.getChildren().add(sep);
                continue;
            }

            // Puce - item ou • item ou * item
            if (line.matches("^\\s*[-•*]\\s+.+")) {
                flushParagraph(paragraph, target);
                String content = line.replaceFirst("^\\s*[-•*]\\s+", "").trim();
                content = stripBold(content);
                renderBullet(content, target, false);
                continue;
            }

            // Liste numérotée 1. / 2. etc.
            if (line.matches("^\\s*\\d+\\.\\s+.+")) {
                flushParagraph(paragraph, target);
                String num = line.replaceFirst("^\\s*(\\d+)\\..*", "$1");
                String content = line.replaceFirst("^\\s*\\d+\\.\\s+", "").trim();
                content = stripBold(content);
                renderBullet(num + ". " + content, target, true);
                continue;
            }

            // Ligne vide → séparer paragraphes
            if (line.isBlank()) {
                flushParagraph(paragraph, target);
                continue;
            }

            // Ligne normale → accumuler dans le paragraphe
            if (paragraph.length() > 0) {
                paragraph.append(" ");
            }
            paragraph.append(line.trim());
        }

        flushParagraph(paragraph, target);
    }

    /**
     * Vide le buffer de paragraphe en créant un Label stylé.
     */
    private static void flushParagraph(StringBuilder buf, Pane target) {
        if (buf.length() == 0) return;
        String text = stripBold(buf.toString().trim());
        if (!text.isBlank()) {
            Label lbl = new Label(text);
            lbl.setWrapText(true);
            lbl.setStyle("-fx-font-size: 13.5px; -fx-text-fill: " + COLOR_TEXT
                    + "; -fx-line-spacing: 2; -fx-padding: 1 0;");
            target.getChildren().add(lbl);
        }
        buf.setLength(0);
    }

    /**
     * Affiche une ligne de liste (puce ou numéro).
     */
    private static void renderBullet(String content, Pane target, boolean numbered) {
        HBox row = new HBox(6);
        row.setStyle("-fx-padding: 1 0 1 8;");
        row.setAlignment(javafx.geometry.Pos.TOP_LEFT);

        Label bullet = new Label(numbered ? "" : "•");
        bullet.setStyle("-fx-font-size: 13px; -fx-text-fill: "
                + (numbered ? COLOR_NUMBER : COLOR_BULLET) + "; -fx-font-weight: bold; "
                + "-fx-min-width: 18;");

        Label text = new Label(content);
        text.setWrapText(true);
        text.setStyle("-fx-font-size: 13px; -fx-text-fill: " + COLOR_TEXT + ";");
        HBox.setHgrow(text, javafx.scene.layout.Priority.ALWAYS);

        row.getChildren().addAll(bullet, text);
        target.getChildren().add(row);
    }

    /**
     * Supprime les marqueurs **gras** et _italique_ et retourne le texte pur.
     * (JavaFX Label ne supporte pas le rich-text inline sans TextFlow complexe.)
     */
    public static String stripBold(String text) {
        if (text == null) return "";
        // Supprimer **texte** et __texte__ et *texte* et _texte_
        return text
                .replaceAll("\\*\\*(.+?)\\*\\*", "$1")
                .replaceAll("__(.+?)__", "$1")
                .replaceAll("\\*(.+?)\\*", "$1")
                .replaceAll("_(.+?)_", "$1")
                .replaceAll("`(.+?)`", "$1"); // inline code
    }
}
