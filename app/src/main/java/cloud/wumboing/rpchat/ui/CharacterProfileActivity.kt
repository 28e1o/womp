package cloud.wumboing.rpchat.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import cloud.wumboing.rpchat.R
import cloud.wumboing.rpchat.adapter.StatusGalleryAdapter
import cloud.wumboing.rpchat.data.Character
import cloud.wumboing.rpchat.data.Storage
import cloud.wumboing.rpchat.databinding.ActivityCharacterProfileBinding
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class CharacterProfileActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CHARACTER_ID = "extra_character_id"
    }

    private lateinit var binding: ActivityCharacterProfileBinding
    private lateinit var storage: Storage
    private lateinit var character: Character

    private var currentGalleryPosition = 0
    private val snapHelper = PagerSnapHelper()

    private val pickStatusPhotoLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) addStatusPhoto(uri) }

    private val pickAvatarLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) launchCrop(uri) }

    private val cropLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val path = result.data?.getStringExtra(CropAvatarActivity.EXTRA_RESULT_PATH)
            if (path != null) {
                val outFile = File(storage.avatarsDir, "char_${character.id}.jpg")
                File(path).copyTo(outFile, overwrite = true)
                character.avatarPath = outFile.absolutePath
                storage.updateCharacter(character)
                refreshGallery()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCharacterProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        storage = Storage(this)
        val characterId = intent.getStringExtra(EXTRA_CHARACTER_ID)
        val found = storage.loadCharacters().firstOrNull { it.id == characterId }
        if (found == null) {
            finish()
            return
        }
        character = found

        binding.galleryRecycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        snapHelper.attachToRecyclerView(binding.galleryRecycler)
        binding.galleryRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val lm = recyclerView.layoutManager ?: return
                    val snapView = snapHelper.findSnapView(lm) ?: return
                    currentGalleryPosition = lm.getPosition(snapView)
                }
            }
        })
        refreshGallery()

        binding.txtProfileName.text = character.name
        binding.editUsername.setText(character.username ?: "")
        binding.editDescription.setText(character.bio ?: "")

        binding.btnBack.setOnClickListener { finish() }
        binding.btnGalleryMenu.setOnClickListener { showGalleryMenu() }

        binding.btnProfileChat.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra(ChatActivity.EXTRA_CHARACTER_ID, character.id)
            startActivity(intent)
        }
        // btnProfileMute, btnProfileCall, btnProfileVideo sengaja tidak diberi aksi (belum berfungsi)

        binding.editUsername.addTextChangedListener(simpleWatcher {
            val raw = binding.editUsername.text.toString().trim().removePrefix("@")
            character.username = raw.ifEmpty { null }
            storage.updateCharacter(character)
        })

        binding.editDescription.addTextChangedListener(simpleWatcher {
            character.bio = binding.editDescription.text.toString().trim().ifEmpty { null }
            storage.updateCharacter(character)
        })
    }

    private fun simpleWatcher(onChanged: () -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) { onChanged() }
    }

    private fun galleryPaths(): List<String> {
        val list = mutableListOf<String>()
        character.avatarPath?.let { if (File(it).exists()) list.add(it) }
        list.addAll(character.statusPhotos.filter { File(it).exists() })
        return list
    }

    private fun refreshGallery() {
        binding.galleryRecycler.adapter = StatusGalleryAdapter(galleryPaths(), character.name, character.id)
    }

    private fun showGalleryMenu() {
        val paths = galleryPaths()
        val hasAvatar = character.avatarPath != null
        val isViewingStatusPhoto = hasAvatar && currentGalleryPosition in 1 until paths.size ||
            (!hasAvatar && currentGalleryPosition in paths.indices)

        val options = mutableListOf(
            getString(R.string.add_status_photo),
            getString(R.string.change_avatar)
        )
        if (isViewingStatusPhoto) options.add(getString(R.string.delete_status_photo))

        AlertDialog.Builder(this)
            .setItems(options.toTypedArray()) { _, which ->
                when (which) {
                    0 -> pickStatusPhotoLauncher.launch("image/*")
                    1 -> pickAvatarLauncher.launch("image/*")
                    2 -> deleteCurrentStatusPhoto()
                }
            }
            .show()
    }

    private fun addStatusPhoto(uri: Uri) {
        try {
            val outFile = File(storage.statusDir, "${character.id}_${UUID.randomUUID()}.jpg")
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(outFile).use { output -> input.copyTo(output) }
            }
            character.statusPhotos.add(outFile.absolutePath)
            storage.updateCharacter(character)
            refreshGallery()
        } catch (e: Exception) {
            // abaikan jika gagal
        }
    }

    private fun deleteCurrentStatusPhoto() {
        val paths = galleryPaths()
        if (currentGalleryPosition !in paths.indices) return
        val path = paths[currentGalleryPosition]
        if (path == character.avatarPath) return // jangan hapus avatar lewat sini
        character.statusPhotos.remove(path)
        File(path).delete()
        storage.updateCharacter(character)
        currentGalleryPosition = 0
        refreshGallery()
    }

    private fun launchCrop(uri: Uri) {
        val intent = Intent(this, CropAvatarActivity::class.java)
        intent.putExtra(CropAvatarActivity.EXTRA_IMAGE_URI, uri.toString())
        cropLauncher.launch(intent)
    }
}
