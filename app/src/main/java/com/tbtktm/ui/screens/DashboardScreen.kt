package com.tbtktm.ui.screens

import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.core.app.NotificationManagerCompat
import com.tbtktm.TbTApplication
import com.tbtktm.ble.KtmBccuSimulator
import com.tbtktm.ble.KtmBleManager
import com.tbtktm.ble.KtmRfcommManager
import com.tbtktm.i18n.AppLanguage
import com.tbtktm.i18n.AppLanguageManager
import com.tbtktm.model.KtmTurnIcon
import com.tbtktm.model.NavSource
import com.tbtktm.model.NavigationData
import com.tbtktm.ui.components.TftDashboardCard
import com.tbtktm.ui.theme.DarkCard
import com.tbtktm.ui.theme.DarkCardBorder
import com.tbtktm.ui.theme.KtmOrange
import com.tbtktm.ui.theme.StatusGreen
import com.tbtktm.ui.theme.StatusRed
import com.tbtktm.ui.theme.TftTextCyan
import com.tbtktm.ui.theme.TftTextDim
import com.tbtktm.util.FileLogger

@Composable
fun DashboardScreen(
    onNavigateToScan: () -> Unit
) {
    val context = LocalContext.current
    val bleManager = KtmBleManager.getInstance(context)
    val rfcommManager = KtmRfcommManager.getInstance(context)

    val navData by TbTApplication.currentNavData.collectAsState()
    val connectionState by bleManager.connectionState.collectAsState()
    val connectedDeviceName by bleManager.connectedDeviceName.collectAsState()
    val rfcommChannelsCount by rfcommManager.connectedChannelsCount.collectAsState()
    val logs by TbTApplication.diagnosticLogs.collectAsState()

    val currentLang by AppLanguageManager.currentLanguage.collectAsState()
    val strings by AppLanguageManager.strings.collectAsState()

    var showLanguageDialog by remember { mutableStateOf(false) }

    val prefs = remember { context.getSharedPreferences("tbtktm_ble_prefs", Context.MODE_PRIVATE) }
    var enteredVin by remember { mutableStateOf(prefs.getString("saved_vin", "") ?: "") }

    var isNotificationAccessGranted by remember { mutableStateOf(false) }

    fun checkNotificationAccess() {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        isNotificationAccessGranted = enabledPackages.contains(context.packageName)
    }

    LaunchedEffect(Unit) {
        checkNotificationAccess()
    }

    val isConnected = connectionState == BluetoothProfile.STATE_CONNECTED || rfcommChannelsCount > 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Üst Başlık ve Dil Seçici Barı
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = strings.appTitle,
                    color = KtmOrange,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = strings.appSubtitle,
                    color = TftTextDim,
                    fontSize = 10.sp
                )
            }

            // Dil Seçici Butonu
            OutlinedButton(
                onClick = { showLanguageDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, KtmOrange.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(text = currentLang.flagEmoji, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = currentLang.code.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KtmOrange)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 1. Bildirim İzin Durumu Uyarısı
        if (!isNotificationAccessGranted) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StatusRed, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = StatusRed.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = StatusRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = strings.notifPermissionRequired,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Google Maps / WhatsApp / E-Mail",
                                color = TftTextDim,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = strings.btnGrantPermission, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 2. Bağlantı Durum Kartı
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) StatusGreen else StatusRed)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isConnected) (connectedDeviceName ?: "KTM SPORTMOTORCYCLE") else strings.connectionCardTitle,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isConnected) strings.connectedStatus else strings.disconnectedStatus,
                            color = if (isConnected) StatusGreen else TftTextDim,
                            fontSize = 12.sp
                        )
                    }
                }

                Button(
                    onClick = onNavigateToScan,
                    colors = ButtonDefaults.buttonColors(containerColor = if (isConnected) DarkCardBorder else KtmOrange),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = if (isConnected) strings.btnManage else strings.btnConnect, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. KTM 1290 VIN / Yetkilendirme Kartı
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, KtmOrange.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = KtmOrange, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.vinCardTitle,
                        color = KtmOrange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = enteredVin,
                        onValueChange = {
                            enteredVin = it.uppercase().trim()
                            prefs.edit().putString("saved_vin", enteredVin).apply()
                        },
                        placeholder = { Text(strings.vinPlaceholder, color = TftTextDim, fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = KtmOrange,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val targetAddress = bleManager.getLastConnectedAddress()
                            if (!targetAddress.isNullOrBlank()) {
                                rfcommManager.connect(targetAddress, enteredVin.ifBlank { "12345678" })
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KtmOrange),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = strings.btnSaveVin, color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Canlı TFT Simülatör Kartı
        TftDashboardCard(navData = navData)

        Spacer(modifier = Modifier.height(14.dp))

        // 5. Test Butonları
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    val testNav = NavigationData(
                        isActive = true,
                        turnIcon = KtmTurnIcon.QUITE_RIGHT,
                        distanceToTurn = "350 m",
                        roadName = "Bagdat Caddesi",
                        eta = "19:15",
                        distanceToDestination = "8.4 km",
                        source = NavSource.GOOGLE_MAPS
                    )
                    TbTApplication.updateNavigationData(testNav)
                    bleManager.sendNavigationUpdate(testNav)
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = KtmOrange),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = strings.btnPushTestData, color = Color.Black, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    val emptyNav = NavigationData(isActive = false)
                    TbTApplication.updateNavigationData(emptyNav)
                    bleManager.sendNavigationUpdate(emptyNav)
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Stop, contentDescription = null, tint = StatusRed)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = strings.btnStop)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 5.1 Sanal Motosiklet Simülatör Butonu
        Button(
            onClick = {
                KtmBccuSimulator.runFullProtocolSimulation()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = DarkCardBorder),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = StatusGreen)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = strings.btnSimulateMotorcycle, color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 5.2 WhatsApp & E-Posta Kayan Yazı Test Butonu
        Button(
            onClick = {
                val ticker = com.tbtktm.ticker.TftMarqueeTicker(bleManager)
                val mockNotif = com.tbtktm.parser.AppNotificationData(
                    appName = "WhatsApp",
                    senderOrTitle = "Ahmet Yılmaz",
                    fullMessageText = "Toplantı yarın saat 15:00'te Levent ofisinde yapılacak, herkesin katılması rica olunur.",
                    timeFormatted = "14:25",
                    badgeText = "MSG"
                )
                ticker.displayNotification(mockNotif)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366).copy(alpha = 0.8f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = strings.btnTestWhatsAppTicker, color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 6. Canlı Bildirim & BLE Log Paneli
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.liveLogsTitle,
                        color = KtmOrange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    Text(
                        text = strings.btnClearLogs,
                        color = TftTextCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            FileLogger.clearLogs()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F0F0F))
                        .padding(10.dp)
                ) {
                    if (logs.isEmpty()) {
                        Text(
                            text = strings.noLogsYet,
                            color = TftTextDim,
                            fontSize = 12.sp
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            logs.takeLast(30).forEach { logLine ->
                                Text(
                                    text = logLine,
                                    color = if (logLine.contains("GELDİ") || logLine.contains("BAŞARIYLA")) StatusGreen
                                    else if (logLine.contains("Hata") || logLine.contains("KOPTU") || logLine.contains("closed")) StatusRed
                                    else if (logLine.contains("RFCOMM") || logLine.contains("KMRC")) KtmOrange
                                    else Color(0xFFCCCCCC),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 15.sp
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Dil Seçimi Dialog'u
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = KtmOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = strings.languageSelectorTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLanguage.entries.forEach { lang ->
                        val isSelected = lang == currentLang
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) KtmOrange.copy(alpha = 0.2f) else DarkCard)
                                .border(1.dp, if (isSelected) KtmOrange else DarkCardBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    AppLanguageManager.setLanguage(lang)
                                    showLanguageDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = lang.flagEmoji, fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(text = lang.displayName, color = if (isSelected) KtmOrange else Color.White, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                            if (isSelected) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = KtmOrange, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showLanguageDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = KtmOrange)
                ) {
                    Text(text = "Tamam", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkCard
        )
    }
}
