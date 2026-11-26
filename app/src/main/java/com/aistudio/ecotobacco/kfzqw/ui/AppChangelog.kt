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
            version = "4.8",
            buildNumber = 20,
            date = "26 Nov 2026",
            headline = "Penyelarasan Satuan Database, Tanggal Nota, & Login Cloud",
            highlights = listOf(
                "Satuan hasil scan otomatis diselaraskan dengan database produk (tanpa tertimpa default)",
                "Dialog hasil scan kini menampilkan dan bisa mengedit Tanggal Nota",
                "Tombol Pindai langsung membuka kamera scanner tanpa halaman perantara",
                "Persistensi sesi login Cloud tetap aktif setelah aplikasi ditutup"
            ),
            changes = listOf(
                "Mencocokkan barang hasil scan secara otomatis dengan database produk agar satuannya langsung tersinkron dan database tidak tertimpa default.",
                "Menambahkan kolom input Satuan serta chip pilihan cepat (pack, slop, bal, kg, dll.) pada mode edit hasil scan.",
                "Menampilkan kolom Tanggal Nota pada dialog Hasil Ekstraksi Nota dengan indikator status dan pemilih tanggal (DatePickerDialog).",
                "Tombol Pindai di Input Kulakan langsung membuka kamera dokumen dan mengekstrak teks otomatis begitu foto diambil.",
                "Menyediakan tombol cepat Galeri langsung di sebelah tombol Pindai.",
                "Menyimpan sesi login akun Cloud secara permanen di SharedPreferences dan menginisialisasi Firebase sejak startup aplikasi agar login tidak hilang saat aplikasi ditutup."
            )
        ),
        ChangelogRelease(
            version = "4.7",
            buildNumber = 19,
            date = "25 Nov 2026",
            headline = "Input Kulakan Multi-Item & Scan Nota dari Galeri",
            highlights = listOf(
                "Input kulakan manual mendukung banyak barang sekaligus (keranjang)",
                "Form kuantitas & harga lebih compact dengan tombol cepat 0,5",
                "Scan nota langsung dari Galeri HP dengan pemandu crop visual",
                "Toleransi OCR mata uang struk thermal (Rp/Rd/Ro/Rb) lebih presisi"
            ),
            changes = listOf(
                "Merombak layar Input Kulakan menjadi satu kartu form terpadu dengan daftar keranjang agar bisa mencatat banyak barang sekaligus.",
                "Menyusun kolom kuantitas dan harga berdampingan secara ringkas serta mendukung pecahan 0,5 dan tombol ±0,5.",
                "Menambahkan fitur Scan dari Galeri menggunakan Android Photo Picker (tanpa izin penyimpanan tambahan).",
                "Menambahkan UI overlay pemandu crop visual pada AI Scanner Nota untuk membantu menyelaraskan struk.",
                "Meningkatkan ReceiptParser agar mengenali salah baca OCR huruf mata uang seperti Rd, Ro, dan Rb.",
                "Menambahkan popup What's New otomatis saat pertama kali membuka pembaruan aplikasi."
            )
        ),
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
