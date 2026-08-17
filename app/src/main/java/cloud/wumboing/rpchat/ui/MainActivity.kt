package cloud.wumboing.rpchat.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import cloud.wumboing.rpchat.adapter.CharacterAdapter
import cloud.wumboing.rpchat.data.Character
import cloud.wumboing.rpchat.data.Storage
import cloud.wumboing.rpchat.databinding.ActivityMainBinding
import cloud.wumboing.rpchat.databinding.DialogAddCharacterBinding
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var storage: Storage
    private lateinit var adapter: CharacterAdapter

    private var pendingAvatarUri: Uri? = null
    private var dialogBinding: DialogAddCharacterBinding? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pendingAvatarUri = uri
            dialogBinding?.imgAvatarPreview?.let { iv ->
                contentResolver.openInputStream(uri)?.use { input ->
                    val bmp = BitmapFactory.decodeStream(input)
                    iv.setImageBitmap(bmp)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        storage = Storage(this)

        adapter = CharacterAdapter(
            items = storage.loadCharacters(),
            previewProvider = { id -> storage.lastMessagePreview(id) },
            onClick = { character -> openChat(character) },
            onLongClick = { character -> confirmDelete(character) }
        )
        binding.recyclerCharacters.layoutManager = LinearLayoutManager(this)
        binding.recyclerCharacters.adapter = adapter

        binding.fabAdd.setOnClickListener { showAddCharacterDialog() }

        refreshList()
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun refreshList() {
        val list = storage.loadCharacters()
        adapter.update(list)
        binding.emptyView.visibility = if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun openChat(character: Character) {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra(ChatActivity.EXTRA_CHARACTER_ID, character.id)
        startActivity(intent)
    }

    private fun confirmDelete(character: Character) {
        AlertDialog.Builder(this)
            .setTitle(character.name)
            .setMessage("Hapus karakter ini beserta riwayat chat?")
            .setPositiveButton("Hapus") { _, _ ->
                storage.deleteCharacter(character.id)
                refreshList()
            }
            .setNegativeButton(getString(cloud.wumboing.rpchat.R.string.cancel), null)
            .show()
    }

    private fun showAddCharacterDialog() {
        pendingAvatarUri = null
        val db = DialogAddCharacterBinding.inflate(layoutInflater)
        dialogBinding = db

        db.imgAvatarPreview.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        AlertDialog.Builder(this)
            .setTitle(cloud.wumboing.rpchat.R.string.add_character)
            .setView(db.root)
            .setPositiveButton(cloud.wumboing.rpchat.R.string.save) { _, _ ->
                val name = db.editName.text.toString().trim()
                if (name.isNotEmpty()) {
                    val character = Character(name = name)
                    pendingAvatarUri?.let { uri ->
                        character.avatarPath = copyAvatarToInternal(uri, character.id)
                    }
                    storage.addCharacter(character)
                    refreshList()
                }
                dialogBinding = null
            }
            .setNegativeButton(cloud.wumboing.rpchat.R.string.cancel) { _, _ ->
                dialogBinding = null
            }
            .show()
    }

    private fun copyAvatarToInternal(uri: Uri, characterId: String): String? {
        return try {
            val outFile = File(storage.avatarsDir, "$characterId.jpg")
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
            outFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}
