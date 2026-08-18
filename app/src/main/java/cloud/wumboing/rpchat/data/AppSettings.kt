package cloud.wumboing.rpchat.data

import org.json.JSONObject

/**
 * fontFamily pakai nama font sistem bawaan Android (default/serif/monospace/sans-serif-condensed)
 * supaya tidak perlu menyertakan file font tambahan (APK tetap kecil).
 */
data class AppSettings(
    var fontFamily: String = "default",
    var fontSizeSp: Float = 15f,
    var chatBackgroundColor: Int = 0xFF0E1621.toInt(),
    var bubbleSelfColor: Int = 0xFF2B5278.toInt(),
    var bubbleOtherColor: Int = 0xFF182533.toInt()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("fontFamily", fontFamily)
        put("fontSizeSp", fontSizeSp.toDouble())
        put("chatBackgroundColor", chatBackgroundColor)
        put("bubbleSelfColor", bubbleSelfColor)
        put("bubbleOtherColor", bubbleOtherColor)
    }

    companion object {
        fun fromJson(o: JSONObject): AppSettings = AppSettings(
            fontFamily = o.optString("fontFamily", "default"),
            fontSizeSp = o.optDouble("fontSizeSp", 15.0).toFloat(),
            chatBackgroundColor = o.optInt("chatBackgroundColor", 0xFF0E1621.toInt()),
            bubbleSelfColor = o.optInt("bubbleSelfColor", 0xFF2B5278.toInt()),
            bubbleOtherColor = o.optInt("bubbleOtherColor", 0xFF182533.toInt())
        )
    }
}
