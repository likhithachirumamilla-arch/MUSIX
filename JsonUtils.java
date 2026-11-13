package com.musix.server;

import com.musix.model.Mood;
import com.musix.model.Playlist;
import com.musix.model.Song;
import com.musix.model.User;

import java.util.*;

public class JsonUtils {

    public static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        String hex = Integer.toHexString(c);
                        sb.append("\\u");
                        for (int k = 0; k < 4 - hex.length(); k++) sb.append('0');
                        sb.append(hex);
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    public static String songToJson(Song s) {
        if (s == null) return "null";
        return "{" +
                "\"id\":\"" + escape(s.getId()) + "\"," +
                "\"title\":\"" + escape(s.getTitle()) + "\"," +
                "\"artist\":\"" + escape(s.getArtist()) + "\"," +
                "\"album\":\"" + escape(s.getAlbum()) + "\"," +
                "\"duration\":" + s.getDuration() + "," +
                "\"durationFormatted\":\"" + escape(s.getFormattedDuration()) + "\"," +
                "\"mood\":\"" + escape(s.getMood().name()) + "\"," +
                "\"moodEmoji\":\"" + escape(s.getMood().getEmoji()) + "\"," +
                "\"bpm\":" + s.getBpm() + "," +
                "\"genre\":\"" + escape(s.getGenre()) + "\"," +
                "\"coverUrl\":\"" + escape(s.getCoverUrl()) + "\"," +
                "\"coverGradient\":\"" + escape(s.getCoverGradient()) + "\"," +
                "\"audioKey\":\"" + escape(s.getAudioKey()) + "\"," +
                "\"lyrics\":\"" + escape(s.getLyrics()) + "\"," +
                "\"liked\":" + s.isLiked() +
                "}";
    }

    public static String songsToJson(List<Song> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(songToJson(list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    public static String playlistToJson(Playlist p) {
        if (p == null) return "null";
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"id\":\"").append(escape(p.getId())).append("\",");
        sb.append("\"name\":\"").append(escape(p.getName())).append("\",");
        sb.append("\"description\":\"").append(escape(p.getDescription())).append("\",");
        sb.append("\"coverGradient\":\"").append(escape(p.getCoverGradient())).append("\",");
        sb.append("\"coverUrl\":\"").append(escape(p.getCoverUrl())).append("\",");
        sb.append("\"isCustom\":").append(p.isCustom()).append(",");
        sb.append("\"songIds\":[");
        for (int i = 0; i < p.getSongIds().size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escape(p.getSongIds().get(i))).append("\"");
        }
        sb.append("]}");
        return sb.toString();
    }

    public static String playlistsToJson(List<Playlist> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(playlistToJson(list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    public static String userToJson(User u) {
        if (u == null) return "null";
        return "{" +
                "\"id\":\"" + escape(u.getId()) + "\"," +
                "\"name\":\"" + escape(u.getName()) + "\"," +
                "\"email\":\"" + escape(u.getEmail()) + "\"," +
                "\"avatar\":\"" + escape(u.getAvatar()) + "\"" +
                "}";
    }

    /**
     * Simple key-value JSON parser for flat requests.
     */
    public static Map<String, String> parseSimpleJson(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null) return map;
        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}")) json = json.substring(0, json.length() - 1);

        String[] pairs = json.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        for (String pair : pairs) {
            String[] kv = pair.split(":(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", 2);
            if (kv.length == 2) {
                String key = kv[0].trim().replaceAll("^\"|\"$", "");
                String val = kv[1].trim().replaceAll("^\"|\"$", "");
                map.put(key, val);
            }
        }
        return map;
    }
}
