package com.example

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EditorWorkspace(
    uiState: EditorUiState,
    onIntent: (EditorIntent) -> Unit,
    onImportPhotoClick: () -> Unit
) {
    val currentBase = uiState.workingBitmap
    val currentAdjusted = uiState.adjustedWorkingBitmap
    val currentMode = uiState.currentMode
    val isCheckingOriginal = uiState.isCheckingOriginal
    val drawnPaths = uiState.drawnPaths
    val currentPathPoints = uiState.currentPathPoints
    val brushSize = uiState.brushSize
    val brushOpacity = uiState.brushOpacity
    val activeColor = uiState.activeColor
    val cropLeft = uiState.cropLeft
    val cropTop = uiState.cropTop
    val cropRight = uiState.cropRight
    val cropBottom = uiState.cropBottom
    val activeCropHandle = uiState.activeCropHandle

    if (currentBase == null) {
        // 1. EMPTY STATE / IMPORT COMPONENT
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_editor_empty),
                contentDescription = "Mô tả ảnh Studio",
                modifier = Modifier
                    .size(240.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .border(1.dp, Color(0xFF232533), RoundedCornerShape(32.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Chỉnh Sửa Ảnh Đỉnh Cao",
                fontSize = 20.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Thêm ngay một bức ảnh để cắt tỉa, kéo màu nghệ thuật và vẽ sketch trực tiếp lên tác phẩm.",
                fontSize = 14.sp,
                color = Color(0xFF8E92B3),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onImportPhotoClick,
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                modifier = Modifier.testTag("import_select_button")
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Chọn ảnh từ gallery", fontSize = 15.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
        }
    } else {
        // 2. ACTIVE EDITOR WORKSPACE
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            val containerWidth = constraints.maxWidth.toFloat()
            val containerHeight = constraints.maxHeight.toFloat()

            // Pick target display image depending on "hold comparing" option
            val activeRenderBmp = if (isCheckingOriginal) currentBase else (currentAdjusted ?: currentBase)

            val imageWidth = activeRenderBmp.width.toFloat()
            val imageHeight = activeRenderBmp.height.toFloat()

            if (imageWidth > 0f && imageHeight > 0f) {
                val scale = minOf(containerWidth / imageWidth, containerHeight / imageHeight)
                val actualImageWidth = imageWidth * scale
                val actualImageHeight = imageHeight * scale

                // Define exact coordinate constraints block
                Box(
                    modifier = Modifier
                        .size(
                            width = (actualImageWidth / LocalDensity.current.density).dp,
                            height = (actualImageHeight / LocalDensity.current.density).dp
                        )
                        .pointerInput(currentMode, activeRenderBmp) {
                            if (currentMode == EditMode.DRAW) {
                                detectDragGestures(
                                    onDragStart = { startOffset ->
                                        val u = startOffset.x / actualImageWidth
                                        val v = startOffset.y / actualImageHeight
                                        val xBmp = u * activeRenderBmp.width
                                        val yBmp = v * activeRenderBmp.height
                                        onIntent(EditorIntent.UpdateCurrentPathPoints(listOf(Offset(xBmp, yBmp))))
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val screenPos = change.position
                                        val u = (screenPos.x / actualImageWidth).coerceIn(0f, 1f)
                                        val v = (screenPos.y / actualImageHeight).coerceIn(0f, 1f)
                                        val xBmp = u * activeRenderBmp.width
                                        val yBmp = v * activeRenderBmp.height
                                        onIntent(EditorIntent.UpdateCurrentPathPoints(currentPathPoints + Offset(xBmp, yBmp)))
                                    },
                                    onDragEnd = {
                                        if (currentPathPoints.isNotEmpty()) {
                                            onIntent(
                                                EditorIntent.UpdateDrawnPaths(
                                                    drawnPaths + DrawnPath(
                                                        points = currentPathPoints,
                                                        color = activeColor,
                                                        strokeWidth = brushSize,
                                                        opacity = brushOpacity
                                                    )
                                                )
                                            )
                                            onIntent(EditorIntent.UpdateCurrentPathPoints(emptyList()))
                                        }
                                    }
                                )
                            } else if (currentMode == EditMode.CROP) {
                                detectDragGestures(
                                    onDragStart = { startOffset ->
                                        val tl = Offset(cropLeft * actualImageWidth, cropTop * actualImageHeight)
                                        val tr = Offset(cropRight * actualImageWidth, cropTop * actualImageHeight)
                                        val bl = Offset(cropLeft * actualImageWidth, cropBottom * actualImageHeight)
                                        val br = Offset(cropRight * actualImageWidth, cropBottom * actualImageHeight)

                                        val radiusThresh = 48.dp.toPx()

                                        val handle = when {
                                            (startOffset - tl).getDistance() < radiusThresh -> 0
                                            (startOffset - tr).getDistance() < radiusThresh -> 1
                                            (startOffset - bl).getDistance() < radiusThresh -> 2
                                            (startOffset - br).getDistance() < radiusThresh -> 3
                                            else -> null
                                        }
                                        onIntent(EditorIntent.SetActiveCropHandle(handle))
                                    },
                                    onDrag = { change, dragAmount ->
                                        val handle = activeCropHandle ?: return@detectDragGestures
                                        change.consume()

                                        val dx = dragAmount.x / actualImageWidth
                                        val dy = dragAmount.y / actualImageHeight

                                        when (handle) {
                                            0 -> { // Top-Left
                                                onIntent(EditorIntent.UpdateCropLeft((cropLeft + dx).coerceIn(0f, cropRight - 0.15f)))
                                                onIntent(EditorIntent.UpdateCropTop((cropTop + dy).coerceIn(0f, cropBottom - 0.15f)))
                                            }
                                            1 -> { // Top-Right
                                                onIntent(EditorIntent.UpdateCropRight((cropRight + dx).coerceIn(cropLeft + 0.15f, 1f)))
                                                onIntent(EditorIntent.UpdateCropTop((cropTop + dy).coerceIn(0f, cropBottom - 0.15f)))
                                            }
                                            2 -> { // Bottom-Left
                                                onIntent(EditorIntent.UpdateCropLeft((cropLeft + dx).coerceIn(0f, cropRight - 0.15f)))
                                                onIntent(EditorIntent.UpdateCropBottom((cropBottom + dy).coerceIn(cropTop + 0.15f, 1f)))
                                            }
                                            3 -> { // Bottom-Right
                                                onIntent(EditorIntent.UpdateCropRight((cropRight + dx).coerceIn(cropLeft + 0.15f, 1f)))
                                                onIntent(EditorIntent.UpdateCropBottom((cropBottom + dy).coerceIn(cropTop + 0.15f, 1f)))
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        onIntent(EditorIntent.SetActiveCropHandle(null))
                                    }
                                )
                            }
                        }
                ) {
                    // Draw Bitmap
                    Image(
                        bitmap = activeRenderBmp.asImageBitmap(),
                        contentDescription = "Workstation target photo file",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )

                    // DRAW LAYER
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawnPaths.forEach { path ->
                            val strokePx = path.strokeWidth * density
                            val pathObj = Path().apply {
                                if (path.points.isNotEmpty()) {
                                    val p0 = path.points[0]
                                    val sX = (p0.x / activeRenderBmp.width) * size.width
                                    val sY = (p0.y / activeRenderBmp.height) * size.height
                                    moveTo(sX, sY)
                                    for (i in 1 until path.points.size) {
                                        val p = path.points[i]
                                        val ptX = (p.x / activeRenderBmp.width) * size.width
                                        val ptY = (p.y / activeRenderBmp.height) * size.height
                                        lineTo(ptX, ptY)
                                    }
                                }
                            }
                            drawPath(
                                path = pathObj,
                                color = path.color.copy(alpha = path.opacity),
                                style = Stroke(
                                    width = strokePx,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }

                        if (currentPathPoints.isNotEmpty()) {
                            val strokePx = brushSize * density
                            val pathObj = Path().apply {
                                val p0 = currentPathPoints[0]
                                val sX = (p0.x / activeRenderBmp.width) * size.width
                                val sY = (p0.y / activeRenderBmp.height) * size.height
                                moveTo(sX, sY)
                                for (i in 1 until currentPathPoints.size) {
                                    val p = currentPathPoints[i]
                                    val ptX = (p.x / activeRenderBmp.width) * size.width
                                    val ptY = (p.y / activeRenderBmp.height) * size.height
                                    lineTo(ptX, ptY)
                                }
                            }
                            drawPath(
                                path = pathObj,
                                color = activeColor.copy(alpha = brushOpacity),
                                style = Stroke(
                                    width = strokePx,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }

                    // CROP MASK & REGIONS DISPLAY
                    if (currentMode == EditMode.CROP) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val l = cropLeft * size.width
                            val t = cropTop * size.height
                            val r = cropRight * size.width
                            val b = cropBottom * size.height

                            drawRect(Color.Black.copy(alpha = 0.7f), topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(size.width, t))
                            drawRect(Color.Black.copy(alpha = 0.7f), topLeft = Offset(0f, b), size = androidx.compose.ui.geometry.Size(size.width, size.height - b))
                            drawRect(Color.Black.copy(alpha = 0.7f), topLeft = Offset(0f, t), size = androidx.compose.ui.geometry.Size(l, b - t))
                            drawRect(Color.Black.copy(alpha = 0.7f), topLeft = Offset(r, t), size = androidx.compose.ui.geometry.Size(size.width - r, b - t))

                            drawRect(
                                color = Color.White,
                                topLeft = Offset(l, t),
                                size = androidx.compose.ui.geometry.Size(r - l, b - t),
                                style = Stroke(width = 2.dp.toPx())
                            )

                            val dx = (r - l) / 3f
                            val dy = (b - t) / 3f

                            drawLine(Color.White.copy(0.3f), Offset(l + dx, t), Offset(l + dx, b), 1.dp.toPx())
                            drawLine(Color.White.copy(0.3f), Offset(l + 2 * dx, t), Offset(l + 2 * dx, b), 1.dp.toPx())

                            drawLine(Color.White.copy(0.3f), Offset(l, t + dy), Offset(r, t + dy), 1.dp.toPx())
                            drawLine(Color.White.copy(0.3f), Offset(l, t + 2 * dy), Offset(r, t + 2 * dy), 1.dp.toPx())

                            val pad = 4.dp.toPx()
                            val wB = 4.dp.toPx()
                            val flagL = 16.dp.toPx()

                            drawLine(Color.White, Offset(l - pad, t), Offset(l + flagL, t), wB)
                            drawLine(Color.White, Offset(l, t - pad), Offset(l, t + flagL), wB)

                            drawLine(Color.White, Offset(r + pad, t), Offset(r - flagL, t), wB)
                            drawLine(Color.White, Offset(r, t - pad), Offset(r, t + flagL), wB)

                            drawLine(Color.White, Offset(l - pad, b), Offset(l + flagL, b), wB)
                            drawLine(Color.White, Offset(l, b + pad), Offset(l, b - flagL), wB)

                            drawLine(Color.White, Offset(r + pad, b), Offset(r - flagL, b), wB)
                            drawLine(Color.White, Offset(r, b + pad), Offset(r, b - flagL), wB)
                        }
                    }

                    // Interactive View Origin hold button
                    if (currentMode == EditMode.ADJUST && !isCheckingOriginal) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(0.6f))
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { onIntent(EditorIntent.SetCheckingOriginal(true)) },
                                        onDrag = { change, _ -> change.consume() },
                                        onDragEnd = { onIntent(EditorIntent.SetCheckingOriginal(false)) },
                                        onDragCancel = { onIntent(EditorIntent.SetCheckingOriginal(false)) }
                                    )
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Compare,
                                    contentDescription = "So sánh",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Đè xem ảnh gốc",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
