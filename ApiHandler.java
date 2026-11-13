package com.musix.server;

import com.musix.model.Mood;
import com.musix.model.Playlist;
import com.musix.model.Song;
import com.musix.model.User;
import com.musix.service.MoodAnalyzerService;
import com.musix.service.MusicService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ApiHandler implements HttpHandler {
    private final MusicService musicService = MusicService.getInstance();
    private final MoodAnalyzerService moodAnalyzer = MoodAnalyzerService.getInstance();
    private final Map<String, User> users = new HashMap<>();

    public ApiHandler() {
        // Default demo user
        users.put("lucky@musix.ai", new User("u-1", "Lucky", "lucky@musix.ai", "password123", "assets/covers/happy_1.svg"));
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Enable CORS
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        URI uri = exchange.getRequestURI();
        String path = uri.getPath();
        String method = exchange.getRequestMethod().toUpperCase();

        try {
            if (path.equals("/api/status")) {
                handleStatus(exchange);
            } else if (path.equals("/api/songs") && method.equals("GET")) {
                handleGetAllSongs(exchange);
            } else if (path.startsWith("/api/songs/mood/") && method.equals("GET")) {
                String moodStr = path.substring("/api/songs/mood/".length());
                handleGetSongsByMood(exchange, moodStr);
            } else if (path.equals("/api/mood/analyze") && method.equals("POST")) {
                handleAnalyzeMood(exchange);
            } else if (path.startsWith("/api/search") && method.equals("GET")) {
                handleSearch(exchange, uri.getQuery());
            } else if (path.equals("/api/playlists") && method.equals("GET")) {
                handleGetPlaylists(exchange);
            } else if (path.equals("/api/playlists") && method.equals("POST")) {
                handleCreatePlaylist(exchange);
            } else if (path.startsWith("/api/playlists/") && path.contains("/songs") && method.equals("POST")) {
                handleAddSongToPlaylist(exchange, path);
            } else if (path.equals("/api/like") && method.equals("POST")) {
                handleToggleLike(exchange);
            } else if (path.equals("/api/liked") && method.equals("GET")) {
                handleGetLikedSongs(exchange);
            } else if (path.equals("/api/recently-played") && method.equals("GET")) {
                handleGetRecentlyPlayed(exchange);
            } else if (path.equals("/api/recently-played") && method.equals("POST")) {
                handleRecordRecentlyPlayed(exchange);
            } else if (path.equals("/api/auth/login") && method.equals("POST")) {
                handleLogin(exchange);
            } else if (path.equals("/api/auth/register") && method.equals("POST")) {
                handleRegister(exchange);
            } else {
                sendJson(exchange, 404, "{\"error\":\"API route not found: " + path + "\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, "{\"error\":\"" + JsonUtils.escape(e.getMessage()) + "\"}");
        }
    }

    private void handleStatus(HttpExchange exchange) throws IOException {
        String json = "{\"status\":\"online\",\"app\":\"MUSIX\",\"theme\":\"Midnight Purple\",\"totalSongs\":" +
                musicService.getAllSongs().size() + ",\"version\":\"2.0.0\"}";
        sendJson(exchange, 200, json);
    }

    private void handleGetAllSongs(HttpExchange exchange) throws IOException {
        List<Song> songs = musicService.getAllSongs();
        sendJson(exchange, 200, JsonUtils.songsToJson(songs));
    }

    private void handleGetSongsByMood(HttpExchange exchange, String moodStr) throws IOException {
        Mood mood = Mood.fromString(moodStr);
        List<Song> songs = musicService.getSongsByMood(mood);
        sendJson(exchange, 200, JsonUtils.songsToJson(songs));
    }

    private void handleAnalyzeMood(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        Map<String, String> data = JsonUtils.parseSimpleJson(body);
        String moodStr = data.getOrDefault("mood", "NEUTRAL");

        // Parse any custom score keys
        Map<String, Integer> scores = new HashMap<>();
        for (String k : Arrays.asList("happy", "sad", "angry", "relaxed", "neutral", "surprised")) {
            if (data.containsKey(k)) {
                try {
                    scores.put(k, Integer.parseInt(data.get(k)));
                } catch (NumberFormatException ignored) {}
            }
        }

        MoodAnalyzerService.MoodAnalysisResult result = moodAnalyzer.analyzeMood(moodStr, scores);
        sendJson(exchange, 200, result.toJson());
    }

    private void handleSearch(HttpExchange exchange, String queryParam) throws IOException {
        String query = "";
        if (queryParam != null) {
            for (String pair : queryParam.split("&")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2 && kv[0].equals("q")) {
                    query = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                }
            }
        }
        List<Song> results = musicService.search(query);
        sendJson(exchange, 200, JsonUtils.songsToJson(results));
    }

    private void handleGetPlaylists(HttpExchange exchange) throws IOException {
        List<Playlist> list = musicService.getAllPlaylists();
        sendJson(exchange, 200, JsonUtils.playlistsToJson(list));
    }

    private void handleCreatePlaylist(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        Map<String, String> data = JsonUtils.parseSimpleJson(body);
        String name = data.getOrDefault("name", "My Playlist");
        String desc = data.getOrDefault("description", "Custom playlist on MUSIX");

        Playlist pl = musicService.createPlaylist(name, desc, Collections.emptyList());
        sendJson(exchange, 201, JsonUtils.playlistToJson(pl));
    }

    private void handleAddSongToPlaylist(HttpExchange exchange, String path) throws IOException {
        // e.g. /api/playlists/{id}/songs
        String[] parts = path.split("/");
        if (parts.length >= 4) {
            String playlistId = parts[3];
            String body = readRequestBody(exchange);
            Map<String, String> data = JsonUtils.parseSimpleJson(body);
            String songId = data.get("songId");
            if (songId != null) {
                boolean ok = musicService.addSongToPlaylist(playlistId, songId);
                sendJson(exchange, ok ? 200 : 400, "{\"success\":" + ok + "}");
                return;
            }
        }
        sendJson(exchange, 400, "{\"error\":\"Invalid request\"}");
    }

    private void handleToggleLike(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        Map<String, String> data = JsonUtils.parseSimpleJson(body);
        String songId = data.get("songId");
        if (songId != null) {
            boolean liked = musicService.toggleLike(songId);
            sendJson(exchange, 200, "{\"songId\":\"" + songId + "\",\"liked\":" + liked + "}");
        } else {
            sendJson(exchange, 400, "{\"error\":\"Missing songId\"}");
        }
    }

    private void handleGetLikedSongs(HttpExchange exchange) throws IOException {
        List<Song> liked = musicService.getLikedSongs();
        sendJson(exchange, 200, JsonUtils.songsToJson(liked));
    }

    private void handleGetRecentlyPlayed(HttpExchange exchange) throws IOException {
        List<Song> recent = musicService.getRecentlyPlayed();
        sendJson(exchange, 200, JsonUtils.songsToJson(recent));
    }

    private void handleRecordRecentlyPlayed(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        Map<String, String> data = JsonUtils.parseSimpleJson(body);
        String songId = data.get("songId");
        if (songId != null) {
            musicService.recordRecentlyPlayed(songId);
            sendJson(exchange, 200, "{\"success\":true}");
        } else {
            sendJson(exchange, 400, "{\"error\":\"Missing songId\"}");
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        Map<String, String> data = JsonUtils.parseSimpleJson(body);
        String email = data.getOrDefault("email", "").trim().toLowerCase();
        String password = data.getOrDefault("password", "");

        User user = users.get(email);
        if (user != null && user.getPassword().equals(password)) {
            sendJson(exchange, 200, "{\"success\":true,\"user\":" + JsonUtils.userToJson(user) + "}");
        } else if (email.equals("lucky@musix.ai") || email.endsWith("@musix.ai") || email.contains("@")) {
            // Friendly demo login fallback
            User demo = new User("u-" + Math.abs(email.hashCode()), email.split("@")[0], email, password, "assets/covers/happy_1.svg");
            users.put(email, demo);
            sendJson(exchange, 200, "{\"success\":true,\"user\":" + JsonUtils.userToJson(demo) + "}");
        } else {
            sendJson(exchange, 401, "{\"error\":\"Invalid credentials\"}");
        }
    }

    private void handleRegister(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        Map<String, String> data = JsonUtils.parseSimpleJson(body);
        String email = data.getOrDefault("email", "").trim().toLowerCase();
        String name = data.getOrDefault("name", "Musix Explorer");
        String password = data.getOrDefault("password", "");

        if (email.isEmpty() || password.isEmpty()) {
            sendJson(exchange, 400, "{\"error\":\"Email and password required\"}");
            return;
        }

        User user = new User("u-" + UUID.randomUUID().toString().substring(0, 8), name, email, password, "assets/covers/happy_1.svg");
        users.put(email, user);
        sendJson(exchange, 201, "{\"success\":true,\"user\":" + JsonUtils.userToJson(user) + "}");
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = is.read(buffer)) != -1) {
            baos.write(buffer, 0, length);
        }
        return baos.toString(StandardCharsets.UTF_8);
    }

    private void sendJson(HttpExchange exchange, int statusCode, String responseJson) throws IOException {
        byte[] bytes = responseJson.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }
}
