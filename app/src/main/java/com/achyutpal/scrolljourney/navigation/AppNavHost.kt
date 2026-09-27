package com.achyutpal.scrolljourney.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.achyutpal.scrolljourney.ui.apppicker.AppPickerScreen
import com.achyutpal.scrolljourney.ui.home.HomePlaceholderScreen
import com.achyutpal.scrolljourney.ui.onboarding.IntroScreen
import com.achyutpal.scrolljourney.ui.onboarding.UsageAccessScreen
import com.achyutpal.scrolljourney.ui.startup.StartDestination

object Routes {
    const val INTRO = "intro"
    const val USAGE_ACCESS = "usage_access"
    const val APP_PICKER = "app_picker"
    const val HOME = "home"
}

fun StartDestination.toRoute(): String = when (this) {
    StartDestination.ONBOARDING -> Routes.INTRO
    StartDestination.APP_PICKER -> Routes.APP_PICKER
    StartDestination.HOME -> Routes.HOME
}

@Composable
fun AppNavHost(startDestination: String) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.INTRO) {
            IntroScreen(onFinished = { navController.navigate(Routes.USAGE_ACCESS) })
        }
        composable(Routes.USAGE_ACCESS) {
            UsageAccessScreen(onGranted = { navController.navigateClearingBackStack(Routes.APP_PICKER) })
        }
        composable(Routes.APP_PICKER) {
            AppPickerScreen(
                onDone = {
                    // Opened from Home ("Change apps"): just go back. During onboarding: continue to Home.
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigateClearingBackStack(Routes.HOME)
                    }
                },
            )
        }
        composable(Routes.HOME) {
            HomePlaceholderScreen(onChangeApps = { navController.navigate(Routes.APP_PICKER) })
        }
    }
}

/** Navigate and drop everything behind, so Back can't return to finished onboarding steps. */
private fun NavHostController.navigateClearingBackStack(route: String) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
    }
}
