package edu.connection3a36.services;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Service pour interagir avec l'API Wikipedia.
 * Aide à expliquer des notions difficiles en récupérant des résumés.
 */
public class WikipediaService {

    private static final String API_URL = "https://fr.wikipedia.org/api/rest_v1/page/summary/";

    /**
     * Récupère le résumé d'une page Wikipedia par son titre.
     * @param title Le titre de la notion (ex: "Intelligence artificielle")
     * @return Le résumé textuel ou un message d'erreur.
     */
    public String getSummary(String title) throws Exception {
        if (title == null || title.trim().isEmpty()) {
            return "Veuillez entrer une notion à rechercher.";
        }

        String encodedTitle = URLEncoder.encode(title.trim().replace(" ", "_"), StandardCharsets.UTF_8);
        URL url = new URL(API_URL + encodedTitle);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "MentorAI/1.0 (https://esprit.tn; contact@esprit.tn)");
        conn.setConnectTimeout(5000); // 5 secondes
        conn.setReadTimeout(10000);   // 10 secondes

        int responseCode = conn.getResponseCode();
        if (responseCode == 404) {
            return "Désolé, aucune page Wikipedia n'a été trouvée pour la notion : " + title;
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
}
