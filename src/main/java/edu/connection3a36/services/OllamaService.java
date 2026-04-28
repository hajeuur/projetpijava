package edu.connection3a36.services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * SERVICE : OllamaService
 * ============================================================
 * Communique avec Ollama, un serveur d'IA local (LLM).
 * Ollama fait tourner le modèle llama3.1:8b sur la machine locale.
 *
 * POURQUOI OLLAMA ET PAS UNE API CLOUD ?
 * - Fonctionne sans connexion internet
 * - Données privées (rien n'est envoyé à l'extérieur)
 * - Gratuit (pas de coût par requête)
 *
 * PRÉREQUIS :
 * - Ollama doit être installé et démarré sur localhost:11434
 * - Le modèle llama3.1:8b doit être téléchargé (ollama pull llama3.1:8b)
 *
 * FONCTIONNALITÉS UTILISÉES DANS LE MODULE OBJECTIFS :
 * 1. genererTaches()         → Génère 5 tâches pour un objectif donné
 * 2. genererMessageMotivant() → Génère un message d'encouragement personnalisé
 * 3. appeler()               → Point d'entrée générique (utilisé par RisqueAbandonService)
 *
 * GESTION DES ERREURS :
 * Si Ollama est indisponible, chaque méthode a un fallback (valeurs par défaut)
 * pour ne pas bloquer l'application.
 *
 * COMMUNICATION HTTP :
 * POST http://localhost:11434/api/generate
 * Body JSON : { "model": "llama3.1:8b", "prompt": "...", "stream": false }
 * Response JSON : { "response": "..." }
 * ============================================================
 */
public class OllamaService {

    /** URL de l'API Ollama locale */
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";

    /** Modèle LLM utilisé (llama 3.1, 8 milliards de paramètres) */
    private static final String MODEL = "llama3.1:8b";

    // ─────────────────────────────────────────────────────────────────────────
    // POINT D'ENTRÉE PUBLIC GÉNÉRIQUE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Envoie un prompt à Ollama et retourne la réponse brute.
     * Utilisé par RisqueAbandonService et PlanificateurService.
     *
     * @param prompt Le texte à envoyer à l'IA
     * @return La réponse textuelle de l'IA
     * @throws Exception Si Ollama est indisponible ou retourne une erreur
     */
    public String appeler(String prompt) throws Exception {
        return callOllama(prompt);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GÉNÉRATION DE TÂCHES
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Génère automatiquement 5 tâches concrètes pour un objectif donné.
     * Appelé depuis ObjectifFormController quand l'utilisateur clique
     * sur "Générer les tâches avec l'IA".
     *
     * PROMPT ENVOYÉ À OLLAMA :
     * "Tu es un coach personnel expert en productivité. Un utilisateur a défini
     *  l'objectif suivant : [titre] / [description]
     *  Génère exactement 5 tâches concrètes, ordonnées et progressives..."
     *
     * FORMAT DE RÉPONSE ATTENDU (JSON) :
     * [{"titre": "Titre court", "description": "Description détaillée"}, ...]
     *
     * @param objectifTitre       Le titre de l'objectif
     * @param objectifDescription La description de l'objectif (peut être vide)
     * @return Liste de tableaux String[2] : [0]=titre, [1]=description
     * @throws Exception Si Ollama est indisponible (le fallback est utilisé)
     */
    public List<String[]> genererTaches(String objectifTitre, String objectifDescription) throws Exception {
        String prompt = """
                Tu es un coach personnel expert en productivite. Un utilisateur a defini l objectif suivant :
                Titre : %s
                Description : %s
                Genere exactement 5 taches concretes, ordonnees et progressives pour atteindre cet objectif.
                Reponds UNIQUEMENT avec un tableau JSON, sans texte avant ni apres, au format :
                [{"titre": "Titre court", "description": "Description detaillee"}, ...]
                """.formatted(objectifTitre, objectifDescription != null ? objectifDescription : "");

        return parseTachesJson(callOllama(prompt));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GÉNÉRATION DE MESSAGE MOTIVANT
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Génère un message de motivation personnalisé basé sur la progression.
     * Appelé depuis ProgrammeDetailController quand l'utilisateur clique
     * sur "Rafraîchir le message".
     *
     * CONTEXTE ENVOYÉ À OLLAMA :
     * - Titre de l'objectif
     * - Score actuel (%)
     * - Évolution depuis la dernière fois (+/- %)
     * - Jours restants avant la deadline
     *
     * EXEMPLE DE MESSAGE GÉNÉRÉ :
     * "Bravo pour ta progression de +15% ! Tu es sur la bonne voie pour
     *  atteindre ton objectif 'Apprendre Java'. Il te reste 12 jours,
     *  continue sur cette lancée !"
     *
     * @param objectifTitre  Le titre de l'objectif
     * @param scoreCourant   Le score actuel en %
     * @param scorePrec      Le score précédent en % (pour calculer l'évolution)
     * @param datefin        La deadline de l'objectif (peut être null)
     * @return Le message motivant (2-3 phrases)
     * @throws Exception Si Ollama est indisponible
     */
    public String genererMessageMotivant(String objectifTitre, int scoreCourant,
                                          int scorePrec, LocalDate datefin) throws Exception {
        // Calculer les jours restants avant la deadline
        long joursRestants = datefin != null
                ? ChronoUnit.DAYS.between(LocalDate.now(), datefin) : -1;

        // Construire le contexte de la deadline
        String contexteDeadline = joursRestants < 0
                ? "La deadline est depassee."
                : joursRestants == 0
                    ? "C est le dernier jour !"
                    : "Il reste " + joursRestants + " jour(s) avant la deadline.";

        // Construire le contexte de l'évolution du score
        int evolution = scoreCourant - scorePrec;
        String contexteEvolution = evolution > 0
                ? "Progression +" + evolution + "% depuis la derniere fois."
                : evolution < 0
                    ? "Progression -" + Math.abs(evolution) + "% depuis la derniere fois."
                    : "Progression stable.";

        String prompt = """
                Tu es un coach bienveillant. Genere un message d encouragement personnalise en francais (2-3 phrases max).
                Objectif : %s | Score : %d%% | %s | %s
                Reponds UNIQUEMENT avec le message.
                """.formatted(objectifTitre, scoreCourant, contexteDeadline, contexteEvolution);

        return callOllama(prompt).trim();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COMMUNICATION HTTP AVEC OLLAMA
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Envoie une requête HTTP POST à l'API Ollama et retourne la réponse.
     *
     * STRUCTURE DE LA REQUÊTE :
     * POST http://localhost:11434/api/generate
     * Content-Type: application/json
     * Body: {
     *   "model": "llama3.1:8b",
     *   "prompt": "...",
     *   "stream": false,           ← Réponse complète (pas de streaming)
     *   "options": {
     *     "temperature": 0.7,      ← Créativité (0=déterministe, 1=créatif)
     *     "num_predict": 512       ← Longueur max de la réponse
     *   }
     * }
     *
     * TIMEOUTS :
     * - Connexion : 15 secondes
     * - Lecture : 120 secondes (le modèle peut prendre du temps)
     *
     * @param prompt Le texte à envoyer au modèle
     * @return La réponse textuelle du modèle
     * @throws Exception Si la connexion échoue ou si le statut HTTP n'est pas 200
     */
    private String callOllama(String prompt) throws Exception {
        URL url = new URL(OLLAMA_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);  // 15 secondes pour se connecter
        conn.setReadTimeout(120000);    // 2 minutes pour lire la réponse

        // Construire le corps de la requête JSON
        JSONObject body = new JSONObject();
        body.put("model", MODEL);
        body.put("prompt", prompt);
        body.put("stream", false); // Réponse complète en une fois
        body.put("options", new JSONObject()
                .put("temperature", 0.7)   // Équilibre créativité/cohérence
                .put("num_predict", 512));  // Max 512 tokens dans la réponse

        // Envoyer le corps de la requête
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        // Vérifier le code de statut HTTP
        int status = conn.getResponseCode();
        if (status != 200) {
            // Lire le message d'erreur
            BufferedReader err = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = err.readLine()) != null) sb.append(line);
            throw new Exception("Erreur Ollama (" + status + "): " + sb);
        }

        // Lire la réponse JSON
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);

        // Extraire le champ "response" du JSON retourné par Ollama
        return new JSONObject(sb.toString()).getString("response");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PARSING DE LA RÉPONSE JSON
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Parse la réponse JSON d'Ollama pour extraire les tâches générées.
     *
     * STRATÉGIE DE PARSING :
     * 1. Chercher le premier '[' et le dernier ']' dans la réponse
     *    (Ollama peut ajouter du texte avant/après le JSON)
     * 2. Parser le JSON extrait
     * 3. En cas d'erreur → utiliser le fallback
     *
     * @param response La réponse brute d'Ollama
     * @return Liste de String[2] : [0]=titre, [1]=description
     */
    private List<String[]> parseTachesJson(String response) {
        List<String[]> taches = new ArrayList<>();
        try {
            // Trouver les délimiteurs du tableau JSON
            int start = response.indexOf('[');
            int end = response.lastIndexOf(']');
            if (start == -1 || end == -1) return fallbackTaches(); // Pas de JSON trouvé

            JSONArray arr = new JSONArray(response.substring(start, end + 1));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                taches.add(new String[]{
                        obj.optString("titre", "Tache " + (i + 1)),
                        obj.optString("description", "")
                });
            }
        } catch (Exception e) {
            return fallbackTaches(); // En cas d'erreur de parsing → fallback
        }
        return taches;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FALLBACK (si Ollama est indisponible)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Retourne 5 tâches génériques si Ollama est indisponible.
     * Permet à l'application de fonctionner même sans IA.
     *
     * @return Liste de 5 tâches génériques
     */
    private List<String[]> fallbackTaches() {
        List<String[]> list = new ArrayList<>();
        list.add(new String[]{"Definir le plan d action",    "Identifier les etapes cles."});
        list.add(new String[]{"Rassembler les ressources",   "Collecter les outils necessaires."});
        list.add(new String[]{"Demarrer la premiere etape",  "Commencer par la tache la plus simple."});
        list.add(new String[]{"Suivre la progression",       "Faire un point regulier."});
        list.add(new String[]{"Finaliser et valider",        "Verifier que l objectif est atteint."});
        return list;
    }
}
