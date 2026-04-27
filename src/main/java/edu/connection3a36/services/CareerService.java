package edu.connection3a36.services;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class CareerService {

    private static final String SCRIPT_PATH = getPythonScriptPath();

    private static String getPythonScriptPath() {
        try {
            URL resource = CareerService.class.getResource("/");
            if (resource != null) {
                Path classesDir = Paths.get(resource.toURI());
                Path projectRoot = classesDir.getParent().getParent();
                return projectRoot.resolve("python/career_predictor.py").toString();
            }
        } catch (Exception e) {}
        return "python/career_predictor.py";
    }

    public PredictionResult predict(String skills) {
        try {
            String python = "python"; // Ou chemin complet
            ProcessBuilder pb = new ProcessBuilder(python, SCRIPT_PATH, skills);
            pb.redirectErrorStream(false);
            Process process = pb.start();

            // Lire la sortie standard (JSON)
            StringBuilder stdout = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) stdout.append(line);
            }

            // Lire les erreurs (stderr) pour le debug
            StringBuilder stderr = new StringBuilder();
            try (BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = errReader.readLine()) != null) {
                    stderr.append(line).append("\n");
                    System.err.println("[Career Python] " + line);
                }
            }

            process.waitFor();
            String output = stdout.toString().trim();
            if (output.isEmpty()) {
                return new PredictionResult(false, "Erreur script Python vide.\nDetails: " + stderr.toString());
            }

            JSONObject json = new JSONObject(output);
            return new PredictionResult(json);

        } catch (Exception e) {
            return new PredictionResult(false, e.getMessage());
        }
    }

    public static class PredictionResult {
        public final boolean success;
        public final String error;
        public final List<PredictionItem> predictions = new ArrayList<>();

        public PredictionResult(boolean success, String error) {
            this.success = success;
            this.error = error;
        }

        public PredictionResult(JSONObject json) {
            this.success = json.optBoolean("success", false);
            this.error = json.optString("error", "");
            if (success) {
                var array = json.getJSONArray("predictions");
                for (int i = 0; i < array.length(); i++) {
                    predictions.add(new PredictionItem(array.getJSONObject(i)));
                }
            }
        }
    }

    public static class PredictionItem {
        public final String job;
        public final double score;
        public final String salary;
        public final String demand;

        public PredictionItem(JSONObject json) {
            this.job = json.getString("job");
            this.score = json.getDouble("score");
            this.salary = json.getString("salary");
            this.demand = json.getString("demand");
        }
    }
}
