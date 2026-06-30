package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.data.RecentStream
import com.example.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IptvDashboard(
    viewModel: MainViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    // Observe playing states
    val currentUrl by viewModel.currentPlayingUrl.collectAsState()
    val currentTitle by viewModel.currentPlayingTitle.collectAsState()
    val currentHeaders by viewModel.currentPlayingHeaders.collectAsState()

    // Observe stream URL state
    val streamUrlInput by viewModel.streamUrl.collectAsState()
    val streamTitleInput by viewModel.streamTitle.collectAsState()

    // Observe channel playlist searches
    val channelsList by viewModel.filteredChannels.collectAsState()
    val groupList by viewModel.groups.collectAsState()
    val activeGroup by viewModel.selectedGroup.collectAsState()
    val searchInput by viewModel.searchQuery.collectAsState()

    // Observe matchcards
    val matchCardsList by viewModel.matchCards.collectAsState()

    // Observe ads configuration
    val adsConfig by viewModel.adsConfig.collectAsState()

    // Full screen state toggle
    var isFullscreen by remember { mutableStateOf(false) }

    // Screen sections navigation tabs
    var selectedTab by remember { mutableStateOf(0) }
    val tabLabels = listOf("Live Console", "Channel Playlist")
    val tabIcons = listOf(Icons.Default.ConnectedTv, Icons.Default.LiveTv)

    // Bottom Sheet Settings toggle
    var showSettingsSheet by remember { mutableStateOf(false) }
    val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (adsConfig.remote.maintenanceMode) {
        MaintenanceScreen(message = adsConfig.remote.maintenanceMessage)
    } else {
        val customThemeBackground = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1C1B1F),
                Color(0xFF141316)
            )
        )

        if (isFullscreen && !currentUrl.isNullOrEmpty()) {
            // Render Player in clean Fullscreen overlay
            VideoPlayer(
                url = currentUrl!!,
                headers = currentHeaders,
                title = currentTitle,
                viewModel = viewModel,
                isFullscreen = true,
                onFullscreenToggle = { isFullscreen = it },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SubcomposeAsyncImage(
                                    model = "https://raw.githubusercontent.com/Deezy1info2/tv-logos.html/refs/heads/main/file_00000000292071f48f2f306e69eaa974.png",
                                    contentDescription = "Logo",
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "SPORTS INFO TV",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = Color(0xFFE6E1E5)
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { showSettingsSheet = true },
                                modifier = Modifier.testTag("settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings and Developer Tools",
                                    tint = Color(0xFFE6E1E5)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF1C1B1F),
                            titleContentColor = Color(0xFFE6E1E5)
                        ),
                    )
                },
                bottomBar = {
                    AdBannerView()
                },
                modifier = modifier.fillMaxSize().background(Color(0xFF1C1B1F))
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(customThemeBackground)
                        .padding(innerPadding)
                ) {
                    // Top Section: Video Player spanning full width
                    if (!currentUrl.isNullOrEmpty()) {
                        VideoPlayer(
                            url = currentUrl!!,
                            headers = currentHeaders,
                            title = currentTitle,
                            viewModel = viewModel,
                            isFullscreen = false,
                            onFullscreenToggle = { isFullscreen = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }

                    // Tab selection header to let each section take over the page
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color(0xFF1C1B1F),
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        tabLabels.forEachIndexed { index, label ->
                            val isSelected = selectedTab == index
                            Tab(
                                selected = isSelected,
                                onClick = { selectedTab = index },
                                text = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f)
                                    )
                                },
                                icon = {
                                    Icon(
                                        imageVector = tabIcons[index],
                                        contentDescription = label,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            )
                        }
                    }

                    // Bottom Section Content: Dynamic Full-Screen Grid based on selected tab
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        if (selectedTab == 0) {
                            // Live Console Content: All Upcoming Matches in a Grid taking over the full page
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 350.dp),
                                modifier = Modifier
                                    .fillMaxSize(),
                                contentPadding = PaddingValues(top = 0.dp, bottom = 0.dp),
                                horizontalArrangement = Arrangement.spacedBy(0.dp),
                                verticalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                        Text(
                                            text = "Upcoming Matches",
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Select a live or scheduled match to stream instantly",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                }

                                if (matchCardsList.isEmpty()) {
                                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "No Matches Found",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.White.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                } else {
                                    items(matchCardsList) { card ->
                                        val now = java.time.Instant.now()
                                        val start = parseInstantSafely(card.startDateTime)
                                        val end = parseInstantSafely(card.endDateTime)
                                        val statusText = when {
                                            start == null || end == null -> "NOT STARTED"
                                            now.isBefore(start) -> "NOT STARTED"
                                            now.isAfter(end) -> "MATCH ENDED"
                                            else -> "LIVE"
                                        }
                                        val isLive = statusText == "LIVE"
                                        val statusColor = if (isLive) Color(0xFFF43F5E) else MaterialTheme.colorScheme.primary

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    val firstServerUrl = card.servers.firstOrNull()?.url ?: ""
                                                    if (firstServerUrl.isNotEmpty()) {
                                                        viewModel.playStream(firstServerUrl, "${card.homeTeam} vs ${card.awayTeam}", isMatch = true, matchCard = card)
                                                        isFullscreen = true
                                                    }
                                                },
                                            colors = CardDefaults.cardColors(
                                                containerColor = Color.Transparent
                                            ),
                                            shape = RoundedCornerShape(16.dp),
                                            border = BorderStroke(
                                                width = if (isLive) 1.5.dp else 1.dp,
                                                color = if (isLive) Color(0xFFF43F5E).copy(alpha = 0.8f) else Color(0xFF2C2A33)
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(
                                                        Brush.verticalGradient(
                                                            colors = listOf(
                                                                Color(0xFF24222E),
                                                                Color(0xFF16151C)
                                                            )
                                                        )
                                                    )
                                                    .padding(14.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                // Header Row: Competition centered
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.Center,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Competition Badge
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                                            .border(
                                                                width = 0.5.dp,
                                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                                                shape = RoundedCornerShape(6.dp)
                                                            )
                                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                                    ) {
                                                        Text(
                                                            text = card.competition.uppercase(),
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontWeight = FontWeight.Bold,
                                                                letterSpacing = 0.5.sp
                                                            ),
                                                            color = MaterialTheme.colorScheme.primary,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.widthIn(max = 120.dp)
                                                        )
                                                    }
                                                }
                                                
                                                // Teams Logos Row with VS Badge
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    // Home Logo Frame
                                                    Box(
                                                        modifier = Modifier
                                                            .size(52.dp)
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(Color(0xFFF9F9FB))
                                                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                                            .padding(6.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        SubcomposeAsyncImage(
                                                            model = card.homeLogo,
                                                            contentDescription = card.homeTeam,
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentScale = ContentScale.Fit,
                                                            error = {
                                                                FallbackChannelText(text = card.homeTeam)
                                                            }
                                                        )
                                                    }

                                                    // VS Badge
                                                    Box(
                                                        modifier = Modifier
                                                            .padding(horizontal = 12.dp)
                                                            .size(26.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFF2C2A33))
                                                            .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "VS",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                                            color = Color.White.copy(alpha = 0.6f),
                                                            fontSize = 9.sp
                                                        )
                                                    }

                                                    // Away Logo Frame
                                                    Box(
                                                        modifier = Modifier
                                                            .size(52.dp)
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(Color(0xFFF9F9FB))
                                                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                                            .padding(6.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        SubcomposeAsyncImage(
                                                            model = card.awayLogo,
                                                            contentDescription = card.awayTeam,
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentScale = ContentScale.Fit,
                                                            error = {
                                                                FallbackChannelText(text = card.awayTeam)
                                                            }
                                                        )
                                                    }
                                                }

                                                // Match Teams Title: Side-by-side weighted row to prevent truncation
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center,
                                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
                                                ) {
                                                    Text(
                                                        text = card.homeTeam,
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = Color.White,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f),
                                                        textAlign = TextAlign.End
                                                    )
                                                    Text(
                                                        text = " • ",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                        color = Color.White.copy(alpha = 0.3f),
                                                        modifier = Modifier.padding(horizontal = 4.dp)
                                                    )
                                                    Text(
                                                        text = card.awayTeam,
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = Color.White,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f),
                                                        textAlign = TextAlign.Start
                                                    )
                                                }

                                                // Start Time
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Schedule,
                                                        contentDescription = "Match Schedule",
                                                        tint = Color.White.copy(alpha = 0.4f),
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Text(
                                                        text = card.startTime,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = Color.White.copy(alpha = 0.5f)
                                                    )
                                                }

                                                // Live Status Banner
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(
                                                            if (isLive) Color(0xFFF43F5E).copy(alpha = 0.15f)
                                                            else Color.White.copy(alpha = 0.05f)
                                                        )
                                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    if (isLive) {
                                                        LiveIndicator()
                                                    }
                                                    Text(
                                                        text = statusText,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            letterSpacing = 0.5.sp
                                                        ),
                                                        color = statusColor
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Channel Playlist Content: Grid layout taking over the full page
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 180.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "Channels Playlist",
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = searchInput,
                                            onValueChange = { viewModel.searchQuery.value = it },
                                            label = { Text("Search channels...") },
                                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                            modifier = Modifier.fillMaxWidth().testTag("channel_search_bar"),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor = Color.Gray
                                            )
                                        )
                                    }
                                }

                                if (groupList.isNotEmpty()) {
                                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                        ) {
                                            items(groupList) { group ->
                                                val isSelected = activeGroup == group
                                                val containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF2B2930)
                                                val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color(0xFFE6E1E5)

                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(containerColor)
                                                        .clickable { viewModel.selectedGroup.value = group }
                                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                                ) {
                                                    Text(
                                                        text = group,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = contentColor
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (channelsList.isEmpty()) {
                                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("No Channels Match", color = Color.White.copy(alpha = 0.6f))
                                        }
                                    }
                                } else {
                                    items(channelsList) { channel ->
                                        val isPlayingThis = currentUrl == channel.url
                                        ChannelItemCard(
                                            channelId = channel.id,
                                            isFavorite = channel.isFavorite,
                                            channelName = channel.name,
                                            channelLogo = channel.logoUrl,
                                            groupName = channel.group,
                                            isPlaying = isPlayingThis,
                                            onClick = {
                                                viewModel.playStream(channel.url, channel.name, isMatch = false)
                                                isFullscreen = true
                                            },
                                            viewModel = viewModel,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (showSettingsSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showSettingsSheet = false },
                    sheetState = settingsSheetState,
                    containerColor = Color(0xFF1C1B1F),
                    dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .verticalScroll(rememberScrollState())
                            .navigationBarsPadding()
                    ) {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        DynamicSettingsScreen(viewModel = viewModel)
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
