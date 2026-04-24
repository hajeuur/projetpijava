package com.mentorai.services;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class TraductionService {

    private static final String API_URL = "https://api.mymemory.translated.net/get";

    public String traduire(String texte, String langueSource, String langueCible) {
        try {
            String paire = langueSource + "|" + langueCible;
            String url = API_URL +
                    "?q=" + URLEncoder.encode(texte, StandardCharsets.UTF_8) +
                    "&langpair=" + URLEncoder.encode(paire, StandardCharsets.UTF_8);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(
                    request, HttpResponse.BodyHandlers.ofString()
            );

            System.out.println("Traduction status : " + response.statusCode());

            if (response.statusCode() == 200) {
                String body = response.body();
                String marker = "\"translatedText\":\"";
                int start = body.indexOf(marker);
                if (start != -1) {
                    start += marker.length();
                    int end = body.indexOf("\"", start);
                    if (end != -1) {
                        return body.substring(start, end);
                    }
                }
            }
            return null;

        } catch (Exception e) {
            System.out.println("Erreur traduction : " + e.getMessage());
            return null;
        }
    }

    public String detecterLangue(String texte) {
        // Détection simple basée sur les caractères
        // MyMemory ne nécessite pas de détection séparée
        // On utilise "auto" comme langue source
        return "auto";
    }
}