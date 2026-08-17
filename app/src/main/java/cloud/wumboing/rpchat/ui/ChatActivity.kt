package cloud.wumboing.rpchat.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import cloud.wumboing.rpchat.adapter.MessageAdapter
import cloud.wumboing.rpchat.data.Character
import cloud.wumboing.rpchat.data.Message
import cloud.wumboing.rpchat.data.Storage
import cloud.wumboing.rpchat.databinding.ActivityChatBinding

class ChatActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CHARACTER_ID = "extra_character_id"
    }

    private lateinit var binding: ActivityChatBinding
    private lateinit var storage: Storage
    private lateinit var adapter: MessageAdapter
    private lateinit var character: Character

    private var replyingTo: Message? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        storage = Storage(this)
        val characterId = intent.getStringExtra(EXTRA_CHARACTER_ID)
        val found = storage.loadCharacters().firstOrNull { it.id == characterId }
        if (found == null) {
            finish()
            return
        }
        character = found
        binding.toolbar.title = character.name
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = MessageAdapter(
            items = storage.loadMessages(character.id),
            onReplySwipeOrLongPress = { message -> startReply(message) }
        )
        binding.recyclerMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.recyclerMessages.adapter = adapter
        scrollToBottom()

        binding.btnSendAsSelf.setOnClickListener { sendMessage(isSelf = true) }
        binding.btnSendAsOther.setOnClickListener { sendMessage(isSelf = false) }
        binding.btnCancelReply.setOnClickListener { clearReply() }
    }

    private fun startReply(message: Message) {
        replyingTo = message
        binding.replyBar.visibility = android.view.View.VISIBLE
        binding.txtReplyPreview.text = message.text
    }

    private fun clearReply() {
        replyingTo = null
        binding.replyBar.visibility = android.view.View.GONE
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
