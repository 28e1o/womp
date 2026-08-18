package cloud.wumboing.rpchat.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
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
import cloud.wumboing.rpchat.databinding.ActivityMainBinding
import cloud.wumboing.rpchat.databinding.DialogAddCharacterBinding
import cloud.wumboing.rpchat.databinding.DialogNewChatBinding
import cloud.wumboing.rpchat.util.clipToCircle
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var storage: Storage
    private lateinit var adapter: CharacterAdapter

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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.imgMyAvatar.clipToCircle()

        storage = Storage(this)

        adapter = CharacterAdapter(
            items = storage.visibleCharacters().toMutableList(),
            previewProvider = { id -> storage.lastMessagePreview(id) },
            onClick = { character -> openChat(character) },
            onLongClick = { character -> confirmHide(character) }
        )
        binding.recyclerCharacters.layoutManager = LinearLayoutManager(this)
        binding.recyclerCharacters.adapter = adapter

        binding.fabAdd.setOnClickListener { showNewChatDialog() }
        binding.profileHeader.setOnClickListener { showEditProfileDialog() }

        refreshList()
        updateProfileHeader()
    }

    override fun onResume() {
        super.onResume()
        refreshList()
        updateProfileHeader()
    }

    private fun refreshList() {
        val list = storage.visibleCharacters()
        adapter.update(list)
        binding.emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun updateProfileHeader() {
        val profile = storage.loadProfile()
        binding.txtMyName.text = profile.name
        binding.imgMyAvatar.setImageResource(R.drawable.avatar_placeholder)
        profile.avatarPath?.let { path ->
            val f = File(path)
            if (f.exists()) {
                val bmp = BitmapFactory.decodeFile(path)
                if (bmp != null) binding.imgMyAvatar.setImageBitmap(bmp)
            }
        }
    }

    private fun openChat(character: Character) {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra(ChatActivity.EXTRA_CHARACTER_ID, character.id)
        startActivity(intent)
    }

    private fun confirmHide(character: Character) {
        AlertDialog.Builder(this)
            .setTitle(character.name)
            .setMessage("Hapus obrolan ini dari daftar? Kontak & riwayat chat tetap tersimpan, bisa dipanggil lagi lewat tombol +.")
            .setPositiveButton(R.string.delete_message) { _, _ ->
                storage.hideFromChatList(character.id)
                refreshList()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun launchCrop(uri: Uri) {
        val intent = Intent(this, CropAvatarActivity::class.java)
        intent.putExtra(CropAvatarActivity.EXTRA_IMAGE_URI, uri.toString())
        cropLauncher.launch(intent)
    }

    private fun showNewChatDialog() {
        val db = DialogNewChatBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.new_chat)
            .setView(db.root)
            .create()

        db.rowNewGroup.setOnClickListener {
            dialog.dismiss()
            Toast.makeText(this, R.string.new_group_soon, Toast.LENGTH_SHORT).show()
        }
        db.rowNewContact.setOnClickListener {
            dialog.dismiss()
            showAddCharacterDialog()
        }

        val allContacts = storage.loadCharacters()
        if (allContacts.isNotEmpty()) {
            db.txtAllContactsLabel.visibility = View.VISIBLE
            db.recyclerAllContacts.visibility = View.VISIBLE
            db.recyclerAllContacts.layoutManager = LinearLayoutManager(this)
            db.recyclerAllContacts.adapter = CharacterAdapter(
                items = allContacts.toMutableList(),
                previewProvider = { id -> storage.lastMessagePreview(id) },
                onClick = { character ->
                    dialog.dismiss()
                    if (!character.visible) storage.unhide(character.id)
                    openChat(character)
                },
                onLongClick = { }
            )
        } else {
            db.txtAllContactsLabel.visibility = View.GONE
            db.recyclerAllContacts.visibility = View.GONE
        }

        dialog.show()
    }

    private fun showAddCharacterDialog() {
        showAvatarNameBioDialog(
            titleRes = R.string.add_character,
            initialName = null,
            initialBio = null,
            initialAvatarPath = null
        ) { name, bio, croppedPath ->
            val character = Character(name = name, bio = bio)
            croppedPath?.let { character.avatarPath = copyCroppedToInternal(it, "char_${character.id}") }
            storage.addCharacter(character)
            refreshList()
        }
    }

    private fun showEditProfileDialog() {
        val profile = storage.loadProfile()
        showAvatarNameBioDialog(
            titleRes = R.string.edit_profile,
            initialName = profile.name,
            initialBio = profile.bio,
            initialAvatarPath = profile.avatarPath
        ) { name, bio, croppedPath ->
            profile.name = name
            profile.bio = bio
            croppedPath?.let { profile.avatarPath = copyCroppedToInternal(it, "self_profile") }
            storage.saveProfile(profile)
            updateProfileHeader()
            adapter.notifyDataSetChanged()
        }
    }

    private fun showAvatarNameBioDialog(
        titleRes: Int,
        initialName: String?,
        initialBio: String?,
        initialAvatarPath: String?,
        onSave: (name: String, bio: String?, croppedPath: String?) -> Unit
    ) {
        pendingAvatarCroppedPath = null
        val db = DialogAddCharacterBinding.inflate(layoutInflater)
        db.imgAvatarPreview.clipToCircle()
        dialogBinding = db

        if (!initialName.isNullOrEmpty()) db.editName.setText(initialName)
        if (!initialBio.isNullOrEmpty()) db.editBio.setText(initialBio)
        if (!initialAvatarPath.isNullOrEmpty()) {
            val f = File(initialAvatarPath)
            if (f.exists()) {
                val bmp = BitmapFactory.decodeFile(initialAvatarPath)
                if (bmp != null) db.imgAvatarPreview.setImageBitmap(bmp)
            }
        }

        db.imgAvatarPreview.setOnClickListener {
            pickAvatarLauncher.launch("image/*")
        }

        AlertDialog.Builder(this)
            .setTitle(titleRes)
            .setView(db.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = db.editName.text.toString().trim()
                val bio = db.editBio.text.toString().trim().ifEmpty { null }
                if (name.isNotEmpty()) onSave(name, bio, pendingAvatarCroppedPath)
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
