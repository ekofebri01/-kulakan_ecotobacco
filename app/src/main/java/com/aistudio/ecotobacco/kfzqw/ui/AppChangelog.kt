package com.aistudio.ecotobacco.kfzqw.ui

data class ChangelogRelease(
    val version: String,
    val buildNumber: Int,
    val date: String,
    val headline: String,
    val highlights: List<String>,
    val changes: List<String>
)

object AppChangelog {
    val releases: List<ChangelogRelease> = listOf(
        ChangelogRelease(
            version = "4.6",
            buildNumber = 18,
            date = "23 Nov 2026",
            headline = "Pembaruan GitHub Updater & Scanner Nota Modular",
            highlights = listOf(
                "In-app GitHub Auto-Update lebih akurat dan aman",
                "Pemisahan modul OCR/Scanner & peningkatan akurasi parser nota",
                "Halaman What's New & Changelog langsung di Pengaturan"
            ),
            changes = listOf(
                "Sinkronisasi cek versi otomatis langsung ke BuildConfig.VERSION_NAME agar tidak salah deteksi versi.",
                "Menghapus pengecekan update ganda saat aplikasi dibuka dan mematuhi toggle Auto-Cek.",
                "Validasi lampiran .apk pada GitHub Release agar tidak mengunduh halaman HTML secara keliru.",
                "Dialog instalasi tetap siaga bila pengguna perlu mengaktifkan izin Sumber Tidak Dikenal.",
                "Modularisasi scanner nota ke paket scanner/ (OcrEngine, ReceiptParser, ReceiptScannerManager) dengan dukungan nota kasir 2-baris, normalisasi typo OCR, dan validasi matematika."
            )
        ),
        ChangelogRelease(
            version = "4.5",
            buildNumber = 17,
            date = "2026",
            headline = "Peningkatan LocalScanner & Pembersih Cache APK",
            highlights = listOf(
                "Pembacaan format harga Rupiah Indonesia & satuan barang",
                "Auto-cleanup file installer APK lama"
            ),
            changes = listOf(
                "Meningkatkan LocalScanner untuk membaca format harga Indonesia dan mendeteksi jumlah serta satuan barang.",
                "Mengabaikan baris total/pembayaran dan memanfaatkan koordinat posisi teks OCR.",
                "Menambahkan penanda needsReview pada item scan yang meragukan.",
                "Menambahkan pembersih otomatis file APK sisa pembaruan di cache internal."
            )
        )
    )

    val latest: ChangelogRelease
        get() = releases.first()
}
