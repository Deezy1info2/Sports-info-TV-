package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.MainViewModel

@Composable
fun AdvancedHeadersSheet(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    // User-entered local states for customized additions
    var incomingKey by remember { mutableStateOf("") }
    var incomingValue by remember { mutableStateOf("") }

    val userAgent by viewModel.userAgent.collectAsState()
    val referer by viewModel.referer.collectAsState()
    val origin by viewModel.origin.collectAsState()
    val authorization by viewModel.authorization.collectAsState()
    val cookies by viewModel.cookies.collectAsState()
    val customMap by viewModel.customHeaders.collectAsState()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("advanced_headers_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2B2930)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Expand header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickableNoRipple { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Advanced headers icon",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Advanced Playback Options",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Configure custom user agents, HTTP origins, cookies, or auth keys",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse settings" else "Expand settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Standard User Agent field
                    OutlinedTextField(
                        value = userAgent,
                        onValueChange = { viewModel.userAgent.value = it },
                        label = { Text("User-Agent Header") },
                        placeholder = { Text("e.g. VLC/3.0.18 LibVLC/3.0.18") },
                        leadingIcon = { Icon(Icons.Default.Devices, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("user_agent_input"),
                        singleLine = true,
                        colors = outlinedTextFieldColors()
                    )

                    // Referer field
                    OutlinedTextField(
                        value = referer,
                        onValueChange = { viewModel.referer.value = it },
                        label = { Text("Referer Header") },
                        placeholder = { Text("e.g. https://origin-stream.com") },
                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("referer_input"),
                        singleLine = true,
                        colors = outlinedTextFieldColors()
                    )

                    // Origin field
                    OutlinedTextField(
                        value = origin,
                        onValueChange = { viewModel.origin.value = it },
                        label = { Text("Origin Header") },
                        placeholder = { Text("e.g. https://live.tv-provider.org") },
                        leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("origin_input"),
                        singleLine = true,
                        colors = outlinedTextFieldColors()
                    )

                    // Cookie field
                    OutlinedTextField(
                        value = cookies,
                        onValueChange = { viewModel.cookies.value = it },
                        label = { Text("Cookie Value") },
                        placeholder = { Text("e.g. session_id=xyz987; user=active") },
                        leadingIcon = { Icon(Icons.Default.Cookie, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("cookie_input"),
                        singleLine = true,
                        colors = outlinedTextFieldColors()
                    )

                    // Authorization Token field
                    OutlinedTextField(
                        value = authorization,
                        onValueChange = { viewModel.authorization.value = it },
                        label = { Text("Authorization Token (Auth)") },
                        placeholder = { Text("e.g. Bearer eyJhbGciOiJIUzI1NiI...") },
                        leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("auth_token_input"),
                        singleLine = true,
                        colors = outlinedTextFieldColors()
                    )

                    // Header Builder custom adding rows
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Additional Request Headers",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = incomingKey,
                            onValueChange = { incomingKey = it },
                            label = { Text("Header Key") },
                            placeholder = { Text("X-Custom-ID") },
                            modifier = Modifier.weight(0.45f).testTag("custom_key_input"),
                            singleLine = true,
                            colors = outlinedTextFieldColors()
                        )
                        OutlinedTextField(
                            value = incomingValue,
                            onValueChange = { incomingValue = it },
                            label = { Text("Header Value") },
                            placeholder = { Text("value") },
                            modifier = Modifier.weight(0.45f).testTag("custom_value_input"),
                            singleLine = true,
                            colors = outlinedTextFieldColors()
                        )
                        IconButton(
                            onClick = {
                                if (incomingKey.isNotBlank() && incomingValue.isNotBlank()) {
                                    viewModel.addCustomHeader(incomingKey, incomingValue)
                                    incomingKey = ""
                                    incomingValue = ""
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add custom header tag", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }

                    // Active list of added properties rows
                    if (customMap.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.2f))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            customMap.forEach { (k, v) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = k,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = v,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.removeCustomHeader(k) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Delete custom header",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
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

// Clean helper for click interactions without massive animations noise
@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    return this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}

@Composable
private fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
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
