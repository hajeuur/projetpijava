package edu.connection3a36.services;

import edu.connection3a36.entities.Etat;
import edu.connection3a36.entities.Objectif;
import edu.connection3a36.entities.Tache;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Service d'alertes deadline pour les objectifs personnels.
 * Renommé DeadlineNotificationService pour coexister avec
 * NotificationService (notifications système admin des collègues).
 *
 * Génère des alertes quand la deadline d'un objectif est dans ≤ 3 jours
 * ET qu'il reste des tâches non terminées.
 */
public class DeadlineNotificationService {

    private final TacheService tacheService = new TacheService();

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
}
