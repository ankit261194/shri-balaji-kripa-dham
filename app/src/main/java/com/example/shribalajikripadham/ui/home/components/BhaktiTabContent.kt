package com.example.shribalajikripadham.ui.home.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shribalajikripadham.data.sacred.SACRED_TRACKS
import com.example.shribalajikripadham.data.sacred.SacredTrack
import com.example.shribalajikripadham.service.BhajanAudioService
import com.example.shribalajikripadham.theme.SacredTheme

/**
 * Pro-Tier Dedicated Bhakti & Aarti Tab Content.
 * Clean, frictionless player for 15 sacred hymns, chalisas, and aartis.
 */
@Composable
fun BhaktiTabContent(
    isHindi: Boolean,
    currentTheme: SacredTheme,
    onNavigateToGranth: () -> Unit,
    onViewLyrics: (SacredTrack) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { com.example.shribalajikripadham.data.repository.AshramRepository(context) }
    var tracksList by remember { mutableStateOf<List<SacredTrack>>(SACRED_TRACKS) }
    val currentTrackIdx by BhajanAudioService.currentTrackIndex.collectAsState()
    val isAudioPlaying by BhajanAudioService.isPlaying.collectAsState()
    val isAudioBuffering by BhajanAudioService.isBuffering.collectAsState()

    LaunchedEffect(Unit) {
        val localTracks = repository.getSacredTracks(publishedOnly = true)
        if (localTracks.isNotEmpty()) {
            tracksList = localTracks
        }
        try {
            val (ok, remoteList) = repository.syncSacredTracksFromHostinger(admin = false)
            if (ok && remoteList.isNotEmpty()) {
                tracksList = remoteList
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Top Banner for Sacred Granth
        Card(
            colors = CardDefaults.cardColors(containerColor = currentTheme.surfaceLight),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, currentTheme.cardBorderColor.copy(alpha = 0.6f)),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToGranth() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = currentTheme.primaryColor.copy(alpha = 0.10f),
                        shape = CircleShape,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("📖", fontSize = 20.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isHindi) "श्री पावन ग्रन्थ व स्तोत्र" else "Sacred Scriptures & Stotras",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = currentTheme.primaryColor
                        )
                        Text(
                            text = if (isHindi) "चालीसा, बाण, नामावली व कवच" else "Chalisa, Baan, Kavach",
                            fontSize = 11.5.sp,
                            color = Color.Gray
                        )
                    }
                }

                Text(
                    text = "➔",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = currentTheme.primaryColor
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = if (isHindi) "पावन नित्य आरती व भजन (${tracksList.size} पाठ)" else "Daily Aartis & Bhajans (${tracksList.size} Tracks)",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = currentTheme.primaryColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Track list
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            tracksList.forEachIndexed { index, track ->
                val isThisTrackActive = currentTrackIdx == index
                val isPlayingNow = isThisTrackActive && isAudioPlaying

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isThisTrackActive) currentTheme.primaryColor.copy(alpha = 0.08f) else currentTheme.surfaceLight
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        if (isThisTrackActive) 1.2.dp else 0.8.dp,
                        if (isThisTrackActive) currentTheme.primaryColor else currentTheme.cardBorderColor.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Play / Pause Circle
                            Surface(
                                color = if (isPlayingNow) currentTheme.primaryColor else currentTheme.primaryColor.copy(alpha = 0.12f),
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clickable {
                                        if (isThisTrackActive) {
                                            BhajanAudioService.togglePlayPause(context)
                                        } else {
                                            BhajanAudioService.playTrack(
                                                context = context,
                                                trackIndex = index,
                                                title = if (isHindi) track.titleHindi else track.titleEnglish,
                                                artist = "श्री बालाजी कृपा धाम",
                                                audioUrl = track.audioUrl,
                                                trackKey = track.trackKey
                                            )
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isThisTrackActive && isAudioBuffering) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = Color.White
                                        )
                                    } else {
                                        Text(
                                            text = if (isPlayingNow) "⏸" else "▶",
                                            fontSize = 14.sp,
                                            color = if (isPlayingNow) Color.White else currentTheme.primaryColor
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = if (isHindi) track.titleHindi else track.titleEnglish,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isThisTrackActive) currentTheme.primaryColor else Color(0xFF1F2937),
                                    maxLines = 1
                                )
                                Text(
                                    text = track.subtitleHindi,
                                    fontSize = 10.5.sp,
                                    color = Color.Gray,
                                    maxLines = 1
                                )
                            }
                        }

                        // Lyrics Button
                        TextButton(
                            onClick = { onViewLyrics(track) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isHindi) "पाठ देखें" else "Lyrics",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = currentTheme.primaryColor
                            )
                        }
                    }
                }
            }
        }
    }
}
