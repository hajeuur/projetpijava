package com.mentorai.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class GeminiServiceCustom {

    private static final String API_KEY = getApiKey();

    private static String getApiKey() {
        String key = System.getenv("GEMINI_API_KEY");
        if (key == null || key.isBlank()) {
            return "AIzaSyAoTA2VVNV3jiI3WW4aebbv-2sInm77E7o";
        }
        return key;
    }

    private static final String URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent";

    public String suggererReponse(String typeFeedback, String note, String message) {
        try {
            String prompt = "Tu es un assistant MentorAI. Un étudiant a mis " + note + "/5 pour " + typeFeedback + 
                            " avec ce message: '" + message + "'. Suggère une réponse professionnelle courte.";
            String body = "{\"contents\":[{\"parts\":[{\"text\":\"" + prompt.replace("\"", "\\\"") + "\"}]}]}";
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(URL)).header("Content-Type", "application/json").header("X-goog-api-key", API_KEY).POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) return extractTextFromBody(response.body());
        } catch (Exception e) { e.printStackTrace(); }
        return "Réponse de secours : Merci pour votre retour, nous allons l'étudier.";
    }

    public void genererInsightsRapport(ApprentissageService.RapportData data) {
        String weakCur = String.join(", ", data.current.weakSubjects);
        String patterns = String.join(", ", data.current.patterns);

        String prompt =
            "Tu es un coach pédagogique MentorAI. Génère un rapport de coaching en JSON STRICT.\n" +
            "NE RÉPONDS QUE PAR LE JSON. PAS DE MARKDOWN. PAS DE TEXTE AVANT OU APRÈS.\n\n" +
            "DONNÉES SOURCE:\n" +
            "- Profil: " + data.current.behaviorProfile + "\n" +
            "- Humeur: " + String.format("%.1f", data.current.avgMood) + "/5\n" +
            "- Complétion: " + data.current.completionRate + "%\n" +
            "- Matières faibles: [" + weakCur + "]\n" +
            "- Alertes: [" + patterns + "]\n\n" +
            "FORMAT JSON REQUIS (TOUTES LES CLÉS DOIVENT EXISTER):\n" +
            "{\n" +
            "\"resumeGlobal\": \"(5-7 lignes)\",\n" +
            "\"analyseComportement\": \"(détail 150 mots)\",\n" +
            "\"analyseMentale\": \"(détail 150 mots)\",\n" +
            "\"diagnosticMatieres\": \"(analyse des matières)\",\n" +
            "\"profil\": \"(description du profil)\",\n" +
            "\"insights\": [\"ins1\", \"ins2\", \"ins3\", \"ins4\", \"ins5\", \"ins6\", \"ins7\", \"ins8\"],\n" +
            "\"planAction\": \"(actions concrètes)\",\n" +
            "\"planning\": \"(planning hebdomadaire textuel)\"\n" +
            "}";

        try {
            System.out.println("--- ENVOI REQUÊTE GEMINI ---");
            String escaped = escapeForJson(prompt);
            String body    = "{\"contents\":[{\"parts\":[{\"text\":\"" + escaped + "\"}]}]}";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URL))
                    .header("Content-Type", "application/json")
                    .header("X-goog-api-key", API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Gemini Status Code: " + response.statusCode());

            if (response.statusCode() == 200) {
                String rawText = extractTextFromBody(response.body());
                System.out.println("=== JSON REÇU (NETTOYÉ) ===");
                System.out.println(rawText);
                System.out.println("===========================");
                parseDetailedJson(rawText, data);
            } else {
                System.out.println("Gemini Error: " + response.body());
                applyFallback(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
            applyFallback(data);
        }
    }

    private void parseDetailedJson(String json, ApprentissageService.RapportData data) {
        if (json == null || json.isBlank()) { applyFallback(data); return; }
        json = json.trim();
        
        if (json.contains("```")) {
            int s = json.indexOf('{');
            int e = json.lastIndexOf('}');
            if (s != -1 && e != -1 && e > s) json = json.substring(s, e + 1);
        }

        data.resumeGlobal = extractString(json, "resumeGlobal");
        data.analyseComportement = extractString(json, "analyseComportement");
        data.analyseMentale = extractString(json, "analyseMentale");
        data.diagnosticMatieres = extractString(json, "diagnosticMatieres");
        data.profil = extractString(json, "profil");
        data.insights = extractArray(json, "insights");
        data.planAction = extractString(json, "planAction");
        data.planning = extractString(json, "planning");
    }

    private void applyFallback(ApprentissageService.RapportData data) {
        System.out.println("Utilisation du fallback Java pour le rapport.");
        String perf = data.current.completionRate >= 70 ? "bonne régularité" : "une progression à consolider";
        data.resumeGlobal = "Votre état actuel montre " + perf + " avec un taux de complétion de " + data.current.completionRate + "%.";
        data.analyseMentale = data.current.avgMood < 2.5 ? "Baisse de moral détectée." : "État mental stable.";
        data.insights = new ArrayList<>();
        data.insights.add("Complétion réelle : " + data.current.completionRate + "%");
        data.planAction = "Maintenez vos sessions à " + data.current.avgDurationPlanned + " min.";
    }

    // ===== JSON HELPERS =====

    private String escapeForJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String extractTextFromBody(String body) {
        String marker = "\"text\": \"";
        int start = body.indexOf(marker);
        if (start == -1) return "";
        start += marker.length();
        int end = start;
        while (end < body.length()) {
            char c = body.charAt(end);
            if (c == '"' && (end == 0 || body.charAt(end - 1) != '\\')) break;
            end++;
        }
        if (end >= body.length()) return "";
        return body.substring(start, end)
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private String extractString(String json, String key) {
        String marker = "\"" + key + "\"";
        int keyIdx = json.indexOf(marker);
        if (keyIdx == -1) return "";
        int colonIdx = json.indexOf(":", keyIdx + marker.length());
        if (colonIdx == -1) return "";
        int startQuote = json.indexOf("\"", colonIdx);
        if (startQuote == -1) return "";
        int start = startQuote + 1;
        int end = start;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (c == '"' && (end == 0 || json.charAt(end - 1) != '\\')) break;
            end++;
        }
        if (end >= json.length()) return "";
        return json.substring(start, end).replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\").trim();
    }

    private List<String> extractArray(String json, String key) {
        List<String> result = new ArrayList<>();
        String marker = "\"" + key + "\"";
        int keyIdx = json.indexOf(marker);
        if (keyIdx == -1) return result;
        int colonIdx = json.indexOf(":", keyIdx + marker.length());
        if (colonIdx == -1) return result;
        int startBracket = json.indexOf("[", colonIdx);
        if (startBracket == -1) return result;
        int endBracket = json.indexOf("]", startBracket);
        if (endBracket == -1) return result;
        String content = json.substring(startBracket + 1, endBracket);
        int i = 0;
        while (i < content.length()) {
            int q1 = content.indexOf('"', i);
            if (q1 == -1) break;
            int q2 = q1 + 1;
            while (q2 < content.length()) {
                if (content.charAt(q2) == '"' && (q2 == 0 || content.charAt(q2 - 1) != '\\')) break;
                q2++;
            }
            if (q2 >= content.length()) break;
            String item = content.substring(q1 + 1, q2).replace("\\n", "\n").replace("\\\"", "\"").trim();
            if (!item.isEmpty()) result.add(item);
            i = q2 + 1;
        }
        return result;
    }
}