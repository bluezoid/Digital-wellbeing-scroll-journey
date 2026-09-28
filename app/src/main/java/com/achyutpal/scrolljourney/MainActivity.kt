package com.achyutpal.scrolljourney

import android.content.pm.ApplicationInfo
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
import com.achyutpal.scrolljourney.ui.space.SpaceViewModel
import com.achyutpal.scrolljourney.ui.startup.StartupViewModel
import com.achyutpal.scrolljourney.ui.theme.ScrollJourneyTheme

class MainActivity : ComponentActivity() {

    private val startupViewModel: StartupViewModel by viewModels { StartupViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Debug builds only: `adb shell am start -n <pkg>/.MainActivity --es debug_route <route>` jumps to a screen.
        val isDebuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        val debugRoute = if (isDebuggable) intent.getStringExtra(EXTRA_DEBUG_ROUTE) else null
        // Debug builds only: `--ei debug_fuel N` previews the 3D journey at N fuel (real data untouched).
        if (isDebuggable && intent.hasExtra(EXTRA_DEBUG_FUEL)) {
            SpaceViewModel.debugFuelOverride = intent.getIntExtra(EXTRA_DEBUG_FUEL, 0)
        }

        setContent {
            ScrollJourneyTheme {
                val start by startupViewModel.start.collectAsStateWithLifecycle()
                // The first screen needs a quick database read; show a blank background until it's known.
                start?.let { AppNavHost(startDestination = it.toRoute(), debugRoute = debugRoute) }
                    ?: Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
            }
        }
    }

    private companion object {
        const val EXTRA_DEBUG_ROUTE = "debug_route"
        const val EXTRA_DEBUG_FUEL = "debug_fuel"
    }
}
