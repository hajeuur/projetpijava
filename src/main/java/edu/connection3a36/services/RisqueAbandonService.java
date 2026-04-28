package edu.connection3a36.services;

import edu.connection3a36.entities.Etat;
import edu.connection3a36.entities.Tache;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Fonctionnalite 2 — RisqueAbandonService
 * Detecte si les taches abandonnees mettent en danger l objectif via Ollama.
 */
public class RisqueAbandonService {

    private final OllamaService ollamaService = new OllamaService();

    // ── Resultat structuré ────────────────────────────────────────────────────
    public static class RisqueResultat {
        public final boolean risque;
        public final List<String> tachesARelancer;
        public final String conseil;

        public RisqueResultat(boolean risque, List<String> tachesARelancer, String conseil) {
            this.risque = risque;
            this.tachesARelancer = tachesARelancer;
            this.conseil = conseil;
        }
    }

    /**
     * Analyse les taches abandonnees et retourne un resultat de risque.
     * Ne propage jamais d exception.
     */
    public RisqueResultat analyserRisque(List<Tache> toutesLesTaches, int scorePourcentage, LocalDate datefin) {
        List<Tache> abandonnees = toutesLesTaches.stream()
                .filter(t -> t.getEtat() == Etat.Abandonner)
                .toList();

        if (abandonnees.isEmpty()) {
            return new RisqueResultat(false, new ArrayList<>(), "Aucune tache abandonnee, continuez ainsi !");
        }

        long joursRestants = datefin != null ? ChronoUnit.DAYS.between(LocalDate.now(), datefin) : 30;
        String tachesJson = buildTachesJson(abandonnees);

        String prompt = """
                Un etudiant a abandonne ces taches : %s
                Score actuel : %d%%, Jours restants : %d
                Ces abandons mettent-ils en danger son objectif ?
                Reponds UNIQUEMENT en JSON valide, sans texte avant ni apres :
                {"risque": "Oui/Non", "taches_a_relancer": ["titre1", "titre2"], "conseil": "..."}
                """.formatted(tachesJson, scorePourcentage, joursRestants);

        try {
            String response = ollamaService.appeler(prompt);
            return parseReponse(response, abandonnees);
        } catch (Exception e) {
            System.err.println("RisqueAbandonService — Ollama indisponible : " + e.getMessage());
            return fallback(abandonnees, scorePourcentage, joursRestants);
        }
    }

    // ── Parsing JSON ──────────────────────────────────────────────────────────
    private RisqueResultat parseReponse(String response, List<Tache> abandonnees) {
        if (response == null || response.isBlank()) return fallback(abandonnees, 0, 0);
        try {
            int start = response.indexOf('{');
            int end   = response.lastIndexOf('}');
            if (start == -1 || end == -1) return fallback(abandonnees, 0, 0);

            JSONObject json = new JSONObject(response.substring(start, end + 1));
            boolean risque = "Oui".equalsIgnoreCase(json.optString("risque", "Non"));
            String conseil = json.optString("conseil", "Relancez les taches abandonnees.");

            List<String> aRelancer = new ArrayList<>();
            JSONArray arr = json.optJSONArray("taches_a_relancer");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) aRelancer.add(arr.optString(i));
            }
            return new RisqueResultat(risque, aRelancer, conseil);
        } catch (Exception e) {
            return fallback(abandonnees, 0, 0);
        }
    }

    // ── Fallback ──────────────────────────────────────────────────────────────
    private RisqueResultat fallback(List<Tache> abandonnees, int score, long jours) {
        boolean risque = !abandonnees.isEmpty() && (score < 50 || jours < 7);
        List<String> aRelancer = abandonnees.stream().map(Tache::getTitre).limit(3).toList();
        String conseil = risque
                ? "Attention : des taches abandonnees menacent votre objectif. Relancez-les rapidement."
                : "Quelques taches sont abandonnees. Pensez a les relancer pour ameliorer votre score.";
        return new RisqueResultat(risque, aRelancer, conseil);
    }

    // ── Construction JSON ─────────────────────────────────────────────────────
    private String buildTachesJson(List<Tache> taches) {
        JSONArray arr = new JSONArray();
        for (Tache t : taches) {
            JSONObject obj = new JSONObject();
            obj.put("titre", t.getTitre());
            obj.put("etat", "Abandonner");
            arr.put(obj);
        }
        return arr.toString();
    }
}
