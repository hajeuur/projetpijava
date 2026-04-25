package edu.connection3a36.services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class OllamaService {

    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String MODEL = "llama3.1:8b";

    public List<String[]> genererTaches(String objectifTitre, String objectifDescription) throws Exception {
        String prompt = """
                Tu es un coach personnel expert en productivite. Un utilisateur a defini l objectif suivant :
                Titre : %s
                Description : %s
                Genere exactement 5 taches concretes, ordonnees et progressives pour atteindre cet objectif.
                Reponds UNIQUEMENT avec un tableau JSON, sans texte avant ni apres, au format :
                [{"titre": "Titre court", "description": "Description detaillee"}, ...]
                """.formatted(objectifTitre, objectifDescription != null ? objectifDescription : "");
        return parseTachesJson(callOllama(prompt));
    }

    public String genererMessageMotivant(String objectifTitre, int scoreCourant, int scorePrec, LocalDate datefin) throws Exception {
        long joursRestants = datefin != null ? ChronoUnit.DAYS.between(LocalDate.now(), datefin) : -1;
        int evolution = scoreCourant - scorePrec;
        String contexteDeadline = joursRestants < 0 ? "La deadline est depassee."
                : joursRestants == 0 ? "C est le dernier jour !"
                : "Il reste " + joursRestants + " jour(s) avant la deadline.";
        String contexteEvolution = evolution > 0 ? "Progression +"+evolution+"% depuis la derniere fois."
                : evolution < 0 ? "Progression -"+Math.abs(evolution)+"% depuis la derniere fois."
                : "Progression stable.";
        String prompt = """
                Tu es un coach bienveillant. Genere un message d encouragement personnalise en francais (2-3 phrases max).
                Objectif : %s | Score : %d%% | %s | %s
                Reponds UNIQUEMENT avec le message.
                """.formatted(objectifTitre, scoreCourant, contexteDeadline, contexteEvolution);
        return callOllama(prompt).trim();
    }

    private String callOllama(String prompt) throws Exception {
        URL url = new URL(OLLAMA_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(120000);
        JSONObject body = new JSONObject();
        body.put("model", MODEL);
        body.put("prompt", prompt);
        body.put("stream", false);
        body.put("options", new JSONObject().put("temperature", 0.7).put("num_predict", 512));
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }
        int status = conn.getResponseCode();
        if (status != 200) {
            BufferedReader err = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = err.readLine()) != null) sb.append(line);
            throw new Exception("Erreur Ollama (" + status + "): " + sb);
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        return new JSONObject(sb.toString()).getString("response");
    }

    private List<String[]> parseTachesJson(String response) {
        List<String[]> taches = new ArrayList<>();
        try {
            int start = response.indexOf('[');
            int end = response.lastIndexOf(']');
            if (start == -1 || end == -1) return fallbackTaches();
            JSONArray arr = new JSONArray(response.substring(start, end + 1));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                taches.add(new String[]{obj.optString("titre", "Tache " + (i+1)), obj.optString("description", "")});
            }
        } catch (Exception e) { return fallbackTaches(); }
        return taches;
    }

    private List<String[]> fallbackTaches() {
        List<String[]> list = new ArrayList<>();
        list.add(new String[]{"Definir le plan d action", "Identifier les etapes cles."});
        list.add(new String[]{"Rassembler les ressources", "Collecter les outils necessaires."});
        list.add(new String[]{"Demarrer la premiere etape", "Commencer par la tache la plus simple."});
        list.add(new String[]{"Suivre la progression", "Faire un point regulier."});
        list.add(new String[]{"Finaliser et valider", "Verifier que l objectif est atteint."});
        return list;
    }
}
