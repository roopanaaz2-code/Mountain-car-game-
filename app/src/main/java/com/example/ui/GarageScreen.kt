package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CarCustomization
import com.example.model.GameContent
import com.example.model.VehicleSpec

@Composable
fun GarageScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val selectedCar by viewModel.selectedVehicle.collectAsState()
    val customization by viewModel.selectedCustomization.collectAsState()
    val totalKm by viewModel.totalDistanceDriven.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Cars, 1: Paint, 2: Liveries, 3: Wheels, 4: Interior

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
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .border(1.dp, Color(0x44FFFFFF), CircleShape)
                            .testTag("garage_back_button")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Alpine Garage",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Choose and customize your mountain road car",
                            color = Color(0xFF90CAF9),
                            fontSize = 12.sp
                        )
                    }
                }

                // Mileage badge
                Surface(
                    color = Color(0x44263238),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = String.format("Mileage: %.1f km", totalKm),
                        color = Color(0xFF4CAF50),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Car Inspection & Customization Area
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Left Column: Selected Car Specs & Info
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x551E293B)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight()
                        .border(1.dp, Color(0x3390CAF9), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = Color(0xFFFF9800),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = selectedCar.category,
                                        color = Color.Black,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                val isUnlocked = viewModel.repository.isCarUnlocked(selectedCar)
                                if (isUnlocked) {
                                    Text(
                                        text = "UNLOCKED",
                                        color = Color(0xFF4CAF50),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Text(
                                        text = "LOCKED (${selectedCar.unlockKm.toInt()} km)",
                                        color = Color(0xFFFF5252),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = selectedCar.name,
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = selectedCar.description,
                                color = Color(0xFFB0BEC5),
                                fontSize = 11.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Performance Stat Bars
                            PerformanceStatBar("Top Speed", "${selectedCar.topSpeedKmh} km/h", (selectedCar.topSpeedKmh / 360f))
                            PerformanceStatBar("0-100 km/h", "${selectedCar.acceleration0To100}s", (1f - (selectedCar.acceleration0To100 / 9f)).coerceIn(0f, 1f))
                            PerformanceStatBar("Handling", "${(selectedCar.handling * 100).toInt()}%", selectedCar.handling)
                            PerformanceStatBar("Braking", "${(selectedCar.braking * 100).toInt()}%", selectedCar.braking)
                        }

                        // Select / Unlock Button
                        val isUnlocked = viewModel.repository.isCarUnlocked(selectedCar)
                        Button(
                            onClick = {
                                if (isUnlocked) {
                                    viewModel.selectVehicle(selectedCar)
                                    onBack()
                                }
                            },
                            enabled = isUnlocked,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF9800),
                                disabledContainerColor = Color(0x44455A64)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("select_car_button")
                        ) {
                            Text(
                                text = if (isUnlocked) "DRIVE THIS CAR" else "DRIVE ${selectedCar.unlockKm.toInt()} KM TO UNLOCK",
                                color = if (isUnlocked) Color.Black else Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                // Right Column: Customization Controls & Vehicle Selector
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Customization Tabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Vehicles", "Paint Color", "Liveries", "Wheels", "Interior").forEachIndexed { index, tabName ->
                            val isSelected = activeTab == index
                            Surface(
                                color = if (isSelected) Color(0xFFFF9800) else Color(0x44263238),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clickable { activeTab = index }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = tabName,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Tab Content Body
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x441E293B)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 8.dp)
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            when (activeTab) {
                                0 -> {
                                    // Vehicle Selector
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        items(GameContent.vehicles) { v ->
                                            val isSelected = v.id == selectedCar.id
                                            val isUnlocked = viewModel.repository.isCarUnlocked(v)
                                            Card(
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) Color(0xFF263238) else Color(0x441E293B)
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .width(140.dp)
                                                    .fillMaxHeight()
                                                    .border(
                                                        if (isSelected) 2.dp else 1.dp,
                                                        if (isSelected) Color(0xFFFF9800) else Color(0x33FFFFFF),
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                    .clickable { viewModel.selectVehicle(v) }
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(10.dp),
                                                    verticalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.DirectionsCar,
                                                        contentDescription = v.name,
                                                        tint = if (isUnlocked) Color(0xFF64B5F6) else Color.Gray,
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                    Column {
                                                        Text(
                                                            text = v.name,
                                                            color = Color.White,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = if (isUnlocked) "Ready" else "Locked",
                                                            color = if (isUnlocked) Color(0xFF4CAF50) else Color(0xFFFF5252),
                                                            fontSize = 10.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                1 -> {
                                    // Paint Colors Palette
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        items(GameContent.paintColors.indices.toList()) { idx ->
                                            val p = GameContent.paintColors[idx]
                                            val isSel = customization.paintColorIndex == idx
                                            val col = Color(
                                                red = p.rgbFloats[0],
                                                green = p.rgbFloats[1],
                                                blue = p.rgbFloats[2]
                                            )
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.clickable {
                                                    viewModel.updateCustomization(customization.copy(paintColorIndex = idx))
                                                }
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(54.dp)
                                                        .clip(CircleShape)
                                                        .background(col)
                                                        .border(
                                                            if (isSel) 3.dp else 1.dp,
                                                            if (isSel) Color.White else Color(0x44FFFFFF),
                                                            CircleShape
                                                        )
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = p.name,
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }

                                2 -> {
                                    // Liveries
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        GameContent.liveries.forEachIndexed { idx, option ->
                                            val isSel = customization.liveryIndex == idx
                                            Surface(
                                                color = if (isSel) Color(0xFF263238) else Color(0x441E293B),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(70.dp)
                                                    .border(
                                                        if (isSel) 2.dp else 1.dp,
                                                        if (isSel) Color(0xFFFF9800) else Color(0x33FFFFFF),
                                                        RoundedCornerShape(10.dp)
                                                    )
                                                    .clickable {
                                                        viewModel.updateCustomization(customization.copy(liveryIndex = idx))
                                                    }
                                            ) {
                                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(6.dp)) {
                                                    Text(
                                                        text = option.name,
                                                        color = Color.White,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                3 -> {
                                    // Wheels
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        GameContent.wheelDesigns.forEachIndexed { idx, option ->
                                            val isSel = customization.wheelIndex == idx
                                            Surface(
                                                color = if (isSel) Color(0xFF263238) else Color(0x441E293B),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(70.dp)
                                                    .border(
                                                        if (isSel) 2.dp else 1.dp,
                                                        if (isSel) Color(0xFFFF9800) else Color(0x33FFFFFF),
                                                        RoundedCornerShape(10.dp)
                                                    )
                                                    .clickable {
                                                        viewModel.updateCustomization(customization.copy(wheelIndex = idx))
                                                    }
                                            ) {
                                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(6.dp)) {
                                                    Text(
                                                        text = option.name,
                                                        color = Color.White,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                4 -> {
                                    // Interior
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        GameContent.interiorTrims.forEachIndexed { idx, trim ->
                                            val isSel = customization.interiorIndex == idx
                                            Surface(
                                                color = if (isSel) Color(0xFF263238) else Color(0x441E293B),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(70.dp)
                                                    .border(
                                                        if (isSel) 2.dp else 1.dp,
                                                        if (isSel) Color(0xFFFF9800) else Color(0x33FFFFFF),
                                                        RoundedCornerShape(10.dp)
                                                    )
                                                    .clickable {
                                                        viewModel.updateCustomization(customization.copy(interiorIndex = idx))
                                                    }
                                            ) {
                                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(6.dp)) {
                                                    Text(
                                                        text = trim.name,
                                                        color = Color.White,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
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
    }
}

@Composable
fun PerformanceStatBar(label: String, value: String, fraction: Float) {
    Column(modifier = Modifier.padding(vertical = 3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color(0xFF90A4AE), fontSize = 11.sp)
            Text(value, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(2.5.dp))
                .background(Color(0x33FFFFFF))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .background(Color(0xFFFF9800))
            )
        }
    }
}
