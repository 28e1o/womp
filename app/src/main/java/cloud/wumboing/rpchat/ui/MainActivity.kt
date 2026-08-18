package cloud.wumboing.rpchat.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import cloud.wumboing.rpchat.R
import cloud.wumboing.rpchat.adapter.CharacterAdapter
import cloud.wumboing.rpchat.data.Character
import cloud.wumboing.rpchat.data.Storage
import cloud.wumboing.rpchat.databinding.ActivityMainBinding
import cloud.wumboing.rpchat.util.clipToCircle
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var storage: Storage
    private lateinit var adapter: CharacterAdapter

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

        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, NewChatActivity::class.java))
        }
        binding.profileHeader.setOnClickListener { openSettings() }
        binding.btnSettings.setOnClickListener { openSettings() }

        refreshList()
        updateProfileHeader()
    }

    override fun onResume() {
        super.onResume()
        refreshList()
        updateProfileHeader()
    }

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
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
}
