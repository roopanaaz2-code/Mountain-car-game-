package com.example.renderer

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.sin

class Character3D {
    private var maleBuffer: FloatBuffer? = null
    private var femaleBuffer: FloatBuffer? = null
    private var maleVertexCount = 0
    private var femaleVertexCount = 0

    private val charModel = FloatArray(16)
    private val charMVP = FloatArray(16)

    init {
        buildMaleMesh()
        buildFemaleMesh()
    }

    private fun buildMaleMesh() {
        val data = ArrayList<Float>()
        val skin = floatArrayOf(0.92f, 0.78f, 0.68f, 1f)
        val hair = floatArrayOf(0.25f, 0.18f, 0.12f, 1f)
        val jacket = floatArrayOf(0.12f, 0.38f, 0.68f, 1f) // Alpine Explorer Blue
        val pants = floatArrayOf(0.20f, 0.22f, 0.25f, 1f) // Dark cargo pants
        val boots = floatArrayOf(0.35f, 0.25f, 0.15f, 1f) // Hiking leather

        // Head
        addBox(data, 0f, 1.65f, 0f, 0.22f, 0.25f, 0.22f, skin)
        // Hair / Beanie
        addBox(data, 0f, 1.76f, -0.02f, 0.24f, 0.12f, 0.24f, hair)
        // Torso / Mountain Jacket
        addBox(data, 0f, 1.25f, 0f, 0.42f, 0.55f, 0.25f, jacket)
        // Left Arm
        addBox(data, -0.28f, 1.22f, 0f, 0.12f, 0.48f, 0.12f, jacket)
        addBox(data, -0.28f, 0.92f, 0f, 0.10f, 0.12f, 0.10f, skin)
        // Right Arm
        addBox(data, 0.28f, 1.22f, 0f, 0.12f, 0.48f, 0.12f, jacket)
        addBox(data, 0.28f, 0.92f, 0f, 0.10f, 0.12f, 0.10f, skin)
        // Left Leg
        addBox(data, -0.12f, 0.55f, 0f, 0.16f, 0.70f, 0.16f, pants)
        addBox(data, -0.12f, 0.10f, 0.04f, 0.17f, 0.18f, 0.25f, boots)
        // Right Leg
        addBox(data, 0.12f, 0.55f, 0f, 0.16f, 0.70f, 0.16f, pants)
        addBox(data, 0.12f, 0.10f, 0.04f, 0.17f, 0.18f, 0.25f, boots)

        maleBuffer = createBuffer(data)
        maleVertexCount = data.size / 10
    }

    private fun buildFemaleMesh() {
        val data = ArrayList<Float>()
        val skin = floatArrayOf(0.95f, 0.82f, 0.74f, 1f)
        val hair = floatArrayOf(0.65f, 0.42f, 0.18f, 1f) // Auburn Ponytail
        val jacket = floatArrayOf(0.85f, 0.20f, 0.45f, 1f) // Berry Fleece
        val pants = floatArrayOf(0.15f, 0.15f, 0.18f, 1f) // Black active leggings
        val boots = floatArrayOf(0.45f, 0.32f, 0.22f, 1f) // Tan trail boots

        // Head
        addBox(data, 0f, 1.58f, 0f, 0.20f, 0.23f, 0.20f, skin)
        // Ponytail Hair
        addBox(data, 0f, 1.66f, -0.04f, 0.22f, 0.14f, 0.24f, hair)
        addBox(data, 0f, 1.50f, -0.16f, 0.10f, 0.22f, 0.10f, hair)
        // Torso / Fleece
        addBox(data, 0f, 1.20f, 0f, 0.36f, 0.50f, 0.22f, jacket)
        // Left Arm
        addBox(data, -0.24f, 1.18f, 0f, 0.10f, 0.45f, 0.10f, jacket)
        addBox(data, -0.24f, 0.90f, 0f, 0.08f, 0.10f, 0.08f, skin)
        // Right Arm
        addBox(data, 0.24f, 1.18f, 0f, 0.10f, 0.45f, 0.10f, jacket)
        addBox(data, 0.24f, 0.90f, 0f, 0.08f, 0.10f, 0.08f, skin)
        // Left Leg
        addBox(data, -0.10f, 0.52f, 0f, 0.14f, 0.68f, 0.14f, pants)
        addBox(data, -0.10f, 0.09f, 0.04f, 0.15f, 0.16f, 0.22f, boots)
        // Right Leg
        addBox(data, 0.10f, 0.52f, 0f, 0.14f, 0.68f, 0.14f, pants)
        addBox(data, 0.10f, 0.09f, 0.04f, 0.15f, 0.16f, 0.22f, boots)

        femaleBuffer = createBuffer(data)
        femaleVertexCount = data.size / 10
    }

    fun draw(
        shaderProgram: Int,
        viewProjMatrix: FloatArray,
        modelMatrix: FloatArray,
        isMale: Boolean,
        walkPhase: Float,
        isWalking: Boolean,
        uMVPMatrixLoc: Int,
        uModelMatrixLoc: Int
    ) {
        val buffer = (if (isMale) maleBuffer else femaleBuffer) ?: return
        val vCount = if (isMale) maleVertexCount else femaleVertexCount

        System.arraycopy(modelMatrix, 0, charModel, 0, 16)
        if (isWalking) {
            // Natural walking bob & slight shoulder sway
            val bob = kotlin.math.abs(sin(walkPhase)) * 0.04f
            val sway = sin(walkPhase) * 2.5f
            Matrix3D.translate(charModel, 0f, bob, 0f)
            Matrix3D.rotate(charModel, sway, 0f, 1f, 0f)
        }

        Matrix3D.multiply(charMVP, viewProjMatrix, charModel)
        android.opengl.GLES20.glUniformMatrix4fv(uMVPMatrixLoc, 1, false, charMVP, 0)
        android.opengl.GLES20.glUniformMatrix4fv(uModelMatrixLoc, 1, false, charModel, 0)

        val stride = 10 * 4
        buffer.position(0)
        val posLoc = android.opengl.GLES20.glGetAttribLocation(shaderProgram, "a_Position")
        if (posLoc >= 0) {
            android.opengl.GLES20.glEnableVertexAttribArray(posLoc)
            android.opengl.GLES20.glVertexAttribPointer(posLoc, 3, android.opengl.GLES20.GL_FLOAT, false, stride, buffer)
        }
        buffer.position(3)
        val normLoc = android.opengl.GLES20.glGetAttribLocation(shaderProgram, "a_Normal")
        if (normLoc >= 0) {
            android.opengl.GLES20.glEnableVertexAttribArray(normLoc)
            android.opengl.GLES20.glVertexAttribPointer(normLoc, 3, android.opengl.GLES20.GL_FLOAT, false, stride, buffer)
        }
        buffer.position(6)
        val colLoc = android.opengl.GLES20.glGetAttribLocation(shaderProgram, "a_Color")
        if (colLoc >= 0) {
            android.opengl.GLES20.glEnableVertexAttribArray(colLoc)
            android.opengl.GLES20.glVertexAttribPointer(colLoc, 4, android.opengl.GLES20.GL_FLOAT, false, stride, buffer)
        }

        android.opengl.GLES20.glDrawArrays(android.opengl.GLES20.GL_TRIANGLES, 0, vCount)

        if (posLoc >= 0) android.opengl.GLES20.glDisableVertexAttribArray(posLoc)
        if (normLoc >= 0) android.opengl.GLES20.glDisableVertexAttribArray(normLoc)
        if (colLoc >= 0) android.opengl.GLES20.glDisableVertexAttribArray(colLoc)
    }

    private fun addBox(data: ArrayList<Float>, cx: Float, cy: Float, cz: Float,
                       sx: Float, sy: Float, sz: Float, c: FloatArray) {
        val hx = sx * 0.5f
        val hy = sy * 0.5f
        val hz = sz * 0.5f

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

    private fun createBuffer(data: ArrayList<Float>): FloatBuffer {
        val array = data.toFloatArray()
        return ByteBuffer.allocateDirect(array.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(array)
            .apply { position(0) }
    }
}
