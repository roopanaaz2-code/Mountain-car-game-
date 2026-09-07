package com.example.renderer

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.example.audio.SoundEngine
import com.example.engine.DistantMountainEngine
import com.example.engine.FrustumCuller
import com.example.engine.ParticleEngine
import com.example.engine.TrafficCarEngine
import com.example.model.*
import com.example.physics.PlayerWalkingPhysics
import com.example.physics.VehiclePhysics
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

class MountainGLRenderer(
    private val context: Context,
    val map: MountainMap,
    val soundEngine: SoundEngine
) : GLSurfaceView.Renderer {

    // Active specifications
    var activeVehicleSpec: VehicleSpec = GameContent.vehicles[0]
    var activeCustomization: CarCustomization = CarCustomization()
    var activeCharacter: CharacterSpec = GameContent.characters[0]
    var graphicsQuality: GraphicsQuality = GraphicsQuality.HIGH
    var controlType: ControlType = ControlType.STEERING_WHEEL
    var transmissionType: TransmissionType = TransmissionType.AUTOMATIC
    var controlSensitivity: Float = 1.0f
    var timeOfDaySetting: TimeOfDay = TimeOfDay.DYNAMIC
    var weatherSetting: WeatherType = WeatherType.CLEAR

    // Subsystems & 3D Engine Modules
    val vehicle = VehiclePhysics(activeVehicleSpec, map)
    val walker = PlayerWalkingPhysics()
    val camera = CameraSystem()
    val environment = EnvironmentSystem()
    val vehicle3D = Vehicle3D()
    val character3D = Character3D()
    val frustumCuller = FrustumCuller()
    val particleEngine = ParticleEngine()
    val distantMountains = DistantMountainEngine()
    val trafficEngine = TrafficCarEngine(map)

    // Shader programs
    private var mainProgram = 0
    private var skyProgram = 0
    private var rainProgram = 0

    // Matrices
    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val viewProjMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val carMatrix = FloatArray(16)
    private val charMatrix = FloatArray(16)

    // Time tracking
    private var lastFrameTimeNano: Long = 0L
    var fps: Int = 60
    private var frameCount = 0
    private var fpsTimer = 0f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.2f, 0.4f, 0.8f, 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glDisable(GLES20.GL_CULL_FACE)

        mainProgram = Shaders.buildProgram(Shaders.MAIN_VERTEX_SHADER, Shaders.MAIN_FRAGMENT_SHADER)
        skyProgram = Shaders.buildProgram(Shaders.SKY_VERTEX_SHADER, Shaders.SKY_FRAGMENT_SHADER)
        rainProgram = Shaders.buildProgram(Shaders.RAIN_VERTEX_SHADER, Shaders.RAIN_FRAGMENT_SHADER)

        lastFrameTimeNano = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / height.coerceAtLeast(1).toFloat()
        // 55 degree FOV for realistic mountain driving perspective with expansive far plane for mountain ranges
        Matrix3D.perspective(projMatrix, 55.0f, aspect, 0.4f, graphicsQuality.renderDistance + 450f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = if (lastFrameTimeNano == 0L) 0.016f else ((now - lastFrameTimeNano) / 1_000_000_000f).coerceIn(0.001f, 0.1f)
        lastFrameTimeNano = now

        // FPS meter
        frameCount++
        fpsTimer += dt
        if (fpsTimer >= 1.0f) {
            fps = frameCount
            frameCount = 0
            fpsTimer = 0f
        }

        // 1. UPDATE PHYSICS & AUDIO
        vehicle.update(dt, transmissionType, controlSensitivity)
        walker.update(dt)
        trafficEngine.update(dt)
        camera.update(dt, vehicle, walker)
        environment.update(dt, timeOfDaySetting, weatherSetting)

        // Traffic Car Collision Detection
        val hitTraffic = trafficEngine.checkPlayerCollision(vehicle.worldX, vehicle.worldY, vehicle.worldZ)
        if (hitTraffic != null) {
            vehicle.hasCollisionImpact = true
            vehicle.collisionSparksX = (vehicle.worldX + hitTraffic.worldX) * 0.5f
            vehicle.collisionSparksY = (vehicle.worldY + hitTraffic.worldY) * 0.5f + 0.5f
            vehicle.collisionSparksZ = (vehicle.worldZ + hitTraffic.worldZ) * 0.5f
            vehicle.speedMps *= 0.45f
            soundEngine.playCrashSound()
        }

        // Road barrier crash sound trigger
        if (vehicle.hasCollisionImpact && hitTraffic == null) {
            soundEngine.playCrashSound()
        }

        // Update Sound parameters
        soundEngine.engineRpm = vehicle.engineRpm
        soundEngine.throttle = vehicle.throttleInput
        soundEngine.speedKmh = vehicle.speedKmh
        soundEngine.tireSlip = vehicle.tireSlipFactor
        soundEngine.isBrakingHard = vehicle.brakeInput > 0.65f && vehicle.speedKmh > 10f
        soundEngine.enginePitchMultiplier = activeVehicleSpec.engineSoundPitchMultiplier
        soundEngine.isWalking = !walker.isInsideVehicle && walker.isWalking
        soundEngine.isBlinkerActive = vehicle.blinkerLeft || vehicle.blinkerRight || vehicle.hazardLights

        // Gorge River / Waterfall proximity check (1700m - 2400m on highway)
        val currentDist = if (walker.isInsideVehicle) vehicle.distanceS else (walker.worldZ * -1f)
        if (currentDist in 1650f..2450f) {
            soundEngine.isNearWater = true
            soundEngine.waterProximity = 1.0f - (Math.abs(currentDist - 2050f) / 400f).coerceIn(0f, 1f)
        } else {
            soundEngine.isNearWater = false
            soundEngine.waterProximity = 0f
        }

        // 2. SETUP VIEW & PROJECTION
        Matrix3D.lookAt(
            viewMatrix,
            camera.eyeX, camera.eyeY, camera.eyeZ,
            camera.targetX, camera.targetY, camera.targetZ,
            camera.upX, camera.upY, camera.upZ
        )
        Matrix3D.multiply(viewProjMatrix, projMatrix, viewMatrix)

        // Update Frustum Culler & Particle Engine
        frustumCuller.update(viewProjMatrix)
        particleEngine.update(dt)

        val lighting = environment.computeLighting(graphicsQuality)

        // Clear Color matches atmospheric fog
        GLES20.glClearColor(lighting.fogColor[0], lighting.fogColor[1], lighting.fogColor[2], 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // 3. DRAW SKY DOME
        environment.drawSky(skyProgram, viewProjMatrix, camera.eyeX, camera.eyeY, camera.eyeZ, lighting)

        // 4. DRAW 3D HIGHWAY & CHUNK TERRAIN (STREAMED BY DISTANCE)
        GLES20.glUseProgram(mainProgram)
        setupLightingUniforms(mainProgram, lighting)

        val uMVPLoc = GLES20.glGetUniformLocation(mainProgram, "u_MVPMatrix")
        val uModelLoc = GLES20.glGetUniformLocation(mainProgram, "u_ModelMatrix")

        // Draw Majestic Distant Mountain Range Ring
        distantMountains.draw(mainProgram, viewProjMatrix, camera.eyeX, camera.eyeY, camera.eyeZ, uMVPLoc, uModelLoc)

        val visibleChunks = map.getChunksInRange(vehicle.distanceS, graphicsQuality.renderDistance)
        for (chunk in visibleChunks) {
            GLES20.glUniformMatrix4fv(uMVPLoc, 1, false, viewProjMatrix, 0)
            Matrix3D.setIdentity(modelMatrix)
            GLES20.glUniformMatrix4fv(uModelLoc, 1, false, modelMatrix, 0)

            drawRoadChunkBuffer(mainProgram, chunk)

            // Draw Roadside Trees along chunk
            val treeStep = (35f / graphicsQuality.foliageDensity).coerceAtLeast(18f)
            var ts = chunk.startDistance
            while (ts <= chunk.endDistance) {
                val pt = map.getRoadPointAt(ts)
                val leftDist = 8.5f + (sin(ts * 0.1f) * 3f)
                val rightDist = 8.5f + (cos(ts * 0.1f) * 3f)

                // Left pine tree
                environment.drawTree(
                    mainProgram, viewProjMatrix,
                    pt.x + pt.normX * leftDist, pt.y - 0.2f, pt.z + pt.normZ * leftDist,
                    1.4f, uMVPLoc, uModelLoc
                )
                // Right tree (if not a sheer precipice bridge)
                if (!chunk.hasBridge) {
                    environment.drawTree(
                        mainProgram, viewProjMatrix,
                        pt.x - pt.normX * rightDist, pt.y - 0.5f, pt.z - pt.normZ * rightDist,
                        1.2f, uMVPLoc, uModelLoc
                    )
                }
                ts += treeStep
            }
        }

        // 4.5 DRAW HIGHWAY TRAFFIC CARS
        trafficEngine.draw(mainProgram, viewProjMatrix, camera.eyeX, camera.eyeZ, graphicsQuality.renderDistance, uMVPLoc, uModelLoc)

        // 5. DRAW VEHICLE
        vehicle3D.prepareVehicle(activeVehicleSpec.id, activeCustomization)
        Matrix3D.setIdentity(carMatrix)
        Matrix3D.translate(carMatrix, vehicle.worldX, vehicle.worldY, vehicle.worldZ)
        Matrix3D.rotate(carMatrix, vehicle.headingAngleDeg, 0f, 1f, 0f)
        Matrix3D.rotate(carMatrix, vehicle.bodyPitchDeg, 1f, 0f, 0f)
        Matrix3D.rotate(carMatrix, vehicle.bodyRollDeg, 0f, 0f, 1f)

        val isBlinkerL = vehicle.blinkerLeft || vehicle.hazardLights
        val isBlinkerR = vehicle.blinkerRight || vehicle.hazardLights

        vehicle3D.draw(
            mainProgram,
            viewProjMatrix,
            carMatrix,
            vehicle.steerAngleDeg,
            vehicle.wheelSpinDeg,
            vehicle.doorOpenAngleDeg,
            vehicle.headlightsOn,
            vehicle.highBeam,
            vehicle.brakeInput > 0.05f,
            vehicle.currentGear == Gear.REVERSE,
            isBlinkerL,
            isBlinkerR,
            vehicle.isBlinkerBlink,
            vehicle.isNitro,
            uMVPLoc,
            uModelLoc
        )

        // Real-Time Particle Emission
        val carHeadingRad = Math.toRadians(vehicle.headingAngleDeg.toDouble()).toFloat()
        val fwdX = sin(carHeadingRad)
        val fwdZ = cos(carHeadingRad)
        val rightX = cos(carHeadingRad)
        val rightZ = -sin(carHeadingRad)

        // Nitro Jet Flames from dual exhaust outlets
        if (vehicle.isNitro) {
            val exL_X = vehicle.worldX + rightX * -0.34f - fwdX * 2.2f
            val exL_Y = vehicle.worldY + 0.35f
            val exL_Z = vehicle.worldZ + rightZ * -0.34f - fwdZ * 2.2f
            particleEngine.emitNitroFlame(exL_X, exL_Y, exL_Z, fwdX, fwdZ, 3)

            val exR_X = vehicle.worldX + rightX * 0.34f - fwdX * 2.2f
            val exR_Y = vehicle.worldY + 0.35f
            val exR_Z = vehicle.worldZ + rightZ * 0.34f - fwdZ * 2.2f
            particleEngine.emitNitroFlame(exR_X, exR_Y, exR_Z, fwdX, fwdZ, 3)
        }

        // Tire Drift Smoke
        if (vehicle.tireSlipFactor > 0.25f && vehicle.speedKmh > 10f) {
            val rlX = vehicle.worldX + rightX * -0.85f - fwdX * 1.35f
            val rlZ = vehicle.worldZ + rightZ * -0.85f - fwdZ * 1.35f
            val rrX = vehicle.worldX + rightX * 0.85f - fwdX * 1.35f
            val rrZ = vehicle.worldZ + rightZ * 0.85f - fwdZ * 1.35f
            particleEngine.emitTireSmoke(rlX, vehicle.worldY, rlZ, 2)
            particleEngine.emitTireSmoke(rrX, vehicle.worldY, rrZ, 2)
        }

        // Collision Sparks
        if (vehicle.hasCollisionImpact) {
            particleEngine.emitSparks(vehicle.collisionSparksX, vehicle.collisionSparksY, vehicle.collisionSparksZ, 6)
        }

        // Render Active Particles
        particleEngine.draw(viewProjMatrix)

        // 6. DRAW CHARACTER
        if (walker.isInsideVehicle) {
            // Character sitting in driver seat inside the vehicle
            System.arraycopy(carMatrix, 0, charMatrix, 0, 16)
            Matrix3D.translate(charMatrix, -0.42f, 0.40f, 0.05f)
            Matrix3D.scale(charMatrix, 0.85f, 0.85f, 0.85f)
            character3D.draw(
                mainProgram,
                viewProjMatrix,
                charMatrix,
                activeCharacter.gender == "Male",
                0f, false,
                uMVPLoc, uModelLoc
            )
        } else {
            // Character walking freely on foot
            Matrix3D.setIdentity(charMatrix)
            Matrix3D.translate(charMatrix, walker.worldX, walker.worldY, walker.worldZ)
            Matrix3D.rotate(charMatrix, walker.headingAngleDeg, 0f, 1f, 0f)
            character3D.draw(
                mainProgram,
                viewProjMatrix,
                charMatrix,
                activeCharacter.gender == "Male",
                walker.walkPhase, walker.isWalking,
                uMVPLoc, uModelLoc
            )
        }

        // 7. DRAW RAIN PARTICLES
        if (weatherSetting == WeatherType.RAIN && graphicsQuality.hasRainParticles) {
            environment.drawRain(rainProgram, viewProjMatrix, camera.eyeX, camera.eyeY, camera.eyeZ, dt)
        }
    }

    private fun setupLightingUniforms(program: Int, lighting: LightingState) {
        val sunDirLoc = GLES20.glGetUniformLocation(program, "u_SunDirection")
        val sunColLoc = GLES20.glGetUniformLocation(program, "u_SunColor")
        val ambLoc = GLES20.glGetUniformLocation(program, "u_AmbientLight")
        val fogColLoc = GLES20.glGetUniformLocation(program, "u_FogColor")
        val fogStartLoc = GLES20.glGetUniformLocation(program, "u_FogStart")
        val fogEndLoc = GLES20.glGetUniformLocation(program, "u_FogEnd")
        val wetnessLoc = GLES20.glGetUniformLocation(program, "u_Wetness")
        val camPosLoc = GLES20.glGetUniformLocation(program, "u_CameraPos")

        // Headlights Spotlight Uniforms
        val headPosLoc = GLES20.glGetUniformLocation(program, "u_HeadlightPos")
        val headDirLoc = GLES20.glGetUniformLocation(program, "u_HeadlightDir")
        val headColLoc = GLES20.glGetUniformLocation(program, "u_HeadlightColor")
        val headEnLoc = GLES20.glGetUniformLocation(program, "u_HeadlightEnabled")

        GLES20.glUniform3fv(sunDirLoc, 1, lighting.sunDirection, 0)
        GLES20.glUniform3fv(sunColLoc, 1, lighting.sunColor, 0)
        GLES20.glUniform3fv(ambLoc, 1, lighting.ambientColor, 0)
        GLES20.glUniform3fv(fogColLoc, 1, lighting.fogColor, 0)
        GLES20.glUniform1f(fogStartLoc, lighting.fogStart)
        GLES20.glUniform1f(fogEndLoc, lighting.fogEnd)
        GLES20.glUniform1f(wetnessLoc, lighting.wetness)
        GLES20.glUniform3f(camPosLoc, camera.eyeX, camera.eyeY, camera.eyeZ)

        // Project vehicle headlights onto road
        val hRad = Math.toRadians(vehicle.headingAngleDeg.toDouble()).toFloat()
        val hDirX = sin(hRad)
        val hDirZ = cos(hRad)

        GLES20.glUniform3f(headPosLoc, vehicle.worldX, vehicle.worldY + 0.65f, vehicle.worldZ)
        GLES20.glUniform3f(headDirLoc, hDirX, -0.10f, hDirZ)
        GLES20.glUniform3f(headColLoc, 1.0f, 0.98f, 0.92f)
        GLES20.glUniform1f(headEnLoc, if (vehicle.headlightsOn) 1.0f else 0.0f)
    }

    private fun drawRoadChunkBuffer(program: Int, chunk: RoadChunk) {
        val buffer = chunk.vertexBuffer
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

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, chunk.vertexCount)

        if (posLoc >= 0) GLES20.glDisableVertexAttribArray(posLoc)
        if (normLoc >= 0) GLES20.glDisableVertexAttribArray(normLoc)
        if (colLoc >= 0) GLES20.glDisableVertexAttribArray(colLoc)
    }
}
