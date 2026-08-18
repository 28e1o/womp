package cloud.wumboing.rpchat.ui

import android.content.Intent
import android.database.Cursor
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.GridLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
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
import cloud.wumboing.rpchat.util.BitmapUtils
import cloud.wumboing.rpchat.util.clipToCircle
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ChatActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CHARACTER_ID = "extra_character_id"
        private val REACTIONS = listOf("👍", "❤️", "😂", "😮", "😢", "🙏", "🔥", "😡")
        private val EMOJIS = listOf(
            "😀", "😁", "😂", "🤣", "😊", "😍", "😘", "😜", "🤔", "😎",
            "😢", "😭", "😡", "😱", "🥰", "🙄", "😴", "🤗", "😇", "🙃",
            "👍", "👎", "👏", "🙏", "💪", "❤️", "💔", "🔥", "✨", "🎉",
            "😏", "😌", "🥺", "😳", "🤯", "🥳", "😤", "😷", "🤝", "👀"
        )
    }

    private data class PendingMedia(val path: String, val type: String, val displayName: String)

    private lateinit var binding: ActivityChatBinding
    private lateinit var storage: Storage
    private lateinit var adapter: MessageAdapter
    private lateinit var character: Character

    private var replyingTo: Message? = null
    private var replyingToName: String? = null
    private var pendingMedia: PendingMedia? = null

    private var pendingAvatarCroppedPath: String? = null
    private var editDialogBinding: DialogAddCharacterBinding? = null

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
                editDialogBinding?.imgAvatarPreview?.let { iv ->
                    val bmp = BitmapFactory.decodeFile(path)
                    if (bmp != null) iv.setImageBitmap(bmp)
                }
            }
        }
    }

    private var pendingMediaType: String? = null
    private val pickMediaLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) handlePickedMedia(uri) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.imgToolbarAvatar.clipToCircle()

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
            onLongPress = { message, position -> showMessageOptions(message, position) },
            onMediaClick = { message -> openMediaExternally(message) }
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
        binding.btnCancelAttach.setOnClickListener { clearAttachment() }
        binding.btnAttach.setOnClickListener { showAttachOptions() }
        binding.btnEmoji.setOnClickListener { showEmojiPicker { emoji -> insertEmoji(emoji) } }

        binding.editMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { updateSendIconsVisibility() }
        })
        updateSendIconsVisibility()
    }

    private fun updateSendIconsVisibility() {
        val hasContent = binding.editMessage.text.toString().isNotEmpty() || pendingMedia != null
        binding.sendIconsContainer.visibility = if (hasContent) View.VISIBLE else View.GONE
        binding.btnAttach.visibility = if (hasContent) View.GONE else View.VISIBLE
    }

    private fun insertEmoji(emoji: String) {
        val start = binding.editMessage.selectionStart.coerceAtLeast(0)
        val end = binding.editMessage.selectionEnd.coerceAtLeast(0)
        binding.editMessage.text.replace(minOf(start, end), maxOf(start, end), emoji)
    }

    private fun showEmojiPicker(onPick: (String) -> Unit) {
        val grid = GridLayout(this).apply {
            columnCount = 8
            setPadding(16, 16, 16, 16)
        }
        EMOJIS.forEach { emoji ->
            val tv = TextView(this).apply {
                text = emoji
                textSize = 22f
                gravity = Gravity.CENTER
                setPadding(12, 12, 12, 12)
                layoutParams = GridLayout.LayoutParams().apply {
                    width = ViewGroup.LayoutParams.WRAP_CONTENT
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                }
                setOnClickListener { onPick(emoji) }
            }
            grid.addView(tv)
        }
        AlertDialog.Builder(this)
            .setView(grid)
            .setPositiveButton(R.string.save, null)
            .show()
    }

    private fun updateToolbarHeader() {
        binding.txtToolbarName.text = character.name
        if (!character.bio.isNullOrEmpty()) {
            binding.txtToolbarBio.text = character.bio
            binding.txtToolbarBio.visibility = View.VISIBLE
        } else {
            binding.txtToolbarBio.visibility = View.GONE
        }
        binding.imgToolbarAvatar.setImageResource(R.drawable.avatar_placeholder)
        character.avatarPath?.let { path ->
            val f = File(path)
            if (f.exists()) {
                val bmp = BitmapFactory.decodeFile(path)
                if (bmp != null) binding.imgToolbarAvatar.setImageBitmap(bmp)
            }
        }
    }

    private fun launchCrop(uri: Uri) {
        val intent = Intent(this, CropAvatarActivity::class.java)
        intent.putExtra(CropAvatarActivity.EXTRA_IMAGE_URI, uri.toString())
        cropLauncher.launch(intent)
    }

    private fun showEditCharacterDialog() {
        pendingAvatarCroppedPath = null
        val db = DialogAddCharacterBinding.inflate(layoutInflater)
        db.imgAvatarPreview.clipToCircle()
        editDialogBinding = db

        db.editName.setText(character.name)
        db.editBio.setText(character.bio ?: "")
        character.avatarPath?.let { path ->
            val f = File(path)
            if (f.exists()) {
                val bmp = BitmapFactory.decodeFile(path)
                if (bmp != null) db.imgAvatarPreview.setImageBitmap(bmp)
            }
        }

        db.imgAvatarPreview.setOnClickListener {
            pickAvatarLauncher.launch("image/*")
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.edit_character)
            .setView(db.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = db.editName.text.toString().trim()
                if (name.isNotEmpty()) character.name = name
                character.bio = db.editBio.text.toString().trim().ifEmpty { null }
                pendingAvatarCroppedPath?.let { path ->
                    character.avatarPath = copyCroppedToInternal(path, "char_${character.id}")
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

    private fun copyCroppedToInternal(tempPath: String, key: String): String? {
        return try {
            val outFile = File(storage.avatarsDir, "$key.jpg")
            File(tempPath).copyTo(outFile, overwrite = true)
            outFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    // ---------- Attachment (photo/video/audio) ----------

    private fun showAttachOptions() {
        val options = arrayOf(
            getString(R.string.attach_photo),
            getString(R.string.attach_video),
            getString(R.string.attach_audio)
        )
        AlertDialog.Builder(this)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> { pendingMediaType = "photo"; pickMediaLauncher.launch("image/*") }
                    1 -> { pendingMediaType = "video"; pickMediaLauncher.launch("video/*") }
                    2 -> { pendingMediaType = "audio"; pickMediaLauncher.launch("audio/*") }
                }
            }
            .show()
    }

    private fun handlePickedMedia(uri: Uri) {
        val type = pendingMediaType ?: return
        val displayName = getDisplayName(uri) ?: "file"
        val key = "${UUID.randomUUID()}_$displayName"
        val outFile = File(storage.mediaDir, key)
        try {
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(outFile).use { output -> input.copyTo(output) }
            }
        } catch (e: Exception) {
            return
        }
        pendingMedia = PendingMedia(outFile.absolutePath, type, displayName)
        binding.attachBar.visibility = View.VISIBLE
        binding.txtAttachPreviewName.text = displayName
        binding.imgAttachPreviewIcon.setImageResource(
            when (type) {
                "photo" -> R.drawable.ic_photo
                "video" -> R.drawable.ic_video
                else -> R.drawable.ic_audio
            }
        )
        updateSendIconsVisibility()
    }

    private fun getDisplayName(uri: Uri): String? {
        var name: String? = null
        val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) name = it.getString(idx)
            }
        }
        return name ?: uri.lastPathSegment
    }

    private fun clearAttachment() {
        pendingMedia = null
        binding.attachBar.visibility = View.GONE
        updateSendIconsVisibility()
    }

    private fun openMediaExternally(message: Message) {
        val path = message.mediaPath ?: return
        val file = File(path)
        if (!file.exists()) return
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val mime = when (message.mediaType) {
                "photo" -> "image/*"
                "video" -> "video/*"
                "audio" -> "audio/*"
                else -> "*/*"
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            // tidak ada aplikasi yang bisa membuka file ini
        }
    }

    // ---------- Swipe to reply & long-press options ----------

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
        replyingToName = if (message.isSelf) storage.loadProfile().name else character.name
        binding.replyBar.visibility = View.VISIBLE
        binding.txtReplyLabel.text = "${getString(R.string.reply_to)} $replyingToName"
        binding.txtReplyPreview.text = if (!message.text.isEmpty()) {
            message.text
        } else when (message.mediaType) {
            "photo" -> "📷 Foto"
            "video" -> "🎬 Video"
            "audio" -> "🎵 Audio"
            else -> ""
        }
    }

    private fun clearReply() {
        replyingTo = null
        replyingToName = null
        binding.replyBar.visibility = View.GONE
    }

    private fun sendMessage(isSelf: Boolean) {
        val text = binding.editMessage.text.toString().trim()
        val media = pendingMedia
        if (text.isEmpty() && media == null) return

        val message = Message(
            text = text,
            isSelf = isSelf,
            replyToId = replyingTo?.id,
            replyPreview = binding.txtReplyPreview.text?.toString()?.takeIf { replyingTo != null },
            replyName = replyingToName,
            mediaPath = media?.path,
            mediaType = media?.type
        )

        val messages = storage.loadMessages(character.id)
        messages.add(message)
        storage.saveMessages(character.id, messages)

        adapter.addMessage(message)
        binding.editMessage.text.clear()
        clearReply()
        clearAttachment()
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
