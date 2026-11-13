package com.musix.server;

import com.musix.model.Mood;
import com.musix.model.Playlist;
import com.musix.model.Song;
import com.musix.model.User;
import com.musix.service.MoodAnalyzerService;
import com.musix.service.MusicService;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MusixHttpServer {
    private final int port;
    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final MusicService musicService = MusicService.getInstance();
    private final MoodAnalyzerService moodAnalyzer = MoodAnalyzerService.getInstance();
    private final Map<String, User> users = new HashMap<>();
    private File webDir;

    public MusixHttpServer(int port) {
        this.port = port;
        this.webDir = findWebDirectory();
        users.put("lucky@musix.ai", new User("u-1", "Lucky", "lucky@musix.ai", "password123", "assets/covers/happy_1.svg"));
    }

    public int getPort() {
        return port;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        try {
            serverSocket.bind(new InetSocketAddress("0.0.0.0", port));
        } catch (Exception e) {
            try {
                serverSocket.close();
            } catch (Exception ignored) {}
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress("127.0.0.1", port));
        }
        running = true;

        printBanner();

        executor.submit(() -> {
            while (running && !serverSocket.isClosed()) {
                try {
                    Socket client = serverSocket.accept();
                    executor.submit(() -> handleClient(client));
                } catch (IOException e) {
                    if (!running) break;
                }
            }
        });
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {}
        executor.shutdownNow();
        System.out.println("MUSIX Server stopped.");
    }

    private void handleClient(Socket client) {
        try (client;
             InputStream in = client.getInputStream();
             OutputStream out = client.getOutputStream()) {

            // Read request line and headers
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.isEmpty()) return;

            String[] parts = requestLine.split(" ");
            if (parts.length < 2) return;

            String method = parts[0].toUpperCase();
            String uriStr = parts[1];

            // Parse headers
            Map<String, String> headers = new HashMap<>();
            String headerLine;
            int contentLength = 0;
            while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
                int colon = headerLine.indexOf(':');
                if (colon > 0) {
                    String name = headerLine.substring(0, colon).trim().toLowerCase();
                    String val = headerLine.substring(colon + 1).trim();
                    headers.put(name, val);
                    if (name.equals("content-length")) {
                        try { contentLength = Integer.parseInt(val); } catch (NumberFormatException ignored) {}
                    }
                }
            }

            // Read body if present
            String body = "";
            if (contentLength > 0) {
                char[] bodyChars = new char[contentLength];
                int read = 0;
                while (read < contentLength) {
                    int r = reader.read(bodyChars, read, contentLength - read);
                    if (r == -1) break;
                    read += r;
                }
                body = new String(bodyChars, 0, read);
            }

            // CORS preflight
            if (method.equals("OPTIONS")) {
                sendResponse(out, 204, "No Content", "text/plain", new byte[0]);
                return;
            }

            // Route request
            URI uri = URI.create(uriStr);
            String path = uri.getPath();

            if (path.startsWith("/api/")) {
                handleApi(out, method, path, uri.getQuery(), body);
            } else {
                handleStatic(out, path);
            }
        } catch (Exception e) {
            // Ignore socket disconnects
        }
    }

    private void handleApi(OutputStream out, String method, String path, String query, String body) throws IOException {
        try {
            if (path.equals("/api/status")) {
                String json = "{\"status\":\"online\",\"app\":\"MUSIX\",\"theme\":\"Midnight Purple\",\"totalSongs\":" +
                        musicService.getAllSongs().size() + ",\"version\":\"2.0.0\"}";
                sendJsonResponse(out, 200, json);
            } else if (path.equals("/api/songs") && method.equals("GET")) {
                sendJsonResponse(out, 200, JsonUtils.songsToJson(musicService.getAllSongs()));
            } else if (path.startsWith("/api/songs/mood/") && method.equals("GET")) {
                String moodStr = path.substring("/api/songs/mood/".length());
                Mood mood = Mood.fromString(moodStr);
                sendJsonResponse(out, 200, JsonUtils.songsToJson(musicService.getSongsByMood(mood)));
            } else if (path.equals("/api/mood/analyze") && method.equals("POST")) {
                Map<String, String> data = JsonUtils.parseSimpleJson(body);
                String moodStr = data.getOrDefault("mood", "HAPPY");
                Map<String, Integer> scores = new HashMap<>();
                for (String k : Arrays.asList("happy", "sad", "angry", "relaxed", "neutral", "surprised")) {
                    if (data.containsKey(k)) {
                        try { scores.put(k, Integer.parseInt(data.get(k))); } catch (NumberFormatException ignored) {}
                    }
                }
                MoodAnalyzerService.MoodAnalysisResult result = moodAnalyzer.analyzeMood(moodStr, scores);
                sendJsonResponse(out, 200, result.toJson());
            } else if (path.startsWith("/api/search") && method.equals("GET")) {
                String searchQ = "";
                if (query != null) {
                    for (String pair : query.split("&")) {
                        String[] kv = pair.split("=", 2);
                        if (kv.length == 2 && kv[0].equals("q")) {
                            searchQ = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                        }
                    }
                }
                sendJsonResponse(out, 200, JsonUtils.songsToJson(musicService.search(searchQ)));
            } else if (path.equals("/api/playlists") && method.equals("GET")) {
                sendJsonResponse(out, 200, JsonUtils.playlistsToJson(musicService.getAllPlaylists()));
            } else if (path.equals("/api/playlists") && method.equals("POST")) {
                Map<String, String> data = JsonUtils.parseSimpleJson(body);
                String name = data.getOrDefault("name", "My Playlist");
                String desc = data.getOrDefault("description", "Custom playlist on MUSIX");
                Playlist pl = musicService.createPlaylist(name, desc, Collections.emptyList());
                sendJsonResponse(out, 201, JsonUtils.playlistToJson(pl));
            } else if (path.startsWith("/api/playlists/") && path.contains("/songs") && method.equals("POST")) {
                String[] parts = path.split("/");
                if (parts.length >= 4) {
                    String playlistId = parts[3];
                    Map<String, String> data = JsonUtils.parseSimpleJson(body);
                    String songId = data.get("songId");
                    if (songId != null) {
                        boolean ok = musicService.addSongToPlaylist(playlistId, songId);
                        sendJsonResponse(out, ok ? 200 : 400, "{\"success\":" + ok + "}");
                        return;
                    }
                }
                sendJsonResponse(out, 400, "{\"error\":\"Invalid playlist or songId\"}");
            } else if (path.startsWith("/api/playlists/") && path.contains("/songs/") && method.equals("DELETE")) {
                String[] parts = path.split("/");
                if (parts.length >= 6) {
                    String playlistId = parts[3];
                    String songId = parts[5];
                    boolean ok = musicService.removeSongFromPlaylist(playlistId, songId);
                    sendJsonResponse(out, ok ? 200 : 400, "{\"success\":" + ok + "}");
                    return;
                }
                sendJsonResponse(out, 400, "{\"error\":\"Invalid delete route\"}");
            } else if (path.equals("/api/like") && method.equals("POST")) {
                Map<String, String> data = JsonUtils.parseSimpleJson(body);
                String songId = data.get("songId");
                if (songId != null) {
                    boolean liked = musicService.toggleLike(songId);
                    sendJsonResponse(out, 200, "{\"songId\":\"" + songId + "\",\"liked\":" + liked + "}");
                } else {
                    sendJsonResponse(out, 400, "{\"error\":\"Missing songId\"}");
                }
            } else if (path.equals("/api/liked") && method.equals("GET")) {
                sendJsonResponse(out, 200, JsonUtils.songsToJson(musicService.getLikedSongs()));
            } else if (path.equals("/api/recently-played") && method.equals("GET")) {
                sendJsonResponse(out, 200, JsonUtils.songsToJson(musicService.getRecentlyPlayed()));
            } else if (path.equals("/api/recently-played") && method.equals("POST")) {
                Map<String, String> data = JsonUtils.parseSimpleJson(body);
                String songId = data.get("songId");
                if (songId != null) {
                    musicService.recordRecentlyPlayed(songId);
                    sendJsonResponse(out, 200, "{\"success\":true}");
                } else {
                    sendJsonResponse(out, 400, "{\"error\":\"Missing songId\"}");
                }
            } else if (path.equals("/api/recently-played") && method.equals("DELETE")) {
                musicService.clearRecentlyPlayed();
                sendJsonResponse(out, 200, "{\"success\":true,\"cleared\":true}");
            } else if (path.equals("/api/auth/login") && method.equals("POST")) {
                Map<String, String> data = JsonUtils.parseSimpleJson(body);
                String email = data.getOrDefault("email", "").trim().toLowerCase();
                String pwd = data.getOrDefault("password", "");
                User u = users.get(email);
                if (u != null && u.getPassword().equals(pwd)) {
                    sendJsonResponse(out, 200, "{\"success\":true,\"user\":" + JsonUtils.userToJson(u) + "}");
                } else if (email.equals("lucky@musix.ai") || email.endsWith("@musix.ai") || email.contains("@")) {
                    User demo = new User("u-" + Math.abs(email.hashCode()), email.split("@")[0], email, pwd, "assets/covers/happy_1.svg");
                    users.put(email, demo);
                    sendJsonResponse(out, 200, "{\"success\":true,\"user\":" + JsonUtils.userToJson(demo) + "}");
                } else {
                    sendJsonResponse(out, 401, "{\"error\":\"Invalid credentials\"}");
                }
            } else if (path.equals("/api/auth/register") && method.equals("POST")) {
                Map<String, String> data = JsonUtils.parseSimpleJson(body);
                String email = data.getOrDefault("email", "").trim().toLowerCase();
                String name = data.getOrDefault("name", "Musix Explorer");
                String pwd = data.getOrDefault("password", "");
                if (email.isEmpty() || pwd.isEmpty()) {
                    sendJsonResponse(out, 400, "{\"error\":\"Email and password required\"}");
                } else {
                    User newUser = new User("u-" + UUID.randomUUID().toString().substring(0, 8), name, email, pwd, "assets/covers/happy_1.svg");
                    users.put(email, newUser);
                    sendJsonResponse(out, 201, "{\"success\":true,\"user\":" + JsonUtils.userToJson(newUser) + "}");
                }
            } else if (path.equals("/api/auth/logout") && method.equals("POST")) {
                sendJsonResponse(out, 200, "{\"success\":true,\"message\":\"Logged out\"}");
            } else {
                sendJsonResponse(out, 404, "{\"error\":\"Not found\"}");
            }
        } catch (Exception e) {
            sendJsonResponse(out, 500, "{\"error\":\"" + JsonUtils.escape(e.getMessage()) + "\"}");
        }
    }

    private void handleStatic(OutputStream out, String path) throws IOException {
        if (path == null || path.equals("/") || path.isEmpty()) {
            path = "/index.html";
        }

        File target = new File(webDir, path.replace('/', File.separatorChar));
        String targetCanonical = target.getCanonicalPath();
        String webCanonical = webDir.getCanonicalPath();

        if (!targetCanonical.startsWith(webCanonical) || !target.exists() || target.isDirectory()) {
            target = new File(webDir, "index.html");
            if (!target.exists()) {
                sendResponse(out, 404, "Not Found", "text/plain", "404 Not Found".getBytes(StandardCharsets.UTF_8));
                return;
            }
        }

        byte[] bytes = Files.readAllBytes(target.toPath());
        String mime = getMimeType(target.getName());
        sendResponse(out, 200, "OK", mime, bytes);
    }

    private void sendJsonResponse(OutputStream out, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        sendResponse(out, status, status == 200 || status == 201 ? "OK" : "Error", "application/json; charset=UTF-8", bytes);
    }

    private void sendResponse(OutputStream out, int status, String statusText, String contentType, byte[] data) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 ").append(status).append(" ").append(statusText).append("\r\n");
        sb.append("Content-Type: ").append(contentType).append("\r\n");
        sb.append("Content-Length: ").append(data.length).append("\r\n");
        sb.append("Access-Control-Allow-Origin: *\r\n");
        sb.append("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS\r\n");
        sb.append("Access-Control-Allow-Headers: Content-Type, Authorization\r\n");
        sb.append("Connection: close\r\n");
        sb.append("\r\n");

        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        out.write(data);
        out.flush();
    }

    private File findWebDirectory() {
        File[] candidates = new File[] {
                new File("web"),
                new File("C:\\Users\\lucky\\.gemini\\antigravity\\scratch\\musix\\web"),
                new File("../web"),
                new File("../../web")
        };
        for (File f : candidates) {
            if (f.exists() && f.isDirectory() && new File(f, "index.html").exists()) {
                return f;
            }
        }
        return new File("web");
    }

    private String getMimeType(String name) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=UTF-8";
        if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
        if (lower.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".ico")) return "image/x-icon";
        if (lower.endsWith(".mp3")) return "audio/mpeg";
        if (lower.endsWith(".wav")) return "audio/wav";
        return "application/octet-stream";
    }

    private void printBanner() {
        System.out.println("==========================================================");
        System.out.println("   __  __ _    _  _____ _______   __");
        System.out.println("  |  \\/  | |  | |/ ____|_   _\\ \\ / /");
        System.out.println("  | \\  / | |  | | (___   | |  \\ V / ");
        System.out.println("  | |\\/| | |  | |\\___ \\  | |   > <  ");
        System.out.println("  | |  | | |__| |____) |_| |_ / . \\ ");
        System.out.println("  |_|  |_|\\____/|_____/|_____/_/ \\_\\");
        System.out.println("   AI-POWERED MUSIC STREAMING PLATFORM");
        System.out.println("   Theme: Midnight Purple (#100B1D | #8B5CF6)");
        System.out.println("==========================================================");
        System.out.println(">> Server running at: http://localhost:" + port);
        System.out.println(">> REST API available at: http://localhost:" + port + "/api/status");
        System.out.println(">> Press Ctrl+C to terminate the server.");
        System.out.println("==========================================================");
    }
}
