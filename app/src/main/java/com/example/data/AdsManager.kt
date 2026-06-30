package com.example.data

import android.app.Activity
import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
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

object AdsManager {
    private const val TAG = "AdsManager"
    
    // Easily configurable remote JSON URL
    const val REMOTE_CONFIG_URL = "https://pub-886883dcae414a3f8864ea53ef3887a7.r2.dev/ads.json"
    private const val CACHE_FILE_NAME = "ads_config.json"

    private val moshi = Moshi.Builder().build()
    private val configAdapter = moshi.adapter(AdsConfig::class.java)

    // Observable State Flow for the active configuration
    private val _config = MutableStateFlow(AdsConfig())
    val config: StateFlow<AdsConfig> = _config.asStateFlow()

    // Flag indicating whether initialization has been completed or tried
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    /**
     * Initializes the configuration and sets up Unity Ads on app startup.
     */
    fun init(context: Context, coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            // 1. Load from local cache or built-in defaults first for offline support
            val cachedConfig = loadFromCache(context)
            _config.value = cachedConfig ?: AdsConfig()
            
            // If we have a cached config, try to initialize immediately with it
            val initialConfig = _config.value
            if (initialConfig.ads.enabled && !initialConfig.remote.maintenanceMode) {
                initializeUnitySdk(context.applicationContext, initialConfig)
            }

            // 2. Fetch the latest remote config
            fetchRemoteConfig(context)
        }
    }

    /**
     * Fetches remote ads.json from Cloudflare, caches it, and applies updates.
     */
    private suspend fun fetchRemoteConfig(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching remote configuration from $REMOTE_CONFIG_URL")
                val client = OkHttpClient.Builder().build()
                val request = Request.Builder()
                    .url(REMOTE_CONFIG_URL)
                    .addHeader("User-Agent", "Mozilla/5.0")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw Exception("Failed to download: HTTP ${response.code}")
                    }
                    val jsonString = response.body?.string() ?: throw Exception("Response body is empty")
                    
                    // Parse the JSON safely
                    val parsedConfig = configAdapter.fromJson(jsonString)
                        ?: throw Exception("Failed to parse JSON schema")
                    
                    Log.d(TAG, "Successfully downloaded remote config. Parsed: $parsedConfig")
                    Log.d(TAG, "MaintenanceMode: ${parsedConfig.remote.maintenanceMode}, AdsEnabled: ${parsedConfig.ads.enabled}")

                    // Save downloaded config to local file cache
                    saveToCache(context, jsonString)

                    // Update config StateFlow
                    _config.value = parsedConfig

                    // Initialize Unity SDK with the updated config if applicable
                    if (parsedConfig.ads.enabled && !parsedConfig.remote.maintenanceMode) {
                        initializeUnitySdk(context.applicationContext, parsedConfig)
                    } else {
                        Log.d(TAG, "Ads are disabled or maintenance mode is active. Skipping Unity SDK initialization.")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching remote config: ${e.message}. Using cached/default config.", e)
            }
        }
    }

    /**
     * Loads the AdsConfig from local cache directory.
     */
    private suspend fun loadFromCache(context: Context): AdsConfig? = withContext(Dispatchers.IO) {
        try {
            val cacheFile = File(context.filesDir, CACHE_FILE_NAME)
            if (cacheFile.exists()) {
                val jsonString = cacheFile.readText()
                val parsed = configAdapter.fromJson(jsonString)
                Log.d(TAG, "Loaded configuration from local cache successfully.")
                parsed
            } else {
                Log.d(TAG, "No local config cache exists yet.")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load config from cache: ${e.message}", e)
            null
        }
    }

    /**
     * Saves the JSON configuration to a local cache file.
     */
    private suspend fun saveToCache(context: Context, jsonString: String) = withContext(Dispatchers.IO) {
        try {
            val cacheFile = File(context.filesDir, CACHE_FILE_NAME)
            cacheFile.writeText(jsonString)
            Log.d(TAG, "Saved remote config to local cache.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save config to cache: ${e.message}", e)
        }
    }

    /**
     * Initializes the Unity Ads SDK.
     */
    private fun initializeUnitySdk(context: Context, config: AdsConfig) {
        val gameId = config.ads.android.gameId
        val testMode = config.ads.testMode

        if (gameId.isBlank()) {
            Log.w(TAG, "Unity Ads Game ID is blank. Cannot initialize Unity Ads.")
            return
        }

        Log.d(TAG, "Initializing Unity Ads with Game ID: $gameId, testMode: $testMode")
        UnityAds.initialize(context, gameId, testMode, object : IUnityAdsInitializationListener {
            override fun onInitializationComplete() {
                Log.d(TAG, "Unity Ads Initialization Completed successfully.")
                _isInitialized.value = true
            }

            override fun onInitializationFailed(error: UnityAds.UnityAdsInitializationError?, message: String?) {
                Log.e(TAG, "Unity Ads Initialization Failed: [$error] $message")
                _isInitialized.value = false
            }
        })
    }

    /**
     * Displays an interstitial ad if enabled.
     */
    fun showInterstitialAd(activity: Activity, onAdClosed: () -> Unit = {}) {
        val currentConfig = _config.value
        
        if (!currentConfig.ads.enabled || !currentConfig.settings.interstitialEnabled || currentConfig.remote.maintenanceMode) {
            Log.d(TAG, "Skipping interstitial ad: ads enabled=${currentConfig.ads.enabled}, interstitial enabled=${currentConfig.settings.interstitialEnabled}, maintenance=${currentConfig.remote.maintenanceMode}")
            onAdClosed()
            return
        }

        val placement = currentConfig.ads.android.interstitial
        if (placement.isBlank()) {
            Log.w(TAG, "Interstitial placement ID is blank. Cannot load ad.")
            onAdClosed()
            return
        }

        Log.d(TAG, "Loading interstitial ad for placement: $placement")
        UnityAds.load(placement, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String?) {
                Log.d(TAG, "Interstitial ad loaded. Showing now.")
                UnityAds.show(activity, placement, object : IUnityAdsShowListener {
                    override fun onUnityAdsShowFailure(placementId: String?, error: UnityAds.UnityAdsShowError?, message: String?) {
                        Log.e(TAG, "Interstitial show failed: [$error] $message")
                        onAdClosed()
                    }

                    override fun onUnityAdsShowStart(placementId: String?) {
                        Log.d(TAG, "Interstitial show started.")
                    }

                    override fun onUnityAdsShowClick(placementId: String?) {
                        Log.d(TAG, "Interstitial clicked.")
                    }

                    override fun onUnityAdsShowComplete(placementId: String?, state: UnityAds.UnityAdsShowCompletionState?) {
                        Log.d(TAG, "Interstitial show completed with state: $state")
                        onAdClosed()
                    }
                })
            }

            override fun onUnityAdsFailedToLoad(placementId: String?, error: UnityAds.UnityAdsLoadError?, message: String?) {
                Log.e(TAG, "Interstitial failed to load: [$error] $message")
                onAdClosed()
            }
        })
    }

    /**
     * Displays a rewarded ad if enabled.
     */
    fun showRewardedAd(activity: Activity, onRewardEarned: (Boolean) -> Unit) {
        val currentConfig = _config.value

        if (!currentConfig.ads.enabled || !currentConfig.settings.rewardedEnabled || currentConfig.remote.maintenanceMode) {
            Log.d(TAG, "Skipping rewarded ad: ads enabled=${currentConfig.ads.enabled}, rewarded enabled=${currentConfig.settings.rewardedEnabled}, maintenance=${currentConfig.remote.maintenanceMode}")
            onRewardEarned(false)
            return
        }

        val placement = currentConfig.ads.android.rewarded
        if (placement.isBlank()) {
            Log.w(TAG, "Rewarded placement ID is blank. Cannot load ad.")
            onRewardEarned(false)
            return
        }

        Log.d(TAG, "Loading rewarded ad for placement: $placement")
        UnityAds.load(placement, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String?) {
                Log.d(TAG, "Rewarded ad loaded. Showing now.")
                UnityAds.show(activity, placement, object : IUnityAdsShowListener {
                    override fun onUnityAdsShowFailure(placementId: String?, error: UnityAds.UnityAdsShowError?, message: String?) {
                        Log.e(TAG, "Rewarded show failed: [$error] $message")
                        onRewardEarned(false)
                    }

                    override fun onUnityAdsShowStart(placementId: String?) {
                        Log.d(TAG, "Rewarded show started.")
                    }

                    override fun onUnityAdsShowClick(placementId: String?) {
                        Log.d(TAG, "Rewarded clicked.")
                    }

                    override fun onUnityAdsShowComplete(placementId: String?, state: UnityAds.UnityAdsShowCompletionState?) {
                        Log.d(TAG, "Rewarded show completed with state: $state")
                        if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                            onRewardEarned(true)
                        } else {
                            onRewardEarned(false)
                        }
                    }
                })
            }

            override fun onUnityAdsFailedToLoad(placementId: String?, error: UnityAds.UnityAdsLoadError?, message: String?) {
                Log.e(TAG, "Rewarded failed to load: [$error] $message")
                onRewardEarned(false)
            }
        })
    }
}
