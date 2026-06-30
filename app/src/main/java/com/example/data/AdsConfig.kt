package com.example.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AdsConfig(
    val version: String = "1.0.0",
    val lastUpdated: String = "",
    val ads: AdsSection = AdsSection(),
    val settings: SettingsSection = SettingsSection(),
    val remote: RemoteSection = RemoteSection()
)

@JsonClass(generateAdapter = true)
data class AdsSection(
    val enabled: Boolean = false,
    val provider: String = "unity",
    val testMode: Boolean = true,
    val android: AndroidSection = AndroidSection()
)

@JsonClass(generateAdapter = true)
data class AndroidSection(
    val gameId: String = "",
    val banner: String = "",
    val interstitial: String = "",
    val rewarded: String = ""
)

@JsonClass(generateAdapter = true)
data class SettingsSection(
    val bannerEnabled: Boolean = false,
    val interstitialEnabled: Boolean = false,
    val rewardedEnabled: Boolean = false,
    val interstitialInterval: Int = 30,
    val rewardedCooldown: Int = 60
)

@JsonClass(generateAdapter = true)
data class RemoteSection(
    val minimumAppVersion: Int = 1,
    val latestVersion: Int = 1,
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String = ""
)
