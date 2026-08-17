package cloud.wumboing.rpchat.adapter

import android.graphics.BitmapFactory
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cloud.wumboing.rpchat.R
import cloud.wumboing.rpchat.data.Message
import cloud.wumboing.rpchat.databinding.ItemMessageBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter(
    private val items: MutableList<Message>,
    private val selfAvatarProvider: () -> String?,
    private val otherAvatarProvider: () -> String?,
    private val onLongPress: (Message, Int) -> Unit
) : RecyclerView.Adapter<MessageAdapter.VH>() {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    inner class VH(val binding: ItemMessageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val message = items[position]
        val b = holder.binding

        b.txtMessage.text = message.text
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

        b.contentRow.setOnLongClickListener {
            onLongPress(message, holder.bindingAdapterPosition)
            true
        }
    }

    private fun loadAvatar(iv: android.widget.ImageView, path: String?) {
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
