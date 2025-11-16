package com.blockbase;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

public class ApiClient {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final String baseUrl;
    private final String apiKey; // optional for step 5.4

    public ApiClient(String baseUrl, String apiKey) {
        this.baseUrl = trimTrailingSlash(defaultIfBlank(baseUrl, "http://localhost:3000/api"));
        this.apiKey = apiKey;
        this.httpClient = new OkHttpClient.Builder()
                .callTimeout(Duration.ofSeconds(15))
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(20))
                .build();
    }

    public static ApiClient fromConfig() {
        // Allow overriding via system properties or env, fallback to localhost
        String url = System.getProperty("BLOCKBASE_API_URL");
        if (isBlank(url)) {
            url = System.getenv("BLOCKBASE_API_URL");
        }
        String key = System.getProperty("BLOCKBASE_API_KEY");
        if (isBlank(key)) {
            key = System.getenv("BLOCKBASE_API_KEY");
        }
        return new ApiClient(url, key);
    }

    // --- Public endpoints (Step 5.1) ---

    // POST /api/repos
    public ApiResult createRepository(String repoId, String name, Map<String, Object> extra) {
        String json = buildJson(mapOf(
                "id", repoId,
                "name", name
        ), extra);
        return post("/repos", json);
    }

    // GET /api/repos/:id
    public ApiResult getRepository(String repoId) {
        return get("/repos/" + encode(repoId));
    }

    // POST /api/repos/:id/commits
    public ApiResult createCommit(String repoId, String commitId, String message, String author, String timestamp, String changesJsonArray) {
        // changesJsonArray is expected to be a raw JSON array string built elsewhere
        String json = "{"
                + "\"id\":\"" + escape(commitId) + "\","
                + "\"message\":\"" + escape(message) + "\","
                + "\"author\":\"" + escape(author) + "\","
                + "\"timestamp\":\"" + escape(timestamp) + "\","
                + "\"changes\":" + (changesJsonArray == null ? "[]" : changesJsonArray)
                + "}";
        return post("/repos/" + encode(repoId) + "/commits", json);
    }

    // GET /api/repos/:id/commits
    public ApiResult listCommits(String repoId) {
        return get("/repos/" + encode(repoId) + "/commits");
    }

    // --- HTTP helpers ---

    private ApiResult get(String path) {
        Request.Builder req = new Request.Builder().url(baseUrl + path);
        withCommonHeaders(req);
        try (Response resp = httpClient.newCall(req.build()).execute()) {
            return ApiResult.fromResponse(resp);
        } catch (IOException e) {
            return ApiResult.error(e.getMessage());
        }
    }

    private ApiResult post(String path, String jsonBody) {
        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request.Builder req = new Request.Builder().url(baseUrl + path).post(body);
        withCommonHeaders(req);
        try (Response resp = httpClient.newCall(req.build()).execute()) {
            return ApiResult.fromResponse(resp);
        } catch (IOException e) {
            return ApiResult.error(e.getMessage());
        }
    }

    private void withCommonHeaders(Request.Builder req) {
        req.header("Content-Type", "application/json");
        if (!isBlank(this.apiKey)) {
            req.header("Authorization", "Bearer " + this.apiKey);
        }
    }

    // --- Utils ---

    public static class ApiResult {
        public final boolean ok;
        public final int status;
        public final String body;
        public final String error;

        private ApiResult(boolean ok, int status, String body, String error) {
            this.ok = ok;
            this.status = status;
            this.body = body;
            this.error = error;
        }

        static ApiResult fromResponse(Response response) throws IOException {
            String respBody = response.body() != null ? response.body().string() : "";
            return new ApiResult(response.isSuccessful(), response.code(), respBody, null);
        }

        static ApiResult error(String message) {
            return new ApiResult(false, -1, null, message);
        }
    }

    private static String buildJson(Map<String, Object> base, Map<String, Object> extra) {
        // Minimal JSON builder for flat primitives and strings
        String jsonBase = base.entrySet().stream()
                .map(e -> "\"" + escape(e.getKey()) + "\":" + toJsonValue(e.getValue()))
                .collect(Collectors.joining(","));
        String jsonExtra = extra != null ? extra.entrySet().stream()
                .map(e -> "\"" + escape(e.getKey()) + "\":" + toJsonValue(e.getValue()))
                .collect(Collectors.joining(",")) : "";
        String combined = jsonBase + (jsonExtra.isEmpty() ? "" : ("," + jsonExtra));
        return "{" + combined + "}";
    }

    private static String toJsonValue(Object v) {
        if (v == null) return "null";
        if (v instanceof Number || v instanceof Boolean) {
            return String.valueOf(v);
        }
        return "\"" + escape(String.valueOf(v)) + "\"";
    }

    private static String encode(String s) {
        return s.replace(" ", "%20");
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String trimTrailingSlash(String s) {
        if (s == null) return null;
        if (s.endsWith("/")) return s.substring(0, s.length() - 1);
        return s;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String defaultIfBlank(String s, String def) {
        return isBlank(s) ? def : s;
    }

    @SafeVarargs
    private static <K, V> Map<K, V> mapOf(Object... kv) {
        // Simple builder: expects even number of args key, value,...
        java.util.LinkedHashMap<K, V> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            @SuppressWarnings("unchecked")
            K k = (K) kv[i];
            @SuppressWarnings("unchecked")
            V v = i + 1 < kv.length ? (V) kv[i + 1] : null;
            map.put(k, v);
        }
        return map;
    }
}


