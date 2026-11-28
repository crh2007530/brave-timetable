package com.xingheyuzhuan.shiguangschedule.ui.campus

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import shiguangschedule.shared.generated.resources.*

private data class NearbyDevice(val id: String, val name: String?, val rssi: Int?)

@SuppressLint("MissingPermission")
@Composable
internal actual fun WaterBluetoothDiagnostic() {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    val permissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    var scanJob by remember { mutableStateOf<Job?>(null) }
    var nearby by remember { mutableStateOf<Map<String, NearbyDevice>>(emptyMap()) }
    var status by remember { mutableStateOf<StringResource?>(null) }

    fun startScan() {
        val manager = context.getSystemService(BluetoothManager::class.java)
        val scanner = try {
            manager?.adapter?.bluetoothLeScanner
        } catch (_: SecurityException) {
            status = Res.string.campus_ble_permission
            return
        }
        if (scanner == null) {
            status = Res.string.campus_ble_unavailable
            return
        }
        nearby = emptyMap()
        status = Res.string.campus_ble_scanning
        scanJob = scope.launch {
            try {
                withTimeoutOrNull(8_000L) {
                    scanner.scanAdvertisements().collect { device ->
                        val previous = nearby[device.id]
                        val latest = device.copy(
                            name = device.name ?: previous?.name,
                            rssi = device.rssi ?: previous?.rssi
                        )
                        nearby = (nearby + (device.id to latest)).values
                            .sortedByDescending { it.rssi ?: Int.MIN_VALUE }
                            .take(12)
                            .associateBy { it.id }
                    }
                }
                status = if (nearby.isEmpty()) Res.string.campus_ble_empty else null
            } catch (_: SecurityException) {
                status = Res.string.campus_ble_permission
            } catch (_: IllegalStateException) {
                status = if (nearby.isEmpty()) Res.string.campus_ble_failed else Res.string.campus_ble_partial
            } finally {
                scanJob = null
            }
        }
    }

    val requestPermissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (permissions.all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }) {
            startScan()
        } else {
            status = Res.string.campus_ble_permission
        }
    }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE && scanJob != null) {
                scanJob?.cancel()
                status = null
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            scanJob?.cancel()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        HorizontalDivider()
        Text(stringResource(Res.string.campus_ble_explanation), style = MaterialTheme.typography.bodySmall)
        TextButton(
            enabled = scanJob == null,
            onClick = {
                if (permissions.all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }) {
                    startScan()
                } else {
                    requestPermissions.launch(permissions)
                }
            }
        ) {
            Text(stringResource(Res.string.campus_ble_scan))
        }
        status?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall) }
        for (device in nearby.values) {
            val name = device.name ?: stringResource(Res.string.campus_ble_unnamed)
            val signal = device.rssi?.let { "$it dBm" } ?: stringResource(Res.string.campus_ble_signal_unknown)
            Text(
                "$name · …${device.id.takeLast(5)} · $signal",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@SuppressLint("MissingPermission")
private fun BluetoothLeScanner.scanAdvertisements() = callbackFlow {
    val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val rssi = result.rssi.takeIf { it in -127..126 }
            trySend(NearbyDevice(result.device.address, result.scanRecord?.deviceName, rssi))
        }

        override fun onScanFailed(errorCode: Int) {
            close(IllegalStateException("BLE scan failed ($errorCode)"))
        }
    }
    startScan(callback)
    awaitClose {
        try {
            stopScan(callback)
        } catch (_: SecurityException) {
            // Permission can be revoked while the scan is running.
        } catch (_: IllegalStateException) {
            // Bluetooth may have been turned off before the scan ended.
        }
    }
}
