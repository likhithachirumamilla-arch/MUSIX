package com.musix.model;

import java.util.ArrayList;
import java.util.List;

public class Playlist {
    private String id;
    private String name;
    private String description;
    private String coverGradient;
    private String coverUrl;
    private List<String> songIds;
    private boolean isCustom;

    public Playlist() {
        this.songIds = new ArrayList<>();
    }

    public Playlist(String id, String name, String description, String coverGradient, String coverUrl, boolean isCustom) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.coverGradient = coverGradient;
        this.coverUrl = coverUrl;
        this.songIds = new ArrayList<>();
        this.isCustom = isCustom;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCoverGradient() { return coverGradient; }
    public void setCoverGradient(String coverGradient) { this.coverGradient = coverGradient; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public List<String> getSongIds() { return songIds; }
    public void setSongIds(List<String> songIds) { this.songIds = songIds; }

    public boolean isCustom() { return isCustom; }
    public void setCustom(boolean custom) { isCustom = custom; }

    public void addSong(String songId) {
        if (!songIds.contains(songId)) {
            songIds.add(songId);
        }
    }

    public void removeSong(String songId) {
        songIds.remove(songId);
    }
}
