# Changelog Eco Tobacco

## v4.7 (Build 19) — 25 Nov 2026
**Rombak Input Kulakan Multi-Item & Scanner Nota Galeri**

### What's New
- **Input Kulakan Banyak Barang Sekaligus**: Rombak total form input manual menjadi satu kartu terpadu dengan daftar keranjang (*multi-item*) sebelum disimpan dalam satu transaksi.
- **Kuantitas & Harga Ringkas + Dukungan 0,5**: Kolom jumlah dan harga modal kini tersusun berdampingan lebih compact dengan tombol cepat pecahan `0,5` dan pengatur `±0,5`.
- **Scan Nota dari Galeri HP**: Tambahan tombol **Galeri** (Android Photo Picker tanpa izin penyimpanan) di layar AI Scanner Nota untuk memindai foto atau screenshot struk yang tersimpan di perangkat.
- **Pemandu Crop Visual & Akurasi OCR**: Overlay garis pemandu crop untuk menyejajarkan nota sebelum dipindai serta toleransi pembacaan mata uang struk thermal (`Rd`, `Ro`, `Rb`).
- **Popup Otomatis What's New**: Menampilkan sorotan fitur baru secara otomatis saat membuka versi terbaru, plus akses riwayat lengkap di Pengaturan.

---

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
