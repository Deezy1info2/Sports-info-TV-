package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dexoplayer_prefs")

class RecentStreamStore(private val context: Context) {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, RecentStream::class.java)
    private val adapter = moshi.adapter<List<RecentStream>>(listType)

    companion object {
        private val RECENT_STREAMS_KEY = stringPreferencesKey("recent_streams")
        private val LAST_PLAYED_URL_KEY = stringPreferencesKey("last_played_url")
        private val LAST_USER_AGENT_KEY = stringPreferencesKey("last_user_agent")
        private val LAST_REFERER_KEY = stringPreferencesKey("last_referer")
        private val LAST_ORIGIN_KEY = stringPreferencesKey("last_origin")
        private val LAST_AUTH_KEY = stringPreferencesKey("last_auth")
        private val LAST_COOKIES_KEY = stringPreferencesKey("last_cookies")
        private val LAST_CUSTOM_HEADERS_KEY = stringPreferencesKey("last_custom_headers")
    }

    val recentStreams: Flow<List<RecentStream>> = context.dataStore.data.map { preferences ->
        val json = preferences[RECENT_STREAMS_KEY]
        if (json.isNullOrEmpty()) {
            emptyList()
        } else {
            try {
                adapter.fromJson(json) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    val lastPlayedUrl: Flow<String?> = context.dataStore.data.map { it[LAST_PLAYED_URL_KEY] }
    
    val savedHeaders: Flow<Map<String, String>> = context.dataStore.data.map { preferences ->
        val headers = mutableMapOf<String, String>()
        preferences[LAST_USER_AGENT_KEY]?.let { if (it.isNotEmpty()) headers["User-Agent"] = it }
        preferences[LAST_REFERER_KEY]?.let { if (it.isNotEmpty()) headers["Referer"] = it }
        preferences[LAST_ORIGIN_KEY]?.let { if (it.isNotEmpty()) headers["Origin"] = it }
        preferences[LAST_AUTH_KEY]?.let { if (it.isNotEmpty()) headers["Authorization"] = it }
        preferences[LAST_COOKIES_KEY]?.let { if (it.isNotEmpty()) headers["Cookie"] = it }
        
        val customJson = preferences[LAST_CUSTOM_HEADERS_KEY]
        if (!customJson.isNullOrEmpty()) {
            try {
                val mapType = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
                val mapAdapter = moshi.adapter<Map<String, String>>(mapType)
                mapAdapter.fromJson(customJson)?.let { headers.putAll(it) }
            } catch (e: Exception) {
                // Ignore parsing errors
            }
        }
        headers
    }

    suspend fun saveLastPlayedUrl(url: String) {
         context.dataStore.edit { it[LAST_PLAYED_URL_KEY] = url }
    }

    suspend fun saveHeaders(headers: Map<String, String>) {
        context.dataStore.edit { prefs ->
            prefs[LAST_USER_AGENT_KEY] = headers["User-Agent"] ?: ""
            prefs[LAST_REFERER_KEY] = headers["Referer"] ?: ""
            prefs[LAST_ORIGIN_KEY] = headers["Origin"] ?: ""
            prefs[LAST_AUTH_KEY] = headers["Authorization"] ?: ""
            prefs[LAST_COOKIES_KEY] = headers["Cookie"] ?: ""
            
            val customHeaders = headers.filterKeys { 
                it !in listOf("User-Agent", "Referer", "Origin", "Authorization", "Cookie")
            }
            val mapType = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
            val mapAdapter = moshi.adapter<Map<String, String>>(mapType)
            prefs[LAST_CUSTOM_HEADERS_KEY] = mapAdapter.toJson(customHeaders)
        }
    }

    suspend fun addRecentStream(url: String, title: String? = null, headers: Map<String, String>) {
        context.dataStore.edit { preferences ->
            val json = preferences[RECENT_STREAMS_KEY]
            val currentList = if (json.isNullOrEmpty()) {
                emptyList()
            } else {
                try {
                    adapter.fromJson(json) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            }

            val filtered = currentList.filterNot { it.url == url }
            val updated = listOf(
                RecentStream(
                    url = url,
                    title = title ?: url,
                    timestamp = System.currentTimeMillis(),
                    headers = headers
                )
            ) + filtered
            
            preferences[RECENT_STREAMS_KEY] = adapter.toJson(updated.take(10))
        }
    }

    suspend fun clearRecents() {
        context.dataStore.edit { prefs ->
            prefs.remove(RECENT_STREAMS_KEY)
        }
    }
}
