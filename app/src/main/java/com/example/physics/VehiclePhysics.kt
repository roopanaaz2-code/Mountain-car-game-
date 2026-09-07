package com.example.physics

import com.example.model.Gear
import com.example.model.TransmissionType
import com.example.model.VehicleSpec
import com.example.renderer.MountainMap
import com.example.renderer.RoadPoint
import kotlin.math.*

class VehiclePhysics(val spec: VehicleSpec, val map: MountainMap) {
    // Spatial coordinates
    var distanceS: Float = 50.0f // Starts at 50m in pine valley
    var lateralOffset: Float = 2.0f // Right lane center (+2m from middle line)
    var worldX: Float = 0f
    var worldY: Float = 0f
    var worldZ: Float = 0f
    var headingAngleDeg: Float = 0f

    // Dynamic speeds & forces
    var speedMps: Float = 0f // meters per second
    val speedKmh: Float get() = speedMps * 3.6f
    var engineRpm: Float = 850f
    var currentGear: Gear = Gear.FIRST
    var currentGearNumber: Int = 1 // 1 to 6 for manual
    var isNitro: Boolean = false

    // Controls & inputs
    var throttleInput: Float = 0f // 0 to 1
    var brakeInput: Float = 0f // 0 to 1
    var handbrake: Boolean = false
    var steerInput: Float = 0f // -1 (left) to +1 (right)
    var steerAngleDeg: Float = 0f // Front wheel angle (-32 to +32 deg)

    // Visual & animation states
    var wheelSpinDeg: Float = 0f
    var bodyPitchDeg: Float = 0f // Dive/squat
    var bodyRollDeg: Float = 0f // Cornering lean
    var tireSlipFactor: Float = 0f // 0 to 1
    var doorOpenAngleDeg: Float = 0f
    var hasCollisionImpact: Boolean = false
    var collisionSparksX: Float = 0f
    var collisionSparksY: Float = 0f
    var collisionSparksZ: Float = 0f

    // 4-Wheel Suspension Contact Positions (FL, FR, RL, RR)
    var flSuspensionCompression: Float = 0f
    var frSuspensionCompression: Float = 0f
    var rlSuspensionCompression: Float = 0f
    var rrSuspensionCompression: Float = 0f

    // Dynamic velocities
    private var lateralVelocity: Float = 0f
    private var driftAngleDeg: Float = 0f

    // Light controls
    var headlightsOn: Boolean = true
    var highBeam: Boolean = false
    var hazardLights: Boolean = false
    var blinkerLeft: Boolean = false
    var blinkerRight: Boolean = false
    var blinkTimer: Float = 0f
    val isBlinkerBlink: Boolean get() = (blinkTimer % 0.6f) < 0.35f

    // Wiper animation
    var wiperActive: Boolean = false
    var wiperPhase: Float = 0f

    init {
        updateWorldPosition()
    }

    fun update(deltaTimeSec: Float, transmissionType: TransmissionType, sensitivity: Float) {
        val dt = deltaTimeSec.coerceIn(0.001f, 0.1f)

        // 1. Steering dynamics with speed-sensitive ratio
        val speedSensitivity = (1.0f - (speedKmh / 350f) * 0.65f).coerceIn(0.35f, 1.0f)
        val maxSteer = 30f * speedSensitivity * sensitivity
        val targetSteer = steerInput * maxSteer
        steerAngleDeg += (targetSteer - steerAngleDeg) * (14f * dt).coerceAtMost(1f)

        // 2. Transmission & Engine RPM
        updateGearsAndRpm(dt, transmissionType)

        // 3. Drive & Brake Forces
        val baseMaxSpeedMps = (spec.topSpeedKmh / 3.6f)
        val maxSpeedMps = if (isNitro) baseMaxSpeedMps * 1.25f else baseMaxSpeedMps
        val basePower = (100f / spec.acceleration0To100) * 1.8f
        val enginePower = if (isNitro) basePower * 2.2f else basePower
        val accelForce = when (currentGear) {
            Gear.REVERSE -> -throttleInput * enginePower * 0.45f
            Gear.PARK, Gear.NEUTRAL -> 0f
            else -> throttleInput * enginePower * (1.0f - (speedMps / maxSpeedMps).pow(1.5f).coerceIn(0f, 1f))
        }

        val brakeForce = brakeInput * 28.0f * spec.braking + (if (handbrake) 35.0f else 0f)
        val rollingResistance = 0.45f
        val airDrag = 0.0015f * speedMps * speedMps

        // Grade resistance (mountain slope)
        val currentPoint = map.getRoadPointAt(distanceS)
        val slopeGrade = currentPoint.tanY * 9.81f

        var netAccel = 0f
        if (speedMps > 0.05f) {
            netAccel = accelForce - brakeForce - rollingResistance - airDrag - slopeGrade
        } else if (speedMps < -0.05f) {
            netAccel = accelForce + brakeForce + rollingResistance + airDrag - slopeGrade
        } else {
            // Static or start
            if (currentGear == Gear.REVERSE && throttleInput > 0.05f) {
                netAccel = accelForce
            } else if (currentGear != Gear.PARK && currentGear != Gear.NEUTRAL && throttleInput > 0.05f) {
                netAccel = accelForce
            } else {
                speedMps = 0f
            }
        }

        speedMps += netAccel * dt
        if (currentGear == Gear.PARK) speedMps = 0f

        // 4. Movement along highway
        distanceS = (distanceS + speedMps * dt).coerceIn(0f, map.totalLength)

        // Lateral steering displacement & Drift Dynamics
        val isDrifting = (handbrake && speedKmh > 12f) || (abs(steerAngleDeg) > 18f && speedKmh > 85f)
        val lateralTraction = if (isDrifting) 0.35f else 1.0f

        val targetLateralSpeed = (steerAngleDeg / 30f) * (speedMps * 0.52f)
        lateralVelocity += (targetLateralSpeed - lateralVelocity) * (8f * lateralTraction * dt)
        lateralOffset += lateralVelocity * dt

        // Drift yaw angle
        val targetDrift = if (isDrifting) {
            (-steerAngleDeg * 0.85f * (speedKmh / 100f).coerceIn(0.5f, 1.5f))
        } else {
            0f
        }
        driftAngleDeg += (targetDrift - driftAngleDeg) * (5f * dt)

        // Barrier & Road Edge Collisions (Crash barriers and mountain rock walls)
        hasCollisionImpact = false
        val maxRoadOffset = 4.65f
        if (abs(lateralOffset) > maxRoadOffset) {
            hasCollisionImpact = true
            collisionSparksX = worldX
            collisionSparksY = worldY + 0.3f
            collisionSparksZ = worldZ

            // Elastic bounce off barrier
            lateralVelocity = -lateralVelocity * 0.35f
            lateralOffset = sign(lateralOffset) * maxRoadOffset
            // Friction loss
            speedMps *= (1.0f - 3.5f * dt).coerceAtLeast(0.4f)
        }

        // 5. Wheel Spin
        val wheelCircumference = 2f * PI.toFloat() * 0.35f
        wheelSpinDeg = (wheelSpinDeg + (speedMps / wheelCircumference) * 360f * dt) % 360f

        // 6. Centripetal acceleration & Tire slip calculation
        val centripetalAccel = (speedMps * speedMps * (steerAngleDeg / 30f) * 0.055f)
        val lateralG = abs(centripetalAccel)
        tireSlipFactor = (lateralG / 13f + (if (isDrifting) 0.65f else 0f)).coerceIn(0f, 1f)

        // 7. Suspension Physics & Road Banking
        updateWorldPositionAndSuspension(dt, netAccel, centripetalAccel)

        // 8. Timers & Wipers
        blinkTimer += dt
        if (wiperActive) {
            wiperPhase = (wiperPhase + dt * 4.0f) % (2f * PI.toFloat())
        }
    }

    private fun updateWorldPositionAndSuspension(dt: Float, netAccel: Float, centripetalAccel: Float) {
        val pt = map.getRoadPointAt(distanceS)

        // Center on 3D highway with lateral offset
        worldX = pt.x + pt.normX * lateralOffset
        worldZ = pt.z + pt.normZ * lateralOffset

        // Base road heading + drift angle
        val roadHeading = Math.toDegrees(atan2(pt.tanX.toDouble(), pt.tanZ.toDouble())).toFloat()
        headingAngleDeg = roadHeading + driftAngleDeg

        // 4-Wheel Contact Sampling (FL, FR, RL, RR)
        val headingRad = Math.toRadians(headingAngleDeg.toDouble()).toFloat()
        val fwdX = sin(headingRad)
        val fwdZ = cos(headingRad)
        val rightX = cos(headingRad)
        val rightZ = -sin(headingRad)

        val halfW = 0.85f // track width half
        val halfL = 1.35f // wheelbase half

        // Wheel world positions
        val flX = worldX + rightX * -halfW + fwdX * halfL
        val flZ = worldZ + rightZ * -halfW + fwdZ * halfL
        val frX = worldX + rightX * halfW + fwdX * halfL
        val frZ = worldZ + rightZ * halfW + fwdZ * halfL

        val rlX = worldX + rightX * -halfW - fwdX * halfL
        val rlZ = worldZ + rightZ * -halfW - fwdZ * halfL
        val rrX = worldX + rightX * halfW - fwdX * halfL
        val rrZ = worldZ + rightZ * halfW - fwdZ * halfL

        // Ground heights beneath 4 tires
        val flGroundY = map.getSurfaceHeight(flX, flZ)
        val frGroundY = map.getSurfaceHeight(frX, frZ)
        val rlGroundY = map.getSurfaceHeight(rlX, rlZ)
        val rrGroundY = map.getSurfaceHeight(rrX, rrZ)

        // Static slope and road banking derived from terrain
        val staticPitch = Math.toDegrees(atan2(((flGroundY + frGroundY) * 0.5f - (rlGroundY + rrGroundY) * 0.5f).toDouble(), (halfL * 2f).toDouble())).toFloat()
        val staticRoll = Math.toDegrees(atan2(((flGroundY + rlGroundY) * 0.5f - (frGroundY + rrGroundY) * 0.5f).toDouble(), (halfW * 2f).toDouble())).toFloat()

        // Dynamic inertia from acceleration and cornering
        val targetDynamicPitch = (netAccel / 9.8f) * -3.2f
        val targetDynamicRoll = (centripetalAccel / 9.8f) * 4.2f

        val targetPitch = (staticPitch + targetDynamicPitch).coerceIn(-25f, 25f)
        val targetRoll = (staticRoll + targetDynamicRoll).coerceIn(-20f, 20f)

        bodyPitchDeg += (targetPitch - bodyPitchDeg) * (10f * dt)
        bodyRollDeg += (targetRoll - bodyRollDeg) * (9f * dt)

        // Center chassis elevation
        val avgGroundY = (flGroundY + frGroundY + rlGroundY + rrGroundY) * 0.25f
        worldY = avgGroundY + 0.15f
    }

    private fun updateGearsAndRpm(dt: Float, transmissionType: TransmissionType) {
        val idleRpm = 850f
        val redlineRpm = 8200f

        if (transmissionType == TransmissionType.AUTOMATIC) {
            if (currentGear == Gear.PARK && throttleInput > 0.05f) {
                currentGear = Gear.FIRST
            }
            if (currentGear != Gear.PARK && currentGear != Gear.REVERSE && currentGear != Gear.NEUTRAL) {
                // Auto Shift
                when {
                    speedKmh < 35f -> currentGear = Gear.FIRST
                    speedKmh < 65f -> currentGear = Gear.SECOND
                    speedKmh < 105f -> currentGear = Gear.THIRD
                    speedKmh < 155f -> currentGear = Gear.FOURTH
                    speedKmh < 215f -> currentGear = Gear.FIFTH
                    else -> currentGear = Gear.SIXTH
                }
            }
        } else {
            currentGear = when (currentGearNumber) {
                -1 -> Gear.REVERSE
                0 -> Gear.NEUTRAL
                1 -> Gear.FIRST
                2 -> Gear.SECOND
                3 -> Gear.THIRD
                4 -> Gear.FOURTH
                5 -> Gear.FIFTH
                6 -> Gear.SIXTH
                else -> Gear.FIRST
            }
        }

        // RPM calculation based on gear ratio and vehicle speed
        val ratio = abs(currentGear.ratio).coerceAtLeast(0.5f)
        if (currentGear == Gear.PARK || currentGear == Gear.NEUTRAL) {
            val targetRpm = idleRpm + throttleInput * 6500f
            engineRpm += (targetRpm - engineRpm) * (9f * dt)
        } else {
            val mechanicalRpm = (abs(speedMps) * ratio * 75f) + idleRpm
            val nitroRpm = if (isNitro) 1200f else 0f
            val targetRpm = mechanicalRpm + throttleInput * 600f + nitroRpm
            engineRpm = targetRpm.coerceIn(idleRpm, redlineRpm)
        }
    }

    private fun updateWorldPosition() {
        val pt = map.getRoadPointAt(distanceS)
        // Position on 3D highway with lateral offset
        worldX = pt.x + pt.normX * lateralOffset
        worldY = pt.y + 0.15f
        worldZ = pt.z + pt.normZ * lateralOffset

        // Road heading
        headingAngleDeg = Math.toDegrees(atan2(pt.tanX.toDouble(), pt.tanZ.toDouble())).toFloat()
    }

    fun setPosition(newDistanceS: Float) {
        distanceS = newDistanceS.coerceIn(0f, map.totalLength)
        lateralOffset = 2.0f
        speedMps = 0f
        engineRpm = 850f
        if (currentGear == Gear.PARK) currentGear = Gear.FIRST
        updateWorldPosition()
    }

    fun setGearShift(newGear: Gear) {
        currentGear = newGear
    }
}
