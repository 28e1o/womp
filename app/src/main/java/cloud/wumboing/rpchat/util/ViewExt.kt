package cloud.wumboing.rpchat.util

import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider

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
