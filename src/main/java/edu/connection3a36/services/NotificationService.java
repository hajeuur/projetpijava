package edu.connection3a36.services;

import edu.connection3a36.entities.Etat;
import edu.connection3a36.entities.Objectif;
import edu.connection3a36.entities.Tache;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * SERVICE : NotificationService — FONCTIONNALITÉ 1
 * ============================================================
 * Génère des alertes de deadline pour les objectifs dont la date
 * limite approche et qui ont encore des tâches non terminées.
 *
 * RÈGLE MÉTIER :
 * Une alerte est générée si :
 * 1. La deadline est dans ≤ 3 jours (et pas encore passée)
 * 2. ET il reste au moins une tâche non réalisée
 *
 * AFFICHAGE DANS L'INTERFACE :
 * - Bandeau jaune en haut de ObjectifList (lblAlertes)
 * - Bandeau jaune en haut de ProgrammeDetail (lblAlerte)
 *
 * MESSAGES GÉNÉRÉS :
 * - Deadline aujourd'hui : "URGENT : La deadline de "X" est aujourd'hui ! N tache(s) non terminee(s)."
 * - Deadline dans N jours : "Il reste N jour(s) avant la deadline de "X". N tache(s) non terminee(s)."
 *
 * DIFFÉRENCE AVEC RisqueAbandonService :
 * - NotificationService → alerte sur la DEADLINE (temps restant)
 * - RisqueAbandonService → alerte sur les TÂCHES ABANDONNÉES (risque IA)
 * ============================================================
 */
public class NotificationService {

    /** Service pour récupérer les tâches d'un programme */
    private final TacheService tacheService = new TacheService();

    // ─────────────────────────────────────────────────────────────────────────
    // VÉRIFICATION D'UN OBJECTIF
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Vérifie si un objectif nécessite une alerte de deadline.
     *
     * CONDITIONS POUR GÉNÉRER UNE ALERTE :
     * 1. L'objectif a une date de fin définie
     * 2. La deadline est dans 0 à 3 jours (pas encore passée)
     * 3. Il reste au moins une tâche non réalisée
     *
     * @param objectif    L'objectif à vérifier
     * @param programmeId L'ID du programme lié (pour récupérer les tâches)
     * @return Le message d'alerte, ou null si pas d'alerte nécessaire
     */
    public String verifierDeadline(Objectif objectif, int programmeId) {
        // Pas de deadline définie → pas d'alerte
        if (objectif.getDatefin() == null) return null;

        // Calculer le nombre de jours restants
        long joursRestants = ChronoUnit.DAYS.between(LocalDate.now(), objectif.getDatefin());

        // Deadline déjà passée (< 0) ou trop loin (> 3 jours) → pas d'alerte
        if (joursRestants > 3 || joursRestants < 0) return null;

        try {
            // Récupérer les tâches du programme
            List<Tache> taches = tacheService.getByProgramme(programmeId);

            // Compter les tâches non terminées (encours + abandonnées)
            long nonTerminees = taches.stream()
                    .filter(t -> t.getEtat() != Etat.realisee)
                    .count();

            // Toutes les tâches sont terminées → pas d'alerte
            if (nonTerminees == 0) return null;

            // Générer le message selon l'urgence
            if (joursRestants == 0) {
                // Deadline aujourd'hui → message URGENT
                return "URGENT : La deadline de \"" + objectif.getTitre()
                        + "\" est aujourd'hui ! " + nonTerminees + " tache(s) non terminee(s).";
            }

            // Deadline dans N jours
            return "Il reste " + joursRestants + " jour(s) avant la deadline de \""
                    + objectif.getTitre() + "\". " + nonTerminees + " tache(s) non terminee(s).";

        } catch (Exception e) {
            return null; // En cas d'erreur → pas d'alerte (ne pas bloquer l'UI)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VÉRIFICATION DE TOUS LES OBJECTIFS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Vérifie toutes les deadlines d'une liste d'objectifs et retourne
     * tous les messages d'alerte.
     * Utilisé dans ObjectifListController pour afficher le bandeau global.
     *
     * @param objectifs La liste des objectifs à vérifier
     * @return Liste des messages d'alerte (peut être vide si tout va bien)
     */
    public List<String> verifierToutesDeadlines(List<Objectif> objectifs) {
        List<String> alertes = new ArrayList<>();

        for (Objectif o : objectifs) {
            try {
                // Ignorer les objectifs sans programme lié
                if (o.getProgramme() == null) continue;

                // Vérifier la deadline de cet objectif
                String alerte = verifierDeadline(o, o.getProgramme().getId());

                // Ajouter l'alerte si elle existe
                if (alerte != null) alertes.add(alerte);

            } catch (Exception ignored) {
                // Ignorer les erreurs individuelles
            }
        }

        return alertes;
    }
}
