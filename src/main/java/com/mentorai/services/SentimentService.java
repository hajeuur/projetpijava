package com.mentorai.services;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Service d'analyse de sentiment des feedbacks
 * Utilise l'API Azure Text Analytics pour détecter si un feedback est positif, négatif ou neutre
 * API: Azure Cognitive Services - Text Analytics
 */
public class SentimentService {

    private static String AZURE_ENDPOINT;
    private static String AZURE_KEY;
    private static String API_URL;

    static {
        try {
            Properties props = new Properties();
            InputStream input = SentimentService.class.getClassLoader()
                    .getResourceAsStream("config.properties");
            if (input != null) {
                props.load(input);
                AZURE_ENDPOINT = props.getProperty("azure.sentiment.endpoint");
                AZURE_KEY = props.getProperty("azure.sentiment.key");
                API_URL = AZURE_ENDPOINT + "text/analytics/v3.1/sentiment";
                input.close();
            } else {
                System.err.println("❌ Fichier config.properties introuvable !");
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement config : " + e.getMessage());
        }
    }

    /**
     * Analyse le sentiment d'un texte avec Azure Text Analytics
     * @param texte Le contenu du feedback à analyser
     * @return "positif", "negatif" ou "neutre"
     */
    public String analyserSentiment(String texte) {
        try {
            String jsonBody = String.format(
                "{\"documents\": [{\"id\": \"1\", \"language\": \"fr\", \"text\": \"%s\"}]}",
                texte.replace("\"", "\\\"").replace("\n", " ")
            );

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Ocp-Apim-Subscription-Key", AZURE_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(
                    request, HttpResponse.BodyHandlers.ofString()
            );

            System.out.println("✅ Azure Sentiment API status : " + response.statusCode());

            if (response.statusCode() == 200) {
                String body = response.body();
                System.out.println("✅ Azure Sentiment API response : " + body);

                String sentiment = "neutre";
                if (body.contains("\"sentiment\":\"positive\"")) sentiment = "positif";
                else if (body.contains("\"sentiment\":\"negative\"")) sentiment = "negatif";
                else if (body.contains("\"sentiment\":\"neutral\"")) sentiment = "neutre";
                else if (body.contains("\"sentiment\":\"mixed\"")) sentiment = "neutre";
                
                return sentiment;
            }

            System.out.println("⚠️ Fallback vers analyse locale");
            return analyserSentimentLocal(texte);

        } catch (Exception e) {
            System.out.println("❌ Erreur Azure Sentiment API : " + e.getMessage());
            e.printStackTrace();
            return analyserSentimentLocal(texte);
        }
    }

    /**
     * Analyse détaillée du sentiment avec pourcentages
     * @param texte Le contenu à analyser
     * @return Map avec "sentiment", "positif", "neutre", "negatif", "recommandation", "explication"
     */
    public java.util.Map<String, String> analyserSentimentDetaille(String texte) {
        java.util.Map<String, String> resultat = new java.util.HashMap<>();
        
        try {
            String jsonBody = String.format(
                "{\"documents\": [{\"id\": \"1\", \"language\": \"fr\", \"text\": \"%s\"}]}",
                texte.replace("\"", "\\\"").replace("\n", " ")
            );

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Ocp-Apim-Subscription-Key", AZURE_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(
                    request, HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() == 200) {
                String body = response.body();
                
                String sentiment = "neutre";
                if (body.contains("\"sentiment\":\"positive\"")) sentiment = "positif";
                else if (body.contains("\"sentiment\":\"negative\"")) sentiment = "negatif";
                else if (body.contains("\"sentiment\":\"neutral\"")) sentiment = "neutre";
                else if (body.contains("\"sentiment\":\"mixed\"")) sentiment = "neutre";
                
                double scorePositif = extraireScore(body, "positive");
                double scoreNeutre = extraireScore(body, "neutral");
                double scoreNegatif = extraireScore(body, "negative");
                
                resultat.put("sentiment", sentiment);
                resultat.put("positif", String.format("%.0f", scorePositif * 100));
                resultat.put("neutre", String.format("%.0f", scoreNeutre * 100));
                resultat.put("negatif", String.format("%.0f", scoreNegatif * 100));
                resultat.put("recommandation", genererRecommandation(sentiment, scorePositif, scoreNegatif));
                resultat.put("explication", genererExplication(sentiment, scorePositif, scoreNegatif));
                
                return resultat;
            }

            return analyserSentimentDetailleLocal(texte);

        } catch (Exception e) {
            System.out.println("❌ Erreur analyse détaillée : " + e.getMessage());
            return analyserSentimentDetailleLocal(texte);
        }
    }

    private double extraireScore(String json, String type) {
        try {
            String pattern = "\"" + type + "\":";
            int index = json.indexOf(pattern);
            if (index == -1) return 0.0;
            
            int start = index + pattern.length();
            int end = start;
            while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.')) {
                end++;
            }
            
            String scoreStr = json.substring(start, end);
            return Double.parseDouble(scoreStr);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private String genererRecommandation(String sentiment, double scorePositif, double scoreNegatif) {
        if (sentiment.equals("positif") && scorePositif > 0.7) {
            return "✅ Utilisateur très satisfait - Poursuivre dans cette direction";
        } else if (sentiment.equals("positif")) {
            return "✅ Utilisateur satisfait - Maintenir la qualité";
        } else if (sentiment.equals("negatif") && scoreNegatif > 0.7) {
            return "⚠️ Utilisateur très mécontent - Action urgente requise";
        } else if (sentiment.equals("negatif")) {
            return "⚠️ Utilisateur mécontent - Traitement prioritaire";
        } else {
            return "ℹ️ Sentiment neutre - Réponse standard appropriée";
        }
    }

    public String genererExplication(String sentiment, double scorePositif, double scoreNegatif) {
        if (sentiment.equals("positif") && scorePositif > 0.7) {
            return "L'analyse révèle un sentiment très positif. L'utilisateur exprime une satisfaction élevée. " +
                   "Recommandation : Remerciez chaleureusement l'utilisateur et encouragez-le à partager son expérience. " +
                   "Une réponse personnalisée renforcera la fidélité client.";
        } else if (sentiment.equals("positif")) {
            return "Le feedback montre une satisfaction générale avec quelques nuances. " +
                   "Recommandation : Répondez avec enthousiasme tout en restant attentif aux points d'amélioration mentionnés. " +
                   "Proposez un suivi pour garantir une expérience optimale.";
        } else if (sentiment.equals("negatif") && scoreNegatif > 0.7) {
            return "Attention : Le sentiment est fortement négatif. L'utilisateur exprime une frustration importante. " +
                   "Recommandation : Réponse urgente requise. Présentez des excuses sincères, reconnaissez le problème " +
                   "et proposez une solution concrète immédiate (remboursement, geste commercial). Contactez l'utilisateur directement.";
        } else if (sentiment.equals("negatif")) {
            return "Le feedback révèle une insatisfaction qui nécessite une attention particulière. " +
                   "Recommandation : Répondez rapidement avec empathie. Expliquez les mesures correctives prises " +
                   "et proposez une compensation adaptée. Assurez un suivi personnalisé pour restaurer la confiance.";
        } else {
            return "Le sentiment est neutre, indiquant une expérience standard sans émotion forte. " +
                   "Recommandation : Répondez de manière professionnelle et courtoise. Remerciez l'utilisateur pour son retour " +
                   "et proposez des améliorations ou services complémentaires pour enrichir son expérience.";
        }
    }

    private String analyserSentimentLocal(String texte) {
        String texteLower = texte.toLowerCase();

        String[] motsPositifs = {
                "merci", "excellent", "super", "génial", "parfait", "bravo",
                "satisfait", "content", "heureux", "bien", "bon", "top",
                "love", "aime", "adore", "formidable", "magnifique"
        };

        String[] motsNegatifs = {
                "problème", "bug", "erreur", "mauvais", "nul", "horrible",
                "déçu", "mécontent", "triste", "pas", "ne marche pas",
                "lent", "frustré", "colère", "fâché", "urgent", "grave"
        };

        int scorePositif = 0;
        int scoreNegatif = 0;

        for (String mot : motsPositifs) {
            if (texteLower.contains(mot)) {
                scorePositif++;
            }
        }

        for (String mot : motsNegatifs) {
            if (texteLower.contains(mot)) {
                scoreNegatif++;
            }
        }

        if (scorePositif > scoreNegatif) {
            return "positif";
        } else if (scoreNegatif > scorePositif) {
            return "negatif";
        } else {
            return "neutre";
        }
    }

    private java.util.Map<String, String> analyserSentimentDetailleLocal(String texte) {
        java.util.Map<String, String> resultat = new java.util.HashMap<>();
        String sentiment = analyserSentimentLocal(texte);
        
        if (sentiment.equals("positif")) {
            resultat.put("positif", "75");
            resultat.put("neutre", "20");
            resultat.put("negatif", "5");
        } else if (sentiment.equals("negatif")) {
            resultat.put("positif", "5");
            resultat.put("neutre", "20");
            resultat.put("negatif", "75");
        } else {
            resultat.put("positif", "30");
            resultat.put("neutre", "50");
            resultat.put("negatif", "20");
        }
        
        resultat.put("sentiment", sentiment);
        resultat.put("recommandation", genererRecommandation(sentiment, 
            Double.parseDouble(resultat.get("positif")) / 100.0,
            Double.parseDouble(resultat.get("negatif")) / 100.0));
        resultat.put("explication", genererExplication(sentiment,
            Double.parseDouble(resultat.get("positif")) / 100.0,
            Double.parseDouble(resultat.get("negatif")) / 100.0));
        
        return resultat;
    }

    public String getCouleurSentiment(String sentiment) {
        return switch (sentiment) {
            case "positif" -> "#28a745";
            case "negatif" -> "#d52e28";
            case "neutre"  -> "#f0a500";
            default        -> "#888";
        };
    }

    public String getEmojiSentiment(String sentiment) {
        return switch (sentiment) {
            case "positif" -> "😊";
            case "negatif" -> "😞";
            case "neutre"  -> "😐";
            default        -> "❓";
        };
    }
}
