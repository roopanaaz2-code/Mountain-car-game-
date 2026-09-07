package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameContent
import com.example.model.TimeOfDay
import com.example.model.WeatherType

@Composable
fun MainMenuScreen(
    viewModel: GameViewModel,
    onStartDrive: () -> Unit,
    onOpenGarage: () -> Unit,
    onOpenCharacters: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMusic: () -> Unit
) {
    val totalKm by viewModel.totalDistanceDriven.collectAsState()
    val selectedCar by viewModel.selectedVehicle.collectAsState()
    val selectedChar by viewModel.selectedCharacter.collectAsState()
    val timeOfDay by viewModel.timeOfDay.collectAsState()
    val weather by viewModel.weather.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D1B2A),
                        Color(0xFF1B263B),
                        Color(0xFF0F1722)
                    )
                )
            )
    ) {
        // Main content in landscape
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Hero Section: Title, Stats & Environment Pills
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFFFF9800),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.padding(end = 10.dp)
                        ) {
                            Text(
                                text = "REAL ROAD 3D",
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = "8 KM EXPLORABLE HIGHWAY",
                            color = Color(0xFF90CAF9),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Mountain Drive",
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Realistic Offline Mountain Road-Trip Simulator",
                        color = Color(0xFFB0BEC5),
                        fontSize = 13.sp
                    )
                }

                // Stats & Current Loadout Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x33263238)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0x4478909C), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "EXPLORATION MILEAGE",
                                color = Color(0xFF90A4AE),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = String.format("%.1f km", totalKm),
                                color = Color(0xFF4CAF50),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Divider(
                            modifier = Modifier
                                .height(32.dp)
                                .width(1.dp),
                            color = Color(0x33FFFFFF)
                        )

                        Column {
                            Text(
                                text = "SELECTED CAR",
                                color = Color(0xFF90A4AE),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = selectedCar.name,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Divider(
                            modifier = Modifier
                                .height(32.dp)
                                .width(1.dp),
                            color = Color(0x33FFFFFF)
                        )

                        Column {
                            Text(
                                text = "EXPLORER",
                                color = Color(0xFF90A4AE),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = selectedChar.name,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Quick Environment selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Time selector chip
                    Surface(
                        color = Color(0x4437474F),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .clickable {
                                val nextTime = when (timeOfDay) {
                                    TimeOfDay.DYNAMIC -> TimeOfDay.SUNRISE
                                    TimeOfDay.SUNRISE -> TimeOfDay.MORNING
                                    TimeOfDay.MORNING -> TimeOfDay.AFTERNOON
                                    TimeOfDay.AFTERNOON -> TimeOfDay.SUNSET
                                    TimeOfDay.SUNSET -> TimeOfDay.EVENING
                                    TimeOfDay.EVENING -> TimeOfDay.NIGHT
                                    TimeOfDay.NIGHT -> TimeOfDay.DYNAMIC
                                    else -> TimeOfDay.DYNAMIC
                                }
                                viewModel.setTimeOfDay(nextTime)
                            }
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = "Time",
                                tint = Color(0xFFFFB74D),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = timeOfDay.displayName,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Weather selector chip
                    Surface(
                        color = Color(0x4437474F),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .clickable {
                                val nextWeather = when (weather) {
                                    WeatherType.CLEAR -> WeatherType.MIST
                                    WeatherType.MIST -> WeatherType.RAIN
                                    WeatherType.RAIN -> WeatherType.CLEAR
                                }
                                viewModel.setWeather(nextWeather)
                            }
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = when (weather) {
                                    WeatherType.CLEAR -> Icons.Default.Brightness5
                                    WeatherType.MIST -> Icons.Default.Cloud
                                    WeatherType.RAIN -> Icons.Default.WaterDrop
                                },
                                contentDescription = "Weather",
                                tint = Color(0xFF64B5F6),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = weather.displayName,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(28.dp))

            // Right Action Menu Grid
            Column(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.End
            ) {
                // PRIMARY PLAY BUTTON
                Button(
                    onClick = onStartDrive,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .testTag("play_drive_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Drive Highway",
                            tint = Color.Black,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "START HIGHWAY DRIVE",
                            color = Color.Black,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Secondary Navigation Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MenuTile(
                        icon = Icons.Default.DirectionsCar,
                        label = "Garage",
                        subtitle = "6 Cars & Paint",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenGarage
                    )
                    MenuTile(
                        icon = Icons.Default.Person,
                        label = "Explorer",
                        subtitle = "Alex / Elena",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenCharacters
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MenuTile(
                        icon = Icons.Default.MusicNote,
                        label = "My Music",
                        subtitle = "Radio & Local",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenMusic
                    )
                    MenuTile(
                        icon = Icons.Default.Tune,
                        label = "Settings",
                        subtitle = "Graphics & Controls",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenSettings
                    )
                }
            }
        }
    }
}

@Composable
fun MenuTile(
    icon: ImageVector,
    label: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0x44263238)),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .height(72.dp)
            .border(1.dp, Color(0x3390A4AE), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color(0x33FFFFFF),
                shape = CircleShape,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = Color(0xFF64B5F6),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = Color(0xFF90A4AE),
                    fontSize = 11.sp
                )
            }
        }
    }
}
