package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*

@Composable
fun SettingsScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val quality by viewModel.graphicsQuality.collectAsState()
    val controlType by viewModel.controlType.collectAsState()
    val transmission by viewModel.transmissionType.collectAsState()
    val sensitivity by viewModel.controlSensitivity.collectAsState()
    val speedUnit by viewModel.speedUnit.collectAsState()
    val timeOfDay by viewModel.timeOfDay.collectAsState()
    val weather by viewModel.weather.collectAsState()

    var masterVol by remember { mutableFloatStateOf(viewModel.repository.masterVolume) }
    var engineVol by remember { mutableFloatStateOf(viewModel.repository.engineVolume) }
    var ambienceVol by remember { mutableFloatStateOf(viewModel.repository.ambienceVolume) }
    var musicVol by remember { mutableFloatStateOf(viewModel.repository.musicVolume) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F1722), Color(0xFF1B263B), Color(0xFF0D1B2A))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .border(1.dp, Color(0x44FFFFFF), CircleShape)
                        .testTag("settings_back_button")
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Game Settings",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Graphics, steering, audio, and environment options",
                        color = Color(0xFF90CAF9),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Two-column layout in landscape
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Left Column: Graphics & Driving Controls
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // GRAPHICS PRESET
                    SettingsSectionCard("GRAPHICS & PERFORMANCE") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                GraphicsQuality.values().forEach { gq ->
                                    val isSel = quality == gq
                                    Surface(
                                        color = if (isSel) Color(0xFFFF9800) else Color(0x44263238),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .clickable { viewModel.updateGraphicsQuality(gq) }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = gq.displayName,
                                                color = if (isSel) Color.Black else Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                            Text(
                                text = "View distance: ${quality.renderDistance.toInt()}m  •  Optimized for ${if (quality == GraphicsQuality.LOW) "Low-end devices" else "Smooth 60 FPS"}",
                                color = Color(0xFF90A4AE),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // CONTROLS & TRANSMISSION
                    SettingsSectionCard("STEERING & TRANSMISSION") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Control Type
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ControlType.values().forEach { ct ->
                                    val isSel = controlType == ct
                                    Surface(
                                        color = if (isSel) Color(0xFFFF9800) else Color(0x44263238),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .clickable { viewModel.updateControlType(ct) }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = ct.displayName,
                                                color = if (isSel) Color.Black else Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // Sensitivity Slider
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Steering Sensitivity", color = Color.White, fontSize = 11.sp)
                                    Text(String.format("%.1fx", sensitivity), color = Color(0xFF90CAF9), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = sensitivity,
                                    onValueChange = { viewModel.updateSensitivity(it) },
                                    valueRange = 0.5f..2.0f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFFF9800),
                                        activeTrackColor = Color(0xFFFF9800)
                                    )
                                )
                            }

                            // Transmission & Speed Unit
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Transmission
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Transmission", color = Color(0xFF90A4AE), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TransmissionType.values().forEach { tr ->
                                            val isSel = transmission == tr
                                            Surface(
                                                color = if (isSel) Color(0xFF64B5F6) else Color(0x44263238),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(30.dp)
                                                    .clickable { viewModel.updateTransmission(tr) }
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(tr.displayName, color = if (isSel) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }

                                // Speed Unit
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Speed Units", color = Color(0xFF90A4AE), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        SpeedUnit.values().forEach { su ->
                                            val isSel = speedUnit == su
                                            Surface(
                                                color = if (isSel) Color(0xFF64B5F6) else Color(0x44263238),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(30.dp)
                                                    .clickable { viewModel.updateSpeedUnit(su) }
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(su.displayName, color = if (isSel) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Right Column: Audio & Environment
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // AUDIO VOLUMES
                    SettingsSectionCard("AUDIO & PROCEDURAL SOUND") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            AudioVolumeSlider("Master Volume", masterVol) {
                                masterVol = it
                                viewModel.updateVolumes(masterVol, engineVol, ambienceVol, musicVol)
                            }
                            AudioVolumeSlider("Engine & Exhaust", engineVol) {
                                engineVol = it
                                viewModel.updateVolumes(masterVol, engineVol, ambienceVol, musicVol)
                            }
                            AudioVolumeSlider("Mountain Nature & Wind", ambienceVol) {
                                ambienceVol = it
                                viewModel.updateVolumes(masterVol, engineVol, ambienceVol, musicVol)
                            }
                            AudioVolumeSlider("Radio & Music", musicVol) {
                                musicVol = it
                                viewModel.updateVolumes(masterVol, engineVol, ambienceVol, musicVol)
                            }
                        }
                    }

                    // ENVIRONMENT PRESETS
                    SettingsSectionCard("TIME OF DAY & WEATHER") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Time of Day", color = Color(0xFF90A4AE), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(TimeOfDay.DYNAMIC, TimeOfDay.SUNRISE, TimeOfDay.AFTERNOON, TimeOfDay.SUNSET, TimeOfDay.NIGHT).forEach { t ->
                                    val isSel = timeOfDay == t
                                    Surface(
                                        color = if (isSel) Color(0xFFFFB74D) else Color(0x44263238),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(30.dp)
                                            .clickable { viewModel.setTimeOfDay(t) }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(t.displayName, color = if (isSel) Color.Black else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            Text("Weather Condition", color = Color(0xFF90A4AE), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                WeatherType.values().forEach { w ->
                                    val isSel = weather == w
                                    Surface(
                                        color = if (isSel) Color(0xFF64B5F6) else Color(0x44263238),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(32.dp)
                                            .clickable { viewModel.setWeather(w) }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(w.displayName, color = if (isSel) Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x441E293B)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                color = Color(0xFFFF9800),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
fun AudioVolumeSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color.White, fontSize = 11.sp)
            Text("${(value * 100).toInt()}%", color = Color(0xFF90CAF9), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF64B5F6),
                activeTrackColor = Color(0xFF64B5F6)
            ),
            modifier = Modifier.height(28.dp)
        )
    }
}
