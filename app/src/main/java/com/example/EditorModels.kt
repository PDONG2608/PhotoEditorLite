package com.example

import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

/**
 * Model representing a physical freehand brush stroke drawn on the photo.
 * Coordinates are mapped to the Bitmap space so edits scale perfectly independent of screen size.
 */
data class DrawnPath(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float,
    val opacity: Float
)

enum class EditMode {
    ADJUST, DRAW, CROP
}

sealed interface EditorIntent {
    data class LoadBitmap(val bitmap: android.graphics.Bitmap) : EditorIntent
    data class SetMode(val mode: EditMode) : EditorIntent
    data class UpdateBrightness(val value: Float) : EditorIntent
    data class UpdateExposure(val value: Float) : EditorIntent
    data class UpdateContrast(val value: Float) : EditorIntent
    data class UpdateSaturation(val value: Float) : EditorIntent
    data class UpdateSharpness(val value: Float) : EditorIntent
    object ResetAdjustments : EditorIntent
    data class SetCheckingOriginal(val check: Boolean) : EditorIntent
    object UndoDraw : EditorIntent
    object ClearDraw : EditorIntent
    data class UpdateActiveColor(val color: Color) : EditorIntent
    data class UpdateBrushSize(val size: Float) : EditorIntent
    data class UpdateBrushOpacity(val opacity: Float) : EditorIntent
    data class UpdateDrawnPaths(val paths: List<DrawnPath>) : EditorIntent
    data class UpdateCurrentPathPoints(val points: List<Offset>) : EditorIntent
    data class StartPath(val point: Offset) : EditorIntent
    data class DragPath(val point: Offset) : EditorIntent
    object EndPath : EditorIntent
    data class UpdateCropLeft(val value: Float) : EditorIntent
    data class UpdateCropTop(val value: Float) : EditorIntent
    data class UpdateCropRight(val value: Float) : EditorIntent
    data class UpdateCropBottom(val value: Float) : EditorIntent
    data class SetActiveCropHandle(val handle: Int?) : EditorIntent
    object OnPresetSquare : EditorIntent
    object OnResetBoundary : EditorIntent
    object ApplyCrop : EditorIntent
    object GlobalReset : EditorIntent
    object SaveEditedImage : EditorIntent
}

sealed interface EditorEffect {
    data class ShowToast(val message: String) : EditorEffect
    data class SaveSuccess(val uri: Uri) : EditorEffect
    object SaveError : EditorEffect
}
