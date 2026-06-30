package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val url: String,
    val logoUrl: String? = null,
    val group: String? = null,
    val playlistUrl: String? = null,
    val isFavorite: Boolean = false
)
