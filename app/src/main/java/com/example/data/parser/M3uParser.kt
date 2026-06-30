package com.example.data.parser

import com.example.data.database.ChannelEntity
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

object M3uParser {
    fun parse(inputStream: InputStream, playlistUrl: String? = null): List<ChannelEntity> {
        val channels = mutableListOf<ChannelEntity>()
        val reader = BufferedReader(InputStreamReader(inputStream))
        var line: String?
        
        var currentName = ""
        var currentLogoUrl: String? = null
        var currentGroup: String? = null
        
        val logoRegex = """tvg-logo=["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE)
        val groupRegex = """group-title=["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE)

        try {
            while (reader.readLine().also { line = it } != null) {
                val trimmedLine = line?.trim() ?: continue
                if (trimmedLine.isEmpty()) continue
                
                if (trimmedLine.startsWith("#EXTINF:") || trimmedLine.startsWith("#EXTINF ")) {
                    currentName = ""
                    currentLogoUrl = null
                    currentGroup = null
                    
                    logoRegex.find(trimmedLine)?.let { match ->
                        currentLogoUrl = match.groupValues[1]
                    }
                    
                    groupRegex.find(trimmedLine)?.let { match ->
                        currentGroup = match.groupValues[1]
                    }
                    
                    val nameStartIndex = trimmedLine.lastIndexOf(',')
                    if (nameStartIndex != -1 && nameStartIndex < trimmedLine.length - 1) {
                        currentName = trimmedLine.substring(nameStartIndex + 1).trim()
                    }
                } else if (!trimmedLine.startsWith("#")) {
                    val streamUrl = trimmedLine
                    if (streamUrl.startsWith("http://") || streamUrl.startsWith("https://") || 
                        streamUrl.startsWith("rtmp://") || streamUrl.startsWith("rtsp://")) {
                        
                        var finalName = currentName
                        if (finalName.isEmpty()) {
                            finalName = streamUrl.substringAfterLast('/').substringBefore('?')
                            if (finalName.isEmpty()) finalName = "Unnamed Channel"
                        }
                        
                        channels.add(
                            ChannelEntity(
                                name = finalName,
                                url = streamUrl,
                                logoUrl = currentLogoUrl,
                                group = currentGroup ?: "Other",
                                playlistUrl = playlistUrl
                            )
                        )
                        
                        currentName = ""
                        currentLogoUrl = null
                        currentGroup = null
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                reader.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
        
        return channels
    }
}
