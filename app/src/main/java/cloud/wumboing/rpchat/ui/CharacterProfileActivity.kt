package cloud.wumboing.rpchat.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import cloud.wumboing.rpchat.adapter.StatusGalleryAdapter
import cloud.wumboing.rpchat.data.Character
import cloud.wumboing.rpchat.data.Storage
import cloud.wumboing.rpchat.databinding.ActivityCharacterProfileBinding
import java.io.File

class CharacterProfileActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CHARACTER_ID = "extra_character_id"
    }

    private lateinit var binding: ActivityCharacterProfileBinding
    private lateinit var storage: Storage
    private lateinit var character: Character

    private val snapHelper = PagerSnapHelper()

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
        refreshGallery()

        binding.txtProfileName.text = character.name

        binding.btnBack.setOnClickListener { finish() }
        binding.btnGalleryMenu.setOnClickListener { pickAvatarLauncher.launch("image/*") }

        binding.btnProfileChat.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra(ChatActivity.EXTRA_CHARACTER_ID, character.id)
            startActivity(intent)
        }
        // btnProfileMute, btnProfileCall, btnProfileVideo sengaja tidak diberi aksi (belum berfungsi)
    }

    private fun refreshGallery() {
        val paths = character.avatarPath?.let { if (File(it).exists()) listOf(it) else emptyList() } ?: emptyList()
        binding.galleryRecycler.adapter = StatusGalleryAdapter(paths, character.name, character.id)
    }

    private fun launchCrop(uri: Uri) {
        val intent = Intent(this, CropAvatarActivity::class.java)
        intent.putExtra(CropAvatarActivity.EXTRA_IMAGE_URI, uri.toString())
        cropLauncher.launch(intent)
    }
}
