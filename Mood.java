package com.musix.model;

public enum Mood {
    HAPPY("Happy", "Vibrant, feel-good hits & upbeat rhythms", "#10B981", "😄"),
    SAD("Sad", "Soulful acoustic, melancholy piano & deep ballads", "#6366F1", "😢"),
    ANGRY("Angry", "Fierce pop, hard rock & electrifying high-energy beats", "#EF4444", "😡"),
    RELAXED("Relaxed", "Lofi chillhop, ambient textures & serene instrumentals", "#06B6D4", "🧘"),
    NEUTRAL("Neutral", "Personalized trending hits & balanced daily mix", "#8B5CF6", "😐"),
    SURPRISED("Surprised", "Exhilarating EDM drops, unexpected synthwave & energetic bangers", "#F59E0B", "😲");

    private final String displayName;
    private final String description;
    private final String colorHex;
    private final String emoji;

    Mood(String displayName, String description, String colorHex, String emoji) {
        this.displayName = displayName;
        this.description = description;
        this.colorHex = colorHex;
        this.emoji = emoji;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getColorHex() {
        return colorHex;
    }

    public String getEmoji() {
        return emoji;
    }

    public static Mood fromString(String text) {
        if (text == null) return NEUTRAL;
        String clean = text.trim().toUpperCase();
        for (Mood m : values()) {
            if (m.name().equalsIgnoreCase(clean) || m.displayName.equalsIgnoreCase(clean)) {
                return m;
            }
        }
        return NEUTRAL;
    }
}
