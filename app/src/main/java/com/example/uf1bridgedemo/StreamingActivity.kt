package com.example.uf1bridgedemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.uf1bridgedemo.ui.theme.UF1BridgeDemoTheme

/**
 * Configure UDP destination and start/stop the streaming worker thread.
 * Shows per-device notify rates for all connected sessions.
 *
 * Navigation: launched from [DeviceListActivity].
 * Host/port fields are stored in [UmyoApp] so they survive back/forward navigation.
 */
class StreamingActivity : ComponentActivity() {

    private val app get() = application as UmyoApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            UF1BridgeDemoTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    StreamingScreen(
                        host        = app.host,
                        portStr     = app.portStr,
                        isStreaming = app.isStreaming,
                        streamLog   = app.streamLog,
                        devices     = app.deviceList,
                        onHostChange = { if (!app.isStreaming) app.host = it },
                        onPortChange = { if (!app.isStreaming)
                            app.portStr = it.filter(Char::isDigit).take(5)
                        },
                        onStartStop = {
                            if (app.isStreaming) {
                                app.stopStreaming()
                            } else {
                                val port = app.portStr.toIntOrNull()?.coerceIn(1, 65535) ?: 26750
                                app.startStreaming(app.host, port)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StreamingScreen(
    host: String,
    portStr: String,
    isStreaming: Boolean,
    streamLog: String,
    devices: List<DeviceSession>,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onStartStop: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Streaming", style = MaterialTheme.typography.titleMedium)

        Text(streamLog, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())

        OutlinedTextField(
            value           = host,
            onValueChange   = onHostChange,
            label           = { Text("PC IP") },
            singleLine      = true,
            enabled         = !isStreaming,
            modifier        = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value           = portStr,
            onValueChange   = onPortChange,
            label           = { Text("Port") },
            singleLine      = true,
            enabled         = !isStreaming,
            modifier        = Modifier.fillMaxWidth()
        )

        Button(onClick = onStartStop, modifier = Modifier.fillMaxWidth()) {
            Text(if (isStreaming) "Stop Streaming" else "Start Streaming")
        }

        if (devices.isEmpty()) {
            Text(
                "No devices connected. Go back and scan for devices.",
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            Text("Connected devices:", style = MaterialTheme.typography.labelMedium)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(devices, key = { it.mac }) { session ->
                    DeviceStatsCard(session)
                }
            }
        }
    }
}

@Composable
private fun DeviceStatsCard(session: DeviceSession) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(session.deviceName, style = MaterialTheme.typography.bodyMedium)
                Text(session.mac, style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Text(
                    "${"%.1f".format(session.notifyRateFps)} notify/s",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "len=${session.lastPayloadLen}",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    session.status.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
