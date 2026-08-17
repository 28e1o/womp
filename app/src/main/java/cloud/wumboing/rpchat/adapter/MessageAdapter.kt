package cloud.wumboing.rpchat.adapter

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cloud.wumboing.rpchat.R
import cloud.wumboing.rpchat.data.Message
import cloud.wumboing.rpchat.databinding.ItemMessageBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter(
    private val items: MutableList<Message>,
    private val onReplySwipeOrLongPress: (Message) -> Unit
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
        b.txtTime.text = timeFormat.format(Date(message.timestamp))

        val params = b.bubble.layoutParams as? android.widget.LinearLayout.LayoutParams
        if (message.isSelf) {
            b.bubble.background = b.root.context.getDrawable(R.drawable.bg_bubble_self)
            b.bubble.gravity = Gravity.END
            params?.gravity = Gravity.END
        } else {
            b.bubble.background = b.root.context.getDrawable(R.drawable.bg_bubble_other)
            b.bubble.gravity = Gravity.START
            params?.gravity = Gravity.START
        }
        b.bubble.layoutParams = params

        if (!message.replyPreview.isNullOrEmpty()) {
            b.replyPreviewContainer.visibility = android.view.View.VISIBLE
            b.txtReplyPreviewInBubble.text = message.replyPreview
        } else {
            b.replyPreviewContainer.visibility = android.view.View.GONE
        }

        b.root.setOnLongClickListener {
            onReplySwipeOrLongPress(message)
            true
        }
    }

    override fun getItemCount() = items.size

    fun submit(newItems: List<Message>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun addMessage(message: Message) {
        items.add(message)
        notifyItemInserted(items.size - 1)
    }
}
