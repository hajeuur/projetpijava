package edu.connection3a36.tools;

import org.json.JSONObject;

public class AIJsonParser {

    public static JSONObject extractFirstJsonObject(String raw) {
        if (raw == null) return null;
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) return null;
        try {
            return new JSONObject(raw.substring(start, end + 1));
        } catch (Exception ignored) {
            return null;
        }
    }

    public static String stripCodeFences(String raw) {
        if (raw == null) return "";
        String cleaned = raw.trim();
        if (cleaned.startsWith("```json")) cleaned = cleaned.substring(7).trim();
        else if (cleaned.startsWith("```")) cleaned = cleaned.substring(3).trim();
        if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
        return cleaned;
    }

    public static String extractMarkdownContent(String raw) {
        String cleaned = stripCodeFences(raw);
        JSONObject json = extractFirstJsonObject(cleaned);
        if (json != null && json.has("content_markdown")) {
            return json.optString("content_markdown", cleaned).trim();
        }
        return cleaned;
    }
}
