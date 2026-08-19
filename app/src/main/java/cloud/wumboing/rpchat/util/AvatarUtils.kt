package cloud.wumboing.rpchat.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import kotlin.math.abs

/**
 * Avatar inisial (huruf depan nama + latar warna) dipakai saat karakter/profil
 * belum punya foto. Warna ditentukan dari hash id/nama supaya konsisten
 * (tidak berubah-ubah tiap render) tapi tetap terasa acak antar karakter.
 */
object AvatarUtils {

    private val palette = listOf(
        0xFFE57373, 0xFF64B5F6, 0xFF81C784, 0xFFFFB74D, 0xFFBA68C8,
        0xFF4DB6AC, 0xFFF06292, 0xFF9575CD, 0xFFA1887F, 0xFF7986CB,
        0xFF4FC3F7, 0xFFAED581
    ).map { it.toInt() }

    fun colorFor(seed: String): Int {
        val index = abs(seed.hashCode()) % palette.size
        return palette[index]
    }

    fun initialsBitmap(name: String, seed: String, sizePx: Int = 160): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colorFor(seed) }
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, bgPaint)

        val letter = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = sizePx * 0.45f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        val textY = sizePx / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(letter, sizePx / 2f, textY, textPaint)
        return bitmap
    }
}
