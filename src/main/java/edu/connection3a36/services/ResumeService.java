package edu.connection3a36.services;

import edu.connection3a36.entities.Tache;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * Fonctionnalite 1 — ResumeService
 * Genere un resume narratif de l avancement d un programme via Ollama.
 */
public class ResumeService {

    private final OllamaService ollamaService = new OllamaService();

    // ── Resultat structuré ────────────────────────────────────────────────────
    public static class ResumeResultat {
        public final String resume;
        public final String pointPositif;
        public final String conseil;

        public ResumeResultat(String resume, String pointPositif, String conseil) {
            this.resume = resume;
            this.pointPositif = pointPositif;
            this.conseil = conseil;
        }
    }

    /**
     * Genere un resume d avancement. Ne propage jamais d exception — retourne un fallback si Ollama est indisponible.
     */
    public ResumeResultat genererResume(String objectifTitre, int scorePourcentage, List<Tache> taches) {
        String tachesJson = buildTachesJson(taches);
        String prompt = """
                Tu es un assistant pedagogique.
                Objectif : %s
                Score : %d%%
                Taches : %s
                Redige un resume d avancement en 3 phrases, encourageant et factuel.
                Reponds UNIQUEMENT en JSON valide, sans texte avant ni apres :
                {"resume": "...", "point_positif": "...", "conseil": "..."}
                """.formatted(objectifTitre, scorePourcentage, tachesJson);
        try {
            String response = ollamaService.appeler(prompt);
            return parseReponse(response, objectifTitre, scorePourcentage);
        } catch (Exception e) {
            System.err.println("ResumeService — Ollama indisponible : " + e.getMessage());
            return fallback(objectifTitre, scorePourcentage);
        }
    }

    // ── Parsing JSON ──────────────────────────────────────────────────────────
    private ResumeResultat parseReponse(String response, String titre, int score) {
        if (response == null || response.isBlank()) return fallback(titre, score);
        try {
            int start = response.indexOf('{');
            int end   = response.lastIndexOf('}');
            if (start == -1 || end == -1) return fallback(titre, score);
            JSONObject json = new JSONObject(response.substring(start, end + 1));
            return new ResumeResultat(
                    json.optString("resume",        "Avancement en cours."),
                    json.optString("point_positif", "Bonne progression."),
                    json.optString("conseil",       "Continuez ainsi !")
            );
        } catch (Exception e) {
            return fallback(titre, score);
        }
    }

    // ── Fallback si Ollama indisponible ───────────────────────────────────────
    private ResumeResultat fallback(String titre, int score) {
        String resume = "L objectif \"" + titre + "\" progresse avec un score de " + score + "%.";
        String point  = score >= 50 ? "Plus de la moitie du chemin est parcouru !"
                                    : "Les premieres taches ont ete lancees, continuez !";
        String conseil = score < 50 ? "Concentrez-vous sur les taches en cours."
                : score < 80 ? "Vous etes sur la bonne voie, finalisez les taches restantes."
                : "Excellent ! Validez les dernieres taches pour atteindre l objectif.";
        return new ResumeResultat(resume, point, conseil);
    }

    // ── Construction JSON des taches ──────────────────────────────────────────
    private String buildTachesJson(List<Tache> taches) {
        if (taches == null || taches.isEmpty()) return "[]";
        JSONArray arr = new JSONArray();
        for (Tache t : taches) {
            JSONObject obj = new JSONObject();
            obj.put("titre", t.getTitre());
            obj.put("etat", t.getEtat() != null ? t.getEtat().getValue() : "encours");
            arr.put(obj);
        }
        return arr.toString();
    }
}
