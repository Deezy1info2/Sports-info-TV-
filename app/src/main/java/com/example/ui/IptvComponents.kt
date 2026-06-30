package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.ui.components.*

@Composable
fun ChannelItemCard(
    channelId: Int,
    isFavorite: Boolean,
    channelName: String,
    channelLogo: String?,
    groupName: String?,
    isPlaying: Boolean,
    onClick: () -> Unit,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isPlaying) Color(0xFFD0BCFF).copy(alpha = 0.4f) else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) Color(0xFFD0BCFF).copy(alpha = 0.15f) else Color(0xFF2B2930).copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular channel logo or fallback
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                if (!channelLogo.isNullOrEmpty()) {
                    SubcomposeAsyncImage(
                        model = channelLogo,
                        contentDescription = "Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 1.5.dp)
                            }
                        },
                        error = {
                            FallbackChannelText(text = channelName)
                        }
                    )
                } else {
                    FallbackChannelText(text = channelName)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channelName,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = if (isPlaying) MaterialTheme.colorScheme.primary else Color.White,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (!groupName.isNullOrEmpty()) {
                    Text(
                        text = groupName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.45f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = { viewModel.toggleFavorite(channelId, !isFavorite) }) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Toggle favorite",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }

            if (isPlaying) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Playing focus indicator",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play channel",
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun FallbackChannelText(text: String) {
    val char = if (text.isNotBlank()) text.trim().first().uppercaseChar().toString() else "T"
    Text(
        text = char,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun inputColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color(0xFFE6E1E5),
    unfocusedTextColor = Color(0xFFE6E1E5).copy(alpha = 0.9f),
    focusedBorderColor = Color(0xFFD0BCFF),
    unfocusedBorderColor = Color(0xFF49454F),
    cursorColor = Color(0xFFD0BCFF),
    focusedLabelColor = Color(0xFFD0BCFF),
    unfocusedLabelColor = Color(0xFFCAC4D0).copy(alpha = 0.6f),
    focusedContainerColor = Color(0xFF2B2930),
    unfocusedContainerColor = Color(0xFF2B2930)
)
