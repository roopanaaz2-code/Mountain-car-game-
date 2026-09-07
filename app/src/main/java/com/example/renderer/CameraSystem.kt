package com.example.renderer

import com.example.model.CameraMode
import com.example.physics.PlayerWalkingPhysics
import com.example.physics.VehiclePhysics
import kotlin.math.*

class CameraSystem {
    var mode: CameraMode = CameraMode.THIRD_PERSON

    // Camera world position & look-at target (safe non-zero defaults to prevent NaN on first frame)
    var eyeX: Float = 0f
    var eyeY: Float = 4f
    var eyeZ: Float = 8f

    var targetX: Float = 0f
    var targetY: Float = 1f
    var targetZ: Float = 0f

    var upX: Float = 0f
    var upY: Float = 1f
    var upZ: Float = 0f

    // 360 Orbit touch angles
    var orbitYawDeg: Float = 0f
    var orbitPitchDeg: Float = 14f
    var orbitDistance: Float = 5.8f

    // Smooth dampening
    private var isInitialized: Boolean = false
    private var smoothEyeX: Float = 0f
    private var smoothEyeY: Float = 0f
    private var smoothEyeZ: Float = 0f

    fun cycleCameraMode() {
        mode = when (mode) {
            CameraMode.THIRD_PERSON -> CameraMode.DRIVER
            CameraMode.DRIVER -> CameraMode.FRONT
            CameraMode.FRONT -> CameraMode.REAR
            CameraMode.REAR -> CameraMode.THIRD_PERSON
        }
    }

    fun onTouchDrag(deltaX: Float, deltaY: Float) {
        // Drag finger to orbit camera 360 degrees around car
        orbitYawDeg = (orbitYawDeg - deltaX * 0.45f) % 360f
        orbitPitchDeg = (orbitPitchDeg - deltaY * 0.35f).coerceIn(-15f, 65f)
    }

    fun update(
        deltaTimeSec: Float,
        vehicle: VehiclePhysics,
        walker: PlayerWalkingPhysics
    ) {
        val dt = deltaTimeSec.coerceIn(0.001f, 0.1f)
        val carX = vehicle.worldX
        val carY = vehicle.worldY
        val carZ = vehicle.worldZ
        val carHeading = vehicle.headingAngleDeg

        val carHeadingRad = Math.toRadians(carHeading.toDouble()).toFloat()
        val fwdX = sin(carHeadingRad)
        val fwdZ = cos(carHeadingRad)
        // Correct 3D perpendicular right vector
        val rightX = -fwdZ
        val rightZ = fwdX

        if (!walker.isInsideVehicle) {
            // ON-FOOT CAMERA (follows walking explorer)
            val pX = walker.worldX
            val pY = walker.worldY + 1.4f
            val pZ = walker.worldZ

            val totalYawRad = Math.toRadians((walker.headingAngleDeg + orbitYawDeg + 180f).toDouble()).toFloat()
            val pitchRad = Math.toRadians(orbitPitchDeg.toDouble()).toFloat()

            val dist = 3.5f
            val camX = pX + sin(totalYawRad) * cos(pitchRad) * dist
            val camY = pY + sin(pitchRad) * dist + 0.6f
            val camZ = pZ + cos(totalYawRad) * cos(pitchRad) * dist

            if (!isInitialized) {
                smoothEyeX = camX
                smoothEyeY = camY
                smoothEyeZ = camZ
                isInitialized = true
            } else {
                smoothEyeX += (camX - smoothEyeX) * (12f * dt)
                smoothEyeY += (camY - smoothEyeY) * (12f * dt)
                smoothEyeZ += (camZ - smoothEyeZ) * (12f * dt)
            }

            eyeX = smoothEyeX
            eyeY = smoothEyeY
            eyeZ = smoothEyeZ

            targetX = pX
            targetY = pY
            targetZ = pZ
            return
        }

        when (mode) {
            CameraMode.DRIVER -> {
                // Interior Cockpit Camera: inside driver seat, looking forward through windshield at steering wheel & dials
                val driverOffsetX = -0.42f
                val driverOffsetY = 0.95f
                val driverOffsetZ = 0.22f

                val posX = carX + rightX * driverOffsetX + fwdX * driverOffsetZ
                val posY = carY + driverOffsetY + (vehicle.speedKmh / 200f) * 0.02f
                val posZ = carZ + rightZ * driverOffsetX + fwdZ * driverOffsetZ

                eyeX = posX
                eyeY = posY
                eyeZ = posZ

                // Look 25m ahead along car's forward vector
                targetX = eyeX + fwdX * 25f
                targetY = eyeY - 0.35f + (vehicle.bodyPitchDeg * 0.05f)
                targetZ = eyeZ + fwdZ * 25f
            }

            CameraMode.FRONT -> {
                // Hood / Front Bumper Camera: low, high sense of speed
                val hoodZ = 2.1f
                val hoodY = 0.65f

                eyeX = carX + fwdX * hoodZ
                eyeY = carY + hoodY
                eyeZ = carZ + fwdZ * hoodZ

                targetX = eyeX + fwdX * 30f
                targetY = eyeY - 0.2f
                targetZ = eyeZ + fwdZ * 30f
            }

            CameraMode.REAR -> {
                // Rear Facing Camera: looking back from car
                val rearZ = -2.4f
                val rearY = 0.90f

                eyeX = carX + fwdX * rearZ
                eyeY = carY + rearY
                eyeZ = carZ + fwdZ * rearZ

                targetX = eyeX - fwdX * 30f
                targetY = eyeY
                targetZ = eyeZ - fwdZ * 30f
            }

            CameraMode.THIRD_PERSON -> {
                // 360 Full Orbit Spring-Arm Chase Camera
                val totalYaw = carHeading + 180f + orbitYawDeg
                val yawRad = Math.toRadians(totalYaw.toDouble()).toFloat()
                val pitchRad = Math.toRadians(orbitPitchDeg.toDouble()).toFloat()

                val desiredDist = orbitDistance
                val desiredX = carX + sin(yawRad) * cos(pitchRad) * desiredDist
                val desiredY = (carY + 0.9f) + sin(pitchRad) * desiredDist + 0.9f
                val desiredZ = carZ + cos(yawRad) * cos(pitchRad) * desiredDist

                if (!isInitialized) {
                    smoothEyeX = desiredX
                    smoothEyeY = desiredY
                    smoothEyeZ = desiredZ
                    isInitialized = true
                } else {
                    val lag = (8f * dt).coerceAtMost(1f)
                    smoothEyeX += (desiredX - smoothEyeX) * lag
                    smoothEyeY += (desiredY - smoothEyeY) * lag
                    smoothEyeZ += (desiredZ - smoothEyeZ) * lag
                }

                eyeX = smoothEyeX
                eyeY = smoothEyeY
                eyeZ = smoothEyeZ

                targetX = carX
                targetY = carY + 0.95f
                targetZ = carZ
            }
        }
    }
}
