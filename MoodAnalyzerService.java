package com.musix.service;

import com.musix.model.Mood;
import com.musix.model.Song;

import java.util.*;

public class MoodAnalyzerService {
    private static MoodAnalyzerService instance;
    private final MusicService musicService;

    private MoodAnalyzerService() {
        this.musicService = MusicService.getInstance();
    }

    public static synchronized MoodAnalyzerService getInstance() {
        if (instance == null) {
            instance = new MoodAnalyzerService();
        }
        return instance;
    }

    public static class MoodAnalysisResult {
        public Mood detectedMood;
        public int confidencePercentage;
        public String insightText;
        public String dominantCharacteristic;
        public Map<String, Integer> emotionScores;
        public List<Song> recommendedSongs;
        public String recommendedPlaylistName;
        public String colorGradient;

        public String toJson() {
            StringBuilder sb = new StringBuilder("{");
            sb.append("\"mood\":\"").append(detectedMood.name()).append("\",");
            sb.append("\"displayName\":\"").append(detectedMood.getDisplayName()).append("\",");
            sb.append("\"emoji\":\"").append(detectedMood.getEmoji()).append("\",");
            sb.append("\"confidence\":").append(confidencePercentage).append(",");
            sb.append("\"insightText\":\"").append(com.musix.server.JsonUtils.escape(insightText)).append("\",");
            sb.append("\"characteristic\":\"").append(com.musix.server.JsonUtils.escape(dominantCharacteristic)).append("\",");
            sb.append("\"colorGradient\":\"").append(com.musix.server.JsonUtils.escape(colorGradient)).append("\",");
            sb.append("\"playlistName\":\"").append(com.musix.server.JsonUtils.escape(recommendedPlaylistName)).append("\",");
            sb.append("\"scores\":{");
            int i = 0;
            for (Map.Entry<String, Integer> entry : emotionScores.entrySet()) {
                if (i++ > 0) sb.append(",");
                sb.append("\"").append(entry.getKey()).append("\":").append(entry.getValue());
            }
            sb.append("},");
            sb.append("\"recommendedSongs\":").append(com.musix.server.JsonUtils.songsToJson(recommendedSongs));
            sb.append("}");
            return sb.toString();
        }
    }

    public MoodAnalysisResult analyzeMood(String moodStr, Map<String, Integer> clientScores) {
        Mood mood = Mood.fromString(moodStr);
        MoodAnalysisResult result = new MoodAnalysisResult();
        result.detectedMood = mood;

        Map<String, Integer> scores = new LinkedHashMap<>();
        if (clientScores != null && !clientScores.isEmpty()) {
            scores.putAll(clientScores);
        } else {
            // Default realistic breakdown based on selected mood
            scores.put("happy", mood == Mood.HAPPY ? 92 : 12);
            scores.put("sad", mood == Mood.SAD ? 88 : 8);
            scores.put("angry", mood == Mood.ANGRY ? 86 : 6);
            scores.put("relaxed", mood == Mood.RELAXED ? 94 : 14);
            scores.put("neutral", mood == Mood.NEUTRAL ? 85 : 20);
            scores.put("surprised", mood == Mood.SURPRISED ? 89 : 10);
        }
        result.emotionScores = scores;
        result.confidencePercentage = scores.getOrDefault(mood.name().toLowerCase(), 91);

        switch (mood) {
            case HAPPY:
                result.insightText = "Your face radiates positive energy and warm smiles. Time for uplifting dance and vibrant feel-good rhythms!";
                result.dominantCharacteristic = "Bright smile, high energy & relaxed forehead";
                result.recommendedPlaylistName = "Face Mood: Euphoria & Joy";
                result.colorGradient = "linear-gradient(135deg, #8B5CF6, #F59E0B)";
                break;
            case SAD:
                result.insightText = "We noticed gentle melancholy and thoughtful introspection. Deep acoustic chords and heartfelt ballads will keep you company.";
                result.dominantCharacteristic = "Reflective gaze, gentle lowered brow & subtle expression";
                result.recommendedPlaylistName = "Face Mood: Melancholy & Rain";
                result.colorGradient = "linear-gradient(135deg, #312E81, #6366F1)";
                break;
            case ANGRY:
                result.insightText = "Intense concentration and fierce facial energy detected! Channel that raw adrenaline with powerful rock and aggressive beats.";
                result.dominantCharacteristic = "Furrowed brow, heightened muscle tension & intense focus";
                result.recommendedPlaylistName = "Face Mood: Neon Fury & Raw Power";
                result.colorGradient = "linear-gradient(135deg, #DC2626, #8B5CF6)";
                break;
            case RELAXED:
                result.insightText = "Total tranquility and calm facial serenity. Slip into smooth lo-fi beats, warm vinyl textures, and ambient serenity.";
                result.dominantCharacteristic = "Soft eye posture, smooth brow & relaxed jawline";
                result.recommendedPlaylistName = "Face Mood: Midnight Velvet Lofi";
                result.colorGradient = "linear-gradient(135deg, #06B6D4, #8B5CF6)";
                break;
            case SURPRISED:
                result.insightText = "Wide eyes and excitement! You're ready for unexpected sonic adventures, electrifying drops, and exhilarating basslines.";
                result.dominantCharacteristic = "Elevated eyebrows, wide eye aperture & dynamic expression";
                result.recommendedPlaylistName = "Face Mood: Electric Shock & Discovery";
                result.colorGradient = "linear-gradient(135deg, #F59E0B, #EC4899)";
                break;
            case NEUTRAL:
            default:
                result.insightText = "Balanced and centered. Here is your personalized daily selection of the freshest trending hits.";
                result.dominantCharacteristic = "Even composure, balanced facial symmetry & resting focus";
                result.recommendedPlaylistName = "Face Mood: Daily Pulse Mix";
                result.colorGradient = "linear-gradient(135deg, #8B5CF6, #EC4899)";
                break;
        }

        result.recommendedSongs = musicService.getSongsByMood(mood);
        if (result.recommendedSongs.isEmpty()) {
            result.recommendedSongs = musicService.getAllSongs();
        }

        return result;
    }
}
