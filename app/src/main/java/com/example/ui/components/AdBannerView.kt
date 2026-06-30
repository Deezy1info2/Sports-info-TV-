package com.example.ui.components

import android.app.Activity
import android.util.Log
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.AdsManager
import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize

@Composable
fun AdBannerView(modifier: Modifier = Modifier) {
    val config by AdsManager.config.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    // Tracking load success to avoid empty white placeholder space if it fails
    var adLoaded by remember { mutableStateOf(false) }
    var loadFailed by remember { mutableStateOf(false) }

    val isAdsEnabled = config.ads.enabled
    val isBannerEnabled = config.settings.bannerEnabled
    val isMaintenance = config.remote.maintenanceMode
    val placementId = config.ads.android.banner

    if (activity != null && isAdsEnabled && isBannerEnabled && !isMaintenance && placementId.isNotBlank() && !loadFailed) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(Color(0xFF1C1B1F))
        ) {
            Divider(color = Color.White.copy(alpha = 0.12f), thickness = 0.5.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    factory = { ctx ->
                        FrameLayout(ctx).apply {
                            val unityBanner = BannerView(activity, placementId, UnityBannerSize(320, 50))
                            unityBanner.listener = object : BannerView.IListener {
                                override fun onBannerLoaded(bannerView: BannerView?) {
                                    Log.d("AdBannerView", "Unity banner loaded successfully: $placementId")
                                    adLoaded = true
                                    loadFailed = false
                                }

                                override fun onBannerClick(bannerView: BannerView?) {
                                    Log.d("AdBannerView", "Unity banner clicked")
                                }

                                override fun onBannerFailedToLoad(bannerView: BannerView?, errorInfo: BannerErrorInfo?) {
                                    Log.e("AdBannerView", "Unity banner failed to load: [${errorInfo?.errorCode}] ${errorInfo?.errorMessage}")
                                    loadFailed = true
                                    adLoaded = false
                                }

                                override fun onBannerLeftApplication(bannerView: BannerView?) {
                                    Log.d("AdBannerView", "Unity banner left application")
                                }

                                override fun onBannerShown(bannerView: BannerView?) {
                                    Log.d("AdBannerView", "Unity banner shown")
                                }
                            }
                            addView(unityBanner)
                            unityBanner.load()
                        }
                    },
                    update = { _ -> }
                )
            }
        }
    }
}
