package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.Coil
import coil.ImageLoader
import com.example.data.AppDatabase
import com.example.data.MediaRepository
import com.example.ui.AppNavGraph
import com.example.ui.MediaViewModel
import com.example.ui.MediaViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ThemePreferences
import okhttp3.OkHttpClient

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configure global Coil ImageLoader with standard browser User-Agent so CDNs (Wikimedia, AniList, Kitsu) don't block images with HTTP 403
        val customImageLoader = ImageLoader.Builder(this)
            .okHttpClient {
                OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                            .build()
                        chain.proceed(request)
                    }
                    .build()
            }
            .crossfade(true)
            .build()
        Coil.setImageLoader(customImageLoader)

        val database = AppDatabase.getDatabase(this)
        val repository = MediaRepository(database.mediaDao())
        val viewModelFactory = MediaViewModelFactory(repository)
        val viewModel: MediaViewModel by viewModels { viewModelFactory }
        val themePreferences = ThemePreferences(applicationContext)
        
        enableEdgeToEdge()
        setContent {
            val currentTheme by themePreferences.currentTheme.collectAsStateWithLifecycle()

            MyApplicationTheme(appTheme = currentTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavGraph(
                        viewModel = viewModel,
                        themePreferences = themePreferences
                    )
                }
            }
        }
    }
}

