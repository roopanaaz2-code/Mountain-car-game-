package com.example.engine

import android.opengl.GLES20
import com.example.renderer.Shaders
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * High-performance, zero-allocation real-time particle engine for mobile OpenGL ES.
 * Handles nitro exhaust jet flames, tire drift smoke, and collision sparks.
 */
class ParticleEngine(private val maxParticles: Int = 400) {

    private val posX = FloatArray(maxParticles)
    private val posY = FloatArray(maxParticles)
    private val posZ = FloatArray(maxParticles)

    private val velX = FloatArray(maxParticles)
    private val velY = FloatArray(maxParticles)
    private val velZ = FloatArray(maxParticles)

    private val colR = FloatArray(maxParticles)
    private val colG = FloatArray(maxParticles)
    private val colB = FloatArray(maxParticles)
    private val colA = FloatArray(maxParticles)

    private val life = FloatArray(maxParticles)
    private val maxLife = FloatArray(maxParticles)
    private val pointSize = FloatArray(maxParticles)

    private var activeCount = 0

    // Vertex data: 3 pos + 4 col + 1 size = 8 floats per particle
    private val vertexData = FloatArray(maxParticles * 8)
    private val vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(maxParticles * 8 * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()

    private var program = 0
    private var uMVPMatrixLoc = -1
    private var aPositionLoc = -1
    private var aColorLoc = -1
    private var aSizeLoc = -1

    private val random = Random(42)

    init {
        initShader()
    }

    private fun initShader() {
        val vShader = """
            uniform mat4 u_MVPMatrix;
            attribute vec4 a_Position;
            attribute vec4 a_Color;
            attribute float a_PointSize;
            varying vec4 v_Color;
            void main() {
                gl_Position = u_MVPMatrix * a_Position;
                gl_PointSize = clamp(a_PointSize * (300.0 / gl_Position.w), 2.0, 64.0);
                v_Color = a_Color;
            }
        """.trimIndent()

        val fShader = """
            precision mediump float;
            varying vec4 v_Color;
            void main() {
                // Circular soft particle
                vec2 coord = gl_PointCoord - vec2(0.5);
                float distSq = dot(coord, coord);
                if (distSq > 0.25) discard;
                float alpha = v_Color.a * (1.0 - distSq * 4.0);
                gl_FragColor = vec4(v_Color.rgb, alpha);
            }
        """.trimIndent()

        program = Shaders.buildProgram(vShader, fShader)
        uMVPMatrixLoc = GLES20.glGetUniformLocation(program, "u_MVPMatrix")
        aPositionLoc = GLES20.glGetAttribLocation(program, "a_Position")
        aColorLoc = GLES20.glGetAttribLocation(program, "a_Color")
        aSizeLoc = GLES20.glGetAttribLocation(program, "a_PointSize")
    }

    fun emitNitroFlame(
        originX: Float, originY: Float, originZ: Float,
        forwardX: Float, forwardZ: Float, count: Int = 4
    ) {
        for (i in 0 until count) {
            if (activeCount >= maxParticles) break
            val idx = activeCount++
            posX[idx] = originX + (random.nextFloat() - 0.5f) * 0.12f
            posY[idx] = originY + (random.nextFloat() - 0.5f) * 0.08f
            posZ[idx] = originZ + (random.nextFloat() - 0.5f) * 0.12f

            // High-speed reverse jet
            val speed = 9.0f + random.nextFloat() * 7.0f
            val spread = 0.8f
            velX[idx] = -forwardX * speed + (random.nextFloat() - 0.5f) * spread
            velY[idx] = (random.nextFloat() - 0.2f) * 0.8f
            velZ[idx] = -forwardZ * speed + (random.nextFloat() - 0.5f) * spread

            // Electric Cyan to intense Blue-White
            colR[idx] = 0.1f + random.nextFloat() * 0.4f
            colG[idx] = 0.85f + random.nextFloat() * 0.15f
            colB[idx] = 1.0f
            colA[idx] = 0.95f

            life[idx] = 0.12f + random.nextFloat() * 0.10f
            maxLife[idx] = life[idx]
            pointSize[idx] = 22.0f + random.nextFloat() * 14.0f
        }
    }

    fun emitTireSmoke(
        originX: Float, originY: Float, originZ: Float, count: Int = 2
    ) {
        for (i in 0 until count) {
            if (activeCount >= maxParticles) break
            val idx = activeCount++
            posX[idx] = originX + (random.nextFloat() - 0.5f) * 0.25f
            posY[idx] = originY + 0.1f + random.nextFloat() * 0.1f
            posZ[idx] = originZ + (random.nextFloat() - 0.5f) * 0.25f

            velX[idx] = (random.nextFloat() - 0.5f) * 1.5f
            velY[idx] = 0.8f + random.nextFloat() * 1.2f // gently rises
            velZ[idx] = (random.nextFloat() - 0.5f) * 1.5f

            // Billowing white smoke
            val brightness = 0.82f + random.nextFloat() * 0.15f
            colR[idx] = brightness
            colG[idx] = brightness
            colB[idx] = brightness
            colA[idx] = 0.55f

            life[idx] = 0.6f + random.nextFloat() * 0.5f
            maxLife[idx] = life[idx]
            pointSize[idx] = 28.0f + random.nextFloat() * 20.0f
        }
    }

    fun emitSparks(
        originX: Float, originY: Float, originZ: Float, count: Int = 5
    ) {
        for (i in 0 until count) {
            if (activeCount >= maxParticles) break
            val idx = activeCount++
            posX[idx] = originX
            posY[idx] = originY
            posZ[idx] = originZ

            velX[idx] = (random.nextFloat() - 0.5f) * 6.0f
            velY[idx] = 2.0f + random.nextFloat() * 4.0f
            velZ[idx] = (random.nextFloat() - 0.5f) * 6.0f

            colR[idx] = 1.0f
            colG[idx] = 0.65f + random.nextFloat() * 0.35f
            colB[idx] = 0.1f
            colA[idx] = 1.0f

            life[idx] = 0.25f + random.nextFloat() * 0.2f
            maxLife[idx] = life[idx]
            pointSize[idx] = 8.0f
        }
    }

    fun update(dtSec: Float) {
        val dt = dtSec.coerceIn(0.001f, 0.05f)
        var i = 0
        while (i < activeCount) {
            life[i] -= dt
            if (life[i] <= 0f) {
                // Swap with last active particle
                val last = activeCount - 1
                if (i != last) {
                    posX[i] = posX[last]
                    posY[i] = posY[last]
                    posZ[i] = posZ[last]
                    velX[i] = velX[last]
                    velY[i] = velY[last]
                    velZ[i] = velZ[last]
                    colR[i] = colR[last]
                    colG[i] = colG[last]
                    colB[i] = colB[last]
                    colA[i] = colA[last]
                    life[i] = life[last]
                    maxLife[i] = maxLife[last]
                    pointSize[i] = pointSize[last]
                }
                activeCount--
                continue
            }

            // Physics integration
            posX[i] += velX[i] * dt
            posY[i] += velY[i] * dt
            posZ[i] += velZ[i] * dt

            // Alpha fade over lifetime
            val progress = 1.0f - (life[i] / maxLife[i])
            val initialA = if (colR[i] > 0.9f && colB[i] < 0.3f) 1.0f else 0.55f
            colA[i] = (1.0f - progress) * initialA

            i++
        }
    }

    fun draw(viewProjMatrix: FloatArray) {
        if (activeCount <= 0 || program == 0) return

        // Pack vertex data: 8 floats per particle
        var vIdx = 0
        for (i in 0 until activeCount) {
            vertexData[vIdx++] = posX[i]
            vertexData[vIdx++] = posY[i]
            vertexData[vIdx++] = posZ[i]

            vertexData[vIdx++] = colR[i]
            vertexData[vIdx++] = colG[i]
            vertexData[vIdx++] = colB[i]
            vertexData[vIdx++] = colA[i]

            vertexData[vIdx++] = pointSize[i]
        }

        vertexBuffer.position(0)
        vertexBuffer.put(vertexData, 0, activeCount * 8)
        vertexBuffer.position(0)

        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(uMVPMatrixLoc, 1, false, viewProjMatrix, 0)

        // Enable additive alpha blending for glowing flames and soft smoke
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE)
        GLES20.glDepthMask(false) // Don't write to depth buffer to avoid sorting artifacts

        val stride = 8 * 4 // 8 floats * 4 bytes
        vertexBuffer.position(0)
        GLES20.glEnableVertexAttribArray(aPositionLoc)
        GLES20.glVertexAttribPointer(aPositionLoc, 3, GLES20.GL_FLOAT, false, stride, vertexBuffer)

        vertexBuffer.position(3)
        GLES20.glEnableVertexAttribArray(aColorLoc)
        GLES20.glVertexAttribPointer(aColorLoc, 4, GLES20.GL_FLOAT, false, stride, vertexBuffer)

        vertexBuffer.position(7)
        GLES20.glEnableVertexAttribArray(aSizeLoc)
        GLES20.glVertexAttribPointer(aSizeLoc, 1, GLES20.GL_FLOAT, false, stride, vertexBuffer)

        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, activeCount)

        GLES20.glDisableVertexAttribArray(aPositionLoc)
        GLES20.glDisableVertexAttribArray(aColorLoc)
        GLES20.glDisableVertexAttribArray(aSizeLoc)

        // Restore OpenGL depth write and standard alpha blending
        GLES20.glDepthMask(true)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
    }
}
