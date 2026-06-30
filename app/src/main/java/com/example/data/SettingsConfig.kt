package com.example.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SettingsResponse(
    val settings: DynamicSettings = DynamicSettings()
)

@JsonClass(generateAdapter = true)
data class DynamicSettings(
    val darkMode: DarkModeConfig = DarkModeConfig(),
    val followUs: Map<String, SocialConfig> = emptyMap(),
    val menu: List<MenuItemConfig> = emptyList()
)

@JsonClass(generateAdapter = true)
data class DarkModeConfig(
    val enabled: Boolean = false
)

@JsonClass(generateAdapter = true)
data class SocialConfig(
    val enabled: Boolean = false,
    val title: String = "",
    val url: String = ""
)

@JsonClass(generateAdapter = true)
data class MenuItemConfig(
    val id: String = "",
    val icon: String = "",
    val title: String = "",
    val subtitle: String? = null,
    val enabled: Boolean = true,
    val visible: Boolean = true,
    val url: String? = null,
    val email: String? = null,
    val content: String? = null
)
