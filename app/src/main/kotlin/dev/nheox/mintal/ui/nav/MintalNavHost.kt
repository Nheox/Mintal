package dev.nheox.mintal.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.nheox.mintal.ui.grove.DayDetailScreen
import dev.nheox.mintal.ui.grove.GroveScreen
import dev.nheox.mintal.ui.grove.SessionDetailScreen
import dev.nheox.mintal.ui.home.HomeScreen
import dev.nheox.mintal.ui.onboarding.OnboardingScreen
import dev.nheox.mintal.ui.session.SessionResultScreen
import dev.nheox.mintal.ui.session.SessionRunningScreen
import dev.nheox.mintal.ui.settings.AboutScreen
import dev.nheox.mintal.ui.settings.AllowlistScreen
import dev.nheox.mintal.ui.settings.SettingsScreen
import dev.nheox.mintal.ui.species.SpeciesScreen
import dev.nheox.mintal.ui.stats.StatsScreen
import dev.nheox.mintal.ui.tags.TagsScreen

@Composable
fun MintalNavHost(startDestination: String) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onDone = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onOpenGrove = { navController.navigate(Routes.GROVE) },
                onOpenSpecies = { navController.navigate(Routes.SPECIES) },
                onOpenTags = { navController.navigate(Routes.TAGS) },
                onOpenStats = { navController.navigate(Routes.STATS) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onSessionRunning = {
                    navController.navigate(Routes.RUNNING) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(Routes.RUNNING) {
            SessionRunningScreen(
                onFinished = { sessionId ->
                    navController.navigate(Routes.result(sessionId)) {
                        popUpTo(Routes.HOME)
                    }
                },
                onAbandoned = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = Routes.RESULT_PATTERN,
            arguments = listOf(
                navArgument(Routes.ARG_SESSION_ID) { type = NavType.LongType },
            ),
        ) {
            SessionResultScreen(
                onDone = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
                onStartAnother = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.GROVE) {
            GroveScreen(
                onBack = { navController.popBackStack() },
                onOpenDay = { epochDay -> navController.navigate(Routes.day(epochDay)) },
            )
        }

        composable(
            route = Routes.DAY_PATTERN,
            arguments = listOf(
                navArgument(Routes.ARG_EPOCH_DAY) { type = NavType.LongType },
            ),
        ) {
            DayDetailScreen(
                onBack = { navController.popBackStack() },
                onOpenSession = { id -> navController.navigate(Routes.sessionDetail(id)) },
            )
        }

        composable(
            route = Routes.SESSION_DETAIL_PATTERN,
            arguments = listOf(
                navArgument(Routes.ARG_SESSION_ID) { type = NavType.LongType },
            ),
        ) {
            SessionDetailScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.STATS) {
            StatsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SPECIES) {
            SpeciesScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.TAGS) {
            TagsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenAllowlist = { navController.navigate(Routes.ALLOWLIST) },
                onOpenAbout = { navController.navigate(Routes.ABOUT) },
            )
        }

        composable(Routes.ALLOWLIST) {
            AllowlistScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}
