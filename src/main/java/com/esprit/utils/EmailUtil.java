package com.esprit.utils;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;
import java.util.UUID;

public class EmailUtil {

    private static final String EMAIL_FROM = "hejerh666@gmail.com";
    private static final String EMAIL_PASSWORD = "rjwe gouc yklq dcna";

    public static String genererToken() {
        return UUID.randomUUID().toString();
    }

    public static boolean envoyerEmailReset(String emailDestinataire, String token) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EMAIL_FROM, EMAIL_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_FROM));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailDestinataire));
            message.setSubject("MentorAI — Réinitialisation de votre mot de passe");

            String contenu = "<html><body style='font-family:Arial;'>"
                    + "<div style='max-width:500px;margin:auto;padding:30px;border:1px solid #ddd;border-radius:8px;'>"
                    + "<h2 style='color:#1a2a4a;'>MentorAI</h2>"
                    + "<p>Vous avez demandé une réinitialisation de mot de passe.</p>"
                    + "<p>Votre code de réinitialisation est :</p>"
                    + "<div style='background:#f0f4ff;padding:15px;border-radius:6px;text-align:center;'>"
                    + "<h1 style='color:#1a2a4a;letter-spacing:5px;'>" + token.substring(0, 8).toUpperCase() + "</h1>"
                    + "</div>"
                    + "<p style='color:#888;font-size:12px;'>Ce code expire dans 30 minutes.</p>"
                    + "<p style='color:#888;font-size:12px;'>Si vous n'avez pas fait cette demande, ignorez cet email.</p>"
                    + "</div></body></html>";

            message.setContent(contenu, "text/html; charset=UTF-8");
            Transport.send(message);
            return true;
        } catch (MessagingException e) {
            System.out.println("Erreur envoi email : " + e.getMessage());
            return false;
        }
    }
}