# Eco Tobacco — Manajemen Kulakan & Stok Tembakau

Aplikasi Android berbasis **Kotlin** dan **Jetpack Compose (Material 3)** untuk mencatat kulakan tembakau, memindai nota belanja secara otomatis menggunakan OCR, mengelola daftar produk & harga modal, memantau laporan pembelian, serta melakukan sinkronisasi cloud & backup perangkat.

---

## Fitur Utama

- **Input Kulakan Multi-Item**: Catat banyak barang sekaligus dalam satu keranjang transaksi, lengkap dengan kolom kuantitas & harga modal yang ringkas serta dukungan pecahan **0,5** dan tombol cepat `±0,5`.
- **AI Scanner Nota (Kamera Langsung & Galeri)**: Pindai struk fisik secara instan lewat kamera scanner otomatis atau pilih dari **Galeri HP** (Android Photo Picker), lengkap dengan pemandu crop visual, deteksi dan editor **Tanggal Nota**, serta penyelarasan satuan barang otomatis dengan database produk master.
- **Master Produk & Harga**: Kelola katalog tembakau, sub-kategori, satuan kustom (`kg`, `ons`, `pcs`, `pack`, `slop`, `bal`, dll.), harga beli modal, dan harga jual dengan kalkulasi margin otomatis.
- **Perencanaan Kulakan (*Procurement Planning*)**: Susun rencana belanja stok berikutnya dan konversi ke transaksi kulakan.
- **Laporan & Analitik**: Ringkasan pengeluaran bulanan, grafik tren kulakan, filter riwayat transaksi, dan ekspor/impor CSV.
- **Backup Perangkat & Cloud Sync Permanen**: Pencadangan otomatis ke perangkat serta sinkronisasi Firebase Firestore dengan persistensi sesi login akun yang tetap aktif saat aplikasi ditutup dan dibuka kembali.
- **Widget Layar Beranda**: Widget Android untuk melihat ringkasan dan akses cepat *Input Kulakan* maupun *Rencana Belanja*.
- **GitHub In-App Auto-Update & What's New**: Pemeriksaan rilis baru langsung dari GitHub Releases, pembersihan cache APK otomatis, serta dialog **What's New & Changelog** di dalam aplikasi.

---

## Teknologi yang Digunakan

- **Bahasa & UI**: Kotlin, Jetpack Compose, Material Design 3
- **Arsitektur**: MVVM + Repository Pattern (`TobaccoViewModel`, `TobaccoRepository`)
- **Database Lokal**: Room Database (`KSP`)
- **OCR & Scanner**: Google ML Kit Text Recognition & Document Scanner
- **Cloud & Auth**: Firebase Firestore, Firebase Auth, Jetpack Credential Manager (Google Sign-In)
- **Networking**: OkHttp & Moshi
- **Image Loading**: Coil Compose

---

## Build & Pengujian Lokal

Gunakan Gradle CLI untuk membangun APK atau menjalankan unit test lokal:

```bash
# Jalankan unit test
gradle :app:testDebugUnitTest

# Build APK Release
gradle :app:assembleRelease
```

---

## Catatan Rilis

Lihat daftar lengkap pembaruan dan catatan versi di [CHANGELOG.md](CHANGELOG.md) atau melalui menu **Pengaturan → Eco Tobacco (What's New & Changelog)** di dalam aplikasi.
