package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class SoundEngine {
    private val sampleRate = 22050
    private var isRunning = false
    private var audioThread: Thread? = null
    private var audioTrack: AudioTrack? = null

    // Real-time audio parameters controlled by game physics
    @Volatile var engineRpm: Float = 900f
    @Volatile var throttle: Float = 0f
    @Volatile var speedKmh: Float = 0f
    @Volatile var tireSlip: Float = 0f
    @Volatile var isBrakingHard: Boolean = false
    @Volatile var isNearWater: Boolean = false
    @Volatile var waterProximity: Float = 0f // 0 to 1
    @Volatile var isWalking: Boolean = false
    @Volatile var isHornActive: Boolean = false
    @Volatile var isBlinkerActive: Boolean = false
    @Volatile var enginePitchMultiplier: Float = 1.0f

    // Master / Channel Volume controls
    @Volatile var masterVolume: Float = 1.0f
    @Volatile var engineVolume: Float = 0.85f
    @Volatile var ambienceVolume: Float = 0.6f
    @Volatile var effectsVolume: Float = 0.8f

    // One-shot triggers
    @Volatile var triggerDoorOpen: Boolean = false
    @Volatile var triggerDoorClose: Boolean = false
    @Volatile var triggerCrash: Boolean = false
    @Volatile var triggerShift: Boolean = false

    private var doorOpenFramesRemaining = 0
    private var doorCloseFramesRemaining = 0
    private var crashFramesRemaining = 0
    private var shiftFramesRemaining = 0
    private var blinkerCounter = 0

    fun start() {
        if (isRunning) return
        isRunning = true

        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
        } catch (e: Exception) {
            Log.e("SoundEngine", "Failed to init AudioTrack: ${e.message}")
            return
        }

        audioThread = Thread({
            val chunkSize = 1024
            val buffer = ShortArray(chunkSize)

            var phaseEngine1 = 0.0
            var phaseEngine2 = 0.0
            var phaseEngine3 = 0.0
            var phaseHorn = 0.0
            var walkPhase = 0.0
            var birdTimer = 0
            var birdChirpFrames = 0
            var birdPhase = 0.0

            var windFilter = 0.0
            var tireFilter = 0.0
            var waterFilter = 0.0

            while (isRunning) {
                if (triggerDoorOpen) {
                    doorOpenFramesRemaining = (sampleRate * 0.35f).toInt()
                    triggerDoorOpen = false
                }
                if (triggerDoorClose) {
                    doorCloseFramesRemaining = (sampleRate * 0.30f).toInt()
                    triggerDoorClose = false
                }
                if (triggerCrash) {
                    crashFramesRemaining = (sampleRate * 0.40f).toInt()
                    triggerCrash = false
                }
                if (triggerShift) {
                    shiftFramesRemaining = (sampleRate * 0.12f).toInt()
                    triggerShift = false
                }

                val currentRpm = engineRpm.coerceIn(700f, 8500f)
                val fundamentalFreq = ((currentRpm / 60.0) * 3.0 * enginePitchMultiplier).coerceIn(35.0, 500.0)
                val subFreq = fundamentalFreq * 0.5
                val harmonicFreq = fundamentalFreq * 2.0

                val currentThrottle = throttle.coerceIn(0f, 1f)
                val currentSpeed = speedKmh.coerceIn(0f, 350f)
                val currentSlip = tireSlip.coerceIn(0f, 1f)
                val hardBrake = isBrakingHard

                val master = masterVolume.coerceIn(0f, 1f)
                val engVol = engineVolume * master * (if (isWalking) 0.15f else 1.0f)
                val ambVol = ambienceVolume * master
                val fxVol = effectsVolume * master

                for (i in 0 until chunkSize) {
                    var sample = 0.0

                    // 1. ENGINE SYNTHESIS (multi-harmonic pulse with intake roar on throttle)
                    val step1 = (2.0 * PI * fundamentalFreq) / sampleRate
                    val step2 = (2.0 * PI * subFreq) / sampleRate
                    val step3 = (2.0 * PI * harmonicFreq) / sampleRate
                    phaseEngine1 = (phaseEngine1 + step1) % (2.0 * PI)
                    phaseEngine2 = (phaseEngine2 + step2) % (2.0 * PI)
                    phaseEngine3 = (phaseEngine3 + step3) % (2.0 * PI)

                    val engineWave = (sin(phaseEngine1) * 0.55 +
                            sin(phaseEngine2) * 0.35 +
                            sin(phaseEngine3) * (0.2 + currentThrottle * 0.4))
                    val engineRumble = ((Random.nextFloat() - 0.5f) * (0.08f + currentThrottle * 0.15f))
                    val engineMix = (engineWave + engineRumble) * engVol * (0.35 + currentThrottle * 0.5)
                    sample += engineMix

                    // 2. TIRE SCREECH / SKID (bandpass noise on drift/slip)
                    if (currentSlip > 0.15f || (hardBrake && currentSpeed > 10f)) {
                        val slipFactor = if (hardBrake) 0.8f else currentSlip
                        val whiteNoise = (Random.nextFloat() * 2f - 1f)
                        tireFilter += (whiteNoise - tireFilter) * 0.25
                        sample += tireFilter * slipFactor * fxVol * 0.55
                    }

                    // 3. WIND RUSH (low pass noise scaled by vehicle speed)
                    if (currentSpeed > 5f) {
                        val windAmount = (currentSpeed / 200f).coerceIn(0f, 1f)
                        val whiteNoise = (Random.nextFloat() * 2f - 1f)
                        windFilter += (whiteNoise - windFilter) * 0.06
                        sample += windFilter * windAmount * ambVol * 0.45
                    }

                    // 4. MOUNTAIN AMBIENCE & WATERFALL / RIVER NOISE
                    if (isNearWater && waterProximity > 0.05f) {
                        val waterNoise = (Random.nextFloat() * 2f - 1f)
                        waterFilter += (waterNoise - waterFilter) * 0.12
                        sample += waterFilter * waterProximity * ambVol * 0.5
                    }

                    // Bird chirps in the alpine forest
                    birdTimer++
                    if (birdTimer > sampleRate * 4) {
                        birdTimer = 0
                        birdChirpFrames = (sampleRate * 0.2f).toInt()
                    }
                    if (birdChirpFrames > 0) {
                        birdChirpFrames--
                        val chirpFreq = 2400.0 + sin(birdChirpFrames * 0.05) * 600.0
                        birdPhase = (birdPhase + (2.0 * PI * chirpFreq / sampleRate)) % (2.0 * PI)
                        sample += sin(birdPhase) * ambVol * 0.15
                    }

                    // 5. FOOTSTEPS ON FOOT
                    if (isWalking) {
                        walkPhase += 0.008
                        if (sin(walkPhase) > 0.98) {
                            sample += (Random.nextFloat() * 2f - 1f) * fxVol * 0.4
                        }
                    }

                    // 6. DOOR OPEN / CLOSE
                    if (doorOpenFramesRemaining > 0) {
                        doorOpenFramesRemaining--
                        val t = doorOpenFramesRemaining.toDouble() / (sampleRate * 0.35)
                        sample += sin(t * 180.0) * (1.0 - t) * fxVol * 0.6
                    }
                    if (doorCloseFramesRemaining > 0) {
                        doorCloseFramesRemaining--
                        val t = doorCloseFramesRemaining.toDouble() / (sampleRate * 0.30)
                        sample += (sin(t * 80.0) + (Random.nextFloat() - 0.5) * 0.8) * t * fxVol * 0.8
                    }

                    // 7. BLINKER TICK
                    if (isBlinkerActive) {
                        blinkerCounter = (blinkerCounter + 1) % (sampleRate / 2)
                        if (blinkerCounter < 150) {
                            sample += sin(blinkerCounter * 0.8) * fxVol * 0.35
                        }
                    }

                    // 8. HORN
                    if (isHornActive) {
                        val stepHorn1 = (2.0 * PI * 440.0) / sampleRate
                        val stepHorn2 = (2.0 * PI * 554.0) / sampleRate
                        phaseHorn = (phaseHorn + stepHorn1) % (2.0 * PI)
                        sample += (sin(phaseHorn) * 0.5 + sin(phaseHorn * (554.0 / 440.0)) * 0.5) * fxVol * 0.85
                    }

                    // 9. CRASH IMPACT & BARRIER THUD
                    if (crashFramesRemaining > 0) {
                        crashFramesRemaining--
                        val t = crashFramesRemaining.toDouble() / (sampleRate * 0.40)
                        val crashNoise = (Random.nextFloat() * 2f - 1f) * t * t
                        val lowBoom = sin(crashFramesRemaining * 0.08) * t
                        sample += (crashNoise * 0.8 + lowBoom * 0.6) * fxVol * 1.1
                    }

                    // 10. GEAR SHIFT CLACK
                    if (shiftFramesRemaining > 0) {
                        shiftFramesRemaining--
                        val t = shiftFramesRemaining.toDouble() / (sampleRate * 0.12)
                        sample += sin(shiftFramesRemaining * 0.45) * t * fxVol * 0.45
                    }

                    // Clamp to 16-bit PCM range
                    val clamped = (sample * 32767.0).coerceIn(-32767.0, 32767.0).toInt().toShort()
                    buffer[i] = clamped
                }

                audioTrack?.write(buffer, 0, chunkSize)
            }
        }, "ProceduralAudioThread")

        audioThread?.priority = Thread.MAX_PRIORITY
        audioThread?.start()
    }

    fun playDoorOpenSound() {
        triggerDoorOpen = true
    }

    fun playDoorCloseSound() {
        triggerDoorClose = true
    }

    fun playCrashSound() {
        triggerCrash = true
    }

    fun playShiftSound() {
        triggerShift = true
    }

    fun stop() {
        isRunning = false
        try {
            audioThread?.join(500)
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            Log.e("SoundEngine", "Error stopping audio: ${e.message}")
        }
        audioTrack = null
        audioThread = null
    }
}
