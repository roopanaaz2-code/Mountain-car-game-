package com.example.renderer

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.*

data class RoadPoint(
    val x: Float,
    val y: Float, // elevation
    val z: Float,
    val tanX: Float,
    val tanY: Float,
    val tanZ: Float,
    val normX: Float, // perpendicular along road plane
    val normZ: Float,
    val curvature: Float,
    val featureType: RoadFeatureType
)

enum class RoadFeatureType {
    VALLEY_FOREST,
    CANYON_PASS,
    GORGE_BRIDGE,
    SWITCHBACKS,
    CLIFF_TUNNEL,
    ALPINE_VILLAGE,
    HIGH_RIDGE,
    SUMMIT_LOOKOUT
}

data class RoadChunk(
    val index: Int,
    val startDistance: Float,
    val endDistance: Float,
    val vertexBuffer: FloatBuffer,
    val vertexCount: Int,
    val hasStreetlights: Boolean,
    val hasBridge: Boolean,
    val hasGuardrails: Boolean,
    val featureType: RoadFeatureType,
    val boundingCenterX: Float,
    val boundingCenterY: Float,
    val boundingCenterZ: Float,
    val boundingRadius: Float
)

class MountainMap {
    val totalLength = 8000f // 8.0 kilometers of continuous mountain highway
    val roadWidth = 9.0f // 2-lane highway
    val stepSize = 8.0f // sampling resolution
    val pointsCount = (totalLength / stepSize).toInt() + 1
    val roadPoints = ArrayList<RoadPoint>(pointsCount)

    val chunkSize = 80.0f // 80m per chunk
    val totalChunks = (totalLength / chunkSize).toInt()
    private val chunks = ArrayList<RoadChunk>(totalChunks)

    init {
        generateSpline()
        buildChunks()
    }

    private fun generateSpline() {
        for (i in 0 until pointsCount) {
            val s = i * stepSize
            val (x, y, z) = calculateHighwayCoordinates(s)

            // Calculate forward tangent
            val nextS = (s + 2.0f).coerceAtMost(totalLength)
            val (nx, ny, nz) = calculateHighwayCoordinates(nextS)
            val dx = nx - x
            val dy = ny - y
            val dz = nz - z
            val len = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(0.0001f)
            val tx = dx / len
            val ty = dy / len
            val tz = dz / len

            // Road horizontal normal (perpendicular)
            val perpLen = sqrt(tz * tz + tx * tx).coerceAtLeast(0.0001f)
            val normX = -tz / perpLen
            val normZ = tx / perpLen

            // Curvature estimation
            val prevS = (s - 2.0f).coerceAtLeast(0f)
            val (px, _, pz) = calculateHighwayCoordinates(prevS)
            val curvature = ((nx - 2 * x + px).pow(2) + (nz - 2 * z + pz).pow(2)).pow(0.5f)

            val featureType = when {
                s < 900f -> RoadFeatureType.VALLEY_FOREST
                s < 1700f -> RoadFeatureType.CANYON_PASS
                s < 2400f -> RoadFeatureType.GORGE_BRIDGE
                s < 4300f -> RoadFeatureType.SWITCHBACKS
                s < 5200f -> RoadFeatureType.CLIFF_TUNNEL
                s < 6300f -> RoadFeatureType.ALPINE_VILLAGE
                s < 7400f -> RoadFeatureType.HIGH_RIDGE
                else -> RoadFeatureType.SUMMIT_LOOKOUT
            }

            roadPoints.add(RoadPoint(x, y, z, tx, ty, tz, normX, normZ, curvature, featureType))
        }
    }

    /**
     * Parametric 8km Highway Path with realistic curves, hairpin switchbacks, canyons, bridges, and elevation climbing
     */
    fun calculateHighwayCoordinates(s: Float): Triple<Float, Float, Float> {
        var x = 0f
        var y = 25f // Base elevation
        var z = -s

        when {
            // 0 - 900m: Lush Pine Valley (gentle sweeping curves, elevation 25 -> 45m)
            s < 900f -> {
                val t = s / 900f
                x = sin(s * 0.006f) * 65f + sin(s * 0.002f) * 40f
                y = 25f + t * 20f + sin(s * 0.01f) * 4f
                z = -s
            }
            // 900 - 1700m: Canyon Pass (carving through rock cliffs, elevation 45 -> 75m)
            s < 1700f -> {
                val relS = s - 900f
                val t = relS / 800f
                x = sin(900f * 0.006f) * 65f + sin(relS * 0.008f) * 110f - relS * 0.12f
                y = 45f + t * 30f + sin(relS * 0.005f) * 6f
                z = -s
            }
            // 1700 - 2400m: Grand Alpine River Gorge Bridge (high suspension bridge over roaring river, elevation 75 -> 100m)
            s < 2400f -> {
                val relS = s - 1700f
                val t = relS / 700f
                val baseX = sin(900f * 0.006f) * 65f + sin(800f * 0.008f) * 110f - 800f * 0.12f
                x = baseX + sin(relS * 0.003f) * 35f
                y = 75f + t * 25f + sin(t * Math.PI.toFloat()) * 5f // slight arch bridge camber
                z = -s
            }
            // 2400 - 4300m: Mountain Switchbacks & Hairpin Passes (steep climb 100m -> 460m!)
            s < 4300f -> {
                val relS = s - 2400f
                val t = relS / 1900f
                val baseX = sin(900f * 0.006f) * 65f + sin(800f * 0.008f) * 110f - 800f * 0.12f + sin(700f * 0.003f) * 35f
                // Hairpin oscillations
                x = baseX + sin(relS * 0.018f) * 180f + cos(relS * 0.006f) * 60f
                y = 100f + t * 360f + sin(relS * 0.012f) * 8f
                z = -s
            }
            // 4300 - 5200m: Cliffside Tunnel & Rock Canopy (elevation 460 -> 560m)
            s < 5200f -> {
                val relS = s - 4300f
                val t = relS / 900f
                val lastX = calculateHighwayCoordinates(4299f).first
                x = lastX + sin(relS * 0.005f) * 70f
                y = 460f + t * 100f
                z = -s
            }
            // 5200 - 6300m: Alpine Village & Overlook Rest Stop (elevation 560 -> 680m)
            s < 6300f -> {
                val relS = s - 5200f
                val t = relS / 1100f
                val lastX = calculateHighwayCoordinates(5199f).first
                x = lastX + sin(relS * 0.007f) * 120f
                y = 560f + t * 120f
                z = -s
            }
            // 6300 - 7400m: High Alpine Tundra Ridge (elevation 680 -> 820m)
            s < 7400f -> {
                val relS = s - 6300f
                val t = relS / 1100f
                val lastX = calculateHighwayCoordinates(6299f).first
                x = lastX + sin(relS * 0.009f) * 140f
                y = 680f + t * 140f
                z = -s
            }
            // 7400 - 8000m: Mountain Summit & Panoramic Roundabout (elevation 820 -> 850m)
            else -> {
                val relS = s - 7400f
                val t = (relS / 600f).coerceIn(0f, 1f)
                val lastX = calculateHighwayCoordinates(7399f).first
                x = lastX + sin(relS * 0.008f) * 80f
                y = 820f + t * 30f
                z = -s
            }
        }

        return Triple(x, y, z)
    }

    /**
     * Returns interpolated road transform at any distance 's' (0 to 8000)
     */
    fun getRoadPointAt(s: Float): RoadPoint {
        val clampedS = s.coerceIn(0f, totalLength - 0.1f)
        val idx = (clampedS / stepSize).toInt().coerceIn(0, roadPoints.size - 2)
        val frac = (clampedS % stepSize) / stepSize

        val p1 = roadPoints[idx]
        val p2 = roadPoints[idx + 1]

        val x = p1.x + (p2.x - p1.x) * frac
        val y = p1.y + (p2.y - p1.y) * frac
        val z = p1.z + (p2.z - p1.z) * frac
        val tx = p1.tanX + (p2.tanX - p1.tanX) * frac
        val ty = p1.tanY + (p2.tanY - p1.tanY) * frac
        val tz = p1.tanZ + (p2.tanZ - p1.tanZ) * frac
        val nx = p1.normX + (p2.normX - p1.normX) * frac
        val nz = p1.normZ + (p2.normZ - p1.normZ) * frac

        return RoadPoint(x, y, z, tx, ty, tz, nx, nz, p1.curvature, p1.featureType)
    }

    private fun buildChunks() {
        // Build 3D mesh data per 80-meter chunk
        for (c in 0 until totalChunks) {
            val startS = c * chunkSize
            val endS = (startS + chunkSize).coerceAtMost(totalLength)
            val chunkPoints = ArrayList<RoadPoint>()

            var s = startS
            while (s <= endS) {
                chunkPoints.add(getRoadPointAt(s))
                s += 4.0f // fine mesh resolution: every 4 meters
            }
            if (chunkPoints.size < 2) continue

            val vertexData = ArrayList<Float>()
            val isBridge = startS in 1700f..2400f
            val isVillage = startS in 5200f..6300f
            val isCliff = startS in 2400f..4300f || startS in 6300f..7400f
            val hasStreetlights = isVillage || isBridge || startS > 7500f

            // Build Road Strip Vertices (Asphalt + Center Double Yellow Lines + Side White Lines + Guardrails)
            val halfW = roadWidth * 0.5f

            for (i in 0 until chunkPoints.size - 1) {
                val p0 = chunkPoints[i]
                val p1 = chunkPoints[i + 1]

                // --- 1. ASPHALT ROAD SURFACE ---
                val asphaltColor = floatArrayOf(0.24f, 0.25f, 0.27f, 1.0f)
                val shoulderColor = if (isBridge) floatArrayOf(0.40f, 0.42f, 0.45f, 1.0f) else floatArrayOf(0.28f, 0.29f, 0.24f, 1.0f)

                // Left asphalt edge
                val l0x = p0.x + p0.normX * halfW
                val l0y = p0.y + 0.05f
                val l0z = p0.z + p0.normZ * halfW

                // Right asphalt edge
                val r0x = p0.x - p0.normX * halfW
                val r0y = p0.y + 0.05f
                val r0z = p0.z - p0.normZ * halfW

                val l1x = p1.x + p1.normX * halfW
                val l1y = p1.y + 0.05f
                val l1z = p1.z + p1.normZ * halfW

                val r1x = p1.x - p1.normX * halfW
                val r1y = p1.y + 0.05f
                val r1z = p1.z - p1.normZ * halfW

                // Road Quad (2 Triangles)
                addVertex(vertexData, l0x, l0y, l0z, 0f, 1f, 0f, asphaltColor)
                addVertex(vertexData, r0x, r0y, r0z, 0f, 1f, 0f, asphaltColor)
                addVertex(vertexData, l1x, l1y, l1z, 0f, 1f, 0f, asphaltColor)

                addVertex(vertexData, r0x, r0y, r0z, 0f, 1f, 0f, asphaltColor)
                addVertex(vertexData, r1x, r1y, r1z, 0f, 1f, 0f, asphaltColor)
                addVertex(vertexData, l1x, l1y, l1z, 0f, 1f, 0f, asphaltColor)

                // --- 2. DOUBLE YELLOW CENTER LINES ---
                val yellow = floatArrayOf(0.95f, 0.82f, 0.12f, 1.0f)
                val lineHalfW = 0.12f
                val lineOffset = 0.20f

                // Left yellow stripe
                val yl0x1 = p0.x + p0.normX * (lineOffset + lineHalfW)
                val yl0z1 = p0.z + p0.normZ * (lineOffset + lineHalfW)
                val yl0x2 = p0.x + p0.normX * (lineOffset - lineHalfW)
                val yl0z2 = p0.z + p0.normZ * (lineOffset - lineHalfW)

                val yl1x1 = p1.x + p1.normX * (lineOffset + lineHalfW)
                val yl1z1 = p1.z + p1.normZ * (lineOffset + lineHalfW)
                val yl1x2 = p1.x + p1.normX * (lineOffset - lineHalfW)
                val yl1z2 = p1.z + p1.normZ * (lineOffset - lineHalfW)

                addVertex(vertexData, yl0x1, p0.y + 0.08f, yl0z1, 0f, 1f, 0f, yellow)
                addVertex(vertexData, yl0x2, p0.y + 0.08f, yl0z2, 0f, 1f, 0f, yellow)
                addVertex(vertexData, yl1x1, p1.y + 0.08f, yl1z1, 0f, 1f, 0f, yellow)

                addVertex(vertexData, yl0x2, p0.y + 0.08f, yl0z2, 0f, 1f, 0f, yellow)
                addVertex(vertexData, yl1x2, p1.y + 0.08f, yl1z2, 0f, 1f, 0f, yellow)
                addVertex(vertexData, yl1x1, p1.y + 0.08f, yl1z1, 0f, 1f, 0f, yellow)

                // Right yellow stripe
                val yr0x1 = p0.x - p0.normX * (lineOffset - lineHalfW)
                val yr0z1 = p0.z - p0.normZ * (lineOffset - lineHalfW)
                val yr0x2 = p0.x - p0.normX * (lineOffset + lineHalfW)
                val yr0z2 = p0.z - p0.normZ * (lineOffset + lineHalfW)

                val yr1x1 = p1.x - p1.normX * (lineOffset - lineHalfW)
                val yr1z1 = p1.z - p1.normZ * (lineOffset - lineHalfW)
                val yr1x2 = p1.x - p1.normX * (lineOffset + lineHalfW)
                val yr1z2 = p1.z - p1.normZ * (lineOffset + lineHalfW)

                addVertex(vertexData, yr0x1, p0.y + 0.08f, yr0z1, 0f, 1f, 0f, yellow)
                addVertex(vertexData, yr0x2, p0.y + 0.08f, yr0z2, 0f, 1f, 0f, yellow)
                addVertex(vertexData, yr1x1, p1.y + 0.08f, yr1z1, 0f, 1f, 0f, yellow)

                addVertex(vertexData, yr0x2, p0.y + 0.08f, yr0z2, 0f, 1f, 0f, yellow)
                addVertex(vertexData, yr1x2, p1.y + 0.08f, yr1z2, 0f, 1f, 0f, yellow)
                addVertex(vertexData, yr1x1, p1.y + 0.08f, yr1z1, 0f, 1f, 0f, yellow)

                // --- 3. ROADSIDE TERRAIN STRIPS / MOUNTAIN CLIFF / RIVER ---
                val terrainW = if (isBridge) 4.0f else 35.0f
                val grassColor = if (p0.y > 650f) floatArrayOf(0.35f, 0.40f, 0.28f, 1f) else floatArrayOf(0.22f, 0.45f, 0.18f, 1f)
                val rockColor = floatArrayOf(0.42f, 0.40f, 0.38f, 1f)

                // Left terrain bank
                val tl0x = p0.x + p0.normX * (halfW + terrainW)
                val tl0y = if (isCliff) p0.y + 25f else p0.y - 4f
                val tl0z = p0.z + p0.normZ * (halfW + terrainW)

                val tl1x = p1.x + p1.normX * (halfW + terrainW)
                val tl1y = if (isCliff) p1.y + 25f else p1.y - 4f
                val tl1z = p1.z + p1.normZ * (halfW + terrainW)

                addVertex(vertexData, l0x, l0y - 0.2f, l0z, -0.6f, 0.8f, 0f, if (isCliff) rockColor else grassColor)
                addVertex(vertexData, tl0x, tl0y, tl0z, -0.6f, 0.8f, 0f, if (isCliff) rockColor else grassColor)
                addVertex(vertexData, l1x, l1y - 0.2f, l1z, -0.6f, 0.8f, 0f, if (isCliff) rockColor else grassColor)

                addVertex(vertexData, tl0x, tl0y, tl0z, -0.6f, 0.8f, 0f, if (isCliff) rockColor else grassColor)
                addVertex(vertexData, tl1x, tl1y, tl1z, -0.6f, 0.8f, 0f, if (isCliff) rockColor else grassColor)
                addVertex(vertexData, l1x, l1y - 0.2f, l1z, -0.6f, 0.8f, 0f, if (isCliff) rockColor else grassColor)

                // Right terrain bank (cliff precipice overlooking valley)
                val tr0x = p0.x - p0.normX * (halfW + terrainW)
                val tr0y = if (isBridge) 5.0f else (p0.y - 35f).coerceAtLeast(0f)
                val tr0z = p0.z - p0.normZ * (halfW + terrainW)

                val tr1x = p1.x - p1.normX * (halfW + terrainW)
                val tr1y = if (isBridge) 5.0f else (p1.y - 35f).coerceAtLeast(0f)
                val tr1z = p1.z - p1.normZ * (halfW + terrainW)

                addVertex(vertexData, r0x, r0y - 0.2f, r0z, 0.6f, 0.8f, 0f, rockColor)
                addVertex(vertexData, r1x, r1y - 0.2f, r1z, 0.6f, 0.8f, 0f, rockColor)
                addVertex(vertexData, tr0x, tr0y, tr0z, 0.6f, 0.8f, 0f, rockColor)

                addVertex(vertexData, tr0x, tr0y, tr0z, 0.6f, 0.8f, 0f, rockColor)
                addVertex(vertexData, r1x, r1y - 0.2f, r1z, 0.6f, 0.8f, 0f, rockColor)
                addVertex(vertexData, tr1x, tr1y, tr1z, 0.6f, 0.8f, 0f, rockColor)

                // --- 4. METAL CRASH BARRIERS / GUARDRAILS ALONG CLIFFS ---
                if (isCliff || isBridge) {
                    val railColor = floatArrayOf(0.75f, 0.77f, 0.80f, 1.0f)
                    val railH = 0.85f
                    val rx0 = r0x - p0.normX * 0.3f
                    val rz0 = r0z - p0.normZ * 0.3f
                    val rx1 = r1x - p1.normX * 0.3f
                    val rz1 = r1z - p1.normZ * 0.3f

                    addVertex(vertexData, rx0, r0y, rz0, p0.normX, 0f, p0.normZ, railColor)
                    addVertex(vertexData, rx0, r0y + railH, rz0, p0.normX, 0f, p0.normZ, railColor)
                    addVertex(vertexData, rx1, r1y, rz1, p1.normX, 0f, p1.normZ, railColor)

                    addVertex(vertexData, rx0, r0y + railH, rz0, p0.normX, 0f, p0.normZ, railColor)
                    addVertex(vertexData, rx1, r1y + railH, rz1, p1.normX, 0f, p1.normZ, railColor)
                    addVertex(vertexData, rx1, r1y, rz1, p1.normX, 0f, p1.normZ, railColor)
                }

                // --- 5. BRIDGE SUSPENSION PILLARS & ROARING RIVER IN GORGE ---
                if (isBridge && i % 4 == 0) {
                    // River water surface below
                    val riverBlue = floatArrayOf(0.12f, 0.38f, 0.58f, 0.85f)
                    val rw = 60f
                    addVertex(vertexData, p0.x - rw, 4f, p0.z - 20f, 0f, 1f, 0f, riverBlue)
                    addVertex(vertexData, p0.x + rw, 4f, p0.z - 20f, 0f, 1f, 0f, riverBlue)
                    addVertex(vertexData, p0.x - rw, 4f, p0.z + 20f, 0f, 1f, 0f, riverBlue)

                    addVertex(vertexData, p0.x + rw, 4f, p0.z - 20f, 0f, 1f, 0f, riverBlue)
                    addVertex(vertexData, p0.x + rw, 4f, p0.z + 20f, 0f, 1f, 0f, riverBlue)
                    addVertex(vertexData, p0.x - rw, 4f, p0.z + 20f, 0f, 1f, 0f, riverBlue)
                }

                // --- 6. CLIFFSIDE TUNNEL ARCH & CEILING (4300m - 5200m) ---
                if (startS in 4300f..5200f) {
                    val tunnelDark = floatArrayOf(0.18f, 0.19f, 0.21f, 1.0f)
                    val lampAmber = floatArrayOf(1.0f, 0.78f, 0.35f, 1.0f)
                    val archH = 6.2f
                    val archW = halfW + 1.2f

                    val twL0x = p0.x + p0.normX * archW
                    val twL1x = p1.x + p1.normX * archW
                    val twL0z = p0.z + p0.normZ * archW
                    val twL1z = p1.z + p1.normZ * archW

                    val twR0x = p0.x - p0.normX * archW
                    val twR1x = p1.x - p1.normX * archW
                    val twR0z = p0.z - p0.normZ * archW
                    val twR1z = p1.z - p1.normZ * archW

                    val rc0x = p0.x
                    val rc1x = p1.x
                    val rc0z = p0.z
                    val rc1z = p1.z
                    val rc0y = p0.y + archH
                    val rc1y = p1.y + archH

                    // Left tunnel arch wall
                    addVertex(vertexData, twL0x, p0.y, twL0z, -p0.normX, 0f, -p0.normZ, tunnelDark)
                    addVertex(vertexData, rc0x, rc0y, rc0z, 0f, -1f, 0f, tunnelDark)
                    addVertex(vertexData, twL1x, p1.y, twL1z, -p1.normX, 0f, -p1.normZ, tunnelDark)

                    addVertex(vertexData, twL1x, p1.y, twL1z, -p1.normX, 0f, -p1.normZ, tunnelDark)
                    addVertex(vertexData, rc0x, rc0y, rc0z, 0f, -1f, 0f, tunnelDark)
                    addVertex(vertexData, rc1x, rc1y, rc1z, 0f, -1f, 0f, tunnelDark)

                    // Right tunnel arch wall
                    addVertex(vertexData, twR0x, p0.y, twR0z, p0.normX, 0f, p0.normZ, tunnelDark)
                    addVertex(vertexData, twR1x, p1.y, twR1z, p1.normX, 0f, p1.normZ, tunnelDark)
                    addVertex(vertexData, rc0x, rc0y, rc0z, 0f, -1f, 0f, tunnelDark)

                    addVertex(vertexData, rc0x, rc0y, rc0z, 0f, -1f, 0f, tunnelDark)
                    addVertex(vertexData, twR1x, p1.y, twR1z, p1.normX, 0f, p1.normZ, tunnelDark)
                    addVertex(vertexData, rc1x, rc1y, rc1z, 0f, -1f, 0f, tunnelDark)

                    // Ceiling amber lighting strip
                    val lampW = 0.35f
                    val lx0_1 = rc0x + p0.normX * lampW
                    val lz0_1 = rc0z + p0.normZ * lampW
                    val lx0_2 = rc0x - p0.normX * lampW
                    val lz0_2 = rc0z - p0.normZ * lampW

                    val lx1_1 = rc1x + p1.normX * lampW
                    val lz1_1 = rc1z + p1.normZ * lampW
                    val lx1_2 = rc1x - p1.normX * lampW
                    val lz1_2 = rc1z - p1.normZ * lampW

                    addVertex(vertexData, lx0_1, rc0y - 0.05f, lz0_1, 0f, -1f, 0f, lampAmber)
                    addVertex(vertexData, lx0_2, rc0y - 0.05f, lz0_2, 0f, -1f, 0f, lampAmber)
                    addVertex(vertexData, lx1_1, rc1y - 0.05f, lz1_1, 0f, -1f, 0f, lampAmber)

                    addVertex(vertexData, lx0_2, rc0y - 0.05f, lz0_2, 0f, -1f, 0f, lampAmber)
                    addVertex(vertexData, lx1_2, rc1y - 0.05f, lz1_2, 0f, -1f, 0f, lampAmber)
                    addVertex(vertexData, lx1_1, rc1y - 0.05f, lz1_1, 0f, -1f, 0f, lampAmber)
                }
            }

            // Create FloatBuffer for chunk
            val floatArray = vertexData.toFloatArray()
            val buffer = ByteBuffer.allocateDirect(floatArray.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(floatArray)
            buffer.position(0)

            val vCount = floatArray.size / 10 // 3 pos + 3 norm + 4 col = 10 floats per vertex
            val midS = (startS + endS) * 0.5f
            val midPt = getRoadPointAt(midS)
            val bRadius = (chunkSize * 0.5f) + 48f

            chunks.add(
                RoadChunk(
                    c, startS, endS, buffer, vCount,
                    hasStreetlights, isBridge, isCliff,
                    chunkPoints.first().featureType,
                    midPt.x, midPt.y, midPt.z, bRadius
                )
            )
        }
    }

    private fun addVertex(data: ArrayList<Float>, x: Float, y: Float, z: Float,
                          nx: Float, ny: Float, nz: Float, color: FloatArray) {
        data.add(x)
        data.add(y)
        data.add(z)
        data.add(nx)
        data.add(ny)
        data.add(nz)
        data.add(color[0])
        data.add(color[1])
        data.add(color[2])
        data.add(color[3])
    }

    fun getChunksInRange(centerS: Float, viewDist: Float): List<RoadChunk> {
        val minS = centerS - 60f // keep a bit behind car for mirrors and rear camera
        val maxS = centerS + viewDist
        return chunks.filter { it.endDistance >= minS && it.startDistance <= maxS }
    }

    fun getAllChunks(): List<RoadChunk> = chunks

    fun getSurfaceHeight(wx: Float, wz: Float): Float {
        val approxS = (-wz).coerceIn(0f, totalLength - 0.1f)
        val pt = getRoadPointAt(approxS)
        val dx = wx - pt.x
        val dz = wz - pt.z
        val offset = dx * pt.normX + dz * pt.normZ
        val halfW = roadWidth * 0.5f

        return when {
            abs(offset) <= halfW + 0.8f -> pt.y + 0.15f
            offset > 0f -> {
                // Left bank / mountain side
                val isCliff = approxS in 2400f..4300f || approxS in 6300f..7400f
                if (isCliff) pt.y + ((offset - halfW) * 0.6f).coerceAtMost(25f)
                else pt.y - 0.2f
            }
            else -> {
                // Right bank / cliff side
                val isBridge = approxS in 1700f..2400f
                if (isBridge) 5.0f
                else (pt.y - (abs(offset) - halfW) * 0.8f).coerceAtLeast(0f)
            }
        }
    }

    fun getRoadOffset(wx: Float, wz: Float, s: Float): Float {
        val pt = getRoadPointAt(s)
        val dx = wx - pt.x
        val dz = wz - pt.z
        return dx * pt.normX + dz * pt.normZ
    }
}
