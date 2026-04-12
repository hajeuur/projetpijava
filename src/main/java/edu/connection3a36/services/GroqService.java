package edu.connection3a36.services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service d'appel à l'API Groq pour l'IA pédagogique (enseignant) et décisionnelle (admin).
 * Migration de GroqService.php avec le même system prompt.
 */
public class GroqService {

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.1-8b-instant";
    private static String API_KEY;

    static {
        // Chargement de l'API Key depuis config.properties
        try (java.io.InputStream is = new java.io.FileInputStream("config.properties")) {
            java.util.Properties props = new java.util.Properties();
            props.load(is);
            API_KEY = props.getProperty("GROQ_API_KEY");
        } catch (Exception e) {
            System.err.println("❌ Erreur lecture config.properties: " + e.getMessage());
            API_KEY = "dummy_key"; // Fallback ou gérer autrement
        }
    }

    private static final String SYSTEM_PROMPT = """
            Tu es MentorAI, l'intelligence stratégique de la plateforme MentorAI à l'école ESPRIT.
            Ton rôle est d'être l'assistant expert pour l'Aide à la Décision Pédagogique et Stratégique.

            STRUCTURE MODULAIRE DE MENTORAI :
            1. GESTION DES ACCÈS : Authentification, Rôles (Admin/Teacher/Student).
            2. AIDE À LA DÉCISION (Ton module principal) : Analyse des performances, détection de risques, recommandations, prédictions, plans d'actions.
            3. PSYCHOLOGIE : Suivi de l'état psychologique et Résumés de cours.
            4. PORTFOLIO & ORIENTATION : Profil étudiant et recommandations d'employabilité.
            5. FEEDBACK IA : Amélioration continue via les retours utilisateurs.
            6. COACHING & PRODUCTIVITÉ : Gestion des objectifs personnels avec gamification.

            CONSIGNES :
            - Sois concret et actionnable dans tes recommandations
            - Utilise les données contextuelles pour faire des liens
            - Fournis un bloc JSON structuré si tu fais une analyse :
            ```json
            {
                "metrics": [{"label": "Libellé", "value": "99", "unit": "%", "trend": "up/down/neutral"}],
                "alerts": [{"level": "low/medium/high", "message": "Description"}],
                "predictions": [{"label": "Titre", "probability": "85%", "details": "Pourquoi"}],
                "decisions": [{"action": "Action concrète", "category": "PEDAGOGIQUE/STRATEGIQUE/ADMINISTRATIVE", "priority": "high/medium/low"}]
            }
            ```
            """;

    /**
     * Envoie un message à l'API Groq avec l'historique de conversation.
     *
     * @param userMessage  Le message de l'utilisateur
     * @param history      L'historique des messages [{role, content}, ...]
     * @param roleContext  Contexte du rôle (ADMIN ou ENSEIGNANT) pour adapter les réponses
     * @return La réponse de l'IA
     */
    public String sendMessage(String userMessage, List<Map<String, String>> history, String roleContext) throws Exception {
        JSONArray messages = new JSONArray();

        // System prompt
        String contextualPrompt = SYSTEM_PROMPT;
        if ("ADMIN".equalsIgnoreCase(roleContext)) {
            contextualPrompt += "\nL'utilisateur est un ADMINISTRATEUR. Tu as une vue globale. Analyse les statistiques de l'école.";
        } else {
            contextualPrompt += "\nL'utilisateur est un ENSEIGNANT. Concentre-toi sur ses classes et étudiants.";
        }

        messages.put(new JSONObject().put("role", "system").put("content", contextualPrompt));

        // Historique limité aux 4 derniers messages pour éviter la saturation TPM (Token Per Minute) de Groq
        if (history != null) {
            int startIndex = Math.max(0, history.size() - 4);
            for (int i = startIndex; i < history.size(); i++) {
                Map<String, String> msg = history.get(i);
                String role = msg.get("role");
                String content = msg.get("content");
                if (role != null && content != null && (role.equals("user") || role.equals("assistant"))) {
                    messages.put(new JSONObject().put("role", role).put("content", content));
                }
            }
        }

        // Message utilisateur
        messages.put(new JSONObject().put("role", "user").put("content", userMessage));

        // Corps de la requête
        JSONObject body = new JSONObject();
        body.put("model", MODEL);
        body.put("messages", messages);
        body.put("temperature", 0.7);
        body.put("max_tokens", 800); // 800 au lieu de 2048 pour éviter Request Token = 3170 > 6000 TPM limit

        // Appel HTTP
        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        int statusCode = conn.getResponseCode();
        if (statusCode != 200) {
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
            StringBuilder errorBody = new StringBuilder();
            String line;
            while ((line = errorReader.readLine()) != null) {
                errorBody.append(line);
            }
            throw new Exception("Erreur API Groq (" + statusCode + "): " + errorBody);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder responseBody = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            responseBody.append(line);
        }

        JSONObject response = new JSONObject(responseBody.toString());
        return response.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");
    }

    /**
     * Version simplifiée sans historique
     */
    public String sendSimpleMessage(String message, String roleContext) throws Exception {
        return sendMessage(message, new ArrayList<>(), roleContext);
    }
}
