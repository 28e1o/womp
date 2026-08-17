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
        supportActionBar?.setDisplayShowTitleEnabled(false)

        storage = Storage(this)

        adapter = CharacterAdapter(
            items = storage.loadCharacters(),
            previewProvider = { id -> storage.lastMessagePreview(id) },
            onClick = { character -> openChat(character) },
            onLongClick = { character -> confirmDelete(character) }
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
        val list = storage.loadCharacters()
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

    private fun confirmDelete(character: Character) {
        AlertDialog.Builder(this)
            .setTitle(character.name)
            .setMessage("Hapus kontak ini beserta riwayat chat?")
            .setPositiveButton(R.string.delete_message) { _, _ ->
                storage.deleteCharacter(character.id)
                refreshList()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
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
        dialog.show()
    }

    private fun showAddCharacterDialog() {
        showAvatarNameDialog(
            titleRes = R.string.add_character,
            initialName = null,
            initialAvatarPath = null
        ) { name, uri ->
            val character = Character(name = name)
            uri?.let { character.avatarPath = copyAvatarToInternal(it, "char_${character.id}") }
            storage.addCharacter(character)
            refreshList()
        }
    }

    private fun showEditProfileDialog() {
        val profile = storage.loadProfile()
        showAvatarNameDialog(
            titleRes = R.string.edit_profile,
            initialName = profile.name,
            initialAvatarPath = profile.avatarPath
        ) { name, uri ->
            profile.name = name
            uri?.let { profile.avatarPath = copyAvatarToInternal(it, "self_profile") }
            storage.saveProfile(profile)
            updateProfileHeader()
            adapter.notifyDataSetChanged()
        }
    }

    private fun showAvatarNameDialog(
        titleRes: Int,
        initialName: String?,
        initialAvatarPath: String?,
        onSave: (name: String, avatarUri: Uri?) -> Unit
    ) {
        pendingAvatarUri = null
        val db = DialogAddCharacterBinding.inflate(layoutInflater)
        dialogBinding = db

        if (!initialName.isNullOrEmpty()) db.editName.setText(initialName)
        if (!initialAvatarPath.isNullOrEmpty()) {
            val f = File(initialAvatarPath)
            if (f.exists()) {
                val bmp = BitmapFactory.decodeFile(initialAvatarPath)
                if (bmp != null) db.imgAvatarPreview.setImageBitmap(bmp)
            }
        }

        db.imgAvatarPreview.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        AlertDialog.Builder(this)
            .setTitle(titleRes)
            .setView(db.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = db.editName.text.toString().trim()
                if (name.isNotEmpty()) onSave(name, pendingAvatarUri)
                dialogBinding = null
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                dialogBinding = null
            }
            .show()
    }

    private fun copyAvatarToInternal(uri: Uri, key: String): String? {
        return try {
            val outFile = File(storage.avatarsDir, "$key.jpg")
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
