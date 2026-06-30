package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels ORDER BY name ASC")
    fun getAllChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchChannels(query: String): Flow<List<ChannelEntity>>

    @Query("SELECT DISTINCT `group` FROM channels WHERE `group` IS NOT NULL AND `group` != '' ORDER BY `group` ASC")
    fun getDistinctGroups(): Flow<List<String>>

    @Query("SELECT * FROM channels WHERE `group` = :group ORDER BY name ASC")
    fun getChannelsByGroup(group: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE isFavorite = 1")
    suspend fun getFavoriteChannels(): List<ChannelEntity>

    @Query("UPDATE channels SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Int, isFavorite: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Query("DELETE FROM channels")
    suspend fun deleteAllChannels()

    @Query("DELETE FROM channels WHERE playlistUrl = :playlistUrl")
    suspend fun deletePlaylistChannels(playlistUrl: String)
}
