package com.achyutpal.scrolljourney.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.achyutpal.scrolljourney.ui.home.HomePlaceholderScreen
import com.achyutpal.scrolljourney.ui.onboarding.IntroScreen
import com.achyutpal.scrolljourney.ui.onboarding.UsageAccessScreen

object Routes {
    const val INTRO = "intro"
    const val USAGE_ACCESS = "usage_access"
    const val HOME = "home"
}

@Composable
fun AppNavHost(startDestination: String) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.INTRO) {
            IntroScreen(onFinished = { navController.navigate(Routes.USAGE_ACCESS) })
        }
        composable(Routes.USAGE_ACCESS) {
            UsageAccessScreen(
                onGranted = {
                    // Clear onboarding from the back stack so Back doesn't return to it.
                    navController.navigate(Routes.HOME) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.HOME) {
            HomePlaceholderScreen()
        }
    }
}
