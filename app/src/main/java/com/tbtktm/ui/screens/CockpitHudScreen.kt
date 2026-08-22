package com.tbtktm.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbtktm.telemetry.KtmTelemetryManager
import com.tbtktm.ui.components.LeanAngleGauge
import com.tbtktm.ui.theme.DarkBackground
import com.tbtktm.ui.theme.DarkCard
import com.tbtktm.ui.theme.KtmOrange
import com.tbtktm.ui.theme.TftGreen
import com.tbtktm.ui.theme.TftRed
import com.tbtktm.ui.theme.TftTextDim

@Composable
fun CockpitHudScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val telemetryManager = remember { KtmTelemetryManager.getInstance(context) }
    val telemetryData by telemetryManager.telemetryState.collectAsState()
    val telemetrySource by telemetryManager.telemetrySource.collectAsState()

    var isSimulating by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Canlı telefon sensörlerini hemen başlat
        telemetryManager.startPhoneSensors()

        // Kayıtlı motosiklet varsa arka planda KTM CAN bağlantısını da dene
        val lastAddress = com.tbtktm.ble.KtmBleManager.getInstance(context).getLastConnectedAddress()
        if (!lastAddress.isNullOrBlank() && !isSimulating) {
            telemetryManager.connect(lastAddress)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            telemetryManager.stopSimulation()
            telemetryManager.stopPhoneSensors()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "KTM 1290 COCKPIT HUD",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "● $telemetrySource",
                    color = if (telemetryData.isConnected) TftGreen else TftTextDim,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Kalibrasyon / Sıfırlama Butonu
                OutlinedButton(
                    onClick = {
                        telemetryManager.calibrateZero()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, KtmOrange.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = KtmOrange, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "SIFIRLA", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KtmOrange)
                }

                Button(
                    onClick = {
                        isSimulating = !isSimulating
                        if (isSimulating) {
                            telemetryManager.startSimulation()
                        } else {
                            telemetryManager.stopSimulation()
                            telemetryManager.startPhoneSensors()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSimulating) TftRed else KtmOrange
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isSimulating) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (isSimulating) Color.White else Color.Black
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isSimulating) "DURDUR" else "SİMÜLE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSimulating) Color.White else Color.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 1. Central Lean Angle Gauge (MotoGP Style)
        LeanAngleGauge(
            currentLeanAngle = telemetryData.currentLeanAngle,
            maxLeftLean = telemetryData.maxLeftLeanAngle,
            maxRightLean = telemetryData.maxRightLeanAngle,
            onResetPeaks = { telemetryManager.resetPeakLeanAngles() }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Throttle vs Brake Hydraulic Pressure Dual Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "BRAKE & THROTTLE TELEMETRY",
                    color = TftTextDim,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Front Brake Bar Pressure
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("FRONT BRAKE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${String.format(java.util.Locale.US, "%.1f", telemetryData.frontBrakePressureBar)} Bar",
                                color = TftRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        val brakeProgress = (telemetryData.frontBrakePressureBar / 20.0f).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { brakeProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp),
                            color = TftRed,
                            trackColor = Color(0xFF222222)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // Throttle TPS %
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("THROTTLE TPS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${telemetryData.throttlePositionPercent.toInt()}%",
                                color = TftGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        val tpsProgress = (telemetryData.throttlePositionPercent / 100.0f).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { tpsProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp),
                            color = TftGreen,
                            trackColor = Color(0xFF222222)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Motion & Dynamics Stats (Pitch, G-Force, Gear, Speed)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Speed & Gear Box
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("GEAR", color = TftTextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (telemetryData.gear == 0) "N" else telemetryData.gear.toString(),
                        color = if (telemetryData.gear == 0) TftGreen else KtmOrange,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${telemetryData.speedKmh.toInt()} km/h",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (telemetryData.engineRpm > 0) "${telemetryData.engineRpm} RPM" else "GPS HIZ",
                        color = TftTextDim,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Pitch & G-Force Box
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("IMU PITCH", color = TftTextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${String.format(java.util.Locale.US, "%+.1f", telemetryData.pitchAngle)}°",
                        color = if (telemetryData.pitchAngle > 3f) KtmOrange else Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("G-FORCE", color = TftTextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${String.format(java.util.Locale.US, "%+.2f", telemetryData.trajectoryAccelG)} G",
                        color = if (telemetryData.trajectoryAccelG < -0.3f) TftRed else TftGreen,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. TPMS & Engine Temp Strip
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ÖN LASTİK", color = TftTextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (telemetryData.frontTirePressureBar > 0) "${String.format(java.util.Locale.US, "%.2f", telemetryData.frontTirePressureBar)} Bar" else "--",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("HARARET", color = TftTextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (telemetryData.coolantTempC > 0) "${telemetryData.coolantTempC.toInt()} °C" else "--",
                        color = if (telemetryData.coolantTempC > 105) TftRed else Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ARKA LASTİK", color = TftTextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (telemetryData.rearTirePressureBar > 0) "${String.format(java.util.Locale.US, "%.2f", telemetryData.rearTirePressureBar)} Bar" else "--",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
