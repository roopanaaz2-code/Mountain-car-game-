package com.example.renderer

import android.opengl.Matrix
import kotlin.math.cos
import kotlin.math.sin

object Matrix3D {
    fun setIdentity(m: FloatArray, offset: Int = 0) {
        Matrix.setIdentityM(m, offset)
    }

    fun perspective(m: FloatArray, fovY: Float, aspect: Float, zNear: Float, zFar: Float) {
        Matrix.perspectiveM(m, 0, fovY, aspect, zNear, zFar)
    }

    fun lookAt(m: FloatArray, eyeX: Float, eyeY: Float, eyeZ: Float,
               centerX: Float, centerY: Float, centerZ: Float,
               upX: Float, upY: Float, upZ: Float) {
        Matrix.setLookAtM(m, 0, eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ)
    }

    fun multiply(result: FloatArray, lhs: FloatArray, rhs: FloatArray) {
        Matrix.multiplyMM(result, 0, lhs, 0, rhs, 0)
    }

    fun translate(m: FloatArray, x: Float, y: Float, z: Float) {
        Matrix.translateM(m, 0, x, y, z)
    }

    fun rotate(m: FloatArray, angleDeg: Float, x: Float, y: Float, z: Float) {
        Matrix.rotateM(m, 0, angleDeg, x, y, z)
    }

    fun scale(m: FloatArray, sx: Float, sy: Float, sz: Float) {
        Matrix.scaleM(m, 0, sx, sy, sz)
    }
}
