package com.example

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
