package com.hasielectronic.hasitv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import com.hasielectronic.hasitv.ui.screens.AppNavigation
import com.hasielectronic.hasitv.ui.theme.HasiTVTheme
import com.hasielectronic.hasitv.viewmodel.MainViewModel

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
