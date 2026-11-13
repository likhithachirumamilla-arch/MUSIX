/**
 * MUSIX Spotify-Inspired Bottom Music Player Controller
 */

class MusixPlayer {
    constructor() {
        this.currentSong = null;
        this.queue = [];
        this.queueIndex = 0;
        this.isShuffle = false;
        this.repeatMode = 'OFF'; // OFF, ALL, ONE
        this.visualizerCanvas = null;
        this.visualizerCtx = null;
        this.visualizerActive = true;
        this.visualizerAnimId = null;

        // DOM elements
        this.elements = {};
    }

    init() {
        this.bindDOMElements();
        this.bindEvents();
        this.initVisualizer();

        // Audio engine callbacks
        window.musixAudio.onProgressCallback = (currentTime, duration) => {
            this.updateProgressBar(currentTime, duration);
            this.updateLyrics(currentTime);
        };

        window.musixAudio.onEndedCallback = () => {
            this.handleTrackEnded();
        };
    }

    bindDOMElements() {
        this.elements = {
            playerBar: document.getElementById('player-bar'),
            coverArt: document.getElementById('player-cover-art'),
            songTitle: document.getElementById('player-song-title'),
            songArtist: document.getElementById('player-song-artist'),
            likeBtn: document.getElementById('player-like-btn'),
            playPauseBtn: document.getElementById('player-play-btn'),
            playIcon: document.getElementById('player-play-icon'),
            pauseIcon: document.getElementById('player-pause-icon'),
            prevBtn: document.getElementById('player-prev-btn'),
            nextBtn: document.getElementById('player-next-btn'),
            shuffleBtn: document.getElementById('player-shuffle-btn'),
            repeatBtn: document.getElementById('player-repeat-btn'),
            progressBar: document.getElementById('player-progress-bar'),
            progressFill: document.getElementById('player-progress-fill'),
            currentTime: document.getElementById('player-current-time'),
            totalDuration: document.getElementById('player-total-duration'),
            volumeSlider: document.getElementById('player-volume-slider'),
            volumeIcon: document.getElementById('player-volume-icon'),
            lyricsBtn: document.getElementById('player-lyrics-btn'),
            queueBtn: document.getElementById('player-queue-btn'),
            visualizerCanvas: document.getElementById('player-visualizer')
        };
    }

    bindEvents() {
        if (this.elements.playPauseBtn) {
            this.elements.playPauseBtn.addEventListener('click', () => this.togglePlayPause());
        }
        if (this.elements.prevBtn) {
            this.elements.prevBtn.addEventListener('click', () => this.playPrev());
        }
        if (this.elements.nextBtn) {
            this.elements.nextBtn.addEventListener('click', () => this.playNext());
        }
        if (this.elements.shuffleBtn) {
            this.elements.shuffleBtn.addEventListener('click', () => this.toggleShuffle());
        }
        if (this.elements.repeatBtn) {
            this.elements.repeatBtn.addEventListener('click', () => this.toggleRepeat());
        }
        if (this.elements.likeBtn) {
            this.elements.likeBtn.addEventListener('click', () => {
                if (this.currentSong) {
                    window.musixApp.toggleLikeSong(this.currentSong.id);
                }
            });
        }

        // Interactive Progress Bar seeking
        if (this.elements.progressBar) {
            let isSeeking = false;

            const handleSeek = (e) => {
                const rect = this.elements.progressBar.getBoundingClientRect();
                const clickX = e.clientX - rect.left;
                const ratio = Math.max(0, Math.min(1, clickX / rect.width));
                const targetSeconds = ratio * (this.currentSong ? this.currentSong.duration : 180);
                window.musixAudio.seek(targetSeconds);
            };

            this.elements.progressBar.addEventListener('mousedown', (e) => {
                isSeeking = true;
                handleSeek(e);
            });

            window.addEventListener('mousemove', (e) => {
                if (isSeeking) handleSeek(e);
            });

            window.addEventListener('mouseup', () => {
                isSeeking = false;
            });
        }

        // Volume control
        if (this.elements.volumeSlider) {
            this.elements.volumeSlider.addEventListener('input', (e) => {
                const val = parseFloat(e.target.value);
                window.musixAudio.setVolume(val);
                this.updateVolumeUI(val);
            });
        }

        if (this.elements.volumeIcon) {
            this.elements.volumeIcon.addEventListener('click', () => {
                const muted = window.musixAudio.toggleMute();
                this.updateVolumeUI(muted ? 0 : window.musixAudio.volume);
            });
        }

        // Lyrics Modal Toggle
        if (this.elements.lyricsBtn) {
            this.elements.lyricsBtn.addEventListener('click', () => {
                window.musixApp.toggleLyricsModal();
            });
        }

        // Queue Drawer Toggle
        if (this.elements.queueBtn) {
            this.elements.queueBtn.addEventListener('click', () => {
                window.musixApp.toggleQueueModal();
            });
        }
    }

    setQueue(songs, startIndex = 0) {
        if (!songs || songs.length === 0) return;
        this.queue = [...songs];
        this.queueIndex = Math.max(0, Math.min(startIndex, this.queue.length - 1));
        this.playSong(this.queue[this.queueIndex]);
    }

    playSong(song) {
        if (!song) return;
        this.currentSong = song;

        // Update UI
        if (this.elements.songTitle) this.elements.songTitle.textContent = song.title;
        if (this.elements.songArtist) this.elements.songArtist.textContent = song.artist;
        if (this.elements.coverArt) {
            this.elements.coverArt.src = song.coverUrl || 'assets/covers/happy_1.svg';
        }
        if (this.elements.totalDuration) {
            this.elements.totalDuration.textContent = song.durationFormatted || this.formatTime(song.duration);
        }
        if (this.elements.currentTime) {
            this.elements.currentTime.textContent = '0:00';
        }
        if (this.elements.progressFill) {
            this.elements.progressFill.style.width = '0%';
        }

        this.updateLikeButton(song.liked);

        // Record history in backend & app
        window.musixApp.recordRecentlyPlayed(song.id);

        // Start playing in Web Audio
        window.musixAudio.play(song);
        this.setPlayState(true);

        // Notify app
        window.musixApp.onSongChanged(song);
    }

    togglePlayPause() {
        if (!this.currentSong) {
            if (this.queue.length > 0) {
                this.playSong(this.queue[0]);
            }
            return;
        }

        if (window.musixAudio.isPlaying) {
            window.musixAudio.pause();
            this.setPlayState(false);
        } else {
            window.musixAudio.play(this.currentSong);
            this.setPlayState(true);
        }
    }

    playNext() {
        if (this.queue.length === 0) return;

        if (this.isShuffle) {
            this.queueIndex = Math.floor(Math.random() * this.queue.length);
        } else {
            this.queueIndex = (this.queueIndex + 1) % this.queue.length;
        }

        this.playSong(this.queue[this.queueIndex]);
    }

    playPrev() {
        if (this.queue.length === 0) return;

        // If played more than 3 seconds, restart current track
        if (window.musixAudio.currentTime > 3) {
            window.musixAudio.seek(0);
            return;
        }

        this.queueIndex = (this.queueIndex - 1 + this.queue.length) % this.queue.length;
        this.playSong(this.queue[this.queueIndex]);
    }

    toggleShuffle() {
        this.isShuffle = !this.isShuffle;
        if (this.elements.shuffleBtn) {
            this.elements.shuffleBtn.classList.toggle('active', this.isShuffle);
        }
        window.musixApp.showToast(this.isShuffle ? "Shuffle On" : "Shuffle Off");
    }

    toggleRepeat() {
        if (this.repeatMode === 'OFF') {
            this.repeatMode = 'ALL';
            if (this.elements.repeatBtn) {
                this.elements.repeatBtn.classList.add('active');
                this.elements.repeatBtn.setAttribute('title', 'Repeat: All');
            }
            window.musixApp.showToast("Repeat All");
        } else if (this.repeatMode === 'ALL') {
            this.repeatMode = 'ONE';
            if (this.elements.repeatBtn) {
                this.elements.repeatBtn.classList.add('active', 'repeat-one');
                this.elements.repeatBtn.setAttribute('title', 'Repeat: One');
            }
            window.musixApp.showToast("Repeat Current Track");
        } else {
            this.repeatMode = 'OFF';
            if (this.elements.repeatBtn) {
                this.elements.repeatBtn.classList.remove('active', 'repeat-one');
                this.elements.repeatBtn.setAttribute('title', 'Repeat: Off');
            }
            window.musixApp.showToast("Repeat Off");
        }
    }

    handleTrackEnded() {
        if (this.repeatMode === 'ONE') {
            window.musixAudio.seek(0);
            window.musixAudio.play(this.currentSong);
        } else if (this.repeatMode === 'ALL' || this.queueIndex < this.queue.length - 1) {
            this.playNext();
        } else {
            this.setPlayState(false);
        }
    }

    setPlayState(isPlaying) {
        if (this.elements.playIcon && this.elements.pauseIcon) {
            if (isPlaying) {
                this.elements.playIcon.style.display = 'none';
                this.elements.pauseIcon.style.display = 'block';
            } else {
                this.elements.playIcon.style.display = 'block';
                this.elements.pauseIcon.style.display = 'none';
            }
        }
    }

    updateProgressBar(currentTime, duration) {
        if (this.elements.currentTime) {
            this.elements.currentTime.textContent = this.formatTime(currentTime);
        }
        if (this.elements.progressFill) {
            const pct = Math.min(100, (currentTime / (duration || 180)) * 100);
            this.elements.progressFill.style.width = `${pct}%`;
        }
    }

    updateLikeButton(liked) {
        if (this.elements.likeBtn) {
            this.elements.likeBtn.classList.toggle('active', !!liked);
            this.elements.likeBtn.setAttribute('title', liked ? "Remove from Liked Songs" : "Save to Liked Songs");
            const svg = this.elements.likeBtn.querySelector('svg');
            if (svg) {
                svg.setAttribute('fill', liked ? '#8B5CF6' : 'none');
                svg.setAttribute('stroke', liked ? '#8B5CF6' : 'currentColor');
            }
        }
    }

    updateVolumeUI(val) {
        if (this.elements.volumeSlider) {
            this.elements.volumeSlider.value = val;
            this.elements.volumeSlider.style.background = `linear-gradient(to right, #8B5CF6 0%, #8B5CF6 ${val * 100}%, rgba(255,255,255,0.2) ${val * 100}%, rgba(255,255,255,0.2) 100%)`;
        }
    }

    formatTime(seconds) {
        const m = Math.floor(seconds / 60);
        const s = Math.floor(seconds % 60);
        return `${m}:${s < 10 ? '0' : ''}${s}`;
    }

    initVisualizer() {
        this.visualizerCanvas = this.elements.visualizerCanvas;
        if (!this.visualizerCanvas) return;
        this.visualizerCtx = this.visualizerCanvas.getContext('2d');

        const drawVisualizer = () => {
            if (this.visualizerActive && this.visualizerCtx) {
                const w = this.visualizerCanvas.width;
                const h = this.visualizerCanvas.height;
                this.visualizerCtx.clearRect(0, 0, w, h);

                const freqData = window.musixAudio.getFrequencyData();
                const bars = 8;
                const barWidth = 3;
                const gap = 2;
                const startX = (w - (bars * (barWidth + gap))) / 2;

                for (let i = 0; i < bars; i++) {
                    const val = window.musixAudio.isPlaying ? (freqData[i * 2] || 0) / 255 : 0.05;
                    const barHeight = Math.max(3, val * h * 0.9);
                    const x = startX + i * (barWidth + gap);
                    const y = h - barHeight;

                    // Midnight purple gradient for visualizer
                    const grad = this.visualizerCtx.createLinearGradient(0, y, 0, h);
                    grad.addColorStop(0, '#D8B4FE');
                    grad.addColorStop(1, '#8B5CF6');

                    this.visualizerCtx.fillStyle = grad;
                    this.visualizerCtx.beginPath();
                    this.visualizerCtx.roundRect(x, y, barWidth, barHeight, 2);
                    this.visualizerCtx.fill();
                }
            }
            this.visualizerAnimId = requestAnimationFrame(drawVisualizer);
        };

        drawVisualizer();
    }

    updateLyrics(currentTime) {
        const modal = document.getElementById('lyrics-container');
        if (!modal || !this.currentSong || !this.currentSong.lyrics) return;

        // Parse lyrics with timestamps
        // [mm:ss] text
        const lines = modal.querySelectorAll('.lyrics-line');
        lines.forEach(line => {
            const timeAttr = parseFloat(line.getAttribute('data-time') || 0);
            if (currentTime >= timeAttr && currentTime < timeAttr + 12) {
                line.classList.add('active');
                line.scrollIntoView({ behavior: 'smooth', block: 'center' });
            } else {
                line.classList.remove('active');
            }
        });
    }
}

window.musixPlayer = new MusixPlayer();
