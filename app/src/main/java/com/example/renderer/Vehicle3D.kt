package com.example.renderer

import android.opengl.GLES20
import com.example.model.CarCustomization
import com.example.model.GameContent
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class Vehicle3D {
    // Reusable buffers for rendering
    private var bodyBuffer: FloatBuffer? = null
    private var bodyVertexCount = 0

    private var glassBuffer: FloatBuffer? = null
    private var glassVertexCount = 0

    private var wheelBuffer: FloatBuffer? = null
    private var wheelVertexCount = 0

    private var interiorBuffer: FloatBuffer? = null
    private var interiorVertexCount = 0

    private var steeringWheelBuffer: FloatBuffer? = null
    private var steeringWheelVertexCount = 0

    private var doorLeftBuffer: FloatBuffer? = null
    private var doorLeftVertexCount = 0

    private var nitroFlameBuffer: FloatBuffer? = null
    private var nitroFlameVertexCount = 0

    private var lastVehicleId: String = ""
    private var lastCustomization: CarCustomization? = null

    // Scratch matrices for rendering
    private val carMVP = FloatArray(16)
    private val partModel = FloatArray(16)
    private val partMVP = FloatArray(16)

    fun prepareVehicle(vehicleId: String, customization: CarCustomization) {
        if (vehicleId == lastVehicleId && customization == lastCustomization && bodyBuffer != null) {
            return
        }
        lastVehicleId = vehicleId
        lastCustomization = customization

        val paintColor = GameContent.paintColors[customization.paintColorIndex.coerceIn(0, GameContent.paintColors.size - 1)]
        val livery = customization.liveryIndex
        val wheelStyle = customization.wheelIndex
        val interiorTrim = GameContent.interiorTrims[customization.interiorIndex.coerceIn(0, GameContent.interiorTrims.size - 1)]

        buildBodyMesh(vehicleId, paintColor.rgbFloats, livery)
        buildGlassMesh(vehicleId)
        buildWheelMesh(wheelStyle)
        buildInteriorMesh(interiorTrim.rgbFloats)
        buildSteeringWheelMesh()
        buildDoorMesh(paintColor.rgbFloats)
        buildNitroFlameMesh()
    }

    private fun buildBodyMesh(vehicleId: String, paintRgb: FloatArray, livery: Int) {
        val data = ArrayList<Float>()
        val p = paintRgb

        // Vehicle category & dimensions
        val isSupercar = vehicleId == "supercar_apex"
        val isSUV = vehicleId == "suv_summit"
        val isCoupe = vehicleId == "coupe_bavaria"
        val isSports = vehicleId == "sports_phantom"
        val isSedan = vehicleId == "sedan_apex" || vehicleId == "sedan_monarch"

        val halfW = if (isSupercar) 1.08f else if (isSUV) 1.02f else 0.96f
        val halfL = if (isSupercar) 2.38f else if (isSUV) 2.22f else if (isSedan) 2.45f else 2.18f
        val roofH = if (isSupercar) 1.08f else if (isSUV) 1.76f else 1.32f
        val waistH = if (isSupercar) 0.58f else if (isSUV) 0.95f else 0.72f
        val hoodL = if (isSupercar) 1.25f else 1.28f
        val trunkL = if (isSupercar) 1.15f else 1.12f

        val bodyColor = floatArrayOf(p[0], p[1], p[2], 1.0f)
        val stripeColor = floatArrayOf(0.96f, 0.96f, 0.96f, 1.0f)
        val trimBlack = floatArrayOf(0.08f, 0.08f, 0.09f, 1.0f)
        val carbonColor = floatArrayOf(0.14f, 0.14f, 0.16f, 1.0f)
        val chromeColor = floatArrayOf(0.85f, 0.88f, 0.90f, 1.0f)
        val engineRed = floatArrayOf(0.92f, 0.15f, 0.12f, 1.0f)

        // 1. Lower chassis / underbody tray
        addBox(data, 0f, 0.22f, 0f, halfW * 2f, 0.30f, halfL * 2f, trimBlack)

        // 2. Hood / Front Fenders
        val hoodColor = if (livery == 3) carbonColor else bodyColor
        addBox(data, 0f, waistH, hoodL * 0.48f, halfW * 1.92f, 0.20f, hoodL, hoodColor)

        // Dual Racing Stripes
        if (livery == 1) {
            addBox(data, -0.22f, waistH + 0.01f, hoodL * 0.48f, 0.14f, 0.205f, hoodL, stripeColor)
            addBox(data, 0.22f, waistH + 0.01f, hoodL * 0.48f, 0.14f, 0.205f, hoodL, stripeColor)
        }

        // 3. Front Nose, Splitter & Intakes
        val frontZ = halfL
        if (isSupercar) {
            // Lambo wedge sharp sloping nose
            addBox(data, 0f, waistH * 0.55f, frontZ - 0.20f, halfW * 1.96f, waistH * 0.65f, 0.40f, bodyColor)
            // Carbon front splitter with side winglets
            addBox(data, 0f, 0.14f, frontZ - 0.05f, halfW * 2.05f, 0.08f, 0.35f, carbonColor)
            addBox(data, -halfW * 1.02f, 0.22f, frontZ - 0.05f, 0.08f, 0.18f, 0.25f, carbonColor)
            addBox(data, halfW * 1.02f, 0.22f, frontZ - 0.05f, 0.08f, 0.18f, 0.25f, carbonColor)
            // Hexagonal black air dams
            addBox(data, -0.55f, waistH * 0.42f, frontZ - 0.02f, 0.45f, 0.24f, 0.10f, trimBlack)
            addBox(data, 0.55f, waistH * 0.42f, frontZ - 0.02f, 0.45f, 0.24f, 0.10f, trimBlack)
            addBox(data, 0f, waistH * 0.35f, frontZ - 0.02f, 0.35f, 0.18f, 0.10f, trimBlack)
        } else {
            // Standard / Coupe / SUV / Sedan front
            addBox(data, 0f, waistH * 0.60f, frontZ - 0.15f, halfW * 1.94f, waistH * 0.70f, 0.30f, bodyColor)
            addBox(data, 0f, waistH * 0.45f, frontZ - 0.02f, halfW * 1.4f, 0.24f, 0.08f, trimBlack)
        }

        // 4. Mid / Rear Deck & Engine Bay
        if (isSupercar) {
            // Mid-engine V12 compartment behind cabin
            addBox(data, 0f, waistH + 0.02f, -trunkL * 0.45f, halfW * 1.88f, 0.22f, trunkL, bodyColor)
            // Exposed V12 engine cover with red intake runners
            addBox(data, 0f, waistH + 0.04f, -trunkL * 0.45f, 0.65f, 0.14f, 0.70f, carbonColor)
            addBox(data, -0.18f, waistH + 0.12f, -trunkL * 0.45f, 0.12f, 0.06f, 0.65f, engineRed)
            addBox(data, 0.18f, waistH + 0.12f, -trunkL * 0.45f, 0.12f, 0.06f, 0.65f, engineRed)
            // Side massive Lambo air intakes before rear wheels
            addBox(data, -halfW * 0.96f, waistH * 0.75f, -0.25f, 0.20f, 0.35f, 0.60f, carbonColor)
            addBox(data, halfW * 0.96f, waistH * 0.75f, -0.25f, 0.20f, 0.35f, 0.60f, carbonColor)
        } else {
            // Standard rear trunk
            addBox(data, 0f, waistH, -trunkL * 0.50f, halfW * 1.88f, 0.22f, trunkL, bodyColor)
        }

        if (livery == 1) {
            addBox(data, -0.22f, waistH + 0.01f, -trunkL * 0.50f, 0.14f, 0.225f, trunkL, stripeColor)
            addBox(data, 0.22f, waistH + 0.01f, -trunkL * 0.50f, 0.14f, 0.225f, trunkL, stripeColor)
        }

        // 5. Rear Bumper & Diffuser
        val rearZ = -halfL
        addBox(data, 0f, waistH * 0.65f, rearZ + 0.15f, halfW * 1.94f, waistH * 0.80f, 0.30f, bodyColor)
        // Aggressive rear aerodynamic diffuser
        addBox(data, 0f, 0.26f, rearZ + 0.05f, halfW * 1.70f, 0.22f, 0.20f, carbonColor)
        // Vertical diffuser strakes
        addBox(data, -0.30f, 0.26f, rearZ + 0.05f, 0.04f, 0.25f, 0.25f, carbonColor)
        addBox(data, 0.30f, 0.26f, rearZ + 0.05f, 0.04f, 0.25f, 0.25f, carbonColor)

        // Quad Exhaust Tips (Centered for Supercar)
        if (isSupercar) {
            addBox(data, -0.20f, 0.38f, rearZ - 0.06f, 0.11f, 0.11f, 0.18f, chromeColor)
            addBox(data, -0.07f, 0.38f, rearZ - 0.06f, 0.11f, 0.11f, 0.18f, chromeColor)
            addBox(data, 0.07f, 0.38f, rearZ - 0.06f, 0.11f, 0.11f, 0.18f, chromeColor)
            addBox(data, 0.20f, 0.38f, rearZ - 0.06f, 0.11f, 0.11f, 0.18f, chromeColor)
        } else {
            addBox(data, -0.60f, 0.28f, rearZ - 0.05f, 0.12f, 0.08f, 0.15f, chromeColor)
            addBox(data, 0.60f, 0.28f, rearZ - 0.05f, 0.12f, 0.08f, 0.15f, chromeColor)
        }

        // 6. Roof & Pillars
        val roofW = halfW * 1.48f
        val roofL = halfL * 0.88f
        val roofZ = -0.08f
        val roofColor = if (livery == 3) carbonColor else bodyColor
        addBox(data, 0f, roofH, roofZ, roofW, 0.08f, roofL, roofColor)
        if (livery == 1) {
            addBox(data, -0.22f, roofH + 0.005f, roofZ, 0.14f, 0.085f, roofL, stripeColor)
            addBox(data, 0.22f, roofH + 0.005f, roofZ, 0.14f, 0.085f, roofL, stripeColor)
        }

        // 7. Supercar GT Wing / Spoiler OR SUV Roof Bars
        if (isSupercar || livery == 3) {
            // Aggressive high-downforce GT wing
            addBox(data, -0.65f, waistH + 0.35f, rearZ + 0.28f, 0.06f, 0.48f, 0.08f, carbonColor)
            addBox(data, 0.65f, waistH + 0.35f, rearZ + 0.28f, 0.06f, 0.48f, 0.08f, carbonColor)
            addBox(data, 0f, waistH + 0.58f, rearZ + 0.28f, halfW * 1.95f, 0.06f, 0.35f, carbonColor)
            // Wing endplates
            addBox(data, -halfW * 0.98f, waistH + 0.58f, rearZ + 0.28f, 0.04f, 0.18f, 0.42f, carbonColor)
            addBox(data, halfW * 0.98f, waistH + 0.58f, rearZ + 0.28f, 0.04f, 0.18f, 0.42f, carbonColor)
        } else if (isSUV) {
            addBox(data, -halfW * 0.70f, roofH + 0.08f, roofZ, 0.05f, 0.08f, roofL * 0.90f, chromeColor)
            addBox(data, halfW * 0.70f, roofH + 0.08f, roofZ, 0.05f, 0.08f, roofL * 0.90f, chromeColor)
        }

        // 8. Side Aerodynamic Mirrors
        addBox(data, -halfW * 1.05f, waistH + 0.18f, 0.55f, 0.18f, 0.12f, 0.12f, bodyColor)
        addBox(data, halfW * 1.05f, waistH + 0.18f, 0.55f, 0.18f, 0.12f, 0.12f, bodyColor)

        // Upload to GPU buffer
        bodyBuffer = createBuffer(data)
        bodyVertexCount = data.size / 10
    }

    private fun buildGlassMesh(vehicleId: String) {
        val data = ArrayList<Float>()
        val glassColor = floatArrayOf(0.12f, 0.18f, 0.24f, 0.65f)
        val isSupercar = vehicleId == "supercar_apex"
        val isSUV = vehicleId == "suv_summit"

        val roofH = if (isSupercar) 1.08f else if (isSUV) 1.76f else 1.32f
        val waistH = if (isSupercar) 0.58f else if (isSUV) 0.95f else 0.72f
        val halfW = 0.82f

        // Front Windshield (slanted quad)
        val wZ1 = 0.50f
        val wZ2 = 1.18f
        addQuad(data,
            -halfW, roofH, wZ1,
            halfW, roofH, wZ1,
            halfW * 1.06f, waistH, wZ2,
            -halfW * 1.06f, waistH, wZ2,
            0f, 0.7f, 0.7f, glassColor
        )

        // Rear Windshield / Engine Bay Hatch
        val rZ1 = -0.72f
        val rZ2 = -1.38f
        addQuad(data,
            halfW, roofH, rZ1,
            -halfW, roofH, rZ1,
            -halfW * 1.06f, waistH, rZ2,
            halfW * 1.06f, waistH, rZ2,
            0f, 0.7f, -0.7f, glassColor
        )

        // Side windows
        addQuad(data,
            -halfW * 1.02f, waistH, wZ1,
            -halfW * 1.02f, waistH, rZ1,
            -halfW * 0.96f, roofH, rZ1,
            -halfW * 0.96f, roofH, wZ1,
            -1f, 0f, 0f, glassColor
        )
        addQuad(data,
            halfW * 1.02f, waistH, rZ1,
            halfW * 1.02f, waistH, wZ1,
            halfW * 0.96f, roofH, wZ1,
            halfW * 0.96f, roofH, rZ1,
            1f, 0f, 0f, glassColor
        )

        glassBuffer = createBuffer(data)
        glassVertexCount = data.size / 10
    }

    private fun buildInteriorMesh(trimRgb: FloatArray) {
        val data = ArrayList<Float>()
        val leatherColor = floatArrayOf(trimRgb[0], trimRgb[1], trimRgb[2], 1.0f)
        val dashColor = floatArrayOf(0.08f, 0.08f, 0.10f, 1.0f)
        val screenColor = floatArrayOf(0.10f, 0.50f, 0.95f, 1.0f)

        // Dashboard & Center console
        addBox(data, 0f, 0.72f, 0.62f, 1.48f, 0.28f, 0.42f, dashColor)
        addBox(data, 0f, 0.50f, 0.15f, 0.32f, 0.30f, 0.72f, dashColor)
        // Center digital infotainment display
        addBox(data, 0f, 0.76f, 0.60f, 0.28f, 0.16f, 0.05f, screenColor)

        // Driver Sport Bucket Seat (Left)
        addBox(data, -0.42f, 0.42f, -0.05f, 0.44f, 0.18f, 0.48f, leatherColor)
        addBox(data, -0.42f, 0.72f, -0.28f, 0.40f, 0.48f, 0.14f, leatherColor)
        addBox(data, -0.42f, 1.00f, -0.28f, 0.24f, 0.15f, 0.12f, leatherColor)

        // Passenger Sport Bucket Seat (Right)
        addBox(data, 0.42f, 0.42f, -0.05f, 0.44f, 0.18f, 0.48f, leatherColor)
        addBox(data, 0.42f, 0.72f, -0.28f, 0.40f, 0.48f, 0.14f, leatherColor)
        addBox(data, 0.42f, 1.00f, -0.28f, 0.24f, 0.15f, 0.12f, leatherColor)

        interiorBuffer = createBuffer(data)
        interiorVertexCount = data.size / 10
    }

    private fun buildSteeringWheelMesh() {
        val data = ArrayList<Float>()
        val wheelBlack = floatArrayOf(0.14f, 0.14f, 0.16f, 1.0f)
        val metalTrim = floatArrayOf(0.80f, 0.82f, 0.85f, 1.0f)

        val segments = 12
        val radius = 0.18f
        for (i in 0 until segments) {
            val a1 = (i.toFloat() / segments) * 2f * PI.toFloat()
            val a2 = ((i + 1).toFloat() / segments) * 2f * PI.toFloat()
            val x1 = cos(a1) * radius
            val y1 = sin(a1) * radius
            val x2 = cos(a2) * radius
            val y2 = sin(a2) * radius
            addBox(data, (x1 + x2) * 0.5f, (y1 + y2) * 0.5f, 0f, 0.035f, 0.035f, 0.035f, wheelBlack)
        }
        addBox(data, 0f, 0f, 0f, 0.09f, 0.09f, 0.04f, metalTrim)
        addBox(data, 0f, 0f, 0f, radius * 1.8f, 0.03f, 0.02f, metalTrim)

        steeringWheelBuffer = createBuffer(data)
        steeringWheelVertexCount = data.size / 10
    }

    private fun buildWheelMesh(style: Int) {
        val data = ArrayList<Float>()
        val tireColor = floatArrayOf(0.09f, 0.09f, 0.09f, 1.0f)
        val rimColor = when (style) {
            1 -> floatArrayOf(0.35f, 0.35f, 0.38f, 1.0f) // Gunmetal
            2 -> floatArrayOf(0.85f, 0.72f, 0.38f, 1.0f) // Bronze / Gold
            3 -> floatArrayOf(0.12f, 0.12f, 0.14f, 1.0f) // Gloss Black
            else -> floatArrayOf(0.88f, 0.90f, 0.92f, 1.0f) // Silver Chrome
        }
        val brakeCaliperColor = floatArrayOf(0.92f, 0.10f, 0.10f, 1.0f) // Red Brembo

        val outerR = 0.36f
        val innerR = 0.23f
        val width = 0.25f
        val segments = 16

        // 1. Rubber tire tread
        for (i in 0 until segments) {
            val a1 = (i.toFloat() / segments) * 2f * PI.toFloat()
            val a2 = ((i + 1).toFloat() / segments) * 2f * PI.toFloat()
            val y1 = cos(a1) * outerR
            val z1 = sin(a1) * outerR
            val y2 = cos(a2) * outerR
            val z2 = sin(a2) * outerR

            addQuad(data,
                -width * 0.5f, y1, z1,
                width * 0.5f, y1, z1,
                width * 0.5f, y2, z2,
                -width * 0.5f, y2, z2,
                0f, (y1 + y2) * 0.5f, (z1 + z2) * 0.5f, tireColor
            )
        }

        // 2. Alloy Rim Disc & Spokes
        addBox(data, 0.08f, 0f, 0f, 0.06f, innerR * 1.85f, innerR * 1.85f, rimColor)
        // Red brake caliper
        addBox(data, -0.02f, 0.12f, 0f, 0.08f, 0.12f, 0.14f, brakeCaliperColor)

        wheelBuffer = createBuffer(data)
        wheelVertexCount = data.size / 10
    }

    private fun buildDoorMesh(paintRgb: FloatArray) {
        val data = ArrayList<Float>()
        val bodyColor = floatArrayOf(paintRgb[0], paintRgb[1], paintRgb[2], 1.0f)
        val handleColor = floatArrayOf(0.1f, 0.1f, 0.1f, 1.0f)

        // Left car door panel
        addBox(data, 0f, 0.32f, -0.55f, 0.06f, 0.65f, 1.1f, bodyColor)
        // Door handle
        addBox(data, -0.04f, 0.55f, -0.95f, 0.04f, 0.06f, 0.15f, handleColor)

        doorLeftBuffer = createBuffer(data)
        doorLeftVertexCount = data.size / 10
    }

    private fun buildNitroFlameMesh() {
        val data = ArrayList<Float>()
        val cyanColor = floatArrayOf(0.0f, 0.85f, 1.0f, 0.90f)
        val whiteCore = floatArrayOf(0.85f, 0.95f, 1.0f, 1.0f)

        // Flame plume shooting from exhaust
        addBox(data, 0f, 0f, -0.25f, 0.08f, 0.08f, 0.50f, cyanColor)
        addBox(data, 0f, 0f, -0.15f, 0.04f, 0.04f, 0.30f, whiteCore)

        nitroFlameBuffer = createBuffer(data)
        nitroFlameVertexCount = data.size / 10
    }

    fun draw(
        shaderProgram: Int,
        viewProjMatrix: FloatArray,
        carModelMatrix: FloatArray,
        steerAngleDeg: Float,
        wheelSpinDeg: Float,
        doorOpenAngleDeg: Float,
        headlightsOn: Boolean,
        highBeam: Boolean,
        isBraking: Boolean,
        isReversing: Boolean,
        isBlinkerLeft: Boolean,
        isBlinkerRight: Boolean,
        blinkerBlink: Boolean,
        isNitro: Boolean,
        uMVPMatrixLoc: Int,
        uModelMatrixLoc: Int
    ) {
        if (bodyBuffer == null) return

        // 1. Draw Body
        Matrix3D.multiply(carMVP, viewProjMatrix, carModelMatrix)
        GLES20.glUniformMatrix4fv(uMVPMatrixLoc, 1, false, carMVP, 0)
        GLES20.glUniformMatrix4fv(uModelMatrixLoc, 1, false, carModelMatrix, 0)
        drawBuffer(shaderProgram, bodyBuffer!!, bodyVertexCount)

        // 2. Draw Interior
        interiorBuffer?.let { drawBuffer(shaderProgram, it, interiorVertexCount) }

        // 3. Draw Steering Wheel
        steeringWheelBuffer?.let { buffer ->
            System.arraycopy(carModelMatrix, 0, partModel, 0, 16)
            Matrix3D.translate(partModel, -0.42f, 0.80f, 0.48f)
            Matrix3D.rotate(partModel, -25f, 1f, 0f, 0f)
            Matrix3D.rotate(partModel, steerAngleDeg * 1.8f, 0f, 0f, 1f)
            Matrix3D.multiply(partMVP, viewProjMatrix, partModel)
            GLES20.glUniformMatrix4fv(uMVPMatrixLoc, 1, false, partMVP, 0)
            GLES20.glUniformMatrix4fv(uModelMatrixLoc, 1, false, partModel, 0)
            drawBuffer(shaderProgram, buffer, steeringWheelVertexCount)
        }

        // 4. Draw Animated Driver Door
        doorLeftBuffer?.let { buffer ->
            System.arraycopy(carModelMatrix, 0, partModel, 0, 16)
            val halfW = 0.98f
            Matrix3D.translate(partModel, -halfW, 0.4f, 0.65f)
            Matrix3D.rotate(partModel, -doorOpenAngleDeg, 0f, 1f, 0f)
            Matrix3D.multiply(partMVP, viewProjMatrix, partModel)
            GLES20.glUniformMatrix4fv(uMVPMatrixLoc, 1, false, partMVP, 0)
            GLES20.glUniformMatrix4fv(uModelMatrixLoc, 1, false, partModel, 0)
            drawBuffer(shaderProgram, buffer, doorLeftVertexCount)
        }

        // 5. Draw 4 Wheels
        wheelBuffer?.let { buffer ->
            val trackW = 0.98f
            val wheelBase = 1.48f
            val wheelY = 0.36f

            // Front Left (steers and spins)
            drawSingleWheel(shaderProgram, viewProjMatrix, carModelMatrix, -trackW, wheelY, wheelBase, steerAngleDeg, wheelSpinDeg, buffer, uMVPMatrixLoc, uModelMatrixLoc)
            // Front Right
            drawSingleWheel(shaderProgram, viewProjMatrix, carModelMatrix, trackW, wheelY, wheelBase, steerAngleDeg, wheelSpinDeg, buffer, uMVPMatrixLoc, uModelMatrixLoc, flip = true)
            // Rear Left (spins)
            drawSingleWheel(shaderProgram, viewProjMatrix, carModelMatrix, -trackW, wheelY, -wheelBase, 0f, wheelSpinDeg, buffer, uMVPMatrixLoc, uModelMatrixLoc)
            // Rear Right
            drawSingleWheel(shaderProgram, viewProjMatrix, carModelMatrix, trackW, wheelY, -wheelBase, 0f, wheelSpinDeg, buffer, uMVPMatrixLoc, uModelMatrixLoc, flip = true)
        }

        // 6. Draw Nitro Flames if active
        if (isNitro && nitroFlameBuffer != null) {
            val rearZ = -2.42f
            drawNitroJet(shaderProgram, viewProjMatrix, carModelMatrix, -0.14f, 0.38f, rearZ, uMVPMatrixLoc, uModelMatrixLoc)
            drawNitroJet(shaderProgram, viewProjMatrix, carModelMatrix, 0.14f, 0.38f, rearZ, uMVPMatrixLoc, uModelMatrixLoc)
        }

        // 7. Draw Glass with alpha transparency
        glassBuffer?.let { buffer: FloatBuffer ->
            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            GLES20.glUniformMatrix4fv(uMVPMatrixLoc, 1, false, carMVP, 0)
            GLES20.glUniformMatrix4fv(uModelMatrixLoc, 1, false, carModelMatrix, 0)
            drawBuffer(shaderProgram, buffer, glassVertexCount)
            GLES20.glDisable(GLES20.GL_BLEND)
        }
    }

    private fun drawSingleWheel(
        program: Int,
        viewProjMatrix: FloatArray,
        carModelMatrix: FloatArray,
        tx: Float, ty: Float, tz: Float,
        steerDeg: Float, spinDeg: Float,
        buffer: FloatBuffer,
        mvpLoc: Int, modelLoc: Int,
        flip: Boolean = false
    ) {
        System.arraycopy(carModelMatrix, 0, partModel, 0, 16)
        Matrix3D.translate(partModel, tx, ty, tz)
        Matrix3D.rotate(partModel, steerDeg, 0f, 1f, 0f)
        Matrix3D.rotate(partModel, spinDeg, 1f, 0f, 0f)
        if (flip) Matrix3D.rotate(partModel, 180f, 0f, 1f, 0f)

        Matrix3D.multiply(partMVP, viewProjMatrix, partModel)
        GLES20.glUniformMatrix4fv(mvpLoc, 1, false, partMVP, 0)
        GLES20.glUniformMatrix4fv(modelLoc, 1, false, partModel, 0)
        drawBuffer(program, buffer, wheelVertexCount)
    }

    private fun drawNitroJet(
        program: Int,
        viewProjMatrix: FloatArray,
        carModelMatrix: FloatArray,
        tx: Float, ty: Float, tz: Float,
        mvpLoc: Int, modelLoc: Int
    ) {
        System.arraycopy(carModelMatrix, 0, partModel, 0, 16)
        Matrix3D.translate(partModel, tx, ty, tz)
        // Pulsing flame scale
        val flicker = 0.85f + (Math.random().toFloat() * 0.35f)
        Matrix3D.scale(partModel, flicker, flicker, flicker * 1.5f)

        Matrix3D.multiply(partMVP, viewProjMatrix, partModel)
        GLES20.glUniformMatrix4fv(mvpLoc, 1, false, partMVP, 0)
        GLES20.glUniformMatrix4fv(modelLoc, 1, false, partModel, 0)
        drawBuffer(program, nitroFlameBuffer!!, nitroFlameVertexCount)
    }

    private fun drawBuffer(program: Int, buffer: FloatBuffer, vertexCount: Int) {
        val stride = 10 * 4
        buffer.position(0)
        val posLoc = GLES20.glGetAttribLocation(program, "a_Position")
        if (posLoc >= 0) {
            GLES20.glEnableVertexAttribArray(posLoc)
            GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, stride, buffer)
        }

        buffer.position(3)
        val normLoc = GLES20.glGetAttribLocation(program, "a_Normal")
        if (normLoc >= 0) {
            GLES20.glEnableVertexAttribArray(normLoc)
            GLES20.glVertexAttribPointer(normLoc, 3, GLES20.GL_FLOAT, false, stride, buffer)
        }

        buffer.position(6)
        val colLoc = GLES20.glGetAttribLocation(program, "a_Color")
        if (colLoc >= 0) {
            GLES20.glEnableVertexAttribArray(colLoc)
            GLES20.glVertexAttribPointer(colLoc, 4, GLES20.GL_FLOAT, false, stride, buffer)
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)

        if (posLoc >= 0) GLES20.glDisableVertexAttribArray(posLoc)
        if (normLoc >= 0) GLES20.glDisableVertexAttribArray(normLoc)
        if (colLoc >= 0) GLES20.glDisableVertexAttribArray(colLoc)
    }

    private fun addBox(data: ArrayList<Float>, cx: Float, cy: Float, cz: Float,
                       sx: Float, sy: Float, sz: Float, c: FloatArray) {
        val hx = sx * 0.5f
        val hy = sy * 0.5f
        val hz = sz * 0.5f

        // Front (+Z)
        addQuad(data, cx-hx, cy-hy, cz+hz,  cx+hx, cy-hy, cz+hz,  cx+hx, cy+hy, cz+hz,  cx-hx, cy+hy, cz+hz,  0f, 0f, 1f, c)
        // Back (-Z)
        addQuad(data, cx+hx, cy-hy, cz-hz,  cx-hx, cy-hy, cz-hz,  cx-hx, cy+hy, cz-hz,  cx+hx, cy+hy, cz-hz,  0f, 0f, -1f, c)
        // Top (+Y)
        addQuad(data, cx-hx, cy+hy, cz+hz,  cx+hx, cy+hy, cz+hz,  cx+hx, cy+hy, cz-hz,  cx-hx, cy+hy, cz-hz,  0f, 1f, 0f, c)
        // Bottom (-Y)
        addQuad(data, cx-hx, cy-hy, cz-hz,  cx+hx, cy-hy, cz-hz,  cx+hx, cy-hy, cz+hz,  cx-hx, cy-hy, cz+hz,  0f, -1f, 0f, c)
        // Right (+X)
        addQuad(data, cx+hx, cy-hy, cz+hz,  cx+hx, cy-hy, cz-hz,  cx+hx, cy+hy, cz-hz,  cx+hx, cy+hy, cz+hz,  1f, 0f, 0f, c)
        // Left (-X)
        addQuad(data, cx-hx, cy-hy, cz-hz,  cx-hx, cy-hy, cz+hz,  cx-hx, cy+hy, cz+hz,  cx-hx, cy+hy, cz-hz,  -1f, 0f, 0f, c)
    }

    private fun addQuad(data: ArrayList<Float>,
                        x1: Float, y1: Float, z1: Float,
                        x2: Float, y2: Float, z2: Float,
                        x3: Float, y3: Float, z3: Float,
                        x4: Float, y4: Float, z4: Float,
                        nx: Float, ny: Float, nz: Float, c: FloatArray) {
        addVert(data, x1, y1, z1, nx, ny, nz, c)
        addVert(data, x2, y2, z2, nx, ny, nz, c)
        addVert(data, x3, y3, z3, nx, ny, nz, c)

        addVert(data, x1, y1, z1, nx, ny, nz, c)
        addVert(data, x3, y3, z3, nx, ny, nz, c)
        addVert(data, x4, y4, z4, nx, ny, nz, c)
    }

    private fun addVert(data: ArrayList<Float>, x: Float, y: Float, z: Float,
                        nx: Float, ny: Float, nz: Float, c: FloatArray) {
        data.add(x); data.add(y); data.add(z)
        data.add(nx); data.add(ny); data.add(nz)
        data.add(c[0]); data.add(c[1]); data.add(c[2]); data.add(c[3])
    }

    private fun createBuffer(data: ArrayList<Float>): FloatBuffer {
        val array = data.toFloatArray()
        return ByteBuffer.allocateDirect(array.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(array)
            .apply { position(0) }
    }
}
