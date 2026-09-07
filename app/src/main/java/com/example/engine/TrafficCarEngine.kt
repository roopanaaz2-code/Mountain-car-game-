package com.example.engine

import android.opengl.GLES20
import com.example.renderer.Matrix3D
import com.example.renderer.MountainMap
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class TrafficCarType(
    val modelName: String,
    val r: Float, val g: Float, val b: Float,
    val length: Float, val width: Float, val height: Float,
    val speedKmh: Float
) {
    SEDAN_SILVER("Silver Sedan", 0.75f, 0.78f, 0.82f, 4.3f, 1.85f, 1.35f, 75f),
    SUV_NAVY("Navy SUV", 0.12f, 0.22f, 0.42f, 4.6f, 1.95f, 1.68f, 68f),
    HATCH_RED("Red Compact", 0.85f, 0.15f, 0.12f, 3.8f, 1.75f, 1.32f, 82f),
    TRUCK_WHITE("White Delivery Van", 0.90f, 0.90f, 0.92f, 5.2f, 2.05f, 2.10f, 60f),
    COUPE_GOLD("Gold Coupe", 0.82f, 0.65f, 0.25f, 4.2f, 1.88f, 1.25f, 90f)
}

class TrafficVehicle(
    val id: Int,
    var distanceS: Float,
    val laneOffset: Float, // +2.2f for right lane, -2.2f for left lane (oncoming)
    val type: TrafficCarType,
    val isOpposing: Boolean
) {
    var worldX: Float = 0f
    var worldY: Float = 0f
    var worldZ: Float = 0f
    var headingDeg: Float = 0f
    var currentSpeedMps: Float = (type.speedKmh / 3.6f) * (if (isOpposing) -1f else 1f)

    fun update(dt: Float, map: MountainMap) {
        val delta = currentSpeedMps * dt
        distanceS += delta
        if (distanceS < 100f) {
            distanceS = 7800f
        } else if (distanceS > 7900f) {
            distanceS = 150f
        }

        val pt = map.getRoadPointAt(distanceS)
        worldX = pt.x + pt.normX * laneOffset
        worldZ = pt.z + pt.normZ * laneOffset
        worldY = pt.y + 0.12f

        val rawHeading = Math.toDegrees(atan2(pt.tanX.toDouble(), pt.tanZ.toDouble())).toFloat()
        headingDeg = if (isOpposing) (rawHeading + 180f) % 360f else rawHeading
    }
}

class TrafficCarEngine(private val map: MountainMap) {
    val trafficCars = ArrayList<TrafficVehicle>()
    private var carMeshBuffer: FloatBuffer? = null
    private var carVertexCount = 0

    private val tempModel = FloatArray(16)
    private val tempMVP = FloatArray(16)

    init {
        initTraffic()
        buildTrafficCarMesh()
    }

    private fun initTraffic() {
        // Spawn 14 distinct AI traffic vehicles along the 8km highway:
        // Half in the forward lane (+2.25m), half in oncoming lane (-2.25m)
        val initialSpawns = listOf(
            Triple(280f, 2.25f, TrafficCarType.SEDAN_SILVER),
            Triple(680f, -2.25f, TrafficCarType.SUV_NAVY),
            Triple(1150f, 2.25f, TrafficCarType.HATCH_RED),
            Triple(1620f, -2.25f, TrafficCarType.TRUCK_WHITE),
            Triple(2100f, 2.25f, TrafficCarType.COUPE_GOLD),
            Triple(2750f, -2.25f, TrafficCarType.SEDAN_SILVER),
            Triple(3400f, 2.25f, TrafficCarType.SUV_NAVY),
            Triple(4100f, -2.25f, TrafficCarType.HATCH_RED),
            Triple(4650f, 2.25f, TrafficCarType.TRUCK_WHITE),
            Triple(5300f, -2.25f, TrafficCarType.COUPE_GOLD),
            Triple(5900f, 2.25f, TrafficCarType.SEDAN_SILVER),
            Triple(6500f, -2.25f, TrafficCarType.SUV_NAVY),
            Triple(7100f, 2.25f, TrafficCarType.HATCH_RED),
            Triple(7600f, -2.25f, TrafficCarType.COUPE_GOLD)
        )

        for (i in initialSpawns.indices) {
            val (dist, lane, carType) = initialSpawns[i]
            val isOpposing = lane < 0f
            trafficCars.add(TrafficVehicle(i, dist, lane, carType, isOpposing))
        }
    }

    private fun buildTrafficCarMesh() {
        val data = ArrayList<Float>()
        // Generic low-poly stylized chassis that scales to any vehicle type
        // Base dimensions: L=4.2, W=1.85, H=1.35
        val halfW = 0.92f
        val halfL = 2.10f
        val bodyH = 0.55f
        val cabinH = 0.58f

        val baseCol = floatArrayOf(1f, 1f, 1f, 1f) // tinted per car in shader/uniform
        val glassCol = floatArrayOf(0.12f, 0.18f, 0.25f, 1f)
        val wheelCol = floatArrayOf(0.12f, 0.12f, 0.14f, 1f)
        val headlampCol = floatArrayOf(1.0f, 0.98f, 0.85f, 1f)
        val taillampCol = floatArrayOf(0.95f, 0.05f, 0.05f, 1f)
        val trimBlack = floatArrayOf(0.08f, 0.08f, 0.09f, 1f)

        // 1. Lower chassis box
        addBox(data, 0f, 0.38f, 0f, halfW * 2f, bodyH, halfL * 2f, baseCol)

        // 2. Cabin greenhouse (roof and pillars)
        addBox(data, 0f, 0.38f + bodyH * 0.5f + cabinH * 0.5f, -0.2f, halfW * 1.75f, cabinH, halfL * 1.15f, baseCol)

        // 3. Front windshield & rear glass
        addBox(data, 0f, 0.38f + bodyH * 0.5f + cabinH * 0.5f, -0.2f, halfW * 1.78f, cabinH * 0.85f, halfL * 1.18f, glassCol)

        // 4. Headlights (amber/white)
        addBox(data, -halfW * 0.72f, 0.45f, halfL + 0.02f, 0.34f, 0.16f, 0.04f, headlampCol)
        addBox(data, halfW * 0.72f, 0.45f, halfL + 0.02f, 0.34f, 0.16f, 0.04f, headlampCol)

        // 5. Taillights (red)
        addBox(data, -halfW * 0.72f, 0.50f, -halfL - 0.02f, 0.34f, 0.14f, 0.04f, taillampCol)
        addBox(data, halfW * 0.72f, 0.50f, -halfL - 0.02f, 0.34f, 0.14f, 0.04f, taillampCol)

        // 6. Wheels (4 black cylinders/boxes)
        val wR = 0.34f
        val wW = 0.24f
        val wY = 0.34f
        val fwdW = halfL * 0.65f
        // Front-left, Front-right, Rear-left, Rear-right
        addBox(data, -halfW, wY, fwdW, wW, wR * 2f, wR * 2f, wheelCol)
        addBox(data, halfW, wY, fwdW, wW, wR * 2f, wR * 2f, wheelCol)
        addBox(data, -halfW, wY, -fwdW, wW, wR * 2f, wR * 2f, wheelCol)
        addBox(data, halfW, wY, -fwdW, wW, wR * 2f, wR * 2f, wheelCol)

        val bb = ByteBuffer.allocateDirect(data.size * 4).order(ByteOrder.nativeOrder())
        carMeshBuffer = bb.asFloatBuffer()
        for (f in data) carMeshBuffer!!.put(f)
        carMeshBuffer!!.position(0)
        carVertexCount = data.size / 10
    }

    fun update(dt: Float) {
        for (car in trafficCars) {
            car.update(dt, map)
        }
    }

    fun checkPlayerCollision(playerX: Float, playerY: Float, playerZ: Float): TrafficVehicle? {
        for (car in trafficCars) {
            val dx = playerX - car.worldX
            val dy = playerY - car.worldY
            val dz = playerZ - car.worldZ
            val distSq = dx * dx + dz * dz
            if (distSq < 7.2f && kotlin.math.abs(dy) < 2.5f) {
                return car
            }
        }
        return null
    }

    fun draw(
        program: Int,
        viewProjMatrix: FloatArray,
        camX: Float,
        camZ: Float,
        renderDist: Float,
        uMVPLoc: Int,
        uModelLoc: Int
    ) {
        val buffer = carMeshBuffer ?: return

        buffer.position(0)
        val stride = 10 * 4
        val aPos = GLES20.glGetAttribLocation(program, "a_Position")
        val aNorm = GLES20.glGetAttribLocation(program, "a_Normal")
        val aCol = GLES20.glGetAttribLocation(program, "a_Color")

        GLES20.glEnableVertexAttribArray(aPos)
        GLES20.glEnableVertexAttribArray(aNorm)
        GLES20.glEnableVertexAttribArray(aCol)

        buffer.position(0)
        GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, stride, buffer)
        buffer.position(3)
        GLES20.glVertexAttribPointer(aNorm, 3, GLES20.GL_FLOAT, false, stride, buffer)
        buffer.position(6)
        GLES20.glVertexAttribPointer(aCol, 4, GLES20.GL_FLOAT, false, stride, buffer)

        val maxDistSq = renderDist * renderDist

        for (car in trafficCars) {
            val dx = car.worldX - camX
            val dz = car.worldZ - camZ
            if (dx * dx + dz * dz > maxDistSq) {
                continue
            }

            Matrix3D.setIdentity(tempModel)
            Matrix3D.translate(tempModel, car.worldX, car.worldY, car.worldZ)
            Matrix3D.rotate(tempModel, car.headingDeg, 0f, 1f, 0f)

            // Scale to specific vehicle proportions
            val sx = car.type.width / 1.85f
            val sy = car.type.height / 1.35f
            val sz = car.type.length / 4.2f
            Matrix3D.scale(tempModel, sx, sy, sz)

            Matrix3D.multiply(tempMVP, viewProjMatrix, tempModel)
            GLES20.glUniformMatrix4fv(uMVPLoc, 1, false, tempMVP, 0)
            GLES20.glUniformMatrix4fv(uModelLoc, 1, false, tempModel, 0)

            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, carVertexCount)
        }

        GLES20.glDisableVertexAttribArray(aPos)
        GLES20.glDisableVertexAttribArray(aNorm)
        GLES20.glDisableVertexAttribArray(aCol)
    }

    private fun addBox(
        data: ArrayList<Float>,
        cx: Float, cy: Float, cz: Float,
        w: Float, h: Float, l: Float,
        col: FloatArray
    ) {
        val hw = w * 0.5f
        val hh = h * 0.5f
        val hl = l * 0.5f

        val x0 = cx - hw
        val x1 = cx + hw
        val y0 = cy - hh
        val y1 = cy + hh
        val z0 = cz - hl
        val z1 = cz + hl

        // Front face (+Z)
        addQuad(data, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0f, 0f, 1f, col)
        // Back face (-Z)
        addQuad(data, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0f, 0f, -1f, col)
        // Top face (+Y)
        addQuad(data, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0f, 1f, 0f, col)
        // Bottom face (-Y)
        addQuad(data, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0f, -1f, 0f, col)
        // Right face (+X)
        addQuad(data, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1f, 0f, 0f, col)
        // Left face (-X)
        addQuad(data, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1f, 0f, 0f, col)
    }

    private fun addQuad(
        data: ArrayList<Float>,
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        nx: Float, ny: Float, nz: Float,
        c: FloatArray
    ) {
        addVertex(data, x0, y0, z0, nx, ny, nz, c)
        addVertex(data, x1, y1, z1, nx, ny, nz, c)
        addVertex(data, x2, y2, z2, nx, ny, nz, c)

        addVertex(data, x0, y0, z0, nx, ny, nz, c)
        addVertex(data, x2, y2, z2, nx, ny, nz, c)
        addVertex(data, x3, y3, z3, nx, ny, nz, c)
    }

    private fun addVertex(
        data: ArrayList<Float>,
        x: Float, y: Float, z: Float,
        nx: Float, ny: Float, nz: Float,
        c: FloatArray
    ) {
        data.add(x); data.add(y); data.add(z)
        data.add(nx); data.add(ny); data.add(nz)
        data.add(c[0]); data.add(c[1]); data.add(c[2]); data.add(c[3])
    }
}
