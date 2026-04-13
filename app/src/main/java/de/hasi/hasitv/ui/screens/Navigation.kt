package de.hasi.hasitv.ui.screens

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.hasi.hasitv.data.model.Channel
import de.hasi.hasitv.viewmodel.MainViewModel

sealed class Screen(val route: String) {
    object Home     : Screen("home")
    object Player   : Screen("player")
    object EPG      : Screen("epg")
    object Settings : Screen("settings")
    object AddPlaylist : Screen("add_playlist")
}

@Composable
fun AppNavigation(
    vm: MainViewModel,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Home.route) {

        composable(Screen.Home.route) {
            HomeScreen(
                vm = vm,
                onPlayChannel = { channel ->
                    vm.selectChannel(channel)
                    navController.navigate(Screen.Player.route)
                },
                onOpenEpg      = { navController.navigate(Screen.EPG.route) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Player.route) {
            PlayerScreen(
                vm     = vm,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.EPG.route) {
            EpgScreen(
                vm     = vm,
                onBack = { navController.popBackStack() },
                onPlayChannel = { channel ->
                    vm.selectChannel(channel)
                    navController.navigate(Screen.Player.route)
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                vm            = vm,
                darkTheme     = darkTheme,
                onToggleTheme = onToggleTheme,
                onBack        = { navController.popBackStack() },
                onAddPlaylist = { navController.navigate(Screen.AddPlaylist.route) }
            )
        }

        composable(Screen.AddPlaylist.route) {
            AddPlaylistScreen(
                vm     = vm,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
