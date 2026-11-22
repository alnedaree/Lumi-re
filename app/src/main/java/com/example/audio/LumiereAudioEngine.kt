package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.util.Log
import com.example.model.AmbientPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.Collections
import java.util.Random
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

private const val TAG = "LumiereAudioEngine"

data class CachedAudioClip(
    val bytes: ByteArray,
    val extension: String
)

/**
 * In-memory LRU audio cache keyed by beatId + locked character voice signature
 * so replaying a scene never wastes ElevenLabs or Gemini API credits.
 */
class AudioCacheManager(private val maxEntries: Int = 48) {
    private val map = Collections.synchronizedMap(
        object : LinkedHashMap<String, CachedAudioClip>(maxEntries, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedAudioClip>?): Boolean {
                return size > maxEntries
            }
        }
    )

    private val _cachedKeys = MutableStateFlow<Set<String>>(emptySet())
    val cachedKeys: StateFlow<Set<String>> = _cachedKeys.asStateFlow()

    fun get(key: String): CachedAudioClip? = map[key]

    fun put(key: String, clip: CachedAudioClip) {
        map[key] = clip
        _cachedKeys.value = map.keys.toSet()
    }

    fun clear() {
        map.clear()
        _cachedKeys.value = emptySet()
    }
}

/**
 * 3-Channel Cinema Audio Engine:
 * 1. Voice Playback Channel (ElevenLabs v4 MP3 / Gemini 3.8 TTS WAV) with animated waveform metering
 * 2. Background Music Channel (Real-time procedural DSP Ambient Synthesizer OR custom uploaded .mp3/.wav)
 * 3. Sound Effects Channel (ElevenLabs eleven_text_to_sound_v3 SFX playback)
 *
 * STRICT BAN: Does NOT use android.speech.tts.TextToSpeech.
 */
class LumiereAudioEngine(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val cache = AudioCacheManager()

    private var voicePlayer: MediaPlayer? = null
    private var customMusicPlayer: MediaPlayer? = null
    private var sfxPlayer: MediaPlayer? = null
    private var waveformJob: Job? = null
    private var ambientSynthJob: Job? = null

    @Volatile private var voiceVolume: Float = 0.95f
    @Volatile private var musicVolume: Float = 0.35f
    @Volatile private var sfxVolume: Float = 0.72f
    @Volatile private var activeAmbientPreset: AmbientPreset = AmbientPreset.NONE
    @Volatile private var activeCustomMusicPath: String? = null
    @Volatile private var isAmbientRunning: Boolean = false

    private val _isVoicePlaying = MutableStateFlow(false)
    val isVoicePlaying: StateFlow<Boolean> = _isVoicePlaying.asStateFlow()

    private val _waveformLevels = MutableStateFlow(List(16) { 0.12f })
    val waveformLevels: StateFlow<List<Float>> = _waveformLevels.asStateFlow()

    fun setVolumes(voice: Float, music: Float, sfx: Float) {
        voiceVolume = voice.coerceIn(0f, 1f)
        musicVolume = music.coerceIn(0f, 1f)
        sfxVolume = sfx.coerceIn(0f, 1f)
        runCatching { voicePlayer?.setVolume(voiceVolume, voiceVolume) }
        runCatching { customMusicPlayer?.setVolume(musicVolume, musicVolume) }
        runCatching { sfxPlayer?.setVolume(sfxVolume, sfxVolume) }
    }

    fun playVoiceClip(
        clip: CachedAudioClip,
        onCompleted: () -> Unit,
        onError: (String) -> Unit
    ) {
        stopVoice()
        try {
            val tempFile = File(context.cacheDir, "lumiere_voice_${System.nanoTime()}.${clip.extension}")
            tempFile.writeBytes(clip.bytes)

            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(tempFile.absolutePath)
                setVolume(voiceVolume, voiceVolume)
                setOnCompletionListener {
                    stopWaveformAnimation()
                    _isVoicePlaying.value = false
                    runCatching { tempFile.delete() }
                    onCompleted()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "Voice MediaPlayer error: what=$what extra=$extra")
                    stopWaveformAnimation()
                    _isVoicePlaying.value = false
                    runCatching { tempFile.delete() }
                    onError("Unable to decode voice audio stream (code $what).")
                    true
                }
                prepare()
                start()
            }
            voicePlayer = player
            _isVoicePlaying.value = true
            startWaveformAnimation(clip.bytes)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play voice clip", e)
            stopWaveformAnimation()
            _isVoicePlaying.value = false
            onError("Voice playback failed: ${e.localizedMessage ?: "Unsupported audio format"}")
        }
    }

    fun stopVoice() {
        stopWaveformAnimation()
        _isVoicePlaying.value = false
        voicePlayer?.let { p ->
            runCatching {
                if (p.isPlaying) p.stop()
                p.release()
            }
        }
        voicePlayer = null
    }

    fun playSfxFile(filePath: String, onError: (String) -> Unit = {}) {
        stopSfx()
        val file = File(filePath)
        if (!file.exists()) {
            onError("SFX audio file not found.")
            return
        }
        try {
            sfxPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(file.absolutePath)
                setVolume(sfxVolume, sfxVolume)
                setOnCompletionListener {
                    runCatching { release() }
                    if (sfxPlayer === this) sfxPlayer = null
                }
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play SFX", e)
            onError("SFX playback error: ${e.localizedMessage}")
        }
    }

    fun stopSfx() {
        sfxPlayer?.let { p ->
            runCatching {
                if (p.isPlaying) p.stop()
                p.release()
            }
        }
        sfxPlayer = null
    }

    /**
     * Starts or transitions the ambient soundtrack channel.
     * Plays custom uploaded .mp3/.wav if assigned to the scene; otherwise synthesizes
     * the selected procedural soundscape in real time via AudioTrack.
     */
    fun startAmbient(preset: AmbientPreset, customMusicPath: String?) {
        val validCustom = customMusicPath?.takeIf { File(it).exists() }
        if (isAmbientRunning && activeAmbientPreset == preset && activeCustomMusicPath == validCustom) {
            return
        }
        stopAmbient()

        activeAmbientPreset = preset
        activeCustomMusicPath = validCustom

        if (validCustom != null) {
            startCustomMusicLoop(validCustom)
            return
        }

        if (preset == AmbientPreset.NONE) {
            return
        }

        isAmbientRunning = true
        ambientSynthJob = scope.launch(Dispatchers.Default) {
            runProceduralSoundscape(preset)
        }
    }

    fun stopAmbient() {
        isAmbientRunning = false
        ambientSynthJob?.cancel()
        ambientSynthJob = null
        customMusicPlayer?.let { p ->
            runCatching {
                if (p.isPlaying) p.stop()
                p.release()
            }
        }
        customMusicPlayer = null
    }

    private fun startCustomMusicLoop(path: String) {
        try {
            customMusicPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(path)
                isLooping = true
                setVolume(musicVolume, musicVolume)
                prepare()
                start()
            }
            isAmbientRunning = true
        } catch (e: Exception) {
            Log.e(TAG, "Custom music playback failed", e)
            isAmbientRunning = false
        }
    }

    private fun startWaveformAnimation(audioBytes: ByteArray) {
        waveformJob?.cancel()
        // Pre-compute pseudo-spectral energy envelope from the real audio bytes
        val chunkCount = 128
        val step = (audioBytes.size / chunkCount).coerceAtLeast(1)
        val envelope = FloatArray(chunkCount) { idx ->
            val start = (idx * step).coerceAtMost(audioBytes.lastIndex)
            val sample = (audioBytes[start].toInt() and 0xFF) / 255f
            (0.25f + sample * 0.75f).coerceIn(0.2f, 1f)
        }
        waveformJob = scope.launch {
            var tick = 0
            while (isActive && _isVoicePlaying.value) {
                val baseEnergy = envelope[tick % envelope.size]
                val bars = List(16) { barIndex ->
                    val phase = (tick * 0.42f) + (barIndex * 0.58f)
                    val wave = (sin(phase.toDouble()).toFloat() + 1f) * 0.5f
                    val centerWeight = 1f - (kotlin.math.abs(barIndex - 7.5f) / 10f)
                    (0.14f + baseEnergy * wave * centerWeight * 0.86f).coerceIn(0.12f, 1.0f)
                }
                _waveformLevels.value = bars
                tick++
                delay(70L)
            }
            _waveformLevels.value = List(16) { 0.12f }
        }
    }

    private fun stopWaveformAnimation() {
        waveformJob?.cancel()
        waveformJob = null
        _waveformLevels.value = List(16) { 0.12f }
    }

    /**
     * Real-time procedural DSP Ambient Synthesizer running on Android AudioTrack.
     */
    private fun runProceduralSoundscape(preset: AmbientPreset) {
        val sampleRate = 22050
        val minBufBytes = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        val audioTrack = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBufBytes * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AudioTrack for procedural ambient synth", e)
            return
        }

        val buffer = ShortArray(1024)
        val rng = Random(42)

        // Filter state variables
        var b0 = 0.0; var b1 = 0.0; var b2 = 0.0
        var lpState = 0.0
        var lpState2 = 0.0
        var sampleIndex = 0L
        var dropEnv = 0.0
        var dropFreq = 1200.0
        var dropPhase = 0.0

        // Nostalgic piano chord progression: Am9 -> Fmaj7 -> Dm9 -> E7sus4 (Hz)
        val pianoChords = arrayOf(
            doubleArrayOf(220.00, 261.63, 329.63, 392.00, 493.88),
            doubleArrayOf(174.61, 220.00, 261.63, 329.63, 392.00),
            doubleArrayOf(146.83, 220.00, 261.63, 329.63, 349.23),
            doubleArrayOf(164.81, 220.00, 246.94, 293.66, 329.63)
        )

        try {
            audioTrack.play()
            while (isAmbientRunning && !Thread.currentThread().isInterrupted) {
                val vol = musicVolume
                for (i in buffer.indices) {
                    val t = sampleIndex.toDouble() / sampleRate
                    val fadeIn = (t / 1.5).coerceIn(0.0, 1.0)

                    val rawSample: Double = when (preset) {
                        AmbientPreset.RAIN -> {
                            val white = rng.nextDouble() * 2.0 - 1.0
                            // Soft pink/brown noise cascade for lush steady rain
                            b0 = 0.997 * b0 + white * 0.029
                            b1 = 0.985 * b1 + white * 0.032
                            b2 = 0.950 * b2 + white * 0.048
                            val pink = (b0 + b1 + b2 + white * 0.018)
                            lpState += 0.14 * (pink - lpState)

                            // Occasional subtle raindrop ping on carriage glass
                            if (dropEnv < 0.002 && rng.nextInt(3800) == 0) {
                                dropEnv = 0.16
                                dropFreq = 950.0 + rng.nextDouble() * 900.0
                            }
                            dropPhase += 2.0 * PI * dropFreq / sampleRate
                            val drop = sin(dropPhase) * dropEnv
                            dropEnv *= 0.9965

                            (lpState * 0.55 + drop * 0.35)
                        }

                        AmbientPreset.NOIR_SUSPENSE -> {
                            // Deep obsidian cello drone + dark tritone tension LFO
                            val lfo = 0.5 + 0.5 * sin(2.0 * PI * 0.12 * t)
                            val lfo2 = 0.5 + 0.5 * sin(2.0 * PI * 0.07 * t + 1.4)
                            val sub = sin(2.0 * PI * 65.41 * t) * 0.42
                            val fifth = sin(2.0 * PI * 98.00 * t + 0.2 * sin(2.0 * PI * 0.25 * t)) * 0.24
                            val minorThird = sin(2.0 * PI * 155.56 * t) * (0.08 + 0.09 * lfo)
                            val shimmer = sin(2.0 * PI * 293.66 * t) * (0.04 * lfo2)
                            val air = (rng.nextDouble() * 2.0 - 1.0) * 0.03
                            lpState += (0.05 + 0.08 * lfo) * ((sub + fifth + minorThird + shimmer + air) - lpState)
                            lpState * 0.9
                        }

                        AmbientPreset.NOSTALGIC_PIANO -> {
                            // Warm felt-piano arpeggiated chord progression
                            val chordDuration = 4.2
                            val chordIdx = ((t / chordDuration).toInt()) % pianoChords.size
                            val localT = t % chordDuration
                            val chord = pianoChords[chordIdx]
                            var sum = 0.0
                            for (n in chord.indices) {
                                val noteStart = n * 0.42
                                val dt = localT - noteStart
                                if (dt > 0.0) {
                                    val attack = (dt / 0.035).coerceIn(0.0, 1.0)
                                    val decay = exp(-dt * 1.15)
                                    val f = chord[n]
                                    // Warm fundamental + gentle 2nd & 3rd harmonics
                                    val tone = sin(2.0 * PI * f * dt) * 0.72 +
                                        sin(2.0 * PI * f * 2.0 * dt) * 0.20 * exp(-dt * 2.2) +
                                        sin(2.0 * PI * f * 3.0 * dt) * 0.08 * exp(-dt * 3.5)
                                    sum += tone * attack * decay
                                }
                            }
                            // Subtle vinyl room warmth
                            val warmth = (rng.nextDouble() * 2.0 - 1.0) * 0.008
                            (sum * 0.24 + warmth)
                        }

                        AmbientPreset.NIGHT_WIND -> {
                            val white = rng.nextDouble() * 2.0 - 1.0
                            val gustLfo = 0.5 + 0.5 * sin(2.0 * PI * 0.11 * t)
                            val swellLfo = 0.5 + 0.5 * sin(2.0 * PI * 0.045 * t + 0.8)
                            val cutoff = 0.03 + 0.11 * gustLfo * swellLfo
                            lpState += cutoff * (white - lpState)
                            lpState2 += (cutoff * 0.85) * (lpState - lpState2)
                            val whistle = sin(2.0 * PI * (240.0 + 90.0 * gustLfo) * t) * 0.045 * swellLfo
                            (lpState2 * (1.2 + 0.8 * gustLfo) + whistle)
                        }

                        AmbientPreset.NONE -> 0.0
                    }

                    val scaled = (rawSample * fadeIn * vol * 0.55).coerceIn(-0.98, 0.98)
                    buffer[i] = (scaled * Short.MAX_VALUE).toInt().toShort()
                    sampleIndex++
                }
                audioTrack.write(buffer, 0, buffer.size)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Procedural soundscape loop interrupted", e)
        } finally {
            runCatching {
                audioTrack.pause()
                audioTrack.flush()
                audioTrack.release()
            }
        }
    }

    fun release() {
        stopVoice()
        stopAmbient()
        stopSfx()
        scope.cancel()
    }
}
