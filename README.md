# RP Chat (Native Kotlin)

Aplikasi roleplay chat gaya Telegram — **native Kotlin Android**, bukan WebView/Capacitor.

## Fitur
- Daftar karakter (avatar + nama), tekan **+** untuk menambah
- Bubble chat kiri (karakter) / kanan (diri sendiri)
- Input bar dengan tombol **←** (kirim sebagai karakter) dan **→** (kirim sebagai diri sendiri)
- Tekan-tahan pesan untuk **reply**
- Tekan-tahan karakter di daftar untuk hapus
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
