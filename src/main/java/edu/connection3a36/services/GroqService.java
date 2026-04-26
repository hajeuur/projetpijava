package edu.connection3a36.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import org.json.JSONObject;
import org.json.JSONArray;

public class GroqService {
        private static final String API_KEY = "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";
        private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

        public static CompletableFuture<String> getResponse(String userMessage, boolean isProposeRequest) {
                HttpClient client = HttpClient.newHttpClient();

                String systemPrompt = "Tu es MentorAI, un mentor expert en gestion de projets et en développement de carrière. "
                                + "Tu es capable de répondre à toutes les questions techniques, méthodologiques ou stratégiques concernant les projets. "
                                + "Si l'utilisateur te demande de 'proposer' ou 'suggérer' un projet, tu dois : "
                                + "1. Expliquer ton idée et ses avantages de manière pédagogique. "
                                + "2. Absolument inclure à la FIN de ton message un bloc JSON valide entouré de balises [JSON] et [/JSON] "
                                + "contenant : {\"titre\": \"...\", \"description\": \"...\", \"technologies\": \"...\"}. "
                                + "Si ce n'est pas une demande de projet, réponds simplement de manière professionnelle en restant dans le domaine des projets.";

                JSONObject requestBody = new JSONObject();
                requestBody.put("model", "llama-3.3-70b-versatile");

                JSONArray messages = new JSONArray();
                messages.put(new JSONObject().put("role", "system").put("content", systemPrompt));
                messages.put(new JSONObject().put("role", "user").put("content", userMessage));

                requestBody.put("messages", messages);

                HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(API_URL))
                                .header("Content-Type", "application/json")
                                .header("Authorization", "Bearer " + API_KEY)
                                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                                .build();

                return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                                .thenApply(response -> {
                                        if (response.statusCode() == 200) {
                                                JSONObject jsonResponse = new JSONObject(response.body());
                                                return jsonResponse.getJSONArray("choices")
                                                                .getJSONObject(0)
                                                                .getJSONObject("message")
                                                                .getString("content");
                                        } else {
                                                System.err.println("Groq Error Response: " + response.body());
                                                return "Désolé, je rencontre une petite difficulté technique.";
                                        }
                                })
                                .exceptionally(ex -> "Erreur de connexion : " + ex.getMessage());
        }
}
