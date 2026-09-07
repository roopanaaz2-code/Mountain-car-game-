package com.example.ui

import android.annotation.SuppressLint
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.CameraMode
import com.example.model.ControlType
import com.example.model.Gear
import com.example.model.TransmissionType
import kotlin.math.PI
import kotlin.math.atan2

@SuppressLint("ClickableViewAccessibility")
@Composable
fun DrivingScreen(
    viewModel: GameViewModel,
    onOpenMenu: () -> Unit
) {
    val speedDisplay by viewModel.speedDisplay.collectAsState()
    val speedUnitStr by viewModel.speedUnitStr.collectAsState()
    val speedKmh by viewModel.speedKmh.collectAsState()
    val engineRpm by viewModel.engineRpm.collectAsState()
    val gearName by viewModel.currentGearName.collectAsState()
    val highwayDistKm by viewModel.highwayDistanceKm.collectAsState()
    val currentZone by viewModel.currentZoneName.collectAsState()
    val fps by viewModel.fpsCount.collectAsState()

    val isInsideCar by viewModel.isInsideVehicle.collectAsState()
    val canEnterCar by viewModel.canEnterVehicle.collectAsState()
    val cameraMode by viewModel.activeCameraMode.collectAsState()

    val headlightsOn by viewModel.isHeadlightsOn.collectAsState()
    val highBeam by viewModel.isHighBeam.collectAsState()
    val blinkerL by viewModel.isBlinkerLeft.collectAsState()
    val blinkerR by viewModel.isBlinkerRight.collectAsState()
    val hazard by viewModel.isHazard.collectAsState()
    val wiperOn by viewModel.isWiperOn.collectAsState()

    val radioTitle by viewModel.radioTitle.collectAsState()
    val isRadioPlaying by viewModel.isRadioPlaying.collectAsState()

    val controlType by viewModel.controlType.collectAsState()
    val transmissionType by viewModel.transmissionType.collectAsState()

    var wheelAngle by remember { mutableFloatStateOf(0f) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. OpenGL 3D Surface View
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        viewModel.onCameraTouchDrag(dragAmount.x, dragAmount.y)
                    }
                },
            factory = { ctx ->
                GLSurfaceView(ctx).apply {
                    setEGLContextClientVersion(2)
                    setRenderer(viewModel.renderer)
                    renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                }
            }
        )

        // 2. Cockpit Animated Windshield Wiper Sweep (visible in DRIVER camera mode)
        if (isInsideCar && cameraMode == CameraMode.DRIVER && wiperOn) {
            val wiperAngle = kotlin.math.sin(viewModel.renderer.vehicle.wiperPhase) * 45f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 20.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Canvas(
                    modifier = Modifier
                        .size(360.dp, 160.dp)
                        .rotate(wiperAngle)
                ) {
                    drawLine(
                        color = Color(0x99111111),
                        start = Offset(size.width * 0.5f, size.height),
                        end = Offset(size.width * 0.5f, 20f),
                        strokeWidth = 10f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color(0xBB333333),
                        start = Offset(size.width * 0.5f - 24f, 20f),
                        end = Offset(size.width * 0.5f + 24f, 20f),
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        // 3. TOP TELEMETRY HUD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Speedometer & Tachometer Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x770D1B2A)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Digital Speed
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = speedDisplay,
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = speedUnitStr,
                            color = Color(0xFF90CAF9),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Divider(
                        modifier = Modifier
                            .height(38.dp)
                            .width(1.dp),
                        color = Color(0x33FFFFFF)
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    // Gear & RPM Gauge
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = if (gearName == "R") Color(0xFFE53935) else Color(0xFFFF9800),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "GEAR $gearName",
                                    color = Color.Black,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = String.format("%.0f RPM", engineRpm),
                                color = if (engineRpm > 6500f) Color(0xFFFF5252) else Color(0xFFE0E0E0),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // RPM Bar
                        val rpmFraction = (engineRpm / 8000f).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0x44FFFFFF))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(rpmFraction)
                                    .background(
                                        if (rpmFraction > 0.8f) Color(0xFFFF5252) else Color(0xFF4CAF50)
                                    )
                            )
                        }
                    }
                }
            }

            // Center: Mini Radio Pill & Road Landmark
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Radio Quick Player
                Surface(
                    color = Color(0x881B263B),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radio,
                            contentDescription = "Radio",
                            tint = Color(0xFFFFB74D),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = radioTitle,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { viewModel.toggleRadioPlay() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (isRadioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.nextRadioStation() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next Station",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Highway Milepost & Active Alpine Zone
                Surface(
                    color = Color(0x77000000),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = currentZone,
                            color = Color(0xFFFFD54F),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = String.format("%.2f / 8.00 km  •  %d FPS", highwayDistKm, fps),
                            color = Color(0xFFCFD8DC),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Top Right Quick Controls: Camera, Exit/Enter Vehicle, Menu
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Camera Mode Switcher
                Surface(
                    color = Color(0x88263238),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(44.dp)
                        .border(1.dp, Color(0x44FFFFFF), CircleShape)
                        .clickable { viewModel.cycleCamera() }
                        .testTag("camera_switch_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Camera: ${cameraMode.displayName}",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Wiper toggle (in car)
                if (isInsideCar) {
                    Surface(
                        color = if (wiperOn) Color(0xFF0288D1) else Color(0x88263238),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(44.dp)
                            .border(1.dp, Color(0x44FFFFFF), CircleShape)
                            .clickable { viewModel.toggleWiper() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = "Wipers",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // ENTER / EXIT VEHICLE BUTTON
                if (isInsideCar) {
                    // Exit vehicle button (available when stopped or very slow)
                    val canExit = speedKmh < 8f
                    Surface(
                        color = if (canExit) Color(0xFF388E3C) else Color(0x44455A64),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(12.dp))
                            .clickable(enabled = canExit) { viewModel.toggleEnterExitVehicle() }
                            .testTag("exit_vehicle_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsWalk,
                                contentDescription = "Exit Car",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "EXIT CAR",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    // Enter vehicle button (visible when close to car)
                    AnimatedVisibility(visible = canEnterCar, enter = fadeIn(), exit = fadeOut()) {
                        Surface(
                            color = Color(0xFFFF9800),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(44.dp)
                                .border(2.dp, Color.White, RoundedCornerShape(12.dp))
                                .clickable { viewModel.toggleEnterExitVehicle() }
                                .testTag("enter_vehicle_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = "Enter Car",
                                    tint = Color.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ENTER CAR",
                                    color = Color.Black,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }

                // Reset / Respawn car button
                Surface(
                    color = Color(0x88263238),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(44.dp)
                        .border(1.dp, Color(0x44FFFFFF), CircleShape)
                        .clickable { viewModel.respawnVehicle() }
                        .testTag("respawn_vehicle_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Car",
                            tint = Color(0xFFFFB74D),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Pause / Main Menu button
                Surface(
                    color = Color(0x88263238),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(44.dp)
                        .border(1.dp, Color(0x44FFFFFF), CircleShape)
                        .clickable(onClick = onOpenMenu)
                        .testTag("pause_menu_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // 4. BOTTOM CONTROLS OVERLAY (DRIVING OR ON-FOOT)
        if (isInsideCar) {
            // --- IN-CAR DRIVING CONTROLS ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // LEFT SIDE: STEERING & LIGHTS
                Column(horizontalAlignment = Alignment.Start) {
                    // Blinker & Hazard Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        ControlChipButton(
                            icon = Icons.Default.ArrowBack,
                            label = "L",
                            active = blinkerL,
                            activeColor = Color(0xFFFFB300),
                            onClick = { viewModel.toggleLeftBlinker() }
                        )
                        ControlChipButton(
                            icon = Icons.Default.Warning,
                            label = "HAZARD",
                            active = hazard,
                            activeColor = Color(0xFFFF3D00),
                            onClick = { viewModel.toggleHazard() }
                        )
                        ControlChipButton(
                            icon = Icons.Default.ArrowForward,
                            label = "R",
                            active = blinkerR,
                            activeColor = Color(0xFFFFB300),
                            onClick = { viewModel.toggleRightBlinker() }
                        )
                        ControlChipButton(
                            icon = Icons.Default.Highlight,
                            label = if (highBeam) "HIGH" else if (headlightsOn) "LOW" else "OFF",
                            active = headlightsOn,
                            activeColor = if (highBeam) Color(0xFF2979FF) else Color(0xFFFFD54F),
                            onClick = { viewModel.toggleHeadlights() }
                        )
                    }

                    // STEERING INPUT
                    when (controlType) {
                        ControlType.STEERING_WHEEL -> {
                            // Interactive On-Screen Steering Wheel
                            Box(
                                modifier = Modifier
                                    .size(150.dp)
                                    .clip(CircleShape)
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragEnd = {
                                                wheelAngle = 0f
                                                viewModel.setSteerInput(0f)
                                            },
                                            onDragCancel = {
                                                wheelAngle = 0f
                                                viewModel.setSteerInput(0f)
                                            }
                                        ) { change, dragAmount ->
                                            change.consume()
                                            // Rotate wheel
                                            val newAngle = (wheelAngle + dragAmount.x * 1.5f).coerceIn(-130f, 130f)
                                            wheelAngle = newAngle
                                            viewModel.setSteerInput(newAngle / 130f)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                // Wheel Visual
                                Canvas(modifier = Modifier.fillMaxSize().rotate(wheelAngle)) {
                                    val r = size.minDimension * 0.46f
                                    val center = Offset(size.width * 0.5f, size.height * 0.5f)
                                    // Rim
                                    drawCircle(
                                        color = Color(0xDD212121),
                                        radius = r,
                                        center = center,
                                        style = Stroke(width = 24f)
                                    )
                                    drawCircle(
                                        color = Color(0xFF424242),
                                        radius = r - 12f,
                                        center = center,
                                        style = Stroke(width = 4f)
                                    )
                                    // Spokes
                                    drawLine(Color(0xFF757575), center, Offset(center.x - r, center.y), strokeWidth = 14f)
                                    drawLine(Color(0xFF757575), center, Offset(center.x + r, center.y), strokeWidth = 14f)
                                    drawLine(Color(0xFF757575), center, Offset(center.x, center.y + r), strokeWidth = 14f)
                                    // Hub
                                    drawCircle(Color(0xFF263238), radius = 26f, center = center)
                                }
                            }
                        }

                        ControlType.BUTTONS -> {
                            // Left & Right Arrow Buttons
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                DirectionalTouchButton(
                                    icon = Icons.Default.ArrowBack,
                                    onPress = { viewModel.setSteerInput(-1f) },
                                    onRelease = { viewModel.setSteerInput(0f) }
                                )
                                DirectionalTouchButton(
                                    icon = Icons.Default.ArrowForward,
                                    onPress = { viewModel.setSteerInput(1f) },
                                    onRelease = { viewModel.setSteerInput(0f) }
                                )
                            }
                        }

                        ControlType.GYROSCOPE -> {
                            // Gyroscope / Tilt Mode Indicator
                            Surface(
                                color = Color(0x66000000),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ScreenRotation,
                                        contentDescription = "Tilt Steering Active",
                                        tint = Color(0xFF64B5F6),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Tilt Phone to Steer",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // RIGHT SIDE: PEDALS, GEAR SHIFTER & HORN
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Horn Button
                    PedalButton(
                        label = "HORN",
                        icon = Icons.Default.VolumeUp,
                        color = Color(0xFF455A64),
                        width = 54.dp,
                        height = 54.dp,
                        onPress = { viewModel.setHorn(true) },
                        onRelease = { viewModel.setHorn(false) }
                    )

                    // Handbrake Toggle
                    val handbrakeOn = viewModel.renderer.vehicle.handbrake
                    Surface(
                        color = if (handbrakeOn) Color(0xFFD32F2F) else Color(0x7737474F),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .size(54.dp)
                            .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(12.dp))
                            .clickable { viewModel.toggleHandbrake() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "(P)",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // Transmission Shifter (Auto P/R/N/D or Manual Up/Down)
                    if (transmissionType == TransmissionType.AUTOMATIC) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            listOf(Gear.PARK, Gear.REVERSE, Gear.NEUTRAL, Gear.FIRST).forEach { g ->
                                val isSelected = viewModel.renderer.vehicle.currentGear == g || (g == Gear.FIRST && viewModel.renderer.vehicle.currentGear != Gear.PARK && viewModel.renderer.vehicle.currentGear != Gear.REVERSE && viewModel.renderer.vehicle.currentGear != Gear.NEUTRAL)
                                val label = if (g == Gear.FIRST) "D" else g.displayName
                                Surface(
                                    color = if (isSelected) Color(0xFFFF9800) else Color(0x66263238),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .size(34.dp, 26.dp)
                                        .clickable { viewModel.shiftGear(g) }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Manual Shifter +/-
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                color = Color(0x88263238),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .size(44.dp, 36.dp)
                                    .clickable { viewModel.shiftManualUp() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Shift Up", tint = Color.White)
                                }
                            }
                            Surface(
                                color = Color(0x88263238),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .size(44.dp, 36.dp)
                                    .clickable { viewModel.shiftManualDown() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Shift Down", tint = Color.White)
                                }
                            }
                        }
                    }

                    // BRAKE PEDAL
                    PedalButton(
                        label = "BRAKE",
                        icon = Icons.Default.VerticalAlignBottom,
                        color = Color(0xFFC62828),
                        width = 64.dp,
                        height = 110.dp,
                        onPress = { viewModel.setBrake(1.0f) },
                        onRelease = { viewModel.setBrake(0f) }
                    )

                    // NITRO (N2O) BOOST PEDAL
                    val isNitro by viewModel.isNitroActive.collectAsState()
                    PedalButton(
                        label = "N2O",
                        icon = Icons.Default.Bolt,
                        color = if (isNitro) Color(0xFF00E5FF) else Color(0xFF0097A7),
                        width = 56.dp,
                        height = 95.dp,
                        onPress = { viewModel.setNitro(true) },
                        onRelease = { viewModel.setNitro(false) }
                    )

                    // ACCELERATOR / GAS PEDAL
                    PedalButton(
                        label = "GAS",
                        icon = Icons.Default.Speed,
                        color = Color(0xFF2E7D32),
                        width = 68.dp,
                        height = 135.dp,
                        onPress = { viewModel.setThrottle(1.0f) },
                        onRelease = { viewModel.setThrottle(0f) }
                    )
                }
            }
        } else {
            // --- ON-FOOT WALKING CONTROLS ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 32.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Virtual Walk D-Pad
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    DirectionalTouchButton(
                        icon = Icons.Default.KeyboardArrowUp,
                        onPress = { viewModel.setWalkerMove(1f, 0f) },
                        onRelease = { viewModel.setWalkerMove(0f, 0f) }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        DirectionalTouchButton(
                            icon = Icons.Default.KeyboardArrowLeft,
                            onPress = { viewModel.setWalkerMove(0f, -1f) },
                            onRelease = { viewModel.setWalkerMove(0f, 0f) }
                        )
                        DirectionalTouchButton(
                            icon = Icons.Default.KeyboardArrowDown,
                            onPress = { viewModel.setWalkerMove(-1f, 0f) },
                            onRelease = { viewModel.setWalkerMove(0f, 0f) }
                        )
                        DirectionalTouchButton(
                            icon = Icons.Default.KeyboardArrowRight,
                            onPress = { viewModel.setWalkerMove(0f, 1f) },
                            onRelease = { viewModel.setWalkerMove(0f, 0f) }
                        )
                    }
                }

                // Look Yaw / Rotate View Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    DirectionalTouchButton(
                        icon = Icons.Default.RotateLeft,
                        onPress = { viewModel.setWalkerLook(-1f) },
                        onRelease = { viewModel.setWalkerLook(0f) }
                    )
                    DirectionalTouchButton(
                        icon = Icons.Default.RotateRight,
                        onPress = { viewModel.setWalkerLook(1f) },
                        onRelease = { viewModel.setWalkerLook(0f) }
                    )
                }
            }
        }
    }
}

@Composable
fun ControlChipButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Surface(
        color = if (active) activeColor else Color(0x66263238),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .height(38.dp)
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) Color.Black else Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = if (active) Color.Black else Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DirectionalTouchButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Surface(
        color = if (isPressed) Color(0xFFFF9800) else Color(0x77263238),
        shape = CircleShape,
        modifier = Modifier
            .size(56.dp)
            .border(1.5.dp, Color(0x44FFFFFF), CircleShape)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull()
                        if (change != null) {
                            if (change.pressed && !isPressed) {
                                isPressed = true
                                onPress()
                            } else if (!change.pressed && isPressed) {
                                isPressed = false
                                onRelease()
                            }
                        }
                    }
                }
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = "Direction",
                tint = if (isPressed) Color.Black else Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun PedalButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Surface(
        color = if (isPressed) color else color.copy(alpha = 0.55f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .size(width, height)
            .border(2.dp, if (isPressed) Color.White else Color(0x44FFFFFF), RoundedCornerShape(14.dp))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull()
                        if (change != null) {
                            if (change.pressed && !isPressed) {
                                isPressed = true
                                onPress()
                            } else if (!change.pressed && isPressed) {
                                isPressed = false
                                onRelease()
                            }
                        }
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}
