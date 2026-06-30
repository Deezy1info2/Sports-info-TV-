package com.example.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RecentStream(
    val url: String,
    val title: String? = null,
    val timestamp: Long,
    val headers: Map<String, String> = emptyMap()
)
