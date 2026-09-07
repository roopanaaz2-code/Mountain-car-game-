package com.example.engine

import kotlin.math.sqrt

/**
 * Mobile-optimized frustum culler for real-time scene spatial partitioning.
 * Extracts the 6 frustum clipping planes from the combined View-Projection matrix
 * and performs zero-allocation sphere/box intersection tests to reject off-screen
 * terrain chunks and roadside props.
 */
class FrustumCuller {
    // 6 planes: Left, Right, Bottom, Top, Near, Far
    // Each plane is represented by (A, B, C, D) such that Ax + By + Cz + D >= 0 is inside.
    private val planes = Array(6) { FloatArray(4) }

    fun update(m: FloatArray) {
        // Left plane
        planes[0][0] = m[3] + m[0]
        planes[0][1] = m[7] + m[4]
        planes[0][2] = m[11] + m[8]
        planes[0][3] = m[15] + m[12]

        // Right plane
        planes[1][0] = m[3] - m[0]
        planes[1][1] = m[7] - m[4]
        planes[1][2] = m[11] - m[8]
        planes[1][3] = m[15] - m[12]

        // Bottom plane
        planes[2][0] = m[3] + m[1]
        planes[2][1] = m[7] + m[5]
        planes[2][2] = m[11] + m[9]
        planes[2][3] = m[15] + m[13]

        // Top plane
        planes[3][0] = m[3] - m[1]
        planes[3][1] = m[7] - m[5]
        planes[3][2] = m[11] - m[9]
        planes[3][3] = m[15] - m[13]

        // Near plane
        planes[4][0] = m[3] + m[2]
        planes[4][1] = m[7] + m[6]
        planes[4][2] = m[11] + m[10]
        planes[4][3] = m[15] + m[14]

        // Far plane
        planes[5][0] = m[3] - m[2]
        planes[5][1] = m[7] - m[6]
        planes[5][2] = m[11] - m[10]
        planes[5][3] = m[15] - m[14]

        // Normalize plane normals
        for (i in 0..5) {
            val p = planes[i]
            val len = sqrt(p[0] * p[0] + p[1] * p[1] + p[2] * p[2])
            if (len > 0.00001f) {
                p[0] /= len
                p[1] /= len
                p[2] /= len
                p[3] /= len
            }
        }
    }

    /**
     * Fast sphere-frustum rejection test.
     * Returns false if the bounding sphere is completely outside any of the 6 planes.
     */
    fun sphereInFrustum(cx: Float, cy: Float, cz: Float, radius: Float): Boolean {
        for (i in 0..5) {
            val p = planes[i]
            val dist = p[0] * cx + p[1] * cy + p[2] * cz + p[3]
            if (dist < -radius) {
                return false
            }
        }
        return true
    }

    /**
     * Fast Axis-Aligned Bounding Box (AABB) frustum test.
     */
    fun boxInFrustum(minX: Float, minY: Float, minZ: Float, maxX: Float, maxY: Float, maxZ: Float): Boolean {
        for (i in 0..5) {
            val p = planes[i]
            val px = if (p[0] > 0) maxX else minX
            val py = if (p[1] > 0) maxY else minY
            val pz = if (p[2] > 0) maxZ else minZ

            if (p[0] * px + p[1] * py + p[2] * pz + p[3] < 0) {
                return false
            }
        }
        return true
    }
}
