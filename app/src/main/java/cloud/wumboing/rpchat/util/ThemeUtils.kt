package cloud.wumboing.rpchat.util

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable

object ThemeUtils {

    /** key -> (nama family sistem Android, label tampilan) — tanpa file font tambahan, APK tetap kecil. */
    val FONT_OPTIONS = listOf(
        "default" to "Default",
        "sans-serif-light" to "Light",
        "sans-serif-medium" to "Medium",
        "sans-serif-black" to "Black",
        "sans-serif-condensed" to "Condensed",
        "serif" to "Serif",
        "monospace" to "Monospace",
        "cursive" to "Cursive",
        "casual" to "Casual"
    )

    fun typefaceFor(fontFamily: String): Typeface = when (fontFamily) {
        "default" -> Typeface.DEFAULT
        "serif" -> Typeface.SERIF
        "monospace" -> Typeface.MONOSPACE
        else -> try {
            Typeface.create(fontFamily, Typeface.NORMAL)
        } catch (e: Exception) {
            Typeface.DEFAULT
        }
    }

    fun bubbleDrawable(context: Context, color: Int, radiusDp: Float = 16f): GradientDrawable {
        val radiusPx = radiusDp * context.resources.displayMetrics.density
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radiusPx
        }
    }
}
