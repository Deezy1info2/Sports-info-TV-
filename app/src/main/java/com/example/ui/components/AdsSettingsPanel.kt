package com.example.ui.components

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AdsManager
import com.example.ui.MainViewModel

@Composable
fun AdsSettingsPanel(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val config by viewModel.adsConfig.collectAsState()
    val isInitialized by AdsManager.isInitialized.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1D22)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AdsClick,
                    contentDescription = "Ads Icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Remote Ads Control (Cloudflare)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            Divider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(bottom = 12.dp))

            // Initialization status
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Unity Ads Status:", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (isInitialized) Color.Green else Color.Red,
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isInitialized) "Initialized" else "Not Initialized",
                        color = if (isInitialized) Color.Green else Color.Red,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            // Ads Enabled
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Ads Enabled (Overall):", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = config.ads.enabled.toString().uppercase(),
                    color = if (config.ads.enabled) Color.Green else Color.Red,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            // Provider & Test Mode
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Provider / Test Mode:", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "${config.ads.provider} / testMode=${config.ads.testMode}",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Game ID
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Game ID:", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = config.ads.android.gameId.ifBlank { "N/A" },
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Feature Flags
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Placement / Enable Flags:",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Banner Flag
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "• Banner (${config.ads.android.banner})", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = if (config.settings.bannerEnabled) "ENABLED" else "DISABLED",
                    color = if (config.settings.bannerEnabled) Color.Green else Color.Red,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            // Interstitial Flag
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "• Interstitial (${config.ads.android.interstitial})", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = if (config.settings.interstitialEnabled) "ENABLED" else "DISABLED",
                    color = if (config.settings.interstitialEnabled) Color.Green else Color.Red,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            // Rewarded Flag
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "• Rewarded (${config.ads.android.rewarded})", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = if (config.settings.rewardedEnabled) "ENABLED" else "DISABLED",
                    color = if (config.settings.rewardedEnabled) Color.Green else Color.Red,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            // Cooldowns
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Interstitial Interval / Cooldown:", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = "${config.settings.interstitialInterval}s / ${config.settings.rewardedCooldown}s",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Maintenance Mode Status
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Maintenance Mode:", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = config.remote.maintenanceMode.toString().uppercase(),
                    color = if (config.remote.maintenanceMode) Color.Red else Color.Green,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            if (config.remote.maintenanceMode) {
                Text(
                    text = "Msg: ${config.remote.maintenanceMessage}",
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Test ad trigger buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (activity != null) {
                            Toast.makeText(context, "Loading Test Interstitial Ad...", Toast.LENGTH_SHORT).show()
                            AdsManager.showInterstitialAd(activity) {
                                Toast.makeText(context, "Interstitial Ad Closed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    enabled = config.ads.enabled && config.settings.interstitialEnabled && !config.remote.maintenanceMode
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Interstitial", style = MaterialTheme.typography.bodySmall)
                }

                Button(
                    onClick = {
                        if (activity != null) {
                            Toast.makeText(context, "Loading Test Rewarded Ad...", Toast.LENGTH_SHORT).show()
                            AdsManager.showRewardedAd(activity) { success ->
                                if (success) {
                                    Toast.makeText(context, "🎉 Reward Earned successfully!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "❌ Rewarded Ad failed or closed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    enabled = config.ads.enabled && config.settings.rewardedEnabled && !config.remote.maintenanceMode
                ) {
                    Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Rewarded", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
