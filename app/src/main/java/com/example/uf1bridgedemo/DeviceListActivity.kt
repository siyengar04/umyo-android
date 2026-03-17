package com.example.uf1bridgedemo

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.uf1bridgedemo.ui.theme.UF1BridgeDemoTheme

/**
 * Entry screen: scan for uMyo devices, show connection list.
 * Max [UmyoApp.MAX_DEVICES] devices enforced in UmyoApp.
 * Navigate to [StreamingActivity] to configure and start UDP streaming.
 */
class DeviceListActivity : ComponentActivity() {

    private val app get() = application as UmyoApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val btAdapter = btManager.adapter

        setContent {
            UF1BridgeDemoTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    var logText by remember { mutableStateOf("Ready") }
                    val onLog: (String) -> Unit = { msg ->
                        runOnUiThread { logText = msg }
                    }

                    // Multi-permission launcher: covers Android 12+ (BLUETOOTH_SCAN/CONNECT)
                    // and Android 11 (ACCESS_FINE_LOCATION).
                    val permLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions()
                    ) { grants ->
                        val allOk = grants.values.all { it }
                        if (allOk) {
                            onLog("Permissions granted.")
                        } else {
                            val denied = grants.filterValues { !it }.keys.joinToString(", ")
                            onLog("Denied: $denied — BLE scan will not work.")
                        }
                    }

                    DeviceListScreen(
                        devices     = app.deviceList,
                        isScanning  = app.isScanning,
                        logText     = logText,
                        onScanToggle = {
                            if (app.isScanning) {
                                app.stopScan()
                                onLog("Scan stopped.")
                            } else {
                                val missing = missingPermissions()
                                if (missing.isNotEmpty()) {
                                    permLauncher.launch(missing.toTypedArray())
                                    return@DeviceListScreen
                                }
                                if (btAdapter == null || !btAdapter.isEnabled) {
                                    onLog("Bluetooth is OFF — enable it and try again.")
                                    return@DeviceListScreen
                                }
                                app.startScan(btAdapter, onLog)
                            }
                        },
                        onDisconnect = { mac -> app.disconnectDevice(mac) },
                        onOpenStreaming = {
                            startActivity(Intent(this, StreamingActivity::class.java))
                        }
                    )
                }
            }
        }
    }

    private fun missingPermissions(): List<String> {
        val needed = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            needed += Manifest.permission.BLUETOOTH_SCAN
            needed += Manifest.permission.BLUETOOTH_CONNECT
        }
        return needed.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
    }
}

@Composable
private fun DeviceListScreen(
    devices: List<DeviceSession>,
    isScanning: Boolean,
    logText: String,
    onScanToggle: () -> Unit,
    onDisconnect: (String) -> Unit,
    onOpenStreaming: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("uMyo Bridge", style = MaterialTheme.typography.titleMedium)

        Text(logText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onScanToggle) {
                Text(if (isScanning) "Stop Scan" else "Scan for Devices")
            }
            Button(onClick = onOpenStreaming) {
                Text("Streaming →")
            }
        }

        if (devices.isEmpty()) {
            Text(
                "No devices connected. Start scan to discover uMyo devices.",
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            Text(
                "${devices.size} / ${UmyoApp.MAX_DEVICES} devices",
                style = MaterialTheme.typography.labelMedium
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(devices, key = { it.mac }) { session ->
                    DeviceCard(session = session, onDisconnect = { onDisconnect(session.mac) })
                }
            }
        }
    }
}

@Composable
private fun DeviceCard(session: DeviceSession, onDisconnect: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(session.deviceName, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${session.mac}  rssi=${session.lastScanRssi} dBm",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    session.status.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor(session.status)
                )
            }
            Button(onClick = onDisconnect) { Text("Disconnect") }
        }
    }
}

@Composable
private fun statusColor(status: DeviceSession.Status) = when (status) {
    DeviceSession.Status.STREAMING    -> MaterialTheme.colorScheme.primary
    DeviceSession.Status.CONNECTED    -> MaterialTheme.colorScheme.secondary
    DeviceSession.Status.CONNECTING   -> MaterialTheme.colorScheme.tertiary
    DeviceSession.Status.DISCONNECTED -> MaterialTheme.colorScheme.error
    DeviceSession.Status.ERROR        -> MaterialTheme.colorScheme.error
}
