package com.achyutpal.scrolljourney

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.achyutpal.scrolljourney.domain.model.UsageAccessState
import com.achyutpal.scrolljourney.navigation.AppNavHost
import com.achyutpal.scrolljourney.navigation.Routes
import com.achyutpal.scrolljourney.ui.theme.ScrollJourneyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Phase 1: no stored "onboarding complete" flag yet (arrives with Room in Phase 4),
        // so the permission itself decides where to start.
        val container = (application as ScrollJourneyApp).container
        val start = if (container.usageAccessRepository.checkUsageAccess() == UsageAccessState.GRANTED) {
            Routes.HOME
        } else {
            Routes.INTRO
        }

        setContent {
            ScrollJourneyTheme {
                AppNavHost(startDestination = start)
            }
        }
    }
}
