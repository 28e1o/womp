package cloud.wumboing.rpchat.util

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable

object ThemeUtils {

    fun typefaceFor(fontFamily: String): Typeface = when (fontFamily) {
        "serif" -> Typeface.SERIF
        "monospace" -> Typeface.MONOSPACE
        "condensed" -> Typeface.create("sans-serif-condensed", Typeface.NORMAL)
        else -> Typeface.DEFAULT
    }

    fun bubbleDrawable(context: Context, color: Int, radiusDp: Float = 16f): GradientDrawable {
        val radiusPx = radiusDp * context.resources.displayMetrics.density
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radiusPx
        }
    }
}
