package com.aistudio.ecotobacco.kfzqw.ui.screens.settings

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aistudio.ecotobacco.kfzqw.ui.Routes
import com.aistudio.ecotobacco.kfzqw.viewmodel.TobaccoViewModel
import com.aistudio.ecotobacco.kfzqw.widget.TobaccoAppWidgetProvider

@Composable
fun DataAndWidgetSection(viewModel: TobaccoViewModel) {
    val context = LocalContext.current
    val defaultReportFilter by viewModel.defaultReportFilter.collectAsState()
    val reportFilterOptions = TobaccoViewModel.REPORT_FILTER_OPTIONS

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        PremiumSettingSection(title = "Tampilan Default Rekap Kulakan") {
            PremiumCardGroup {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.QueryStats,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Periode Pembuka Rekap",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Otomatis diterapkan saat layar Rekap Kulakan dibuka",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        reportFilterOptions.chunked(3).forEach { rowOptions ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowOptions.forEach { option ->
                                    val isSelected = defaultReportFilter == option
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                viewModel.setDefaultReportFilter(option)
                                                Toast.makeText(
                                                    context,
                                                    "Tampilan default Rekap Kulakan: $option",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            .testTag("default_filter_option_${option.lowercase()}"),
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        },
                                        border = BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.outlineVariant
                                            }
                                        )
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = option,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                        }
                                    }
                                }
                                repeat(3 - rowOptions.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        PremiumSettingSection(title = "Widget Layar Utama") {
            PremiumCardGroup {
                PremiumSettingItem(
                    title = "Pasang Widget ke Layar Utama",
                    subtitle = "Pantau ringkasan stok & akses cepat dari home screen HP",
                    icon = Icons.Default.Widgets,
                    trailingBadge = "Pin",
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
                            val myProvider = ComponentName(context, TobaccoAppWidgetProvider::class.java)
                            if (appWidgetManager != null && appWidgetManager.isRequestPinAppWidgetSupported) {
                                appWidgetManager.requestPinAppWidget(myProvider, null, null)
                            } else {
                                Toast.makeText(
                                    context,
                                    "Tekan lama area kosong layar utama HP Anda > pilih Widget",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            Toast.makeText(
                                context,
                                "Tekan lama area kosong layar utama HP Anda > pilih Widget",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun UpdateAndAboutSection(
    currentGitHubRepo: String,
    isAutoCheckUpdateEnabled: Boolean,
    onOpenWhatsNew: () -> Unit,
    onCheckUpdate: () -> Unit,
    onOpenRepoDialog: () -> Unit,
    onOpenResetDialog: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Eco Tobacco",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = "v${Routes.APP_VERSION}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isAutoCheckUpdateEnabled) {
                                "Auto-cek pembaruan aktif saat aplikasi dibuka"
                            } else {
                                "Auto-cek pembaruan manual"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        PremiumSettingSection(title = "Pembaruan & Informasi Rilis") {
            PremiumCardGroup {
                Column {
                    PremiumSettingItem(
                        title = "What's New & Riwayat Perubahan",
                        subtitle = "Lihat catatan fitur & perbaikan v${Routes.APP_VERSION}",
                        icon = Icons.Default.Info,
                        trailingBadge = "v${Routes.APP_VERSION}",
                        onClick = onOpenWhatsNew
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    PremiumSettingItem(
                        title = "Cek Pembaruan GitHub",
                        subtitle = "Unduh & pasang update langsung di dalam aplikasi",
                        icon = Icons.Default.CloudDownload,
                        iconColor = MaterialTheme.colorScheme.primary,
                        onClick = onCheckUpdate
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    PremiumSettingItem(
                        title = "Target Repository GitHub",
                        subtitle = currentGitHubRepo,
                        icon = Icons.Default.Computer,
                        onClick = onOpenRepoDialog
                    )
                }
            }
        }

        PremiumSettingSection(title = "Keamanan & Reset Data") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.22f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
            ) {
                PremiumSettingItem(
                    title = "Reset Semua Data (Danger Zone)",
                    subtitle = "Hapus permanen seluruh data lokal aplikasi",
                    icon = Icons.Default.DeleteForever,
                    iconColor = MaterialTheme.colorScheme.error,
                    textColor = MaterialTheme.colorScheme.error,
                    onClick = onOpenResetDialog
                )
            }
        }
    }
}
