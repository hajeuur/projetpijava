package edu.connection3a36.services;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * Service pour interagir avec l'API Wikipedia (aucune clé requise).
 * Récupère des résumés de notions en français.
 */
public class WikipediaService {

    private static final String API_URL = "https://fr.wikipedia.org/api/rest_v1/page/summary/";

    /**
     * Méthode synchrone pour récupérer un résumé depuis Wikipedia.
     * (Ajoutée depuis la branche main)
     * Note: Renommée en getSummarySync car la méthode getSummary() existe déjà avec une signature asynchrone.
     */
    public String getSummarySync(String title) throws Exception {
        if (title == null || title.trim().isEmpty()) {
            return "Veuillez entrer une notion à rechercher.";
        }

        String encodedTitle = URLEncoder.encode(title.trim().replace(" ", "_"), StandardCharsets.UTF_8);
        URL url = new URL(API_URL + encodedTitle);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "MentorAI/1.0 (https://esprit.tn; contact@esprit.tn)");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);

        int responseCode = conn.getResponseCode();
        if (responseCode == 404) {
            return "Désolé, aucune page Wikipedia n'a été trouvée pour : " + title
                    + "\n\nEssayez avec un terme plus précis (ex: 'Intelligence artificielle').";
        }
        if (responseCode != 200) {
            throw new Exception("Erreur API Wikipedia (" + responseCode + ")");
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();

        JSONObject json = new JSONObject(content.toString());
        if (json.has("extract")) {
            return json.getString("extract");
        }

        return "Aucun résumé disponible pour cette notion.";
    }

    /**
     * Méthode asynchrone originale pour récupérer un résumé depuis Wikipedia.
     */
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
