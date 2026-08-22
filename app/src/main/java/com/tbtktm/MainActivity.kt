package com.tbtktm

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import com.tbtktm.ui.screens.DashboardScreen
import com.tbtktm.ui.screens.DeviceScanScreen
import com.tbtktm.ui.screens.KeyMappingScreen
import com.tbtktm.ui.theme.DarkBackground
import com.tbtktm.ui.theme.DarkCard
import com.tbtktm.ui.theme.KtmOrange
import com.tbtktm.ui.theme.TbTKTMTheme
import com.tbtktm.ui.theme.TftTextDim

import androidx.compose.material.icons.filled.Speed
import com.tbtktm.ui.screens.CockpitHudScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Gösterge", Icons.Default.Dashboard)
    object Cockpit : Screen("cockpit", "Kokpit", Icons.Default.Speed)
    object Scan : Screen("scan", "Motosiklet", Icons.Default.BluetoothSearching)
    object KeyMapping : Screen("keys", "Gidon", Icons.Default.Gamepad)
}

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestRequiredPermissions()
        ensureNotificationServiceRunning()

        setContent {
            TbTKTMTheme {
                MainAppContainer(
                    onOpenNotificationSettings = {
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        com.tbtktm.ble.KtmBleManager.getInstance(this).autoConnect()
    }

    private fun ensureNotificationServiceRunning() {
        try {
            val componentName = ComponentName(this, com.tbtktm.parser.NotificationParserService::class.java)
            packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        } catch (_: Exception) {}
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }
}

@Composable
fun MainAppContainer(
    onOpenNotificationSettings: () -> Unit
) {
    val strings by com.tbtktm.i18n.AppLanguageManager.strings.collectAsState()
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    // Yalnızca TbT Navigasyon & Bildirim (Gösterge) ve Bluetooth Bağlantı (Motosiklet) sekmeleri aktif
    val items = listOf(Screen.Dashboard, Screen.Scan)

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = DarkCard,
                contentColor = Color.White
            ) {
                items.forEach { screen ->
                    val labelText = when (screen) {
                        is Screen.Dashboard -> strings.tabDashboard
                        is Screen.Cockpit -> strings.tabCockpit
                        is Screen.Scan -> strings.tabMotorcycle
                        is Screen.KeyMapping -> strings.tabHandlebar
                    }
                    NavigationBarItem(
                        icon = { Icon(imageVector = screen.icon, contentDescription = labelText) },
                        label = { Text(text = labelText) },
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = KtmOrange,
                            indicatorColor = KtmOrange,
                            unselectedIconColor = TftTextDim,
                            unselectedTextColor = TftTextDim
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                is Screen.Dashboard -> DashboardScreen(
                    onNavigateToScan = { currentScreen = Screen.Scan }
                )
                is Screen.Cockpit -> CockpitHudScreen()
                is Screen.Scan -> DeviceScanScreen(
                    onDeviceSelected = { currentScreen = Screen.Dashboard }
                )
                is Screen.KeyMapping -> KeyMappingScreen()
            }
        }
    }
}
