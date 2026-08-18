package cloud.wumboing.rpchat.data

import org.json.JSONObject

/** Satu sesi kunjungan ke sebuah chat karakter, dicatat saat keluar dari layar chat. */
data class ChatSession(
    val characterId: String,
    val startTime: Long,
    val durationSeconds: Long
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("characterId", characterId)
        put("startTime", startTime)
        put("durationSeconds", durationSeconds)
    }

    companion object {
        fun fromJson(o: JSONObject): ChatSession = ChatSession(
            characterId = o.getString("characterId"),
            startTime = o.getLong("startTime"),
            durationSeconds = o.getLong("durationSeconds")
        )
    }
}
