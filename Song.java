package com.musix.model;

public class Song {
    private String id;
    private String title;
    private String artist;
    private String album;
    private int duration; // in seconds
    private Mood mood;
    private int bpm;
    private String genre;
    private String coverUrl;
    private String coverGradient;
    private String audioKey; // tone key for synthesizer or audio stream
    private String lyrics;
    private boolean liked;

    public Song() {}

    public Song(String id, String title, String artist, String album, int duration,
                Mood mood, int bpm, String genre, String coverUrl, String coverGradient,
                String audioKey, String lyrics) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.duration = duration;
        this.mood = mood;
        this.bpm = bpm;
        this.genre = genre;
        this.coverUrl = coverUrl;
        this.coverGradient = coverGradient;
        this.audioKey = audioKey;
        this.lyrics = lyrics;
        this.liked = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getAlbum() { return album; }
    public void setAlbum(String album) { this.album = album; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public Mood getMood() { return mood; }
    public void setMood(Mood mood) { this.mood = mood; }

    public int getBpm() { return bpm; }
    public void setBpm(int bpm) { this.bpm = bpm; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public String getCoverGradient() { return coverGradient; }
    public void setCoverGradient(String coverGradient) { this.coverGradient = coverGradient; }

    public String getAudioKey() { return audioKey; }
    public void setAudioKey(String audioKey) { this.audioKey = audioKey; }

    public String getLyrics() { return lyrics; }
    public void setLyrics(String lyrics) { this.lyrics = lyrics; }

    public boolean isLiked() { return liked; }
    public void setLiked(boolean liked) { this.liked = liked; }

    public String getFormattedDuration() {
        int minutes = duration / 60;
        int seconds = duration % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}
