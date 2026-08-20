package cloud.wumboing.rpchat.data

import org.json.JSONObject
import java.util.UUID

data class GroupMember(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var avatarPath: String? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("avatarPath", avatarPath ?: JSONObject.NULL)
    }

    companion object {
        fun fromJson(o: JSONObject): GroupMember = GroupMember(
            id = o.getString("id"),
            name = o.getString("name"),
            avatarPath = if (o.isNull("avatarPath")) null else o.optString("avatarPath")
        )
    }
}
