package edu.connection3a36.services;

import edu.connection3a36.entities.Etat;
import edu.connection3a36.entities.Objectif;
import edu.connection3a36.entities.Tache;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Fonctionnalite 6 — PlanificateurService
 * Genere un plan de travail journalier base sur toutes les taches encours.
 */
public class PlanificateurService {

    private final OllamaService ollamaService = new OllamaService();

    // ── Resultat ──────────────────────────────────────────────────────────────
    public static class CreneauPlan {
        public final String heure;
        public final String tache;
        public final String objectif;

        public CreneauPlan(String heure, String tache, String objectif) {
            this.heure    = heure;
            this.tache    = tache;
            this.objectif = objectif;
        }
    }

    public static class PlanResultat {
        public final List<CreneauPlan> plan;
        public final String conseil;

        public PlanResultat(List<CreneauPlan> plan, String conseil) {
            this.plan    = plan;
            this.conseil = conseil;
        }
    }

    /**
     * Genere un plan journalier a partir des taches encours de tous les objectifs.
     * Ne propage jamais d exception.
     */
    public PlanResultat genererPlan(List<Objectif> objectifs) {
        // Collecter toutes les taches encours avec leur deadline
        List<JSONObject> tachesAvecDeadline = new ArrayList<>();
        for (Objectif o : objectifs) {
            if (o.getProgramme() == null) continue;
            try {
                TacheService ts = new TacheService();
                List<Tache> taches = ts.getByProgramme(o.getProgramme().getId());
                for (Tache t : taches) {
                    if (t.getEtat() == Etat.encours) {
                        JSONObject obj = new JSONObject();
                        obj.put("tache", t.getTitre());
                        obj.put("objectif", o.getTitre());
                        long jours = o.getDatefin() != null
                                ? ChronoUnit.DAYS.between(LocalDate.now(), o.getDatefin()) : 30;
                        obj.put("jours_restants", jours);
                        tachesAvecDeadline.add(obj);
                    }
                }
            } catch (Exception ignored) {}
        }

        if (tachesAvecDeadline.isEmpty()) {
            return fallbackVide();
        }

        String tachesJson = new JSONArray(tachesAvecDeadline).toString();
        String prompt = """
                Voici toutes les taches en cours d un etudiant avec leurs deadlines : %s
                Genere un plan de travail realiste pour aujourd hui.
                Reponds UNIQUEMENT en JSON valide, sans texte avant ni apres :
                {"plan": [{"heure": "9h-10h", "tache": "...", "objectif": "..."}], "conseil": "..."}
                """.formatted(tachesJson);

        try {
            String response = ollamaService.appeler(prompt);
            return parseReponse(response, tachesAvecDeadline);
        } catch (Exception e) {
            System.err.println("PlanificateurService — Ollama indisponible : " + e.getMessage());
            return fallback(tachesAvecDeadline);
        }
    }

    // ── Parsing ───────────────────────────────────────────────────────────────
    private PlanResultat parseReponse(String response, List<JSONObject> taches) {
        if (response == null || response.isBlank()) return fallback(taches);
        try {
            int start = response.indexOf('{');
            int end   = response.lastIndexOf('}');
            if (start == -1 || end == -1) return fallback(taches);

            JSONObject json = new JSONObject(response.substring(start, end + 1));
            String conseil = json.optString("conseil", "Bonne journee de travail !");
            List<CreneauPlan> plan = new ArrayList<>();

            JSONArray arr = json.optJSONArray("plan");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject item = arr.optJSONObject(i);
                    if (item != null) {
                        plan.add(new CreneauPlan(
                                item.optString("heure", "—"),
                                item.optString("tache", "—"),
                                item.optString("objectif", "—")
                        ));
                    }
                }
            }
            return plan.isEmpty() ? fallback(taches) : new PlanResultat(plan, conseil);
        } catch (Exception e) {
            return fallback(taches);
        }
    }

    // ── Fallback logique pure ─────────────────────────────────────────────────
    private PlanResultat fallback(List<JSONObject> taches) {
        String[] heures = {"9h-10h", "10h-11h", "11h-12h", "14h-15h", "15h-16h", "16h-17h"};
        List<CreneauPlan> plan = new ArrayList<>();
        for (int i = 0; i < Math.min(taches.size(), heures.length); i++) {
            JSONObject t = taches.get(i);
            plan.add(new CreneauPlan(
                    heures[i],
                    t.optString("tache", "Tache " + (i + 1)),
                    t.optString("objectif", "—")
            ));
        }
        return new PlanResultat(plan, "Concentrez-vous sur une tache a la fois pour maximiser votre productivite.");
    }

    private PlanResultat fallbackVide() {
        return new PlanResultat(new ArrayList<>(), "Aucune tache en cours. Ajoutez des taches a vos objectifs !");
    }
}
