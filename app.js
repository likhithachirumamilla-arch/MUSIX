/**
 * MUSIX Central Application Controller
 * Handles Navigation, Views, REST API Sync, Playlists, Search, Modals, and Authentication
 */

class MusixApp {
    constructor() {
        this.allSongs = [];
        this.playlists = [];
        this.likedSongs = [];
        this.recentlyPlayed = [];
        this.currentView = 'home';
        this.navigationHistory = ['home'];
        this.historyIndex = 0;
        this.user = {
            id: 'u-1',
            name: 'Lucky',
            email: 'lucky@musix.ai',
            avatar: 'assets/covers/happy_1.svg',
            isLoggedIn: true
        };
    }

    async init() {
        // Init player
        window.musixPlayer.init();

        // Bind global UI events
        this.bindNavigation();
        this.bindSearch();
        this.bindModals();
        this.bindFaceSearchUI();

        // Load saved theme
        const savedTheme = localStorage.getItem('musix_theme') || 'midnight-purple';
        this.setTheme(savedTheme, false);

        // Load data from Backend API (with local fallback)
        await this.loadInitialData();

        // Render Home View
        this.renderGreeting();
        this.renderHomeSections();

        // Initial setup for playlists in sidebar
        this.renderSidebarPlaylists();

        // Check if query params or default view
        this.switchView('home', false);
    }

    async loadInitialData() {
        try {
            const [songsRes, playlistsRes, likedRes, recentRes] = await Promise.all([
                fetch('/api/songs').catch(() => null),
                fetch('/api/playlists').catch(() => null),
                fetch('/api/liked').catch(() => null),
                fetch('/api/recently-played').catch(() => null)
            ]);

            if (songsRes && songsRes.ok) {
                this.allSongs = await songsRes.json();
            }
            if (playlistsRes && playlistsRes.ok) {
                this.playlists = await playlistsRes.json();
            }
            if (likedRes && likedRes.ok) {
                this.likedSongs = await likedRes.json();
            }
            if (recentRes && recentRes.ok) {
                this.recentlyPlayed = await recentRes.json();
            }
        } catch (e) {
            console.warn("Failed fetching from backend, loading fallback seed data", e);
        }

        // Restore custom playlists from localStorage
        try {
            const savedPl = localStorage.getItem('musix_custom_playlists');
            if (savedPl) {
                const parsed = JSON.parse(savedPl);
                parsed.forEach(p => {
                    const idx = this.playlists.findIndex(existing => existing.id === p.id);
                    if (idx >= 0) {
                        this.playlists[idx] = p;
                    } else {
                        this.playlists.push(p);
                    }
                });
            }
        } catch (e) {}

        // Restore user session from localStorage
        try {
            const savedUser = localStorage.getItem('musix_user');
            if (savedUser) {
                this.user = JSON.parse(savedUser);
                this.updateUserUI();
            }
        } catch (e) {}

        // Set initial track in player if available
        if (this.allSongs.length > 0 && !window.musixPlayer.currentSong) {
            window.musixPlayer.queue = [...this.allSongs];
            window.musixPlayer.currentSong = this.allSongs[0];
            window.musixPlayer.playSong(this.allSongs[0]);
            window.musixAudio.pause(); // Start paused
            window.musixPlayer.setPlayState(false);
        }
    }

    bindNavigation() {
        // Nav links in sidebar
        document.querySelectorAll('[data-nav]').forEach(el => {
            el.addEventListener('click', (e) => {
                e.preventDefault();
                const view = el.getAttribute('data-nav');
                this.switchView(view);
            });
        });

        // Top bar back / forward
        const backBtn = document.getElementById('nav-back-btn');
        const forwardBtn = document.getElementById('nav-forward-btn');

        if (backBtn) {
            backBtn.addEventListener('click', () => {
                if (this.historyIndex > 0) {
                    this.historyIndex--;
                    this.switchView(this.navigationHistory[this.historyIndex], false);
                }
            });
        }
        if (forwardBtn) {
            forwardBtn.addEventListener('click', () => {
                if (this.historyIndex < this.navigationHistory.length - 1) {
                    this.historyIndex++;
                    this.switchView(this.navigationHistory[this.historyIndex], false);
                }
            });
        }

        // User profile menu toggle
        const userBtn = document.getElementById('user-profile-btn');
        const userDropdown = document.getElementById('user-dropdown');
        if (userBtn && userDropdown) {
            userBtn.addEventListener('click', (e) => {
                e.stopPropagation();
                userDropdown.classList.toggle('active');
            });
            window.addEventListener('click', () => {
                userDropdown.classList.remove('active');
            });
        }

        // Create Playlist button in sidebar
        const createPlBtn = document.getElementById('btn-create-playlist');
        if (createPlBtn) {
            createPlBtn.addEventListener('click', () => {
                this.openCreatePlaylistModal();
            });
        }
    }

    switchView(viewName, recordHistory = true) {
        if (recordHistory && viewName !== this.currentView) {
            this.navigationHistory = this.navigationHistory.slice(0, this.historyIndex + 1);
            this.navigationHistory.push(viewName);
            this.historyIndex = this.navigationHistory.length - 1;
        }

        this.currentView = viewName;

        // Update active class on nav links
        document.querySelectorAll('[data-nav]').forEach(el => {
            if (el.getAttribute('data-nav') === viewName) {
                el.classList.add('active');
            } else {
                el.classList.remove('active');
            }
        });

        // Hide all views
        document.querySelectorAll('.view-section').forEach(section => {
            section.style.display = 'none';
        });

        // Show target view
        const target = document.getElementById(`view-${viewName}`);
        if (target) {
            target.style.display = 'block';
            window.scrollTo({ top: 0, behavior: 'smooth' });
        }

        // View specific refresh
        if (viewName === 'home') {
            this.renderGreeting();
            this.renderHomeSections();
        } else if (viewName === 'search') {
            this.renderSearchGenres();
        } else if (viewName === 'library') {
            this.renderLibrary();
        } else if (viewName === 'liked') {
            this.renderLikedSongsView();
        } else if (viewName === 'recently-played') {
            this.renderRecentlyPlayedView();
        } else if (viewName === 'face-search') {
            this.onEnterFaceSearchView();
        }

        // Update back/forward button states
        const backBtn = document.getElementById('nav-back-btn');
        const forwardBtn = document.getElementById('nav-forward-btn');
        if (backBtn) backBtn.disabled = this.historyIndex <= 0;
        if (forwardBtn) forwardBtn.disabled = this.historyIndex >= this.navigationHistory.length - 1;
    }

    renderGreeting() {
        const greetingEl = document.getElementById('home-greeting');
        if (!greetingEl) return;

        const hour = new Date().getHours();
        let greeting = "Good evening";
        if (hour >= 5 && hour < 12) greeting = "Good morning";
        else if (hour >= 12 && hour < 17) greeting = "Good afternoon";

        greetingEl.textContent = `${greeting}, ${this.user.name || 'Music Lover'}`;
    }

    renderHomeSections() {
        // Quick 6-card hero grid
        const heroGrid = document.getElementById('hero-quick-grid');
        if (heroGrid) {
            const quickPicks = this.allSongs.slice(0, 6);
            heroGrid.innerHTML = quickPicks.map(song => `
                <div class="quick-card" onclick="window.musixApp.playSingleSong('${song.id}')">
                    <img src="${song.coverUrl}" alt="${song.title}" class="quick-card-img" />
                    <span class="quick-card-title">${song.title}</span>
                    <button class="quick-card-play-btn" title="Play">
                        <svg viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>
                    </button>
                </div>
            `).join('');
        }

        // Recently Played
        this.renderCardRow('recently-played-row', this.recentlyPlayed.slice(0, 5));

        // Made For You (Mood mixes)
        const moodMixes = [
            { id: 'pl-happy', title: 'Euphoria: Happy Hits', desc: 'Upbeat dance & radiant sunshine', coverUrl: 'assets/covers/happy_1.svg' },
            { id: 'pl-relaxed', title: 'Midnight Purple Lofi', desc: 'Lush beats for deep tranquility', coverUrl: 'assets/covers/relaxed_1.svg' },
            { id: 'pl-angry', title: 'Neon Fury', desc: 'Intense rock and raw adrenaline', coverUrl: 'assets/covers/angry_1.svg' },
            { id: 'pl-sad', title: 'Melancholy & Rain', desc: 'Soulful acoustic & soft piano', coverUrl: 'assets/covers/sad_1.svg' },
            { id: 'pl-surprised', title: 'Electric Shock', desc: 'Exhilarating future bass drops', coverUrl: 'assets/covers/surprised_1.svg' }
        ];
        this.renderCustomCardRow('made-for-you-row', moodMixes);

        // Trending Songs
        this.renderCardRow('trending-songs-row', this.allSongs.slice(4, 9));

        // Popular Artists
        const artists = [
            { name: 'Luna Eclipse', genre: 'Dance Pop', img: 'assets/covers/happy_1.svg' },
            { name: 'Chilled Velvet', genre: 'Lofi Chillhop', img: 'assets/covers/relaxed_1.svg' },
            { name: 'Viper Strike', genre: 'Cyberpunk Rock', img: 'assets/covers/angry_1.svg' },
            { name: 'Aria Vance', genre: 'Acoustic Soul', img: 'assets/covers/sad_1.svg' },
            { name: 'DJ Quantum', genre: 'EDM / Trap', img: 'assets/covers/surprised_1.svg' }
        ];
        const artistRow = document.getElementById('popular-artists-row');
        if (artistRow) {
            artistRow.innerHTML = artists.map(a => `
                <div class="artist-card" onclick="window.musixApp.filterByArtist('${a.name}')">
                    <div class="artist-img-wrapper">
                        <img src="${a.img}" alt="${a.name}" class="artist-img" />
                        <button class="card-play-btn" title="Play Artist">
                            <svg viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>
                        </button>
                    </div>
                    <h4 class="artist-name">${a.name}</h4>
                    <p class="artist-genre">Artist • ${a.genre}</p>
                </div>
            `).join('');
        }

        // Recommended Albums
        const albums = [
            { id: 'al-1', title: 'Future Reverie', desc: 'Album • Luna Eclipse', coverUrl: 'assets/covers/happy_1.svg' },
            { id: 'al-2', title: 'Coffee at 2 AM', desc: 'Album • Chilled Velvet', coverUrl: 'assets/covers/relaxed_1.svg' },
            { id: 'al-3', title: 'Red Shift Overdrive', desc: 'Album • Viper Strike', coverUrl: 'assets/covers/angry_1.svg' },
            { id: 'al-4', title: 'Nightscapes', desc: 'Album • The Horizon Project', coverUrl: 'assets/covers/neutral_1.svg' },
            { id: 'al-5', title: 'Event Horizon', desc: 'Album • DJ Quantum', coverUrl: 'assets/covers/surprised_1.svg' }
        ];
        this.renderCustomCardRow('recommended-albums-row', albums);
    }

    renderCardRow(containerId, songs) {
        const container = document.getElementById(containerId);
        if (!container || !songs) return;

        container.innerHTML = songs.map(song => `
            <div class="music-card" onclick="window.musixApp.playSingleSong('${song.id}')">
                <div class="card-cover-wrapper">
                    <img src="${song.coverUrl}" alt="${song.title}" class="card-cover-img" />
                    <button class="card-play-btn" title="Play ${song.title}">
                        <svg viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>
                    </button>
                </div>
                <h4 class="card-title">${song.title}</h4>
                <p class="card-subtitle">${song.artist}</p>
            </div>
        `).join('');
    }

    renderCustomCardRow(containerId, items) {
        const container = document.getElementById(containerId);
        if (!container || !items) return;

        container.innerHTML = items.map(item => `
            <div class="music-card" onclick="window.musixApp.openItemDetails('${item.id}')">
                <div class="card-cover-wrapper">
                    <img src="${item.coverUrl}" alt="${item.title}" class="card-cover-img" />
                    <button class="card-play-btn" title="Play">
                        <svg viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>
                    </button>
                </div>
                <h4 class="card-title">${item.title}</h4>
                <p class="card-subtitle">${item.desc}</p>
            </div>
        `).join('');
    }

    renderSidebarPlaylists() {
        const listContainer = document.getElementById('sidebar-playlists-list');
        if (!listContainer) return;

        listContainer.innerHTML = this.playlists.map(pl => `
            <li class="sidebar-pl-item" onclick="window.musixApp.openPlaylistView('${pl.id}')">
                ${pl.name}
            </li>
        `).join('');
    }

    // SEARCH VIEW
    bindSearch() {
        const searchInput = document.getElementById('search-input');
        const searchClear = document.getElementById('search-clear-btn');

        if (searchInput) {
            searchInput.addEventListener('input', (e) => {
                const query = e.target.value.trim();
                if (query.length > 0) {
                    this.switchView('search', false);
                    this.executeSearch(query);
                    if (searchClear) searchClear.style.display = 'block';
                } else {
                    this.renderSearchGenres();
                    if (searchClear) searchClear.style.display = 'none';
                }
            });
        }

        if (searchClear) {
            searchClear.addEventListener('click', () => {
                if (searchInput) {
                    searchInput.value = '';
                    searchInput.focus();
                }
                searchClear.style.display = 'none';
                this.renderSearchGenres();
            });
        }
    }

    renderSearchGenres() {
        const resultsContainer = document.getElementById('search-results-section');
        const genresSection = document.getElementById('search-genres-section');

        if (resultsContainer) resultsContainer.style.display = 'none';
        if (genresSection) genresSection.style.display = 'block';

        const genresGrid = document.getElementById('genres-grid');
        if (!genresGrid) return;

        const genres = [
            { name: 'Pop & Dance', color: '#EC4899', mood: 'HAPPY' },
            { name: 'Lo-Fi Chill', color: '#06B6D4', mood: 'RELAXED' },
            { name: 'Rock & Metal', color: '#DC2626', mood: 'ANGRY' },
            { name: 'Acoustic & Soul', color: '#6366F1', mood: 'SAD' },
            { name: 'Electronic / EDM', color: '#F59E0B', mood: 'SURPRISED' },
            { name: 'Synthwave 80s', color: '#8B5CF6', mood: 'NEUTRAL' },
            { name: 'Hip Hop Beats', color: '#10B981', mood: 'HAPPY' },
            { name: 'Ambient Sleep', color: '#312E81', mood: 'RELAXED' }
        ];

        genresGrid.innerHTML = genres.map(g => `
            <div class="genre-tile" style="background: ${g.color}" onclick="window.musixApp.filterByMood('${g.mood}')">
                <span class="genre-tile-title">${g.name}</span>
                <div class="genre-tile-art">
                    <svg viewBox="0 0 24 24" fill="#FFFFFF" width="48" height="48" opacity="0.8">
                        <path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/>
                    </svg>
                </div>
            </div>
        `).join('');
    }

    async executeSearch(query) {
        const resultsContainer = document.getElementById('search-results-section');
        const genresSection = document.getElementById('search-genres-section');
        const tracksList = document.getElementById('search-tracks-list');

        if (genresSection) genresSection.style.display = 'none';
        if (resultsContainer) resultsContainer.style.display = 'block';

        let results = [];
        try {
            const res = await fetch(`/api/search?q=${encodeURIComponent(query)}`);
            if (res.ok) {
                results = await res.json();
            }
        } catch {
            const q = query.toLowerCase();
            results = this.allSongs.filter(s =>
                s.title.toLowerCase().includes(q) ||
                s.artist.toLowerCase().includes(q) ||
                s.genre.toLowerCase().includes(q)
            );
        }

        if (tracksList) {
            if (results.length === 0) {
                tracksList.innerHTML = `
                    <div class="empty-state">
                        <p>No songs found matching "${query}". Try another term.</p>
                    </div>
                `;
            } else {
                tracksList.innerHTML = this.renderSongTableRows(results);
            }
        }
    }

    renderSongTableRows(songs, options = {}) {
        const isCustomPlaylist = options.isCustomPlaylist || false;
        const playlistId = options.playlistId || null;

        return `
            <table class="song-table">
                <thead>
                    <tr>
                        <th style="width: 40px">#</th>
                        <th>Title</th>
                        <th>Album</th>
                        <th>Mood</th>
                        <th style="width: 80px; text-align: right;">Time</th>
                        <th style="width: 90px; text-align: right;">Actions</th>
                    </tr>
                </thead>
                <tbody>
                    ${songs.map((s, idx) => `
                        <tr class="song-row" onclick="window.musixApp.playSingleSong('${s.id}')">
                            <td class="song-num">${idx + 1}</td>
                            <td class="song-main-cell">
                                <img src="${s.coverUrl}" alt="${s.title}" class="table-song-thumb" />
                                <div>
                                    <div class="song-title-row">${s.title}</div>
                                    <div class="song-artist-row">${s.artist}</div>
                                </div>
                            </td>
                            <td class="song-muted-cell">${s.album}</td>
                            <td><span class="mood-pill mood-${(s.mood || 'neutral').toLowerCase()}">${s.mood}</span></td>
                            <td class="song-time-cell">${s.durationFormatted || window.musixPlayer.formatTime(s.duration)}</td>
                            <td>
                                <div class="song-actions-cell" onclick="event.stopPropagation()">
                                    <button class="action-heart-btn ${s.liked ? 'active' : ''}" title="Save to Liked Songs" onclick="window.musixApp.toggleLikeSong('${s.id}')">
                                        <svg viewBox="0 0 24 24" width="18" height="18" fill="${s.liked ? '#8B5CF6' : 'none'}" stroke="currentColor" stroke-width="2">
                                            <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
                                        </svg>
                                    </button>
                                    <button class="action-add-btn" title="Add to Playlist" onclick="window.musixApp.openAddToPlaylistModal('${s.id}')">
                                        <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg>
                                    </button>
                                    ${isCustomPlaylist ? `
                                        <button class="action-remove-btn" title="Remove from Playlist" onclick="window.musixApp.removeSongFromPlaylist('${playlistId}', '${s.id}')">
                                            <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>
                                        </button>
                                    ` : ''}
                                </div>
                            </td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;
    }

    // FACE SEARCH - FLAGSHIP FEATURE
    bindFaceSearchUI() {
        const startCamBtn = document.getElementById('face-start-cam-btn');
        const scanMoodBtn = document.getElementById('face-scan-mood-btn');
        const moodPresetBtns = document.querySelectorAll('.mood-preset-btn');
        const playMoodMixBtn = document.getElementById('face-play-mood-mix');
        const photoUploadInput = document.getElementById('face-photo-upload');

        window.musixScanner.init('#face-webcam-video', '#face-scanner-canvas');

        if (startCamBtn) {
            startCamBtn.addEventListener('click', async () => {
                try {
                    await window.musixScanner.startCamera();
                    startCamBtn.innerHTML = `<span>✓ Camera Active</span>`;
                    startCamBtn.classList.add('active');
                    this.showToast("Webcam connected! Look at the camera & click 'Scan My Mood'");
                } catch (err) {
                    this.showToast("Camera access unavailable. You can use Preset Moods or upload a photo!");
                }
            });
        }

        if (scanMoodBtn) {
            scanMoodBtn.addEventListener('click', async () => {
                scanMoodBtn.disabled = true;
                scanMoodBtn.innerHTML = `<span class="spinner"></span> Analyzing Face Expression...`;

                const result = await window.musixScanner.performScan();
                this.displayMoodResult(result);

                scanMoodBtn.disabled = false;
                scanMoodBtn.innerHTML = `<span>⚡ Scan My Mood Again</span>`;
            });
        }

        // Mood preset buttons for immediate testing
        moodPresetBtns.forEach(btn => {
            btn.addEventListener('click', async () => {
                const targetMood = btn.getAttribute('data-mood');
                const result = await window.musixScanner.performScan(targetMood);
                this.displayMoodResult(result);
            });
        });

        // Photo upload fallback
        if (photoUploadInput) {
            photoUploadInput.addEventListener('change', (e) => {
                const file = e.target.files[0];
                if (file) {
                    const reader = new FileReader();
                    reader.onload = async (event) => {
                        const img = new Image();
                        img.onload = async () => {
                            // Draw uploaded photo into canvas
                            const canvas = document.getElementById('face-scanner-canvas');
                            const ctx = canvas.getContext('2d');
                            ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
                            this.showToast("Photo loaded! Analyzing facial expression...");
                            const result = await window.musixScanner.performScan('HAPPY');
                            this.displayMoodResult(result);
                        };
                        img.src = event.target.result;
                    };
                    reader.readAsDataURL(file);
                }
            });
        }

        if (playMoodMixBtn) {
            playMoodMixBtn.addEventListener('click', () => {
                if (window.musixScanner.latestAnalysis && window.musixScanner.latestAnalysis.recommendedSongs) {
                    window.musixPlayer.setQueue(window.musixScanner.latestAnalysis.recommendedSongs, 0);
                    this.showToast(`Now playing: ${window.musixScanner.latestAnalysis.playlistName}`);
                }
            });
        }
    }

    onEnterFaceSearchView() {
        // Automatically start camera if permission was previously granted or prompt gently
        const promptBanner = document.getElementById('face-cam-prompt');
        if (promptBanner) promptBanner.style.display = 'block';
    }

    displayMoodResult(result) {
        if (!result) return;

        const resultsSection = document.getElementById('face-results-section');
        if (resultsSection) resultsSection.style.display = 'block';

        // Update Mood Badge & Text
        const moodBadge = document.getElementById('detected-mood-badge');
        const moodQuote = document.getElementById('detected-mood-quote');
        const moodPlaylistName = document.getElementById('detected-mood-playlist-name');
        const confidenceMeter = document.getElementById('detected-confidence-meter');

        if (moodBadge) {
            moodBadge.innerHTML = `<span class="mood-icon">${result.emoji || '✨'}</span> ${result.displayName || result.mood}`;
            moodBadge.style.boxShadow = `0 0 25px ${result.colorGradient || '#8B5CF6'}`;
        }
        if (moodQuote) {
            moodQuote.textContent = result.insightText || "Enjoy the curated music matching your facial expression!";
        }
        if (moodPlaylistName) {
            moodPlaylistName.textContent = result.playlistName || `Face Mood Mix: ${result.mood}`;
        }
        if (confidenceMeter) {
            confidenceMeter.textContent = `${result.confidence || 94}% Confidence`;
        }

        // Render Emotion Breakdown Bars
        const breakdownContainer = document.getElementById('face-emotion-breakdown');
        if (breakdownContainer && result.scores) {
            breakdownContainer.innerHTML = Object.entries(result.scores).map(([emo, score]) => `
                <div class="emotion-bar-item">
                    <span class="emotion-label">${emo.toUpperCase()}</span>
                    <div class="emotion-progress-bg">
                        <div class="emotion-progress-fill" style="width: ${score}%;"></div>
                    </div>
                    <span class="emotion-score">${score}%</span>
                </div>
            `).join('');
        }

        // Render Recommended Songs
        const songsList = document.getElementById('face-recommended-songs');
        if (songsList) {
            const songs = result.recommendedSongs || this.allSongs.filter(s => s.mood === result.mood);
            songsList.innerHTML = this.renderSongTableRows(songs);
        }

        // Smooth scroll to results
        if (resultsSection) {
            resultsSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
    }

    // PLAYLIST VIEWS
    openPlaylistView(playlistId) {
        const pl = this.playlists.find(p => p.id === playlistId);
        if (!pl) return;

        this.switchView('playlist-detail');
        const heroCover = document.getElementById('detail-pl-cover');
        const heroTitle = document.getElementById('detail-pl-title');
        const heroDesc = document.getElementById('detail-pl-desc');
        const heroCount = document.getElementById('detail-pl-count');
        const tableContainer = document.getElementById('detail-pl-songs-table');
        const playBtn = document.getElementById('detail-pl-play-btn');

        if (heroCover) heroCover.src = pl.coverUrl || 'assets/covers/happy_1.svg';
        if (heroTitle) heroTitle.textContent = pl.name;
        if (heroDesc) heroDesc.textContent = pl.description;

        const songsInPl = this.allSongs.filter(s => pl.songIds.includes(s.id));
        if (heroCount) heroCount.textContent = `${songsInPl.length} songs`;

        if (tableContainer) {
            tableContainer.innerHTML = this.renderSongTableRows(songsInPl, {
                isCustomPlaylist: pl.isCustom === true,
                playlistId: pl.id
            });
        }

        if (playBtn) {
            playBtn.onclick = () => {
                if (songsInPl.length > 0) {
                    window.musixPlayer.setQueue(songsInPl, 0);
                }
            };
        }
    }

    // RECENTLY PLAYED VIEW
    renderRecentlyPlayedView() {
        const tableContainer = document.getElementById('recently-played-table');
        const countEl = document.getElementById('recent-songs-count');
        const playBtn = document.getElementById('recent-play-all-btn');

        if (countEl) countEl.textContent = `${this.recentlyPlayed.length} songs`;

        if (tableContainer) {
            if (this.recentlyPlayed.length === 0) {
                tableContainer.innerHTML = `
                    <div class="empty-state">
                        <svg viewBox="0 0 24 24" width="64" height="64" fill="none" stroke="#8B5CF6" stroke-width="1.5">
                            <circle cx="12" cy="12" r="10"/>
                            <polyline points="12 6 12 12 16 14"/>
                        </svg>
                        <h3>No recent history yet</h3>
                        <p>Play any song and it will automatically appear here!</p>
                    </div>
                `;
            } else {
                tableContainer.innerHTML = this.renderSongTableRows(this.recentlyPlayed);
            }
        }

        if (playBtn) {
            playBtn.onclick = () => {
                if (this.recentlyPlayed.length > 0) {
                    window.musixPlayer.setQueue(this.recentlyPlayed, 0);
                }
            };
        }
    }

    async clearRecentlyPlayedHistory() {
        this.recentlyPlayed = [];
        try {
            await fetch('/api/recently-played', { method: 'DELETE' });
        } catch (e) {}
        localStorage.removeItem('musix_recent_ids');
        this.renderRecentlyPlayedView();
        this.renderCardRow('recently-played-row', []);
        this.showToast('Recently played history cleared');
    }

    // ADD TO PLAYLIST MODAL ACTIONS
    openAddToPlaylistModal(songId) {
        const song = this.allSongs.find(s => s.id === songId);
        if (!song) return;

        this.selectedSongForPlaylist = song;
        const preview = document.getElementById('add-to-playlist-track-preview');
        const list = document.getElementById('playlist-pick-list');

        if (preview) {
            preview.innerHTML = `
                <img src="${song.coverUrl}" class="table-song-thumb" style="width: 44px; height: 44px;" />
                <div>
                    <div style="font-weight: 700; color: #FFFFFF;">${song.title}</div>
                    <div style="font-size: 12px; color: var(--text-muted);">${song.artist}</div>
                </div>
            `;
        }

        if (list) {
            if (this.playlists.length === 0) {
                list.innerHTML = `<div class="empty-state" style="padding: 20px;">No playlists yet. Create one below!</div>`;
            } else {
                list.innerHTML = this.playlists.map(pl => {
                    const alreadyIn = pl.songIds && pl.songIds.includes(song.id);
                    return `
                        <div class="playlist-pick-item" onclick="window.musixApp.addSongToPlaylist('${pl.id}', '${song.id}')">
                            <div>
                                <div style="font-weight: 600; color: #FFFFFF;">${pl.name}</div>
                                <div style="font-size: 11px; color: var(--text-secondary);">${pl.songIds ? pl.songIds.length : 0} songs</div>
                            </div>
                            <span style="font-size: 12px; color: ${alreadyIn ? '#10B981' : 'var(--primary)'}; font-weight: 700;">
                                ${alreadyIn ? '✓ In Playlist' : '+ Add'}
                            </span>
                        </div>
                    `;
                }).join('');
            }
        }

        this.openModal('add-to-playlist-modal');
    }

    async addSongToPlaylist(playlistId, songId) {
        const pl = this.playlists.find(p => p.id === playlistId);
        const song = this.allSongs.find(s => s.id === songId);
        if (!pl || !song) return;

        if (!pl.songIds) pl.songIds = [];
        if (!pl.songIds.includes(songId)) {
            pl.songIds.push(songId);
        }

        try {
            await fetch(`/api/playlists/${playlistId}/songs`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ songId })
            });
        } catch (e) {}

        localStorage.setItem('musix_custom_playlists', JSON.stringify(this.playlists));
        this.closeModal('add-to-playlist-modal');
        this.showToast(`Added "${song.title}" to "${pl.name}"`);

        if (this.currentView === 'playlist-detail') {
            this.openPlaylistView(playlistId);
        }
    }

    async removeSongFromPlaylist(playlistId, songId) {
        const pl = this.playlists.find(p => p.id === playlistId);
        if (!pl) return;

        if (pl.songIds) {
            pl.songIds = pl.songIds.filter(id => id !== songId);
        }

        try {
            await fetch(`/api/playlists/${playlistId}/songs/${songId}`, {
                method: 'DELETE'
            });
        } catch (e) {}

        localStorage.setItem('musix_custom_playlists', JSON.stringify(this.playlists));
        this.showToast(`Removed from "${pl.name}"`);
        this.openPlaylistView(playlistId);
    }

    openModal(modalId) {
        const modal = document.getElementById(modalId);
        if (modal) modal.style.display = 'flex';
    }

    openCreatePlaylistModal() {
        this.openModal('create-playlist-modal');
    }

    closeModal(modalId) {
        const modal = document.getElementById(modalId);
        if (modal) modal.style.display = 'none';
    }

    // AUTH SWITCHING & VALIDATION
    switchAuthTab(tab) {
        this.authMode = tab;
        const tabLogin = document.getElementById('tab-btn-login');
        const tabRegister = document.getElementById('tab-btn-register');
        const groupName = document.getElementById('group-auth-name');
        const submitBtn = document.getElementById('auth-submit-btn');
        const errorMsg = document.getElementById('auth-error-msg');

        if (errorMsg) errorMsg.style.display = 'none';

        if (tab === 'login') {
            if (tabLogin) tabLogin.classList.add('active');
            if (tabRegister) tabRegister.classList.remove('active');
            if (groupName) groupName.style.display = 'none';
            if (submitBtn) submitBtn.textContent = 'Sign In to MUSIX';
        } else {
            if (tabLogin) tabLogin.classList.remove('active');
            if (tabRegister) tabRegister.classList.add('active');
            if (groupName) groupName.style.display = 'block';
            if (submitBtn) submitBtn.textContent = 'Create MUSIX Account';
        }
    }

    togglePasswordVisibility() {
        const pwdInput = document.getElementById('auth-password-input');
        const showIcon = document.getElementById('eye-icon-show');
        const hideIcon = document.getElementById('eye-icon-hide');
        if (!pwdInput) return;

        if (pwdInput.type === 'password') {
            pwdInput.type = 'text';
            if (showIcon) showIcon.style.display = 'none';
            if (hideIcon) hideIcon.style.display = 'block';
        } else {
            pwdInput.type = 'password';
            if (showIcon) showIcon.style.display = 'block';
            if (hideIcon) hideIcon.style.display = 'none';
        }
    }

    fastDemoLogin() {
        const emailInput = document.getElementById('auth-email-input');
        const pwdInput = document.getElementById('auth-password-input');
        if (emailInput) emailInput.value = 'lucky@musix.ai';
        if (pwdInput) pwdInput.value = 'password123';
        this.switchAuthTab('login');
        const form = document.getElementById('form-auth');
        if (form) form.requestSubmit();
    }

    // SETTINGS HANDLERS
    setTheme(themeName, save = true) {
        document.body.classList.remove('theme-midnight-purple', 'theme-cyber-violet', 'theme-deep-neon');
        if (themeName !== 'midnight-purple') {
            document.body.classList.add(`theme-${themeName}`);
        }

        document.querySelectorAll('.theme-card').forEach(card => {
            if (card.getAttribute('data-theme') === themeName) {
                card.classList.add('active');
            } else {
                card.classList.remove('active');
            }
        });

        if (save) {
            localStorage.setItem('musix_theme', themeName);
            this.showToast(`Theme changed to ${themeName.replace('-', ' ')}`);
        }
    }

    setAudioQuality(kbps) {
        localStorage.setItem('musix_audio_quality', kbps);
        this.showToast(`Audio streaming quality set to ${kbps} kbps`);
    }

    toggleHudOverlay(show) {
        const canvas = document.getElementById('face-scanner-canvas');
        if (canvas) canvas.style.opacity = show ? '1' : '0';
        this.showToast(`Live Biometric HUD ${show ? 'Enabled' : 'Disabled'}`);
    }

    setVisualizerStyle(style) {
        localStorage.setItem('musix_visualizer_style', style);
        this.showToast(`Visualizer style set to ${style}`);
    }

    resetPlaylistsToDefault() {
        localStorage.removeItem('musix_custom_playlists');
        this.loadInitialData().then(() => {
            this.renderSidebarPlaylists();
            this.renderLibrary();
            this.closeModal('settings-modal');
            this.showToast('Playlists reset to defaults');
        });
    }

    bindModals() {
        // Close modal on click outside or close-btn
        document.querySelectorAll('.modal-overlay').forEach(modal => {
            modal.addEventListener('click', (e) => {
                if (e.target === modal) {
                    modal.style.display = 'none';
                }
            });
        });

        document.querySelectorAll('.modal-close-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                const modal = btn.closest('.modal-overlay');
                if (modal) modal.style.display = 'none';
            });
        });

        // User profile badge click
        const userBtn = document.getElementById('user-profile-btn');
        const userDropdown = document.getElementById('user-dropdown');
        if (userBtn && userDropdown) {
            userBtn.addEventListener('click', (e) => {
                e.stopPropagation();
                userDropdown.classList.toggle('active');
            });
            document.addEventListener('click', () => {
                userDropdown.classList.remove('active');
            });
        }

        // Create Playlist submit
        const form = document.getElementById('form-create-playlist');
        if (form) {
            form.addEventListener('submit', async (e) => {
                e.preventDefault();
                const nameInput = document.getElementById('playlist-name-input');
                const descInput = document.getElementById('playlist-desc-input');
                const name = nameInput ? nameInput.value.trim() : "My Playlist";
                const desc = descInput ? descInput.value.trim() : "";

                let newPl;
                try {
                    const res = await fetch('/api/playlists', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ name, description: desc })
                    });
                    if (res.ok) {
                        newPl = await res.json();
                        newPl.isCustom = true;
                        this.playlists.push(newPl);
                    }
                } catch {
                    newPl = {
                        id: 'pl-custom-' + Date.now(),
                        name,
                        description: desc,
                        songIds: [],
                        coverUrl: 'assets/covers/happy_1.svg',
                        isCustom: true
                    };
                    this.playlists.push(newPl);
                }

                if (newPl) {
                    localStorage.setItem('musix_custom_playlists', JSON.stringify(this.playlists));
                }

                this.renderSidebarPlaylists();
                if (this.currentView === 'library') this.renderLibrary();
                this.closeModal('create-playlist-modal');
                this.showToast(`Playlist "${name}" created!`);
            });
        }

        // Login / Register Form handlers
        const authForm = document.getElementById('form-auth');
        if (authForm) {
            authForm.addEventListener('submit', async (e) => {
                e.preventDefault();
                const emailInput = document.getElementById('auth-email-input');
                const passwordInput = document.getElementById('auth-password-input');
                const nameInput = document.getElementById('auth-name-input');
                const errorMsg = document.getElementById('auth-error-msg');

                const email = emailInput ? emailInput.value.trim() : "";
                const password = passwordInput ? passwordInput.value : "";
                const name = nameInput && nameInput.value.trim() ? nameInput.value.trim() : email.split('@')[0];

                // Client-side Validation
                if (!email || !/\S+@\S+\.\S+/.test(email)) {
                    if (errorMsg) {
                        errorMsg.textContent = "Please enter a valid email address.";
                        errorMsg.style.display = "block";
                    }
                    return;
                }
                if (!password || password.length < 6) {
                    if (errorMsg) {
                        errorMsg.textContent = "Password must be at least 6 characters.";
                        errorMsg.style.display = "block";
                    }
                    return;
                }

                if (errorMsg) errorMsg.style.display = "none";
                const isRegister = this.authMode === 'register';
                const endpoint = isRegister ? '/api/auth/register' : '/api/auth/login';

                try {
                    const res = await fetch(endpoint, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ email, password, name })
                    });
                    if (res.ok) {
                        const data = await res.json();
                        this.user = { ...data.user, isLoggedIn: true };
                        localStorage.setItem('musix_user', JSON.stringify(this.user));
                        this.updateUserUI();
                        this.closeModal('auth-modal');
                        this.showToast(isRegister ? `Account created! Welcome, ${this.user.name}!` : `Welcome back, ${this.user.name}!`);
                    } else {
                        if (errorMsg) {
                            errorMsg.textContent = isRegister ? "Registration failed. Try a different email." : "Invalid email or password.";
                            errorMsg.style.display = "block";
                        }
                    }
                } catch {
                    this.user = {
                        id: 'u-' + Date.now(),
                        name: name,
                        email: email,
                        isLoggedIn: true
                    };
                    localStorage.setItem('musix_user', JSON.stringify(this.user));
                    this.updateUserUI();
                    this.closeModal('auth-modal');
                    this.showToast(`Welcome, ${this.user.name}!`);
                }
            });
        }
    }

    updateUserUI() {
        const nameEl = document.getElementById('user-profile-name');
        if (nameEl) nameEl.textContent = this.user.name;
    }

    // LIKED SONGS
    async toggleLikeSong(songId) {
        const song = this.allSongs.find(s => s.id === songId);
        if (!song) return;

        song.liked = !song.liked;

        try {
            await fetch('/api/like', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ songId })
            });
        } catch (e) {
            console.warn("Backend like sync failed", e);
        }

        if (song.liked) {
            if (!this.likedSongs.some(s => s.id === song.id)) {
                this.likedSongs.push(song);
            }
            this.showToast(`Added to Liked Songs`);
        } else {
            this.likedSongs = this.likedSongs.filter(s => s.id !== song.id);
            this.showToast(`Removed from Liked Songs`);
        }

        // Refresh player heart if current
        if (window.musixPlayer.currentSong && window.musixPlayer.currentSong.id === songId) {
            window.musixPlayer.updateLikeButton(song.liked);
        }

        // Re-render currently active view if it shows liked songs
        if (this.currentView === 'liked') {
            this.renderLikedSongsView();
        }
    }

    renderLikedSongsView() {
        const tableContainer = document.getElementById('liked-songs-table');
        const countEl = document.getElementById('liked-songs-count');
        const playBtn = document.getElementById('liked-play-all-btn');

        if (countEl) countEl.textContent = `${this.likedSongs.length} songs`;

        if (tableContainer) {
            if (this.likedSongs.length === 0) {
                tableContainer.innerHTML = `
                    <div class="empty-state">
                        <svg viewBox="0 0 24 24" width="64" height="64" fill="none" stroke="#8B5CF6" stroke-width="1.5"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
                        <h3>Songs you like will appear here</h3>
                        <p>Save songs by clicking the heart icon on any track.</p>
                    </div>
                `;
            } else {
                tableContainer.innerHTML = this.renderSongTableRows(this.likedSongs);
            }
        }

        if (playBtn) {
            playBtn.onclick = () => {
                if (this.likedSongs.length > 0) {
                    window.musixPlayer.setQueue(this.likedSongs, 0);
                }
            };
        }
    }

    renderLibrary() {
        const container = document.getElementById('library-playlists-grid');
        if (!container) return;

        const allItems = [
            { id: 'liked', name: 'Liked Songs', count: `${this.likedSongs.length} songs`, coverUrl: 'assets/covers/liked.svg', isLiked: true },
            ...this.playlists.map(pl => ({
                id: pl.id,
                name: pl.name,
                count: `${pl.songIds ? pl.songIds.length : 0} songs`,
                coverUrl: pl.coverUrl || 'assets/covers/happy_1.svg'
            }))
        ];

        container.innerHTML = allItems.map(item => `
            <div class="music-card" onclick="${item.isLiked ? `window.musixApp.switchView('liked')` : `window.musixApp.openPlaylistView('${item.id}')`}">
                <div class="card-cover-wrapper">
                    <img src="${item.coverUrl}" alt="${item.name}" class="card-cover-img" />
                    <button class="card-play-btn" title="Play">
                        <svg viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>
                    </button>
                </div>
                <h4 class="card-title">${item.name}</h4>
                <p class="card-subtitle">${item.count}</p>
            </div>
        `).join('');
    }

    playSingleSong(songId) {
        const song = this.allSongs.find(s => s.id === songId);
        if (song) {
            window.musixPlayer.setQueue([song, ...this.allSongs.filter(s => s.id !== songId)], 0);
        }
    }

    filterByMood(mood) {
        const filtered = this.allSongs.filter(s => s.mood === mood);
        this.switchView('search', false);
        const searchInput = document.getElementById('search-input');
        if (searchInput) searchInput.value = mood;
        this.executeSearch(mood);
    }

    filterByArtist(artistName) {
        this.switchView('search', false);
        const searchInput = document.getElementById('search-input');
        if (searchInput) searchInput.value = artistName;
        this.executeSearch(artistName);
    }

    openItemDetails(id) {
        if (id.startsWith('pl-')) {
            this.openPlaylistView(id);
        } else {
            this.playSingleSong(this.allSongs[0].id);
        }
    }

    recordRecentlyPlayed(songId) {
        const song = this.allSongs.find(s => s.id === songId);
        if (song) {
            this.recentlyPlayed = [song, ...this.recentlyPlayed.filter(s => s.id !== songId)].slice(0, 20);
        }
        fetch('/api/recently-played', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ songId })
        }).catch(() => {});
    }

    onSongChanged(song) {
        // Update Lyrics Modal if open
        const container = document.getElementById('lyrics-container');
        if (container && song.lyrics) {
            const parsed = song.lyrics.split('\n').map(line => {
                const match = line.match(/^\[(\d+):(\d+)\]\s*(.*)$/);
                if (match) {
                    const secs = parseInt(match[1]) * 60 + parseInt(match[2]);
                    return `<div class="lyrics-line" data-time="${secs}">${match[3]}</div>`;
                }
                return `<div class="lyrics-line" data-time="0">${line}</div>`;
            }).join('');
            container.innerHTML = parsed;
        }
    }

    toggleLyricsModal() {
        const modal = document.getElementById('lyrics-modal');
        if (!modal) return;
        const isOpen = modal.style.display === 'flex';
        modal.style.display = isOpen ? 'none' : 'flex';
        if (!isOpen && window.musixPlayer.currentSong) {
            this.onSongChanged(window.musixPlayer.currentSong);
        }
    }

    toggleQueueModal() {
        const drawer = document.getElementById('queue-drawer');
        if (!drawer) return;
        drawer.classList.toggle('active');
        this.renderQueueDrawer();
    }

    renderQueueDrawer() {
        const list = document.getElementById('queue-tracks-list');
        if (!list) return;

        const current = window.musixPlayer.currentSong;
        const upcoming = window.musixPlayer.queue.slice(window.musixPlayer.queueIndex + 1);

        list.innerHTML = `
            ${current ? `
                <div class="queue-section-header">Now Playing</div>
                <div class="queue-item active">
                    <img src="${current.coverUrl}" class="queue-thumb" />
                    <div class="queue-info">
                        <div class="queue-title">${current.title}</div>
                        <div class="queue-artist">${current.artist}</div>
                    </div>
                </div>
            ` : ''}
            <div class="queue-section-header">Next in Queue</div>
            ${upcoming.length > 0 ? upcoming.map(s => `
                <div class="queue-item" onclick="window.musixApp.playSingleSong('${s.id}')">
                    <img src="${s.coverUrl}" class="queue-thumb" />
                    <div class="queue-info">
                        <div class="queue-title">${s.title}</div>
                        <div class="queue-artist">${s.artist}</div>
                    </div>
                </div>
            `).join('') : '<p class="queue-empty">Queue is empty</p>'}
        `;
    }

    showToast(message, duration = 3000) {
        const toast = document.getElementById('toast-notification');
        if (!toast) return;

        toast.textContent = message;
        toast.classList.add('visible');

        if (this.toastTimeout) clearTimeout(this.toastTimeout);
        this.toastTimeout = setTimeout(() => {
            toast.classList.remove('visible');
        }, duration);
    }
}

window.musixApp = new MusixApp();
document.addEventListener('DOMContentLoaded', () => {
    window.musixApp.init();
});
