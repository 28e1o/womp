package cloud.wumboing.rpchat.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import cloud.wumboing.rpchat.R
import cloud.wumboing.rpchat.adapter.CharacterAdapter
import cloud.wumboing.rpchat.data.Character
import cloud.wumboing.rpchat.data.Storage
import cloud.wumboing.rpchat.databinding.ActivityNewChatBinding
import cloud.wumboing.rpchat.databinding.DialogAddCharacterBinding
import cloud.wumboing.rpchat.util.clipToCircle
import java.io.File

class NewChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewChatBinding
    private lateinit var storage: Storage
    private var allContacts: List<Character> = emptyList()

    private var pendingAvatarCroppedPath: String? = null
    private var dialogBinding: DialogAddCharacterBinding? = null

    private val pickAvatarLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) launchCrop(uri) }

    private val cropLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val path = result.data?.getStringExtra(CropAvatarActivity.EXTRA_RESULT_PATH)
            if (path != null) {
                pendingAvatarCroppedPath = path
                dialogBinding?.imgAvatarPreview?.let { iv ->
                    val bmp = BitmapFactory.decodeFile(path)
                    if (bmp != null) iv.setImageBitmap(bmp)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        storage = Storage(this)
        binding.recyclerAllContacts.layoutManager = LinearLayoutManager(this)

        binding.rowNewGroup.setOnClickListener {
            Toast.makeText(this, R.string.new_group_soon, Toast.LENGTH_SHORT).show()
        }
        binding.rowNewContact.setOnClickListener { showAddCharacterDialog() }

        binding.editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { filterContacts(s?.toString() ?: "") }
        })

        loadContacts()
    }

    private fun loadContacts() {
        allContacts = storage.loadCharacters()
        filterContacts(binding.editSearch.text.toString())
    }

    private fun filterContacts(query: String) {
        val filtered = if (query.isBlank()) {
            allContacts
        } else {
            allContacts.filter { it.name.contains(query, ignoreCase = true) }
        }

        if (filtered.isEmpty()) {
            binding.txtAllContactsLabel.visibility = View.GONE
        } else {
            binding.txtAllContactsLabel.visibility = View.VISIBLE
        }

        binding.recyclerAllContacts.adapter = CharacterAdapter(
            items = filtered.toMutableList(),
            previewProvider = { id -> storage.lastMessagePreview(id) },
            timeProvider = { id -> storage.lastMessageTimestamp(id) },
            onClick = { character -> openCharacterChat(character) },
            onLongClick = { }
        )
    }

    private fun openCharacterChat(character: Character) {
        if (!character.visible) storage.unhide(character.id)
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra(ChatActivity.EXTRA_CHARACTER_ID, character.id)
        startActivity(intent)
        finish()
    }

    private fun launchCrop(uri: Uri) {
        val intent = Intent(this, CropAvatarActivity::class.java)
        intent.putExtra(CropAvatarActivity.EXTRA_IMAGE_URI, uri.toString())
        cropLauncher.launch(intent)
    }

    private fun showAddCharacterDialog() {
        pendingAvatarCroppedPath = null
        val db = DialogAddCharacterBinding.inflate(layoutInflater)
        db.imgAvatarPreview.clipToCircle()
        dialogBinding = db

        db.imgAvatarPreview.setOnClickListener {
            pickAvatarLauncher.launch("image/*")
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.add_character)
            .setView(db.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = db.editName.text.toString().trim()
                val bio = db.editBio.text.toString().trim().ifEmpty { null }
                if (name.isNotEmpty()) {
                    val character = Character(name = name, bio = bio)
                    pendingAvatarCroppedPath?.let {
                        character.avatarPath = copyCroppedToInternal(it, "char_${character.id}")
                    }
                    storage.addCharacter(character)
                    openCharacterChat(character)
                }
                dialogBinding = null
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                dialogBinding = null
            }
            .show()
    }

    private fun copyCroppedToInternal(tempPath: String, key: String): String? {
        return try {
            val outFile = File(storage.avatarsDir, "$key.jpg")
            File(tempPath).copyTo(outFile, overwrite = true)
            outFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}
