package com.achyutpal.scrolljourney.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.achyutpal.scrolljourney.ui.apppicker.AppPickerScreen
import com.achyutpal.scrolljourney.ui.dailylimit.DailyLimitScreen
import com.achyutpal.scrolljourney.ui.dashboard.DashboardScreen
import com.achyutpal.scrolljourney.ui.history.HistoryScreen
import com.achyutpal.scrolljourney.ui.onboarding.IntroScreen
import com.achyutpal.scrolljourney.ui.onboarding.UsageAccessScreen
import com.achyutpal.scrolljourney.ui.startup.StartDestination

object Routes {
    const val ARG_ONBOARDING = "onboarding"

    const val INTRO = "intro"
    const val USAGE_ACCESS = "usage_access"
    const val DASHBOARD = "dashboard"
    const val HISTORY = "history"

    // Picker and limit are used both in onboarding (default) and from the dashboard (onboarding=false).
    const val APP_PICKER = "app_picker?$ARG_ONBOARDING={$ARG_ONBOARDING}"
    const val DAILY_LIMIT = "daily_limit?$ARG_ONBOARDING={$ARG_ONBOARDING}"

    fun appPicker(onboarding: Boolean) = "app_picker?$ARG_ONBOARDING=$onboarding"
    fun dailyLimit(onboarding: Boolean) = "daily_limit?$ARG_ONBOARDING=$onboarding"
}

fun StartDestination.toRoute(): String = when (this) {
    StartDestination.ONBOARDING -> Routes.INTRO
    StartDestination.APP_PICKER -> Routes.APP_PICKER
    StartDestination.DASHBOARD -> Routes.DASHBOARD
}

private val onboardingArgument = listOf(
    navArgument(Routes.ARG_ONBOARDING) {
        type = NavType.BoolType
        defaultValue = true
    },
)

/** Onboarding: intro → usage access → app picker → daily limit → dashboard. */
@Composable
fun AppNavHost(startDestination: String) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.INTRO) {
            IntroScreen(onFinished = { navController.navigate(Routes.USAGE_ACCESS) })
        }
        composable(Routes.USAGE_ACCESS) {
            UsageAccessScreen(onGranted = { navController.navigateClearingBackStack(Routes.appPicker(onboarding = true)) })
        }
        composable(Routes.APP_PICKER, arguments = onboardingArgument) { entry ->
            val onboarding = entry.arguments?.getBoolean(Routes.ARG_ONBOARDING) ?: true
            AppPickerScreen(
                onDone = {
                    if (onboarding) {
                        navController.navigate(Routes.dailyLimit(onboarding = true))
                    } else {
                        navController.popBackStack()
                    }
                },
                onBack = if (onboarding) null else { { navController.popBackStack() } },
            )
        }
        composable(Routes.DAILY_LIMIT, arguments = onboardingArgument) { entry ->
            val onboarding = entry.arguments?.getBoolean(Routes.ARG_ONBOARDING) ?: true
            DailyLimitScreen(
                isOnboarding = onboarding,
                onDone = {
                    if (onboarding) {
                        navController.navigateClearingBackStack(Routes.DASHBOARD)
                    } else {
                        navController.popBackStack()
                    }
                },
                onBack = if (onboarding) null else { { navController.popBackStack() } },
            )
        }
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onChangeApps = { navController.navigate(Routes.appPicker(onboarding = false)) },
                onChangeLimit = { navController.navigate(Routes.dailyLimit(onboarding = false)) },
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
            )
        }
        composable(Routes.HISTORY) {
            HistoryScreen(onBack = { navController.popBackStack() })
        }
    }
}

/** Navigate and drop everything behind, so Back can't return to finished onboarding steps. */
private fun NavHostController.navigateClearingBackStack(route: String) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
    }
}
