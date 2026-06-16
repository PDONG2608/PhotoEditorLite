package com.example

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val spectrumColors = listOf(
    Color(0xFFFF0000), // Red
    Color(0xFFFFFF00), // Yellow
    Color(0xFF00FF00), // Green
    Color(0xFF00FFFF), // Cyan
    Color(0xFF0000FF), // Blue
    Color(0xFFFF00FF), // Magenta
    Color(0xFFFF0000)  // Red loops back
)

fun getSpectrumColor(fraction: Float): Color {
    val size = spectrumColors.size
    val maxIndex = size - 1
    val scaledFraction = fraction.coerceIn(0f, 1f) * maxIndex
    val index1 = scaledFraction.toInt().coerceIn(0, maxIndex)
    val index2 = (index1 + 1).coerceIn(0, maxIndex)
    val localFraction = scaledFraction - index1

    val c1 = spectrumColors[index1]
    val c2 = spectrumColors[index2]

    return Color(
        red = c1.red + (c2.red - c1.red) * localFraction,
        green = c1.green + (c2.green - c1.green) * localFraction,
        blue = c1.blue + (c2.blue - c1.blue) * localFraction,
        alpha = 1f
    )
}

@Composable
fun DrawControls(
    drawnPathsExist: Boolean,
    onUndoClick: () -> Unit,
    onDeleteSweepClick: () -> Unit,
    activeColor: Color,
    onColorChange: (Color) -> Unit,
    brushSize: Float,
    onBrushSizeChange: (Float) -> Unit,
    brushOpacity: Float,
    onBrushOpacityChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Header + Undo Rows
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "BẢNG VẼ TỰ DO (SKETCH)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6B7280)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onUndoClick,
                    enabled = drawnPathsExist,
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            if (drawnPathsExist) Color(0xFF232533) else Color(0xFF1B1C26),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Hoàn tác",
                        tint = if (drawnPathsExist) Color.White else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onDeleteSweepClick,
                    enabled = drawnPathsExist,
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            if (drawnPathsExist) Color(0xFF232533) else Color(0xFF1B1C26),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.DeleteSweep,
                        contentDescription = "Xoá nháp",
                        tint = if (drawnPathsExist) Color.White else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Brush presets
        val swatchColors = remember {
            listOf(
                Color.White, Color.Black, Color.Gray, Color.Red,
                Color(0xFFFF9800), Color(0xFFFFEB3B), Color(0xFF4CAF50),
                Color(0xFF00BCD4), Color(0xFF2196F3), Color(0xFF9C27B0), Color(0xFFE91E63)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            swatchColors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (activeColor == color) 2.dp else 1.dp,
                            color = if (activeColor == color) Color.White else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { onColorChange(color) }
                )
            }
        }

        // Spectral Color Slidehue Pick
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Tùy chỉnh sắc độ Màu",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )
                Box(
                    modifier = Modifier
                        .size(24.dp, 12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(activeColor)
                )
            }

            Spacer(Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(
                        brush = Brush.horizontalGradient(spectrumColors)
                    )
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                            onColorChange(getSpectrumColor(fraction))
                        }
                    }
            )
        }

        // Size + Opacities Sliders
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Kích thước vẽ", fontSize = 11.sp, color = Color.Gray)
                    Text("${brushSize.toInt()} px", fontSize = 11.sp, color = Color.White)
                }
                Slider(
                    value = brushSize,
                    onValueChange = onBrushSizeChange,
                    valueRange = 2f..80f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Độ đậm nhạt (Alpha)", fontSize = 11.sp, color = Color.Gray)
                    Text("${(brushOpacity * 100).toInt()}%", fontSize = 11.sp, color = Color.White)
                }
                Slider(
                    value = brushOpacity,
                    onValueChange = onBrushOpacityChange,
                    valueRange = 0.1f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}
