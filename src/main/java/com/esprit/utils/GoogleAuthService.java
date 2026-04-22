package com.esprit.utils;

import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class GoogleAuthService {

    private static final String CLIENT_ID     = "222819625196-9mai6tqr7ls12bnvl90lqjf0cuavhrdd.apps.googleusercontent.com";
    private static final String CLIENT_SECRET = "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";

    private static int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch (Exception e) {
            return 9090;
        }
    }

    public static GoogleUserInfo authenticate() throws Exception {
        int port = findFreePort();
        System.out.println(">>> Port : " + port);
        String redirectUri = "http://localhost:" + port + "/Callback";

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> codeRef = new AtomicReference<>();

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/Callback", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            if (query != null) {
                for (String param : query.split("&")) {
                    if (param.startsWith("code=")) {
                        codeRef.set(URLDecoder.decode(param.substring(5), StandardCharsets.UTF_8));
                        break;
                    }
                }
            }
            String html = "<html><body><h2>Connexion reussie ! Vous pouvez fermer cette fenetre.</h2></body></html>";
            exchange.sendResponseHeaders(200, html.getBytes().length);
            exchange.getResponseBody().write(html.getBytes());
            exchange.getResponseBody().close();
            latch.countDown();
        });
        server.start();

        // Ouvrir le navigateur
        String authUrl = "https://accounts.google.com/o/oauth2/auth"
                + "?client_id=" + CLIENT_ID
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                + "&response_type=code"
                + "&scope=" + URLEncoder.encode("email profile", StandardCharsets.UTF_8)
                + "&access_type=online";

        Desktop.getDesktop().browse(new URI(authUrl));

        latch.await();
        server.stop(1);

        String code = codeRef.get();
        if (code == null) throw new Exception("Code non reçu !");
        System.out.println(">>> Code OK, échange token...");

        // Échange code → token via HTTP pur (sans libs Google)
        String accessToken = exchangeCodeForToken(code, redirectUri);
        System.out.println(">>> Access token OK !");

        // Récupérer le profil
        return fetchUserInfo(accessToken);
    }

    private static String exchangeCodeForToken(String code, String redirectUri) throws Exception {
        String params = "code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                + "&client_id=" + URLEncoder.encode(CLIENT_ID, StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(CLIENT_SECRET, StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                + "&grant_type=authorization_code";

        URL url = new URL("https://oauth2.googleapis.com/token");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(params.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        System.out.println(">>> Token response status: " + status);

        InputStream is = status == 200 ? conn.getInputStream() : conn.getErrorStream();
        String response = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        System.out.println(">>> Token response: " + response);

        // Extraire access_token
        return extractField(response, "access_token");
    }

    private static GoogleUserInfo fetchUserInfo(String accessToken) throws Exception {
        URL url = new URL("https://www.googleapis.com/oauth2/v2/userinfo");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        String response = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        System.out.println(">>> UserInfo: " + response);

        GoogleUserInfo info = new GoogleUserInfo();
        info.setEmail(extractField(response, "email"));
        info.setNom(extractField(response, "family_name"));
        info.setPrenom(extractField(response, "given_name"));
        info.setPhotoUrl(extractField(response, "picture"));
        info.setGoogleId(extractField(response, "id"));

        System.out.println(">>> GoogleUserInfo OK: " + info);
        return info;
    }

    private static String extractField(String json, String field) {
        String key = "\"" + field + "\"";
        int idx = json.indexOf(key);
        if (idx == -1) return "";
        // Valeur peut être string ou number
        int colon = json.indexOf(":", idx + key.length());
        if (colon == -1) return "";
        int start = colon + 1;
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '"')) start++;
        int end = start;
        while (end < json.length() && json.charAt(end) != '"' && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
        return json.substring(start, end).trim();
    }
}