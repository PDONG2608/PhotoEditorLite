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
    brightness: Float,
    exposure: Float,
    contrast: Float,
    saturation: Float,
    sharpness: Float,
    onBrightnessChange: (Float) -> Unit,
    onExposureChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onSharpnessChange: (Float) -> Unit,
    onReset: () -> Unit
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
                onClick = onReset,
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
            value = brightness,
            valueRange = -100f..100f,
            displayValue = "${brightness.toInt()}",
            onValueChange = onBrightnessChange,
            icon = Icons.Default.LightMode
        )

        // Exposure
        AdjustmentSlider(
            name = "Phơi Sáng (Exposure)",
            value = exposure,
            valueRange = -2.0f..2.0f,
            displayValue = String.format("%.2f", exposure),
            onValueChange = onExposureChange,
            icon = Icons.Default.Exposure
        )

        // Contrast
        AdjustmentSlider(
            name = "Tương Phản (Contrast)",
            value = contrast,
            valueRange = 0.2f..2.0f,
            displayValue = String.format("%.2f", contrast),
            onValueChange = onContrastChange,
            icon = Icons.Default.Contrast
        )

        // Saturation
        AdjustmentSlider(
            name = "Độ Bão Hòa (Saturation)",
            value = saturation,
            valueRange = 0.0f..2.0f,
            displayValue = String.format("%.2f", saturation),
            onValueChange = onSaturationChange,
            icon = Icons.Default.Palette
        )

        // Sharpness
        AdjustmentSlider(
            name = "Độ Nét (Sharpness)",
            value = sharpness,
            valueRange = 0.0f..3.0f,
            displayValue = String.format("%.2f", sharpness),
            onValueChange = onSharpnessChange,
            icon = Icons.Default.FilterCenterFocus
        )
    }
}
