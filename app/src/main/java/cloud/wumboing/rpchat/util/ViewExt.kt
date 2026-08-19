package cloud.wumboing.rpchat.util

import android.graphics.BitmapFactory
import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import java.io.File

/**
 * Membuat ImageView (atau View apa pun) terpotong sempurna jadi lingkaran,
 * bukan cuma background-nya saja. Panggil sekali saat bind/setup.
 */
fun View.clipToCircle() {
    clipToOutline = true
    outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outline.setOval(0, 0, view.width, view.height)
        }
    }
}

/**
 * Muat foto avatar dari path; kalau tidak ada/gagal, otomatis pakai avatar
 * inisial (huruf depan nama + warna latar) supaya tidak pernah kosong.
 */
fun ImageView.loadAvatarOrInitials(path: String?, name: String, seed: String) {
    if (!path.isNullOrEmpty()) {
        val f = File(path)
        if (f.exists()) {
            val bmp = BitmapFactory.decodeFile(path)
            if (bmp != null) {
                setImageBitmap(bmp)
                return
            }
        }
    }
    setImageBitmap(cloud.wumboing.rpchat.util.AvatarUtils.initialsBitmap(name, seed))
}
