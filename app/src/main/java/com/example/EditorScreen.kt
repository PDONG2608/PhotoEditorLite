package com.example

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val workingBitmap = uiState.workingBitmap
    val isProcessing = uiState.isProcessing
    val currentMode = uiState.currentMode

    // MVI UI Event Side-Effects handling
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is EditorEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is EditorEffect.SaveSuccess -> {
                    Toast.makeText(context, "Đã lưu ảnh cực nét vào Gallery!", Toast.LENGTH_LONG).show()
                }
                is EditorEffect.SaveError -> {
                    Toast.makeText(context, "Lưu ảnh thất bại!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Picker Activity setup can work with any Uri
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
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
                    viewModel.processIntent(EditorIntent.LoadBitmap(scaled))
                    Toast.makeText(context, "Đã chọn ảnh thành công!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Lỗi tải ảnh, vui lòng thử lại!", Toast.LENGTH_LONG).show()
                }
            }
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
                                viewModel.processIntent(EditorIntent.GlobalReset)
                                Toast.makeText(context, "Đã hoàn tất đặt lại ban đầu!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("reset_image_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Đặt lại")
                        }

                        // Save image
                        Button(
                            onClick = {
                                viewModel.processIntent(EditorIntent.SaveEditedImage)
                            },
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
                    uiState = uiState,
                    onIntent = { viewModel.processIntent(it) },
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
                                    uiState = uiState,
                                    onIntent = { viewModel.processIntent(it) }
                                )
                            }

                            EditMode.DRAW -> {
                                DrawControls(
                                    uiState = uiState,
                                    onIntent = { viewModel.processIntent(it) }
                                )
                            }

                            EditMode.CROP -> {
                                CropControls(
                                    onIntent = { viewModel.processIntent(it) }
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
                            onClick = { viewModel.processIntent(EditorIntent.SetMode(EditMode.ADJUST)) },
                            text = { Text("Ánh Sáng", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Compare, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            modifier = Modifier.testTag("tab_adjust")
                        )
                        Tab(
                            selected = currentMode == EditMode.DRAW,
                            onClick = { viewModel.processIntent(EditorIntent.SetMode(EditMode.DRAW)) },
                            text = { Text("Sketch Vẽ", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Gesture, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            modifier = Modifier.testTag("tab_draw")
                        )
                        Tab(
                            selected = currentMode == EditMode.CROP,
                            onClick = { viewModel.processIntent(EditorIntent.SetMode(EditMode.CROP)) },
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
