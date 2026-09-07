package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.*

class GameRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("mountain_drive_save", Context.MODE_PRIVATE)

    var totalDistanceDrivenKm: Float
        get() = prefs.getFloat("total_distance_km", 0f)
        set(value) = prefs.edit().putFloat("total_distance_km", value).apply()

    var selectedCarId: String
        get() = prefs.getString("selected_car_id", GameContent.vehicles[0].id) ?: GameContent.vehicles[0].id
        set(value) = prefs.edit().putString("selected_car_id", value).apply()

    var selectedCharacterId: String
        get() = prefs.getString("selected_character_id", GameContent.characters[0].id) ?: GameContent.characters[0].id
        set(value) = prefs.edit().putString("selected_character_id", value).apply()

    var graphicsQuality: GraphicsQuality
        get() = GraphicsQuality.valueOf(prefs.getString("graphics_quality", GraphicsQuality.HIGH.name) ?: GraphicsQuality.HIGH.name)
        set(value) = prefs.edit().putString("graphics_quality", value.name).apply()

    var controlType: ControlType
        get() = ControlType.valueOf(prefs.getString("control_type", ControlType.STEERING_WHEEL.name) ?: ControlType.STEERING_WHEEL.name)
        set(value) = prefs.edit().putString("control_type", value.name).apply()

    var transmissionType: TransmissionType
        get() = TransmissionType.valueOf(prefs.getString("transmission_type", TransmissionType.AUTOMATIC.name) ?: TransmissionType.AUTOMATIC.name)
        set(value) = prefs.edit().putString("transmission_type", value.name).apply()

    var speedUnit: SpeedUnit
        get() = SpeedUnit.valueOf(prefs.getString("speed_unit", SpeedUnit.KMH.name) ?: SpeedUnit.KMH.name)
        set(value) = prefs.edit().putString("speed_unit", value.name).apply()

    var controlSensitivity: Float
        get() = prefs.getFloat("control_sensitivity", 1.0f)
        set(value) = prefs.edit().putFloat("control_sensitivity", value).apply()

    var masterVolume: Float
        get() = prefs.getFloat("master_volume", 1.0f)
        set(value) = prefs.edit().putFloat("master_volume", value).apply()

    var engineVolume: Float
        get() = prefs.getFloat("engine_volume", 0.85f)
        set(value) = prefs.edit().putFloat("engine_volume", value).apply()

    var ambienceVolume: Float
        get() = prefs.getFloat("ambience_volume", 0.65f)
        set(value) = prefs.edit().putFloat("ambience_volume", value).apply()

    var musicVolume: Float
        get() = prefs.getFloat("music_volume", 0.70f)
        set(value) = prefs.edit().putFloat("music_volume", value).apply()

    fun getCarCustomization(carId: String): CarCustomization {
        val paint = prefs.getInt("custom_${carId}_paint", 0)
        val livery = prefs.getInt("custom_${carId}_livery", 0)
        val wheel = prefs.getInt("custom_${carId}_wheel", 0)
        val interior = prefs.getInt("custom_${carId}_interior", 0)
        return CarCustomization(paint, livery, wheel, interior)
    }

    fun saveCarCustomization(carId: String, custom: CarCustomization) {
        prefs.edit()
            .putInt("custom_${carId}_paint", custom.paintColorIndex)
            .putInt("custom_${carId}_livery", custom.liveryIndex)
            .putInt("custom_${carId}_wheel", custom.wheelIndex)
            .putInt("custom_${carId}_interior", custom.interiorIndex)
            .apply()
    }

    fun isCarUnlocked(car: VehicleSpec): Boolean {
        if (car.unlockKm <= 0.01f) return true
        return totalDistanceDrivenKm >= car.unlockKm
    }
}
