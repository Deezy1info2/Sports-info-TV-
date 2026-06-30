package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.MainViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `test Room database insert and query`() = runBlocking {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val db = com.example.data.database.IptvDatabase.getDatabase(app)
    val dao = db.channelDao()
    
    dao.deleteAllChannels()
    
    val sampleChannels = listOf(
        com.example.data.database.ChannelEntity(
            name = "Test Channel 1",
            url = "http://test1.m3u8",
            group = "SPORTS"
        ),
        com.example.data.database.ChannelEntity(
            name = "Test Channel 2",
            url = "http://test2.m3u8",
            group = "Worldcup"
        )
    )
    
    dao.insertChannels(sampleChannels)
    
    val retrieved = dao.getAllChannels().first()
    println("Database Test: Retrieved ${retrieved.size} channels from DB")
    assertEquals(2, retrieved.size)
    assertEquals("Test Channel 1", retrieved[0].name)
    assertEquals("Test Channel 2", retrieved[1].name)
  }

  @Test
  fun `test MainViewModel fetch and channels flow`() = runBlocking {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = MainViewModel(app)
    
    // Start collecting filteredChannels in a background coroutine to activate the StateFlow sharing
    val collectedChannelsList = mutableListOf<List<com.example.data.database.ChannelEntity>>()
    val job = this.launch {
        viewModel.filteredChannels.collect {
            collectedChannelsList.add(it)
        }
    }
    
    // Trigger import directly
    viewModel.fetchAndImportChannels("https://raw.githubusercontent.com/Deezy1info2/exo-tv-data/main/channels.m3u")
    
    // Wait for insertion and flow emission to complete
    var channels = emptyList<com.example.data.database.ChannelEntity>()
    for (i in 1..20) {
        channels = viewModel.filteredChannels.value
        if (channels.isNotEmpty()) break
        kotlinx.coroutines.delay(500)
    }
    
    println("Integration Test: Loaded ${channels.size} channels into StateFlow")
    viewModel.devLogs.value.forEach { log ->
        println("VM LOG: [${log.tag}] ${log.message} (isError=${log.isError})")
    }
    
    job.cancel() // stop collecting
    assertTrue("StateFlow should have populated channels", channels.isNotEmpty())
  }
}
