package cloud.wumboing.rpchat.data

import org.json.JSONObject
import java.util.UUID

/**
 * isSelf = true  -> bubble kanan (dikirim sebagai "aku")
 * isSelf = false -> bubble kiri  (dikirim sebagai karakter/pemeran lain)
 * mediaType: null | "photo" | "video" | "audio"
 */
data class Message(
    val id: String = UUID.randomUUID().toString(),
    var text: String,
    var isSelf: Boolean,
    var timestamp: Long = System.currentTimeMillis(),
    var replyToId: String? = null,
    var replyPreview: String? = null,
    var replyName: String? = null,
    var reaction: String? = null,
    var edited: Boolean = false,
    var mediaPath: String? = null,
    var mediaType: String? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("text", text)
        put("isSelf", isSelf)
        put("timestamp", timestamp)
        put("replyToId", replyToId ?: JSONObject.NULL)
        put("replyPreview", replyPreview ?: JSONObject.NULL)
        put("replyName", replyName ?: JSONObject.NULL)
        put("reaction", reaction ?: JSONObject.NULL)
        put("edited", edited)
        put("mediaPath", mediaPath ?: JSONObject.NULL)
        put("mediaType", mediaType ?: JSONObject.NULL)
    }

    companion object {
        fun fromJson(o: JSONObject): Message = Message(
            id = o.getString("id"),
            text = o.getString("text"),
            isSelf = o.getBoolean("isSelf"),
            timestamp = o.optLong("timestamp", System.currentTimeMillis()),
            replyToId = if (o.isNull("replyToId")) null else o.optString("replyToId"),
            replyPreview = if (o.isNull("replyPreview")) null else o.optString("replyPreview"),
            replyName = if (!o.has("replyName") || o.isNull("replyName")) null else o.optString("replyName"),
            reaction = if (!o.has("reaction") || o.isNull("reaction")) null else o.optString("reaction"),
            edited = o.optBoolean("edited", false),
            mediaPath = if (!o.has("mediaPath") || o.isNull("mediaPath")) null else o.optString("mediaPath"),
            mediaType = if (!o.has("mediaType") || o.isNull("mediaType")) null else o.optString("mediaType")
        )
    }
}
