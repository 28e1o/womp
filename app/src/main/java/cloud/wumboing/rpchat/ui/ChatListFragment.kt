package cloud.wumboing.rpchat.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import cloud.wumboing.rpchat.R
import cloud.wumboing.rpchat.adapter.CharacterAdapter
import cloud.wumboing.rpchat.data.Character
import cloud.wumboing.rpchat.data.Storage
import cloud.wumboing.rpchat.databinding.FragmentChatListBinding
import cloud.wumboing.rpchat.util.clipToCircle
import java.io.File

class ChatListFragment : Fragment() {

    private var _binding: FragmentChatListBinding? = null
    private val binding get() = _binding!!

    private lateinit var storage: Storage
    private lateinit var adapter: CharacterAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()
        storage = Storage(context)

        binding.imgMyAvatar.clipToCircle()
        (activity as? androidx.appcompat.app.AppCompatActivity)?.setSupportActionBar(binding.toolbar)
        (activity as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.setDisplayShowTitleEnabled(false)

        adapter = CharacterAdapter(
            items = storage.visibleCharacters().toMutableList(),
            previewProvider = { id -> storage.lastMessagePreview(id) },
            onClick = { character -> openChat(character) },
            onLongClick = { character -> confirmHide(character) }
        )
        binding.recyclerCharacters.layoutManager = LinearLayoutManager(context)
        binding.recyclerCharacters.adapter = adapter

        binding.fabAdd.setOnClickListener {
            startActivity(Intent(context, NewChatActivity::class.java))
        }
        binding.profileHeader.setOnClickListener { openSettings() }
        binding.btnSettings.setOnClickListener { openSettings() }

        refreshList()
        updateProfileHeader()
    }

    override fun onResume() {
        super.onResume()
        if (_binding == null) return
        refreshList()
        updateProfileHeader()
    }

    private fun openSettings() {
        startActivity(Intent(requireContext(), SettingsActivity::class.java))
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
        val intent = Intent(requireContext(), ChatActivity::class.java)
        intent.putExtra(ChatActivity.EXTRA_CHARACTER_ID, character.id)
        startActivity(intent)
    }

    private fun confirmHide(character: Character) {
        AlertDialog.Builder(requireContext())
            .setTitle(character.name)
            .setMessage("Hapus obrolan ini dari daftar? Kontak & riwayat chat tetap tersimpan, bisa dipanggil lagi lewat tombol +.")
            .setPositiveButton(R.string.delete_message) { _, _ ->
                storage.hideFromChatList(character.id)
                refreshList()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
