package com.k.hosken.navipulse

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.k.hosken.navipulse.ui.DashboardScreen
import com.k.hosken.navipulse.ui.DashboardViewModel
import com.k.hosken.navipulse.ui.ExportScreen
import com.k.hosken.navipulse.ui.FontsScreen
import com.k.hosken.navipulse.ui.MapExportScreen
import com.k.hosken.navipulse.ui.SettingsScreen
import com.k.hosken.navipulse.ui.SettingsViewModel
import com.k.hosken.navipulse.ui.TripDetailScreen
import com.k.hosken.navipulse.ui.UnitsScreen
import com.k.hosken.navipulse.ui.theme.NaviPulseTheme
import com.k.hosken.navipulse.ui.theme.toColor
import com.k.hosken.navipulse.ui.theme.toFontFamily
import com.k.hosken.navipulse.ui.theme.toScale

private sealed class Screen {
    data object Dashboard : Screen()
    data class TripDetail(val tripId: Long) : Screen()
    data object Settings : Screen()
    data object Units : Screen()
    data object Fonts : Screen()
    data object Export : Screen()
    data object MapExport : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val appFont by settingsViewModel.appFont.collectAsState()
            val textColor by settingsViewModel.textColor.collectAsState()
            val textSize by settingsViewModel.textSize.collectAsState()
            NaviPulseTheme(
                fontFamily = appFont.toFontFamily(),
                textColor = textColor.toColor(),
                textSizeScale = textSize.toScale()
            ) {
                val dashboardViewModel: DashboardViewModel = viewModel()
                var screen by remember { mutableStateOf<Screen>(Screen.Dashboard) }

                val screenOnEnabled by settingsViewModel.screenOnEnabled.collectAsState()
                DisposableEffect(screenOnEnabled) {
                    if (screenOnEnabled) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                    onDispose {}
                }

                when (val currentScreen = screen) {
                    is Screen.Dashboard -> DashboardScreen(
                        viewModel = dashboardViewModel,
                        onTripClicked = { tripId -> screen = Screen.TripDetail(tripId) },
                        onSettingsClicked = { screen = Screen.Settings },
                        onExportClicked = { screen = Screen.Export }
                    )

                    is Screen.Export -> ExportScreen(
                        viewModel = dashboardViewModel,
                        onBackClicked = { screen = Screen.Dashboard },
                        onMapExportClicked = { screen = Screen.MapExport }
                    )

                    is Screen.MapExport -> MapExportScreen(
                        viewModel = dashboardViewModel,
                        onBackClicked = { screen = Screen.Export }
                    )

                    is Screen.TripDetail -> TripDetailScreen(
                        tripId = currentScreen.tripId,
                        viewModel = dashboardViewModel,
                        onBackClicked = { screen = Screen.Dashboard }
                    )

                    is Screen.Settings -> SettingsScreen(
                        viewModel = settingsViewModel,
                        onBackClicked = { screen = Screen.Dashboard },
                        onUnitsClicked = { screen = Screen.Units },
                        onFontsClicked = { screen = Screen.Fonts }
                    )

                    is Screen.Units -> UnitsScreen(
                        viewModel = settingsViewModel,
                        onBackClicked = { screen = Screen.Settings }
                    )

                    is Screen.Fonts -> FontsScreen(
                        viewModel = settingsViewModel,
                        onBackClicked = { screen = Screen.Settings }
                    )
                }
            }
        }
    }
}
