package com.aistudio.ecotobacco.kfzqw.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.ecotobacco.kfzqw.BuildConfig
import com.aistudio.ecotobacco.kfzqw.data.remote.InAppUpdateState
import com.aistudio.ecotobacco.kfzqw.viewmodel.TobaccoViewModel
import java.util.Locale

@Composable
fun InAppUpdateHost(viewModel: TobaccoViewModel) {
    val context = LocalContext.current
    val updateState by viewModel.updateState.collectAsState()

    when (val state = updateState) {
        is InAppUpdateState.Checking -> {
            AlertDialog(
                onDismissRequest = { /* Keep visible while checking */ },
                title = { Text("Memeriksa Pembaruan...", fontWeight = FontWeight.Bold) },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
                        Text("Menghubungi GitHub Releases API...")
                    }
                },
                confirmButton = {},
                shape = RoundedCornerShape(24.dp)
            )
        }

        is InAppUpdateState.UpdateAvailable -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissUpdateDialog() },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CloudDownload, null, tint = MaterialTheme.colorScheme.primary)
                        Text("Pembaruan Tersedia!", fontWeight = FontWeight.ExtraBold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "${state.release.tagName} • ${state.release.title}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (state.release.apkSize > 0L) {
                            val sizeMb = state.release.apkSize / (1024f * 1024f)
                            Text(
                                text = String.format(Locale.US, "Ukuran file: %.1f MB", sizeMb),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Catatan Rilis:\n${state.release.releaseNotes}",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 6
                        )
                        HorizontalDivider()
                        if (state.release.hasDirectApk) {
                            Text(
                                text = "Unduh & instalasi berjalan 100% di dalam aplikasi dengan animasi progres, lalu installer otomatis dibersihkan.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = "Rilis ini belum melampirkan file .apk langsung. Anda dapat membukanya di browser.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                confirmButton = {
                    if (state.release.hasDirectApk) {
                        Button(onClick = { viewModel.downloadAndInstallUpdate(state.release) }) {
                            Text("Unduh & Pasang In-App")
                        }
                    } else {
                        Button(onClick = {
                            val targetUrl = state.release.htmlUrl
                            if (targetUrl.isNotBlank()) {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Tidak dapat membuka browser.", Toast.LENGTH_SHORT).show()
                                }
                            }
                            viewModel.dismissUpdateDialog()
                        }) {
                            Text("Buka Halaman Rilis")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissUpdateDialog() }) {
                        Text("Nanti Saja")
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }

        is InAppUpdateState.Downloading -> {
            val infiniteTransition = rememberInfiniteTransition(label = "inAppDownloadAnim")
            val bounceY by infiniteTransition.animateFloat(
                initialValue = -7f,
                targetValue = 7f,
                animationSpec = infiniteRepeatable(
                    animation = tween(700, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bounce"
            )
            val sweepAngle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1600, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "sweep"
            )
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 0.92f,
                targetValue = 1.08f,
                animationSpec = infiniteRepeatable(
                    animation = tween(900, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse"
            )
            val animatedProgress by animateFloatAsState(
                targetValue = if (state.totalBytes > 0) (state.progressPercent / 100f).coerceIn(0f, 1f) else 0f,
                animationSpec = tween(220),
                label = "downloadProgress"
            )
            val mbDownloaded = state.bytesDownloaded / (1024f * 1024f)
            val mbTotal = state.totalBytes / (1024f * 1024f)
            val mbText = if (state.totalBytes > 0) {
                String.format(Locale.US, "%.1f MB / %.1f MB", mbDownloaded, mbTotal)
            } else {
                String.format(Locale.US, "%.1f MB", mbDownloaded)
            }
            val speedText = if (state.speedBytesPerSec > 0L) {
                val kb = state.speedBytesPerSec / 1024f
                if (kb >= 1024f) String.format(Locale.US, "%.1f MB/s", kb / 1024f)
                else String.format(Locale.US, "%.0f KB/s", kb)
            } else ""

            val ringColor = MaterialTheme.colorScheme.primary
            val trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)

            AlertDialog(
                onDismissRequest = { /* Keep open during download */ },
                title = null,
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(96.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                modifier = Modifier.size(92.dp).scale(pulseScale)
                            ) {}
                            Canvas(modifier = Modifier.size(86.dp)) {
                                drawArc(
                                    color = trackColor,
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = ringColor,
                                    startAngle = sweepAngle,
                                    sweepAngle = 110f,
                                    useCenter = false,
                                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(64.dp)
                            ) {}
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "Mengunduh APK",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp).offset(y = bounceY.dp)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "Mengunduh Pembaruan...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = state.release?.tagName?.takeIf { it.isNotBlank() }?.let { "Versi $it • Unduhan In-App" }
                                    ?: "Unduhan APK berjalan langsung di dalam aplikasi",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = "${state.progressPercent}%",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = mbText,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (speedText.isNotBlank()) {
                                        Text(
                                            text = speedText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.DeleteForever,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    "Setelah unduhan 100%, instalasi in-app langsung dimulai & cache APK dibersihkan otomatis.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {},
                shape = RoundedCornerShape(26.dp)
            )
        }

        is InAppUpdateState.PermissionRequired -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissUpdateDialog() },
                icon = {
                    Icon(
                        Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = { Text("Izinkan Instalasi In-App", fontWeight = FontWeight.ExtraBold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "File APK (${state.release.tagName}) sudah selesai diunduh! Agar pemasangan dapat berjalan langsung dari dalam Eco Tobacco, aktifkan izin \"Instal aplikasi tidak dikenal\" untuk aplikasi ini.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { viewModel.openInstallPermissionSettings() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Buka Pengaturan Izin")
                        }
                        OutlinedButton(
                            onClick = { viewModel.installDownloadedApk(state.apkFile, state.release) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Sudah Diizinkan, Pasang Sekarang")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissUpdateDialog() }) {
                        Text("Tutup")
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }

        is InAppUpdateState.ReadyToInstall -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissUpdateDialog() },
                icon = {
                    Icon(Icons.Default.InstallMobile, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
                },
                title = { Text("Siap Dipasang In-App", fontWeight = FontWeight.ExtraBold) },
                text = {
                    Text("Pembaruan ${state.release.tagName} telah diunduh dan siap dipasang langsung di dalam aplikasi.")
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.installDownloadedApk(state.apkFile, state.release)
                    }) {
                        Text("Pasang In-App Sekarang")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissUpdateDialog() }) {
                        Text("Tutup")
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }

        is InAppUpdateState.Installing -> {
            val infiniteTransition = rememberInfiniteTransition(label = "inAppInstallAnim")
            val iconScale by infiniteTransition.animateFloat(
                initialValue = 0.90f,
                targetValue = 1.10f,
                animationSpec = infiniteRepeatable(
                    animation = tween(680, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "installScale"
            )
            val ringRotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1400, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "installSweep"
            )
            val animatedInstallProgress by animateFloatAsState(
                targetValue = (state.progressPercent / 100f).coerceIn(0f, 1f),
                animationSpec = tween(260),
                label = "installProgress"
            )
            val primaryColor = MaterialTheme.colorScheme.primary
            val trackColor = MaterialTheme.colorScheme.surfaceVariant

            AlertDialog(
                onDismissRequest = { /* Keep visible while PackageInstaller runs */ },
                title = null,
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(102.dp)
                        ) {
                            Canvas(modifier = Modifier.size(96.dp)) {
                                drawArc(
                                    color = trackColor,
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = primaryColor,
                                    startAngle = -90f,
                                    sweepAngle = (animatedInstallProgress * 360f).coerceAtLeast(18f),
                                    useCenter = false,
                                    style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = primaryColor.copy(alpha = 0.35f),
                                    startAngle = ringRotation,
                                    sweepAngle = 70f,
                                    useCenter = false,
                                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(66.dp).scale(iconScale)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.SystemUpdate,
                                        contentDescription = "Memasang Aplikasi",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Memasang Pembaruan (${state.progressPercent}%)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = state.stageText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }

                        LinearProgressIndicator(
                            progress = { animatedInstallProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            InstallStageRow("1. Menyiapkan paket APK", state.progressPercent >= 20)
                            InstallStageRow("2. Memverifikasi tanda tangan", state.progressPercent >= 65)
                            InstallStageRow("3. Memasang pembaruan sistem", state.progressPercent >= 90)
                        }

                        if (state.awaitingUserConfirm) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Ketuk 'Update / Instal' pada dialog sistem Android untuk menyelesaikan proses instalasi.",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(10.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    if (state.awaitingUserConfirm) {
                        TextButton(onClick = { viewModel.dismissUpdateDialog() }) {
                            Text("Sembunyikan")
                        }
                    }
                },
                shape = RoundedCornerShape(26.dp)
            )
        }

        is InAppUpdateState.InstallSuccess -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissUpdateDialog() },
                icon = {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                },
                title = { Text("Pembaruan Terpasang!", fontWeight = FontWeight.ExtraBold) },
                text = {
                    Text(
                        "Eco Tobacco ${state.release.tagName} berhasil dipasang secara in-app dan file installer APK telah dibersihkan otomatis."
                    )
                },
                confirmButton = {
                    Button(onClick = { viewModel.dismissUpdateDialog() }) {
                        Text("Selesai")
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }

        is InAppUpdateState.UpToDate -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissUpdateDialog() },
                title = { Text("Aplikasi Terkini", fontWeight = FontWeight.Bold) },
                text = {
                    Text("Aplikasi Eco Tobacco v${BuildConfig.VERSION_NAME} sudah menggunakan versi terbaru dari GitHub.")
                },
                confirmButton = {
                    Button(onClick = { viewModel.dismissUpdateDialog() }) {
                        Text("OK")
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }

        is InAppUpdateState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissUpdateDialog() },
                icon = {
                    Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
                },
                title = { Text("Info Pembaruan", fontWeight = FontWeight.Bold) },
                text = { Text(state.message) },
                confirmButton = {
                    val pendingApk = state.pendingApkFile
                    val rel = state.release
                    if (pendingApk != null && pendingApk.exists() && rel != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                if (viewModel.installApkSystemFallback(pendingApk)) {
                                    viewModel.dismissUpdateDialog()
                                }
                            }) {
                                Text("Installer Sistem")
                            }
                            Button(onClick = {
                                viewModel.installDownloadedApk(pendingApk, rel)
                            }) {
                                Text("Pasang Lagi")
                            }
                        }
                    } else {
                        Button(onClick = {
                            viewModel.checkForGitHubUpdate(currentVersion = BuildConfig.VERSION_NAME, silent = false)
                        }) {
                            Text("Coba Lagi")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissUpdateDialog() }) {
                        Text("Tutup")
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }

        is InAppUpdateState.Idle -> Unit
    }
}

@Composable
private fun InstallStageRow(label: String, active: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}
