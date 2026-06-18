package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Exposure
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AdjustControls(
    uiState: EditorUiState,
    onIntent: (EditorIntent) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "BỘ HIỆU CHỈNH MÀU SẮC",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6B7280)
            )

            TextButton(
                onClick = { onIntent(EditorIntent.ResetAdjustments) },
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(2.dp))
                Text("Thiết lập lại", fontSize = 12.sp)
            }
        }

        // Brightness
        AdjustmentSlider(
            name = "Độ Sáng (Brightness)",
            value = uiState.brightness,
            valueRange = -100f..100f,
            displayValue = "${uiState.brightness.toInt()}",
            onValueChange = { onIntent(EditorIntent.UpdateBrightness(it)) },
            icon = Icons.Default.LightMode
        )

        // Exposure
        AdjustmentSlider(
            name = "Phơi Sáng (Exposure)",
            value = uiState.exposure,
            valueRange = -2.0f..2.0f,
            displayValue = String.format("%.2f", uiState.exposure),
            onValueChange = { onIntent(EditorIntent.UpdateExposure(it)) },
            icon = Icons.Default.Exposure
        )

        // Contrast
        AdjustmentSlider(
            name = "Tương Phản (Contrast)",
            value = uiState.contrast,
            valueRange = 0.2f..2.0f,
            displayValue = String.format("%.2f", uiState.contrast),
            onValueChange = { onIntent(EditorIntent.UpdateContrast(it)) },
            icon = Icons.Default.Contrast
        )

        // Saturation
        AdjustmentSlider(
            name = "Độ Bão Hòa (Saturation)",
            value = uiState.saturation,
            valueRange = 0.0f..2.0f,
            displayValue = String.format("%.2f", uiState.saturation),
            onValueChange = { onIntent(EditorIntent.UpdateSaturation(it)) },
            icon = Icons.Default.Palette
        )

        // Sharpness
        AdjustmentSlider(
            name = "Độ Nét (Sharpness)",
            value = uiState.sharpness,
            valueRange = 0.0f..3.0f,
            displayValue = String.format("%.2f", uiState.sharpness),
            onValueChange = { onIntent(EditorIntent.UpdateSharpness(it)) },
            icon = Icons.Default.FilterCenterFocus
        )
    }
}
