package com.example

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EditorUiState(
    val originalBitmap: Bitmap? = null,
    val workingBitmap: Bitmap? = null,
    val adjustedWorkingBitmap: Bitmap? = null,
    val isProcessing: Boolean = false,
    val currentMode: EditMode = EditMode.ADJUST,
    val brightness: Float = 0f,
    val exposure: Float = 0f,
    val contrast: Float = 1.0f,
    val saturation: Float = 1.0f,
    val sharpness: Float = 0f,
    val isCheckingOriginal: Boolean = false,
    val drawnPaths: List<DrawnPath> = emptyList(),
    val currentPathPoints: List<Offset> = emptyList(),
    val brushSize: Float = 12f,
    val brushOpacity: Float = 1.0f,
    val activeColor: Color = Color.Red,
    val cropLeft: Float = 0f,
    val cropTop: Float = 0f,
    val cropRight: Float = 1f,
    val cropBottom: Float = 1f,
    val activeCropHandle: Int? = null
)

class EditorViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val _effects = Channel<EditorEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var adjustmentJob: Job? = null

    fun processIntent(intent: EditorIntent) {
        when (intent) {
            is EditorIntent.LoadBitmap -> {
                _uiState.update {
                    it.copy(
                        originalBitmap = intent.bitmap,
                        workingBitmap = intent.bitmap,
                        adjustedWorkingBitmap = intent.bitmap,
                        drawnPaths = emptyList(),
                        currentPathPoints = emptyList(),
                        brightness = 0f,
                        exposure = 0f,
                        contrast = 1.0f,
                        saturation = 1.0f,
                        sharpness = 0f,
                        cropLeft = 0f,
                        cropTop = 0f,
                        cropRight = 1f,
                        cropBottom = 1f
                    )
                }
            }
            is EditorIntent.SetMode -> {
                _uiState.update { it.copy(currentMode = intent.mode) }
            }
            is EditorIntent.UpdateBrightness -> {
                _uiState.update { it.copy(brightness = intent.value) }
                triggerAdjustment()
            }
            is EditorIntent.UpdateExposure -> {
                _uiState.update { it.copy(exposure = intent.value) }
                triggerAdjustment()
            }
            is EditorIntent.UpdateContrast -> {
                _uiState.update { it.copy(contrast = intent.value) }
                triggerAdjustment()
            }
            is EditorIntent.UpdateSaturation -> {
                _uiState.update { it.copy(saturation = intent.value) }
                triggerAdjustment()
            }
            is EditorIntent.UpdateSharpness -> {
                _uiState.update { it.copy(sharpness = intent.value) }
                triggerAdjustment()
            }
            is EditorIntent.ResetAdjustments -> {
                _uiState.update {
                    it.copy(
                        brightness = 0f,
                        exposure = 0f,
                        contrast = 1.0f,
                        saturation = 1.0f,
                        sharpness = 0f
                    )
                }
                triggerAdjustment()
            }
            is EditorIntent.SetCheckingOriginal -> {
                _uiState.update { it.copy(isCheckingOriginal = intent.check) }
            }
            is EditorIntent.UndoDraw -> {
                _uiState.update {
                    if (it.drawnPaths.isNotEmpty()) {
                        it.copy(drawnPaths = it.drawnPaths.dropLast(1))
                    } else {
                        it
                    }
                }
            }
            is EditorIntent.ClearDraw -> {
                _uiState.update {
                    it.copy(
                        drawnPaths = emptyList(),
                        currentPathPoints = emptyList()
                    )
                }
            }
            is EditorIntent.UpdateActiveColor -> {
                _uiState.update { it.copy(activeColor = intent.color) }
            }
            is EditorIntent.UpdateBrushSize -> {
                _uiState.update { it.copy(brushSize = intent.size) }
            }
            is EditorIntent.UpdateBrushOpacity -> {
                _uiState.update { it.copy(brushOpacity = intent.opacity) }
            }
            is EditorIntent.UpdateDrawnPaths -> {
                _uiState.update { it.copy(drawnPaths = intent.paths) }
            }
            is EditorIntent.UpdateCurrentPathPoints -> {
                _uiState.update { it.copy(currentPathPoints = intent.points) }
            }
            is EditorIntent.StartPath -> {
                val state = _uiState.value
                val activeRenderBmp = state.adjustedWorkingBitmap ?: state.workingBitmap
                if (activeRenderBmp != null) {
                    val u = intent.point.x
                    val v = intent.point.y
                    _uiState.update {
                        it.copy(currentPathPoints = listOf(Offset(u, v)))
                    }
                }
            }
            is EditorIntent.DragPath -> {
                _uiState.update {
                    it.copy(currentPathPoints = it.currentPathPoints + intent.point)
                }
            }
            is EditorIntent.EndPath -> {
                _uiState.update {
                    if (it.currentPathPoints.isNotEmpty()) {
                        it.copy(
                            drawnPaths = it.drawnPaths + DrawnPath(
                                points = it.currentPathPoints,
                                color = it.activeColor,
                                strokeWidth = it.brushSize,
                                opacity = it.brushOpacity
                            ),
                            currentPathPoints = emptyList()
                        )
                    } else {
                        it
                    }
                }
            }
            is EditorIntent.UpdateCropLeft -> {
                _uiState.update { it.copy(cropLeft = intent.value) }
            }
            is EditorIntent.UpdateCropTop -> {
                _uiState.update { it.copy(cropTop = intent.value) }
            }
            is EditorIntent.UpdateCropRight -> {
                _uiState.update { it.copy(cropRight = intent.value) }
            }
            is EditorIntent.UpdateCropBottom -> {
                _uiState.update { it.copy(cropBottom = intent.value) }
            }
            is EditorIntent.SetActiveCropHandle -> {
                _uiState.update { it.copy(activeCropHandle = intent.handle) }
            }
            is EditorIntent.OnPresetSquare -> {
                _uiState.update {
                    it.copy(
                        cropLeft = 0.1f,
                        cropTop = 0.1f,
                        cropRight = 0.9f,
                        cropBottom = 0.9f
                    )
                }
            }
            is EditorIntent.OnResetBoundary -> {
                _uiState.update {
                    it.copy(
                        cropLeft = 0f,
                        cropTop = 0f,
                        cropRight = 1f,
                        cropBottom = 1f
                    )
                }
            }
            is EditorIntent.ApplyCrop -> {
                val state = _uiState.value
                val src = state.workingBitmap
                if (src != null) {
                    _uiState.update { it.copy(isProcessing = true) }
                    viewModelScope.launch(Dispatchers.Default) {
                        val w = src.width
                        val h = src.height
                        val x = (state.cropLeft * w).toInt().coerceIn(0, w - 1)
                        val y = (state.cropTop * h).toInt().coerceIn(0, h - 1)
                        val finalWidth = ((state.cropRight - state.cropLeft) * w).toInt().coerceIn(1, w - x)
                        val finalHeight = ((state.cropBottom - state.cropTop) * h).toInt().coerceIn(1, h - y)

                        val cropped = Bitmap.createBitmap(src, x, y, finalWidth, finalHeight)

                        // Relative drawn points translation on crop area sizing
                        val srcW = src.width.toFloat()
                        val srcH = src.height.toFloat()
                        val offsetX = (state.cropLeft * srcW)
                        val offsetY = (state.cropTop * srcH)

                        val newPaths = state.drawnPaths.map { path ->
                            val newPts = path.points.map { pt ->
                                Offset(pt.x - offsetX, pt.y - offsetY)
                            }.filter { pt ->
                                pt.x >= 0f && pt.x <= cropped.width.toFloat() &&
                                pt.y >= 0f && pt.y <= cropped.height.toFloat()
                            }
                            path.copy(points = newPts)
                        }.filter { it.points.isNotEmpty() }

                        _uiState.update {
                            it.copy(
                                workingBitmap = cropped,
                                adjustedWorkingBitmap = cropped,
                                drawnPaths = newPaths,
                                cropLeft = 0f,
                                cropTop = 0f,
                                cropRight = 1f,
                                cropBottom = 1f,
                                isProcessing = false
                            )
                        }
                        // Re-trigger color/adjust transformations relative to cropped size base bitmap
                        triggerAdjustment()
                    }
                }
            }
            is EditorIntent.GlobalReset -> {
                _uiState.update {
                    it.copy(
                        brightness = 0f,
                        exposure = 0f,
                        contrast = 1.0f,
                        saturation = 1.0f,
                        sharpness = 0f,
                        drawnPaths = emptyList(),
                        currentPathPoints = emptyList(),
                        workingBitmap = it.originalBitmap,
                        adjustedWorkingBitmap = it.originalBitmap,
                        cropLeft = 0f,
                        cropTop = 0f,
                        cropRight = 1f,
                        cropBottom = 1f
                    )
                }
            }
            is EditorIntent.SaveEditedImage -> {
                val state = _uiState.value
                val base = state.workingBitmap
                val drawnPathsToSave = state.drawnPaths
                if (base != null) {
                    val currentEditsImg = state.adjustedWorkingBitmap ?: base
                    _uiState.update { it.copy(isProcessing = true) }
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            val compositedResult = Bitmap.createBitmap(
                                base.width,
                                base.height,
                                Bitmap.Config.ARGB_8888
                            )
                            val nativeCanvas = android.graphics.Canvas(compositedResult)

                            val basePaint = android.graphics.Paint().apply { isAntiAlias = true }
                            nativeCanvas.drawBitmap(currentEditsImg, 0f, 0f, basePaint)

                            drawnPathsToSave.forEach { path ->
                                val strokePaint = android.graphics.Paint().apply {
                                    color = path.color.copy(alpha = path.opacity).toArgb()
                                    strokeWidth = path.strokeWidth
                                    style = android.graphics.Paint.Style.STROKE
                                    strokeCap = android.graphics.Paint.Cap.ROUND
                                    strokeJoin = android.graphics.Paint.Join.ROUND
                                    isAntiAlias = true
                                }

                                val androidPath = android.graphics.Path()
                                if (path.points.isNotEmpty()) {
                                    androidPath.moveTo(path.points[0].x, path.points[0].y)
                                    for (i in 1 until path.points.size) {
                                        androidPath.lineTo(path.points[i].x, path.points[i].y)
                                    }
                                }
                                nativeCanvas.drawPath(androidPath, strokePaint)
                            }

                            val contentResolver = getApplication<Application>().contentResolver
                            val uniqueFileName = "IR_EDIT_${System.currentTimeMillis()}.png"

                            val values = ContentValues().apply {
                                put(MediaStore.Images.Media.DISPLAY_NAME, uniqueFileName)
                                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/IrisPhotoEditor")
                                    put(MediaStore.Images.Media.IS_PENDING, 1)
                                }
                            }

                            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                            if (uri != null) {
                                contentResolver.openOutputStream(uri)?.use { outputStream ->
                                    compositedResult.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    values.clear()
                                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                                    contentResolver.update(uri, values, null, null)
                                }
                                _effects.send(EditorEffect.SaveSuccess(uri))
                            } else {
                                _effects.send(EditorEffect.SaveError)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            _effects.send(EditorEffect.SaveError)
                        } finally {
                            _uiState.update { it.copy(isProcessing = false) }
                        }
                    }
                }
            }
        }
    }

    private fun triggerAdjustment() {
        val state = _uiState.value
        val base = state.workingBitmap ?: return

        adjustmentJob?.cancel()
        adjustmentJob = viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(isProcessing = true) }
            val result = ImageProcessor.adjustBitmap(
                src = base,
                brightness = state.brightness,
                exposure = state.exposure,
                contrast = state.contrast,
                saturation = state.saturation,
                sharpness = state.sharpness
            )
            _uiState.update { it.copy(adjustedWorkingBitmap = result, isProcessing = false) }
        }
    }
}
