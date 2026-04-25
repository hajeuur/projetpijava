package com.mentorai.services;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

public class EmailService {

    private static final String EMAIL_ADMIN  = "amal.mokdad07@gmail.com";
    private static final String APP_PASSWORD = "qxxt nqaa rmvt yext";

    public boolean envoyerEmailTraitement(
            String emailDestinataire,
            String nomUtilisateur,
            String typeFeedback,
            String messageOriginal,
            String typeTraitement,
            String reponseAdmin) {

        try {
            // ===== CONFIGURATION SMTP GMAIL =====
            Properties props = new Properties();
            props.put("mail.smtp.auth",            "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host",            "smtp.gmail.com");
            props.put("mail.smtp.port",            "587");
            props.put("mail.smtp.ssl.protocols",   "TLSv1.2");

            // ===== SESSION =====
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(EMAIL_ADMIN, APP_PASSWORD);
                }
            });

            // ===== CONTENU EMAIL =====
            String sujet = "MentorAI — Votre feedback a été traité";

            String contenu =
                    "<html><body style='font-family: Arial, sans-serif; color: #333;'>" +
                            "<div style='max-width: 600px; margin: auto; border: 1px solid #e0e0e0;" +
                            "border-radius: 8px; overflow: hidden;'>" +

                            // Header
                            "<div style='background-color: #102c59; padding: 20px; text-align: center;'>" +
                            "<h1 style='color: white; font-size: 22px; margin: 0;'>MentorAI</h1>" +
                            "<p style='color: #9dbbce; margin: 5px 0 0 0; font-size: 13px;'>" +
                            "Plateforme d'accompagnement étudiant</p>" +
                            "</div>" +

                            // Body
                            "<div style='padding: 25px;'>" +
                            "<h2 style='color: #102c59; font-size: 18px;'>Votre feedback a été traité</h2>" +
                            "<p style='font-size: 14px;'>Bonjour <strong>" + nomUtilisateur + "</strong>,</p>" +
                            "<p style='font-size: 14px;'>Nous avons bien traité votre feedback. " +
                            "Voici le résumé :</p>" +

                            // Feedback original
                            "<div style='background-color: #f8f9fa; border-left: 4px solid #9dbbce;" +
                            "padding: 12px; border-radius: 4px; margin: 15px 0;'>" +
                            "<p style='margin: 0; font-size: 13px; color: #555;'>" +
                            "<strong>Votre feedback :</strong></p>" +
                            "<p style='margin: 5px 0 0 0; font-size: 13px;'>" +
                            "Type : <strong>" + typeFeedback + "</strong></p>" +
                            "<p style='margin: 5px 0 0 0; font-size: 13px; font-style: italic;'>" +
                            messageOriginal + "</p>" +
                            "</div>" +

                            // Réponse admin
                            "<div style='background-color: #eef2ff; border-left: 4px solid #102c59;" +
                            "padding: 12px; border-radius: 4px; margin: 15px 0;'>" +
                            "<p style='margin: 0; font-size: 13px; color: #102c59;'>" +
                            "<strong>Réponse de l'administration :</strong></p>" +
                            "<p style='margin: 5px 0 0 0; font-size: 13px;'>" +
                            "Type de traitement : <strong>" + typeTraitement + "</strong></p>" +
                            "<p style='margin: 5px 0 0 0; font-size: 14px;'>" +
                            reponseAdmin + "</p>" +
                            "</div>" +

                            "<p style='font-size: 13px; color: #888; margin-top: 20px;'>" +
                            "Merci de faire confiance à MentorAI.</p>" +
                            "</div>" +

                            // Footer
                            "<div style='background-color: #f5f5f5; padding: 15px; text-align: center;'>" +
                            "<p style='margin: 0; font-size: 11px; color: #aaa;'>" +
                            "MentorAI — Ne pas répondre à cet email</p>" +
                            "</div>" +

                            "</div></body></html>";

            // ===== ENVOI =====
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_ADMIN));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(emailDestinataire));
            message.setSubject(sujet);
            message.setContent(contenu, "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("✅ Email envoyé à : " + emailDestinataire);
            return true;

        } catch (Exception e) {
            System.out.println("❌ Erreur email : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}