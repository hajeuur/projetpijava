package edu.connection3a36.services;

import org.json.JSONObject;
import org.json.JSONArray;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class PythonIAIService {

    private static final String PYTHON_EXE = "python"; // Assumes python is in PATH
    private static final String SCRIPT_PATH = "python/skill_gap_analyzer.py";

    public static JSONObject analyzeSkillGap(List<String> userSkills, String targetJob) {
        try {
            JSONObject inputJson = new JSONObject();
            inputJson.put("skills", new JSONArray(userSkills));
            inputJson.put("job", targetJob);

            ProcessBuilder pb = new ProcessBuilder(PYTHON_EXE, SCRIPT_PATH);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Write JSON to stdin
            try (java.io.OutputStream os = process.getOutputStream()) {
                os.write(inputJson.toString().getBytes("UTF-8"));
                os.flush();
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return new JSONObject(output.toString());
            } else {
                return new JSONObject().put("error", "Script Python a échoué (" + exitCode + "). Sortie: " + output.toString());
            }

        } catch (Exception e) {
            return new JSONObject().put("error", "Échec de connexion avec l'IA Python : " + e.getMessage());
        }
    }
}
