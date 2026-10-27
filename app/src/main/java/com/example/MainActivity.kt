package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.JarvisMainScreen
import com.example.ui.theme.JarvisTheme
import com.example.ui.viewmodel.JarvisViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: JarvisViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            JarvisTheme(selectedTheme = uiState.currentTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    JarvisMainScreen(viewModel = viewModel)
                }
            }
        }
    }
}
