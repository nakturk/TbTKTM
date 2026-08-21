package com.tbtktm.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbtktm.TbTApplication
import com.tbtktm.i18n.AppLanguageManager
import com.tbtktm.i18n.AppStrings
import com.tbtktm.model.ButtonAction
import com.tbtktm.model.HandlebarButton
import com.tbtktm.ui.theme.DarkCard
import com.tbtktm.ui.theme.DarkCardBorder
import com.tbtktm.ui.theme.KtmOrange
import com.tbtktm.ui.theme.TftTextDim

@Composable
fun KeyMappingScreen() {
    val keyManager = TbTApplication.instance.handlebarKeyManager
    val keyMappings by keyManager.keyMappings.collectAsState()
    val strings by AppLanguageManager.strings.collectAsState()

    var selectedButtonForEdit by remember { mutableStateOf<HandlebarButton?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = strings.handlebarSettingsTitle,
            color = KtmOrange,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            text = strings.handlebarSettingsDesc,
            color = TftTextDim,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(HandlebarButton.entries.toList()) { button ->
                val currentAction = keyMappings[button] ?: ButtonAction.NONE

                KeyItemRow(
                    button = button,
                    action = currentAction,
                    strings = strings,
                    onClick = { selectedButtonForEdit = button }
                )
            }
        }
    }

    // Eylem Seçim Dialog'u
    selectedButtonForEdit?.let { button ->
        ActionSelectionDialog(
            button = button,
            currentAction = keyMappings[button] ?: ButtonAction.NONE,
            strings = strings,
            onActionSelected = { action ->
                keyManager.updateMapping(button, action)
                selectedButtonForEdit = null
            },
            onDismiss = { selectedButtonForEdit = null }
        )
    }
}

@Composable
private fun KeyItemRow(
    button: HandlebarButton,
    action: ButtonAction,
    strings: AppStrings,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkCard)
            .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = button.displayName,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = action.getLocalizedLabel(strings),
                    color = KtmOrange,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = "➔",
                color = TftTextDim,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun ActionSelectionDialog(
    button: HandlebarButton,
    currentAction: ButtonAction,
    strings: AppStrings,
    onActionSelected: (ButtonAction) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        title = {
            Text(
                text = "${button.displayName} - ${strings.handlebarSettingsTitle}",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                ButtonAction.entries.forEach { action ->
                    val actionLabel = action.getLocalizedLabel(strings)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onActionSelected(action) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = action == currentAction,
                            onClick = { onActionSelected(action) },
                            colors = RadioButtonDefaults.colors(selectedColor = KtmOrange)
                        )
                        Text(
                            text = actionLabel,
                            color = if (action == currentAction) KtmOrange else Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "✕", color = TftTextDim, fontSize = 16.sp)
            }
        }
    )
}
