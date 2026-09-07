package com.example.ui

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.RadioPlayer
import com.example.audio.SoundEngine
import com.example.data.GameRepository
import com.example.model.*
import com.example.renderer.MountainGLRenderer
import com.example.renderer.MountainMap
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppScreen {
    MAIN_MENU,
    DRIVING,
    GARAGE,
    CHARACTER_SELECT,
    SETTINGS,
    MY_MUSIC
}

class GameViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {
    val repository = GameRepository(application)
    val soundEngine = SoundEngine()
    val radioPlayer = RadioPlayer(application)
    val map = MountainMap()
    val renderer = MountainGLRenderer(application, map, soundEngine)

    private val _currentScreen = MutableStateFlow(AppScreen.MAIN_MENU)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Real-time telemetry
    val speedKmh = MutableStateFlow(0f)
    val speedDisplay = MutableStateFlow("0")
    val speedUnitStr = MutableStateFlow("km/h")
    val engineRpm = MutableStateFlow(850f)
    val currentGearName = MutableStateFlow("P")
    val highwayDistanceKm = MutableStateFlow(0f)
    val currentZoneName = MutableStateFlow("Lush Pine Valley")
    val totalDistanceDriven = MutableStateFlow(repository.totalDistanceDrivenKm)
    val fpsCount = MutableStateFlow(60)

    val isInsideVehicle = MutableStateFlow(true)
    val canEnterVehicle = MutableStateFlow(false)
    val activeCameraMode = MutableStateFlow(CameraMode.THIRD_PERSON)

    val isHeadlightsOn = MutableStateFlow(true)
    val isHighBeam = MutableStateFlow(false)
    val isBlinkerLeft = MutableStateFlow(false)
    val isBlinkerRight = MutableStateFlow(false)
    val isHazard = MutableStateFlow(false)
    val isWiperOn = MutableStateFlow(false)
    val isNitroActive = MutableStateFlow(false)

    val radioTitle = MutableStateFlow(radioPlayer.currentTrackTitle)
    val radioArtist = MutableStateFlow(radioPlayer.currentTrackArtist)
    val isRadioPlaying = MutableStateFlow(radioPlayer.isPlaying)

    // Selection & Customization
    val selectedVehicle = MutableStateFlow(GameContent.vehicles[0])
    val selectedCustomization = MutableStateFlow(CarCustomization())
    val selectedCharacter = MutableStateFlow(GameContent.characters[0])
    val graphicsQuality = MutableStateFlow(repository.graphicsQuality)
    val controlType = MutableStateFlow(repository.controlType)
    val transmissionType = MutableStateFlow(repository.transmissionType)
    val controlSensitivity = MutableStateFlow(repository.controlSensitivity)
    val speedUnit = MutableStateFlow(repository.speedUnit)

    val timeOfDay = MutableStateFlow(TimeOfDay.DYNAMIC)
    val weather = MutableStateFlow(WeatherType.CLEAR)

    // Sensor Manager for Tilt / Gyroscope steering
    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    init {
        // Load persisted settings
        val initialCar = GameContent.vehicles.find { it.id == repository.selectedCarId } ?: GameContent.vehicles[0]
        selectedVehicle.value = initialCar
        val initialCustom = repository.getCarCustomization(initialCar.id)
        selectedCustomization.value = initialCustom

        val initialChar = GameContent.characters.find { it.id == repository.selectedCharacterId } ?: GameContent.characters[0]
        selectedCharacter.value = initialChar

        // Apply settings to renderer and sound engine
        soundEngine.masterVolume = repository.masterVolume
        soundEngine.engineVolume = repository.engineVolume
        soundEngine.ambienceVolume = repository.ambienceVolume
        soundEngine.start()

        radioPlayer.setMasterVolume(repository.musicVolume)

        renderer.activeVehicleSpec = initialCar
        renderer.activeCustomization = initialCustom
        renderer.activeCharacter = initialChar
        renderer.graphicsQuality = repository.graphicsQuality
        renderer.controlType = repository.controlType
        renderer.transmissionType = repository.transmissionType
        renderer.controlSensitivity = repository.controlSensitivity

        startTelemetryPolling()
    }

    private fun startTelemetryPolling() {
        viewModelScope.launch {
            var lastDist = renderer.vehicle.distanceS
            while (true) {
                delay(33) // ~30 fps UI refresh
                val v = renderer.vehicle
                val w = renderer.walker

                val kmh = v.speedKmh
                speedKmh.value = kmh
                val unit = speedUnit.value
                val convertedSpeed = kmh * unit.conversionFactor
                speedDisplay.value = String.format("%.0f", convertedSpeed)
                speedUnitStr.value = unit.displayName

                engineRpm.value = v.engineRpm
                currentGearName.value = v.currentGear.displayName
                val distKm = v.distanceS / 1000f
                highwayDistanceKm.value = distKm
                fpsCount.value = renderer.fps

                currentZoneName.value = when {
                    v.distanceS < 900f -> "🌲 Pine Valley (0-0.9km)"
                    v.distanceS < 1700f -> "⛰ Canyon Pass (0.9-1.7km)"
                    v.distanceS < 2400f -> "🌉 Gorge Bridge & River (1.7-2.4km)"
                    v.distanceS < 4300f -> "🌀 Mountain Switchbacks (2.4-4.3km)"
                    v.distanceS < 5200f -> "🚇 Cliffside Tunnel (4.3-5.2km)"
                    v.distanceS < 6300f -> "🏡 Alpine Village (5.2-6.3km)"
                    v.distanceS < 7400f -> "🏔 High Ridge Spine (6.3-7.4km)"
                    else -> "⭐ Summit Lookout (7.4-8.0km)"
                }

                isInsideVehicle.value = w.isInsideVehicle
                canEnterVehicle.value = !w.isInsideVehicle && w.distanceToCar(v.worldX, v.worldY, v.worldZ) < 3.2f
                activeCameraMode.value = renderer.camera.mode

                isHeadlightsOn.value = v.headlightsOn
                isHighBeam.value = v.highBeam
                isBlinkerLeft.value = v.blinkerLeft
                isBlinkerRight.value = v.blinkerRight
                isHazard.value = v.hazardLights
                isWiperOn.value = v.wiperActive

                radioTitle.value = radioPlayer.currentTrackTitle
                radioArtist.value = radioPlayer.currentTrackArtist
                isRadioPlaying.value = radioPlayer.isPlaying

                // Accumulate total distance driven
                val deltaDist = kotlin.math.abs(v.distanceS - lastDist)
                if (deltaDist > 0.05f && deltaDist < 50f) {
                    val deltaKm = deltaDist / 1000f
                    val newTotal = repository.totalDistanceDrivenKm + deltaKm
                    repository.totalDistanceDrivenKm = newTotal
                    totalDistanceDriven.value = newTotal
                }
                lastDist = v.distanceS
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
        if (screen == AppScreen.DRIVING && controlType.value == ControlType.GYROSCOPE) {
            accelerometer?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        } else {
            sensorManager?.unregisterListener(this)
        }
    }

    // Vehicle Controls
    fun setSteerInput(steer: Float) {
        renderer.vehicle.steerInput = steer.coerceIn(-1f, 1f)
    }

    fun setThrottle(throttle: Float) {
        renderer.vehicle.throttleInput = throttle.coerceIn(0f, 1f)
    }

    fun setBrake(brake: Float) {
        renderer.vehicle.brakeInput = brake.coerceIn(0f, 1f)
    }

    fun toggleHandbrake() {
        renderer.vehicle.handbrake = !renderer.vehicle.handbrake
    }

    fun shiftGear(gear: Gear) {
        renderer.vehicle.setGearShift(gear)
        soundEngine.playShiftSound()
    }

    fun shiftManualUp() {
        renderer.vehicle.currentGearNumber = (renderer.vehicle.currentGearNumber + 1).coerceAtMost(6)
        soundEngine.playShiftSound()
    }

    fun shiftManualDown() {
        renderer.vehicle.currentGearNumber = (renderer.vehicle.currentGearNumber - 1).coerceAtLeast(-1)
        soundEngine.playShiftSound()
    }

    fun toggleHeadlights() {
        if (!renderer.vehicle.headlightsOn) {
            renderer.vehicle.headlightsOn = true
            renderer.vehicle.highBeam = false
        } else if (!renderer.vehicle.highBeam) {
            renderer.vehicle.highBeam = true
        } else {
            renderer.vehicle.headlightsOn = false
            renderer.vehicle.highBeam = false
        }
    }

    fun toggleLeftBlinker() {
        renderer.vehicle.blinkerLeft = !renderer.vehicle.blinkerLeft
        if (renderer.vehicle.blinkerLeft) renderer.vehicle.blinkerRight = false
    }

    fun toggleRightBlinker() {
        renderer.vehicle.blinkerRight = !renderer.vehicle.blinkerRight
        if (renderer.vehicle.blinkerRight) renderer.vehicle.blinkerLeft = false
    }

    fun toggleHazard() {
        renderer.vehicle.hazardLights = !renderer.vehicle.hazardLights
    }

    fun toggleWiper() {
        renderer.vehicle.wiperActive = !renderer.vehicle.wiperActive
    }

    fun setHorn(active: Boolean) {
        soundEngine.isHornActive = active
    }

    fun setNitro(active: Boolean) {
        renderer.vehicle.isNitro = active
        isNitroActive.value = active
    }

    fun respawnVehicle(distanceKm: Float = 0.05f) {
        renderer.vehicle.setPosition(distanceKm * 1000f)
    }

    fun cycleCamera() {
        renderer.camera.cycleCameraMode()
    }

    fun onCameraTouchDrag(dx: Float, dy: Float) {
        renderer.camera.onTouchDrag(dx, dy)
    }

    // Walking on Foot controls
    fun toggleEnterExitVehicle() {
        val v = renderer.vehicle
        val w = renderer.walker

        if (w.isInsideVehicle) {
            // Exit vehicle
            if (v.speedKmh < 5f) {
                soundEngine.playDoorOpenSound()
                w.exitVehicle(v.worldX, v.worldY, v.worldZ, v.headingAngleDeg)
                renderer.camera.mode = CameraMode.THIRD_PERSON
            }
        } else {
            // Enter vehicle
            if (w.distanceToCar(v.worldX, v.worldY, v.worldZ) < 3.5f) {
                soundEngine.playDoorCloseSound()
                w.enterVehicle()
            }
        }
    }

    fun setWalkerMove(fwd: Float, strafe: Float) {
        renderer.walker.moveForward = fwd
        renderer.walker.moveStrafe = strafe
    }

    fun setWalkerLook(yaw: Float) {
        renderer.walker.lookYaw = yaw
    }

    // Customization & Selection
    fun selectVehicle(spec: VehicleSpec) {
        selectedVehicle.value = spec
        repository.selectedCarId = spec.id
        renderer.activeVehicleSpec = spec
        val custom = repository.getCarCustomization(spec.id)
        selectedCustomization.value = custom
        renderer.activeCustomization = custom
    }

    fun updateCustomization(custom: CarCustomization) {
        selectedCustomization.value = custom
        repository.saveCarCustomization(selectedVehicle.value.id, custom)
        renderer.activeCustomization = custom
    }

    fun selectCharacter(charSpec: CharacterSpec) {
        selectedCharacter.value = charSpec
        repository.selectedCharacterId = charSpec.id
        renderer.activeCharacter = charSpec
    }

    fun updateGraphicsQuality(quality: GraphicsQuality) {
        graphicsQuality.value = quality
        repository.graphicsQuality = quality
        renderer.graphicsQuality = quality
    }

    fun updateControlType(type: ControlType) {
        controlType.value = type
        repository.controlType = type
        renderer.controlType = type
        if (type == ControlType.GYROSCOPE && currentScreen.value == AppScreen.DRIVING) {
            accelerometer?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        } else {
            sensorManager?.unregisterListener(this)
        }
    }

    fun updateTransmission(type: TransmissionType) {
        transmissionType.value = type
        repository.transmissionType = type
        renderer.transmissionType = type
    }

    fun updateSensitivity(value: Float) {
        controlSensitivity.value = value
        repository.controlSensitivity = value
        renderer.controlSensitivity = value
    }

    fun updateSpeedUnit(unit: SpeedUnit) {
        speedUnit.value = unit
        repository.speedUnit = unit
    }

    fun setTimeOfDay(time: TimeOfDay) {
        timeOfDay.value = time
        renderer.timeOfDaySetting = time
    }

    fun setWeather(w: WeatherType) {
        weather.value = w
        renderer.weatherSetting = w
    }

    fun updateVolumes(master: Float, engine: Float, ambience: Float, music: Float) {
        repository.masterVolume = master
        repository.engineVolume = engine
        repository.ambienceVolume = ambience
        repository.musicVolume = music

        soundEngine.masterVolume = master
        soundEngine.engineVolume = engine
        soundEngine.ambienceVolume = ambience
        radioPlayer.setMasterVolume(music)
    }

    // Radio
    fun nextRadioStation() = radioPlayer.nextStation()
    fun prevRadioStation() = radioPlayer.prevStation()
    fun toggleRadioPlay() = radioPlayer.togglePlayPause()

    fun addLocalMusic(uri: Uri, name: String) {
        radioPlayer.addLocalTrack(uri, name)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER && controlType.value == ControlType.GYROSCOPE) {
            // Landscape tilt: y axis indicates tilt left/right
            val tiltY = event.values[1]
            val steer = (-tiltY / 5.5f) * controlSensitivity.value
            setSteerInput(steer)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onCleared() {
        super.onCleared()
        sensorManager?.unregisterListener(this)
        soundEngine.stop()
        radioPlayer.stop()
    }
}
