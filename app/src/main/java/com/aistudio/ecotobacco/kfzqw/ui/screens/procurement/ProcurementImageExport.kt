package com.aistudio.ecotobacco.kfzqw.ui.screens.procurement

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import com.aistudio.ecotobacco.kfzqw.data.local.entities.ProcurementPlanWithItems
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun sharePlanAsImage(context: Context, planWithItems: ProcurementPlanWithItems) {
    try {
        val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).apply {
            maximumFractionDigits = 0
        }
        val dateStr = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.forLanguageTag("id-ID")).format(Date(planWithItems.plan.date))
        val supplier = planWithItems.plan.supplierName.ifBlank { "TANPA SUPPLIER" }
        
        // Urutkan item berdasarkan nama secara alfabetis A-Z
        val sortedItems = planWithItems.items.sortedBy { it.productName.lowercase(Locale.ROOT) }

        val width = 720
        val padding = 40f
        val contentWidth = width - (padding * 2)

        // Hitung estimasi tinggi kanvas struk
        val baseHeight = 420
        val itemHeight = 90
        val totalHeight = baseHeight + (sortedItems.size * itemHeight)

        val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background Putih
        canvas.drawColor(AndroidColor.WHITE)

        // Paints
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(46, 125, 50) // #2E7D32 Eco Green
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val subBrandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(100, 116, 139)
            textSize = 20f
            textAlign = Paint.Align.CENTER
        }

        val textBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(30, 41, 59)
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val textRegularPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(71, 85, 105)
            textSize = 20f
        }

        val textRightBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(30, 41, 59)
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        val textRightGreen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(46, 125, 50)
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(203, 213, 225)
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }

        var y = 60f

        // 1. Header
        canvas.drawText("ECO TOBACCO", width / 2f, y, brandPaint)
        y += 32f
        canvas.drawText("RENCANA KULAKAN TEMBAKAU", width / 2f, y, subBrandPaint)
        y += 30f

        // Divider
        canvas.drawLine(padding, y, width - padding, y, dividerPaint)
        y += 36f

        // 2. Info Tanggal & Supplier
        canvas.drawText("TANGGAL", padding, y, textRegularPaint)
        canvas.drawText(dateStr, width - padding, y, textRightBold)
        y += 36f

        canvas.drawText("SUPPLIER", padding, y, textRegularPaint)
        canvas.drawText(supplier.uppercase(Locale.ROOT), width - padding, y, textRightBold)
        y += 30f

        // Divider
        canvas.drawLine(padding, y, width - padding, y, dividerPaint)
        y += 40f

        // 3. Item List (Sudah Terurut)
        var grandTotal = 0.0
        sortedItems.forEachIndexed { index, item ->
            val subTotal = item.targetQuantity * item.estimatedUnitPrice
            grandTotal += subTotal

            val qtyStr = if (item.targetQuantity % 1.0 == 0.0) item.targetQuantity.toInt().toString() else item.targetQuantity.toString()
            val numStr = "${index + 1}. "

            // Item Name
            canvas.drawText(numStr + item.productName, padding, y, textBoldPaint)
            // Subtotal Item
            canvas.drawText(formatter.format(subTotal).replace("Rp", "Rp "), width - padding, y, textRightBold)
            y += 30f

            // Qty x Price
            val qtyDetail = "$qtyStr ${item.unit} × ${formatter.format(item.estimatedUnitPrice).replace("Rp", "Rp ")}"
            canvas.drawText("    $qtyDetail", padding, y, textRegularPaint)
            y += 44f
        }

        // Divider
        y -= 10f
        canvas.drawLine(padding, y, width - padding, y, dividerPaint)
        y += 44f

        // 4. Grand Total
        canvas.drawText("TOTAL ESTIMASI", padding, y, textBoldPaint)
        canvas.drawText(formatter.format(grandTotal).replace("Rp", "Rp "), width - padding, y, textRightGreen)
        y += 50f

        // 5. Footer
        canvas.drawText("Dibuat otomatis oleh Eco Tobacco Apps", width / 2f, y, subBrandPaint)

        // Save Bitmap to Cache
        val cachePath = File(context.cacheDir, "images")
        if (!cachePath.exists()) {
            cachePath.mkdirs()
        }
        val file = File(cachePath, "nota_kulakan_${planWithItems.plan.id}_${System.currentTimeMillis()}.jpg")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
        stream.flush()
        stream.close()

        // Content URI FileProvider
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "Rencana Kulakan - $supplier")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooser = Intent.createChooser(shareIntent, "Bagikan Nota Kulakan").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(chooser)
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Gagal membagikan gambar: ${e.localizedMessage ?: "Terjadi kesalahan"}", Toast.LENGTH_LONG).show()
    }
}
