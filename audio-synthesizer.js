/**
 * MUSIX Web Audio Procedural Synthesizer & Player Engine
 * Provides rich, mood-tailored musical playback with real-time frequency analysis.
 */
class MusixAudioEngine {
    constructor() {
        this.ctx = null;
        this.masterGain = null;
        this.analyser = null;
        this.isPlaying = false;
        this.currentSong = null;
        this.currentTime = 0;
        this.duration = 180;
        this.volume = 0.8;
        this.isMuted = false;
        this.timer = null;
        this.onProgressCallback = null;
        this.onEndedCallback = null;
        this.patternTimer = null;
        this.step = 0;
    }

    init() {
        if (!this.ctx) {
            const AudioCtx = window.AudioContext || window.webkitAudioContext;
            this.ctx = new AudioCtx();
            this.masterGain = this.ctx.createGain();
            this.masterGain.gain.setValueAtTime(this.volume, this.ctx.currentTime);

            this.analyser = this.ctx.createAnalyser();
            this.analyser.fftSize = 64;
            this.analyser.smoothingTimeConstant = 0.8;

            this.masterGain.connect(this.analyser);
            this.analyser.connect(this.ctx.destination);
        }
        if (this.ctx.state === 'suspended') {
            this.ctx.resume();
        }
    }

    loadSong(song) {
        this.stop();
        this.currentSong = song;
        this.duration = song.duration || 180;
        this.currentTime = 0;
        this.step = 0;
    }

    play(song) {
        this.init();
        if (song && (!this.currentSong || this.currentSong.id !== song.id)) {
            this.loadSong(song);
        }
        if (!this.currentSong) return;

        this.isPlaying = true;
        this.startLoop();

        if (this.timer) clearInterval(this.timer);
        this.timer = setInterval(() => {
            if (this.isPlaying) {
                this.currentTime++;
                if (this.currentTime >= this.duration) {
                    this.stop();
                    if (this.onEndedCallback) this.onEndedCallback();
                } else {
                    if (this.onProgressCallback) {
                        this.onProgressCallback(this.currentTime, this.duration);
                    }
                }
            }
        }, 1000);
    }

    pause() {
        this.isPlaying = false;
        this.stopLoop();
        if (this.timer) {
            clearInterval(this.timer);
            this.timer = null;
        }
    }

    stop() {
        this.pause();
        this.currentTime = 0;
        this.step = 0;
        if (this.onProgressCallback) {
            this.onProgressCallback(0, this.duration);
        }
    }

    seek(seconds) {
        this.currentTime = Math.max(0, Math.min(seconds, this.duration));
        if (this.onProgressCallback) {
            this.onProgressCallback(this.currentTime, this.duration);
        }
    }

    setVolume(vol) {
        this.volume = Math.max(0, Math.min(1, vol));
        if (this.masterGain && this.ctx) {
            this.masterGain.gain.setValueAtTime(this.isMuted ? 0 : this.volume, this.ctx.currentTime);
        }
    }

    toggleMute() {
        this.isMuted = !this.isMuted;
        this.setVolume(this.volume);
        return this.isMuted;
    }

    startLoop() {
        if (this.patternTimer) clearInterval(this.patternTimer);
        const bpm = (this.currentSong && this.currentSong.bpm) ? this.currentSong.bpm : 120;
        const intervalMs = (60 / bpm / 4) * 1000; // 16th notes

        this.patternTimer = setInterval(() => {
            if (!this.isPlaying || !this.ctx) return;
            this.playStep(this.step % 16);
            this.step++;
        }, intervalMs);
    }

    stopLoop() {
        if (this.patternTimer) {
            clearInterval(this.patternTimer);
            this.patternTimer = null;
        }
    }

    playStep(step) {
        if (!this.currentSong || !this.ctx) return;
        const mood = (this.currentSong.mood || 'NEUTRAL').toUpperCase();

        switch (mood) {
            case 'HAPPY':
                this.synthesizeHappy(step);
                break;
            case 'SAD':
                this.synthesizeSad(step);
                break;
            case 'ANGRY':
                this.synthesizeAngry(step);
                break;
            case 'RELAXED':
                this.synthesizeRelaxed(step);
                break;
            case 'SURPRISED':
                this.synthesizeSurprised(step);
                break;
            case 'NEUTRAL':
            default:
                this.synthesizeNeutral(step);
                break;
        }
    }

    // TONE GENERATORS
    playTone(freq, type = 'sine', duration = 0.3, gain = 0.2, detune = 0) {
        if (!this.ctx || this.isMuted) return;
        const osc = this.ctx.createOscillator();
        const gainNode = this.ctx.createGain();

        osc.type = type;
        osc.frequency.setValueAtTime(freq, this.ctx.currentTime);
        osc.detune.setValueAtTime(detune, this.ctx.currentTime);

        gainNode.gain.setValueAtTime(gain, this.ctx.currentTime);
        gainNode.gain.exponentialRampToValueAtTime(0.0001, this.ctx.currentTime + duration);

        osc.connect(gainNode);
        gainNode.connect(this.masterGain);

        osc.start();
        osc.stop(this.ctx.currentTime + duration);
    }

    playKick() {
        if (!this.ctx || this.isMuted) return;
        const osc = this.ctx.createOscillator();
        const gainNode = this.ctx.createGain();

        osc.frequency.setValueAtTime(150, this.ctx.currentTime);
        osc.frequency.exponentialRampToValueAtTime(0.01, this.ctx.currentTime + 0.35);

        gainNode.gain.setValueAtTime(0.4, this.ctx.currentTime);
        gainNode.gain.exponentialRampToValueAtTime(0.001, this.ctx.currentTime + 0.35);

        osc.connect(gainNode);
        gainNode.connect(this.masterGain);

        osc.start();
        osc.stop(this.ctx.currentTime + 0.35);
    }

    playHiHat(open = false) {
        if (!this.ctx || this.isMuted) return;
        // High frequency filtered noise/square burst
        const osc = this.ctx.createOscillator();
        const filter = this.ctx.createBiquadFilter();
        const gainNode = this.ctx.createGain();

        osc.type = 'square';
        osc.frequency.setValueAtTime(9000, this.ctx.currentTime);

        filter.type = 'highpass';
        filter.frequency.setValueAtTime(7000, this.ctx.currentTime);

        const dur = open ? 0.15 : 0.05;
        gainNode.gain.setValueAtTime(0.08, this.ctx.currentTime);
        gainNode.gain.exponentialRampToValueAtTime(0.0001, this.ctx.currentTime + dur);

        osc.connect(filter);
        filter.connect(gainNode);
        gainNode.connect(this.masterGain);

        osc.start();
        osc.stop(this.ctx.currentTime + dur);
    }

    playSnare() {
        if (!this.ctx || this.isMuted) return;
        const osc = this.ctx.createOscillator();
        const gain = this.ctx.createGain();

        osc.type = 'triangle';
        osc.frequency.setValueAtTime(220, this.ctx.currentTime);
        osc.frequency.exponentialRampToValueAtTime(40, this.ctx.currentTime + 0.15);

        gain.gain.setValueAtTime(0.2, this.ctx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.001, this.ctx.currentTime + 0.2);

        osc.connect(gain);
        gain.connect(this.masterGain);

        osc.start();
        osc.stop(this.ctx.currentTime + 0.2);
        this.playHiHat(true);
    }

    // MOOD PROCEDURAL COMPOSITIONS
    synthesizeHappy(step) {
        // 4-on-the-floor kick
        if (step % 4 === 0) this.playKick();
        if (step % 4 === 2) this.playHiHat(false);
        if (step === 4 || step === 12) this.playSnare();

        // Major scale upbeat arpeggios: C Major / A Minor (C5, E5, G5, A5, C6)
        const notes = [523.25, 659.25, 783.99, 880.00, 1046.50, 783.99, 659.25, 587.33];
        const bassNotes = [130.81, 130.81, 164.81, 174.61, 196.00, 174.61, 164.81, 146.83];

        if (step % 2 === 0) {
            const noteIdx = (step / 2) % notes.length;
            this.playTone(notes[noteIdx], 'triangle', 0.2, 0.15);
        }
        if (step % 4 === 0) {
            const bassIdx = (step / 4) % bassNotes.length;
            this.playTone(bassNotes[bassIdx], 'sine', 0.4, 0.25);
        }
    }

    synthesizeSad(step) {
        // Slow acoustic, melancholy piano chords (D Minor / F Major)
        if (step === 0) {
            // D minor chord (D3, F3, A3, D4)
            this.playTone(146.83, 'sine', 1.8, 0.2);
            this.playTone(174.61, 'sine', 1.8, 0.15);
            this.playTone(220.00, 'sine', 1.8, 0.15);
            this.playTone(293.66, 'sine', 2.0, 0.18);
        } else if (step === 8) {
            // Bb major chord (Bb2, D3, F3, Bb3)
            this.playTone(116.54, 'sine', 1.8, 0.2);
            this.playTone(146.83, 'sine', 1.8, 0.15);
            this.playTone(174.61, 'sine', 1.8, 0.15);
            this.playTone(233.08, 'sine', 2.0, 0.18);
        }
        if (step === 4 || step === 12) {
            // Gentle high piano note
            const highNotes = [440.00, 523.25, 493.88, 392.00];
            this.playTone(highNotes[(step / 4) % highNotes.length], 'sine', 0.6, 0.12);
        }
    }

    synthesizeAngry(step) {
        // High-energy aggressive rock beats
        if (step % 2 === 0) this.playKick();
        if (step === 4 || step === 12) this.playSnare();
        this.playHiHat(step % 2 !== 0);

        // Distorted sawtooth power riffs (E Minor: E2, G2, A2, B2)
        const riff = [82.41, 98.00, 110.00, 123.47, 82.41, 82.41, 110.00, 98.00];
        const riffIdx = (step / 2) % riff.length;
        this.playTone(riff[riffIdx], 'sawtooth', 0.25, 0.18);
        this.playTone(riff[riffIdx] * 2, 'square', 0.2, 0.1);
    }

    synthesizeRelaxed(step) {
        // Smooth Lo-fi 7th chords & relaxed acoustic textures
        if (step === 0) {
            // Cmaj7 (C3, E3, G3, B3)
            this.playTone(130.81, 'triangle', 2.0, 0.16);
            this.playTone(164.81, 'sine', 2.0, 0.12);
            this.playTone(196.00, 'sine', 2.0, 0.12);
            this.playTone(246.94, 'sine', 2.0, 0.12);
        } else if (step === 8) {
            // Am7 (A2, C3, E3, G3)
            this.playTone(110.00, 'triangle', 2.0, 0.16);
            this.playTone(130.81, 'sine', 2.0, 0.12);
            this.playTone(164.81, 'sine', 2.0, 0.12);
            this.playTone(196.00, 'sine', 2.0, 0.12);
        }
        if (step === 4 || step === 12) {
            this.playHiHat(false);
        }
        if (step % 4 === 2) {
            // Soft Rhodes tone
            const mellow = [329.63, 392.00, 440.00, 493.88];
            this.playTone(mellow[(step / 2) % mellow.length], 'sine', 0.5, 0.1);
        }
    }

    synthesizeNeutral(step) {
        // Modern clean synthpop groove
        if (step % 4 === 0) this.playKick();
        if (step === 4 || step === 12) this.playSnare();
        if (step % 2 === 1) this.playHiHat(false);

        const melody = [261.63, 293.66, 329.63, 392.00, 440.00, 392.00, 329.63, 293.66];
        if (step % 2 === 0) {
            this.playTone(melody[(step / 2) % melody.length], 'triangle', 0.25, 0.14);
        }
    }

    synthesizeSurprised(step) {
        // High-BPM energetic future bass & unexpected synth arpeggios
        if (step % 2 === 0) this.playKick();
        if (step % 4 === 2) this.playSnare();
        this.playHiHat(true);

        const arp = [659.25, 880.00, 987.77, 1318.51, 1174.66, 987.77, 880.00, 783.99];
        this.playTone(arp[step % arp.length], 'sawtooth', 0.15, 0.15, 5);
        this.playTone(arp[step % arp.length] * 0.5, 'square', 0.15, 0.1, -5);
    }

    getFrequencyData() {
        if (!this.analyser) return new Uint8Array(16);
        const data = new Uint8Array(this.analyser.frequencyBinCount);
        this.analyser.getByteFrequencyData(data);
        return data;
    }
}

window.musixAudio = new MusixAudioEngine();
