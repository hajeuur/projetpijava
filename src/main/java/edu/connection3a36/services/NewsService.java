package edu.connection3a36.services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class NewsService {

    private static final String API_KEY = "2f957cb36949465b93e03be50f04a9ed";

    // Requêtes variées pour maximiser le nombre d'articles (sans filtre de langue)
    private static final String[] QUERIES = {
        "artificial intelligence technology",
        "cloud computing software",
        "java programming developers",
        "machine learning deep learning",
        "cybersecurity data privacy"
    };

    public static class NewsItem {
        private final String title;
        private final String url;
        private final String category;

        public NewsItem(String title, String url, String category) {
            this.title = title;
            this.url = url;
            this.category = category;
        }

        public String getTitle() { return title; }
        public String getUrl()   { return url; }
        public String getCategory() { return category; }
    }

    public List<NewsItem> getLatestTechNews() {
        HttpClient client = HttpClient.newHttpClient();

        // Catégories associées à chaque requête
        String[] categories = {"🤖 IA", "☁️ Cloud", "☕ Java", "🧠 Machine Learning", "🔐 Sécurité"};

        // Une liste par catégorie (bucket)
        List<List<NewsItem>> buckets = new ArrayList<>();
        for (int i = 0; i < QUERIES.length; i++) {
            buckets.add(new ArrayList<>());
        }

        // Remplir chaque bucket indépendamment
        for (int q = 0; q < QUERIES.length; q++) {
            try {
                String encodedQuery = QUERIES[q].replace(" ", "+");
                String url = "https://newsapi.org/v2/everything?q=" + encodedQuery
                        + "&sortBy=publishedAt&pageSize=6&apiKey=" + API_KEY;

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "Mozilla/5.0")
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JSONObject jsonResponse = new JSONObject(response.body());
                    if (jsonResponse.has("articles")) {
                        JSONArray articles = jsonResponse.getJSONArray("articles");
                        for (int i = 0; i < articles.length(); i++) {
                            JSONObject article = articles.getJSONObject(i);
                            String title = article.optString("title", "");
                            String articleUrl = article.optString("url", "");
                            String source = article.optJSONObject("source") != null
                                    ? article.getJSONObject("source").optString("name", "")
                                    : "";
                            // Ignorer les articles sans titre ou avec "[Removed]"
                            if (!title.isEmpty() && !title.equalsIgnoreCase("[Removed]") && !articleUrl.isEmpty()) {
                                buckets.get(q).add(new NewsItem(
                                        categories[q] + "  " + title + (source.isEmpty() ? "" : " ── " + source),
                                        articleUrl,
                                        categories[q]
                                ));
                            }
                        }
                    }
                } else {
                    System.err.println("NewsAPI HTTP " + response.statusCode() + " pour : " + QUERIES[q]);
                }
            } catch (Exception e) {
                System.err.println("Error fetching news for query [" + QUERIES[q] + "]: " + e.getMessage());
            }
        }

        // ─── Interleave round-robin ───────────────────────────────────────────
        List<NewsItem> interleaved = new ArrayList<>();
        boolean added = true;
        int round = 0;
        while (added) {
            added = false;
            for (List<NewsItem> bucket : buckets) {
                if (round < bucket.size()) {
                    interleaved.add(bucket.get(round));
                    added = true;
                }
            }
            round++;
        }

        return interleaved;
    }
}
