# 📊 PRESENTASI PROYEK: UANGKU (Smart Financial Tracker & AI Receipt Scanner)

> **Slogan:** *"Kelola Keuangan Tanpa Batas, Transparan & Privat"*  
> **Platform:** Android (Native Jetpack Compose, Material Design 3)  
> **Lisensi & Basis:** Offline-First, Privacy-Centric, AI-Powered  

---

## 📑 DAFTAR ISI PRESENTASI
1. [Executive Summary](#1-executive-summary)
2. [Latar Belakang & Masalah (Problem Statement)](#2-latar-belakang--masalah)
3. [Solusi yang Ditawarkan (The Solution)](#3-solusi-yang-ditawarkan)
4. [Target Pengguna (Target Audience)](#4-target-pengguna)
5. [Fitur Unggulan (Core Features)](#5-fitur-unggulan)
6. [Teknologi & Arsitektur Sistem (Tech Stack)](#6-teknologi--arsitektur-sistem)
7. [Skema Desain & Tampilan UI (Design System)](#7-skema-desain--tampilan-ui)
8. [Alur Demo Aplikasi (Live Demo Script)](#8-alur-demo-aplikasi)
9. [Keunggulan Kompetitif (Competitive Advantages)](#9-keunggulan-kompetitif)
10. [Rencana Masa Depan (Future Roadmap)](#10-rencana-masa-depan)
11. [Penutup & Sesi Tanya Jawab (Q&A)](#11-penutup--qna)

---

## 1. EXECUTIVE SUMMARY

**UangKu** adalah aplikasi pencatatan dan manajemen keuangan pribadi modern untuk perangkat Android yang dirancang dengan pendekatan **Offline-First**, mengutamakan **kecepatan transaksi**, **privasi data penuh**, serta kemudahan pencatatan otomatis menggunakan teknologi **AI Vision OCR**.

Aplikasi ini menyatukan pemantauan multi-dompet catatan, perencanaan anggaran bulanan (*budgeting*), target tabungan fleksibel, transaksi rutin, sistem pemulihan tempat sampah (*soft-delete*), hingga ekspor dan pencadangan data lokal maupun cloud.

---

## 2. LATAR BELAKANG & MASALAH

### 🔴 Masalah yang Dihadapi Pengguna:
1. **Malas Mencatat Manual:** Pengguna seringkali lupa mencatat pengeluaran harian karena form pencatatan di aplikasi konvensional lambat dan membosankan.
2. **Struk Belanja Menumpuk:** Struk fisik minimarket, restoran, dan SPBU sering hilang atau tintanya pudar sebelum sempat dicatat.
3. **Kekhawatiran Privasi Data:** Banyak aplikasi finansial yang mewajibkan sinkronisasi kredensial internet banking atau menjual data kebiasaan belanja ke pihak ketiga.
4. **Saldo Tercampur Aduk:** Sulit memisahkan antara uang operasional harian, tabungan impian, dan dana cadangan di berbagai rekening bank maupun e-wallet.
5. **Risiko Kehilangan Data:** Salah hapus transaksi dapat merusak kalkulasi buku kas tanpa ada opsi pemulihan (*undo*).

---

## 3. SOLUSI YANG DITAWARKAN

### 🟢 Solusi dari UangKu:
* **⚡ Pindai Struk Instan (AI OCR):** Cukup foto atau unggah struk belanja; AI mengekstrak nominal, toko, tanggal, dan kategori secara otomatis dalam hitungan detik.
* **🔒 100% Privat & Mandiri (Offline-First):** Seluruh database transaksi tersimpan lokal di SQLite perangkat menggunakan Room Database. Data tidak bergantung pada server luar untuk fungsi dasarnya.
* **🎯 Target Tabungan Fleksibel (Dual-Sync Mode):** Pengguna dapat memilih apakah setoran target tabungan memotong saldo kas bersih atau hanya sebagai catatan progres impian mandiri.
* **🛡️ Perlindungan Tempat Sampah & Jejak Audit:** Transaksi yang dihapus masuk ke *Trash Bin* untuk dapat dipulihkan kapan saja, disertai riwayat log audit saat mengedit data.
* **📊 Analisis Finansial Cerdas:** Visualisasi grafik tren pengeluaran bulanan dan distribusi kategori pengeluaran tanpa iklan mengganggu.

---

## 4. TARGET PENGGUNA

1. **Mahasiswa & Pelajar:** Memantau uang saku, biaya kos, serta mengatur tabungan impian (gadget, liburan).
2. **Karyawan & Profesional Muda:** Mengatur *cash flow* gaji bulanan, tagihan rutin (listrik, internet, sewa), dan alokasi rekening multi-bank.
3. **Pelaku Usaha Mikro / Freelancer:** Memisahkan catatan uang kas tunai dan operasional kerja secara rapi tanpa kerumitan software akuntansi enterprise.
4. **Masyarakat Sadar Finansial:** Siapa saja yang menginginkan pencatatan keuangan yang bersih, tanpa iklan, dan menjaga kerahasiaan data pribadi.

---

## 5. FITUR UNGGULAN

| No | Modul Fitur | Deskripsi & Nilai Tambah |
|---|---|---|
| **1** | **Dashboard Beranda Interaktif** | Ringkasan Saldo Bersih Total, Pemasukan, Pengeluaran bulanan, kartu dompet geser, dan shortcut cepat. |
| **2** | **Pindai Struk AI (Gemini OCR)** | Membaca struk fisik dengan kamera/galeri, mengekstrak teks otomatis ke formulir transaksi. Dilengkapi *fallback heuristic* saat offline. |
| **3** | **Pencatatan Multi-Dompet** | Memantau saldo terpisah (Tunai, Rekening Bank, E-Wallet) dan mencatat alokasi transfer antar dompet pembukuan internal. |
| **4** | **Target Tabungan (Savings Goals)** | Membuat target impian dengan progress bar persentase, tanggal target, dan **Saklar Sinkronisasi Saldo (ON/OFF)**. |
| **5** | **Manajemen Anggaran (Budgeting)** | Menetapkan batas pengeluaran per kategori. Sistem memberi peringatan dini saat pengeluaran mendekati 80% dan 100%. |
| **6** | **Transaksi Rutin & Pengingat** | Pencatatan otomatis untuk tagihan berkala (harian, mingguan, bulanan) dengan notifikasi pengingat ke status bar HP. |
| **7** | **Pencarian & Multi-Filter Canggih** | Filter berdasarkan kategori, dompet, rentang tanggal, rentang nominal, urutan terbaru/terbesar, dan histori pencarian. |
| **8** | **Tempat Sampah (Trash Recovery)** | Menggunakan mekanisme *soft-delete*. Transaksi tidak langsung hilang permanen, bisa dipulihkan dengan satu sentuhan. |
| **9** | **Pencadangan & Ekspor Fleksibel** | Ekspor laporan transaksi ke format **CSV / Excel**, ekspor backup data utuh ke format **JSON**, serta integrasi Google Account / Drive snapshot. |
| **10** | **Tema & Kenyamanan Visual** | Mendukung Mode Gelap murni (*Deep Obsidian*) dan Mode Terang (*Crisp Slate*) dengan palet warna Material 3 standar aksesibilitas tinggi. |

---

## 6. TEKNOLOGI & ARSITEKTUR SISTEM

### 🛠️ Tech Stack:
* **Bahasa:** Kotlin 100%
* **Framework UI:** Jetpack Compose (Declarative UI) dengan Material Design 3 (M3)
* **Pola Arsitektur:** Model-View-ViewModel (MVVM) + Repository Pattern + Clean Architecture
* **Penyimpanan Lokal:** Room Database (SQLite Engine) + KSP (Kotlin Symbol Processing)
* **Kecerdasan Buatan:** Google Gemini Vision 1.5/2.0 API via OkHttp3 & Model Heuristic Parser
* **Manajemen Thread:** Kotlin Coroutines (`Dispatchers.IO`, `Dispatchers.Main`) & StateFlow
* **Otentikasi & Cloud:** Firebase Auth & Google Identity Credential Manager
* **Optimasi Rilis:** ProGuard / R8 Shrinking, Obfuscation, dan Resource Optimization

### 🏗️ Diagram Alir Arsitektur:
```
┌────────────────────────────────────────────────────────┐
│                   Jetpack Compose UI                   │
│   (HomeScreen, TransactionsScreen, AnalyticsScreen,    │
│    BudgetsScreen, WalletsScreen, SavingsGoalsScreen)   │
└───────────────────────────▲────────────────────────────┘
                            │ StateFlow / Events
┌───────────────────────────┴────────────────────────────┐
│                    UangKuViewModel                     │
│  (State Management, Business Logic, Validation, Math)  │
└───────────────────────────▲────────────────────────────┘
                            │
┌───────────────────────────┴────────────────────────────┐
│                   UangKuRepository                     │
└─────────────▲────────────────────────────▲─────────────┘
              │                            │
┌─────────────┴──────────────┐ ┌───────────┴─────────────┐
│    Room Database (Local)   │ │  Network & Cloud (Ext)  │
│  - AppDatabase / AppDao    │ │  - Gemini Vision API    │
│  - Entities & Migrations   │ │  - Google Credential    │
│  - Fast SQLite Offline     │ │  - JSON Cloud Backup    │
└────────────────────────────┘ └─────────────────────────┘
```

---

## 7. SKEMA DESAIN & TAMPILAN UI

UangKu mengadopsi standar **Material You (M3)** dengan rasio kontras warna tinggi (*WCAG Compliant*):

| Komponen | Mode Gelap (*Dark Mode*) | Mode Terang (*Light Mode*) | Filosofi |
|---|---|---|---|
| **Latar Belakang Utama** | `#090D16` *(Deep Obsidian)* | `#F8FAFC` *(Crisp Slate)* | Nyaman di mata, hemat baterai OLED. |
| **Kartu / Surface** | `#131B2E` *(Slate Navy)* | `#FFFFFF` *(Pure White)* | Hierarki kedalaman elemen yang tegas. |
| **Aksen Utama (Primary)** | `#10B981` *(Emerald Mint)* | `#059669` *(Emerald Forest)* | Melambangkan pertumbuhan finansial positif. |
| **Indikator Pemasukan** | `#22C55E` *(Vibrant Green)* | `#16A34A` *(Forest Green)* | Segar dan memberikan kepastian dana masuk. |
| **Indikator Pengeluaran** | `#F43F5E` *(Coral Crimson)* | `#E11D48` *(Ruby Red)* | Kontras jelas sebagai pengingat pengeluaran. |
| **Catatan Transfer Internal** | `#6366F1` *(Indigo Blue)* | `#4F46E5` *(Royal Indigo)* | Khusus perpindahan antar dompet catatan. |

---

## 8. ALUR DEMO APLIKASI (LIVE DEMO SCRIPT)

Saat mendemonstrasikan aplikasi kepada dewan penguji atau audiens, gunakan urutan langkah berikut:

### Skenario 1: Tinjauan Beranda & Kesehatan Keuangan
1. Buka aplikasi, tunjukkan ringkasan saldo bersih total di bagian atas.
2. Geser kartu dompet (*Tunai, BCA, GoPay*) yang menunjukkan saldo masing-masing.
3. Tunjukkan daftar transaksi terkini dengan ikon kategori warna-warni.

### Skenario 2: Pindai Struk Otomatis (Fitur Wow / AI Showcase)
1. Klik tombol kamera/struk di bagian atas atau menu pencatatan.
2. Pilih foto struk pembayaran (misal struk belanja minimarket / cafe).
3. Tunjukkan bagaimana AI secara otomatis mengisi kolom Toko, Tanggal, Nominal Total, dan Kategori pengeluaran.
4. Klik simpan; saldo dompet terkait langsung terpotong secara instan.

### Skenario 3: Target Tabungan (Pilihan Sinkronisasi Finansial)
1. Buka menu samping -> pilih **Tabungan & Impian**.
2. Jelaskan fitur saklar **"Sinkron dengan Keuangan"** di bagian atas:
   - Jika **ON**: Setoran tabungan otomatis memotong saldo dompet sumber dan tercatat di pengeluaran kas.
   - Jika **OFF**: Setoran tabungan berdiri sendiri (untuk pengguna yang sudah memisahkan uang fisiknya).
3. Lakukan setoran demo untuk melihat persentase progress bar bertambah secara interaktif.

### Skenario 4: Analisis & Penganggaran
1. Buka tab **Statistik**, perlihatkan diagram lingkaran pengeluaran per kategori.
2. Buka tab **Anggaran**, perlihatkan kategori yang melebihi batas atau aman dengan visual bar progress.

### Skenario 5: Keamanan & Pemulihan (Trash Bin)
1. Tekan lama pada salah satu transaksi lalu pilih "Pindahkan ke Tempat Sampah".
2. Tunjukkan bahwa saldo dompet kembali terkoreksi.
3. Buka menu samping -> **Tempat Sampah** -> klik "Pulihkan Transaksi". Transaksi kembali ke buku kas tanpa data yang rusak.

---

## 9. KEUNGGULAN KOMPETITIF

| Parameter | Aplikasi Finansial Biasa | UangKu |
|---|---|---|
| **Iklan / Ads** | Banyak iklan pop-up & banner mengganggu | **Bebas Iklan 100%** |
| **Ketergantungan Internet** | Wajib internet (server lambat jika sinyal lemah) | **Offline-First (Cepat & Responsif)** |
| **Privasi Data** | Data tersimpan di server pihak ketiga | **Tersimpan di Perangkat Sendiri** |
| **Input Struk** | Harus ketik manual satu per satu | **Scan OCR Berbasis AI Otomatis** |
| **Salah Hapus Data** | Langsung hilang permanen | **Ada Fitur Tempat Sampah (Soft-Delete)** |
| **Ukuran APK** | Berat (banyak library analitik) | **Ringan & Optimal (R8 Obfuscated)** |

---

## 10. RENCANA MASA DEPAN (FUTURE ROADMAP)

* **Fase 1 (Next Minor Release):**
  - Widget interaktif di Home Screen Android untuk input kilat pengeluaran.
  - Ekspor laporan berformat grafik PDF berlogo resmi.
* **Fase 2:**
  - Deteksi transaksi otomatis melalui izin parsing notifikasi SMS perbankan & e-wallet lokal.
  - Multi-mata uang (*multi-currency*) dengan kurs konversi otomatis.
* **Fase 3:**
  - AI Financial Advisor: Analisis pengeluaran mingguan dengan saran penghematan berbasis Gemini.

---

## 11. PENUTUP & SESI TANYA JAWAB (Q&A)

### Pertanyaan yang Sering Muncul:
1. **Q: Apakah aplikasi ini bisa mentransfer uang antar rekening bank sungguhan?**  
   *A:* Tidak. UangKu adalah aplikasi *personal finance tracker & bookkeeping* (pencatatan dan pemantauan keuangan pribadi). Fitur dompet dan perpindahan dana hanya bersifat pembukuan catatan internal mandiri agar pengguna mengetahui posisi uangnya.
2. **Q: Apakah struk bisa dipindai saat offline?**  
   *A:* Ya. UangKu dilengkapi *smart fallback heuristic parser* yang tetap dapat mengenali pola transaksi dasar struk meskipun pengguna sedang tidak terhubung ke internet.
3. **Q: Bagaimana jika pengguna berganti handphone?**  
   *A:* Pengguna dapat mengekspor file cadangan berekstensi `.json` atau mencadangkan snapshot ke Google Account mereka, lalu merestorasi data tersebut di HP baru dalam hitungan detik.

---

*Disiapkan secara profesional untuk presentasi proyek aplikasi **UangKu**.*
