package edu.connection3a36.services;

import edu.connection3a36.entities.Etat;
import edu.connection3a36.entities.Objectif;
import edu.connection3a36.entities.Tache;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Service de notifications deadline — uniquement dans l'application.
 * Affiche des bandeaux d'alerte dans l'UI quand la deadline approche.
 */
public class NotificationService {

    private final TacheService tacheService = new TacheService();

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
}