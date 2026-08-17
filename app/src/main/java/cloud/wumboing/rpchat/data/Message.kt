package cloud.wumboing.rpchat.data

import org.json.JSONObject
import java.util.UUID

/**
 * isSelf = true  -> bubble kanan (dikirim sebagai "aku")
 * isSelf = false -> bubble kiri  (dikirim sebagai karakter/pemeran lain)
 */
data class Message(
    val id: String = UUID.randomUUID().toString(),
    var text: String,
    var isSelf: Boolean,
    var timestamp: Long = System.currentTimeMillis(),
    var replyToId: String? = null,
    var replyPreview: String? = null,
    var reaction: String? = null,
    var edited: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("text", text)
        put("isSelf", isSelf)
        put("timestamp", timestamp)
        put("replyToId", replyToId ?: JSONObject.NULL)
        put("replyPreview", replyPreview ?: JSONObject.NULL)
        put("reaction", reaction ?: JSONObject.NULL)
        put("edited", edited)
    }

    companion object {
        fun fromJson(o: JSONObject): Message = Message(
            id = o.getString("id"),
            text = o.getString("text"),
            isSelf = o.getBoolean("isSelf"),
            timestamp = o.optLong("timestamp", System.currentTimeMillis()),
            replyToId = if (o.isNull("replyToId")) null else o.optString("replyToId"),
            replyPreview = if (o.isNull("replyPreview")) null else o.optString("replyPreview"),
            reaction = if (!o.has("reaction") || o.isNull("reaction")) null else o.optString("reaction"),
            edited = o.optBoolean("edited", false)
        )
    }
}
