package edu.connection3a36.services;

import org.json.JSONObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class WikipediaService {

    private static final String API_URL = "https://fr.wikipedia.org/api/rest_v1/page/summary/";

    public static CompletableFuture<String> getSummary(String term) {
        // Nettoyer le terme pour l'URL (remplacer espaces par _)
        String formattedTerm = term.trim().replace(" ", "_");
        
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + formattedTerm))
                .header("User-Agent", "MentorAI/1.0 (contact@mentorai.edu)")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        JSONObject json = new JSONObject(response.body());
                        if (json.has("extract")) {
                            return json.getString("extract");
                        }
                    } else if (response.statusCode() == 404) {
                        return "Désolé, aucune définition trouvée sur Wikipedia pour ce terme.";
                    }
                    return "Erreur lors de la récupération de la définition (" + response.statusCode() + ")";
                })
                .exceptionally(ex -> "Erreur de connexion : " + ex.getMessage());
    }
}
