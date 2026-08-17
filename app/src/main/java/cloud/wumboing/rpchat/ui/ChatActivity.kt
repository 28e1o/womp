package cloud.wumboing.rpchat.ui

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cloud.wumboing.rpchat.R
import cloud.wumboing.rpchat.adapter.MessageAdapter
import cloud.wumboing.rpchat.data.Character
import cloud.wumboing.rpchat.data.Message
import cloud.wumboing.rpchat.data.Storage
import cloud.wumboing.rpchat.databinding.ActivityChatBinding
import cloud.wumboing.rpchat.databinding.DialogAddCharacterBinding
import java.io.File
import java.io.FileOutputStream

class ChatActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CHARACTER_ID = "extra_character_id"
        private val REACTIONS = listOf("👍", "❤️", "😂", "😮", "😢", "🙏", "🔥", "😡")
    }

    private lateinit var binding: ActivityChatBinding
    private lateinit var storage: Storage
    private lateinit var adapter: MessageAdapter
    private lateinit var character: Character

    private var replyingTo: Message? = null

    private var pendingAvatarUri: Uri? = null
    private var editDialogBinding: DialogAddCharacterBinding? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pendingAvatarUri = uri
            editDialogBinding?.imgAvatarPreview?.let { iv ->
                contentResolver.openInputStream(uri)?.use { input ->
                    val bmp = BitmapFactory.decodeStream(input)
                    iv.setImageBitmap(bmp)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        storage = Storage(this)
        val characterId = intent.getStringExtra(EXTRA_CHARACTER_ID)
        val found = storage.loadCharacters().firstOrNull { it.id == characterId }
        if (found == null) {
            finish()
            return
        }
        character = found

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.characterHeader.setOnClickListener { showEditCharacterDialog() }
        updateToolbarHeader()

        adapter = MessageAdapter(
            items = storage.loadMessages(character.id),
            selfAvatarProvider = { storage.loadProfile().avatarPath },
            otherAvatarProvider = { character.avatarPath },
            onLongPress = { message, position -> showMessageOptions(message, position) }
        )
        binding.recyclerMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.recyclerMessages.adapter = adapter
        attachSwipeToReply()
        scrollToBottom()

        binding.btnSendAsSelf.setOnClickListener { sendMessage(isSelf = true) }
        binding.btnSendAsOther.setOnClickListener { sendMessage(isSelf = false) }
        binding.btnCancelReply.setOnClickListener { clearReply() }
    }

    private fun updateToolbarHeader() {
        binding.txtToolbarName.text = character.name
        binding.imgToolbarAvatar.setImageResource(R.drawable.avatar_placeholder)
        character.avatarPath?.let { path ->
            val f = File(path)
            if (f.exists()) {
                val bmp = BitmapFactory.decodeFile(path)
                if (bmp != null) binding.imgToolbarAvatar.setImageBitmap(bmp)
            }
        }
    }

    private fun showEditCharacterDialog() {
        pendingAvatarUri = null
        val db = DialogAddCharacterBinding.inflate(layoutInflater)
        editDialogBinding = db

        db.editName.setText(character.name)
        character.avatarPath?.let { path ->
            val f = File(path)
            if (f.exists()) {
                val bmp = BitmapFactory.decodeFile(path)
                if (bmp != null) db.imgAvatarPreview.setImageBitmap(bmp)
            }
        }

        db.imgAvatarPreview.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.edit_character)
            .setView(db.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = db.editName.text.toString().trim()
                if (name.isNotEmpty()) character.name = name
                pendingAvatarUri?.let { uri ->
                    character.avatarPath = copyAvatarToInternal(uri, "char_${character.id}")
                }
                storage.updateCharacter(character)
                updateToolbarHeader()
                adapter.refreshAvatars()
                editDialogBinding = null
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                editDialogBinding = null
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

    private fun attachSwipeToReply() {
        val callback = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder) = 0.35f

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return
                val message = adapter.getItem(position)
                startReply(message)
                adapter.notifyItemChanged(position)
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(binding.recyclerMessages)
    }

    private fun showMessageOptions(message: Message, position: Int) {
        val options = arrayOf(
            getString(R.string.delete_message),
            getString(R.string.edit_text),
            getString(R.string.give_reaction)
        )
        AlertDialog.Builder(this)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> deleteMessage(position)
                    1 -> editMessageText(message, position)
                    2 -> showReactionPicker(message, position)
                }
            }
            .show()
    }

    private fun deleteMessage(position: Int) {
        val messages = storage.loadMessages(character.id).toMutableList()
        if (position !in messages.indices) return
        messages.removeAt(position)
        storage.saveMessages(character.id, messages)
        adapter.removeAt(position)
    }

    private fun editMessageText(message: Message, position: Int) {
        val input = EditText(this)
        input.setText(message.text)
        input.setSelection(input.text.length)
        input.hint = getString(R.string.edit_message_hint)

        AlertDialog.Builder(this)
            .setTitle(R.string.edit_text)
            .setView(input)
            .setPositiveButton(R.string.save) { _, _ ->
                val newText = input.text.toString().trim()
                if (newText.isNotEmpty()) {
                    val messages = storage.loadMessages(character.id).toMutableList()
                    if (position in messages.indices) {
                        val updated = messages[position].copy(text = newText, edited = true)
                        messages[position] = updated
                        storage.saveMessages(character.id, messages)
                        adapter.updateAt(position, updated)
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showReactionPicker(message: Message, position: Int) {
        AlertDialog.Builder(this)
            .setItems(REACTIONS.toTypedArray()) { _, which ->
                val emoji = REACTIONS[which]
                val messages = storage.loadMessages(character.id).toMutableList()
                if (position in messages.indices) {
                    val current = messages[position]
                    val updated = current.copy(reaction = if (current.reaction == emoji) null else emoji)
                    messages[position] = updated
                    storage.saveMessages(character.id, messages)
                    adapter.updateAt(position, updated)
                }
            }
            .show()
    }

    private fun startReply(message: Message) {
        replyingTo = message
        binding.replyBar.visibility = View.VISIBLE
        binding.txtReplyPreview.text = message.text
    }

    private fun clearReply() {
        replyingTo = null
        binding.replyBar.visibility = View.GONE
    }

    private fun sendMessage(isSelf: Boolean) {
        val text = binding.editMessage.text.toString().trim()
        if (text.isEmpty()) return

        val message = Message(
            text = text,
            isSelf = isSelf,
            replyToId = replyingTo?.id,
            replyPreview = replyingTo?.text
        )

        val messages = storage.loadMessages(character.id)
        messages.add(message)
        storage.saveMessages(character.id, messages)

        adapter.addMessage(message)
        binding.editMessage.text.clear()
        clearReply()
        scrollToBottom()
    }

    private fun scrollToBottom() {
        binding.recyclerMessages.post {
            if (adapter.itemCount > 0) {
                binding.recyclerMessages.scrollToPosition(adapter.itemCount - 1)
            }
        }
    }
}
