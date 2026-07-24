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
import com.example.data.AppDatabase
import com.example.data.MediaRepository
import com.example.ui.AppNavGraph
import com.example.ui.MediaViewModel
import com.example.ui.MediaViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ThemePreferences

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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

