package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import kotlin.math.pow

object ImageProcessor {

    /**
     * Applies color and light adjustments (Brightness, Exposure, Contrast, Saturation)
     * using hardware-accelerated ColorMatrix, followed by custom-convolution Sharpness.
     */
    fun adjustBitmap(
        src: Bitmap,
        brightness: Float, // -100 to 100
        exposure: Float,   // -2.0 to 2.0 (stops)
        contrast: Float,   // 0.2 to 2.0
        saturation: Float, // 0.0 to 2.0
        sharpness: Float   // 0.0 to 3.0
    ): Bitmap {
        // 1. Create target bitmap for ColorMatrix edits
        val config = src.config ?: Bitmap.Config.ARGB_8888
        val colorAdjusted = Bitmap.createBitmap(src.width, src.height, config)
        val canvas = Canvas(colorAdjusted)

        // 2. Build and concatenate ColorMatrices
        val finalMatrix = ColorMatrix()

        // Exposure (scaling RGB multiplier)
        val expMatrix = ColorMatrix().apply {
            val scale = 2.0f.pow(exposure)
            set(floatArrayOf(
                scale, 0f, 0f, 0f, 0f,
                0f, scale, 0f, 0f, 0f,
                0f, 0f, scale, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
        }

        // Contrast (rescaling around 128 mid-tone value)
        val conMatrix = ColorMatrix().apply {
            val scale = contrast
            val translate = 128f * (1f - scale)
            set(floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
        }

        // Saturation (Material ColorMatrix helper)
        val satMatrix = ColorMatrix().apply {
            setSaturation(saturation)
        }

        // Brightness (adding linear brightness offset)
        val briMatrix = ColorMatrix().apply {
            set(floatArrayOf(
                1f, 0f, 0f, 0f, brightness,
                0f, 1f, 0f, 0f, brightness,
                0f, 0f, 1f, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            ))
        }

        // Concatenate matrices to execute in a single high-efficiency pass
        finalMatrix.postConcat(expMatrix)
        finalMatrix.postConcat(conMatrix)
        finalMatrix.postConcat(satMatrix)
        finalMatrix.postConcat(briMatrix)

        val paint = Paint().apply {
            isAntiAlias = true
            colorFilter = ColorMatrixColorFilter(finalMatrix)
        }
        canvas.drawBitmap(src, 0f, 0f, paint)

        // 3. Apply custom sharpening convolution if requested
        return if (sharpness > 0f) {
            sharpenBitmap(colorAdjusted, sharpness)
        } else {
            colorAdjusted
        }
    }

    /**
     * Efficient unsharp/kernel sharpening convolution on pixel arrays.
     */
    private fun sharpenBitmap(src: Bitmap, sharpnessValue: Float): Bitmap {
        val width = src.width
        val height = src.height
        val size = width * height
        val srcPixels = IntArray(size)
        val destPixels = IntArray(size)

        src.getPixels(srcPixels, 0, width, 0, 0, width, height)

        // Standard sharpening convolution parameters
        // Kernel format:
        // [  0, -w,  0 ]
        // [ -w, 1+4w, -w ]
        // [  0, -w,  0 ]
        val centerWeight = 1.0f + 4.0f * sharpnessValue
        val edgeWeight = -sharpnessValue

        // Copy source pixels initially for easy boundary handling
        System.arraycopy(srcPixels, 0, destPixels, 0, size)

        for (y in 1 until height - 1) {
            val yOffset = y * width
            val yPrevOffset = (y - 1) * width
            val yNextOffset = (y + 1) * width

            for (x in 1 until width - 1) {
                val idx = yOffset + x

                val c = srcPixels[idx]
                val l = srcPixels[idx - 1]
                val r = srcPixels[idx + 1]
                val t = srcPixels[yPrevOffset + x]
                val b = srcPixels[yNextOffset + x]

                // Extract colors
                val a = (c ushr 24) and 0xff
                val cR = (c ushr 16) and 0xff
                val cG = (c ushr 8) and 0xff
                val cB = c and 0xff

                val lR = (l ushr 16) and 0xff; val lG = (l ushr 8) and 0xff; val lB = l and 0xff
                val rR = (r ushr 16) and 0xff; val rG = (r ushr 8) and 0xff; val rB = r and 0xff
                val tR = (t ushr 16) and 0xff; val tG = (t ushr 8) and 0xff; val tB = t and 0xff
                val bR = (b ushr 16) and 0xff; val bG = (b ushr 8) and 0xff; val bB = b and 0xff

                // Multiply kernel weights
                var red = cR.toFloat() * centerWeight + (lR + rR + tR + bR).toFloat() * edgeWeight
                var green = cG.toFloat() * centerWeight + (lG + rG + tG + bG).toFloat() * edgeWeight
                var blue = cB.toFloat() * centerWeight + (lB + rB + tB + bB).toFloat() * edgeWeight

                // Clamp channel values to [0, 255]
                if (red < 0f) red = 0f else if (red > 255f) red = 255f
                if (green < 0f) green = 0f else if (green > 255f) green = 255f
                if (blue < 0f) blue = 0f else if (blue > 255f) blue = 255f

                destPixels[idx] = (a shl 24) or (red.toInt() shl 16) or (green.toInt() shl 8) or blue.toInt()
            }
        }

        val outBitmap = Bitmap.createBitmap(width, height, src.config ?: Bitmap.Config.ARGB_8888)
        outBitmap.setPixels(destPixels, 0, width, 0, 0, width, height)
        return outBitmap
    }
}
