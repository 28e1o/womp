package cloud.wumboing.rpchat.adapter

import android.graphics.BitmapFactory
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import cloud.wumboing.rpchat.R
import cloud.wumboing.rpchat.data.Message
import cloud.wumboing.rpchat.databinding.ItemMessageBinding
import cloud.wumboing.rpchat.util.clipToCircle
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter(
    private val items: MutableList<Message>,
    private val selfAvatarProvider: () -> String?,
    private val otherAvatarProvider: () -> String?,
    private val onLongPress: (Message, Int) -> Unit,
    private val onMediaClick: (Message) -> Unit
) : RecyclerView.Adapter<MessageAdapter.VH>() {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    inner class VH(val binding: ItemMessageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.imgAvatarLeft.clipToCircle()
        binding.imgAvatarRight.clipToCircle()
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val message = items[position]
        val b = holder.binding

        b.txtMessage.text = message.text
        b.txtMessage.visibility = if (message.text.isEmpty()) View.GONE else View.VISIBLE

        val timeText = timeFormat.format(Date(message.timestamp))
        b.txtTime.text = if (message.edited) {
            "$timeText · ${b.root.context.getString(R.string.edited_label)}"
        } else {
            timeText
        }

        val rowParams = b.contentRow.layoutParams as? android.widget.LinearLayout.LayoutParams

        if (message.isSelf) {
            b.bubble.background = b.root.context.getDrawable(R.drawable.bg_bubble_self)
            rowParams?.gravity = Gravity.END
            b.imgAvatarLeft.visibility = View.GONE
            b.imgAvatarRight.visibility = View.VISIBLE
            loadAvatar(b.imgAvatarRight, selfAvatarProvider())
        } else {
            b.bubble.background = b.root.context.getDrawable(R.drawable.bg_bubble_other)
            rowParams?.gravity = Gravity.START
            b.imgAvatarRight.visibility = View.GONE
            b.imgAvatarLeft.visibility = View.VISIBLE
            loadAvatar(b.imgAvatarLeft, otherAvatarProvider())
        }
        b.contentRow.layoutParams = rowParams

        if (!message.replyPreview.isNullOrEmpty()) {
            b.replyPreviewContainer.visibility = View.VISIBLE
            b.txtReplyNameInBubble.text = message.replyName ?: ""
            b.txtReplyPreviewInBubble.text = message.replyPreview
        } else {
            b.replyPreviewContainer.visibility = View.GONE
        }

        if (!message.reaction.isNullOrEmpty()) {
            b.txtReaction.visibility = View.VISIBLE
            b.txtReaction.text = message.reaction
        } else {
            b.txtReaction.visibility = View.GONE
        }

        b.imgMessagePhoto.visibility = View.GONE
        b.mediaFileRow.visibility = View.GONE
        if (!message.mediaPath.isNullOrEmpty()) {
            when (message.mediaType) {
                "photo" -> {
                    val f = File(message.mediaPath!!)
                    if (f.exists()) {
                        val bmp = BitmapFactory.decodeFile(message.mediaPath)
                        if (bmp != null) {
                            b.imgMessagePhoto.setImageBitmap(bmp)
                            b.imgMessagePhoto.visibility = View.VISIBLE
                        }
                    }
                }
                "video", "audio" -> {
                    b.mediaFileRow.visibility = View.VISIBLE
                    b.imgMediaIcon.setImageResource(
                        if (message.mediaType == "video") R.drawable.ic_video else R.drawable.ic_audio
                    )
                    b.txtMediaName.text = File(message.mediaPath!!).name
                }
            }
        }

        b.contentRow.setOnClickListener {
            if (!message.mediaPath.isNullOrEmpty()) onMediaClick(message)
        }

        b.contentRow.setOnLongClickListener {
            onLongPress(message, holder.bindingAdapterPosition)
            true
        }
    }

    private fun loadAvatar(iv: ImageView, path: String?) {
        iv.setImageResource(R.drawable.avatar_placeholder)
        if (!path.isNullOrEmpty()) {
            val f = File(path)
            if (f.exists()) {
                val bmp = BitmapFactory.decodeFile(path)
                if (bmp != null) iv.setImageBitmap(bmp)
            }
        }
    }

    override fun getItemCount() = items.size

    fun getItem(position: Int): Message = items[position]

    fun submit(newItems: List<Message>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun addMessage(message: Message) {
        items.add(message)
        notifyItemInserted(items.size - 1)
    }

    fun removeAt(position: Int) {
        if (position in items.indices) {
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun updateAt(position: Int, message: Message) {
        if (position in items.indices) {
            items[position] = message
            notifyItemChanged(position)
        }
    }

    fun refreshAvatars() {
        notifyDataSetChanged()
    }
}
