package com.example.ui

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameContent

@Composable
fun MyMusicScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val radioTitle by viewModel.radioTitle.collectAsState()
    val radioArtist by viewModel.radioArtist.collectAsState()
    val isPlaying by viewModel.isRadioPlaying.collectAsState()

    // SAF file picker for local audio
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            var fileName = "Local Audio Track"
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex >= 0) {
                        fileName = cursor.getString(nameIndex) ?: fileName
                    }
                }
            } catch (e: Exception) {
                // fallback name
            }
            viewModel.addLocalMusic(uri, fileName)
        }
    }

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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .border(1.dp, Color(0x44FFFFFF), CircleShape)
                            .testTag("music_back_button")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Alpine Car Radio & My Music",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Listen to alpine stations or select music from your device",
                            color = Color(0xFF90CAF9),
                            fontSize = 12.sp
                        )
                    }
                }

                // Import Phone Music Button
                Button(
                    onClick = { audioPickerLauncher.launch("audio/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(42.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Track", tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ADD MUSIC FROM PHONE",
                        color = Color.Black,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Two-column layout in landscape
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Left Column: Active Player Deck Card
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
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            color = Color(0x33FFFFFF),
                            shape = CircleShape,
                            modifier = Modifier.size(90.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Radio,
                                    contentDescription = "Radio Player",
                                    tint = Color(0xFFFFB74D),
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = radioTitle,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = radioArtist,
                                color = Color(0xFF90CAF9),
                                fontSize = 13.sp
                            )
                        }

                        // Playback Controls
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.prevRadioStation() },
                                modifier = Modifier
                                    .size(46.dp)
                                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
                            ) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = Color.White)
                            }

                            Surface(
                                color = Color(0xFFFF9800),
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clickable { viewModel.toggleRadioPlay() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = Color.Black,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { viewModel.nextRadioStation() },
                                modifier = Modifier
                                    .size(46.dp)
                                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
                            ) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White)
                            }
                        }
                    }
                }

                // Right Column: Station & Local Tracks List
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x441E293B)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "AVAILABLE RADIO STATIONS & LOCAL PLAYLIST",
                            color = Color(0xFFFF9800),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Preset Alpine Stations
                            itemsIndexed(GameContent.defaultRadioStations) { idx, st ->
                                val isCurrent = !viewModel.radioPlayer.isLocalMode && viewModel.radioPlayer.currentStationIndex == idx
                                Surface(
                                    color = if (isCurrent) Color(0xFF263238) else Color(0x331B263B),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            if (isCurrent) 1.5.dp else 1.dp,
                                            if (isCurrent) Color(0xFFFF9800) else Color(0x22FFFFFF),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { viewModel.radioPlayer.selectStation(idx) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = st.title,
                                            tint = if (isCurrent) Color(0xFFFFB74D) else Color(0xFF90CAF9),
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = st.title,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = st.subtitle,
                                                color = Color(0xFF90A4AE),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // Local Phone Audio Tracks
                            val localTracks = viewModel.radioPlayer.getLocalTracks()
                            if (localTracks.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "DEVICE MUSIC (${localTracks.size})",
                                        color = Color(0xFF64B5F6),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                                    )
                                }
                                itemsIndexed(localTracks) { idx, track ->
                                    val isCurrent = viewModel.radioPlayer.isLocalMode && viewModel.radioPlayer.currentLocalIndex == idx
                                    Surface(
                                        color = if (isCurrent) Color(0xFF263238) else Color(0x331B263B),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(
                                                if (isCurrent) 1.5.dp else 1.dp,
                                                if (isCurrent) Color(0xFF64B5F6) else Color(0x22FFFFFF),
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable { viewModel.radioPlayer.selectLocalTrack(idx) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Audiotrack,
                                                contentDescription = track.title,
                                                tint = Color(0xFF64B5F6),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = track.title,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1
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
