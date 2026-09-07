package com.example.renderer

import android.opengl.GLES20
import com.example.model.GraphicsQuality
import com.example.model.TimeOfDay
import com.example.model.WeatherType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class LightingState(
    val sunDirection: FloatArray,
    val sunColor: FloatArray,
    val ambientColor: FloatArray,
    val fogColor: FloatArray,
    val fogStart: Float,
    val fogEnd: Float,
    val wetness: Float,
    val zenithColor: FloatArray,
    val horizonColor: FloatArray,
    val sunDiscColor: FloatArray,
    val moonDirection: FloatArray
)

class EnvironmentSystem {
    var currentTimeHours: Float = 14.0f // 2:00 PM default
    var isDynamicCycle: Boolean = true
    var timeSpeedMultiplier: Float = 60.0f // 1 real sec = 1 game min

    var currentWeather: WeatherType = WeatherType.CLEAR
    var windPhase: Float = 0f

    private var skyDomeBuffer: FloatBuffer? = null
    private var skyDomeVertexCount = 0

    private var treeBuffer: FloatBuffer? = null
    private var treeVertexCount = 0

    private var rainBuffer: FloatBuffer? = null
    private val rainParticleCount = 600
    private val rainPositions = FloatArray(rainParticleCount * 3)

    private val skyMatrix = FloatArray(16)
    private val treeMatrix = FloatArray(16)

    init {
        buildSkyDome()
        buildPineTreeMesh()
        initRainParticles()
    }

    fun update(deltaTimeSec: Float, timeSetting: TimeOfDay, weather: WeatherType) {
        currentWeather = weather

        if (timeSetting == TimeOfDay.DYNAMIC) {
            isDynamicCycle = true
            currentTimeHours = (currentTimeHours + (deltaTimeSec * timeSpeedMultiplier) / 3600f) % 24f
        } else {
            isDynamicCycle = false
            currentTimeHours = timeSetting.hour
        }

        windPhase = (windPhase + deltaTimeSec * 2.5f) % (2f * PI.toFloat())
    }

    fun computeLighting(quality: GraphicsQuality): LightingState {
        val h = currentTimeHours
        // Sun angle: rises at 6h (angle 0), peaks at 12h (angle pi/2), sets at 18h (angle pi)
        val sunElevation = sin(((h - 6f) / 12f) * PI.toFloat())
        val sunAzimuth = cos(((h - 6f) / 12f) * PI.toFloat())

        val sunDir = floatArrayOf(
            sunAzimuth * 0.75f,
            sunElevation.coerceIn(-0.2f, 1.0f),
            0.55f
        )
        // Normalize sun dir
        val sLen = kotlin.math.sqrt(sunDir[0]*sunDir[0] + sunDir[1]*sunDir[1] + sunDir[2]*sunDir[2]).coerceAtLeast(0.001f)
        sunDir[0] /= sLen; sunDir[1] /= sLen; sunDir[2] /= sLen

        // Opposite moon dir
        val moonDir = floatArrayOf(-sunDir[0], (-sunDir[1]).coerceAtLeast(0.1f), -sunDir[2])
        val mLen = kotlin.math.sqrt(moonDir[0]*moonDir[0] + moonDir[1]*moonDir[1] + moonDir[2]*moonDir[2]).coerceAtLeast(0.001f)
        moonDir[0] /= mLen; moonDir[1] /= mLen; moonDir[2] /= mLen

        // Blend colors based on time of day
        val isDay = h in 6.0f..18.5f
        val isSunrise = h in 5.0f..7.5f
        val isSunset = h in 17.5f..20.0f
        val isNight = h < 5.0f || h > 20.5f

        val zenith: FloatArray
        val horizon: FloatArray
        val sunCol: FloatArray
        val ambient: FloatArray
        val fogCol: FloatArray
        val sunDisc: FloatArray

        when {
            isSunrise -> {
                zenith = floatArrayOf(0.18f, 0.28f, 0.48f)
                horizon = floatArrayOf(0.95f, 0.58f, 0.28f) // Warm golden sunrise
                sunCol = floatArrayOf(1.0f, 0.85f, 0.55f)
                ambient = floatArrayOf(0.35f, 0.35f, 0.40f)
                fogCol = floatArrayOf(0.85f, 0.65f, 0.48f)
                sunDisc = floatArrayOf(1.2f, 0.9f, 0.4f)
            }
            isSunset -> {
                zenith = floatArrayOf(0.12f, 0.15f, 0.38f)
                horizon = floatArrayOf(0.92f, 0.35f, 0.18f) // Fiery orange sunset
                sunCol = floatArrayOf(0.98f, 0.55f, 0.22f)
                ambient = floatArrayOf(0.32f, 0.28f, 0.35f)
                fogCol = floatArrayOf(0.72f, 0.42f, 0.38f)
                sunDisc = floatArrayOf(1.4f, 0.6f, 0.2f)
            }
            isNight -> {
                zenith = floatArrayOf(0.02f, 0.03f, 0.08f) // Deep starry navy
                horizon = floatArrayOf(0.06f, 0.08f, 0.14f)
                sunCol = floatArrayOf(0.12f, 0.15f, 0.22f) // Moonlight
                ambient = floatArrayOf(0.12f, 0.14f, 0.18f)
                fogCol = floatArrayOf(0.05f, 0.07f, 0.12f)
                sunDisc = floatArrayOf(0f, 0f, 0f)
            }
            else -> {
                // Crisp Mountain Daytime
                zenith = floatArrayOf(0.20f, 0.45f, 0.85f) // Alpine blue
                horizon = floatArrayOf(0.68f, 0.78f, 0.92f)
                sunCol = floatArrayOf(1.0f, 0.98f, 0.92f)
                ambient = floatArrayOf(0.42f, 0.44f, 0.46f)
                fogCol = floatArrayOf(0.65f, 0.75f, 0.88f)
                sunDisc = floatArrayOf(1.2f, 1.2f, 1.1f)
            }
        }

        // Weather adjustments
        var fogStart = quality.renderDistance * 0.45f
        var fogEnd = quality.renderDistance
        var wetness = 0.0f

        if (currentWeather == WeatherType.MIST) {
            fogStart = 40f
            fogEnd = (quality.renderDistance * 0.45f).coerceAtLeast(180f)
            fogCol[0] = 0.70f; fogCol[1] = 0.72f; fogCol[2] = 0.75f
            ambient[0] *= 0.85f; ambient[1] *= 0.85f; ambient[2] *= 0.85f
        } else if (currentWeather == WeatherType.RAIN) {
            fogStart = 60f
            fogEnd = (quality.renderDistance * 0.55f).coerceAtLeast(220f)
            fogCol[0] = 0.45f; fogCol[1] = 0.48f; fogCol[2] = 0.52f
            zenith[0] *= 0.6f; zenith[1] *= 0.6f; zenith[2] *= 0.6f
            horizon[0] *= 0.65f; horizon[1] *= 0.65f; horizon[2] *= 0.65f
            wetness = 0.85f
        }

        return LightingState(
            sunDirection = sunDir,
            sunColor = sunCol,
            ambientColor = ambient,
            fogColor = fogCol,
            fogStart = fogStart,
            fogEnd = fogEnd,
            wetness = wetness,
            zenithColor = zenith,
            horizonColor = horizon,
            sunDiscColor = sunDisc,
            moonDirection = moonDir
        )
    }

    private fun buildSkyDome() {
        val data = ArrayList<Float>()
        val radius = 800f
        val rings = 12
        val sectors = 18

        for (r in 0 until rings) {
            val phi1 = (r.toFloat() / rings) * (PI.toFloat() * 0.5f)
            val phi2 = ((r + 1).toFloat() / rings) * (PI.toFloat() * 0.5f)

            for (s in 0 until sectors) {
                val theta1 = (s.toFloat() / sectors) * (PI.toFloat() * 2f)
                val theta2 = ((s + 1).toFloat() / sectors) * (PI.toFloat() * 2f)

                val x1 = radius * cos(phi1) * cos(theta1)
                val y1 = radius * sin(phi1)
                val z1 = radius * cos(phi1) * sin(theta1)

                val x2 = radius * cos(phi1) * cos(theta2)
                val y2 = radius * sin(phi1)
                val z2 = radius * cos(phi1) * sin(theta2)

                val x3 = radius * cos(phi2) * cos(theta2)
                val y3 = radius * sin(phi2)
                val z3 = radius * cos(phi2) * sin(theta2)

                val x4 = radius * cos(phi2) * cos(theta1)
                val y4 = radius * sin(phi2)
                val z4 = radius * cos(phi2) * sin(theta1)

                // 2 Triangles per quad
                data.add(x1); data.add(y1); data.add(z1)
                data.add(x2); data.add(y2); data.add(z2)
                data.add(x3); data.add(y3); data.add(z3)

                data.add(x1); data.add(y1); data.add(z1)
                data.add(x3); data.add(y3); data.add(z3)
                data.add(x4); data.add(y4); data.add(z4)
            }
        }

        val array = data.toFloatArray()
        skyDomeBuffer = ByteBuffer.allocateDirect(array.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(array)
            .apply { position(0) }
        skyDomeVertexCount = array.size / 3
    }

    private fun buildPineTreeMesh() {
        val data = ArrayList<Float>()
        val trunkColor = floatArrayOf(0.32f, 0.22f, 0.14f, 1f)
        val foliageColor1 = floatArrayOf(0.12f, 0.35f, 0.16f, 1f)
        val foliageColor2 = floatArrayOf(0.16f, 0.42f, 0.20f, 1f)

        // Trunk (height 3m)
        addBox(data, 0f, 1.5f, 0f, 0.4f, 3.0f, 0.4f, trunkColor)

        // Tier 1 Foliage (Cone / Pyramid)
        addPyramid(data, 0f, 2.5f, 0f, 3.2f, 2.5f, foliageColor1)
        // Tier 2 Foliage
        addPyramid(data, 0f, 4.0f, 0f, 2.4f, 2.2f, foliageColor2)
        // Tier 3 Foliage (Top)
        addPyramid(data, 0f, 5.5f, 0f, 1.6f, 2.0f, foliageColor1)

        val array = data.toFloatArray()
        treeBuffer = ByteBuffer.allocateDirect(array.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(array)
            .apply { position(0) }
        treeVertexCount = array.size / 10
    }

    private fun addPyramid(data: ArrayList<Float>, cx: Float, cy: Float, cz: Float,
                           width: Float, height: Float, c: FloatArray) {
        val hw = width * 0.5f
        val topY = cy + height

        // 4 triangular sides
        // Front
        addTri(data, cx-hw, cy, cz+hw,  cx+hw, cy, cz+hw,  cx, topY, cz,  0f, 0.5f, 0.8f, c)
        // Back
        addTri(data, cx+hw, cy, cz-hw,  cx-hw, cy, cz-hw,  cx, topY, cz,  0f, 0.5f, -0.8f, c)
        // Right
        addTri(data, cx+hw, cy, cz+hw,  cx+hw, cy, cz-hw,  cx, topY, cz,  0.8f, 0.5f, 0f, c)
        // Left
        addTri(data, cx-hw, cy, cz-hw,  cx-hw, cy, cz+hw,  cx, topY, cz,  -0.8f, 0.5f, 0f, c)
    }

    private fun addTri(data: ArrayList<Float>,
                       x1: Float, y1: Float, z1: Float,
                       x2: Float, y2: Float, z2: Float,
                       x3: Float, y3: Float, z3: Float,
                       nx: Float, ny: Float, nz: Float, c: FloatArray) {
        addVert(data, x1, y1, z1, nx, ny, nz, c)
        addVert(data, x2, y2, z2, nx, ny, nz, c)
        addVert(data, x3, y3, z3, nx, ny, nz, c)
    }

    private fun addBox(data: ArrayList<Float>, cx: Float, cy: Float, cz: Float,
                       sx: Float, sy: Float, sz: Float, c: FloatArray) {
        val hx = sx * 0.5f; val hy = sy * 0.5f; val hz = sz * 0.5f
        addQuad(data, cx-hx, cy-hy, cz+hz,  cx+hx, cy-hy, cz+hz,  cx+hx, cy+hy, cz+hz,  cx-hx, cy+hy, cz+hz,  0f, 0f, 1f, c)
        addQuad(data, cx+hx, cy-hy, cz-hz,  cx-hx, cy-hy, cz-hz,  cx-hx, cy+hy, cz-hz,  cx+hx, cy+hy, cz-hz,  0f, 0f, -1f, c)
        addQuad(data, cx-hx, cy+hy, cz+hz,  cx+hx, cy+hy, cz+hz,  cx+hx, cy+hy, cz-hz,  cx-hx, cy+hy, cz-hz,  0f, 1f, 0f, c)
        addQuad(data, cx-hx, cy-hy, cz-hz,  cx+hx, cy-hy, cz-hz,  cx+hx, cy-hy, cz+hz,  cx-hx, cy-hy, cz+hz,  0f, -1f, 0f, c)
        addQuad(data, cx+hx, cy-hy, cz+hz,  cx+hx, cy-hy, cz-hz,  cx+hx, cy+hy, cz-hz,  cx+hx, cy+hy, cz+hz,  1f, 0f, 0f, c)
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

    private fun initRainParticles() {
        for (i in 0 until rainParticleCount) {
            rainPositions[i * 3 + 0] = (kotlin.random.Random.nextFloat() - 0.5f) * 80f
            rainPositions[i * 3 + 1] = kotlin.random.Random.nextFloat() * 40f
            rainPositions[i * 3 + 2] = (kotlin.random.Random.nextFloat() - 0.5f) * 80f
        }
    }

    fun drawSky(skyProgram: Int, viewProjMatrix: FloatArray, camX: Float, camY: Float, camZ: Float, lighting: LightingState) {
        val buffer = skyDomeBuffer ?: return
        GLES20.glUseProgram(skyProgram)

        System.arraycopy(viewProjMatrix, 0, skyMatrix, 0, 16)
        Matrix3D.translate(skyMatrix, camX, camY - 50f, camZ)

        val mvpLoc = GLES20.glGetUniformLocation(skyProgram, "u_MVPMatrix")
        val zenithLoc = GLES20.glGetUniformLocation(skyProgram, "u_ZenithColor")
        val horizonLoc = GLES20.glGetUniformLocation(skyProgram, "u_HorizonColor")
        val sunDirLoc = GLES20.glGetUniformLocation(skyProgram, "u_SunDir")
        val sunDiscLoc = GLES20.glGetUniformLocation(skyProgram, "u_SunDiscColor")
        val moonDirLoc = GLES20.glGetUniformLocation(skyProgram, "u_MoonDir")

        GLES20.glUniformMatrix4fv(mvpLoc, 1, false, skyMatrix, 0)
        GLES20.glUniform3fv(zenithLoc, 1, lighting.zenithColor, 0)
        GLES20.glUniform3fv(horizonLoc, 1, lighting.horizonColor, 0)
        GLES20.glUniform3fv(sunDirLoc, 1, lighting.sunDirection, 0)
        GLES20.glUniform3fv(sunDiscLoc, 1, lighting.sunDiscColor, 0)
        GLES20.glUniform3fv(moonDirLoc, 1, lighting.moonDirection, 0)

        val posLoc = GLES20.glGetAttribLocation(skyProgram, "a_Position")
        buffer.position(0)
        GLES20.glEnableVertexAttribArray(posLoc)
        GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, 3 * 4, buffer)

        // Disable depth write for sky
        GLES20.glDepthMask(false)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, skyDomeVertexCount)
        GLES20.glDepthMask(true)

        GLES20.glDisableVertexAttribArray(posLoc)
    }

    fun drawTree(
        mainProgram: Int,
        viewProjMatrix: FloatArray,
        x: Float, y: Float, z: Float,
        scale: Float,
        uMVPMatrixLoc: Int,
        uModelMatrixLoc: Int
    ) {
        val buffer = treeBuffer ?: return
        System.arraycopy(viewProjMatrix, 0, treeMatrix, 0, 16)
        Matrix3D.translate(treeMatrix, x, y, z)
        Matrix3D.scale(treeMatrix, scale, scale, scale)

        // Wind sway
        val sway = sin(windPhase + x * 0.1f + z * 0.1f) * 2.5f
        Matrix3D.rotate(treeMatrix, sway, 0f, 0f, 1f)

        GLES20.glUniformMatrix4fv(uMVPMatrixLoc, 1, false, treeMatrix, 0)
        GLES20.glUniformMatrix4fv(uModelMatrixLoc, 1, false, treeMatrix, 0)

        val stride = 10 * 4
        buffer.position(0)
        val posLoc = GLES20.glGetAttribLocation(mainProgram, "a_Position")
        if (posLoc >= 0) {
            GLES20.glEnableVertexAttribArray(posLoc)
            GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, stride, buffer)
        }
        buffer.position(3)
        val normLoc = GLES20.glGetAttribLocation(mainProgram, "a_Normal")
        if (normLoc >= 0) {
            GLES20.glEnableVertexAttribArray(normLoc)
            GLES20.glVertexAttribPointer(normLoc, 3, GLES20.GL_FLOAT, false, stride, buffer)
        }
        buffer.position(6)
        val colLoc = GLES20.glGetAttribLocation(mainProgram, "a_Color")
        if (colLoc >= 0) {
            GLES20.glEnableVertexAttribArray(colLoc)
            GLES20.glVertexAttribPointer(colLoc, 4, GLES20.GL_FLOAT, false, stride, buffer)
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, treeVertexCount)

        if (posLoc >= 0) GLES20.glDisableVertexAttribArray(posLoc)
        if (normLoc >= 0) GLES20.glDisableVertexAttribArray(normLoc)
        if (colLoc >= 0) GLES20.glDisableVertexAttribArray(colLoc)
    }

    fun drawRain(
        rainProgram: Int,
        viewProjMatrix: FloatArray,
        camX: Float, camY: Float, camZ: Float,
        deltaTimeSec: Float
    ) {
        if (currentWeather != WeatherType.RAIN) return

        val data = ArrayList<Float>()
        val rainColor = floatArrayOf(0.75f, 0.82f, 0.90f, 0.45f)
        val fallSpeed = 35.0f * deltaTimeSec

        for (i in 0 until rainParticleCount) {
            rainPositions[i * 3 + 1] -= fallSpeed
            if (rainPositions[i * 3 + 1] < 0f) {
                rainPositions[i * 3 + 1] = 35f
                rainPositions[i * 3 + 0] = (kotlin.random.Random.nextFloat() - 0.5f) * 60f
                rainPositions[i * 3 + 2] = (kotlin.random.Random.nextFloat() - 0.5f) * 60f
            }

            val px = camX + rainPositions[i * 3 + 0]
            val py = camY + rainPositions[i * 3 + 1]
            val pz = camZ + rainPositions[i * 3 + 2]

            // Line streak for rain drop
            data.add(px); data.add(py); data.add(pz)
            data.add(rainColor[0]); data.add(rainColor[1]); data.add(rainColor[2]); data.add(rainColor[3])

            data.add(px); data.add(py - 1.2f); data.add(pz)
            data.add(rainColor[0]); data.add(rainColor[1]); data.add(rainColor[2]); data.add(0.0f)
        }

        val array = data.toFloatArray()
        val buf = ByteBuffer.allocateDirect(array.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(array)
            .apply { position(0) }

        GLES20.glUseProgram(rainProgram)
        val mvpLoc = GLES20.glGetUniformLocation(rainProgram, "u_MVPMatrix")
        GLES20.glUniformMatrix4fv(mvpLoc, 1, false, viewProjMatrix, 0)

        val posLoc = GLES20.glGetAttribLocation(rainProgram, "a_Position")
        val colLoc = GLES20.glGetAttribLocation(rainProgram, "a_Color")

        buf.position(0)
        GLES20.glEnableVertexAttribArray(posLoc)
        GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, 7 * 4, buf)

        buf.position(3)
        GLES20.glEnableVertexAttribArray(colLoc)
        GLES20.glVertexAttribPointer(colLoc, 4, GLES20.GL_FLOAT, false, 7 * 4, buf)

        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        GLES20.glDrawArrays(GLES20.GL_LINES, 0, rainParticleCount * 2)

        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glDisableVertexAttribArray(posLoc)
        GLES20.glDisableVertexAttribArray(colLoc)
    }
}
