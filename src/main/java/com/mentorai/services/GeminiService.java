package com.mentorai.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GeminiService {

    private static final String API_KEY =
            "AIzaSyAe2WIxTiIL2PNMI2xKWt37QoMMFBLMSCU";

    private static final String URL =
            "https://generativelanguage.googleapis.com/v1/models/" +
                    "gemini-1.5-flash:generateContent?key=" + API_KEY;

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
        // ===== TENTATIVE GEMINI API =====
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

        } catch (Exception e) {
            System.out.println("Erreur Gemini API : " + e.getMessage());
        }

        // ===== FALLBACK INTELLIGENT (IA locale) =====
        System.out.println("Utilisation du systeme de reponse intelligente (fallback)");
        return genererReponseFallback(typeFeedback, note, message);
    }

    // ===== SYSTÈME DE RÉPONSE INTELLIGENTE (FALLBACK) =====
    private String genererReponseFallback(String typeFeedback, String noteStr, String message) {
        int note = Integer.parseInt(noteStr);
        String messageLower = message.toLowerCase();

        // Analyse des mots-clés dans le message
        boolean mentionneBug = messageLower.contains("bug") || messageLower.contains("erreur") 
                || messageLower.contains("marche pas") || messageLower.contains("ne fonctionne");
        boolean mentionneLent = messageLower.contains("lent") || messageLower.contains("slow") 
                || messageLower.contains("rapide");
        boolean mentionneInterface = messageLower.contains("interface") || messageLower.contains("design") 
                || messageLower.contains("ui");
        boolean mentionneContenu = messageLower.contains("contenu") || messageLower.contains("cours") 
                || messageLower.contains("ressource");

        // ===== PROBLÈME =====
        if (typeFeedback.equals("probleme")) {
            if (note <= 2) {
                if (mentionneBug) {
                    return "Nous sommes vraiment désolés pour ce bug technique. Notre équipe de développement " +
                            "a été immédiatement alertée et travaille activement sur une correction. " +
                            "Nous vous tiendrons informé de l'avancement et vous proposerons une compensation " +
                            "pour ce désagrément.";
                } else if (mentionneLent) {
                    return "Nous comprenons votre frustration concernant la lenteur de la plateforme. " +
                            "Nos équipes techniques analysent actuellement les performances du système. " +
                            "En attendant l'optimisation, nous vous offrons une prolongation d'abonnement " +
                            "en compensation.";
                } else {
                    return "Nous prenons très au sérieux votre retour et nous nous excusons sincèrement " +
                            "pour cette expérience négative. Notre équipe va analyser en détail votre situation " +
                            "et vous proposer une solution adaptée dans les plus brefs délais.";
                }
            } else {
                return "Merci d'avoir signalé ce problème. Bien que la situation ne semble pas critique, " +
                        "nous allons l'examiner attentivement pour améliorer votre expérience. " +
                        "N'hésitez pas à nous contacter si vous avez besoin d'assistance supplémentaire.";
            }
        }

        // ===== SUGGESTION =====
        if (typeFeedback.equals("suggestion")) {
            if (mentionneInterface) {
                return "Merci beaucoup pour cette suggestion concernant l'interface ! " +
                        "Votre retour est précieux pour améliorer l'expérience utilisateur. " +
                        "Nous allons transmettre votre idée à notre équipe design qui l'étudiera " +
                        "pour une future mise à jour.";
            } else if (mentionneContenu) {
                return "Excellente suggestion ! Nous apprécions vraiment votre engagement pour enrichir " +
                        "le contenu de la plateforme. Notre équipe pédagogique va étudier votre proposition " +
                        "et voir comment l'intégrer dans notre roadmap de développement.";
            } else {
                return "Nous vous remercions pour cette suggestion constructive. " +
                        "Votre contribution nous aide à faire évoluer MentorAI dans la bonne direction. " +
                        "Nous allons analyser votre proposition et vous tiendrons informé de sa mise en œuvre.";
            }
        }

        // ===== SATISFACTION =====
        if (typeFeedback.equals("satisfaction")) {
            if (note >= 4) {
                return "Merci infiniment pour ce retour positif ! Votre satisfaction est notre plus belle récompense. " +
                        "Nous sommes ravis que MentorAI réponde à vos attentes et nous continuerons à améliorer " +
                        "nos services pour vous offrir la meilleure expérience possible.";
            } else if (note == 3) {
                return "Merci pour votre retour. Nous sommes contents que l'expérience soit globalement positive, " +
                        "mais nous aimerions faire mieux ! N'hésitez pas à nous dire ce que nous pourrions améliorer " +
                        "pour atteindre l'excellence.";
            } else {
                return "Nous vous remercions pour votre honnêteté. Même si votre satisfaction n'est pas totale, " +
                        "nous apprécions que vous utilisiez MentorAI. Nous travaillons constamment à améliorer " +
                        "nos services et espérons vous satisfaire pleinement très bientôt.";
            }
        }

        // ===== RÉPONSE GÉNÉRIQUE =====
        return "Merci pour votre feedback. Nous prenons en compte tous les retours de nos utilisateurs " +
                "pour améliorer continuellement MentorAI. Notre équipe va analyser votre message " +
                "et vous recontactera si nécessaire.";
    }
}