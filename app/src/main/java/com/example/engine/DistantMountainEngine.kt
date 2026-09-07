package com.example.engine

import android.opengl.GLES20
import com.example.renderer.Matrix3D
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Procedural distant alpine mountain range renderer.
 * Encircles the entire 8km world with majestic 3D mountain peaks, craggy ridges,
 * and snow caps at the horizon so the environment feels grand and never empty.
 */
class DistantMountainEngine {

    private var vertexBuffer: FloatBuffer? = null
    private var vertexCount = 0
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    init {
        buildMountainRing()
    }

    private fun buildMountainRing() {
        val segments = 48
        val innerRadius = 380f
        val outerRadius = 650f
        val vertexData = ArrayList<Float>()

        // Colors: Rocky dark alpine slate, pine green lower slopes, and glistening white/ice snow caps
        val snowColor = floatArrayOf(0.96f, 0.97f, 1.0f, 1.0f)
        val rockColor = floatArrayOf(0.36f, 0.38f, 0.42f, 1.0f)
        val lowerSlopeColor = floatArrayOf(0.22f, 0.30f, 0.20f, 1.0f)

        for (i in 0 until segments) {
            val a0 = (i.toFloat() / segments) * 2f * PI.toFloat()
            val a1 = ((i + 1).toFloat() / segments) * 2f * PI.toFloat()

            // Procedural peak height using harmonics
            val h0 = 120f + sin(a0 * 3f) * 65f + cos(a0 * 6f) * 45f + sin(a0 * 11f) * 25f
            val h1 = 120f + sin(a1 * 3f) * 65f + cos(a1 * 6f) * 45f + sin(a1 * 11f) * 25f

            // Base coordinates (lower slope)
            val bx0 = cos(a0) * innerRadius
            val bz0 = sin(a0) * innerRadius
            val by0 = -30f

            val bx1 = cos(a1) * innerRadius
            val bz1 = sin(a1) * innerRadius
            val by1 = -30f

            // Peak coordinates (outer ridge)
            val px0 = cos(a0) * outerRadius
            val pz0 = sin(a0) * outerRadius
            val py0 = h0

            val px1 = cos(a1) * outerRadius
            val pz1 = sin(a1) * outerRadius
            val py1 = h1

            // Mid-mountain snow line (~55% height)
            val mx0 = bx0 + (px0 - bx0) * 0.55f
            val mz0 = bz0 + (pz0 - bz0) * 0.55f
            val my0 = by0 + (py0 - by0) * 0.55f

            val mx1 = bx1 + (px1 - bx1) * 0.55f
            val mz1 = bz1 + (pz1 - bz1) * 0.55f
            val my1 = by1 + (py1 - by1) * 0.55f

            // Inward-facing slope normal
            val midA = (a0 + a1) * 0.5f
            val nx = -cos(midA) * 0.7f
            val ny = 0.7f
            val nz = -sin(midA) * 0.7f

            // 1. Lower Slope Quad (2 triangles: base to mid)
            addVertex(vertexData, bx0, by0, bz0, nx, ny, nz, lowerSlopeColor)
            addVertex(vertexData, bx1, by1, bz1, nx, ny, nz, lowerSlopeColor)
            addVertex(vertexData, mx0, my0, mz0, nx, ny, nz, rockColor)

            addVertex(vertexData, bx1, by1, bz1, nx, ny, nz, lowerSlopeColor)
            addVertex(vertexData, mx1, my1, mz1, nx, ny, nz, rockColor)
            addVertex(vertexData, mx0, my0, mz0, nx, ny, nz, rockColor)

            // 2. Upper Snow Peak Quad (2 triangles: mid to snow peak)
            addVertex(vertexData, mx0, my0, mz0, nx, ny, nz, rockColor)
            addVertex(vertexData, mx1, my1, mz1, nx, ny, nz, rockColor)
            addVertex(vertexData, px0, py0, pz0, nx, ny, nz, snowColor)

            addVertex(vertexData, mx1, my1, mz1, nx, ny, nz, rockColor)
            addVertex(vertexData, px1, py1, pz1, nx, ny, nz, snowColor)
            addVertex(vertexData, px0, py0, pz0, nx, ny, nz, snowColor)
        }

        val floatArray = vertexData.toFloatArray()
        vertexBuffer = ByteBuffer.allocateDirect(floatArray.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(floatArray)
        vertexBuffer?.position(0)
        vertexCount = floatArray.size / 10 // 3 pos + 3 norm + 4 col
    }

    private fun addVertex(
        data: ArrayList<Float>,
        x: Float, y: Float, z: Float,
        nx: Float, ny: Float, nz: Float,
        color: FloatArray
    ) {
        data.add(x); data.add(y); data.add(z)
        data.add(nx); data.add(ny); data.add(nz)
        data.add(color[0]); data.add(color[1]); data.add(color[2]); data.add(color[3])
    }

    fun draw(
        program: Int,
        viewProjMatrix: FloatArray,
        camX: Float, camY: Float, camZ: Float,
        uMVPLoc: Int,
        uModelLoc: Int
    ) {
        val buf = vertexBuffer ?: return
        if (vertexCount == 0 || program == 0) return

        // Mountain ring centers on camera with subtle elevation dampening for realistic parallax
        Matrix3D.setIdentity(modelMatrix)
        Matrix3D.translate(modelMatrix, camX, camY * 0.25f, camZ)
        Matrix3D.multiply(mvpMatrix, viewProjMatrix, modelMatrix)

        GLES20.glUniformMatrix4fv(uMVPLoc, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uModelLoc, 1, false, modelMatrix, 0)

        val stride = 10 * 4
        val aPosLoc = GLES20.glGetAttribLocation(program, "a_Position")
        val aNormLoc = GLES20.glGetAttribLocation(program, "a_Normal")
        val aColLoc = GLES20.glGetAttribLocation(program, "a_Color")

        buf.position(0)
        GLES20.glEnableVertexAttribArray(aPosLoc)
        GLES20.glVertexAttribPointer(aPosLoc, 3, GLES20.GL_FLOAT, false, stride, buf)

        buf.position(3)
        GLES20.glEnableVertexAttribArray(aNormLoc)
        GLES20.glVertexAttribPointer(aNormLoc, 3, GLES20.GL_FLOAT, false, stride, buf)

        buf.position(6)
        GLES20.glEnableVertexAttribArray(aColLoc)
        GLES20.glVertexAttribPointer(aColLoc, 4, GLES20.GL_FLOAT, false, stride, buf)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)

        GLES20.glDisableVertexAttribArray(aPosLoc)
        GLES20.glDisableVertexAttribArray(aNormLoc)
        GLES20.glDisableVertexAttribArray(aColLoc)
    }
}
