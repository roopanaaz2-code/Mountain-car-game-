package com.example.physics

import kotlin.math.*

class PlayerWalkingPhysics {
    var isInsideVehicle: Boolean = true
    var worldX: Float = 0f
    var worldY: Float = 0f
    var worldZ: Float = 0f
    var headingAngleDeg: Float = 0f

    var walkSpeedMps: Float = 0f
    var walkPhase: Float = 0f
    val isWalking: Boolean get() = walkSpeedMps > 0.1f

    // Input vectors (-1 to 1)
    var moveForward: Float = 0f
    var moveStrafe: Float = 0f
    var lookYaw: Float = 0f
    var lookPitch: Float = 0f

    fun exitVehicle(carX: Float, carY: Float, carZ: Float, carHeadingDeg: Float) {
        isInsideVehicle = false
        // Spawn 1.5m to the left of the driver door
        val rad = Math.toRadians((carHeadingDeg - 90.0)).toFloat()
        worldX = carX + cos(rad) * 1.5f
        worldY = carY
        worldZ = carZ - sin(rad) * 1.5f
        headingAngleDeg = carHeadingDeg
        walkSpeedMps = 0f
        walkPhase = 0f
    }

    fun enterVehicle() {
        isInsideVehicle = true
        walkSpeedMps = 0f
    }

    fun distanceToCar(carX: Float, carY: Float, carZ: Float): Float {
        val dx = worldX - carX
        val dy = worldY - carY
        val dz = worldZ - carZ
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    fun update(deltaTimeSec: Float) {
        if (isInsideVehicle) return
        val dt = deltaTimeSec.coerceIn(0.001f, 0.1f)

        headingAngleDeg = (headingAngleDeg + lookYaw * 90f * dt) % 360f

        val moveLen = sqrt(moveForward * moveForward + moveStrafe * moveStrafe)
        if (moveLen > 0.1f) {
            val maxSpeed = 3.5f // gentle walking speed
            walkSpeedMps = (moveLen.coerceAtMost(1f)) * maxSpeed
            walkPhase += walkSpeedMps * 7f * dt

            // Move in direction of heading
            val rad = Math.toRadians(headingAngleDeg.toDouble())
            val fwdX = sin(rad).toFloat()
            val fwdZ = cos(rad).toFloat()
            val rightX = cos(rad).toFloat()
            val rightZ = -sin(rad).toFloat()

            worldX += (fwdX * moveForward + rightX * moveStrafe) * maxSpeed * dt
            worldZ += (fwdZ * moveForward + rightZ * moveStrafe) * maxSpeed * dt
        } else {
            walkSpeedMps = 0f
        }
    }
}
