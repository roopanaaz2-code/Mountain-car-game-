package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import kotlin.math.PI
import kotlin.math.sin

data class MusicTrack(
    val title: String,
    val artist: String,
    val isLocalFile: Boolean,
    val uri: Uri? = null,
    val stationIndex: Int = 0
)

class RadioPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var synthTrack: AudioTrack? = null
    private var synthThread: Thread? = null
    private var isSynthRunning = false

    @Volatile var isPlaying: Boolean = true
    @Volatile var currentStationIndex: Int = 0
    @Volatile var volume: Float = 0.7f

    var currentTrackTitle: String = "Alpine Chill FM"
        private set

    var currentTrackArtist: String = "Mountain Drive Radio"
        private set

    private val localPlayList = mutableListOf<MusicTrack>()

    init {
        startProceduralRadio()
    }

    val isLocalMode: Boolean
        get() = currentStationIndex >= 4

    val currentLocalIndex: Int
        get() = if (isLocalMode) currentStationIndex - 4 else -1

    fun selectStation(idx: Int) {
        currentStationIndex = idx.coerceIn(0, 3)
        stopMediaPlayer()
        updateStationInfo(currentStationIndex)
    }

    fun selectLocalTrack(idx: Int) {
        if (idx in localPlayList.indices) {
            currentStationIndex = 4 + idx
            playLocalTrack(localPlayList[idx])
        }
    }

    fun getStationCount(): Int = 4

    fun nextStation() {
        if (localPlayList.isNotEmpty() && currentStationIndex >= 4) {
            val localIdx = currentStationIndex - 4
            val nextLocal = (localIdx + 1) % localPlayList.size
            currentStationIndex = 4 + nextLocal
            playLocalTrack(localPlayList[nextLocal])
        } else {
            currentStationIndex = (currentStationIndex + 1) % (4 + localPlayList.size)
            if (currentStationIndex < 4) {
                stopMediaPlayer()
                updateStationInfo(currentStationIndex)
            } else {
                val localIdx = currentStationIndex - 4
                playLocalTrack(localPlayList[localIdx])
            }
        }
    }

    fun prevStation() {
        val total = 4 + localPlayList.size
        currentStationIndex = (currentStationIndex - 1 + total) % total
        if (currentStationIndex < 4) {
            stopMediaPlayer()
            updateStationInfo(currentStationIndex)
        } else {
            val localIdx = currentStationIndex - 4
            playLocalTrack(localPlayList[localIdx])
        }
    }

    fun togglePlayPause() {
        isPlaying = !isPlaying
        if (mediaPlayer != null) {
            if (isPlaying) mediaPlayer?.start() else mediaPlayer?.pause()
        }
    }

    fun addLocalTrack(uri: Uri, displayName: String) {
        val track = MusicTrack(
            title = displayName.substringBeforeLast('.'),
            artist = "Local Storage",
            isLocalFile = true,
            uri = uri
        )
        localPlayList.add(track)
        currentStationIndex = 4 + (localPlayList.size - 1)
        playLocalTrack(track)
    }

    fun getLocalTracks(): List<MusicTrack> = localPlayList.toList()

    private fun updateStationInfo(idx: Int) {
        when (idx) {
            0 -> {
                currentTrackTitle = "Alpine Chill FM"
                currentTrackArtist = "Peaceful Acoustic Chords"
            }
            1 -> {
                currentTrackTitle = "Mountain Synthwave"
                currentTrackArtist = "Retro Sunset Beats"
            }
            2 -> {
                currentTrackTitle = "High Pass Ambient"
                currentTrackArtist = "Tranquil Valleys"
            }
            3 -> {
                currentTrackTitle = "Canyon Lo-Fi"
                currentTrackArtist = "Gentle Mountain Breeze"
            }
        }
    }

    private fun playLocalTrack(track: MusicTrack) {
        currentTrackTitle = track.title
        currentTrackArtist = track.artist
        stopMediaPlayer()

        try {
            track.uri?.let { uri ->
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .build()
                    )
                    setDataSource(context, uri)
                    setVolume(volume, volume)
                    isLooping = true
                    prepare()
                    if (isPlaying) start()
                }
            }
        } catch (e: Exception) {
            Log.e("RadioPlayer", "Error playing local track: ${e.message}")
        }
    }

    private fun stopMediaPlayer() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e("RadioPlayer", "Error releasing player: ${e.message}")
        }
        mediaPlayer = null
    }

    private fun startProceduralRadio() {
        updateStationInfo(0)
        isSynthRunning = true
        val sampleRate = 22050
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        try {
            synthTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
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
                .setBufferSizeInBytes((minBufferSize * 2).coerceAtLeast(4096))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            synthTrack?.play()
        } catch (e: Exception) {
            Log.e("RadioPlayer", "Failed to init radio AudioTrack: ${e.message}")
            return
        }

        synthThread = Thread({
            val bufferSize = 1024
            val buffer = ShortArray(bufferSize)

            // Musical chord progressions for the alpine road trip
            val chordProgressions = listOf(
                // Alpine Chill (Cmaj7 - Am7 - Fmaj7 - G)
                doubleArrayOf(261.63, 329.63, 392.00, 493.88, 220.00, 261.63, 329.63, 392.00, 174.61, 220.00, 261.63, 329.63, 196.00, 246.94, 293.66, 392.00),
                // Synthwave (D min - Bb - F - C)
                doubleArrayOf(146.83, 220.00, 293.66, 349.23, 116.54, 174.61, 233.08, 293.66, 174.61, 261.63, 349.23, 440.00, 130.81, 196.00, 261.63, 329.63),
                // Ambient (E min - G - D - A)
                doubleArrayOf(164.81, 246.94, 329.63, 392.00, 196.00, 293.66, 392.00, 493.88, 146.83, 220.00, 293.66, 369.99, 110.00, 164.81, 220.00, 277.18),
                // Lo-Fi (Fmaj7 - Em7 - Dm7 - Cmaj7)
                doubleArrayOf(174.61, 261.63, 329.63, 440.00, 164.81, 246.94, 329.63, 392.00, 146.83, 220.00, 261.63, 349.23, 130.81, 196.00, 246.94, 329.63)
            )

            var sampleCount = 0
            var noteIndex = 0
            var phaseLead = 0.0
            var phaseBass = 0.0
            var phasePad = 0.0

            while (isSynthRunning) {
                val isRadioActive = isPlaying && mediaPlayer == null && currentStationIndex < 4
                val vol = if (isRadioActive) volume * 0.45f else 0.0f

                val progression = chordProgressions[currentStationIndex.coerceIn(0, 3)]
                val noteDurationSamples = sampleRate / 2 // 0.5s per arpeggio note

                for (i in 0 until bufferSize) {
                    if (vol <= 0.001f) {
                        buffer[i] = 0
                        continue
                    }

                    sampleCount++
                    if (sampleCount >= noteDurationSamples) {
                        sampleCount = 0
                        noteIndex = (noteIndex + 1) % progression.size
                    }

                    val currentFreq = progression[noteIndex]
                    val bassFreq = progression[(noteIndex / 4) * 4] * 0.5
                    val padFreq = progression[(noteIndex / 4) * 4 + 2]

                    phaseLead = (phaseLead + (2.0 * PI * currentFreq / sampleRate)) % (2.0 * PI)
                    phaseBass = (phaseBass + (2.0 * PI * bassFreq / sampleRate)) % (2.0 * PI)
                    phasePad = (phasePad + (2.0 * PI * padFreq / sampleRate)) % (2.0 * PI)

                    val envelope = 1.0 - (sampleCount.toDouble() / noteDurationSamples)
                    val lead = sin(phaseLead) * envelope * 0.5
                    val bass = (sin(phaseBass) + sin(phaseBass * 2.0) * 0.3) * 0.35
                    val pad = sin(phasePad) * 0.25

                    val mix = (lead + bass + pad) * vol
                    buffer[i] = (mix * 32767.0).coerceIn(-32767.0, 32767.0).toInt().toShort()
                }

                synthTrack?.write(buffer, 0, bufferSize)
            }
        }, "ProceduralRadioThread")

        synthThread?.priority = Thread.NORM_PRIORITY
        synthThread?.start()
    }

    fun setMasterVolume(vol: Float) {
        volume = vol.coerceIn(0f, 1f)
        mediaPlayer?.setVolume(volume, volume)
    }

    fun stop() {
        isSynthRunning = false
        stopMediaPlayer()
        try {
            synthThread?.join(400)
            synthTrack?.stop()
            synthTrack?.release()
        } catch (e: Exception) {
            Log.e("RadioPlayer", "Error stopping synth track: ${e.message}")
        }
        synthTrack = null
        synthThread = null
    }
}
