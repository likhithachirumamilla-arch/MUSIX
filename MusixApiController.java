package com.musix.boot;

import com.musix.model.Mood;
import com.musix.model.Playlist;
import com.musix.model.Song;
import com.musix.model.User;
import com.musix.service.MoodAnalyzerService;
import com.musix.service.MusicService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Spring Boot REST API Controller for MUSIX
 * Maps all endpoints for Songs, Playlists, Liked Songs, Recently Played, and AI Biometric Mood Analysis.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class MusixApiController {

    private final MusicService musicService = MusicService.getInstance();
    private final MoodAnalyzerService moodAnalyzer = MoodAnalyzerService.getInstance();
    private final Map<String, User> users = new HashMap<>();

    public MusixApiController() {
        users.put("lucky@musix.ai", new User("u-1", "Lucky", "lucky@musix.ai", "password123", "assets/covers/happy_1.svg"));
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", "online");
        status.put("app", "MUSIX");
        status.put("framework", "Spring Boot 3.2.4");
        status.put("theme", "Midnight Purple");
        status.put("totalSongs", musicService.getAllSongs().size());
        status.put("version", "2.0.0");
        return status;
    }

    @GetMapping("/songs")
    public List<Song> getAllSongs() {
        return musicService.getAllSongs();
    }

    @GetMapping("/songs/mood/{mood}")
    public List<Song> getSongsByMood(@PathVariable String mood) {
        return musicService.getSongsByMood(Mood.fromString(mood));
    }

    @PostMapping("/mood/analyze")
    public ResponseEntity<?> analyzeMood(@RequestBody Map<String, Object> payload) {
        String moodStr = (String) payload.getOrDefault("mood", "HAPPY");
        Map<String, Integer> scores = new HashMap<>();
        for (String k : Arrays.asList("happy", "sad", "angry", "relaxed", "neutral", "surprised")) {
            if (payload.containsKey(k)) {
                try {
                    scores.put(k, Integer.parseInt(payload.get(k).toString()));
                } catch (Exception ignored) {}
            }
        }
        MoodAnalyzerService.MoodAnalysisResult result = moodAnalyzer.analyzeMood(moodStr, scores);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/search")
    public List<Song> search(@RequestParam(name = "q", defaultValue = "") String query) {
        return musicService.search(query);
    }

    @GetMapping("/playlists")
    public List<Playlist> getPlaylists() {
        return musicService.getAllPlaylists();
    }

    @PostMapping("/playlists")
    public ResponseEntity<Playlist> createPlaylist(@RequestBody Map<String, String> body) {
        String name = body.getOrDefault("name", "My Playlist");
        String desc = body.getOrDefault("description", "Custom playlist on MUSIX");
        Playlist pl = musicService.createPlaylist(name, desc, Collections.emptyList());
        return ResponseEntity.status(HttpStatus.CREATED).body(pl);
    }

    @PostMapping("/playlists/{id}/songs")
    public ResponseEntity<?> addSongToPlaylist(@PathVariable String id, @RequestBody Map<String, String> body) {
        String songId = body.get("songId");
        if (songId != null) {
            boolean ok = musicService.addSongToPlaylist(id, songId);
            return ResponseEntity.ok(Collections.singletonMap("success", ok));
        }
        return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Missing songId"));
    }

    @DeleteMapping("/playlists/{id}/songs/{songId}")
    public ResponseEntity<?> removeSongFromPlaylist(@PathVariable String id, @PathVariable String songId) {
        boolean ok = musicService.removeSongFromPlaylist(id, songId);
        return ResponseEntity.ok(Collections.singletonMap("success", ok));
    }

    @PostMapping("/like")
    public ResponseEntity<?> toggleLike(@RequestBody Map<String, String> body) {
        String songId = body.get("songId");
        if (songId != null) {
            boolean liked = musicService.toggleLike(songId);
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("songId", songId);
            resp.put("liked", liked);
            return ResponseEntity.ok(resp);
        }
        return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Missing songId"));
    }

    @GetMapping("/liked")
    public List<Song> getLikedSongs() {
        return musicService.getLikedSongs();
    }

    @GetMapping("/recently-played")
    public List<Song> getRecentlyPlayed() {
        return musicService.getRecentlyPlayed();
    }

    @PostMapping("/recently-played")
    public ResponseEntity<?> recordRecentlyPlayed(@RequestBody Map<String, String> body) {
        String songId = body.get("songId");
        if (songId != null) {
            musicService.recordRecentlyPlayed(songId);
            return ResponseEntity.ok(Collections.singletonMap("success", true));
        }
        return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Missing songId"));
    }

    @DeleteMapping("/recently-played")
    public ResponseEntity<?> clearRecentlyPlayed() {
        musicService.clearRecentlyPlayed();
        return ResponseEntity.ok(Collections.singletonMap("cleared", true));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String email = body.getOrDefault("email", "").trim().toLowerCase();
        String password = body.getOrDefault("password", "");
        User user = users.get(email);
        if (user != null && user.getPassword().equals(password)) {
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("success", true);
            resp.put("user", user);
            return ResponseEntity.ok(resp);
        } else if (email.equals("lucky@musix.ai") || email.endsWith("@musix.ai") || email.contains("@")) {
            User demo = new User("u-" + Math.abs(email.hashCode()), email.split("@")[0], email, password, "assets/covers/happy_1.svg");
            users.put(email, demo);
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("success", true);
            resp.put("user", demo);
            return ResponseEntity.ok(resp);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error", "Invalid credentials"));
    }

    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String email = body.getOrDefault("email", "").trim().toLowerCase();
        String name = body.getOrDefault("name", "Musix Explorer");
        String password = body.getOrDefault("password", "");
        if (email.isEmpty() || password.isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Email and password required"));
        }
        User newUser = new User("u-" + UUID.randomUUID().toString().substring(0, 8), name, email, password, "assets/covers/happy_1.svg");
        users.put(email, newUser);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("user", newUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }
}
