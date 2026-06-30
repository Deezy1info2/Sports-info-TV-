package com.example.data

import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

object SettingsManager {
    private const val TAG = "SettingsManager"
    
    // Remote settings JSON URL
    const val SETTINGS_CONFIG_URL = "https://pub-886883dcae414a3f8864ea53ef3887a7.r2.dev/settings.json"
    private const val CACHE_FILE_NAME = "settings_config.json"

    private val moshi = Moshi.Builder().build()
    private val configAdapter = moshi.adapter(SettingsResponse::class.java)

    // Observable State Flow for the active configuration
    private val _settingsConfig = MutableStateFlow(SettingsResponse())
    val settingsConfig: StateFlow<SettingsResponse> = _settingsConfig.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Default built-in fallback configuration in case network is down and no cache exists
    private val defaultJson = """
        {
          "settings": {
            "darkMode": {
              "enabled": true
            },
            "followUs": {
              "whatsapp": {
                "enabled": true,
                "title": "WhatsApp",
                "url": "https://whatsapp.com/channel/0029VbDEpbo002TAzgBEyH2H"
              },
              "telegram": {
                "enabled": true,
                "title": "Telegram",
                "url": "https://t.me/sports_info2"
              }
            },
            "menu": [
              {
                "id": "share",
                "icon": "share",
                "title": "Share App",
                "enabled": true
              },
              {
                "id": "rate",
                "icon": "star",
                "title": "Rate Us",
                "enabled": true,
                "url": "https://drive.google.com/file/d/11NqtNbbjmCHzVaWVmHudfzcIoE_iEAJ5/view?usp=drivesdk"
              },
              {
                "id": "feedback",
                "icon": "feedback",
                "title": "Feedback",
                "enabled": true,
                "email": "deezy1info@gmail.com"
              },
              {
                "id": "about",
                "icon": "info",
                "title": "About",
                "enabled": true,
                "content": "Version 1.0.0"
              },
              {
                "id": "privacy",
                "icon": "privacy",
                "title": "Privacy Policy",
                "enabled": true,
                "url": "https://yourdomain.com/privacy"
              },
              {
                "id": "terms",
                "icon": "description",
                "title": "Terms of Service",
                "enabled": true,
                "url": "https://yourdomain.com/terms"
              }
            ]
          }
        }
    """.trimIndent()

    /**
     * Initializes Settings and loads remote config.
     */
    fun init(context: Context, coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            _isLoading.value = true
            
            // 1. Load from local cache or built-in defaults first for offline support
            val cachedConfig = loadFromCache(context)
            if (cachedConfig != null) {
                _settingsConfig.value = cachedConfig
            } else {
                _settingsConfig.value = parseConfigSafely(defaultJson) ?: SettingsResponse()
            }

            // 2. Fetch the latest remote config from Cloudflare
            fetchRemoteConfig(context)
            _isLoading.value = false
        }
    }

    /**
     * Fetches remote settings.json from Cloudflare, caches it, and applies updates.
     */
    private suspend fun fetchRemoteConfig(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching remote settings from $SETTINGS_CONFIG_URL")
                val client = OkHttpClient.Builder().build()
                val request = Request.Builder()
                    .url(SETTINGS_CONFIG_URL)
                    .addHeader("User-Agent", "Mozilla/5.0")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw Exception("Failed to download: HTTP ${response.code}")
                    }
                    val jsonString = response.body?.string() ?: throw Exception("Response body is empty")
                    
                    // Parse the JSON safely
                    val parsedConfig = parseConfigSafely(jsonString)
                        ?: throw Exception("Failed to parse settings JSON schema")
                    
                    Log.d(TAG, "Successfully downloaded remote settings. Menu count: ${parsedConfig.settings.menu.size}")

                    // Save downloaded config to local file cache
                    saveToCache(context, jsonString)

                    // Update StateFlow
                    _settingsConfig.value = parsedConfig
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching remote settings: ${e.message}. Using cached/default config.", e)
            }
        }
    }

    private fun parseConfigSafely(json: String): SettingsResponse? {
        return try {
            configAdapter.fromJson(json)
        } catch (e: Exception) {
            Log.e(TAG, "JSON parsing error: ${e.message}", e)
            null
        }
    }

    /**
     * Loads the SettingsResponse from local cache.
     */
    private suspend fun loadFromCache(context: Context): SettingsResponse? = withContext(Dispatchers.IO) {
        try {
            val cacheFile = File(context.filesDir, CACHE_FILE_NAME)
            if (cacheFile.exists()) {
                val jsonString = cacheFile.readText()
                val parsed = parseConfigSafely(jsonString)
                Log.d(TAG, "Loaded settings from local cache successfully.")
                parsed
            } else {
                Log.d(TAG, "No local settings cache exists yet.")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load settings from cache: ${e.message}", e)
            null
        }
    }

    /**
     * Saves the JSON settings configuration to local cache.
     */
    private suspend fun saveToCache(context: Context, jsonString: String) = withContext(Dispatchers.IO) {
        try {
            val cacheFile = File(context.filesDir, CACHE_FILE_NAME)
            cacheFile.writeText(jsonString)
            Log.d(TAG, "Saved remote settings to local cache.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save settings to cache: ${e.message}", e)
        }
    }
}
