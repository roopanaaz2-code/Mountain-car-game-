package com.example.model

enum class GraphicsQuality(val displayName: String, val renderDistance: Float, val foliageDensity: Float, val hasRainParticles: Boolean) {
    LOW("Low", 350f, 0.4f, false),
    MEDIUM("Medium", 550f, 0.7f, true),
    HIGH("High", 800f, 1.0f, true),
    ULTRA("Ultra", 1200f, 1.3f, true)
}

enum class ControlType(val displayName: String) {
    STEERING_WHEEL("On-Screen Wheel"),
    BUTTONS("Arrow Buttons"),
    GYROSCOPE("Tilt / Gyroscope")
}

enum class TransmissionType(val displayName: String) {
    AUTOMATIC("Automatic"),
    MANUAL("Manual (6-Speed)")
}

enum class SpeedUnit(val displayName: String, val conversionFactor: Float) {
    KMH("km/h", 1.0f),
    MPH("mph", 0.621371f)
}

enum class TimeOfDay(val displayName: String, val hour: Float) {
    DYNAMIC("Dynamic Cycle", -1f),
    SUNRISE("Sunrise", 6.0f),
    MORNING("Morning", 9.5f),
    AFTERNOON("Afternoon", 14.0f),
    SUNSET("Sunset", 18.5f),
    EVENING("Evening", 20.5f),
    NIGHT("Night", 23.5f),
    MOONRISE("Moonrise", 21.5f),
    MOONSET("Moonset", 4.5f)
}

enum class WeatherType(val displayName: String) {
    CLEAR("Clear Alpine Sky"),
    MIST("Mountain Mist / Fog"),
    RAIN("Mountain Rain")
}

enum class CameraMode(val displayName: String) {
    DRIVER("Interior / Cockpit"),
    FRONT("Front Bumper / Hood"),
    REAR("Rear Cam"),
    THIRD_PERSON("360° Third-Person")
}

enum class Gear(val displayName: String, val ratio: Float) {
    REVERSE("R", -2.8f),
    NEUTRAL("N", 0.0f),
    PARK("P", 0.0f),
    FIRST("1", 3.6f),
    SECOND("2", 2.2f),
    THIRD("3", 1.5f),
    FOURTH("4", 1.1f),
    FIFTH("5", 0.85f),
    SIXTH("6", 0.7f)
}

data class CarCustomization(
    val paintColorIndex: Int = 0,
    val liveryIndex: Int = 0,
    val wheelIndex: Int = 0,
    val interiorIndex: Int = 0
)

data class VehicleSpec(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val topSpeedKmh: Float,
    val acceleration0To100: Float, // seconds
    val handling: Float, // 0.0 to 1.0
    val braking: Float, // 0.0 to 1.0
    val unlockKm: Float, // distance required to unlock
    val defaultColor: Int,
    val engineSoundPitchMultiplier: Float = 1.0f
)

data class CharacterSpec(
    val id: String,
    val name: String,
    val gender: String,
    val description: String,
    val outfitColor: Long
)
