package de.hasi.hasitv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import de.hasi.hasitv.ui.screens.AppNavigation
import de.hasi.hasitv.ui.theme.HasiTVTheme
import de.hasi.hasitv.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Preference-based dark/light toggle
            var darkTheme by remember { mutableStateOf(true) }

            HasiTVTheme(darkTheme = darkTheme) {
                AppNavigation(
                    vm       = vm,
                    darkTheme= darkTheme,
                    onToggleTheme = { darkTheme = !darkTheme }
                )
            }
        }
    }
}
