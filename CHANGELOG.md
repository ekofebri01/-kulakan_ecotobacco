# Changelog Eco Tobacco

## v4.6 (Build 18) — 23 Nov 2026
**Pembaruan GitHub Updater & Scanner Nota Modular**

### What's New
- **In-App GitHub Auto-Update lebih andal**: Versi pemeriksaan otomatis kini tersinkronisasi langsung dengan `BuildConfig.VERSION_NAME`, tanpa cek ganda saat startup, serta menghormati toggle *Auto-cek saat aplikasi dibuka*.
- **Validasi Release Asset & Izin Instalasi**: Memastikan GitHub Release memiliki file `.apk` sebelum mengunduh (jika belum ada, tersedia tombol untuk membuka halaman rilis di browser) dan menjaga dialog siap pasang saat izin *Sumber Tidak Dikenal* dibuka.
- **OCR & Receipt Parser Modular**: Pemisahan modul scanner (`OcrEngine`, `ReceiptParser`, `ReceiptScannerManager`) dengan dukungan nota kasir 2-baris & 1-baris Indonesia, normalisasi typo OCR, serta validasi `qty × harga satuan ≈ total`.
- **What's New & Changelog In-App**: Riwayat pembaruan kini dapat dibaca langsung dari menu Pengaturan.

---

## v4.5 (Build 17)
**Peningkatan LocalScanner & Pembersih Cache APK**

- Membaca format harga Rupiah Indonesia dan mendeteksi jumlah serta satuan barang.
- Mengabaikan baris total/pembayaran dan memanfaatkan posisi bounding box teks OCR.
- Menambahkan penanda `needsReview` untuk item hasil scan yang meragukan.
- Pembersihan otomatis file installer APK lama di cache agar penyimpanan tetap lega.
