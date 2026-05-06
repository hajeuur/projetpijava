package com.mentorai.services;

import com.mentorai.repositories.PackEtudeRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.net.http.HttpTimeoutException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.mentorai.utils.PackEtudeDebugger;

public class PackEtudeService {

    private final PackEtudeRepository repository = new PackEtudeRepository();
    private static final String N8N_WEBHOOK_URL = "http://127.0.0.1:5678/webhook-test/study";

    // Returns a JSONObject containing { "level": "...", "explanation": "..." }
    public JSONObject computeLevelAndExplanation(int userId, PackEtudeDebugger debugger) {
        debugger.startStep("FETCH user data (humeur + planning)");
        Map<String, Object> profil = repository.getProfil(userId);
        if (profil == null) {
            debugger.logWarn("Aucun profil trouvé.");
            debugger.endStep();
            return new JSONObject().put("level", "intermédiaire").put("explanation", "Aucun profil trouvé, niveau par défaut appliqué.");
        }

        Object profilIdObj = profil.get("id");
        if (profilIdObj == null) {
            return new JSONObject().put("level", "intermédiaire").put("explanation", "ID profil manquant, niveau par défaut appliqué.");
        }
        int profilId = ((Number) profilIdObj).intValue();
        
        Map<String, Object> humeur = repository.getLatestHumeur(profilId);
        double completionRate = repository.getCompletionRate(userId);

        int moodScore = 5;
        String trend = "stable";
        String risk = "modéré";
        if (humeur != null) {
            Object val = humeur.get("valeur_humeur");
            if (val instanceof Number) moodScore = ((Number) val).intValue();
            trend = (String) humeur.get("tendance");
            risk = (String) humeur.get("niveau_risque");
        }

        String speed = (String) profil.get("vitesse_apprentissage");
        String concentration = (String) profil.get("niveau_concentration");

        String level = "intermédiaire";

        // Débutant checks
        if (moodScore <= 4 || "élevé".equalsIgnoreCase(risk) || "faible".equalsIgnoreCase(concentration) || completionRate < 0.4) {
            level = "débutant";
        } 
        // Avancé checks
        else if (moodScore >= 7 && "hausse".equalsIgnoreCase(trend) && "rapide".equalsIgnoreCase(speed) && completionRate >= 0.75) {
            level = "avancé";
        }

        int completionRatePercent = (int) (completionRate * 100);
        String explanation = String.format("Nous avons choisi le niveau %s car votre humeur est de %d/10, votre tendance est %s, et votre taux de complétion récent est de %d%%.",
                level, moodScore, trend, completionRatePercent);

        debugger.endStep();

        JSONObject result = new JSONObject();
        result.put("level", level);
        result.put("explanation", explanation);
        result.put("profil", profil);
        if (humeur != null) {
            result.put("humeur", humeur);
        }
        
        return result;
    }

    public CompletableFuture<JSONObject> generatePack(int userId, String topic, String language, File file) {
        return CompletableFuture.supplyAsync(() -> {
            PackEtudeDebugger debugger = new PackEtudeDebugger();
            debugger.logInfo("START request pour le sujet : " + topic);
            
            try {
                JSONObject result = executeRequestWithRetry(userId, topic, language, file, 0, debugger);
                debugger.endExecution();
                return result;
            } catch (Exception e) {
                debugger.logError(e);
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                throw new RuntimeException(e.getMessage(), e);
            }
        });
    }

    private JSONObject executeRequestWithRetry(int userId, String topic, String language, File file, int attempt, PackEtudeDebugger debugger) throws Exception {
        try {
            JSONObject levelInfo = computeLevelAndExplanation(userId, debugger);
            JSONObject payload = buildPayload(topic, language, file, levelInfo, debugger);

            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(N8N_WEBHOOK_URL))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            System.out.println("PAYLOAD: " + payload.toString());
            
            debugger.startStep("SEND HTTP request to n8n & WAIT for response");
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            debugger.endStep();

            System.out.println("N8N RESPONSE: " + response.body());

            debugger.startStep("RECEIVE response & PARSE response");
            if (response.statusCode() != 200) {
                throw new RuntimeException("Le service n8n a répondu avec une erreur (Code: " + response.statusCode() + ")");
            }

            JSONObject n8nResponse = new JSONObject(response.body());
            n8nResponse.put("computed_explanation", levelInfo.getString("explanation"));
            debugger.endStep();
            
            return n8nResponse;

        } catch (HttpTimeoutException e) {
            if (attempt < 1) { // 1 retry max
                debugger.logWarn("Timeout détecté. Tentative de secours (Retry 1/1)...");
                return executeRequestWithRetry(userId, topic, language, file, attempt + 1, debugger);
            }
            throw new RuntimeException("Délai d'attente dépassé (n8n). Le serveur met trop de temps à répondre.", e);
        } catch (IOException e) {
            throw new RuntimeException("Erreur de communication réseau : " + e.getMessage(), e);
        }
    }

    private JSONObject buildPayload(String topic, String language, File file, JSONObject levelInfo, PackEtudeDebugger debugger) throws IOException {
        debugger.startStep("BUILD JSON payload");
        JSONObject payload = new JSONObject();
        payload.put("topic", topic);
        payload.put("level", levelInfo.getString("level"));
        payload.put("language", language);

        String fileContentStr = null;
        if (file != null && file.exists()) {
            if (file.getName().toLowerCase().endsWith(".pdf")) {
                try (PDDocument document = PDDocument.load(file)) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    fileContentStr = stripper.getText(document);
                } catch (Exception e) {
                    debugger.logWarn("Impossible de lire le PDF: " + e.getMessage());
                }
            } else {
                debugger.logWarn("Fichier non supporté pour l'extraction de texte (seul PDF est géré pour le moment).");
            }
        }
        
        payload.put("file_content", fileContentStr == null ? JSONObject.NULL : fileContentStr);

        debugger.endStep();
        return payload;
    }
}
