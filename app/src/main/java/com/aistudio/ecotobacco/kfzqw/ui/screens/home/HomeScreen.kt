package com.aistudio.ecotobacco.kfzqw.ui.screens.home

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.ecotobacco.kfzqw.BuildConfig
import com.aistudio.ecotobacco.kfzqw.data.remote.SyncStatus
import com.aistudio.ecotobacco.kfzqw.ui.theme.LocalUiConfig
import com.aistudio.ecotobacco.kfzqw.ui.theme.scaled
import com.aistudio.ecotobacco.kfzqw.viewmodel.TobaccoViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TobaccoViewModel,
    onNavigateToTransaction: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToReportsThisMonth: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDebug: () -> Unit,
    onNavigateToProcurement: () -> Unit,
    onNavigateToScanner: () -> Unit = onNavigateToTransaction
) {
    val uiConfig = LocalUiConfig.current
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    var versionClickCount by remember { mutableIntStateOf(0) }
    var lastVersionClickTime by remember { mutableLongStateOf(0L) }

    val totalSpent by viewModel.totalSpentThisMonth.collectAsStateWithLifecycle()
    val transCount by viewModel.transactionCountThisMonth.collectAsStateWithLifecycle()
    val prodCount by viewModel.productCount.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()

    val firebaseUser by viewModel.firebaseUser.collectAsStateWithLifecycle()
    val isFirestoreSyncing by viewModel.isFirestoreSyncing.collectAsStateWithLifecycle()
    val firestoreSyncProgress by viewModel.firestoreSyncProgress.collectAsStateWithLifecycle()
    val firestoreMessage by viewModel.firestoreMessage.collectAsStateWithLifecycle()

    var showExitConfirmation by remember { mutableStateOf(false) }

    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).apply {
            maximumFractionDigits = 0
        }
    }

    val bgColor = MaterialTheme.colorScheme.background
    val cardColor = MaterialTheme.colorScheme.surface
    val cardBorder = MaterialTheme.colorScheme.outlineVariant
    val primaryAccent = MaterialTheme.colorScheme.primary
    val onPrimaryAccent = MaterialTheme.colorScheme.onPrimary
    val warmAmber = Color(0xFFF59E0B)
    val isDarkSurface = bgColor.luminance() < 0.45f

    // Konfirmasi Exit
    BackHandler {
        showExitConfirmation = true
    }

    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = { Text("Konfirmasi Keluar", fontWeight = FontWeight.Bold, fontSize = 18.sp.scaled) },
            text = { Text("Apakah Anda yakin ingin keluar dari aplikasi Eco Tobacco?", fontSize = 14.sp.scaled) },
            confirmButton = {
                TextButton(
                    onClick = {
                        (context as? Activity)?.finish()
                    }
                ) {
                    Text("Keluar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 14.sp.scaled)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmation = false }) {
                    Text("Batal", fontSize = 14.sp.scaled)
                }
            },
            shape = RoundedCornerShape(uiConfig.cornerRadius.dp),
            containerColor = cardColor
        )
    }

    // --- OPTIONAL MATRIX EFFECT STATE ---
    val matrixInfiniteTransition = rememberInfiniteTransition(label = "matrix")
    val matrixProgress by matrixInfiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "matrixProgress"
    )

    val matrixPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = false
            typeface = android.graphics.Typeface.MONOSPACE
        }
    }
    val matrixChars = remember { arrayOf("0", "1", "0", "1", "1", "0", "0", "1") }

    Scaffold(
        containerColor = bgColor,
        bottomBar = {
            FloatingHomeBottomBar(
                cardColor = cardColor,
                borderColor = cardBorder,
                primaryAccent = primaryAccent,
                onPrimaryAccent = onPrimaryAccent,
                onHomeClick = {
                    coroutineScope.launch { scrollState.animateScrollTo(0) }
                },
                onReportsClick = onNavigateToReports,
                onScanClick = onNavigateToScanner,
                onDatabaseClick = onNavigateToProducts,
                onSettingsClick = onNavigateToSettings
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Subtle top-left radial emerald glow matching the reference mockup
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryAccent.copy(alpha = if (isDarkSurface) 0.14f else 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.16f, size.height * 0.06f),
                        radius = size.width * 0.9f
                    ),
                    center = Offset(size.width * 0.16f, size.height * 0.06f),
                    radius = size.width * 0.9f
                )
            }

            // Optional Matrix background when enabled in Theme Customization
            if (uiConfig.showMatrixBackground) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val colStep = 32.dp.toPx()
                    val rowStep = 44.dp.toPx()
                    val columns = (size.width / colStep).toInt()
                    val rows = (size.height / rowStep).toInt() + 2
                    matrixPaint.textSize = 10.sp.toPx()

                    val r = (primaryAccent.red * 255).toInt()
                    val g = (primaryAccent.green * 255).toInt()
                    val b = (primaryAccent.blue * 255).toInt()

                    for (i in 0 until columns) {
                        val x = i * colStep
                        val speedFactor = (i % 5 + 1) * 0.4f
                        val yOffset = (matrixProgress * size.height * speedFactor) % size.height

                        for (j in 0 until rows) {
                            val y = (yOffset + j * rowStep) % size.height
                            val charStr = matrixChars[(i + j) % matrixChars.size]
                            val alpha = (0.22f * (y / size.height)).coerceIn(0.05f, 0.22f)
                            matrixPaint.setARGB((alpha * 255).toInt(), r, g, b)
                            drawContext.canvas.nativeCanvas.drawText(charStr, x, y, matrixPaint)
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(scrollState)
                    .padding(
                        start = 16.dp.scaled,
                        end = 16.dp.scaled,
                        top = 14.dp.scaled,
                        bottom = innerPadding.calculateBottomPadding() + 18.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(14.dp.scaled)
            ) {
                // --- 1. TOP BRAND BAR ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp.scaled)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = primaryAccent.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, primaryAccent.copy(alpha = 0.25f)),
                            modifier = Modifier.size(36.dp.scaled)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Eco,
                                    contentDescription = "Eco Tobacco",
                                    tint = primaryAccent,
                                    modifier = Modifier.size(19.dp.scaled)
                                )
                            }
                        }
                        Text(
                            text = "ECO TOBACCO",
                            style = MaterialTheme.typography.titleSmall,
                            color = primaryAccent,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.6.sp,
                            fontSize = 13.sp.scaled
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = cardColor,
                        border = BorderStroke(1.dp, cardBorder),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastVersionClickTime < 3000) versionClickCount++
                                else versionClickCount = 1
                                lastVersionClickTime = currentTime
                                if (versionClickCount >= 7) {
                                    versionClickCount = 0
                                    onNavigateToDebug()
                                }
                            }
                            .testTag("home_version_badge")
                    ) {
                        Text(
                            text = "v${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp.scaled,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                // --- 2. GREETING HEADER ---
                Column(
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Selamat Datang,",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp.scaled
                    )
                    Text(
                        text = "Partner Eksekutif\nEco Tobacco",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 32.sp.scaled,
                        fontSize = 26.sp.scaled
                    )
                }

                // --- 3. CONNECTIVITY / ACTIVE SYNC BANNER (IF OFFLINE OR SYNCING) ---
                val isOnline = viewModel.syncHelper.isOnline
                if (!isOnline || isFirestoreSyncing) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = if (!isOnline) {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.75f)
                        } else {
                            primaryAccent.copy(alpha = 0.14f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (!isOnline) MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
                            else primaryAccent.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (!isOnline) Icons.Default.CloudOff else Icons.Default.CloudSync,
                                        contentDescription = "Status",
                                        tint = if (!isOnline) MaterialTheme.colorScheme.error else primaryAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (!isOnline) {
                                            "Mode Offline (Tanpa Internet)"
                                        } else {
                                            "Sinkronisasi Cloud (${(firestoreSyncProgress * 100).toInt()}%)"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!isOnline) {
                                            MaterialTheme.colorScheme.onErrorContainer
                                        } else {
                                            MaterialTheme.colorScheme.onBackground
                                        }
                                    )
                                }
                                if (isFirestoreSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = primaryAccent
                                    )
                                }
                            }
                            if (isFirestoreSyncing) {
                                LinearProgressIndicator(
                                    progress = { firestoreSyncProgress.coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = primaryAccent,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Text(
                                    text = firestoreMessage ?: "Menyiapkan data stok & transaksi...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else if (!isOnline) {
                                Text(
                                    text = "Data tetap aman tersimpan di penyimpanan lokal.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }

                // --- 4. HERO EMERALD SUMMARY CARD ("Total Kulakan Bulan Ini") ---
                val heroGradientStart = lerp(Color(0xFF195E42), primaryAccent, 0.14f)
                val heroGradientEnd = lerp(Color(0xFF0E3424), primaryAccent, 0.06f)
                val heroShape = RoundedCornerShape(24.dp)

                Surface(
                    onClick = onNavigateToReportsThisMonth,
                    shape = heroShape,
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, primaryAccent.copy(alpha = 0.30f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_hero_total_card")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(heroGradientStart, heroGradientEnd)
                                )
                            )
                            .padding(horizontal = 20.dp.scaled, vertical = 20.dp.scaled)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp.scaled)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total Kulakan Bulan Ini",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.88f),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp.scaled
                                )
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.14f),
                                    modifier = Modifier.size(38.dp.scaled)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Payments,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.92f),
                                            modifier = Modifier.size(19.dp.scaled)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = currencyFormatter.format(totalSpent.toDouble()).replace("Rp", "Rp "),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                fontSize = 35.sp.scaled,
                                letterSpacing = (-0.5).sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (totalSpent <= 0.0) {
                                        "Belum ada kulakan bulan ini"
                                    } else {
                                        "$transCount transaksi bulan ini"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.72f),
                                    fontSize = 12.sp.scaled
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    repeat(5) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.25f))
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(width = 7.dp, height = 10.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                }
                            }
                        }
                    }
                }

                // --- 5. TWO-COLUMN STAT CARDS (TRANSAKSI & VARIAN PRODUK) ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp.scaled)
                ) {
                    StatBentoCard(
                        label = "Transaksi",
                        countText = "$transCount",
                        unitLabel = "Nota",
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        iconTint = primaryAccent,
                        cardColor = cardColor,
                        borderColor = cardBorder,
                        onClick = onNavigateToReportsThisMonth,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_stat_transaksi")
                    )
                    StatBentoCard(
                        label = "Varian Produk",
                        countText = "$prodCount",
                        unitLabel = "Item",
                        icon = Icons.Default.Inventory2,
                        iconTint = warmAmber,
                        cardColor = cardColor,
                        borderColor = cardBorder,
                        onClick = onNavigateToProducts,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_stat_produk")
                    )
                }

                // --- 6. GOOGLE DRIVE & CLOUD SYNC CARD ---
                val isCloudConnected = (syncStatus != SyncStatus.DISCONNECTED) ||
                    (firebaseUser != null) ||
                    viewModel.syncHelper.isLocalUserConnected
                val isSyncingActive = isFirestoreSyncing || (syncStatus == SyncStatus.SYNCING)

                Surface(
                    onClick = onNavigateToSettings,
                    shape = RoundedCornerShape(20.dp),
                    color = cardColor,
                    border = BorderStroke(
                        1.dp,
                        if (isCloudConnected) primaryAccent.copy(alpha = 0.35f) else cardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_cloud_sync_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp.scaled, vertical = 14.dp.scaled),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(13.dp),
                                    color = if (isCloudConnected) {
                                        primaryAccent.copy(alpha = 0.14f)
                                    } else {
                                        warmAmber.copy(alpha = 0.13f)
                                    },
                                    modifier = Modifier.size(44.dp.scaled)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (isSyncingActive) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp.scaled),
                                                strokeWidth = 2.dp,
                                                color = primaryAccent
                                            )
                                        } else {
                                            Icon(
                                                imageVector = if (isCloudConnected) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                                contentDescription = "Cloud Status",
                                                tint = if (isCloudConnected) primaryAccent else warmAmber,
                                                modifier = Modifier.size(21.dp.scaled)
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.width(12.dp.scaled))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isCloudConnected) {
                                            viewModel.syncHelper.userEmail
                                                ?: firebaseUser?.displayName
                                                ?: firebaseUser?.email
                                                ?: "Akun Cloud Aktif"
                                        } else {
                                            "Google Drive & Cloud Sync"
                                        },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp.scaled,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (isCloudConnected) {
                                            if (isSyncingActive) {
                                                firestoreMessage ?: "Sedang menyinkronkan stok..."
                                            } else {
                                                "Tersinkronisasi & Aman"
                                            }
                                        } else {
                                            "Ketuk untuk masuk & sinkronkan data"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isCloudConnected) primaryAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.5.sp.scaled,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(Modifier.width(10.dp.scaled))

                            if (isCloudConnected) {
                                IconButton(
                                    onClick = {
                                        viewModel.forceSyncNow { status ->
                                            (context as? Activity)?.runOnUiThread {
                                                Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        viewModel.syncFirestore()
                                    },
                                    enabled = !isSyncingActive,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("home_quick_sync_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = "Sinkron Cepat",
                                        tint = primaryAccent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            } else {
                                Button(
                                    onClick = onNavigateToSettings,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = primaryAccent,
                                        contentColor = onPrimaryAccent
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                                    modifier = Modifier
                                        .heightIn(min = 42.dp)
                                        .testTag("home_cloud_login_button")
                                ) {
                                    Text(
                                        text = "Masuk",
                                        fontSize = 13.sp.scaled,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }

                        if (isSyncingActive) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LinearProgressIndicator(
                                    progress = { firestoreSyncProgress.coerceIn(0.05f, 1.0f) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = primaryAccent,
                                    trackColor = primaryAccent.copy(alpha = 0.2f)
                                )
                                Text(
                                    text = "${(firestoreSyncProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryAccent,
                                    fontSize = 10.sp.scaled
                                )
                            }
                        }
                    }
                }

                // --- 7. NAVIGASI CEPAT BENTO GRID ---
                Column(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp.scaled)
                ) {
                    Text(
                        text = "Navigasi Cepat",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 17.sp.scaled
                    )

                    // Row 1: 2 Featured Action Cards (Input Kulakan Mint Hero + Rencana Baru Dark)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp.scaled)
                    ) {
                        FeaturedActionCard(
                            title = "Input Kulakan",
                            subtitle = "Scan nota & manual",
                            icon = Icons.Default.ShoppingCart,
                            isFeatured = true,
                            primaryAccent = primaryAccent,
                            onPrimaryAccent = onPrimaryAccent,
                            cardColor = cardColor,
                            borderColor = cardBorder,
                            onClick = onNavigateToTransaction,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_menu_input_kulakan")
                        )

                        FeaturedActionCard(
                            title = "Rencana Baru",
                            subtitle = "Buat daftar belanja",
                            icon = Icons.AutoMirrored.Filled.PlaylistAddCheck,
                            isFeatured = false,
                            primaryAccent = primaryAccent,
                            onPrimaryAccent = onPrimaryAccent,
                            cardColor = cardColor,
                            borderColor = cardBorder,
                            onClick = onNavigateToProcurement,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_menu_rencana_baru")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingHomeBottomBar(
    cardColor: Color,
    borderColor: Color,
    primaryAccent: Color,
    onPrimaryAccent: Color,
    onHomeClick: () -> Unit,
    onReportsClick: () -> Unit,
    onScanClick: () -> Unit,
    onDatabaseClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = cardColor.copy(alpha = 0.96f),
            border = BorderStroke(1.dp, borderColor),
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BottomNavItem(
                    label = "Beranda",
                    icon = Icons.Default.Home,
                    selected = true,
                    primaryAccent = primaryAccent,
                    onClick = onHomeClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("bottom_nav_home")
                )
                BottomNavItem(
                    label = "Laporan",
                    icon = Icons.Default.Assessment,
                    selected = false,
                    primaryAccent = primaryAccent,
                    onClick = onReportsClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("bottom_nav_reports")
                )

                // Center Prominent Scan Action
                Surface(
                    onClick = onScanClick,
                    shape = RoundedCornerShape(18.dp),
                    color = primaryAccent,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .size(width = 56.dp, height = 50.dp)
                        .testTag("bottom_nav_scan")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Pindai Nota",
                            tint = onPrimaryAccent,
                            modifier = Modifier.size(25.dp)
                        )
                    }
                }

                BottomNavItem(
                    label = "Database",
                    icon = Icons.Default.Inventory,
                    selected = false,
                    primaryAccent = primaryAccent,
                    onClick = onDatabaseClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("bottom_nav_database")
                )
                BottomNavItem(
                    label = "Setelan",
                    icon = Icons.Default.Tune,
                    selected = false,
                    primaryAccent = primaryAccent,
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("bottom_nav_settings")
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    primaryAccent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = if (selected) primaryAccent else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) primaryAccent.copy(alpha = 0.14f) else Color.Transparent,
        modifier = modifier.minimumInteractiveComponentSize()
    ) {
        Column(
            modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun StatBentoCard(
    label: String,
    countText: String,
    unitLabel: String,
    icon: ImageVector,
    iconTint: Color,
    cardColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = cardColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp.scaled, vertical = 14.dp.scaled),
            verticalArrangement = Arrangement.spacedBy(12.dp.scaled)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp.scaled,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(19.dp.scaled)
                )
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = countText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 26.sp.scaled
                )
                Text(
                    text = unitLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp.scaled,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun FeaturedActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isFeatured: Boolean,
    primaryAccent: Color,
    onPrimaryAccent: Color,
    cardColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isFeatured) primaryAccent else cardColor
    val titleColor = if (isFeatured) onPrimaryAccent else MaterialTheme.colorScheme.onSurface
    val subColor = if (isFeatured) onPrimaryAccent.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant
    val iconPillBg = if (isFeatured) Color.Black.copy(alpha = 0.11f) else primaryAccent.copy(alpha = 0.14f)
    val iconTint = if (isFeatured) onPrimaryAccent else primaryAccent
    val arrowTint = if (isFeatured) onPrimaryAccent.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = containerColor,
        border = if (isFeatured) null else BorderStroke(1.dp, borderColor),
        modifier = modifier.heightIn(min = 130.dp.scaled)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp.scaled),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = iconPillBg,
                    modifier = Modifier.size(42.dp.scaled)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(21.dp.scaled)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.NorthEast,
                    contentDescription = null,
                    tint = arrowTint,
                    modifier = Modifier.size(18.dp.scaled)
                )
            }

            Spacer(Modifier.height(16.dp.scaled))

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = titleColor,
                    fontSize = 15.5.sp.scaled,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = subColor,
                    fontSize = 11.5.sp.scaled,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
