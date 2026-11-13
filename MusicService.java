package com.musix.service;

import com.musix.model.Mood;
import com.musix.model.Playlist;
import com.musix.model.Song;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MusicService {
    private static MusicService instance;

    private final Map<String, Song> songs = new ConcurrentHashMap<>();
    private final Map<String, Playlist> playlists = new ConcurrentHashMap<>();
    private final Set<String> likedSongIds = Collections.synchronizedSet(new LinkedHashSet<>());
    private final List<String> recentlyPlayedIds = Collections.synchronizedList(new LinkedList<>());

    private MusicService() {
        seedInitialSongs();
        seedInitialPlaylists();
    }

    public static synchronized MusicService getInstance() {
        if (instance == null) {
            instance = new MusicService();
        }
        return instance;
    }

    private void seedInitialSongs() {
        // HAPPY MOOD SONGS (Upbeat, dance, feel-good pop)
        addSong(new Song("s-101", "Levitating In Purple", "Luna Eclipse", "Future Reverie", 203,
                Mood.HAPPY, 124, "Dance Pop",
                "assets/covers/happy_1.svg", "linear-gradient(135deg, #8B5CF6, #EC4899)",
                "happy_synth_dance",
                "[00:12] If you wanna run away with me, I know a galaxy\n[00:24] And I can take you for a ride\n[00:36] I had a premonition that we fell into a rhythm\n[00:48] Midnight purple starlight shining bright!"));

        addSong(new Song("s-102", "Golden Daylight", "The Sunburst Trio", "Solaris", 188,
                Mood.HAPPY, 118, "Indie Pop",
                "assets/covers/happy_2.svg", "linear-gradient(135deg, #F59E0B, #8B5CF6)",
                "happy_acoustic_groove",
                "[00:10] Morning comes and clears the haze\n[00:22] We got nothing but golden days\n[00:34] Walking under the violet sky\n[00:46] Smiles that never say goodbye"));

        addSong(new Song("s-103", "Sugar Rush Pop", "Kira & The Beats", "Neon Candy", 175,
                Mood.HAPPY, 128, "Hyper Pop",
                "assets/covers/happy_3.svg", "linear-gradient(135deg, #D8B4FE, #F43F5E)",
                "happy_hyper_pop",
                "[00:08] Sweet like bubblegum, fast like lightning\n[00:20] Every bass kick feels so exciting\n[00:32] Turn the volume all the way up\n[00:44] Drink the good vibes from the cup!"));

        addSong(new Song("s-104", "Electric Smiles", "Cosmo Kids", "Good Vibrations", 212,
                Mood.HAPPY, 122, "Nu-Disco",
                "assets/covers/happy_4.svg", "linear-gradient(135deg, #10B981, #8B5CF6)",
                "happy_disco_funk",
                "[00:15] Catch the groove on the purple floor\n[00:28] Dancing till we can't no more\n[00:40] Electric smiles in the flashing lights\n[00:52] These are our favorite midnight nights"));

        // SAD MOOD SONGS (Emotional, slow, acoustic, melancholic ballads)
        addSong(new Song("s-201", "Midnight Tears", "Aria Vance", "Echoes of Silence", 234,
                Mood.SAD, 76, "Acoustic Ballad",
                "assets/covers/sad_1.svg", "linear-gradient(135deg, #3B82F6, #1E1B4B)",
                "sad_piano_ballad",
                "[00:14] The rain taps softly on the glass\n[00:30] Memories of you that never pass\n[00:46] In the quiet corner of my room\n[01:02] Purple shadows fade too soon"));

        addSong(new Song("s-202", "Drowning in Purple", "Cillian Grey", "Aftermath", 248,
                Mood.SAD, 72, "Indie Folk",
                "assets/covers/sad_2.svg", "linear-gradient(135deg, #6366F1, #312E81)",
                "sad_acoustic_folk",
                "[00:18] Words we left unsaid tonight\n[00:36] Drifting out of reachable sight\n[00:54] All the promises painted in dust\n[01:12] Learning what it means to rust"));

        addSong(new Song("s-203", "Rain on the Glass", "Eleanor & Strings", "Solitude", 215,
                Mood.SAD, 80, "Ambient Classical",
                "assets/covers/sad_3.svg", "linear-gradient(135deg, #818CF8, #18112C)",
                "sad_ambient_strings",
                "[00:20] Slow descent of autumn dew\n[00:42] Thinking back to the days with you\n[01:04] Silent chords on an old guitar\n[01:26] Watching from afar"));

        addSong(new Song("s-204", "Echoes in the Hall", "Samuel Knox", "Ghost Notes", 198,
                Mood.SAD, 68, "Slowcore",
                "assets/covers/sad_4.svg", "linear-gradient(135deg, #475569, #8B5CF6)",
                "sad_slow_piano",
                "[00:16] Footsteps fade into the floor\n[00:34] You don't knock here anymore\n[00:52] Just the echo of our laughter\n[01:10] In the quiet ever-after"));

        // ANGRY MOOD SONGS (Energetic pop, rock, fierce beats, high-energy music)
        addSong(new Song("s-301", "Neon Fury", "Viper Strike", "Red Shift Overdrive", 195,
                Mood.ANGRY, 142, "Cyberpunk Rock",
                "assets/covers/angry_1.svg", "linear-gradient(135deg, #EF4444, #8B5CF6)",
                "angry_cyber_rock",
                "[00:10] Break the silence, shatter the glass\n[00:22] This storm of rage is built to last\n[00:34] Sparks fly when the wires cross\n[00:46] We take the world and cut the loss!"));

        addSong(new Song("s-302", "Rage Against the Silence", "Iron Pulse", "Thunderstrike", 210,
                Mood.ANGRY, 138, "Hard Rock",
                "assets/covers/angry_2.svg", "linear-gradient(135deg, #DC2626, #4C1D95)",
                "angry_distorted_riff",
                "[00:12] No more whispers, hear the roar\n[00:24] Battering down the barricade door\n[00:36] Blood pumping at two-hundred bpm\n[00:48] We will not bow down to them!"));

        addSong(new Song("s-303", "Distorted Mind", "Static Vortex", "Glitch Chaos", 182,
                Mood.ANGRY, 150, "Industrial Metal",
                "assets/covers/angry_3.svg", "linear-gradient(135deg, #B91C1C, #100B1D)",
                "angry_industrial_beat",
                "[00:08] Static screaming in my head\n[00:20] Every boundary painted red\n[00:32] Distortion heavy on the mic\n[00:44] Ready for the counter-strike!"));

        addSong(new Song("s-304", "Burn the Bridges", "Kassandra Riot", "Vendetta", 204,
                Mood.ANGRY, 134, "Fierce Pop Punk",
                "assets/covers/angry_4.svg", "linear-gradient(135deg, #F97316, #7C3AED)",
                "angry_punk_drive",
                "[00:14] Light the match and watch it flame\n[00:26] I'm sick of playing in your game\n[00:38] Midnight purple sparks ignite\n[00:50] Burning down the bridge tonight!"));

        // RELAXED MOOD SONGS (Lo-fi, ambient, instrumental, chill)
        addSong(new Song("s-401", "Midnight Purple Lofi", "Chilled Velvet", "Coffee at 2 AM", 172,
                Mood.RELAXED, 82, "Lofi Hip Hop",
                "assets/covers/relaxed_1.svg", "linear-gradient(135deg, #06B6D4, #8B5CF6)",
                "relaxed_lofi_beats",
                "[00:15] (Smooth vinyl crackle)\n[00:30] Gentle rain against the neon sign\n[00:50] Sip of warm tea, everything is fine\n[01:15] Breathing slow, feeling free"));

        addSong(new Song("s-402", "Velvet Clouds", "Sora Nami", "Floating Horizons", 220,
                Mood.RELAXED, 75, "Chillwave",
                "assets/covers/relaxed_2.svg", "linear-gradient(135deg, #2DD4BF, #4338CA)",
                "relaxed_ambient_pad",
                "[00:20] Drifting slowly above the trees\n[00:45] Carried by a gentle breeze\n[01:10] Violet hues in the twilight mist\n[01:35] Calm like this cannot be missed"));

        addSong(new Song("s-403", "Deep Ocean Breath", "Zen Garden Ensemble", "Still Waters", 255,
                Mood.RELAXED, 65, "Ambient Meditation",
                "assets/covers/relaxed_3.svg", "linear-gradient(135deg, #38BDF8, #1E1438)",
                "relaxed_meditation_flute",
                "[00:30] Inhale the peace of the midnight tide\n[01:00] Let all the tension drift aside\n[01:30] Harmonic resonance so deep\n[02:00] Drifting into tranquil sleep"));

        addSong(new Song("s-404", "Starlight Reverie", "Astral Lullaby", "Night Sky", 190,
                Mood.RELAXED, 80, "Downtempo",
                "assets/covers/relaxed_4.svg", "linear-gradient(135deg, #C084FC, #0F172A)",
                "relaxed_downtempo_keys",
                "[00:15] Soft Rhodes keys under cosmic light\n[00:35] Melting into the peaceful night\n[00:55] No rush, no worries on our mind\n[01:20] Leaving the chaotic world behind"));

        // NEUTRAL MOOD SONGS (Trending songs, personalized, daily mix)
        addSong(new Song("s-501", "Midnight Drive", "The Horizon Project", "Nightscapes", 218,
                Mood.NEUTRAL, 105, "Synth Pop",
                "assets/covers/neutral_1.svg", "linear-gradient(135deg, #8B5CF6, #3B82F6)",
                "neutral_synthpop_drive",
                "[00:12] Cruising down the empty highway lane\n[00:26] Streetlights rhythm like a purple train\n[00:40] Radio humming an easy song\n[00:54] Just moving right along"));

        addSong(new Song("s-502", "Lost in Tokyo", "Kenji & Maya", "City Lights", 194,
                Mood.NEUTRAL, 110, "City Pop",
                "assets/covers/neutral_2.svg", "linear-gradient(135deg, #A855F7, #EC4899)",
                "neutral_citypop_groove",
                "[00:14] Neon signs reflecting off the street\n[00:28] Catching strangers to an easy beat\n[00:42] Shinjuku crossing in the violet haze\n[00:56] Caught inside a pleasant maze"));

        addSong(new Song("s-503", "Purple Horizon", "Solar Flares", "Atmospheres", 205,
                Mood.NEUTRAL, 100, "Indie Electronic",
                "assets/covers/neutral_3.svg", "linear-gradient(135deg, #D8B4FE, #6366F1)",
                "neutral_indie_electro",
                "[00:16] Where the sky meets the purple sea\n[00:32] Exactly where I want to be\n[00:48] Neither running nor standing still\n[01:04] Watching time over the hill"));

        addSong(new Song("s-504", "Crystal Clear", "Nova Twins", "Prism", 186,
                Mood.NEUTRAL, 112, "Modern Pop",
                "assets/covers/neutral_4.svg", "linear-gradient(135deg, #60A5FA, #8B5CF6)",
                "neutral_clean_pop",
                "[00:10] Looking through the lens of clarity\n[00:24] Finding beauty in simplicity\n[00:38] Rhythm of the everyday\n[00:52] Showing us the steady way"));

        // SURPRISED MOOD SONGS (Exciting, energetic, discovery-based music)
        addSong(new Song("s-601", "Hypersonic Beats", "DJ Quantum", "Event Horizon", 208,
                Mood.SURPRISED, 145, "EDM / Trap",
                "assets/covers/surprised_1.svg", "linear-gradient(135deg, #F59E0B, #EC4899)",
                "surprised_edm_drop",
                "[00:08] Hold on tight, the drop is near!\n[00:20] Bass vibrating loud and clear!\n[00:32] Three, two, one, blast into space!\n[00:44] Unexpected rhythm hits your face!"));

        addSong(new Song("s-602", "Cyber Overdrive", "Pixel Rave", "Arcade 3000", 192,
                Mood.SURPRISED, 150, "Future Bass",
                "assets/covers/surprised_2.svg", "linear-gradient(135deg, #10B981, #F59E0B)",
                "surprised_future_bass",
                "[00:10] Level up, game on, surprise attack!\n[00:22] Synthesizer spinning off the track!\n[00:34] Glitch effects flashing gold and violet\n[00:46] Full throttle, no autopilot!"));

        addSong(new Song("s-603", "Pulse of the Night", "Electro Shock", "Voltage", 225,
                Mood.SURPRISED, 138, "Progressive House",
                "assets/covers/surprised_3.svg", "linear-gradient(135deg, #8B5CF6, #06B6D4)",
                "surprised_prog_house",
                "[00:18] Sudden transition takes you high\n[00:36] Sparks exploding in the midnight sky\n[00:54] Did you see that coming? Feel the bass!\n[01:12] Purple laser lighting up the place!"));

        addSong(new Song("s-604", "Cosmic Wonder", "Starlight Explorer", "Supernova", 214,
                Mood.SURPRISED, 132, "Electro Pop",
                "assets/covers/surprised_4.svg", "linear-gradient(135deg, #D8B4FE, #E11D48)",
                "surprised_cosmic_electro",
                "[00:12] A supernova right before your eyes\n[00:26] Shockwaves tearing through the starry skies\n[00:40] Wonder in the air tonight\n[00:54] Everything is glowing purple bright!"));
    }

    private void seedInitialPlaylists() {
        // Mood-based playlists
        Playlist happyMix = new Playlist("pl-happy", "Euphoria: Happy Hits",
                "Feel-good dance, upbeat pop, and radiant sunshine for your highest spirits.",
                "linear-gradient(135deg, #8B5CF6, #F59E0B)", "assets/covers/happy_1.svg", false);
        happyMix.setSongIds(Arrays.asList("s-101", "s-102", "s-103", "s-104"));
        playlists.put(happyMix.getId(), happyMix);

        Playlist sadMix = new Playlist("pl-sad", "Melancholy & Rain",
                "Soulful piano, acoustic reflections, and deep emotional ballads for rainy nights.",
                "linear-gradient(135deg, #312E81, #6366F1)", "assets/covers/sad_1.svg", false);
        sadMix.setSongIds(Arrays.asList("s-201", "s-202", "s-203", "s-204"));
        playlists.put(sadMix.getId(), sadMix);

        Playlist angryMix = new Playlist("pl-angry", "Neon Fury: High Energy",
                "Intense rock, hard-hitting electronic drops, and fierce beats to fuel your fire.",
                "linear-gradient(135deg, #DC2626, #8B5CF6)", "assets/covers/angry_1.svg", false);
        angryMix.setSongIds(Arrays.asList("s-301", "s-302", "s-303", "s-304"));
        playlists.put(angryMix.getId(), angryMix);

        Playlist relaxedMix = new Playlist("pl-relaxed", "Midnight Purple Lofi",
                "Velvet instrumentals, ambient rain, and peaceful chillhop for ultimate focus & relaxation.",
                "linear-gradient(135deg, #06B6D4, #8B5CF6)", "assets/covers/relaxed_1.svg", false);
        relaxedMix.setSongIds(Arrays.asList("s-401", "s-402", "s-403", "s-404"));
        playlists.put(relaxedMix.getId(), relaxedMix);

        Playlist neutralMix = new Playlist("pl-neutral", "Today's Top Hits",
                "The hottest global tracks, daily discoveries, and trending songs right now.",
                "linear-gradient(135deg, #8B5CF6, #EC4899)", "assets/covers/neutral_1.svg", false);
        neutralMix.setSongIds(Arrays.asList("s-501", "s-502", "s-503", "s-504"));
        playlists.put(neutralMix.getId(), neutralMix);

        Playlist surprisedMix = new Playlist("pl-surprised", "Electric Shock: Discovery",
                "Breathtaking festival drops, unexpected future bass, and jaw-dropping soundscapes.",
                "linear-gradient(135deg, #F59E0B, #8B5CF6)", "assets/covers/surprised_1.svg", false);
        surprisedMix.setSongIds(Arrays.asList("s-601", "s-602", "s-603", "s-604"));
        playlists.put(surprisedMix.getId(), surprisedMix);

        // General playlists
        Playlist topPicks = new Playlist("pl-toppicks", "Midnight Purple Essentials",
                "The signature sound of MUSIX. Handpicked tracks steeped in deep purple vibrations.",
                "linear-gradient(135deg, #100B1D, #8B5CF6)", "assets/covers/happy_1.svg", false);
        topPicks.setSongIds(Arrays.asList("s-101", "s-401", "s-501", "s-301", "s-601", "s-201"));
        playlists.put(topPicks.getId(), topPicks);
    }

    public void addSong(Song song) {
        songs.put(song.getId(), song);
    }

    public List<Song> getAllSongs() {
        return new ArrayList<>(songs.values());
    }

    public Song getSongById(String id) {
        return songs.get(id);
    }

    public List<Song> getSongsByMood(Mood mood) {
        return songs.values().stream()
                .filter(s -> s.getMood() == mood)
                .collect(Collectors.toList());
    }

    public List<Song> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllSongs();
        }
        String q = query.trim().toLowerCase();
        return songs.values().stream()
                .filter(s -> s.getTitle().toLowerCase().contains(q) ||
                             s.getArtist().toLowerCase().contains(q) ||
                             s.getAlbum().toLowerCase().contains(q) ||
                             s.getGenre().toLowerCase().contains(q) ||
                             s.getMood().name().toLowerCase().contains(q))
                .collect(Collectors.toList());
    }

    public boolean toggleLike(String songId) {
        Song song = songs.get(songId);
        if (song != null) {
            boolean newState = !song.isLiked();
            song.setLiked(newState);
            if (newState) {
                likedSongIds.add(songId);
            } else {
                likedSongIds.remove(songId);
            }
            return newState;
        }
        return false;
    }

    public List<Song> getLikedSongs() {
        List<Song> result = new ArrayList<>();
        synchronized (likedSongIds) {
            for (String id : likedSongIds) {
                Song s = songs.get(id);
                if (s != null) result.add(s);
            }
        }
        return result;
    }

    public void recordRecentlyPlayed(String songId) {
        if (songId == null || !songs.containsKey(songId)) return;
        synchronized (recentlyPlayedIds) {
            recentlyPlayedIds.remove(songId);
            recentlyPlayedIds.add(0, songId);
            if (recentlyPlayedIds.size() > 20) {
                recentlyPlayedIds.remove(recentlyPlayedIds.size() - 1);
            }
        }
    }

    public void clearRecentlyPlayed() {
        synchronized (recentlyPlayedIds) {
            recentlyPlayedIds.clear();
        }
    }

    public List<Song> getRecentlyPlayed() {
        List<Song> result = new ArrayList<>();
        synchronized (recentlyPlayedIds) {
            for (String id : recentlyPlayedIds) {
                Song s = songs.get(id);
                if (s != null) result.add(s);
            }
        }
        if (result.isEmpty()) {
            // Default first 6 tracks if none played yet
            result = getAllSongs().stream().limit(6).collect(Collectors.toList());
        }
        return result;
    }

    public List<Playlist> getAllPlaylists() {
        return new ArrayList<>(playlists.values());
    }

    public Playlist getPlaylistById(String id) {
        return playlists.get(id);
    }

    public Playlist createPlaylist(String name, String description, List<String> songIds) {
        String id = "pl-" + UUID.randomUUID().toString().substring(0, 8);
        Playlist pl = new Playlist(id, name, description,
                "linear-gradient(135deg, #8B5CF6, #3B276D)",
                "assets/covers/happy_1.svg", true);
        if (songIds != null) {
            for (String sid : songIds) {
                pl.addSong(sid);
            }
        }
        playlists.put(id, pl);
        return pl;
    }

    public boolean addSongToPlaylist(String playlistId, String songId) {
        Playlist pl = playlists.get(playlistId);
        if (pl != null && songs.containsKey(songId)) {
            pl.addSong(songId);
            return true;
        }
        return false;
    }

    public boolean removeSongFromPlaylist(String playlistId, String songId) {
        Playlist pl = playlists.get(playlistId);
        if (pl != null) {
            pl.removeSong(songId);
            return true;
        }
        return false;
    }
}
