package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.data.MatchCard
import java.time.Instant

@Composable
fun LiveIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(Color.Red.copy(alpha = alpha))
    )
}

@Composable
fun MatchCardList(matchCards: List<MatchCard>, onCardClick: (MatchCard) -> Unit) {
    if (matchCards.isEmpty()) return
    
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Upcoming Matches", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.heightIn(max = 600.dp),
            userScrollEnabled = false
        ) {
            items(matchCards.take(4)) { card ->
                val now = Instant.now()
                val start = parseInstantSafely(card.startDateTime)
                val end = parseInstantSafely(card.endDateTime)
                val statusText = when {
                    start == null || end == null -> "NOT STARTED"
                    now.isBefore(start) -> "NOT STARTED"
                    now.isAfter(end) -> "MATCH ENDED"
                    else -> "LIVE"
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCardClick(card) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(card.competition, style = MaterialTheme.typography.labelSmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SubcomposeAsyncImage(model = card.homeLogo, contentDescription = null, modifier = Modifier.size(32.dp))
                            Text(" vs ", style = MaterialTheme.typography.bodySmall)
                            SubcomposeAsyncImage(model = card.awayLogo, contentDescription = null, modifier = Modifier.size(32.dp))
                        }
                        Text("${card.homeTeam} vs ${card.awayTeam}", style = MaterialTheme.typography.bodySmall)
                        Text(card.startTime, style = MaterialTheme.typography.labelMedium)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (statusText == "LIVE") {
                                LiveIndicator()
                            }
                            Text(statusText, style = MaterialTheme.typography.labelSmall, color = if (statusText == "LIVE") Color.Red else MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

private fun parseInstantSafely(dateTimeStr: String?): java.time.Instant? {
    if (dateTimeStr.isNullOrBlank()) return null
    return try {
        java.time.Instant.parse(dateTimeStr)
    } catch (e: Exception) {
        try {
            var normalized = dateTimeStr.trim().replace(" ", "T")
            if (!normalized.endsWith("Z") && !normalized.contains("+") && !normalized.contains("-")) {
                normalized += "Z"
            }
            java.time.Instant.parse(normalized)
        } catch (ex: Exception) {
            null
        }
    }
}
