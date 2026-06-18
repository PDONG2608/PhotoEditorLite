package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CropControls(
    onIntent: (EditorIntent) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            "CẮT XÉN ẢNH (CROP)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6B7280),
            modifier = Modifier.align(Alignment.Start)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { onIntent(EditorIntent.OnPresetSquare) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF232533)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.CropFree, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Tỉ lệ 1:1", fontSize = 13.sp, color = Color.White)
            }

            Button(
                onClick = { onIntent(EditorIntent.OnResetBoundary) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF232533)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Fullscreen, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Đặt lại viền", fontSize = 13.sp, color = Color.White)
            }
        }

        Button(
            onClick = { onIntent(EditorIntent.ApplyCrop) },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("apply_crop_button")
        ) {
            Icon(Icons.Default.Crop, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("ÁP DỤNG CẮT ẢNH", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}
