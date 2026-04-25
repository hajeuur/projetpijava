package com.esprit.utils;

import com.esprit.models.Utilisateur;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class OllamaAIService {

    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String MODEL      = "mistral";

    /**
     * Analyse un utilisateur avec Mistral via Ollama
     * et retourne le verdict texte.
     */
    public static String analyzeUser(Utilisateur u) {
        try {
            String prompt = buildPrompt(u);
            String response = callOllama(prompt);
            return response;
        } catch (Exception e) {
            System.out.println(">>> Ollama erreur: " + e.getMessage());
            return "Erreur analyse IA: " + e.getMessage();
        }
    }

    /**
     * Construit le prompt envoyé à Mistral
     */
    private static String buildPrompt(Utilisateur u) {
        return "Tu es un système d'analyse de sécurité pour une plateforme e-learning. " +
                "Analyse ce profil utilisateur et donne un verdict court (3-4 phrases max) en français. " +
                "Dis si le profil est FIABLE, NEUTRE ou SUSPECT et pourquoi.\n\n" +
                "Profil utilisateur :\n" +
                "- Nom : " + u.getNom() + " " + u.getPrenom() + "\n" +
                "- Email : " + u.getEmail() + "\n" +
                "- Rôle : " + u.getRole() + "\n" +
                "- Status : " + u.getStatus() + "\n" +
                "- Score de confiance : " + String.format("%.0f", u.getTrustScore()) + "/100\n" +
                "- Niveau de risque : " + u.getRiskLevel() + "\n" +
                "- Tentatives de connexion échouées : " + u.getLoginAttempts() + "\n" +
                "- Photo de profil : " + (u.getPdpUrl() != null && !u.getPdpUrl().isEmpty() ? "Oui" : "Non") + "\n" +
                "- IP d'inscription : " + (u.getRegistrationIp() != null ? u.getRegistrationIp() : "Inconnue") + "\n" +
                "- Doublon IP détecté : " + (u.getFlaggedDuplicate() == 1 ? "Oui" : "Non") + "\n\n" +
                "Réponds UNIQUEMENT avec le verdict court, sans introduction ni explication supplémentaire.";
    }

    /**
     * Appelle l'API Ollama et retourne la réponse complète
     */
    private static String callOllama(String prompt) throws Exception {
        URL url = new URL(OLLAMA_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(120000); // 2 min max pour Mistral

        // Body JSON — stream:false pour avoir la réponse complète d'un coup
        String body = "{\"model\":\"" + MODEL + "\",\"prompt\":" +
                jsonEscape(prompt) + ",\"stream\":false}";

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        System.out.println(">>> Ollama status: " + status);

        if (status != 200) {
            InputStream err = conn.getErrorStream();
            String errMsg = err != null ? new String(err.readAllBytes()) : "Erreur inconnue";
            throw new Exception("Ollama HTTP " + status + ": " + errMsg);
        }

        String json = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        System.out.println(">>> Ollama réponse reçue (" + json.length() + " chars)");

        // Extraire le champ "response" du JSON
        return extractResponse(json);
    }

    /**
     * Extrait le champ "response" du JSON retourné par Ollama
     */
    private static String extractResponse(String json) {
        String key = "\"response\":\"";
        int start = json.indexOf(key);
        if (start == -1) return "Réponse IA indisponible";
        start += key.length();

        StringBuilder sb = new StringBuilder();
        boolean escape = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escape) {
                if (c == 'n') sb.append('\n');
                else if (c == 't') sb.append('\t');
                else sb.append(c);
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString().trim();
    }

    /**
     * Échappe une chaîne pour JSON
     */
    private static String jsonEscape(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"'  -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default   -> sb.append(c);
            }
        }
        sb.append("\"");
        return sb.toString();
    }
}