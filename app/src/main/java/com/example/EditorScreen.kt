package com.example

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 1. Bitmaps State
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var workingBitmap by remember { mutableStateOf<Bitmap?>(null) } // Current working/cropped base
    var adjustedWorkingBitmap by remember { mutableStateOf<Bitmap?>(null) } // Output after slider changes
    var isProcessing by remember { mutableStateOf(false) }

    // 2. Active Mode Selection
    var currentMode by remember { mutableStateOf(EditMode.ADJUST) }

    // 3. Image Adjustments Parameters
    var brightness by remember { mutableStateOf(0f) }      // -100f to 100f
    var exposure by remember { mutableStateOf(0f) }        // -2.0f to 2.0f
    var contrast by remember { mutableStateOf(1.0f) }      // 0.2f to 2.0f
    var saturation by remember { mutableStateOf(1.0f) }    // 0.0f to 2.0f
    var sharpness by remember { mutableStateOf(0f) }       // 0.0f to 3.0f

    // 4. Temporarily hold to view the original unadjusted image
    var isCheckingOriginal by remember { mutableStateOf(false) }

    // 5. Drawing Parameters
    var drawnPaths by remember { mutableStateOf<List<DrawnPath>>(emptyList()) }
    var currentPathPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

    var brushSize by remember { mutableStateOf(12f) }       // 2f to 80f
    var brushOpacity by remember { mutableStateOf(1.0f) }   // 0.1f to 1.0f
    var activeColor by remember { mutableStateOf(Color.Red) }

    // 6. Cropping Rectangle (Percentages 0f..1f relative to image width/height)
    var cropLeft by remember { mutableStateOf(0f) }
    var cropTop by remember { mutableStateOf(0f) }
    var cropRight by remember { mutableStateOf(1f) }
    var cropBottom by remember { mutableStateOf(1f) }
    var activeCropHandle by remember { mutableStateOf<Int?>(null) } // null or 0=TL, 1=TR, 2=BL, 3=BR

    // On-the-fly slider updates trigger async recalculation to keep edits non-blocking
    LaunchedEffect(workingBitmap, brightness, exposure, contrast, saturation, sharpness) {
        val base = workingBitmap ?: return@LaunchedEffect
        isProcessing = true
        val result = withContext(Dispatchers.Default) {
            ImageProcessor.adjustBitmap(
                src = base,
                brightness = brightness,
                exposure = exposure,
                contrast = contrast,
                saturation = saturation,
                sharpness = sharpness
            )
        }
        adjustedWorkingBitmap = result
        isProcessing = false
    }

    // Picker Activity setup can work with any Uri
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isProcessing = true
                val loaded = withContext(Dispatchers.IO) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            val source = ImageDecoder.createSource(context.contentResolver, uri)
                            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                                decoder.isMutableRequired = true
                                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                            }
                        } else {
                            @Suppress("DEPRECATION")
                            val legacy = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                            legacy.copy(Bitmap.Config.ARGB_8888, true)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }

                if (loaded != null) {
                    // Normalize size to screen viewport ceilings (preventing memory exhaustion)
                    val scaled = withContext(Dispatchers.Default) {
                        val maxDimension = 1200
                        val w = loaded.width
                        val h = loaded.height
                        if (w > maxDimension || h > maxDimension) {
                            val ratio = w.toFloat() / h.toFloat()
                            val (targetW, targetH) = if (ratio > 1f) {
                                Pair(maxDimension, (maxDimension / ratio).toInt())
                            } else {
                                Pair((maxDimension * ratio).toInt(), maxDimension)
                            }
                            Bitmap.createScaledBitmap(loaded, targetW, targetH, true)
                        } else {
                            loaded
                        }
                    }
                    originalBitmap = scaled
                    workingBitmap = scaled
                    adjustedWorkingBitmap = scaled
                    drawnPaths = emptyList()
                    currentPathPoints = emptyList()

                    // Reset crop selectors
                    cropLeft = 0f
                    cropTop = 0f
                    cropRight = 1f
                    cropBottom = 1f

                    Toast.makeText(context, "Đã chọn ảnh thành công!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Lỗi tải ảnh, vui lòng thử lại!", Toast.LENGTH_LONG).show()
                }
                isProcessing = false
            }
        }
    }

    // Function: Composite Color adjustments + Drawing lines on canvas and save
    fun saveEditedImage() {
        val base = workingBitmap ?: return
        val currentEditsImg = adjustedWorkingBitmap ?: base

        scope.launch {
            isProcessing = true
            val savedUri = withContext(Dispatchers.IO) {
                try {
                    // 1. Create a workspace with identical dimensions to the current cropped/base bitmap
                    val compositedResult = Bitmap.createBitmap(
                        base.width,
                        base.height,
                        Bitmap.Config.ARGB_8888
                    )
                    val nativeCanvas = android.graphics.Canvas(compositedResult)

                    // 2. Clear draw background bitmap with adjustments
                    val basePaint = android.graphics.Paint().apply { isAntiAlias = true }
                    nativeCanvas.drawBitmap(currentEditsImg, 0f, 0f, basePaint)

                    // 3. Overlay freehand brush strokes
                    drawnPaths.forEach { path ->
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

                    // 4. Register to MediaStore
                    val contentResolver = context.contentResolver
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
                        uri
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            if (savedUri != null) {
                Toast.makeText(context, "Đã lưu ảnh cực nét vào Gallery!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Lưu ảnh thất bại!", Toast.LENGTH_LONG).show()
            }
            isProcessing = false
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_scaffold"),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Camera,
                            contentDescription = "Logo",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Iris Photo Editor",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    if (workingBitmap != null) {
                        // Quick Reset
                        IconButton(
                            onClick = {
                                // Reset all parameters to normal state
                                brightness = 0f
                                exposure = 0f
                                contrast = 1.0f
                                saturation = 1.0f
                                sharpness = 0f
                                drawnPaths = emptyList()
                                currentPathPoints = emptyList()

                                // Restore original bitmap dimensions
                                workingBitmap = originalBitmap
                                adjustedWorkingBitmap = originalBitmap

                                // Reset Crop overlay coordinates
                                cropLeft = 0f
                                cropTop = 0f
                                cropRight = 1f
                                cropBottom = 1f

                                Toast.makeText(context, "Đã hoàn tất đặt lại ban đầu!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("reset_image_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Đặt lại")
                        }

                        // Save image
                        Button(
                            onClick = { saveEditedImage() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("save_image_button")
                        ) {
                            Icon(
                                Icons.Default.Save,
                                contentDescription = "Lưu",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Lưu", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = Color(0xFF0F1016) // Cool Deep Art Studio Tone
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main workspace
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF07080B)) // Pure Canvas Darkening
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                EditorWorkspace(
                    originalBitmap = originalBitmap,
                    workingBitmap = workingBitmap,
                    adjustedWorkingBitmap = adjustedWorkingBitmap,
                    currentMode = currentMode,
                    isCheckingOriginal = isCheckingOriginal,
                    onCheckingOriginalChange = { isCheckingOriginal = it },
                    drawnPaths = drawnPaths,
                    onDrawnPathsChange = { drawnPaths = it },
                    currentPathPoints = currentPathPoints,
                    onCurrentPathPointsChange = { currentPathPoints = it },
                    brushSize = brushSize,
                    brushOpacity = brushOpacity,
                    activeColor = activeColor,
                    cropLeft = cropLeft,
                    onCropLeftChange = { cropLeft = it },
                    cropTop = cropTop,
                    onCropTopChange = { cropTop = it },
                    cropRight = cropRight,
                    onCropRightChange = { cropRight = it },
                    cropBottom = cropBottom,
                    onCropBottomChange = { cropBottom = it },
                    activeCropHandle = activeCropHandle,
                    onActiveCropHandleChange = { activeCropHandle = it },
                    onImportPhotoClick = { galleryLauncher.launch("image/*") }
                )
            }

            // Processing feedback spinner
            if (isProcessing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color(0xFF1D2030)
                )
            }

            // EDITOR CONTROLS BOARD (ONLY DISPLAYED WHEN AN IMAGE IS LOADED)
            if (workingBitmap != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF131520)) // Darker control slate
                        .padding(top = 16.dp, bottom = 12.dp)
                ) {
                    // CONDITIONAL EDIT PANEL BASED ON SELECTED MODE
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                    ) {
                        when (currentMode) {
                            EditMode.ADJUST -> {
                                AdjustControls(
                                    brightness = brightness,
                                    exposure = exposure,
                                    contrast = contrast,
                                    saturation = saturation,
                                    sharpness = sharpness,
                                    onBrightnessChange = { brightness = it },
                                    onExposureChange = { exposure = it },
                                    onContrastChange = { contrast = it },
                                    onSaturationChange = { saturation = it },
                                    onSharpnessChange = { sharpness = it },
                                    onReset = {
                                        brightness = 0f
                                        exposure = 0f
                                        contrast = 1.0f
                                        saturation = 1.0f
                                        sharpness = 0f
                                    }
                                )
                            }

                            EditMode.DRAW -> {
                                DrawControls(
                                    drawnPathsExist = drawnPaths.isNotEmpty(),
                                    onUndoClick = {
                                        if (drawnPaths.isNotEmpty()) {
                                            drawnPaths = drawnPaths.dropLast(1)
                                        }
                                    },
                                    onDeleteSweepClick = {
                                        drawnPaths = emptyList()
                                        currentPathPoints = emptyList()
                                    },
                                    activeColor = activeColor,
                                    onColorChange = { activeColor = it },
                                    brushSize = brushSize,
                                    onBrushSizeChange = { brushSize = it },
                                    brushOpacity = brushOpacity,
                                    onBrushOpacityChange = { brushOpacity = it }
                                )
                            }

                            EditMode.CROP -> {
                                val src = workingBitmap
                                CropControls(
                                    onPresetSquareClick = {
                                        cropLeft = 0.1f
                                        cropTop = 0.1f
                                        cropRight = 0.9f
                                        cropBottom = 0.9f
                                        Toast.makeText(context, "Khung cắt tỉa tự do hoạt động!", Toast.LENGTH_SHORT).show()
                                    },
                                    onResetBoundaryClick = {
                                        cropLeft = 0f
                                        cropTop = 0f
                                        cropRight = 1f
                                        cropBottom = 1f
                                    },
                                    onApplyCropClick = {
                                        if (src != null) {
                                            scope.launch {
                                                isProcessing = true
                                                val cropped = withContext(Dispatchers.Default) {
                                                    val w = src.width
                                                    val h = src.height
                                                    val x = (cropLeft * w).toInt().coerceIn(0, w - 1)
                                                    val y = (cropTop * h).toInt().coerceIn(0, h - 1)
                                                    val finalWidth = ((cropRight - cropLeft) * w).toInt().coerceIn(1, w - x)
                                                    val finalHeight = ((cropBottom - cropTop) * h).toInt().coerceIn(1, h - y)

                                                    Bitmap.createBitmap(src, x, y, finalWidth, finalHeight)
                                                }

                                                // Translate our drawn paths points relative to crop dimensions!
                                                val w = src.width.toFloat()
                                                val h = src.height.toFloat()
                                                val offsetX = (cropLeft * w)
                                                val offsetY = (cropTop * h)

                                                drawnPaths = drawnPaths.map { path ->
                                                    val newPts = path.points.map { pt ->
                                                        Offset(pt.x - offsetX, pt.y - offsetY)
                                                    }.filter { pt ->
                                                        pt.x >= 0f && pt.x <= (cropped.width).toFloat() &&
                                                        pt.y >= 0f && pt.y <= (cropped.height).toFloat()
                                                    }
                                                    path.copy(points = newPts)
                                                }.filter { it.points.isNotEmpty() }

                                                workingBitmap = cropped
                                                adjustedWorkingBitmap = cropped

                                                // Restart Crop Overlays representation bounds
                                                cropLeft = 0f
                                                cropTop = 0f
                                                cropRight = 1f
                                                cropBottom = 1f

                                                Toast.makeText(context, "Đã hoàn thành cắt ảnh!", Toast.LENGTH_SHORT).show()
                                                isProcessing = false
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // MODE TABS SWITCHER (ADJUST, DRAW, CROP)
                    TabRow(
                        selectedTabIndex = currentMode.ordinal,
                        containerColor = Color(0xFF131520),
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Tab(
                            selected = currentMode == EditMode.ADJUST,
                            onClick = { currentMode = EditMode.ADJUST },
                            text = { Text("Ánh Sáng", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Compare, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            modifier = Modifier.testTag("tab_adjust")
                        )
                        Tab(
                            selected = currentMode == EditMode.DRAW,
                            onClick = { currentMode = EditMode.DRAW },
                            text = { Text("Sketch Vẽ", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Gesture, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            modifier = Modifier.testTag("tab_draw")
                        )
                        Tab(
                            selected = currentMode == EditMode.CROP,
                            onClick = { currentMode = EditMode.CROP },
                            text = { Text("Cắt Cúp", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            modifier = Modifier.testTag("tab_crop")
                        )
                    }
                }
            }
        }
    }
}
