package edu.connection3a36.services;

import javax.sound.sampled.*;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class VoiceRecorderService {

    private static final String API_URL = "https://api.groq.com/openai/v1/audio/transcriptions";
    private TargetDataLine targetDataLine;
    private File audioFile;
    private boolean isRecording = false;

    private AudioFormat getAudioFormat() {
        float sampleRate = 16000;
        int sampleSizeInBits = 16;
        int channels = 1; // Mono
        boolean signed = true;
        boolean bigEndian = false;
        return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }

    public void startRecording() {
        try {
            AudioFormat format = getAudioFormat();
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("Line not supported");
                return;
            }

            targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
            targetDataLine.open(format);
            targetDataLine.start();

            audioFile = new File("temp_voice_" + UUID.randomUUID().toString() + ".wav");
            isRecording = true;

            Thread recordingThread = new Thread(() -> {
                try (AudioInputStream ais = new AudioInputStream(targetDataLine)) {
                    AudioSystem.write(ais, AudioFileFormat.Type.WAVE, audioFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            recordingThread.start();
            System.out.println("Recording started: " + audioFile.getAbsolutePath());

        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public File stopRecording() {
        if (targetDataLine != null) {
            targetDataLine.stop();
            targetDataLine.close();
            isRecording = false;
            System.out.println("Recording stopped.");
            return audioFile;
        }
        return null;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public CompletableFuture<String> transcribe(File file) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String apiKey = getApiKey();
                if (apiKey == null || apiKey.isEmpty()) {
                    return "Erreur: Clé API Groq manquante dans config.properties";
                }

                String boundary = "---" + UUID.randomUUID().toString();
                byte[] body = createMultipartBody(file, boundary);

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    org.json.JSONObject json = new org.json.JSONObject(response.body());
                    return json.getString("text");
                } else {
                    System.err.println("Groq Transcription Error: " + response.body());
                    return "Erreur lors de la transcription: " + response.statusCode();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return "Erreur technique: " + e.getMessage();
            } finally {
                // Cleanup temporary file
                if (file != null && file.exists()) {
                    file.delete();
                }
            }
        });
    }

    private String getApiKey() {
        Properties props = new Properties();
        try (InputStream is = new FileInputStream("config.properties")) {
            props.load(is);
            return props.getProperty("GROQ_API_KEY");
        } catch (IOException e) {
            System.err.println("Could not load config.properties: " + e.getMessage());
            // Fallback to hardcoded key from GroqService if available, for testing
            return "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"; 
        }
    }

    private byte[] createMultipartBody(File file, String boundary) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, "UTF-8"), true);

        // Add model parameter
        writer.append("--").append(boundary).append("\r\n");
        writer.append("Content-Disposition: form-data; name=\"model\"\r\n\r\n");
        writer.append("whisper-large-v3-turbo").append("\r\n");

        // Add language parameter
        writer.append("--").append(boundary).append("\r\n");
        writer.append("Content-Disposition: form-data; name=\"language\"\r\n\r\n");
        writer.append("fr").append("\r\n");

        // Add file
        writer.append("--").append(boundary).append("\r\n");
        writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(file.getName()).append("\"\r\n");
        writer.append("Content-Type: audio/wav\r\n\r\n");
        writer.flush();

        Files.copy(file.toPath(), baos);
        baos.flush();

        writer.append("\r\n");
        writer.append("--").append(boundary).append("--\r\n");
        writer.flush();

        return baos.toByteArray();
    }
}
