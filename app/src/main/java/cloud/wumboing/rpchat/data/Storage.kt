package cloud.wumboing.rpchat.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Penyimpanan sederhana berbasis file JSON di internal storage.
 * Tidak pakai Room/Gson supaya dependency & ukuran APK tetap minim.
 */
class Storage(context: Context) {

    private val charactersFile = File(context.filesDir, "characters.json")
    private val chatsDir = File(context.filesDir, "chats").apply { mkdirs() }
    val avatarsDir = File(context.filesDir, "avatars").apply { mkdirs() }

    fun loadCharacters(): MutableList<Character> {
        if (!charactersFile.exists()) return mutableListOf()
        val arr = JSONArray(charactersFile.readText())
        val list = mutableListOf<Character>()
        for (i in 0 until arr.length()) {
            list.add(Character.fromJson(arr.getJSONObject(i)))
        }
        return list
    }

    fun saveCharacters(list: List<Character>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        charactersFile.writeText(arr.toString())
    }

    fun addCharacter(character: Character) {
        val list = loadCharacters()
        list.add(character)
        saveCharacters(list)
    }

    fun updateCharacter(character: Character) {
        val list = loadCharacters()
        val idx = list.indexOfFirst { it.id == character.id }
        if (idx >= 0) {
            list[idx] = character
            saveCharacters(list)
        }
    }

    fun deleteCharacter(characterId: String) {
        val list = loadCharacters().filterNot { it.id == characterId }
        saveCharacters(list)
        chatFile(characterId).delete()
    }

    private fun chatFile(characterId: String) = File(chatsDir, "$characterId.json")

    fun loadMessages(characterId: String): MutableList<Message> {
        val f = chatFile(characterId)
        if (!f.exists()) return mutableListOf()
        val arr = JSONArray(f.readText())
        val list = mutableListOf<Message>()
        for (i in 0 until arr.length()) {
            list.add(Message.fromJson(arr.getJSONObject(i)))
        }
        return list
    }

    fun saveMessages(characterId: String, messages: List<Message>) {
        val arr = JSONArray()
        messages.forEach { arr.put(it.toJson()) }
        chatFile(characterId).writeText(arr.toString())
    }

    fun lastMessagePreview(characterId: String): String? {
        val messages = loadMessages(characterId)
        return messages.lastOrNull()?.text
    }
}
