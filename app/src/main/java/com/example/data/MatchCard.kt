package com.example.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MatchCard(
    val competition: String,
    val homeTeam: String,
    val awayTeam: String,
    val homeLogo: String,
    val awayLogo: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val duration: String,
    val status: String,
    val startDateTime: String,
    val endDateTime: String,
    val servers: List<Server>
)

@JsonClass(generateAdapter = true)
data class Server(
    val name: String,
    val url: String
)
