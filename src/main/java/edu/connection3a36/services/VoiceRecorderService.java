package edu.connection3a36.services;

import org.json.JSONObject;

import javax.sound.sampled.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.UUID;

/**
 * Service d'enregistrement vocal et transcription via l'API Groq Whisper.
 */
public class VoiceRecorderService {

    private static final String GROQ_WHISPER_URL = "https://api.groq.com/openai/v1/audio/transcriptions";
    private static final String MODEL = "whisper-large-v3-turbo";

    private TargetDataLine targetLine;
    private File audioFile;
    private boolean isRecording = false;

    public void startRecording() throws LineUnavailableException {
        AudioFormat format = new AudioFormat(16000, 16, 1, true, true);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("Microphone non supporté ou introuvable.");
        }

        targetLine = (TargetDataLine) AudioSystem.getLine(info);
        targetLine.open(format);
        targetLine.start();

        audioFile = new File(System.getProperty("java.io.tmpdir"),
                "mentorai_record_" + UUID.randomUUID() + ".wav");
        isRecording = true;

        new Thread(() -> {
            try (AudioInputStream audioStream = new AudioInputStream(targetLine)) {
                AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, audioFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public String stopRecordingAndTranscribe() throws Exception {
        if (!isRecording) return "";
        isRecording = false;
        targetLine.stop();
        targetLine.close();

        if (audioFile == null || !audioFile.exists() || audioFile.length() == 0) {
            throw new Exception("Erreur : Le fichier audio est vide ou inexistant.");
        }

        String result = transcribeWithGroq(audioFile);
        audioFile.delete();
        return result;
    }

    private String transcribeWithGroq(File file) throws Exception {
        String apiKey = GroqService.getApiKey();
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("dummy_key")) {
            throw new Exception("Clé API Groq introuvable ou invalide.");
        }

        String boundary = "----MentorAI" + UUID.randomUUID().toString().replace("-", "");

        URL url = new URL(GROQ_WHISPER_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);

        try (OutputStream os = conn.getOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, "UTF-8"), true)) {

            // model
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"model\"\r\n\r\n");
            writer.append(MODEL).append("\r\n");

            // language
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"language\"\r\n\r\n");
            writer.append("fr").append("\r\n");

            // file
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                  .append(file.getName()).append("\"\r\n");
            writer.append("Content-Type: audio/wav\r\n\r\n");
            writer.flush();

            Files.copy(file.toPath(), os);
            os.flush();

            writer.append("\r\n--").append(boundary).append("--\r\n");
            writer.flush();
        }

        int status = conn.getResponseCode();
        if (status != 200) {
            try (BufferedReader err = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), "UTF-8"))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = err.readLine()) != null) sb.append(line);
                throw new Exception("Erreur Whisper (" + status + "): " + sb);
            }
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return new JSONObject(sb.toString()).getString("text").trim();
        }
    }

    public boolean isRecording() {
        return isRecording;
    }
}
