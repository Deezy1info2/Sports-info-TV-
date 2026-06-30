package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MenuItemConfig
import com.example.data.SettingsManager
import com.example.ui.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun DynamicSettingsScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val configState by viewModel.settingsConfig.collectAsState()
    val isLoading by viewModel.isSettingsLoading.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var aboutDialogItem by remember { mutableStateOf<MenuItemConfig?>(null) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF141316))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Social Follow Us Section
            val followUsMap = configState.settings.followUs
            val enabledSocials = followUsMap.filter { it.value.enabled }

            if (enabledSocials.isNotEmpty()) {
                Text(
                    text = "Join Our Community",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    enabledSocials.forEach { (key, social) ->
                        val socialColor = when (key.lowercase()) {
                            "whatsapp" -> Color(0xFF25D366)
                            "telegram" -> Color(0xFF0088CC)
                            else -> MaterialTheme.colorScheme.secondary
                        }
                        
                        val socialIcon = when (key.lowercase()) {
                            "whatsapp" -> Icons.Default.Message
                            "telegram" -> Icons.Default.Send
                            else -> Icons.Default.Group
                        }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (social.url.isNotBlank()) {
                                        openUrlSafely(context, social.url)
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1E1D22)
                            ),
                            border = BorderStroke(1.dp, socialColor.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = socialIcon,
                                    contentDescription = social.title,
                                    tint = socialColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = social.title.ifBlank { key.replaceFirstChar { it.uppercase() } },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Dynamic Menu Section
            Text(
                text = "Preferences & Support",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                val menuItems = configState.settings.menu.filter { it.visible }

                if (menuItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No options available right now.",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1E1D22)
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            menuItems.forEachIndexed { index, item ->
                                val isEnabled = item.enabled
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = isEnabled) {
                                            handleMenuAction(context, item) {
                                                aboutDialogItem = item
                                            }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                color = if (isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(8.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getIconByName(item.icon),
                                            contentDescription = item.title,
                                            tint = if (isEnabled) MaterialTheme.colorScheme.primary else Color.Gray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title.ifBlank { "Untitled" },
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isEnabled) Color.White else Color.Gray
                                            )
                                        )
                                        if (!item.subtitle.isNullOrBlank()) {
                                            Text(
                                                text = item.subtitle,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = Color.Gray
                                                ),
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        } else if (item.id == "about" && !item.content.isNullOrBlank()) {
                                            Text(
                                                text = item.content,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = Color.Gray
                                                ),
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                        contentDescription = "Navigate",
                                        tint = if (isEnabled) Color.White.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.15f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                if (index < menuItems.size - 1) {
                                    HorizontalDivider(
                                        color = Color.White.copy(alpha = 0.05f),
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // Dynamic About Dialog
    aboutDialogItem?.let { item ->
        AlertDialog(
            onDismissRequest = { aboutDialogItem = null },
            icon = {
                Icon(
                    imageVector = getIconByName(item.icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = item.content ?: "Information description is not provided.",
                    color = Color.LightGray,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { aboutDialogItem = null }) {
                    Text("Close", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = Color(0xFF1E1D22),
            titleContentColor = Color.White,
            textContentColor = Color.LightGray
        )
    }
}

private fun getIconByName(iconName: String): ImageVector {
    return when (iconName.lowercase()) {
        "share" -> Icons.Default.Share
        "star", "rate" -> Icons.Default.Star
        "feedback", "help" -> Icons.Default.Feedback
        "info", "about" -> Icons.Default.Info
        "privacy", "lock", "security" -> Icons.Default.Security
        "description", "terms", "article" -> Icons.Default.Description
        "whatsapp" -> Icons.Default.Message
        "telegram" -> Icons.Default.Send
        "email", "mail" -> Icons.Default.Email
        else -> Icons.Default.Settings
    }
}

private fun handleMenuAction(context: Context, item: MenuItemConfig, onShowAboutDialog: () -> Unit) {
    when {
        item.id == "share" -> {
            shareApp(context)
        }
        item.id == "about" -> {
            onShowAboutDialog()
        }
        !item.email.isNullOrBlank() -> {
            sendEmail(context, item.email)
        }
        !item.url.isNullOrBlank() -> {
            openUrlSafely(context, item.url)
        }
        else -> {
            Toast.makeText(context, "${item.title} clicked", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun openUrlSafely(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open link: Invalid URL or no browser found", Toast.LENGTH_SHORT).show()
    }
}

private fun sendEmail(context: Context, emailAddress: String) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(emailAddress))
            putExtra(Intent.EXTRA_SUBJECT, "SPORTS INFO TV Feedback")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Send Feedback via...").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    } catch (e: Exception) {
        Toast.makeText(context, "Could not initiate email client", Toast.LENGTH_SHORT).show()
    }
}

private fun shareApp(context: Context) {
    try {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "Check out SPORTS INFO TV! Watch exciting sports channels, live streams, match cards, and more: https://play.google.com/store/apps/details?id=${context.packageName}"
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share SPORTS INFO TV").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    } catch (e: Exception) {
        Toast.makeText(context, "Could not share app", Toast.LENGTH_SHORT).show()
    }
}
