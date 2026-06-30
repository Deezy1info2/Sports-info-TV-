package com.example.ui.components

import androidx.activity.compose.BackHandler
import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.util.LayoutDirection
import android.util.Rational
import android.widget.FrameLayout
import kotlin.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.ui.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Formatter
import java.util.Locale

data class VideoTrackOption(
    val group: Tracks.Group,
    val trackIndex: Int,
    val resolution: String,
    val bitrate: Int
)

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayer(
    url: String,
    headers: Map<String, String>,
    title: String?,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    isFullscreen: Boolean,
    onFullscreenToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    // Player instances
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var playWhenReady by remember { mutableStateOf(true) }
    var playbackState by remember { mutableStateOf(Player.STATE_IDLE) }
    var playErrorMsg by remember { mutableStateOf<String?>(null) }
    
    // Playback states
    var isPlayingState by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var bufferedPosition by remember { mutableStateOf(0L) }
    
    // Custom media format / tracks quality states
    var availableTracks by remember { mutableStateOf<List<VideoTrackOption>>(emptyList()) }
    var selectedVideoTrack by remember { mutableStateOf<VideoTrackOption?>(null) }
    var showQualityMenu by remember { mutableStateOf(false) }

    // Display aspect ratio states (FIT, ZOOM, FILL, etc.)
    var resizeMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    // Screen controllers overlay visibility
    var showControls by remember { mutableStateOf(true) }
    var orientationLocked by remember { mutableStateOf(false) }

    // Volume & Brightness controller states
    var volumeValue by remember { mutableStateOf(1.0f) }
    var brightnessValue by remember { mutableStateOf(0.8f) }

    // Brightness setting effect
    LaunchedEffect(brightnessValue) {
        activity?.let { act ->
            val layoutParams = act.window.attributes
            layoutParams.screenBrightness = brightnessValue
            act.window.attributes = layoutParams
        }
    }

    // Orientation logic
    val isMatchStream by viewModel.isMatchStream.collectAsState()
    val currentPlayingMatchCard by viewModel.currentPlayingMatchCard.collectAsState()
    val autoRotationEnabled by viewModel.autoRotationEnabled.collectAsState()
    
    LaunchedEffect(isFullscreen, isMatchStream, autoRotationEnabled) {
        if (isFullscreen) {
            if (autoRotationEnabled && isMatchStream) {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    // Direct Position loop tracker
    LaunchedEffect(player, isPlayingState) {
        while (isPlayingState && player != null) {
            player?.let { p ->
                currentPosition = p.currentPosition
                duration = if (p.duration < 0) 0L else p.duration
                bufferedPosition = p.bufferedPosition
            }
            delay(500)
        }
    }

    // Safe ExoPlayer initializer
    fun initializePlayer() {
        try {
            // Free any past players
            player?.release()
            player = null
            playErrorMsg = null
            availableTracks = emptyList()
            selectedVideoTrack = null

            viewModel.addLog("PlayerEngine", "Initializing Media3 player for URL: $url")
            
            // Build modern HTTP datasource inject headers
            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setDefaultRequestProperties(headers)
                .setAllowCrossProtocolRedirects(true)
            
            val userAgentStr = headers["User-Agent"] ?: "D-EXO Player/1.0 (Android Native Native Player)"
            httpDataSourceFactory.setUserAgent(userAgentStr)

            val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
            val defaultMediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build()

            val p = ExoPlayer.Builder(context)
                .setMediaSourceFactory(defaultMediaSourceFactory)
                .setAudioAttributes(audioAttributes, true)
                .build()
                .apply {
                    val mediaItem = MediaItem.fromUri(url)
                    setMediaItem(mediaItem)
                    prepare()
                    this.playWhenReady = playWhenReady
                    volume = volumeValue
                }

            p.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    playbackState = state
                    when (state) {
                        Player.STATE_BUFFERING -> {
                            viewModel.addLog("ExoPlayer", "State changed to: Buffering...")
                        }
                        Player.STATE_READY -> {
                            viewModel.addLog("ExoPlayer", "State changed to: Ready. Duration: ${p.duration}ms")
                            duration = if (p.duration < C.TIME_UNSET) p.duration else 0L
                        }
                        Player.STATE_ENDED -> {
                            viewModel.addLog("ExoPlayer", "State changed to: Ended")
                        }
                        Player.STATE_IDLE -> {
                            viewModel.addLog("ExoPlayer", "State changed to: Idle")
                        }
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    isPlayingState = isPlaying
                    viewModel.addLog("ExoPlayer", "IsPlaying modified: $isPlaying")
                }

                override fun onTracksChanged(tracks: Tracks) {
                    val videoOptions = mutableListOf<VideoTrackOption>()
                    for (group in tracks.groups) {
                        if (group.type == C.TRACK_TYPE_VIDEO) {
                            for (i in 0 until group.length) {
                                if (group.isTrackSupported(i)) {
                                    val format = group.getTrackFormat(i)
                                    val res = if (format.width > 0 && format.height > 0) {
                                        "${format.width}x${format.height}"
                                    } else {
                                        "Quality ${videoOptions.size + 1}"
                                    }
                                    videoOptions.add(VideoTrackOption(group, i, res, format.bitrate))
                                }
                            }
                        }
                    }
                    availableTracks = videoOptions
                    
                    // See if one track is active
                    selectedVideoTrack = videoOptions.find { it.group.isTrackSelected(it.trackIndex) }
                    viewModel.addLog("ExoPlayer", "Tracks found count: ${videoOptions.size}")
                }

                override fun onPlayerError(error: PlaybackException) {
                    val message = when (error.errorCode) {
                        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> {
                            val msg = error.localizedMessage ?: ""
                            if (msg.contains("403")) "Forbidden (403): Check headers/authorization credentials"
                            else if (msg.contains("401")) "Unauthorized (401): Authentication required"
                            else "HTTP Status error loading stream"
                        }
                        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "Network connection failed. Verify internet."
                        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> "Stream URL or M3U8 source file not found (404)"
                        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> "Device hardware unable to decode stream format"
                        else -> error.localizedMessage ?: "Playback error: ${error.errorCodeName}"
                    }
                    playErrorMsg = message
                    viewModel.addLog("ExoPlayerError", "Playback failed: $message", isError = true)
                    viewModel.addLog("ExoPlayerErrorDetail", "Detailed log: ${error.cause?.message ?: error.message}", isError = true)
                }
            })

            player = p
        } catch (e: Exception) {
            playErrorMsg = e.localizedMessage ?: "Core setup failure"
            viewModel.addLog("PlayerEngine", "Fatal crash preparing player: ${e.message}", isError = true)
        }
    }

    // Reload stream if URL or Custom parameters change
    LaunchedEffect(url, headers) {
        if (url.isNotBlank()) {
            initializePlayer()
        }
    }

    // Clean up player when Composable leaves tree state
    DisposableEffect(Unit) {
        onDispose {
            player?.release()
            player = null
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    if (isFullscreen) {
        BackHandler {
            onFullscreenToggle(false)
        }
    }

    Box(
        modifier = modifier
            .testTag("video_player_container")
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showControls = !showControls
            }
    ) {
        if (url.isBlank()) {
            // Unset empty stream state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircleFilled,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No Stream Loaded",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = "Select a channel below or paste a stream URL to begin playing",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 40.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            // Android ExoPlayer instance view
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false // We draw our custom Compose design controls!
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { view ->
                    view.player = player
                    view.resizeMode = resizeMode
                },
                modifier = Modifier.fillMaxSize()
            )

            // Buffering visual feedback indicator
            if (playbackState == Player.STATE_BUFFERING && playErrorMsg == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Buffering stream...",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Error Layer overlay
            if (playErrorMsg != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Error detail",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Stream Playback Error",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = playErrorMsg ?: "An unknown stream timeout occurred.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { initializePlayer() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retry Playback")
                                }
                            }
                        }
                    }
                }
            }

            // Controls Overlay layout
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    // Top Bar with stream title and Close/Stop button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                                )
                            )
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = title ?: "Live Stream",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                viewModel.stopStream()
                                onFullscreenToggle(false)
                            },
                            modifier = Modifier.testTag("player_close_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Player",
                                tint = Color.White
                            )
                        }
                    }

                    // Large play/pause button overlay
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 70.dp)
                            .clip(RoundedCornerShape(36.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable {
                                player?.let { p ->
                                    if (p.isPlaying) {
                                        p.pause()
                                        playWhenReady = false
                                        viewModel.addLog("PlayerUI", "Paused stream")
                                    } else {
                                        p.play()
                                        playWhenReady = true
                                        viewModel.addLog("PlayerUI", "Resumed play stream")
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlayingState) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlayingState) "Pause clip" else "Play clip",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Bottom Seek area and timing row
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                )
                            )
                            .padding(16.dp)
                    ) {
                        val matchCard = currentPlayingMatchCard
                        if (matchCard != null && matchCard.servers.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Select Server:",
                                    color = Color.White.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(matchCard.servers) { server ->
                                        val isSelected = server.url == url
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary
                                                    else Color.Black.copy(alpha = 0.5f)
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    if (!isSelected) {
                                                        viewModel.playStream(server.url, title, isMatch = true, matchCard = matchCard)
                                                    }
                                                }
                                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                                .testTag("server_btn_${server.name.replace(" ", "_")}")
                                        ) {
                                            Text(
                                                text = server.name,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (duration > 0L) {
                            // Linear slider for seeking progress with a custom sleek, tiny thumb
                            Slider(
                                value = currentPosition.toFloat(),
                                onValueChange = { pos ->
                                    currentPosition = pos.toLong()
                                    player?.seekTo(pos.toLong())
                                },
                                valueRange = 0f..duration.toFloat(),
                                colors = SliderDefaults.colors(
                                    activeTrackColor = Color.Red,
                                    inactiveTrackColor = Color.Red.copy(alpha = 0.3f),
                                    thumbColor = Color.Red
                                ),
                                thumb = {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color.Red, shape = CircleShape)
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = getStringTime(currentPosition),
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = getStringTime(duration),
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        } else {
                            // Live TV stream tag indicators
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.Red)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "LIVE STREAMPLAY",
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                
                                // Reset connection
                                IconButton(
                                    onClick = { initializePlayer() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Retry playback connection", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Playback, volume, and screen layout controllers row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left side controls group (Play/Pause, Volume)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Bottom Play/Pause button
                                IconButton(
                                    onClick = {
                                        player?.let { p ->
                                            if (p.isPlaying) {
                                                p.pause()
                                                playWhenReady = false
                                                viewModel.addLog("PlayerUI", "Paused stream")
                                            } else {
                                                p.play()
                                                playWhenReady = true
                                                viewModel.addLog("PlayerUI", "Resumed play stream")
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(32.dp).testTag("player_play_pause_bottom_btn")
                                ) {
                                    Icon(
                                        imageVector = if (isPlayingState) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlayingState) "Pause" else "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                // Custom separator
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(18.dp)
                                        .background(Color.White.copy(alpha = 0.2f))
                                )

                                // Volume controls (Button & Slider)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    var isMuted by remember { mutableStateOf(false) }
                                    var lastVolumeBeforeMute by remember { mutableStateOf(1.0f) }

                                    IconButton(
                                        onClick = {
                                            player?.let { p ->
                                                if (isMuted) {
                                                    volumeValue = lastVolumeBeforeMute
                                                    p.volume = lastVolumeBeforeMute
                                                } else {
                                                    lastVolumeBeforeMute = volumeValue
                                                    volumeValue = 0f
                                                    p.volume = 0f
                                                }
                                                isMuted = !isMuted
                                            }
                                        },
                                        modifier = Modifier.size(32.dp).testTag("player_volume_toggle")
                                    ) {
                                        Icon(
                                            imageVector = when {
                                                isMuted || volumeValue == 0f -> Icons.Default.VolumeMute
                                                volumeValue < 0.5f -> Icons.Default.VolumeDown
                                                else -> Icons.Default.VolumeUp
                                            },
                                            contentDescription = "Volume controls",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Slider(
                                        value = volumeValue,
                                        onValueChange = { vol ->
                                            volumeValue = vol
                                            player?.volume = vol
                                            if (vol > 0f) isMuted = false
                                        },
                                        valueRange = 0f..1f,
                                        colors = SliderDefaults.colors(
                                            activeTrackColor = Color.Red,
                                            inactiveTrackColor = Color.Red.copy(alpha = 0.3f),
                                            thumbColor = Color.Red
                                        ),
                                        thumb = {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(Color.Red, shape = CircleShape)
                                            )
                                        },
                                        modifier = Modifier
                                            .width(45.dp)
                                            .testTag("player_volume_slider")
                                    )
                                }
                            }

                            // Right side controls group (Gear icon, Full-screen toggle)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Dynamic Quality Selector (Gear icon)
                                if (availableTracks.isNotEmpty()) {
                                    Box {
                                        IconButton(
                                            onClick = { showQualityMenu = !showQualityMenu },
                                            modifier = Modifier.size(32.dp).testTag("player_quality_settings")
                                        ) {
                                            Icon(
                                                Icons.Default.Settings,
                                                contentDescription = "Stream resolutions selector",
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = showQualityMenu,
                                            onDismissRequest = { showQualityMenu = false },
                                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Auto / Adaptable", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                onClick = {
                                                    player?.let { p ->
                                                        p.trackSelectionParameters = p.trackSelectionParameters
                                                            .buildUpon()
                                                            .clearOverrides()
                                                            .build()
                                                        selectedVideoTrack = null
                                                    }
                                                    showQualityMenu = false
                                                    viewModel.addLog("PlayerTrack", "Resolutions reset to Auto Adaptive stream")
                                                }
                                            )
                                            availableTracks.forEach { option ->
                                                DropdownMenuItem(
                                                    text = { 
                                                        Text(
                                                            "${option.resolution} (${(option.bitrate / 1000)} Kbps)",
                                                            color = if (selectedVideoTrack == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                        ) 
                                                    },
                                                    onClick = {
                                                        player?.let { p ->
                                                            p.trackSelectionParameters = p.trackSelectionParameters
                                                                .buildUpon()
                                                                .setOverrideForType(
                                                                    TrackSelectionOverride(option.group.mediaTrackGroup, option.trackIndex)
                                                                )
                                                                .build()
                                                            selectedVideoTrack = option
                                                        }
                                                        showQualityMenu = false
                                                        viewModel.addLog("PlayerTrack", "Switched video stream quality track: ${option.resolution}")
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // Full-screen toggle button
                                IconButton(
                                    onClick = { onFullscreenToggle(!isFullscreen) },
                                    modifier = Modifier.size(32.dp).testTag("player_fullscreen_toggle")
                                ) {
                                    Icon(
                                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                        contentDescription = "Toggle screen fullscreen size",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
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

// Convert Milliseconds to elegant format string
private fun getStringTime(ms: Long): String {
    val totalSecs = (ms / 1000).toInt()
    val secs = totalSecs % 60
    val mins = (totalSecs / 60) % 60
    val hours = totalSecs / 3600
    val builder = StringBuilder()
    val formatter = Formatter(builder, Locale.getDefault())
    return if (hours > 0) {
        formatter.format("%d:%02d:%02d", hours, mins, secs).toString()
    } else {
        formatter.format("%02d:%02d", mins, secs).toString()
    }
}
