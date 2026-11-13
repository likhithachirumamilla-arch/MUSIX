# MUSIX – AI-Powered Music Streaming Platform

> **Theme**: Midnight Purple  
> **Background**: `#100B1D` | **Primary**: `#8B5CF6` | **Accent**: `#D8B4FE` | **Text**: `#FFFFFF`

A full-stack music streaming web application designed with the layout, ergonomics, and responsiveness of **Spotify**, infused with a **Midnight Purple** neon aesthetic and powered by a unique **AI Face Search** biometric mood recommendation engine.

---

## 🌟 Key Features

### 1. Spotify-Inspired UI/UX & Midnight Purple Design
- **Left Sidebar**: MUSIX logo with modern music icon, Home, Search, Your Library, Liked Songs, **Recently Played**, and a highlighted **Face Search** button with animated purple glow and "AI MOOD" badge.
- **Top Navigation Bar**: Glassmorphism blurred header with back/forward history navigation, live search bar, user profile dropdown ("Lucky"), **Settings button**, and quick Face Mood Scanner trigger.
- **Main Content**:
  - Time-of-day greeting ("Good morning", "Good afternoon", "Good evening").
  - 6-card quick hero grid with big album artwork and glowing hover play buttons.
  - "Recently Played", "Made For You", **"Mood-Based Playlists"** (with 6 interactive gradient banners), "Trending on MUSIX", "Popular Artists", and "Recommended Albums".
- **Responsive Layout**: Designed for desktops, tablets, and mobile displays.

### 2. Flagship Feature: AI Face Search (Biometric Facial Mood Recognition)
- **Live Webcam Analysis**: Directly streams video through HTML5 WebRTC.
- **Biometric HUD Overlay**: Cyberpunk targeting brackets, targeting reticle, and **glowing circular scanning animation** with rotating radar sweep.
- **Facial Expression Geometry Engine**:
  - Analyzes real-time video frames to detect expression landmarks (smile curvature, mouth contrast, brow slope and tension, eye openness).
  - Classifies emotions across 6 distinct moods:
    - 😄 **Happy**: Upbeat dance, feel-good pop, sunny summer anthems.
    - 😢 **Sad**: Soulful acoustic, melancholic piano, emotional ballads.
    - 😡 **Angry**: Fierce pop, hard rock, aggressive synths, high-BPM workout.
    - 🧘 **Relaxed**: Lo-fi hip-hop, ambient textures, serene instrumentals.
    - 😐 **Neutral**: Global trending hits & personalized daily mix.
    - 😲 **Surprised**: Exhilarating EDM drops, future bass, festival discoveries.
  - Generates a **Personalized Mood Mix** with 1-click "Play Mood Mix Now".
  - **Graceful Fallbacks**: Includes instant 1-click Preset Mood test chips and a Photo/Selfie upload option.
  - **Privacy First**: 100% of facial processing happens locally in the browser sandbox. No webcam video or photos are ever saved or uploaded.

### 3. Fully Functional Music Player
- Fixed bottom player bar with Spotify layout:
  - **Left**: Currently playing track cover art, song title, artist, and interactive animated heart toggle (Like/Unlike).
  - **Center**: Shuffle, Previous, Play/Pause circle with purple hover glow, Next, Repeat (Off / All / One), and interactive progress scrubber (seekable with hover thumb).
  - **Right**:
    - **Animated Frequency Visualizer**: Real-time canvas spectrum reacting to the music beat.
    - **Synchronized Karaoke Lyrics Modal**: Scrolling synchronized song lyrics.
    - **Upcoming Play Queue Drawer**: Collapsible slide-in drawer showing upcoming tracks.
    - **Volume Slider**: Click-to-mute icon with interactive purple gradient volume slider.
- **Real Procedural Audio Engine (`audio-synthesizer.js`)**:
  - Uses the **Web Audio API** to generate rich, dynamic, multi-instrumental compositions (drums, 808 bass, 7th chords, leads, arpeggios, lofi vinyl textures) matching each track's mood and BPM.
  - Zero external copyright restrictions or broken audio URLs; 100% reliable sound playback!

### 4. Search, Playlists & Library
- **Live Search**: Instant keyword filtering across songs, artists, albums, genres, and moods.
- **Spotify Genre Tiles**: 45-degree angled cover art tiles with vibrant gradients.
- **Playlists & Liked Songs**:
  - Create new playlists with custom descriptions.
  - **Add any song to playlist** via the `+` action button with instant playlist selector.
  - **Remove songs from playlists** via the `×` action button on custom playlists.
  - Like/unlike tracks with instant Toast notifications.
  - Liked Songs dedicated view with header art and track table.
  - Dedicated **Recently Played** view with listening history and "Clear History" button.
  - Data synchronizes with backend REST APIs and persists in `localStorage` across page refreshes.

### 5. Authentication & Settings
- **Auth Modal**:
  - Sign In and Create Account tabs.
  - Email & password validation.
  - Password visibility toggle (eye icon).
  - Fast Demo Sign-In ("Lucky") with 1 click.
- **Settings Modal**:
  - Theme Switcher: Midnight Purple (Default), Cyber Violet, Deep Neon.
  - Streaming Audio Quality: Lossless (320kbps), High (256kbps), Normal (160kbps), Data Saver (96kbps).
  - Biometric Privacy status pill & Live HUD overlay toggle.
  - Audio visualizer styles (Bars, Waveform, Pulse).
  - Data management (Clear recently played, Reset playlists).

---

## 📁 Project Structure

```
musix/
├── src/
│   └── com/
│       └── musix/
│           ├── Main.java                          # Standalone Java entry point with automatic port fallback
│           ├── boot/
│           │   ├── MusixSpringBootApplication.java# Spring Boot 3.x Application entry point (mvn spring-boot:run)
│           │   └── MusixApiController.java        # Spring Boot RestController with full REST endpoints
│           ├── model/
│           │   ├── Mood.java                      # Mood enum (Happy, Sad, Angry, Relaxed, Neutral, Surprised)
│           │   ├── Song.java                      # Song data model with duration, lyrics, mood, tone
│           │   ├── Playlist.java                  # Playlist data model
│           │   └── User.java                      # User authentication model
│           ├── service/
│           │   ├── MusicService.java              # In-memory song catalog (24+ tracks), playlists & likes
│           │   └── MoodAnalyzerService.java       # AI facial mood analysis & recommendation logic
│           └── server/
│               ├── MusixHttpServer.java           # Multithreaded pure Java HTTP Web Server (Zero dependencies)
│               ├── ApiHandler.java                # REST API request handler
│               ├── StaticFileHandler.java         # Static asset file server
│               └── JsonUtils.java                 # Pure Java JSON serializer & parser
├── web/
│   ├── index.html                                 # Spotify-style single-page music streaming interface
│   ├── css/
│   │   ├── style.css                              # Midnight Purple theme, layout, components
│   │   └── face-scanner.css                       # Biometric HUD, glowing circular radar, emotion meters
│   ├── js/
│   │   ├── app.js                                 # Navigation, views, playlists, API synchronization
│   │   ├── player.js                              # Spotify music player controls, queue, lyrics, visualizer
│   │   ├── face-scanner.js                        # WebRTC webcam, canvas HUD, expression classifier
│   │   └── audio-synthesizer.js                   # Web Audio API procedural music synthesis
│   └── assets/
│       └── covers/                                # 27 custom SVG album covers and branding graphics
├── bin/                                           # Compiled Java .class files
├── run.bat                                        # One-click Windows batch launch script
├── run.ps1                                        # PowerShell launch script
├── compile.bat                                    # One-click compilation script
├── pom.xml                                        # Maven / Spring Boot 3.x project descriptor
└── README.md                                      # Documentation
```

---

## 🚀 How to Run the Application

### Method 1: Using Windows Batch Script (Fastest & Zero Dependencies)
Simply double-click:
```bat
run.bat
```
or open PowerShell/CMD in the project directory and run:
```cmd
.\run.bat
```

### Method 2: Using PowerShell Script
```powershell
.\run.ps1
```

### Method 3: Using Maven & Spring Boot
```cmd
mvn spring-boot:run
```

Once started, open your web browser at:
👉 **`http://localhost:8080`**

---

## 📡 REST API Reference

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/status` | Returns backend status, song count, and theme metadata |
| `GET` | `/api/songs` | Returns all curated songs in the catalog |
| `GET` | `/api/songs/mood/{mood}` | Returns songs filtered by mood (HAPPY, SAD, ANGRY, RELAXED, NEUTRAL, SURPRISED) |
| `POST` | `/api/mood/analyze` | Receives mood & facial scores, returns curated playlist & insight |
| `GET` | `/api/search?q={query}` | Searches songs, artists, albums, genres |
| `GET` | `/api/playlists` | Returns all playlists |
| `POST` | `/api/playlists` | Creates a new playlist (`{"name":"...","description":"..."}`) |
| `POST` | `/api/playlists/{id}/songs` | Adds a song to a playlist (`{"songId":"..."}`) |
| `DELETE` | `/api/playlists/{id}/songs/{songId}` | Removes a song from a playlist |
| `POST` | `/api/like` | Toggles like on a song (`{"songId":"..."}`) |
| `GET` | `/api/liked` | Returns all liked songs |
| `GET` | `/api/recently-played` | Returns recently played track history |
| `POST` | `/api/recently-played` | Records a song play (`{"songId":"..."}`) |
| `DELETE` | `/api/recently-played` | Clears recently played history |
| `POST` | `/api/auth/login` | User authentication |
| `POST` | `/api/auth/register` | New user account creation |
| `POST` | `/api/auth/logout` | User logout |

---

## 🎨 Theme Palette Reference
- **Background**: `#100B1D` (Midnight Purple base)
- **Primary**: `#8B5CF6` (Vibrant electric purple)
- **Accent**: `#D8B4FE` (Soft lavender accent)
- **Text**: `#FFFFFF` (Crisp white)
- **Surface**: `#1E1538` / `#261B42` (Elevated dark card surfaces)
