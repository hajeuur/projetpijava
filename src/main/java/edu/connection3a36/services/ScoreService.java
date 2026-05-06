package edu.connection3a36.services;

import edu.connection3a36.entities.Etat;
import edu.connection3a36.entities.Medaille;
import edu.connection3a36.entities.Statutobj;
import edu.connection3a36.entities.Tache;

import java.util.List;

/**
 * ============================================================
 * SERVICE : ScoreService
 * ============================================================
 * Cœur du système de gamification.
 * Calcule le score de progression, attribue les médailles et
 * met à jour automatiquement le statut de l'objectif.
 *
 * CE SERVICE EST APPELÉ À CHAQUE FOIS QU'UNE TÂCHE CHANGE D'ÉTAT.
 * Flux complet déclenché par changerEtat() dans ProgrammeDetailController :
 *
 * 1. L'utilisateur change l'état d'une tâche (ex: encours → realisee)
 * 2. TacheService.updateEtat() sauvegarde en BDD
 * 3. ScoreService.recalculerEtSauvegarder() est appelé :
 *    a. Récupère toutes les tâches du programme
 *    b. Calcule le nouveau score (% tâches réalisées)
 *    c. Détermine la médaille correspondante
 *    d. Sauvegarde score + médaille dans la table programme
 *    e. Calcule le nouveau statut de l'objectif
 *    f. Met à jour le statut dans la table objectif
 * 4. L'interface se rafraîchit (rafraichirScore())
 *
 * FORMULE DU SCORE :
 * score = (nombre de tâches "realisee" / nombre total de tâches) × 100
 *
 * RÈGLES MÉDAILLE :
 * - score = 0        → null (aucune médaille)
 * - 1 ≤ score < 50   → 🥉 Bronze
 * - 50 ≤ score < 80  → 🥈 Argent
 * - score ≥ 80       → 🥇 Or
 *
 * RÈGLES STATUT OBJECTIF :
 * - score = 0   → Abandonner
 * - score = 100 → Atteint
 * - sinon       → EnCours
 * ============================================================
 */
public class ScoreService {

    /** Services utilisés pour accéder aux données */
    private final TacheService tacheService = new TacheService();
    private final ProgrammeService programmeService = new ProgrammeService();
    private final ObjectifService objectifService = new ObjectifService();

    // ─────────────────────────────────────────────────────────────────────────
    // MÉTHODE PRINCIPALE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Recalcule le score du programme ET met à jour le statut de l'objectif lié.
     * C'est la méthode centrale appelée après chaque changement d'état de tâche.
     *
     * @param programmeId L'ID du programme à recalculer
     * @return Le nouveau score en pourcentage (0 à 100)
     * @throws Exception En cas d'erreur d'accès à la BDD
     */
    public int recalculerEtSauvegarder(int programmeId) throws Exception {
        // 1. Récupérer toutes les tâches du programme
        List<Tache> taches = tacheService.getByProgramme(programmeId);

        // 2. Calculer le score
        int score = calculerScore(taches);

        // 3. Déterminer la médaille
        Medaille medaille = attribuerMedaille(score);

        // 4. Sauvegarder score + médaille dans la table programme
        programmeService.updateScore(programmeId, score,
                medaille != null ? medaille.getValue() : null);

        // 5. Mettre à jour le statut de l'objectif lié
        mettreAJourStatutObjectif(programmeId, taches, score);

        return score;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MISE À JOUR DU STATUT OBJECTIF
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Détermine et sauvegarde le statut de l'objectif selon l'avancement des tâches.
     * Cette méthode est privée car elle est toujours appelée depuis recalculerEtSauvegarder().
     *
     * OPTIMISATION : Le statut n'est mis à jour en BDD que s'il a réellement changé.
     *
     * @param programmeId L'ID du programme
     * @param taches      La liste des tâches (déjà chargée)
     * @param score       Le score calculé
     */
    private void mettreAJourStatutObjectif(int programmeId, List<Tache> taches, int score) {
        try {
            // Trouver l'objectif lié à ce programme (relation inverse)
            edu.connection3a36.entities.Objectif objectif =
                    objectifService.getByProgrammeId(programmeId);
            if (objectif == null) return; // Pas d'objectif lié → rien à faire

            // Calculer le nouveau statut selon les règles métier
            Statutobj nouveauStatut = calculerStatutObjectif(taches, score);

            // Mettre à jour SEULEMENT si le statut a changé (optimisation BDD)
            if (objectif.getStatut() != nouveauStatut) {
                objectif.setStatut(nouveauStatut);
                objectifService.updateEntity(objectif.getId(), objectif);
                System.out.println("✅ Statut objectif mis à jour : " + nouveauStatut.getValue());
            }
        } catch (Exception e) {
            // Ne pas bloquer l'application si la mise à jour du statut échoue
            System.err.println("⚠️ Erreur mise à jour statut objectif : " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CALCULS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Calcule le statut de l'objectif selon les règles métier.
     *
     * RÈGLES :
     * - Aucune tâche ou score = 0  → Abandonner (rien n'a été fait)
     * - Score = 100%               → Atteint (tout est réalisé)
     * - Sinon                      → EnCours (en progression)
     *
     * @param taches La liste des tâches du programme
     * @param score  Le score calculé (0 à 100)
     * @return Le statut correspondant
     */
    public Statutobj calculerStatutObjectif(List<Tache> taches, int score) {
        if (taches == null || taches.isEmpty()) return Statutobj.EnCours; // Pas encore de tâches
        if (score == 0)   return Statutobj.Abandonner; // Aucune tâche réalisée
        if (score == 100) return Statutobj.Atteint;    // Toutes les tâches réalisées
        return Statutobj.EnCours;                       // En progression
    }

    /**
     * Calcule le score de progression en pourcentage.
     *
     * FORMULE : score = (nb tâches "realisee" / nb total tâches) × 100
     * Arrondi à l'entier le plus proche (Math.round).
     *
     * Exemples :
     * - 0 tâche réalisée sur 5  → 0%
     * - 1 tâche réalisée sur 5  → 20%
     * - 3 tâches réalisées sur 5 → 60%
     * - 5 tâches réalisées sur 5 → 100%
     *
     * @param taches La liste de toutes les tâches du programme
     * @return Le score en pourcentage (0 à 100)
     */
    public int calculerScore(List<Tache> taches) {
        if (taches == null || taches.isEmpty()) return 0;

        // Compter uniquement les tâches avec l'état "realisee"
        long realisees = taches.stream()
                .filter(t -> t.getEtat() == Etat.realisee)
                .count();

        // Calcul du pourcentage avec arrondi
        return (int) Math.round((double) realisees / taches.size() * 100);
    }

    /**
     * Détermine la médaille à attribuer selon le score.
     *
     * RÈGLES :
     * - score = 0        → null (pas de médaille, rien n'a été fait)
     * - 1 ≤ score < 50   → 🥉 Bronze
     * - 50 ≤ score < 80  → 🥈 Argent
     * - score ≥ 80       → 🥇 Or
     *
     * @param score Le score en pourcentage (0 à 100)
     * @return La médaille correspondante, ou null si score = 0
     */
    public Medaille attribuerMedaille(int score) {
        if (score <= 0)  return null;           // Aucune médaille
        if (score < 50)  return Medaille.Bronze; // 🥉 Moins de la moitié
        if (score < 80)  return Medaille.Argent; // 🥈 Bonne progression
        return Medaille.Or;                      // 🥇 Excellente progression
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS D'AFFICHAGE (utilisés dans les contrôleurs)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Retourne l'emoji et le nom de la médaille pour l'affichage.
     * Utilisé dans ObjectifListController et ProgrammeDetailController.
     *
     * @param m La médaille (peut être null)
     * @return "🥉 Bronze", "🥈 Argent", "🥇 Or", ou "—" si null
     */
    public static String emojiMedaille(Medaille m) {
        if (m == null) return "—";
        return switch (m) {
            case Bronze -> "🥉 Bronze";
            case Argent -> "🥈 Argent";
            case Or     -> "🥇 Or";
        };
    }

    /**
     * Retourne la couleur CSS correspondant à la médaille.
     * Utilisée pour coloriser la barre de progression.
     *
     * @param m La médaille (peut être null)
     * @return Code couleur CSS hexadécimal
     */
    public static String couleurMedaille(Medaille m) {
        if (m == null) return "#8e92a9"; // Gris par défaut (aucune médaille)
        return switch (m) {
            case Bronze -> "#cd7f32"; // Marron bronze
            case Argent -> "#a8a9ad"; // Gris argent
            case Or     -> "#ffd700"; // Jaune or
        };
    }
}
