# RP Chat (Native Kotlin)

Aplikasi roleplay chat gaya Telegram — **native Kotlin Android**, bukan WebView/Capacitor.

## Fitur
- Header atas = profil kamu sendiri (foto + nama), tap untuk edit (termasuk bio)
- Tombol **+** → pilih **Grup baru** (segera hadir) atau **Kontak baru**, plus daftar kontak yang
  sudah pernah dibuat (hapus obrolan tidak menghapus kontak, tinggal pilih lagi dari sini)
- Avatar tampil **bulat sempurna** (bukan kotak), ada area geser + cubit-zoom saat upload foto
- Bio karakter tampil di bawah nama saat buka chat, dengan font berbeda (italic)
- Bubble menampilkan avatar pengirim (kamu di kanan, karakter di kiri)
- **Reply**: geser bubble mana pun; nama pengirim asli ikut tampil di kutipan reply
- Tekan-tahan bubble → Hapus / Edit teks / Beri reaksi emoji
- Input bar: emoji tetap di kiri, tombol lampiran (foto/video/audio) di kanan berubah jadi
  ikon kirim **←** **→** saat mulai mengetik atau ada lampiran
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
