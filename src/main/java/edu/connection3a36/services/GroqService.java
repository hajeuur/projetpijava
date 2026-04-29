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

        public static CompletableFuture<String> getInterviewQuestion(String userContext) {
                String prompt = "Tu es un recruteur expert. Génère UNE seule question d'entretien d'embauche technique ou comportementale "
                                + "pour un profil ayant le contexte suivant : " + userContext + ". "
                                + "La question doit être directe, professionnelle et stimulante. Ne réponds QUE par la question.";
                return getResponse(prompt, false);
        }

        public static CompletableFuture<String> evaluateAnswer(String question, String answer) {
                String prompt = "En tant que recruteur expert, évalue la réponse suivante à la question : '" + question + "'. "
                                + "Réponse de l'utilisateur : '" + answer + "'. "
                                + "Fournis une évaluation structurée en JSON comme ceci : "
                                + "{\"score\": 8, \"feedback\": \"...\", \"tip\": \"...\"}. "
                                + "Le score est sur 10. Le feedback doit être constructif. Le tip est un conseil court.";
                
                HttpClient client = HttpClient.newHttpClient();
                JSONObject requestBody = new JSONObject();
                requestBody.put("model", "llama-3.3-70b-versatile");
                requestBody.put("response_format", new JSONObject().put("type", "json_object"));

                JSONArray messages = new JSONArray();
                messages.put(new JSONObject().put("role", "system").put("content", "Tu es un expert en recrutement qui répond uniquement en JSON."));
                messages.put(new JSONObject().put("role", "user").put("content", prompt));
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
                                                return new JSONObject(response.body()).getJSONArray("choices")
                                                                .getJSONObject(0).getJSONObject("message").getString("content");
                                        }
                                        return "{\"score\": 0, \"feedback\": \"Erreur API\", \"tip\": \"\"}";
                                });
        }
}
