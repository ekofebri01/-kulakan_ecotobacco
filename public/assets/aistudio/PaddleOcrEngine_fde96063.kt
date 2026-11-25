package com.aistudio.ecotobacco.kfzqw.scanner.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.PointF
import com.aistudio.ecotobacco.kfzqw.scanner.model.OcrLine
import com.aistudio.ecotobacco.kfzqw.scanner.model.OcrResult
import com.paddle.ocr.EngineConfig   // <- cek import via Alt+Enter, package bisa beda
import com.paddle.ocr.OCRResult
import com.paddle.ocr.OpenCVUtils
import com.paddle.ocr.PaddleOCR
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Pengganti OcrEngine (ML Kit) dengan PaddleOCR PP-OCRv5/v6 mobile via ONNX Runtime.
 * Signature processImage() sama persis, jadi pemanggil tinggal tukar object.
 *
 * Pakai sekali di Application.onCreate():  PaddleOcrEngine.init(this)
 */
object PaddleOcrEngine {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()           // create() dan recognize() tidak dijamin thread-safe -> serial
    private var appContext: Context? = null
    private var ocr: PaddleOCR? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun processImage(
        bitmap: Bitmap,
        onSuccess: (OcrResult) -> Unit,
        onError: (Exception) -> Unit
    ) {
        scope.launch {
            try {
                val ctx = appContext
                    ?: error("PaddleOcrEngine.init(context) belum dipanggil")

                // Jangan di-downscale. PaddleOCR butuh ARGB_8888.
                val input = if (bitmap.config == Bitmap.Config.ARGB_8888) bitmap
                else bitmap.copy(Bitmap.Config.ARGB_8888, false)

                val run = mutex.withLock {
                    val engine = ocr ?: run {
                        OpenCVUtils.init(ctx)
                        PaddleOCR.create(ctx, engineConfig = EngineConfig(numThreads = 4))
                    }.also { ocr = it }
                    engine.recognize(input)
                }

                val lines = run.results.map { it.toOcrLine() }
                // fullText disusun per baris visual (bukan urutan block mentah)
                val fullText = ReceiptParser.groupIntoRows(lines)
                    .joinToString("\n") { ReceiptParser.rowText(it) }

                withContext(Dispatchers.Main) { onSuccess(OcrResult(fullText = fullText, lines = lines)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError(e) }
            }
        }
    }

    fun close() {
        scope.launch {
            mutex.withLock {
                try { ocr?.release() } catch (_: Exception) { }
                ocr = null            // create ulang otomatis di scan berikutnya
            }
        }
    }

    // ---------- konversi hasil ----------

    private fun OCRResult.toOcrLine(): OcrLine {
        val xs = ArrayList<Float>()
        val ys = ArrayList<Float>()
        collectPoints(box.points, xs, ys)
        return OcrLine(
            text = text,
            left = xs.minOrNull()?.toInt() ?: 0,
            top = ys.minOrNull()?.toInt() ?: 0,
            right = xs.maxOrNull()?.toInt() ?: 0,
            bottom = ys.maxOrNull()?.toInt() ?: 0,
            confidence = confidence
        )
    }

    /**
     * SATU-SATUNYA titik yang mungkin perlu disesuaikan: tipe `box.points` di SDK tidak
     * didokumentasikan. Fungsi ini menangani bentuk yang paling umum; kalau compiler/IDE
     * menunjukkan tipe lain, tambahkan satu cabang `is` di sini.
     */
    private fun collectPoints(points: Any?, xs: MutableList<Float>, ys: MutableList<Float>) {
        when (points) {
            is Iterable<*> -> points.forEach { collectPoints(it, xs, ys) }
            is Array<*> -> points.forEach { collectPoints(it, xs, ys) }
            is PointF -> { xs += points.x; ys += points.y }
            is Point -> { xs += points.x.toFloat(); ys += points.y.toFloat() }
            is org.opencv.core.Point -> { xs += points.x.toFloat(); ys += points.y.toFloat() }
            is FloatArray -> for (i in 0 until points.size - 1 step 2) { xs += points[i]; ys += points[i + 1] }
            is IntArray -> for (i in 0 until points.size - 1 step 2) { xs += points[i].toFloat(); ys += points[i + 1].toFloat() }
            is DoubleArray -> for (i in 0 until points.size - 1 step 2) { xs += points[i].toFloat(); ys += points[i + 1].toFloat() }
        }
    }
}
