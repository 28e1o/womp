package cloud.wumboing.rpchat.data

import org.json.JSONObject

data class UserProfile(
    var name: String = "Aku",
    var avatarPath: String? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        put("avatarPath", avatarPath ?: JSONObject.NULL)
    }

    companion object {
        fun fromJson(o: JSONObject): UserProfile = UserProfile(
            name = o.optString("name", "Aku"),
            avatarPath = if (o.isNull("avatarPath")) null else o.optString("avatarPath")
        )
    }
}
