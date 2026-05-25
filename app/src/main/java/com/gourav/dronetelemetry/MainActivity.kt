package com.gourav.dronetelemetry

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.gourav.dronetelemetry.presentation.TelemetryViewModel
import com.gourav.dronetelemetry.presentation.ui.DroneTelemetryScreen
import com.gourav.dronetelemetry.ui.theme.DroneTelemetryTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: TelemetryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            DroneTelemetryTheme {
                DroneTelemetryScreen(
                    uiState = uiState,
                    onProtocolChange = viewModel::onProtocolChange,
                    onHostChange = viewModel::onHostChange,
                    onPortChange = viewModel::onPortChange,
                    onConnect = viewModel::connect,
                    onDisconnect = viewModel::disconnect,
                    onAction = viewModel::onAction
                )
            }
        }
    }
}
