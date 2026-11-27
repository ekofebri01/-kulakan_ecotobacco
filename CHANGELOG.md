# Changelog Eco Tobacco

## v5.0 (Build 22) — 27 Nov 2026
**Perombakan UI Beranda & Input Kulakan, serta Dialog Tambah Produk 1 Halaman**

### What's New
- **Desain Baru Beranda Hijau Mint**: Tampilan Beranda dirombak lebih bersih dan modern dengan kartu total kulakan bulanan zamrud, statistik transaksi & varian produk, kartu status Cloud Sync, Navigasi Cepat ringkas (*Input Kulakan* & *Rencana Baru* tanpa kartu ganda), serta bilah navigasi bawah mengambang dengan tombol Pindai di tengah.
- **Tampilan Baru Input Kulakan**: Halaman Input Kulakan kini tampil lebih rapi dengan kartu Scan Nota Otomatis OCR (Pindai & Galeri), ringkasan nota langsung, chip pengepul tersusun rapi, pilihan satuan unit cepat, dukungan pecahan `0,5`, serta bilah total & simpan di bagian bawah.
- **Tambah Produk Baru 1 Halaman Tanpa Scroll**: Dialog tambah produk di menu Database kini lebih lebar dan ringkas sehingga seluruh kolom input, chip sub kategori, estimasi laba, satuan beli/jual, dan tombol Simpan tampil lengkap dalam satu layar.
- **Bagikan Rencana Kulakan Lebih Ringkas**: Menghilangkan pemilih sub kategori pada dialog bagikan rencana kulakan agar daftar belanja lebih cepat disalin dan dibagikan.

---

## v4.9 (Build 21) — 26 Nov 2026
**Kustomisasi Tema Lengkap, Pengaturan Slide, Updater In-App Beranimasi, & Perbaikan Cloud Backup**

### What's New
- **Kustomisasi Tema Lengkap & Pratinjau Langsung**: Tersedia 7 preset tema pilihan, 5 mode tampilan (termasuk *Cyber* & *AMOLED*), pemilih warna RGB/Hex untuk warna aksen, latar belakang, dan teks, serta slider ukuran font, padding, tinggi tombol, radius sudut, dan sakelar latar *Matrix* di layar utama.
- **Jendela Pengaturan Slide ke Kiri**: Layar Pengaturan kini muncul sebagai panel mengambang yang bergeser dari kanan ke kiri lengkap dengan pratinjau layar utama di balik layar dan gestur geser kanan untuk menutup.
- **Perbaikan Logo Aplikasi & Layar Splash**: Ikon aplikasi adaptif dan logo layar splash diperbarui agar tampil bersih, proporsional, dan menyatu tanpa bingkai latar hijau.
- **Unduh & Instalasi Update In-App Beranimasi**: Proses pembaruan aplikasi kini berjalan penuh di dalam aplikasi, mulai dari animasi unduh APK lengkap dengan persentase dan kecepatan unduh hingga pemasangan langsung beranimasi via *PackageInstaller*.
- **Perbaikan Daftar & Hapus Backup Cloud**: Menghilangkan duplikasi entri pada pemilih backup cloud serta memperbaiki fungsi hapus per file dan hapus semua agar bersih di cloud maupun penyimpanan lokal.
- **Rekap Kulakan Urut Terbaru & Filter Default**: Daftar riwayat kulakan kini otomatis mengurutkan tanggal terbaru di paling atas, ditambah pengaturan tampilan periode default (*Harian*, *Mingguan*, *Bulanan*, *Tahunan*, atau *All Time*).
- **Simpan Satuan Baru di Edit Transaksi**: Satuan baru yang ditambahkan saat mengedit transaksi satuan maupun edit massal kini otomatis tersimpan ke daftar satuan dan basis data.

---

## v4.8 (Build 20) — 26 Nov 2026
**Penyelarasan Satuan Database, Tanggal Nota, Kamera Cepat, & Persistensi Login Cloud**

### What's New
- **Penyelarasan Satuan Database Hasil Scan**: Setiap barang hasil ekstraksi nota otomatis mencocokkan satuan dengan produk di database (lengkap dengan badge status DB), serta menyediakan input satuan dan tombol chip cepat (*pack*, *slop*, *bal*, *kg*, *ons*, *pcs*) pada mode edit agar database tidak tertimpa nilai default.
- **Deteksi & Tampilan Tanggal Nota**: Dialog *Hasil Ekstraksi Nota* kini menampilkan baris **Tanggal Nota** lengkap dengan badge *Terdeteksi / Hari Ini* dan opsi pemilih tanggal (*DatePickerDialog*) interaktif sebelum transaksi disimpan.
- **Pindai Langsung Buka Kamera**: Mengetuk tombol *Pindai* di layar Input Kulakan kini langsung membuka kamera scanner nota tanpa melewati halaman perantara, dengan pemrosesan OCR otomatis saat gambar tertangkap.
- **Akses Galeri 1-Klik**: Tersedia tombol Galeri cepat di sebelah tombol Pindai dan di dalam scanner untuk memilih foto nota dari HP.
- **Persistensi Login Cloud & Inisialisasi Firebase**: Sesi login akun Cloud disimpan permanen di penyimpanan lokal (`SharedPreferences`), dan `FirebaseApp` diinisialisasi sejak aplikasi berjalan (`Application.onCreate`), sehingga akun tetap aktif dan tidak logout saat aplikasi ditutup lalu dibuka kembali.

---

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
