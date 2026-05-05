package edu.connection3a36.services;

import org.json.JSONObject;
import javax.sound.sampled.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service d'enregistrement vocal et transcription via l'API Groq Whisper.
 * Fusionné pour supporter les appels synchrones et asynchrones.
 */
public class VoiceRecorderService {

    private static final String GROQ_WHISPER_URL = "https://api.groq.com/openai/v1/audio/transcriptions";
    private static final String MODEL = "whisper-large-v3-turbo";

    private TargetDataLine targetLine;
    private File audioFile;
    private boolean isRecording = false;

    private AudioFormat getAudioFormat() {
        return new AudioFormat(16000, 16, 1, true, false);
    }

    public void startRecording() {
        try {
            AudioFormat format = getAudioFormat();
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("Microphone non supporté");
                return;
            }

            targetLine = (TargetDataLine) AudioSystem.getLine(info);
            targetLine.open(format);
            targetLine.start();

            audioFile = new File(System.getProperty("java.io.tmpdir"), "mentorai_record_" + UUID.randomUUID() + ".wav");
            isRecording = true;

            new Thread(() -> {
                try (AudioInputStream audioStream = new AudioInputStream(targetLine)) {
                    AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, audioFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public File stopRecording() {
        if (targetLine != null) {
            targetLine.stop();
            targetLine.close();
            isRecording = false;
            return audioFile;
        }
        return null;
    }

    public String stopRecordingAndTranscribe() throws Exception {
        File file = stopRecording();
        if (file == null || !file.exists() || file.length() == 0) {
            throw new Exception("Erreur audio");
        }
        String result = transcribeSync(file);
        file.delete();
        return result;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public CompletableFuture<String> transcribe(File file) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return transcribeSync(file);
            } catch (Exception e) {
                return "Erreur transcription: " + e.getMessage();
            } finally {
                if (file != null && file.exists()) file.delete();
            }
        });
    }

    private String transcribeSync(File file) throws Exception {
        String apiKey = GroqService.getApiKey();
        String boundary = "----MentorAI" + UUID.randomUUID().toString().replace("-", "");

        URL url = new URL(GROQ_WHISPER_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, "UTF-8"), true)) {

            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"model\"\r\n\r\n");
            writer.append(MODEL).append("\r\n");

            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"language\"\r\n\r\n");
            writer.append("fr").append("\r\n");

            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(file.getName()).append("\"\r\n");
            writer.append("Content-Type: audio/wav\r\n\r\n");
            writer.flush();

            Files.copy(file.toPath(), os);
            os.flush();

            writer.append("\r\n--").append(boundary).append("--\r\n");
            writer.flush();
        }

        int status = conn.getResponseCode();
        if (status != 200) {
            throw new Exception("Erreur API Whisper: " + status);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return new JSONObject(sb.toString()).getString("text").trim();
        }
    }
}
