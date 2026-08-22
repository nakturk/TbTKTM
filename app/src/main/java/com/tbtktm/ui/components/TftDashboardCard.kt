package com.tbtktm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.TurnLeft
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material.icons.filled.TurnSharpLeft
import androidx.compose.material.icons.filled.TurnSharpRight
import androidx.compose.material.icons.filled.TurnSlightLeft
import androidx.compose.material.icons.filled.TurnSlightRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbtktm.i18n.AppLanguageManager
import com.tbtktm.model.KtmTurnIcon
import com.tbtktm.model.NavigationData
import com.tbtktm.ui.theme.KtmOrange
import com.tbtktm.ui.theme.TftDisplayBg
import com.tbtktm.ui.theme.TftTextCyan
import com.tbtktm.ui.theme.TftTextDim
import com.tbtktm.ui.theme.TftTextWhite
import com.tbtktm.ui.theme.TftTextYellow

@Composable
fun TftDashboardCard(
    navData: NavigationData,
    modifier: Modifier = Modifier
) {
    val strings by AppLanguageManager.strings.collectAsState()
    val currentLang by AppLanguageManager.currentLanguage.collectAsState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TftDisplayBg)
            .border(2.dp, KtmOrange, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Üst Başlık Şeridi (TFT Header)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.tftDisplayHeader,
                    color = KtmOrange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = if (navData.isActive) strings.tftGpsActive else strings.tftStandby,
                    color = if (navData.isActive) TftTextCyan else TftTextDim,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (navData.isActive) {
                // 2. Canlı Manevra Alanı (İkon + Kalan Mesafe)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = getManeuverIcon(navData.turnIcon),
                        contentDescription = navData.turnIcon.getLocalizedDescription(currentLang),
                        tint = KtmOrange,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = navData.distanceToTurn.ifBlank { "---" },
                            color = TftTextWhite,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (navData.turnInfo.isNotBlank()) navData.turnInfo else navData.turnIcon.getLocalizedDescription(currentLang),
                            color = TftTextYellow,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Cadde / Sokak Adı (TFT Ana Metin)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TftDisplayBg.copy(alpha = 0.6f))
                        .border(1.dp, KtmOrange.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = navData.roadName.ifBlank { "KTM 1290 Super Adventure" },
                        color = TftTextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4. Alt Bilgi Şeridi: Kalan Toplam Mesafe & Varış Süresi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = strings.tftTargetDistance,
                            color = TftTextDim,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = navData.distanceToDestination.ifBlank { "-- km" },
                            color = TftTextCyan,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = strings.tftEta,
                            color = TftTextDim,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = navData.eta.ifBlank { "--:--" },
                            color = TftTextCyan,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            } else {
                // Bekleme Modu Görünümü
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.NearMe,
                        contentDescription = null,
                        tint = TftTextDim,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = strings.tftNavStandbyMessage,
                        color = TftTextDim,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

private fun getManeuverIcon(turnIcon: KtmTurnIcon): ImageVector {
    return when (turnIcon) {
        KtmTurnIcon.GO_STRAIGHT -> Icons.Default.Navigation
        KtmTurnIcon.QUITE_RIGHT, KtmTurnIcon.HEAVY_RIGHT, KtmTurnIcon.ENTER_HIGHWAY_RIGHT_LANE, KtmTurnIcon.LEAVE_HIGHWAY_RIGHT_LANE -> Icons.Default.TurnRight
        KtmTurnIcon.QUITE_LEFT, KtmTurnIcon.HEAVY_LEFT, KtmTurnIcon.ENTER_HIGHWAY_LEFT_LANE, KtmTurnIcon.LEAVE_HIGHWAY_LEFT_LANE -> Icons.Default.TurnLeft
        KtmTurnIcon.LIGHT_RIGHT, KtmTurnIcon.KEEP_RIGHT, KtmTurnIcon.HIGHWAY_KEEP_RIGHT -> Icons.Default.TurnSlightRight
        KtmTurnIcon.LIGHT_LEFT, KtmTurnIcon.KEEP_LEFT, KtmTurnIcon.HIGHWAY_KEEP_LEFT -> Icons.Default.TurnSlightLeft
        KtmTurnIcon.UTURN_RIGHT -> Icons.Default.TurnSharpRight
        KtmTurnIcon.UTURN_LEFT -> Icons.Default.TurnSharpLeft
        else -> Icons.Default.Navigation
    }
}
