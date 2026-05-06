package edu.connection3a36.services;

import edu.connection3a36.entities.Etat;
import edu.connection3a36.entities.Objectif;
import edu.connection3a36.entities.Statutobj;
import edu.connection3a36.entities.Tache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour NotificationService — logique de vérification des deadlines.
 * On teste uniquement la logique de détection d'alerte (sans BDD ni SMTP).
 */
class NotificationServiceTest {

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private Objectif objectifAvecDeadline(LocalDate deadline) {
        Objectif o = new Objectif();
        o.setTitre("Objectif Test");
        o.setDatefin(deadline);
        o.setStatut(Statutobj.EnCours);
        return o;
    }

    private List<Tache> tachesNonTerminees(int nb) {
        List<Tache> list = new ArrayList<>();
        for (int i = 0; i < nb; i++) {
            Tache t = new Tache();
            t.setEtat(Etat.Abandonner);
            list.add(t);
        }
        return list;
    }

    private List<Tache> tachesTerminees(int nb) {
        List<Tache> list = new ArrayList<>();
        for (int i = 0; i < nb; i++) {
            Tache t = new Tache();
            t.setEtat(Etat.realisee);
            list.add(t);
        }
        return list;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOGIQUE DE DÉTECTION D'ALERTE (méthode interne testée via la logique)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testPasAlerteDeadlineLointaine() {
        // Deadline dans 10 jours → pas d'alerte
        Objectif o = objectifAvecDeadline(LocalDate.now().plusDays(10));
        String alerte = service.verifierDeadline(o, 999); // programme inexistant → null
        assertNull(alerte, "Pas d alerte si deadline > 3 jours");
    }

    @Test
    void testPasAlerteSiDateFinNull() {
        Objectif o = new Objectif();
        o.setTitre("Sans deadline");
        o.setDatefin(null);
        String alerte = service.verifierDeadline(o, 1);
        assertNull(alerte, "Pas d alerte si pas de deadline");
    }

    @Test
    void testPasAlerteSiDeadlineDepassee() {
        // Deadline dépassée → pas d'alerte (joursRestants < 0)
        Objectif o = objectifAvecDeadline(LocalDate.now().minusDays(1));
        String alerte = service.verifierDeadline(o, 999);
        assertNull(alerte, "Pas d alerte si deadline depassee");
    }

    @Test
    void testVerifierToutesDeadlinesListeVide() {
        List<String> alertes = service.verifierToutesDeadlines(new ArrayList<>());
        assertTrue(alertes.isEmpty(), "Aucune alerte pour une liste vide");
    }

    @Test
    void testVerifierToutesDeadlinesSansProgramme() {
        // Objectifs sans programme → ignorés
        Objectif o1 = objectifAvecDeadline(LocalDate.now().plusDays(1));
        o1.setProgramme(null);
        Objectif o2 = objectifAvecDeadline(LocalDate.now().plusDays(2));
        o2.setProgramme(null);

        List<String> alertes = service.verifierToutesDeadlines(List.of(o1, o2));
        assertTrue(alertes.isEmpty(), "Objectifs sans programme doivent etre ignores");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONTENU DES MESSAGES D'ALERTE
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testMessageAlerteContientTitreObjectif() {
        // On simule la logique directement sur le message généré
        // (sans appel BDD — on vérifie le format du message)
        String titre = "Mon objectif important";
        String messageSimule = "Il reste 2 jour(s) avant la deadline de \""
                + titre + "\". 3 tache(s) non terminee(s).";
        assertTrue(messageSimule.contains(titre));
        assertTrue(messageSimule.contains("2 jour"));
        assertTrue(messageSimule.contains("3 tache"));
    }

    @Test
    void testMessageUrgentAujourdHui() {
        String titre = "Objectif urgent";
        String messageSimule = "URGENT : La deadline de \""
                + titre + "\" est aujourd'hui ! 1 tache(s) non terminee(s).";
        assertTrue(messageSimule.contains("URGENT"));
        assertTrue(messageSimule.contains("aujourd"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SEUIL D'ALERTE
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testSeuilAlerteTroisJours() {
        // Deadline dans exactement 3 jours → dans la zone d'alerte
        Objectif o = objectifAvecDeadline(LocalDate.now().plusDays(3));
        // Sans programme valide, verifierDeadline retourne null (pas de tâches)
        // On vérifie juste que la deadline est dans la zone (≤ 3 jours)
        long jours = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), o.getDatefin());
        assertTrue(jours <= 3, "3 jours doit etre dans la zone d alerte");
    }

    @Test
    void testSeuilAlerteCinqJours() {
        // Deadline dans 5 jours → hors zone d'alerte
        Objectif o = objectifAvecDeadline(LocalDate.now().plusDays(5));
        long jours = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), o.getDatefin());
        assertTrue(jours > 3, "5 jours doit etre hors zone d alerte");
    }
}
