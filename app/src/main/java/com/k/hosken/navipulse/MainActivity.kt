package com.k.hosken.navipulse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.k.hosken.navipulse.ui.DashboardScreen
import com.k.hosken.navipulse.ui.DashboardViewModel
import com.k.hosken.navipulse.ui.TripDetailScreen
import com.k.hosken.navipulse.ui.theme.NaviPulseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NaviPulseTheme {
                val viewModel: DashboardViewModel = viewModel()
                var selectedTripId by remember { mutableStateOf<Long?>(null) }

                if (selectedTripId != null) {
                    TripDetailScreen(
                        tripId = selectedTripId!!,
                        viewModel = viewModel,
                        onBackClicked = { selectedTripId = null }
                    )
                } else {
                    DashboardScreen(
                        viewModel = viewModel,
                        onTripClicked = { tripId -> selectedTripId = tripId }
                    )
                }
            }
        }
    }
}