package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MatchCard
import com.example.data.RecentStream
import com.example.data.RecentStreamStore
import com.example.data.AdsManager
import com.example.data.SettingsManager
import com.example.data.parser.M3uParser
import java.io.ByteArrayInputStream
import com.example.data.database.ChannelEntity
import com.example.data.database.IptvDatabase
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = IptvDatabase.getDatabase(application)
    private val channelDao = db.channelDao()
    private val recentStore = RecentStreamStore(application)
    private val moshi = Moshi.Builder().build()
    
    // Match Cards
    val matchCards = MutableStateFlow<List<MatchCard>>(emptyList())

    // Remote Ads and Configuration
    val adsConfig = AdsManager.config
    val settingsConfig = SettingsManager.settingsConfig
    val isSettingsLoading = SettingsManager.isLoading

    // Stream inputs
    val streamUrl = MutableStateFlow("")
    val streamTitle = MutableStateFlow("")

    // Advanced request headers setup
    val userAgent = MutableStateFlow("")
    val referer = MutableStateFlow("")
    val origin = MutableStateFlow("")
    val authorization = MutableStateFlow("")
    val cookies = MutableStateFlow("")
    
    // Additional custom fields (headers list)
    val customHeaders = MutableStateFlow<Map<String, String>>(emptyMap())

    // Active playback states
    val currentPlayingUrl = MutableStateFlow<String?>(null)
    val currentPlayingTitle = MutableStateFlow<String?>(null)
    val currentPlayingHeaders = MutableStateFlow<Map<String, String>>(emptyMap())

    // UI filters
    val searchQuery = MutableStateFlow("")
    val selectedGroup = MutableStateFlow("All")
    
    // Rotation preference
    val autoRotationEnabled = MutableStateFlow(true)

    // Database Flows
    val groups = channelDao.getDistinctGroups()
        .map { list ->
            val customBuiltin = listOf("All", "Favorite", "SPORTS", "Worldcup")
            customBuiltin + list.filter { it !in customBuiltin }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All", "Favorite", "SPORTS", "Worldcup"))

    private val allChannels = channelDao.getAllChannels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredChannels = combine(allChannels, searchQuery, selectedGroup) { list, query, group ->
        list.filter { item ->
            val matchesQuery = query.isEmpty() || item.name.contains(query, ignoreCase = true)
            val isSports = (item.group ?: "").contains("Sport", ignoreCase = true) || item.name.contains("Sport", ignoreCase = true)
            val isWorldcup = (item.group ?: "").contains("World", ignoreCase = true) || item.name.contains("World", ignoreCase = true)
            val isFavorite = item.isFavorite

            val matchesGroup = when (group) {
                "All" -> true
                "Favorite" -> isFavorite
                "SPORTS" -> isSports
                "Worldcup" -> isWorldcup
                else -> item.group == group
            }
            matchesQuery && matchesGroup
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleFavorite(channelId: Int, isFavorite: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            channelDao.updateFavoriteStatus(channelId, isFavorite)
        }
    }

    // DataStore Flows
    val recentStreams = recentStore.recentStreams
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // Developer logging panel logs
    private val _devLogs = MutableStateFlow<List<LogMessage>>(emptyList())
    val devLogs: StateFlow<List<LogMessage>> = _devLogs.asStateFlow()

    init {
        // Initialize remote config, AdsManager, and remote SettingsManager
        AdsManager.init(application, viewModelScope)
        SettingsManager.init(application, viewModelScope)

        // Load matchcards instantly
        viewModelScope.launch {
            kotlinx.coroutines.delay(50)
            loadMatchCards("https://pub-886883dcae414a3f8864ea53ef3887a7.r2.dev/matchcards.json")
            
            // Auto-refresh matchcards less frequently
            while (true) {
                kotlinx.coroutines.delay(60000) // 1 minute instead of 5 seconds
                loadMatchCards("https://pub-886883dcae414a3f8864ea53ef3887a7.r2.dev/matchcards.json")
            }
        }
        
        // Load channels instantly
        viewModelScope.launch {
            kotlinx.coroutines.delay(50)
            fetchAndImportChannels("https://raw.githubusercontent.com/Deezy1info2/exo-tv-data/main/channels.m3u")
        }
        
        // Load the last configurations
        viewModelScope.launch {
            recentStore.lastPlayedUrl.firstOrNull()?.let { url ->
                if (url.isNotEmpty()) {
                    streamUrl.value = url
                    // Auto-start playback
                    playStream(url, null)
                }
            }
            recentStore.savedHeaders.firstOrNull()?.let { headers ->
                userAgent.value = headers["User-Agent"] ?: ""
                referer.value = headers["Referer"] ?: ""
                origin.value = headers["Origin"] ?: ""
                authorization.value = headers["Authorization"] ?: ""
                cookies.value = headers["Cookie"] ?: ""
                
                val customOnly = headers.filterKeys { 
                    it !in listOf("User-Agent", "Referer", "Origin", "Authorization", "Cookie")
                }
                customHeaders.value = customOnly
            }
        }
    }

    fun addLog(tag: String, message: String, isError: Boolean = false) {
        val newLog = LogMessage(
            timestamp = System.currentTimeMillis(),
            tag = tag,
            message = message,
            isError = isError
        )
        _devLogs.value = (_devLogs.value + newLog).takeLast(100) // Keep last 100 log lines
    }

    fun clearLogs() {
        _devLogs.value = emptyList()
    }

    fun addCustomHeader(key: String, value: String) {
        if (key.isBlank() || value.isBlank()) return
        customHeaders.value = customHeaders.value + (key.trim() to value.trim())
        addLog("ViewModel", "Added custom header: $key -> $value")
    }

    fun removeCustomHeader(key: String) {
        customHeaders.value = customHeaders.value - key
        addLog("ViewModel", "Removed custom header: $key")
    }

    val isMatchStream = MutableStateFlow(false)
    val currentPlayingMatchCard = MutableStateFlow<MatchCard?>(null)

    fun playStream(url: String, title: String? = null, isMatch: Boolean = false, matchCard: MatchCard? = null) {
        if (url.isBlank()) {
            addLog("Player", "Requested play failed: URL is empty", isError = true)
            return
        }
        
        isMatchStream.value = isMatch
        currentPlayingMatchCard.value = matchCard
        val assembledHeaders = buildActiveHeadersMap()
        val finalTitle = title ?: url.substringAfterLast("/").substringBefore("?")
        
        streamUrl.value = url
        streamTitle.value = finalTitle
        currentPlayingUrl.value = url
        currentPlayingTitle.value = finalTitle
        currentPlayingHeaders.value = assembledHeaders

        addLog("Player", "Starting playback: $finalTitle")
        addLog("Player", "Stream URL: $url")
        addLog("Player", "Active headers: $assembledHeaders")
        addLog("Player", "Is match: ${isMatchStream.value}")

        viewModelScope.launch {
            recentStore.saveLastPlayedUrl(url)
            recentStore.saveHeaders(assembledHeaders)
            recentStore.addRecentStream(url = url, title = finalTitle, headers = assembledHeaders)
        }
    }

    fun stopStream() {
        if (currentPlayingUrl.value != null) {
            addLog("Player", "Stopping stream: ${currentPlayingTitle.value}")
        }
        currentPlayingUrl.value = null
        currentPlayingTitle.value = null
        currentPlayingHeaders.value = emptyMap()
        currentPlayingMatchCard.value = null
    }

    private fun buildActiveHeadersMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        if (userAgent.value.isNotBlank()) map["User-Agent"] = userAgent.value.trim()
        if (referer.value.isNotBlank()) map["Referer"] = referer.value.trim()
        if (origin.value.isNotBlank()) map["Origin"] = origin.value.trim()
        if (authorization.value.isNotBlank()) map["Authorization"] = authorization.value.trim()
        if (cookies.value.isNotBlank()) map["Cookie"] = cookies.value.trim()
        
        customHeaders.value.forEach { (key, value) ->
            if (key.isNotBlank() && value.isNotBlank()) {
                map[key.trim()] = value.trim()
            }
        }
        return map
    }


    fun fetchAndImportChannels(url: String) {
        addLog("Importer", "Fetching remote M3U: $url")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient.Builder().build()
                val request = Request.Builder().url(url).build()
                
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw Exception("Server returned error code: ${response.code}")
                    }
                    val bodyString = response.body?.string() ?: throw Exception("Empty response body")
                    val parsedChannels = M3uParser.parse(ByteArrayInputStream(bodyString.toByteArray()), playlistUrl = url)
                    
                    if (parsedChannels.isEmpty()) {
                        throw Exception("No valid stream channels found in the playlist")
                    }

                    // Save parsed channels to SQLite while preserving favorite status
                    val favorites = channelDao.getFavoriteChannels()
                    val favoriteUrls = favorites.map { it.url }.toSet()
                    val favoriteNames = favorites.map { it.name }.toSet()

                    val updatedChannels = parsedChannels.map { channel ->
                        if (favoriteUrls.contains(channel.url) || favoriteNames.contains(channel.name)) {
                            channel.copy(isFavorite = true)
                        } else {
                            channel
                        }
                    }

                    channelDao.deleteAllChannels()
                    channelDao.insertChannels(updatedChannels)
                    
                    addLog("Importer", "Successfully imported ${parsedChannels.size} channels from: $url")
                }
            } catch (e: Exception) {
                addLog("Importer", "Import failed from URL: ${e.message}", isError = true)
            }
        }
    }

    fun clearLocalChannels() {
        viewModelScope.launch(Dispatchers.IO) {
            channelDao.deleteAllChannels()
            selectedGroup.value = "All"
            searchQuery.value = ""
            addLog("Database", "Cleared local channels cache database")
        }
    }

    fun clearRecentStreams() {
        viewModelScope.launch {
            recentStore.clearRecents()
            addLog("Database", "Cleared recent streams list history")
        }
    }

    fun loadMatchCards(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient.Builder().build()
                val request = Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0")
                    .build()
                
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw Exception("Failed to fetch matchcards")
                    val bodyString = response.body?.string() ?: throw Exception("Empty body")
                    val type = Types.newParameterizedType(List::class.java, MatchCard::class.java)
                    val adapter = moshi.adapter<List<MatchCard>>(type)
                    val list = adapter.fromJson(bodyString) ?: emptyList()
                    matchCards.value = list
                }
            } catch (e: Exception) {
                addLog("Matchcards", "Failed to load: ${e.message}", isError = true)
            }
        }
    }

    fun refreshMatchCards() {
        loadMatchCards("https://pub-886883dcae414a3f8864ea53ef3887a7.r2.dev/matchcards.json")
        addLog("Matchcards", "Refresh requested")
    }
}


data class LogMessage(
    val timestamp: Long,
    val tag: String,
    val message: String,
    val isError: Boolean = false
)
