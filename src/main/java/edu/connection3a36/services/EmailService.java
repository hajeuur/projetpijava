package edu.connection3a36.services;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;

/**
 * Service d'envoi d'emails via l'API Mailjet (v3.1).
 * Clés configurées dans config.properties : MAILJET_API_KEY, MAILJET_SECRET_KEY, MAILJET_FROM_EMAIL, MAILJET_FROM_NAME
 */
public class EmailService {

    private static final String MAILJET_URL = "https://api.mailjet.com/v3.1/send";

    private static String apiKey;
    private static String secretKey;
    private static String fromEmail;
    private static String fromName;

    static {
        try (java.io.InputStream is = new java.io.FileInputStream("config.properties")) {
            Properties props = new Properties();
            props.load(is);
            apiKey    = props.getProperty("MAILJET_API_KEY", "");
            secretKey = props.getProperty("MAILJET_SECRET_KEY", "");
            fromEmail = props.getProperty("MAILJET_FROM_EMAIL", "noreply@mentorAI.com");
            fromName  = props.getProperty("MAILJET_FROM_NAME", "MentorAI");
        } catch (Exception e) {
            System.err.println("❌ EmailService: erreur lecture config.properties: " + e.getMessage());
        }
    }

    /**
     * Envoie un email simple via Mailjet.
     *
     * @param toEmail   adresse destinataire
     * @param toName    nom destinataire
     * @param subject   sujet
     * @param htmlBody  corps HTML
     * @throws Exception si l'envoi échoue
     */
    public void sendEmail(String toEmail, String toName, String subject, String htmlBody) throws Exception {
        if (apiKey == null || apiKey.isBlank() || secretKey == null || secretKey.isBlank()) {
            throw new Exception("Clés API Mailjet non configurées dans config.properties.");
        }

        String credentials = Base64.getEncoder().encodeToString((apiKey + ":" + secretKey).getBytes(StandardCharsets.UTF_8));

        String payload = """
                {
                  "Messages": [
                    {
                      "From": { "Email": "%s", "Name": "%s" },
                      "To":   [ { "Email": "%s", "Name": "%s" } ],
                      "Subject": "%s",
                      "HTMLPart": "%s"
                    }
                  ]
                }
                """.formatted(
                escape(fromEmail), escape(fromName),
                escape(toEmail),   escape(toName),
                escape(subject),
                escapeHtml(htmlBody)
        );

        URL url = new URL(MAILJET_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Basic " + credentials);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        if (status != 200) {
            InputStream err = conn.getErrorStream();
            String body = err != null ? new String(err.readAllBytes(), StandardCharsets.UTF_8) : "no body";
            throw new Exception("Mailjet erreur " + status + ": " + body);
        }
    }

    // ── Templates prêts à l'emploi ────────────────────────────────────────────

    /** Email d'alerte étudiant à risque. */
    public void sendAtRiskAlert(String toEmail, String toName, String studentName, String analysisText) throws Exception {
        String subject = "🚨 Alerte : Étudiant à risque — " + studentName;
        String html = buildHtml(
            "🚨 Alerte Étudiant à Risque",
            "Bonjour,",
            "<p>L'analyse IA a détecté que l'étudiant <strong>" + studentName + "</strong> est en situation de risque pédagogique.</p>"
            + "<h3>Résumé de l'analyse :</h3>"
            + "<pre style='background:#f4f4f4;padding:12px;border-radius:6px;font-size:13px;'>" + htmlEncode(analysisText) + "</pre>"
            + "<p>Veuillez consulter la plateforme MentorAI pour créer un plan de remédiation.</p>"
        );
        sendEmail(toEmail, toName, subject, html);
    }

    /** Email de notification de plan d'action créé. */
    public void sendPlanCreatedNotification(String toEmail, String toName, String planDecision, String planDescription) throws Exception {
        String subject = "📋 Nouveau Plan d'Action : " + planDecision;
        String html = buildHtml(
            "📋 Plan d'Action Créé",
            "Bonjour " + toName + ",",
            "<p>Un nouveau plan d'action a été créé sur la plateforme MentorAI.</p>"
            + "<h3>Décision :</h3><p><strong>" + htmlEncode(planDecision) + "</strong></p>"
            + "<h3>Description :</h3>"
            + "<pre style='background:#f4f4f4;padding:12px;border-radius:6px;font-size:13px;'>" + htmlEncode(planDescription) + "</pre>"
            + "<p>Connectez-vous à MentorAI pour suivre l'avancement de ce plan.</p>"
        );
        sendEmail(toEmail, toName, subject, html);
    }

    /** Email de notification générique. */
    public void sendNotification(String toEmail, String toName, String titre, String message) throws Exception {
        String subject = "🔔 Notification MentorAI : " + titre;
        String html = buildHtml(
            "🔔 " + titre,
            "Bonjour " + toName + ",",
            "<p>" + htmlEncode(message) + "</p>"
        );
        sendEmail(toEmail, toName, subject, html);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String buildHtml(String title, String greeting, String bodyContent) {
        return """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;border:1px solid #e0e0e0;border-radius:10px;overflow:hidden;">
                  <div style="background:#102c59;padding:20px;text-align:center;">
                    <h1 style="color:white;margin:0;font-size:20px;">%s</h1>
                  </div>
                  <div style="padding:24px;color:#333;">
                    <p>%s</p>
                    %s
                    <hr style="border:none;border-top:1px solid #eee;margin:20px 0;"/>
                    <p style="font-size:12px;color:#999;">Cet email a été envoyé automatiquement par MentorAI. Ne pas répondre.</p>
                  </div>
                </div>
                """.formatted(title, greeting, bodyContent);
    }

    /** Échappe les guillemets pour JSON inline. */
    private String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /** Échappe le HTML pour JSON inline (remplace les sauts de ligne par <br>). */
    private String escapeHtml(String html) {
        return html == null ? "" : html
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    /** Encode les caractères HTML spéciaux dans le contenu. */
    private String htmlEncode(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
