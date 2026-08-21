package com.tbtktm.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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

    var isSimulating by remember { mutableStateOf(false) }

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
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = if (telemetryData.isConnected) "● 6-AXIS IMU & CAN CONNECTED" else "○ WAITING FOR TELEMETRY (PORT 52070)",
                    color = if (telemetryData.isConnected) TftGreen else TftTextDim,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = {
                    isSimulating = !isSimulating
                    if (isSimulating) {
                        telemetryManager.startSimulation()
                    } else {
                        telemetryManager.stopSimulation()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSimulating) TftRed else KtmOrange
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = if (isSimulating) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isSimulating) "STOP SIM" else "TEST SIM",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Central Lean Angle Gauge (MotoGP Style)
        LeanAngleGauge(
            currentLeanAngle = telemetryData.currentLeanAngle,
            maxLeftLean = telemetryData.maxLeftLeanAngle,
            maxRightLean = telemetryData.maxRightLeanAngle,
            onResetPeaks = { telemetryManager.resetPeakLeanAngles() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Throttle vs Brake Hydraulic Pressure Dual Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "BRAKE & THROTTLE TELEMETRY",
                    color = TftTextDim,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

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
                            Text("FRONT BRAKE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "${String.format("%.1f", telemetryData.frontBrakePressureBar)} Bar",
                                color = TftRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        val brakeProgress = (telemetryData.frontBrakePressureBar / 20.0f).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = brakeProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp),
                            color = TftRed,
                            trackColor = Color(0xFF222222)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Throttle TPS %
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("THROTTLE TPS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "${telemetryData.throttlePositionPercent.toInt()}%",
                                color = TftGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        val tpsProgress = (telemetryData.throttlePositionPercent / 100.0f).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = tpsProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp),
                            color = TftGreen,
                            trackColor = Color(0xFF222222)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Motion & Dynamics Stats (Pitch, G-Force, Gear, Speed)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Speed & Gear Box
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("GEAR", color = TftTextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (telemetryData.gear == 0) "N" else telemetryData.gear.toString(),
                        color = if (telemetryData.gear == 0) TftGreen else KtmOrange,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${telemetryData.speedKmh.toInt()} km/h",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${telemetryData.engineRpm} RPM",
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
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("IMU PITCH", color = TftTextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${String.format("%+.1f", telemetryData.pitchAngle)}°",
                        color = if (telemetryData.pitchAngle > 3f) KtmOrange else Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("G-FORCE", color = TftTextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${String.format("%+.2f", telemetryData.trajectoryAccelG)} G",
                        color = if (telemetryData.trajectoryAccelG < -0.5f) TftRed else TftGreen,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. TPMS & Engine Temp Strip
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("FRONT TIRE", color = TftTextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (telemetryData.frontTirePressureBar > 0) "${String.format("%.2f", telemetryData.frontTirePressureBar)} Bar" else "--",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("COOLANT", color = TftTextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (telemetryData.coolantTempC > 0) "${telemetryData.coolantTempC.toInt()} °C" else "--",
                        color = if (telemetryData.coolantTempC > 105) TftRed else Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("REAR TIRE", color = TftTextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (telemetryData.rearTirePressureBar > 0) "${String.format("%.2f", telemetryData.rearTirePressureBar)} Bar" else "--",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
