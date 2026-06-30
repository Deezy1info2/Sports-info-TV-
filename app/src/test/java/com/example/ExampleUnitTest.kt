package com.example

import com.example.data.parser.M3uParser
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File

class ExampleUnitTest {
  @Test
  fun downloadSettingsJson() {
    val url = "https://pub-886883dcae414a3f8864ea53ef3887a7.r2.dev/settings.json"
    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()
    
    val response = client.newCall(request).execute()
    assertTrue("Response should be successful", response.isSuccessful)
    
    val body = response.body?.string() ?: ""
    assertFalse("Body should not be empty", body.isEmpty())
    
    println("SETTINGS_BODY_START")
    println(body)
    println("SETTINGS_BODY_END")
    try {
        File("settings_downloaded.json").writeText(body)
    } catch(e: Exception) {
        println("Could not write file: ${e.message}")
    }
  }

  @Test
  fun downloadAppIcon() {
    val url = "https://raw.githubusercontent.com/Deezy1info2/tv-logos.html/refs/heads/main/file_00000000292071f48f2f306e69eaa974.png"
    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()
    
    val response = client.newCall(request).execute()
    assertTrue("Response should be successful", response.isSuccessful)
    
    val bytes = response.body?.bytes()
    assertNotNull("Bytes should not be null", bytes)
    
    val targetPaths = listOf(
        "/app/src/main/res/drawable/app_logo.png",
        "src/main/res/drawable/app_logo.png"
    )
    
    for (path in targetPaths) {
        try {
            val file = File(path)
            file.parentFile?.mkdirs()
            file.writeBytes(bytes!!)
            println("Successfully wrote to $path")
        } catch (e: Exception) {
            println("Failed to write to $path: ${e.message}")
        }
    }
  }

  @Test
  fun testM3uParserWithLiveUrl() {
    val url = "https://raw.githubusercontent.com/Deezy1info2/exo-tv-data/main/channels.m3u"
    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()
    
    val response = client.newCall(request).execute()
    assertTrue("Response should be successful", response.isSuccessful)
    
    val body = response.body?.string() ?: ""
    assertFalse("Body should not be empty", body.isEmpty())
    
    println("Downloaded M3U payload length: ${body.length}")
    println("First 200 chars of body:\n${body.take(200)}")
    
    val channels = M3uParser.parse(ByteArrayInputStream(body.toByteArray()))
    println("Parsed channels count: ${channels.size}")
    
    if (channels.isNotEmpty()) {
        println("First parsed channel: name='${channels[0].name}', url='${channels[0].url}', group='${channels[0].group}', logo='${channels[0].logoUrl}'")
    }
    
    assertFalse("Parsed channels should not be empty", channels.isEmpty())
  }
}
