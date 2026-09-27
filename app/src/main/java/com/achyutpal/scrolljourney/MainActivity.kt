package com.achyutpal.scrolljourney

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.achyutpal.scrolljourney.navigation.AppNavHost
import com.achyutpal.scrolljourney.navigation.toRoute
import com.achyutpal.scrolljourney.ui.startup.StartupViewModel
import com.achyutpal.scrolljourney.ui.theme.ScrollJourneyTheme

class MainActivity : ComponentActivity() {

    private val startupViewModel: StartupViewModel by viewModels { StartupViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ScrollJourneyTheme {
                val start by startupViewModel.start.collectAsStateWithLifecycle()
                // The first screen needs a quick database read; show a blank background until it's known.
                start?.let { AppNavHost(startDestination = it.toRoute()) }
                    ?: Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
            }
        }
    }
}
