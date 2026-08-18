# RP Chat (Native Kotlin)

Aplikasi roleplay chat gaya Telegram — **native Kotlin Android**, bukan WebView/Capacitor.

## Fitur
- Header atas = profil kamu sendiri (foto + nama). Ikon **⚙️ pengaturan** di kanan header untuk
  ubah foto/nama/bio kamu, font & ukuran font chat, warna latar chat, dan warna bubble
- Tombol **+** membuka layar penuh "Obrolan Baru" (bukan popup) — bisa cari kontak, buat kontak
  baru, atau pilih kontak yang sudah pernah dibuat (hapus obrolan tidak menghapus kontak)
- Avatar tampil **bulat sempurna** (bukan kotak), ada area geser + cubit-zoom saat upload foto
- Bio karakter tampil di bawah nama saat buka chat, font normal (bukan italic)
- Bubble menampilkan avatar pengirim (kamu di kanan, karakter di kiri)
- **Reply**: geser bubble mana pun; nama pengirim asli + isi pesan (2 baris) ikut tampil di kutipan
- Tekan-tahan bubble → Hapus / Edit teks / Beri reaksi emoji (badge reaksi tidak lagi menutup jam)
- Input bar: emoji tetap di kiri, tombol lampiran (foto/video/audio) di kanan berubah jadi
  ikon kirim **←** **→** saat mulai mengetik atau ada lampiran
- Garis pembatas tipis di bawah setiap header supaya tidak menyatu dengan konten di bawahnya
- Data disimpan lokal (JSON file di internal storage) — tanpa Room/Gson supaya APK kecil
- Build APK di-**minify** (R8 + shrinkResources) lewat GitHub Actions

## Cara pakai
1. Upload semua isi folder ini ke repo GitHub baru (root repo = folder ini).
2. Buka tab **Actions** di repo, workflow "Build APK" akan otomatis jalan tiap push ke `main`
   (atau jalankan manual lewat "Run workflow").
3. Setelah selesai:
   - APK bisa diunduh dari tab **Actions → run terakhir → Artifacts → rp-chat-release-apk**, atau
   - dari tab **Releases** (dibuat otomatis tiap push ke `main`).
4. Unduh APK ke HP Android, install (aktifkan "izinkan sumber tidak dikenal" jika diminta).

## Buka & edit di Android Studio (opsional)
Buka folder ini sebagai project — Android Studio akan otomatis membuat `gradlew` +
`gradle-wrapper.jar` yang tidak ikut di-commit (lihat `.gitignore`).

## Catatan ukuran APK
- `minifyEnabled true` + `shrinkResources true` di `app/build.gradle.kts` menghapus kode & resource
  yang tidak terpakai.
- Tidak ada dependency berat (Glide, Room, Retrofit, dll) — hanya AndroidX inti,
  jadi APK release seharusnya beberapa MB saja.

## Catatan signing
Keystore release di-generate otomatis oleh workflow (bukan disimpan di repo) supaya APK
tetap bisa langsung diinstall. Ini cukup untuk pemakaian pribadi/testing, **bukan** untuk
publish ke Play Store (untuk itu perlu keystore permanen tersendiri).
