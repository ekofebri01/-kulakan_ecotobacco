package com.aistudio.ecotobacco.kfzqw.ui.screens.scanner

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aistudio.ecotobacco.kfzqw.data.utils.LocalScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScannerScreen(
    onNavigateBack: () -> Unit,
    onScanComplete: (LocalScanner.ScannedResult) -> Unit
) {
    val context = LocalContext.current

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isScanning by remember { mutableStateOf(false) }
    var cameraUnavailable by remember { mutableStateOf(false) }
    var hasLaunchedInitialCamera by remember { mutableStateOf(false) }

    // Timer & Status States
    var scanTimeMillis by remember { mutableLongStateOf(0L) }
    var scanStatusMessage by remember { mutableStateOf("Menganalisis Gambar...") }

    // Launcher for Document Scanner (camera + built-in gallery import)
    val scannerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val resultData = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val capturedUri = resultData?.pages?.firstOrNull()?.imageUri
            if (capturedUri != null) {
                selectedImageUri = capturedUri
            } else if (selectedImageUri == null) {
                onNavigateBack()
            }
        } else if (selectedImageUri == null) {
            onNavigateBack()
        }
    }

    // Fallback Launcher for Gallery Photo Picker (Zero-permission, Play-compliant)
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            cameraUnavailable = false
            selectedImageUri = uri
        }
    }

    val scannerOptions = remember {
        GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(1)
            .setResultFormats(RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
    }
    val scanner = remember { GmsDocumentScanning.getClient(scannerOptions) }

    fun launchCameraScanner() {
        val activity = context as? Activity ?: return
        cameraUnavailable = false
        scanner.getStartScanIntent(activity)
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener {
                cameraUnavailable = true
                Toast.makeText(
                    context,
                    "Kamera scanner tidak tersedia, silakan pilih dari Galeri",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // Immediately open the camera when entering the scanner screen (no intermediate choice page)
    LaunchedEffect(Unit) {
        if (!hasLaunchedInitialCamera && selectedImageUri == null) {
            hasLaunchedInitialCamera = true
            launchCameraScanner()
        }
    }

    // Automatically run OCR extraction right after camera/gallery image is captured
    LaunchedEffect(selectedImageUri) {
        val uri = selectedImageUri ?: return@LaunchedEffect
        isScanning = true
        scanStatusMessage = "Membaca Teks Nota..."
        delay(200)
        val result = LocalScanner.scanReceipt(context, uri)
        scanStatusMessage = "Menyesuaikan Data Produk..."
        delay(250)
        isScanning = false
        onScanComplete(result)
    }

    // Timer Effect during Scanning
    LaunchedEffect(isScanning) {
        if (isScanning) {
            val startTime = System.currentTimeMillis()
            while (isScanning) {
                scanTimeMillis = System.currentTimeMillis() - startTime
                delay(50)
            }
        } else {
            scanTimeMillis = 0
        }
    }

    val primaryGreen = Color(0xFF10B981) // Modern Emerald Green
    val primaryCyan = Color(0xFF06B6D4)  // Futuristic Cyan Accent
    val darkBackground = Color(0xFF0F172A) // Deep Slate Navy

    Scaffold(
        containerColor = darkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "AI Scanner Nota",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = primaryGreen.copy(alpha = 0.2f),
                                shape = CircleShape,
                                border = androidx.compose.foundation.BorderStroke(1.dp, primaryGreen.copy(alpha = 0.6f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Psychology, null, tint = primaryGreen, modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "Spatial AI v2",
                                        color = primaryGreen,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                        Text(
                            "Deteksi otomatis barang & harga kulakan",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = darkBackground)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .padding(16.dp)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // High-Tech Viewfinder / Image Container
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF1E293B))
                    .drawBehind {
                        // Futuristic grid background pattern
                        val gridSpacing = 30.dp.toPx()
                        val gridColor = Color.White.copy(alpha = 0.03f)
                        var x = 0f
                        while (x < size.width) {
                            drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                            x += gridSpacing
                        }
                        var y = 0f
                        while (y < size.height) {
                            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                            y += gridSpacing
                        }
                    }
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Preview Nota",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )

                    // Laser Scanner Line Overlay Animation (When scanning)
                    if (isScanning) {
                        val infiniteTransition = rememberInfiniteTransition(label = "laser")
                        val laserPos by infiniteTransition.animateFloat(
                            initialValue = 0.05f,
                            targetValue = 0.95f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1800, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "laserPos"
                        )

                        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            val lineY = size.height * laserPos
                            drawLine(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        primaryCyan,
                                        primaryGreen,
                                        primaryCyan,
                                        Color.Transparent
                                    )
                                ),
                                start = Offset(0f, lineY),
                                end = Offset(size.width, lineY),
                                strokeWidth = 4.dp.toPx()
                            )
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        primaryGreen.copy(alpha = 0.25f),
                                        Color.Transparent
                                    )
                                ),
                                topLeft = Offset(0f, maxOf(0f, lineY - 60.dp.toPx())),
                                size = Size(size.width, 60.dp.toPx())
                            )
                        }
                    }

                    // Scanner Viewfinder Corner Markers
                    Canvas(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                        val stroke = 3.dp.toPx()
                        val cornerLen = 28.dp.toPx()
                        val color = if (isScanning) primaryGreen else primaryCyan.copy(alpha = 0.7f)

                        drawLine(color, Offset(0f, 0f), Offset(cornerLen, 0f), strokeWidth = stroke)
                        drawLine(color, Offset(0f, 0f), Offset(0f, cornerLen), strokeWidth = stroke)

                        drawLine(color, Offset(size.width, 0f), Offset(size.width - cornerLen, 0f), strokeWidth = stroke)
                        drawLine(color, Offset(size.width, 0f), Offset(size.width, cornerLen), strokeWidth = stroke)

                        drawLine(color, Offset(0f, size.height), Offset(cornerLen, size.height), strokeWidth = stroke)
                        drawLine(color, Offset(0f, size.height), Offset(0f, size.height - cornerLen), strokeWidth = stroke)

                        drawLine(color, Offset(size.width, size.height), Offset(size.width - cornerLen, size.height), strokeWidth = stroke)
                        drawLine(color, Offset(size.width, size.height), Offset(size.width, size.height - cornerLen), strokeWidth = stroke)
                    }
                } else if (cameraUnavailable) {
                    // Error fallback when GMS Document Scanner isn't available on the device
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = primaryCyan.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, primaryCyan.copy(alpha = 0.4f)),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = primaryCyan
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Kamera Scanner Tidak Tersedia",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Perangkat ini belum mendukung kamera scanner otomatis. Anda tetap bisa memilih foto nota dari galeri.",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    // Brief launching state while system camera opens
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(56.dp),
                            color = primaryGreen,
                            strokeWidth = 4.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Membuka Kamera...",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                // AI Processing Loading Overlay
                if (isScanning) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(72.dp),
                                    color = primaryGreen,
                                    strokeWidth = 5.dp
                                )
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = primaryCyan,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = scanStatusMessage,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Mendeteksi posisi barang & perhitungan harga...",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Timer Badge
                            Surface(
                                color = Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(100.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, primaryGreen.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.History, null, tint = primaryGreen, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "${(scanTimeMillis / 1000)}.${(scanTimeMillis % 1000) / 100}s",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Fallback Action Bar shown ONLY when camera scanner is unavailable
            AnimatedVisibility(visible = cameraUnavailable && selectedImageUri == null) {
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { launchCameraScanner() },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("scan_camera_retry_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.12f),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "Coba Buka Kamera", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Coba Lagi", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Button(
                            onClick = {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("scan_gallery_fallback_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryCyan.copy(alpha = 0.18f),
                                contentColor = primaryCyan
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, primaryCyan.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Image, contentDescription = "Pilih dari Galeri", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Galeri", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReceiptScannerScreenPreview() {
    ReceiptScannerScreen(
        onNavigateBack = {},
        onScanComplete = {}
    )
}
