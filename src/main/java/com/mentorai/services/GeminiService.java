package com.mentorai.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GeminiService {

    private static final String API_KEY =
            "AIzaSyB679M0wqDfOJ4hO2n0F5SULn5I4yqrhfo";

    private static final String URL =
            "https://generativelanguage.googleapis.com/v1/models/" +
                    "gemini-2.0-flash-lite:generateContent?key=" + API_KEY;

    // ✅ TEMPORAIRE — lister les modèles disponibles
    public void listerModeles() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "https://generativelanguage.googleapis.com/v1/models?key=" + API_KEY
                    ))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(
                    request, HttpResponse.BodyHandlers.ofString()
            );
            System.out.println("=== MODELES DISPONIBLES ===");
            System.out.println(response.body());
            System.out.println("===========================");
        } catch (Exception e) {
            System.out.println("Erreur liste modeles : " + e.getMessage());
        }
    }

    public String suggererReponse(String typeFeedback, String note, String message) {
        try {
            String prompt =
                    "Tu es un assistant administrateur d une plateforme educative MentorAI. " +
                            "Un etudiant a soumis un feedback avec les details suivants :" +
                            " Type : " + typeFeedback +
                            ", Note : " + note + "/5" +
                            ", Message : " + message +
                            ". Redige une reponse professionnelle empathique et constructive " +
                            "en francais pour cet etudiant. " +
                            "La reponse doit etre courte (3-4 phrases maximum), " +
                            "personnalisee selon le type et le contenu du feedback. " +
                            "Ne mets pas de salutation ni de signature, juste la reponse directe.";

            String promptEscaped = prompt
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");

            String requestBody =
                    "{\"contents\":[{\"parts\":[{\"text\":\"" +
                            promptEscaped + "\"}]}]}";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(
                    request, HttpResponse.BodyHandlers.ofString()
            );

            System.out.println("Gemini status : " + response.statusCode());
            System.out.println("Gemini body : " + response.body());

            if (response.statusCode() == 200) {
                String body = response.body();
                String marker = "\"text\": \"";
                int start = body.indexOf(marker);
                if (start != -1) {
                    start += marker.length();
                    int end = body.indexOf("\"", start);
                    while (end != -1 && body.charAt(end - 1) == '\\') {
                        end = body.indexOf("\"", end + 1);
                    }
                    if (end != -1) {
                        String texte = body.substring(start, end);
                        texte = texte.replace("\\n", "\n")
                                .replace("\\\"", "\"")
                                .replace("\\\\", "\\");
                        return texte.trim();
                    }
                }
            }
            return null;

        } catch (Exception e) {
            System.out.println("Erreur Gemini : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}