package com.tbtktm.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbtktm.ui.theme.DarkCard
import com.tbtktm.ui.theme.KtmOrange
import com.tbtktm.ui.theme.TftGreen
import com.tbtktm.ui.theme.TftRed
import com.tbtktm.ui.theme.TftTextDim
import kotlin.math.abs

/**
 * MotoGP-Style Dynamic Lean Angle Arc Gauge with Peak Left/Right memory
 */
@Composable
fun LeanAngleGauge(
    currentLeanAngle: Float,
    maxLeftLean: Float,
    maxRightLean: Float,
    onResetPeaks: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedLean by animateFloatAsState(
        targetValue = currentLeanAngle,
        animationSpec = tween(durationMillis = 60),
        label = "LeanAnimation"
    )

    val leanColor = when {
        abs(currentLeanAngle) > 45f -> TftRed
        abs(currentLeanAngle) > 35f -> KtmOrange
        else -> TftGreen
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkCard, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title & Reset
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "6-AXIS IMU LEAN ANGLE",
                color = TftTextDim,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )

            Button(
                onClick = onResetPeaks,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text("RESET PEAKS", fontSize = 10.sp, color = Color.LightGray, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Center Gauge Graphic
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(240.dp)
                .padding(8.dp)
        ) {
            Canvas(modifier = Modifier.size(220.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width / 2f - 16.dp.toPx()

                // Background Semicircular Track (from 140° to 400° -> 260° sweep)
                drawArc(
                    color = Color(0xFF222222),
                    startAngle = 140f,
                    sweepAngle = 260f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                )

                // Scale Ticks (-50°, -40°, -30°, -20°, 0°, +20°, +30°, +40°, +50°)
                val angles = listOf(-50f, -40f, -30f, -20f, -10f, 0f, 10f, 20f, 30f, 40f, 50f)
                for (a in angles) {
                    // Map angle to arc degree: 0° is at top (270°)
                    val mappedAngle = 270f + (a * 2.2f)
                    rotate(degrees = mappedAngle, pivot = center) {
                        val tickColor = if (a == 0f) Color.White else Color(0xFF555555)
                        val tickLen = if (a % 20f == 0f || a == 0f) 12.dp.toPx() else 6.dp.toPx()
                        drawLine(
                            color = tickColor,
                            start = Offset(center.x, center.y - radius - 8.dp.toPx()),
                            end = Offset(center.x, center.y - radius - 8.dp.toPx() + tickLen),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }

                // Active Dynamic Lean Arc (from center top 270° towards left or right)
                val sweep = (animatedLean * 2.2f).coerceIn(-115f, 115f)
                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to TftGreen,
                        0.5f to KtmOrange,
                        1.0f to TftRed
                    ),
                    startAngle = 270f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                )

                // Motorcycle Silhouette / Inclinometer Pointer tilting
                rotate(degrees = animatedLean, pivot = center) {
                    // Center Bike Silhouette Line
                    drawLine(
                        color = leanColor,
                        start = Offset(center.x, center.y - 40.dp.toPx()),
                        end = Offset(center.x, center.y + 40.dp.toPx()),
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    // Handlebar Crossbar
                    drawLine(
                        color = leanColor,
                        start = Offset(center.x - 22.dp.toPx(), center.y - 12.dp.toPx()),
                        end = Offset(center.x + 22.dp.toPx(), center.y - 12.dp.toPx()),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // Center Text Reading
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val sideText = when {
                    animatedLean < -1f -> "LEFT"
                    animatedLean > 1f -> "RIGHT"
                    else -> "UPRIGHT"
                }

                Text(
                    text = "${abs(animatedLean).toInt()}°",
                    color = leanColor,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = sideText,
                    color = TftTextDim,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Peak Lean Angle Stats (Left vs Right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161616), RoundedCornerShape(12.dp))
                .padding(vertical = 10.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("PEAK LEFT", color = TftTextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "${maxLeftLean.toInt()}°",
                    color = if (maxLeftLean > 45f) TftRed else Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Box(
                modifier = Modifier
                    .size(width = 1.dp, height = 30.dp)
                    .background(Color(0xFF333333))
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("PEAK RIGHT", color = TftTextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "${maxRightLean.toInt()}°",
                    color = if (maxRightLean > 45f) TftRed else Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
