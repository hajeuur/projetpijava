package edu.connection3a36.services;

import edu.mentorai.entities.Etat;
import edu.mentorai.entities.Medaille;
import edu.mentorai.entities.Statutobj;
import edu.mentorai.entities.Tache;

import java.util.List;

public class ScoreService {

    private final TacheService tacheService = new TacheService();
    private final ProgrammeService programmeService = new ProgrammeService();
    private final ObjectifService objectifService = new ObjectifService();

    /**
     * Recalcule le score du programme ET met à jour le statut de l'objectif lié.
     *
     * Règles :
     * - Aucune tâche réalisée (score = 0)  → objectif Abandonner
     * - Toutes les tâches réalisées (100%) → objectif Atteint
     * - Sinon                              → objectif EnCours
     */
    public int recalculerEtSauvegarder(int programmeId) throws Exception {
        List<Tache> taches = tacheService.getByProgramme(programmeId);
        int score = calculerScore(taches);
        Medaille medaille = attribuerMedaille(score);
        programmeService.updateScore(programmeId, score, medaille != null ? medaille.getValue() : null);

        // Mettre à jour le statut de l'objectif lié
        mettreAJourStatutObjectif(programmeId, taches, score);

        return score;
    }

    /**
     * Détermine et sauvegarde le statut de l'objectif selon l'avancement des tâches.
     */
    private void mettreAJourStatutObjectif(int programmeId, List<Tache> taches, int score) {
        try {
            // Trouver l'objectif lié à ce programme
            edu.mentorai.entities.Objectif objectif = objectifService.getByProgrammeId(programmeId);
            if (objectif == null) return;

            Statutobj nouveauStatut = calculerStatutObjectif(taches, score);

            // Mettre à jour seulement si le statut a changé
            if (objectif.getStatut() != nouveauStatut) {
                objectif.setStatut(nouveauStatut);
                objectifService.updateEntity(objectif.getId(), objectif);
                System.out.println("✅ Statut objectif mis à jour : " + nouveauStatut.getValue());
            }
        } catch (Exception e) {
            System.err.println("⚠️ Erreur mise à jour statut objectif : " + e.getMessage());
        }
    }

    /**
     * Calcule le statut de l'objectif selon les règles métier :
     * - score = 0  → Abandonner
     * - score = 100 → Atteint
     * - sinon      → EnCours
     */
    public Statutobj calculerStatutObjectif(List<Tache> taches, int score) {
        if (taches == null || taches.isEmpty()) return Statutobj.EnCours;
        if (score == 0) return Statutobj.Abandonner;
        if (score == 100) return Statutobj.Atteint;
        return Statutobj.EnCours;
    }

    public int calculerScore(List<Tache> taches) {
        if (taches == null || taches.isEmpty()) return 0;
        long realisees = taches.stream().filter(t -> t.getEtat() == Etat.realisee).count();
        return (int) Math.round((double) realisees / taches.size() * 100);
    }

    public Medaille attribuerMedaille(int score) {
        if (score <= 0) return null;
        if (score < 50) return Medaille.Bronze;
        if (score < 80) return Medaille.Argent;
        return Medaille.Or;
    }

    public static String emojiMedaille(Medaille m) {
        if (m == null) return "—";
        return switch (m) {
            case Bronze -> "🥉 Bronze";
            case Argent -> "🥈 Argent";
            case Or     -> "🥇 Or";
        };
    }

    public static String couleurMedaille(Medaille m) {
        if (m == null) return "#8e92a9";
        return switch (m) {
            case Bronze -> "#cd7f32";
            case Argent -> "#a8a9ad";
            case Or     -> "#ffd700";
        };
    }
}
