package com.tbtktm.ui.screens

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbtktm.ble.KtmBleManager
import com.tbtktm.i18n.AppLanguageManager
import com.tbtktm.model.BleDeviceItem
import com.tbtktm.ui.theme.DarkCard
import com.tbtktm.ui.theme.DarkCardBorder
import com.tbtktm.ui.theme.KtmOrange
import com.tbtktm.ui.theme.StatusGreen
import com.tbtktm.ui.theme.TftTextCyan
import com.tbtktm.ui.theme.TftTextDim

@SuppressLint("MissingPermission")
@Composable
fun DeviceScanScreen(
    onDeviceSelected: () -> Unit
) {
    val context = LocalContext.current
    val strings by AppLanguageManager.strings.collectAsState()
    val bleManager = KtmBleManager.getInstance(context)

    val bluetoothManager = remember { context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager }
    val bluetoothAdapter: BluetoothAdapter? = remember { bluetoothManager?.adapter }
    val scanner = remember { bluetoothAdapter?.bluetoothLeScanner }

    val scannedDevices = remember { mutableStateListOf<BleDeviceItem>() }
    val pairedDevices = remember { mutableStateListOf<BleDeviceItem>() }
    var isScanning by remember { mutableStateOf(false) }

    fun loadPairedDevices() {
        pairedDevices.clear()
        val bonded = bluetoothAdapter?.bondedDevices ?: emptySet()
        for (device in bonded) {
            val name = device.name ?: strings.pairedTag
            val isKtm = name.contains("KTM", ignoreCase = true) || name.contains("SPORTMOTORCYCLE", ignoreCase = true)
            pairedDevices.add(BleDeviceItem(name, device.address, 0, isKtm))
        }
    }

    LaunchedEffect(Unit) {
        loadPairedDevices()
    }

    val scanCallback = remember {
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                val device = result?.device ?: return
                val scanRecord = result.scanRecord
                val rawName = device.name ?: scanRecord?.deviceName
                val address = device.address
                val rssi = result.rssi

                val name = if (!rawName.isNullOrBlank()) rawName else "${strings.unknownDevice} (${address.takeLast(5)})"
                val isKtm = name.contains("KTM", ignoreCase = true) || name.contains("SPORTMOTORCYCLE", ignoreCase = true)

                val existingIndex = scannedDevices.indexOfFirst { it.address == address }
                if (existingIndex >= 0) {
                    scannedDevices[existingIndex] = BleDeviceItem(name, address, rssi, isKtm)
                } else {
                    scannedDevices.add(BleDeviceItem(name, address, rssi, isKtm))
                }
            }
        }
    }

    fun startBleScan() {
        if (scanner == null || isScanning) return
        scannedDevices.clear()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        scanner.startScan(null, settings, scanCallback)
        isScanning = true
    }

    fun stopBleScan() {
        if (scanner == null || !isScanning) return
        try {
            scanner.stopScan(scanCallback)
        } catch (_: Exception) {}
        isScanning = false
    }

    DisposableEffect(Unit) {
        onDispose {
            stopBleScan()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Üst Başlık
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = strings.scanScreenTitle,
                    color = KtmOrange,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = strings.scanScreenSubtitle,
                    color = TftTextDim,
                    fontSize = 11.sp
                )
            }

            Button(
                onClick = {
                    if (isScanning) stopBleScan() else startBleScan()
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (isScanning) Color(0xFF444444) else KtmOrange),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = strings.btnStopScan, fontSize = 11.sp, color = Color.White)
                } else {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = strings.btnStartScan, fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Eşleşmiş Cihazlar Bölümü
            if (pairedDevices.isNotEmpty()) {
                item {
                    Text(
                        text = strings.pairedDevicesHeader,
                        color = TftTextCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )
                }

                items(pairedDevices) { item ->
                    DeviceItemCard(
                        deviceItem = item,
                        connectLabel = strings.btnConnect,
                        onClick = {
                            stopBleScan()
                            bleManager.connect(item.address)
                            onDeviceSelected()
                        }
                    )
                }
            }

            // 2. Taranan Cihazlar Bölümü
            item {
                Text(
                    text = strings.discoveredDevicesHeader,
                    color = KtmOrange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
            }

            if (scannedDevices.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.noDiscoveredDevices,
                            color = TftTextDim,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                items(scannedDevices) { item ->
                    DeviceItemCard(
                        deviceItem = item,
                        connectLabel = strings.btnConnect,
                        onClick = {
                            stopBleScan()
                            bleManager.connect(item.address)
                            onDeviceSelected()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceItemCard(
    deviceItem: BleDeviceItem,
    connectLabel: String,
    onClick: () -> Unit
) {
    val isKtm = deviceItem.isKtmDevice

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isKtm) KtmOrange else DarkCardBorder,
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = if (isKtm) KtmOrange else Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = deviceItem.name,
                        color = if (isKtm) KtmOrange else Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = deviceItem.address,
                        color = TftTextDim,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (deviceItem.rssi != 0) {
                    Icon(
                        imageVector = Icons.Default.SignalCellularAlt,
                        contentDescription = null,
                        tint = StatusGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${deviceItem.rssi} dBm",
                        color = StatusGreen,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }

                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(containerColor = if (isKtm) KtmOrange else DarkCardBorder),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = connectLabel,
                        color = if (isKtm) Color.Black else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
