package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
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
import com.example.model.GameContent

@Composable
fun CharacterSelectScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val selectedChar by viewModel.selectedCharacter.collectAsState()

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
                .padding(horizontal = 32.dp, vertical = 20.dp)
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
                        .testTag("char_back_button")
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Select Explorer",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Your driver & on-foot mountain traveler",
                        color = Color(0xFF90CAF9),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Characters Cards
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                GameContent.characters.forEach { charSpec ->
                    val isSelected = charSpec.id == selectedChar.id
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF263238) else Color(0x441E293B)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(
                                if (isSelected) 2.5.dp else 1.dp,
                                if (isSelected) Color(0xFFFF9800) else Color(0x33FFFFFF),
                                RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                viewModel.selectCharacter(charSpec)
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = if (isSelected) Color(0xFFFF9800) else Color(0x33FFFFFF),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = charSpec.gender.uppercase(),
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = Color(0xFFFF9800),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = charSpec.name,
                                    tint = if (isSelected) Color(0xFF64B5F6) else Color.LightGray,
                                    modifier = Modifier.size(64.dp)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = charSpec.name,
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "Alpine Explorer • ${charSpec.gender}",
                                    color = Color(0xFF90CAF9),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = charSpec.description,
                                    color = Color(0xFFCFD8DC),
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            }

                            Button(
                                onClick = {
                                    viewModel.selectCharacter(charSpec)
                                    onBack()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) Color(0xFFFF9800) else Color(0x66455A64)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text(
                                    text = if (isSelected) "CHOSEN EXPLORER" else "SELECT ${charSpec.name.uppercase()}",
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 13.sp,
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
