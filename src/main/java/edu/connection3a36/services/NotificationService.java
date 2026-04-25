package edu.connection3a36.services;

import edu.mentorai.entities.Etat;
import edu.mentorai.entities.Objectif;
import edu.mentorai.entities.Tache;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Service de notifications deadline.
 * L'envoi email est automatique au chargement de la liste des objectifs
 * si la deadline approche (≤ 3 jours) et des tâches sont non terminées.
 */
public class NotificationService {

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int    SMTP_PORT = 587;
    private static String SMTP_USER = "";
    private static String SMTP_PASS = "";

    static {
        try (java.io.InputStream is = new java.io.FileInputStream("config.properties")) {
            Properties props = new Properties();
            props.load(is);
            if (props.getProperty("SMTP_USER") != null) SMTP_USER = props.getProperty("SMTP_USER");
            if (props.getProperty("SMTP_PASS") != null) SMTP_PASS = props.getProperty("SMTP_PASS");
        } catch (Exception ignored) {}
    }

    private final TacheService tacheService = new TacheService();

    // ─────────────────────────────────────────────────────────────────────────
    // VÉRIFICATION DEADLINE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Retourne un message d'alerte si la deadline est dans ≤ 3 jours
     * ET qu'il reste des tâches non terminées. Sinon null.
     */
    public String verifierDeadline(Objectif objectif, int programmeId) {
        if (objectif.getDatefin() == null) return null;
        long joursRestants = ChronoUnit.DAYS.between(LocalDate.now(), objectif.getDatefin());
        if (joursRestants > 3 || joursRestants < 0) return null;
        try {
            List<Tache> taches = tacheService.getByProgramme(programmeId);
            long nonTerminees = taches.stream().filter(t -> t.getEtat() != Etat.realisee).count();
            if (nonTerminees == 0) return null;
            if (joursRestants == 0)
                return "URGENT : La deadline de \"" + objectif.getTitre()
                        + "\" est aujourd'hui ! " + nonTerminees + " tache(s) non terminee(s).";
            return "Il reste " + joursRestants + " jour(s) avant la deadline de \""
                    + objectif.getTitre() + "\". " + nonTerminees + " tache(s) non terminee(s).";
        } catch (Exception e) { return null; }
    }

    /**
     * Vérifie toutes les deadlines et retourne les messages d'alerte.
     */
    public List<String> verifierToutesDeadlines(List<Objectif> objectifs) {
        List<String> alertes = new ArrayList<>();
        for (Objectif o : objectifs) {
            try {
                if (o.getProgramme() == null) continue;
                String alerte = verifierDeadline(o, o.getProgramme().getId());
                if (alerte != null) alertes.add(alerte);
            } catch (Exception ignored) {}
        }
        return alertes;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ENVOI EMAIL AUTOMATIQUE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Vérifie toutes les deadlines et envoie automatiquement un email
     * à l'utilisateur pour chaque objectif en alerte.
     * Appelé en arrière-plan au chargement de la liste des objectifs.
     *
     * @param emailUtilisateur Email de l'utilisateur connecté
     * @param objectifs        Liste des objectifs de l'utilisateur
     */
    public void envoyerAlerteAutomatique(String emailUtilisateur, List<Objectif> objectifs) {
        if (emailUtilisateur == null || emailUtilisateur.isBlank()) return;
        if (SMTP_USER.isBlank() || SMTP_PASS.isBlank()) {
            System.out.println("⚠️ SMTP non configuré — alertes email désactivées.");
            return;
        }

        Thread thread = new Thread(() -> {
            for (Objectif o : objectifs) {
                try {
                    if (o.getProgramme() == null) continue;
                    String alerte = verifierDeadline(o, o.getProgramme().getId());
                    if (alerte != null) {
                        envoyerEmail(emailUtilisateur, o, alerte);
                    }
                } catch (Exception e) {
                    System.err.println("⚠️ Erreur envoi email alerte : " + e.getMessage());
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Envoie un email de notification deadline via SMTP Gmail.
     */
    public void envoyerEmail(String destinataire, Objectif objectif, String message) throws Exception {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", String.valueOf(SMTP_PORT));
        props.put("mail.smtp.ssl.trust", SMTP_HOST);

        javax.mail.Session session = javax.mail.Session.getInstance(props, new javax.mail.Authenticator() {
            @Override
            protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                return new javax.mail.PasswordAuthentication(SMTP_USER, SMTP_PASS);
            }
        });

        javax.mail.Message mail = new javax.mail.internet.MimeMessage(session);
        mail.setFrom(new javax.mail.internet.InternetAddress(SMTP_USER, "MentorAI — Notifications"));
        mail.setRecipients(javax.mail.Message.RecipientType.TO,
                javax.mail.internet.InternetAddress.parse(destinataire));
        mail.setSubject("⏰ Rappel deadline : " + objectif.getTitre());

        String corps = "Bonjour,\n\n"
                + message + "\n\n"
                + "Objectif : " + objectif.getTitre() + "\n"
                + "Deadline : " + (objectif.getDatefin() != null ? objectif.getDatefin() : "Non definie") + "\n\n"
                + "Connectez-vous a MentorAI pour mettre a jour vos taches.\n\n"
                + "— L'equipe MentorAI";

        mail.setText(corps);
        javax.mail.Transport.send(mail);
        System.out.println("✅ Email alerte envoyé à " + destinataire + " pour : " + objectif.getTitre());
    }
}
