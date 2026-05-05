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
 * SERVICE : RisqueAbandonService — FONCTIONNALITÉ 2
 * ============================================================
 * Détecte si les tâches abandonnées mettent en danger l'objectif.
 * Utilise l'IA Ollama pour une analyse intelligente et contextuelle.
 *
 * QUAND EST-IL APPELÉ ?
 * 1. Automatiquement au chargement de ProgrammeDetail (analyserRisqueAbandon())
 * 2. Au démarrage de l'application via MainController (detecterAlerteGlobale())
 *
 * FONCTIONNEMENT :
 * 1. Filtrer les tâches avec l'état "Abandonner"
 * 2. Si aucune tâche abandonnée → retourner "pas de risque"
 * 3. Sinon → envoyer à Ollama :
 *    - La liste des tâches abandonnées (JSON)
 *    - Le score actuel (%)
 *    - Le nombre de jours restants avant la deadline
 * 4. Ollama répond en JSON : risque oui/non, tâches à relancer, conseil
 * 5. Si Ollama est indisponible → fallback logique pure
 *
 * RÉSULTATS DANS L'INTERFACE :
 * - Badge 🚨 sur les tâches abandonnées à risque
 * - Message d'alerte jaune dans ProgrammeDetail
 * - Bandeau orange en haut de l'application (MainController)
 *
 * CLASSES INTERNES :
 * - RisqueResultat : résultat pour un objectif spécifique
 * - AlerteAbandon  : résultat global pour le bandeau de notification
 * ============================================================
 */
public class RisqueAbandonService {

    /** Service IA Ollama pour l'analyse intelligente */
    private final OllamaService ollamaService = new OllamaService();

    // ─────────────────────────────────────────────────────────────────────────
    // CLASSE INTERNE : RisqueResultat
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Résultat de l'analyse de risque pour un objectif spécifique.
     * Utilisé dans ProgrammeDetailController pour afficher les badges 🚨.
     */
    public static class RisqueResultat {
        /** true si les tâches abandonnées mettent en danger l'objectif */
        public final boolean risque;

        /** Liste des titres des tâches à relancer en priorité */
        public final List<String> tachesARelancer;

        /** Conseil personnalisé de l'IA pour l'utilisateur */
        public final String conseil;

        public RisqueResultat(boolean risque, List<String> tachesARelancer, String conseil) {
            this.risque = risque;
            this.tachesARelancer = tachesARelancer;
            this.conseil = conseil;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ANALYSE DE RISQUE POUR UN OBJECTIF
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Analyse les tâches abandonnées d'un programme et évalue le risque.
     * Ne propage jamais d'exception (utilise le fallback en cas d'erreur).
     *
     * @param toutesLesTaches Toutes les tâches du programme
     * @param scorePourcentage Le score actuel du programme (0-100)
     * @param datefin La deadline de l'objectif (peut être null)
     * @return RisqueResultat avec l'analyse complète
     */
    public RisqueResultat analyserRisque(List<Tache> toutesLesTaches,
                                          int scorePourcentage, LocalDate datefin) {
        // 1. Filtrer uniquement les tâches abandonnées
        List<Tache> abandonnees = toutesLesTaches.stream()
                .filter(t -> t.getEtat() == Etat.Abandonner)
                .toList();

        // 2. Si aucune tâche abandonnée → pas de risque
        if (abandonnees.isEmpty()) {
            return new RisqueResultat(false, new ArrayList<>(),
                    "Aucune tache abandonnee, continuez ainsi !");
        }

        // 3. Calculer les jours restants (30 par défaut si pas de deadline)
        long joursRestants = datefin != null
                ? ChronoUnit.DAYS.between(LocalDate.now(), datefin) : 30;

        // 4. Construire le JSON des tâches abandonnées pour Ollama
        String tachesJson = buildTachesJson(abandonnees);

        // 5. Construire le prompt pour Ollama
        String prompt = """
                Un etudiant a abandonne ces taches : %s
                Score actuel : %d%%, Jours restants : %d
                Ces abandons mettent-ils en danger son objectif ?
                Reponds UNIQUEMENT en JSON valide, sans texte avant ni apres :
                {"risque": "Oui/Non", "taches_a_relancer": ["titre1", "titre2"], "conseil": "..."}
                """.formatted(tachesJson, scorePourcentage, joursRestants);

        // 6. Appeler Ollama et parser la réponse
        try {
            String response = ollamaService.appeler(prompt);
            return parseReponse(response, abandonnees);
        } catch (Exception e) {
            // Ollama indisponible → utiliser le fallback logique
            System.err.println("RisqueAbandonService — Ollama indisponible : " + e.getMessage());
            return fallback(abandonnees, scorePourcentage, joursRestants);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PARSING DE LA RÉPONSE OLLAMA
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Parse la réponse JSON d'Ollama pour extraire le résultat de risque.
     *
     * FORMAT JSON ATTENDU :
     * {
     *   "risque": "Oui",
     *   "taches_a_relancer": ["Titre tâche 1", "Titre tâche 2"],
     *   "conseil": "Relancez ces tâches rapidement..."
     * }
     *
     * STRATÉGIE : Chercher le premier '{' et le dernier '}' pour extraire
     * le JSON même si Ollama ajoute du texte autour.
     *
     * @param response La réponse brute d'Ollama
     * @param abandonnees Les tâches abandonnées (pour le fallback)
     * @return RisqueResultat parsé
     */
    private RisqueResultat parseReponse(String response, List<Tache> abandonnees) {
        if (response == null || response.isBlank()) return fallback(abandonnees, 0, 0);
        try {
            // ════════════════════════════════════════════════════════════════
            // PROBLÈME : Ollama ne retourne pas toujours du JSON pur.
            // Il peut répondre : "Voici mon analyse : {"risque": "Oui"} Bonne chance!"
            // ou ajouter des explications avant/après le JSON.
            //
            // SOLUTION : on cherche manuellement le premier '{' et le dernier '}'
            // pour extraire uniquement la partie JSON de la réponse.
            //
            // Exemple :
            //   response = "Analyse : {"risque":"Oui","conseil":"..."} Fin."
            //   start = 10 (position du '{')
            //   end   = 42 (position du '}')
            //   → on extrait response.substring(10, 43) = {"risque":"Oui",...}
            //
            // Si on ne trouve pas '{' ou '}' → indexOf retourne -1 → fallback
            // ════════════════════════════════════════════════════════════════
            int start = response.indexOf('{');      // position du premier '{'
            int end   = response.lastIndexOf('}');  // position du dernier '}'
            if (start == -1 || end == -1) return fallback(abandonnees, 0, 0);

            // Parser le JSON extrait
            JSONObject json = new JSONObject(response.substring(start, end + 1));

            // optString() : lit le champ, retourne la valeur par défaut si absent
            // (plus sûr que getString() qui lève une exception si le champ manque)
            boolean risque = "Oui".equalsIgnoreCase(json.optString("risque", "Non"));
            String conseil = json.optString("conseil", "Relancez les taches abandonnees.");

            // Lire le tableau JSON des tâches à relancer
            List<String> aRelancer = new ArrayList<>();
            JSONArray arr = json.optJSONArray("taches_a_relancer");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) aRelancer.add(arr.optString(i));
            }

            return new RisqueResultat(risque, aRelancer, conseil);
        } catch (Exception e) {
            // Si le parsing échoue (JSON malformé, etc.) → utiliser le fallback
            return fallback(abandonnees, 0, 0);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FALLBACK (si Ollama est indisponible)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Analyse de risque basée sur une logique simple (sans IA).
     * Utilisée quand Ollama est indisponible.
     *
     * RÈGLE SIMPLE :
     * risque = true si (score < 50% OU jours restants < 7)
     *
     * @param abandonnees Les tâches abandonnées
     * @param score       Le score actuel
     * @param jours       Les jours restants avant la deadline
     * @return RisqueResultat basé sur la logique simple
     */
    private RisqueResultat fallback(List<Tache> abandonnees, int score, long jours) {
        // Risque si score faible OU deadline proche
        boolean risque = !abandonnees.isEmpty() && (score < 50 || jours < 7);

        // Prendre les 3 premières tâches abandonnées comme prioritaires
        List<String> aRelancer = abandonnees.stream()
                .map(Tache::getTitre)
                .limit(3)
                .toList();

        String conseil = risque
                ? "Attention : des taches abandonnees menacent votre objectif. Relancez-les rapidement."
                : "Quelques taches sont abandonnees. Pensez a les relancer pour ameliorer votre score.";

        return new RisqueResultat(risque, aRelancer, conseil);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CLASSE INTERNE : AlerteAbandon (pour le bandeau global)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Résultat global de l'analyse pour le bandeau de notification en haut
     * de l'application (MainController).
     * Agrège les tâches abandonnées de TOUS les objectifs de l'utilisateur.
     */
    public static class AlerteAbandon {
        /** Nombre total de tâches abandonnées dans tous les objectifs */
        public final int nbTachesAbandonnes;

        /** Titres des objectifs qui ont au moins une tâche abandonnée */
        public final List<String> titresObjectifsConcernes;

        public AlerteAbandon(int nbTachesAbandonnes, List<String> titresObjectifsConcernes) {
            this.nbTachesAbandonnes = nbTachesAbandonnes;
            this.titresObjectifsConcernes = titresObjectifsConcernes;
        }

        /** Retourne true s'il y a au moins une tâche abandonnée */
        public boolean hasRisque() {
            return nbTachesAbandonnes > 0;
        }

        /**
         * Génère le message à afficher dans le bandeau orange.
         * Exemple : "⚠️ 3 tâche(s) abandonnée(s) risquent de compromettre vos objectifs !
         *            — Objectifs concernés : Apprendre Java, Préparer l'examen"
         *
         * @return Le message formaté, ou null si pas de risque
         */
        public String getMessage() {
            if (!hasRisque()) return null;
            String objectifsStr = titresObjectifsConcernes.isEmpty()
                    ? ""
                    : " — Objectifs concernés : " + String.join(", ", titresObjectifsConcernes);
            return "⚠️  " + nbTachesAbandonnes
                    + " tâche(s) abandonnée(s) risquent de compromettre vos objectifs !"
                    + objectifsStr;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ALERTE GLOBALE (pour le bandeau MainController)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Analyse TOUS les objectifs de l'utilisateur et retourne une alerte globale.
     * Utilisé par MainController au démarrage pour afficher le bandeau orange.
     *
     * FONCTIONNEMENT :
     * Pour chaque objectif → récupérer ses tâches → compter les abandonnées
     * Agréger le total et les noms des objectifs concernés.
     *
     * Cette méthode est exécutée dans un Thread séparé (non-bloquant)
     * pour ne pas ralentir le démarrage de l'application.
     *
     * @param objectifs Liste des objectifs de l'utilisateur connecté
     * @return AlerteAbandon avec le total et les objectifs concernés
     */
    public AlerteAbandon detecterAlerteGlobale(List<Objectif> objectifs) {
        TacheService tacheService = new TacheService();
        int totalAbandonnees = 0;
        List<String> objectifsConcernes = new ArrayList<>();

        for (Objectif o : objectifs) {
            try {
                if (o.getProgramme() == null) continue; // Pas de programme → ignorer

                // Récupérer les tâches du programme de cet objectif
                List<Tache> taches = tacheService.getByProgramme(o.getProgramme().getId());

                // Compter les tâches abandonnées
                long nbAbandonnees = taches.stream()
                        .filter(t -> t.getEtat() == Etat.Abandonner)
                        .count();

                if (nbAbandonnees > 0) {
                    totalAbandonnees += nbAbandonnees;
                    objectifsConcernes.add(o.getTitre()); // Cet objectif est concerné
                }
            } catch (Exception ignored) {
                // Ignorer les erreurs individuelles pour ne pas bloquer les autres
            }
        }

        return new AlerteAbandon(totalAbandonnees, objectifsConcernes);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONSTRUCTION DU JSON POUR OLLAMA
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Construit un tableau JSON des tâches abandonnées à envoyer à Ollama.
     *
     * FORMAT PRODUIT :
     * [{"titre": "Titre tâche 1", "etat": "Abandonner"}, ...]
     *
     * @param taches Les tâches abandonnées
     * @return Chaîne JSON représentant les tâches
     */
    private String buildTachesJson(List<Tache> taches) {
        JSONArray arr = new JSONArray();
        for (Tache t : taches) {
            JSONObject obj = new JSONObject();
            obj.put("titre", t.getTitre());
            obj.put("etat", "Abandonner");
            arr.put(obj);
        }
        return arr.toString();
    }
}
