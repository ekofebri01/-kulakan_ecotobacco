package com.aistudio.ecotobacco.kfzqw.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.ecotobacco.kfzqw.ui.theme.LocalUiConfig
import com.aistudio.ecotobacco.kfzqw.ui.theme.scaled
import com.aistudio.ecotobacco.kfzqw.viewmodel.TobaccoViewModel

private data class ThemePreset(
    val id: String,
    val name: String,
    val mode: String,
    val accent: Int,
    val background: Int,
    val text: Int,
    val cornerRadius: Int,
    val matrix: Boolean,
    val previewBg: Color,
    val previewAccent: Color
)

private val CURATED_PRESETS = listOf(
    ThemePreset(
        id = "eco_default",
        name = "Eco Default",
        mode = "DARK",
        accent = 0xFF34D399.toInt(),
        background = 0,
        text = 0,
        cornerRadius = 16,
        matrix = false,
        previewBg = Color(0xFF0B0F0D),
        previewAccent = Color(0xFF34D399)
    ),
    ThemePreset(
        id = "amber_klasik",
        name = "Amber Tembakau",
        mode = "LIGHT",
        accent = 0xFF8D4E17.toInt(),
        background = 0xFFFFFBF8.toInt(),
        text = 0xFF201A17.toInt(),
        cornerRadius = 16,
        matrix = true,
        previewBg = Color(0xFFFFF3E6),
        previewAccent = Color(0xFF8D4E17)
    ),
    ThemePreset(
        id = "cyber_neon",
        name = "Cyber Neon",
        mode = "CYBER",
        accent = 0xFF06B6D4.toInt(),
        background = 0xFF0F172A.toInt(),
        text = 0xFFF8FAFC.toInt(),
        cornerRadius = 14,
        matrix = true,
        previewBg = Color(0xFF0F172A),
        previewAccent = Color(0xFF06B6D4)
    ),
    ThemePreset(
        id = "oled_dark",
        name = "OLED Hitam",
        mode = "AMOLED",
        accent = 0xFF34D399.toInt(),
        background = 0xFF000000.toInt(),
        text = 0xFFF8FAFC.toInt(),
        cornerRadius = 18,
        matrix = true,
        previewBg = Color(0xFF000000),
        previewAccent = Color(0xFF34D399)
    ),
    ThemePreset(
        id = "samudra",
        name = "Samudra Biru",
        mode = "DARK",
        accent = 0xFF38BDF8.toInt(),
        background = 0xFF0B132B.toInt(),
        text = 0xFFF1F5F9.toInt(),
        cornerRadius = 18,
        matrix = true,
        previewBg = Color(0xFF0B132B),
        previewAccent = Color(0xFF38BDF8)
    ),
    ThemePreset(
        id = "royal_ungu",
        name = "Royal Ungu",
        mode = "DARK",
        accent = 0xFFA855F7.toInt(),
        background = 0xFF1A1025.toInt(),
        text = 0xFFF5F3FF.toInt(),
        cornerRadius = 20,
        matrix = true,
        previewBg = Color(0xFF1A1025),
        previewAccent = Color(0xFFA855F7)
    ),
    ThemePreset(
        id = "mawar_senja",
        name = "Mawar Senja",
        mode = "LIGHT",
        accent = 0xFFE11D48.toInt(),
        background = 0xFFFFF5F7.toInt(),
        text = 0xFF3B0D16.toInt(),
        cornerRadius = 20,
        matrix = false,
        previewBg = Color(0xFFFFF5F7),
        previewAccent = Color(0xFFE11D48)
    )
)

@Composable
fun ThemeCustomizationStudio(viewModel: TobaccoViewModel) {
    val context = LocalContext.current
    val themeMode by viewModel.themeMode.collectAsState()
    val customColor by viewModel.customColor.collectAsState()
    val customBackgroundColor by viewModel.customBackgroundColor.collectAsState()
    val customTextColor by viewModel.customTextColor.collectAsState()
    val fontSizeScale by viewModel.fontSizeScale.collectAsState()
    val paddingScale by viewModel.paddingScale.collectAsState()
    val buttonHeight by viewModel.buttonHeight.collectAsState()
    val cornerRadius by viewModel.cornerRadius.collectAsState()
    val showMatrixBackground by viewModel.showMatrixBackground.collectAsState()

    var showAccentPicker by remember { mutableStateOf(false) }
    var showBgPicker by remember { mutableStateOf(false) }
    var showTextPicker by remember { mutableStateOf(false) }

    val uiConfig = LocalUiConfig.current

    Column(verticalArrangement = Arrangement.spacedBy(14.dp.scaled)) {
        // 1. LIVE MINI PREVIEW CARD
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(cornerRadius.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp.scaled),
                verticalArrangement = Arrangement.spacedBy(10.dp.scaled)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Eco, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                        }
                        Text(
                            "Pratinjau Tema Aktif",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp.scaled,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape((cornerRadius / 2).coerceAtLeast(6).dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = themeMode,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape((cornerRadius * 0.8f).dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp.scaled, vertical = 10.dp.scaled),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Total Kulakan Hari Ini",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp.scaled
                            )
                            Text(
                                "Rp 2.450.000",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp.scaled
                            )
                        }
                        Button(
                            onClick = {},
                            shape = RoundedCornerShape((cornerRadius * 0.7f).coerceAtLeast(6f).dp),
                            modifier = Modifier.heightIn(min = (buttonHeight * 0.72f).dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Contoh Tombol", fontSize = 11.sp.scaled, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. PRESET TEMA SIAP PAKAI
        Text(
            "Preset Tema Cepat",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp.scaled
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            items(CURATED_PRESETS, key = { it.id }) { preset ->
                val isCurrent = themeMode == preset.mode &&
                    customColor == preset.accent &&
                    customBackgroundColor == preset.background
                Surface(
                    modifier = Modifier
                        .width(106.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            viewModel.applyThemePreset(
                                mode = preset.mode,
                                accentColor = preset.accent,
                                backgroundColor = preset.background,
                                textColor = preset.text,
                                cornerRadiusDp = preset.cornerRadius,
                                matrixEnabled = preset.matrix
                            )
                            Toast.makeText(context, "Tema '${preset.name}' diterapkan", Toast.LENGTH_SHORT).show()
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(
                        if (isCurrent) 2.dp else 1.dp,
                        if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(preset.previewBg)
                                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(preset.previewAccent)
                            )
                        }
                        Text(
                            text = preset.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        // 3. MODE TAMPILAN (5 PILIHAN TERMASUK CYBER & AMOLED)
        Text(
            "Mode Dasar Tampilan",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp.scaled
        )
        val modes = listOf(
            Triple("SYSTEM", "Otomatis", Icons.Default.SettingsSuggest),
            Triple("LIGHT", "Terang", Icons.Default.LightMode),
            Triple("DARK", "Gelap", Icons.Default.DarkMode),
            Triple("CYBER", "Cyber", Icons.Default.Bolt),
            Triple("AMOLED", "OLED", Icons.Default.Brightness2)
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            modes.chunked(3).forEach { rowModes ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for ((modeKey, label, icon) in rowModes) {
                        val isSelected = themeMode.equals(modeKey, ignoreCase = true)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.setThemeMode(modeKey) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 9.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    icon,
                                    label,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 10.sp.scaled,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    repeat(3 - rowModes.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        // 4. KUSTOMISASI WARNA (AKSEN, BACKGROUND, TEKS)
        Text(
            "Kustomisasi Warna Lengkap",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp.scaled
        )

        ColorRowButton(
            label = "Warna Aksen Utama",
            subtitle = "Tombol, ikon aktif, badge, highlight",
            icon = Icons.Default.ColorLens,
            swatchColor = if (customColor != 0) Color(customColor) else MaterialTheme.colorScheme.primary,
            onClick = { showAccentPicker = true }
        )

        ColorRowButton(
            label = "Warna Latar Belakang",
            subtitle = if (customBackgroundColor == 0) "Mengikuti mode tema aktif" else "Warna background kustom aktif",
            icon = Icons.Default.FormatColorFill,
            swatchColor = if (customBackgroundColor != 0) Color(customBackgroundColor) else MaterialTheme.colorScheme.background,
            onClick = { showBgPicker = true }
        )

        ColorRowButton(
            label = "Warna Teks Utama",
            subtitle = if (customTextColor == 0) "Otomatis kontras dengan latar" else "Warna teks kustom aktif",
            icon = Icons.Default.FormatColorText,
            swatchColor = if (customTextColor != 0) Color(customTextColor) else MaterialTheme.colorScheme.onBackground,
            onClick = { showTextPicker = true }
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        // 5. SLIDER UKURAN, PADDING, TOMBOL & SUDUT
        UiSliderItem(
            label = "Ukuran Huruf / Font",
            value = fontSizeScale,
            displayValue = "${(fontSizeScale * 100).toInt()}%",
            icon = Icons.Default.TextFields,
            valueRange = 0.8f..1.3f,
            presets = listOf(0.85f to "Kecil", 1.0f to "Normal", 1.2f to "Besar"),
            onValueChange = { viewModel.setFontSizeScale(it) }
        )

        UiSliderItem(
            label = "Kepadatan Jarak / Padding",
            value = paddingScale,
            displayValue = "${(paddingScale * 100).toInt()}%",
            icon = Icons.Default.SpaceDashboard,
            valueRange = 0.8f..1.3f,
            presets = listOf(0.85f to "Rapat", 1.0f to "Standar", 1.2f to "Lega"),
            onValueChange = { viewModel.setPaddingScale(it) }
        )

        UiSliderItem(
            label = "Tinggi Tombol & Kartu",
            value = buttonHeight.toFloat(),
            displayValue = "${buttonHeight}dp",
            icon = Icons.Default.Height,
            valueRange = 42f..68f,
            presets = listOf(46f to "Ringkas", 56f to "Normal", 64f to "Tinggi"),
            onValueChange = { viewModel.setButtonHeight(it.toInt()) }
        )

        UiSliderItem(
            label = "Kebulatan Sudut (Corner Radius)",
            value = cornerRadius.toFloat(),
            displayValue = "${cornerRadius}dp",
            icon = Icons.Default.RoundedCorner,
            valueRange = 0f..28f,
            presets = listOf(4f to "Tegas", 16f to "Standar", 24f to "Bulat"),
            onValueChange = { viewModel.setCornerRadius(it.toInt()) }
        )

        // 6. TOGGLE MATRIX ANIMATION BERANDA
        Surface(
            shape = RoundedCornerShape((uiConfig.cornerRadius * 0.7f).coerceAtLeast(10f).dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp.scaled, vertical = 10.dp.scaled),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Column {
                        Text("Animasi Matrix Beranda", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("Matikan untuk tampilan polos lebih hemat baterai", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Switch(
                    checked = showMatrixBackground,
                    onCheckedChange = { viewModel.setShowMatrixBackground(it) }
                )
            }
        }

        // 7. RESET BUTTON
        OutlinedButton(
            onClick = {
                viewModel.resetUiCustomization()
                Toast.makeText(context, "Semua tema & tampilan dikembalikan ke default", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            shape = RoundedCornerShape((cornerRadius * 0.75f).coerceAtLeast(10f).dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Icon(Icons.Default.RestartAlt, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Reset Semua Pengaturan Tema & UI", fontWeight = FontWeight.Bold)
        }
    }

    if (showAccentPicker) {
        RichRgbColorPickerDialog(
            title = "Pilih Warna Aksen Utama",
            currentColorInt = if (customColor != 0) customColor else MaterialTheme.colorScheme.primary.toArgb(),
            presets = listOf(
                "Emerald" to 0xFF10B981.toInt(),
                "Tembakau" to 0xFF8D4E17.toInt(),
                "Cyber Cyan" to 0xFF06B6D4.toInt(),
                "Samudra" to 0xFF3B82F6.toInt(),
                "Violet" to 0xFF8B5CF6.toInt(),
                "Mawar" to 0xFFE11D48.toInt(),
                "Emas" to 0xFFF59E0B.toInt(),
                "Teal" to 0xFF14B8A6.toInt()
            ),
            allowDefaultReset = true,
            defaultColorInt = 0xFF10B981.toInt(),
            onDismiss = { showAccentPicker = false },
            onColorSelected = {
                viewModel.setCustomColor(it)
                showAccentPicker = false
            }
        )
    }

    if (showBgPicker) {
        RichRgbColorPickerDialog(
            title = "Pilih Warna Latar Belakang",
            currentColorInt = if (customBackgroundColor != 0) customBackgroundColor else MaterialTheme.colorScheme.background.toArgb(),
            presets = listOf(
                "Krem Hangat" to 0xFFFFFBF8.toInt(),
                "Putih Bersih" to 0xFFFFFFFF.toInt(),
                "Mint Lembut" to 0xFFF0FDF4.toInt(),
                "Cyber Slate" to 0xFF0F172A.toInt(),
                "Navy Gelap" to 0xFF0B132B.toInt(),
                "Charcoal" to 0xFF121212.toInt(),
                "Plum Gelap" to 0xFF1A1025.toInt(),
                "OLED Hitam" to 0xFF000000.toInt()
            ),
            allowDefaultReset = true,
            defaultColorInt = 0,
            onDismiss = { showBgPicker = false },
            onColorSelected = {
                viewModel.setCustomBackgroundColor(it)
                showBgPicker = false
            }
        )
    }

    if (showTextPicker) {
        RichRgbColorPickerDialog(
            title = "Pilih Warna Teks Utama",
            currentColorInt = if (customTextColor != 0) customTextColor else MaterialTheme.colorScheme.onBackground.toArgb(),
            presets = listOf(
                "Hitam Tegas" to 0xFF111827.toInt(),
                "Cokelat Tua" to 0xFF201A17.toInt(),
                "Putih Cerah" to 0xFFF8FAFC.toInt(),
                "Abu Perak" to 0xFFCBD5E1.toInt(),
                "Emas Amber" to 0xFFFBBF24.toInt(),
                "Cyan Neon" to 0xFF22D3EE.toInt(),
                "Hijau Mint" to 0xFF6EE7B7.toInt(),
                "Merah Muda" to 0xFFFDA4AF.toInt()
            ),
            allowDefaultReset = true,
            defaultColorInt = 0,
            onDismiss = { showTextPicker = false },
            onColorSelected = {
                viewModel.setCustomTextColor(it)
                showTextPicker = false
            }
        )
    }
}

@Composable
private fun ColorRowButton(
    label: String,
    subtitle: String,
    icon: ImageVector,
    swatchColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(swatchColor)
                    .border(2.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), CircleShape)
            )
        }
    }
}

@Composable
private fun RichRgbColorPickerDialog(
    title: String,
    currentColorInt: Int,
    presets: List<Pair<String, Int>>,
    allowDefaultReset: Boolean,
    defaultColorInt: Int,
    onDismiss: () -> Unit,
    onColorSelected: (Int) -> Unit
) {
    val initialColor = Color(currentColorInt)
    var red by remember { mutableFloatStateOf(initialColor.red) }
    var green by remember { mutableFloatStateOf(initialColor.green) }
    var blue by remember { mutableFloatStateOf(initialColor.blue) }
    val previewColor = Color(red, green, blue, 1f)
    val hexString = remember(red, green, blue) {
        String.format(
            "#%02X%02X%02X",
            (red * 255).toInt().coerceIn(0, 255),
            (green * 255).toInt().coerceIn(0, 255),
            (blue * 255).toInt().coerceIn(0, 255)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Live Color Preview + Hex
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(previewColor)
                            .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    )
                    Column {
                        Text("Warna Terpilih", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(hexString, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    }
                }

                // Preset Swatches
                presets.chunked(4).forEach { rowItems ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        rowItems.forEach { (name, colorInt) ->
                            val c = Color(colorInt)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        red = c.red
                                        green = c.green
                                        blue = c.blue
                                    }
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(name, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, maxLines = 1)
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // RGB Sliders
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Merah (${(red * 255).toInt()})", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = red,
                        onValueChange = { red = it },
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFEF4444), activeTrackColor = Color(0xFFEF4444))
                    )
                    Text("Hijau (${(green * 255).toInt()})", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = green,
                        onValueChange = { green = it },
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF10B981), activeTrackColor = Color(0xFF10B981))
                    )
                    Text("Biru (${(blue * 255).toInt()})", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = blue,
                        onValueChange = { blue = it },
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF3B82F6), activeTrackColor = Color(0xFF3B82F6))
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onColorSelected(previewColor.toArgb()) }) {
                Text("Terapkan")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (allowDefaultReset) {
                    TextButton(onClick = { onColorSelected(defaultColorInt) }) {
                        Text("Otomatis")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Batal")
                }
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
