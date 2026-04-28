package edu.connection3a36.services;

import edu.connection3a36.entities.Etat;
import edu.connection3a36.entities.Objectif;
import edu.connection3a36.entities.Tache;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * SERVICE : PlanificateurService — FONCTIONNALITÉ 6
 * ============================================================
 * Génère un plan de travail journalier intelligent basé sur
 * toutes les tâches "en cours" de l'utilisateur.
 *
 * QUAND EST-IL APPELÉ ?
 * Quand l'utilisateur clique sur "📅 Mon plan du jour" dans
 * ObjectifListController.
 *
 * FONCTIONNEMENT :
 * 1. Collecter toutes les tâches "encours" de tous les objectifs
 * 2. Pour chaque tâche, noter les jours restants avant la deadline
 * 3. Envoyer à Ollama pour générer un planning horaire optimal
 * 4. Afficher le planning sous forme de timeline dans l'interface
 *
 * EXEMPLE DE RÉSULTAT :
 * ┌──────────┬──────────────────────────────┬──────────────────┐
 * │ Heure    │ Tâche                        │ Objectif         │
 * ├──────────┼──────────────────────────────┼──────────────────┤
 * │ 9h-10h   │ Lire le chapitre 3           │ Apprendre Java   │
 * │ 10h-11h  │ Faire les exercices          │ Apprendre Java   │
 * │ 14h-15h  │ Réviser les formules         │ Préparer l'examen│
 * └──────────┴──────────────────────────────┴──────────────────┘
 *
 * CLASSES INTERNES :
 * - CreneauPlan : un créneau horaire du planning (heure + tâche + objectif)
 * - PlanResultat : le planning complet + un conseil de l'IA
 * ============================================================
 */
public class PlanificateurService {

    /** Service IA Ollama pour la génération du planning */
    private final OllamaService ollamaService = new OllamaService();

    // ─────────────────────────────────────────────────────────────────────────
    // CLASSES INTERNES
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Représente un créneau horaire dans le planning journalier.
     * Affiché sous forme de ligne dans la timeline de l'interface.
     */
    public static class CreneauPlan {
        /** Plage horaire (ex: "9h-10h", "14h-15h") */
        public final String heure;

        /** Titre de la tâche à réaliser pendant ce créneau */
        public final String tache;

        /** Titre de l'objectif auquel appartient cette tâche */
        public final String objectif;

        public CreneauPlan(String heure, String tache, String objectif) {
            this.heure    = heure;
            this.tache    = tache;
            this.objectif = objectif;
        }
    }

    /**
     * Résultat complet de la génération du planning.
     * Contient la liste des créneaux et un conseil de l'IA.
     */
    public static class PlanResultat {
        /** Liste ordonnée des créneaux horaires du planning */
        public final List<CreneauPlan> plan;

        /** Conseil de productivité généré par l'IA */
        public final String conseil;

        public PlanResultat(List<CreneauPlan> plan, String conseil) {
            this.plan    = plan;
            this.conseil = conseil;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GÉNÉRATION DU PLAN
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Génère un plan de travail journalier à partir des tâches en cours.
     * Ne propage jamais d'exception (utilise le fallback en cas d'erreur).
     *
     * ALGORITHME :
     * 1. Parcourir tous les objectifs
     * 2. Pour chaque objectif → récupérer les tâches "encours"
     * 3. Calculer les jours restants avant la deadline de l'objectif
     * 4. Construire un JSON avec toutes ces informations
     * 5. Envoyer à Ollama pour générer le planning optimal
     * 6. Parser la réponse et retourner le PlanResultat
     *
     * @param objectifs Liste des objectifs de l'utilisateur
     * @return PlanResultat avec le planning et le conseil
     */
    public PlanResultat genererPlan(List<Objectif> objectifs) {
        // Collecter toutes les tâches "encours" avec leur contexte
        List<JSONObject> tachesAvecDeadline = new ArrayList<>();

        for (Objectif o : objectifs) {
            if (o.getProgramme() == null) continue;
            try {
                TacheService ts = new TacheService();
                List<Tache> taches = ts.getByProgramme(o.getProgramme().getId());

                for (Tache t : taches) {
                    // Ne prendre que les tâches "en cours" (pas réalisées ni abandonnées)
                    if (t.getEtat() == Etat.encours) {
                        JSONObject obj = new JSONObject();
                        obj.put("tache", t.getTitre());
                        obj.put("objectif", o.getTitre());

                        // Calculer les jours restants (30 par défaut si pas de deadline)
                        long jours = o.getDatefin() != null
                                ? ChronoUnit.DAYS.between(LocalDate.now(), o.getDatefin()) : 30;
                        obj.put("jours_restants", jours);

                        tachesAvecDeadline.add(obj);
                    }
                }
            } catch (Exception ignored) {}
        }

        // Aucune tâche en cours → message vide
        if (tachesAvecDeadline.isEmpty()) {
            return fallbackVide();
        }

        // Construire le prompt pour Ollama
        String tachesJson = new JSONArray(tachesAvecDeadline).toString();
        String prompt = """
                Voici toutes les taches en cours d un etudiant avec leurs deadlines : %s
                Genere un plan de travail realiste pour aujourd hui.
                Reponds UNIQUEMENT en JSON valide, sans texte avant ni apres :
                {"plan": [{"heure": "9h-10h", "tache": "...", "objectif": "..."}], "conseil": "..."}
                """.formatted(tachesJson);

        // Appeler Ollama
        try {
            String response = ollamaService.appeler(prompt);
            return parseReponse(response, tachesAvecDeadline);
        } catch (Exception e) {
            System.err.println("PlanificateurService — Ollama indisponible : " + e.getMessage());
            return fallback(tachesAvecDeadline);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PARSING DE LA RÉPONSE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Parse la réponse JSON d'Ollama pour extraire le planning.
     *
     * FORMAT JSON ATTENDU :
     * {
     *   "plan": [
     *     {"heure": "9h-10h", "tache": "Lire chapitre 3", "objectif": "Apprendre Java"},
     *     {"heure": "10h-11h", "tache": "Faire exercices", "objectif": "Apprendre Java"}
     *   ],
     *   "conseil": "Commencez par les tâches les plus urgentes..."
     * }
     *
     * @param response La réponse brute d'Ollama
     * @param taches   Les tâches collectées (pour le fallback)
     * @return PlanResultat parsé
     */
    private PlanResultat parseReponse(String response, List<JSONObject> taches) {
        if (response == null || response.isBlank()) return fallback(taches);
        try {
            // Extraire le JSON de la réponse
            int start = response.indexOf('{');
            int end   = response.lastIndexOf('}');
            if (start == -1 || end == -1) return fallback(taches);

            JSONObject json = new JSONObject(response.substring(start, end + 1));
            String conseil = json.optString("conseil", "Bonne journee de travail !");
            List<CreneauPlan> plan = new ArrayList<>();

            JSONArray arr = json.optJSONArray("plan");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject item = arr.optJSONObject(i);
                    if (item != null) {
                        plan.add(new CreneauPlan(
                                item.optString("heure", "—"),
                                item.optString("tache", "—"),
                                item.optString("objectif", "—")
                        ));
                    }
                }
            }

            return plan.isEmpty() ? fallback(taches) : new PlanResultat(plan, conseil);
        } catch (Exception e) {
            return fallback(taches);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FALLBACKS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Génère un planning simple sans IA (si Ollama est indisponible).
     * Assigne chaque tâche à un créneau horaire prédéfini.
     *
     * @param taches Les tâches à planifier
     * @return PlanResultat avec un planning basique
     */
    private PlanResultat fallback(List<JSONObject> taches) {
        // Créneaux horaires prédéfinis (matin + après-midi)
        String[] heures = {"9h-10h", "10h-11h", "11h-12h", "14h-15h", "15h-16h", "16h-17h"};
        List<CreneauPlan> plan = new ArrayList<>();

        // Assigner une tâche par créneau (max 6 tâches)
        for (int i = 0; i < Math.min(taches.size(), heures.length); i++) {
            JSONObject t = taches.get(i);
            plan.add(new CreneauPlan(
                    heures[i],
                    t.optString("tache", "Tache " + (i + 1)),
                    t.optString("objectif", "—")
            ));
        }

        return new PlanResultat(plan,
                "Concentrez-vous sur une tache a la fois pour maximiser votre productivite.");
    }

    /**
     * Retourne un résultat vide quand il n'y a aucune tâche en cours.
     *
     * @return PlanResultat vide avec un message d'encouragement
     */
    private PlanResultat fallbackVide() {
        return new PlanResultat(new ArrayList<>(),
                "Aucune tache en cours. Ajoutez des taches a vos objectifs !");
    }
}
