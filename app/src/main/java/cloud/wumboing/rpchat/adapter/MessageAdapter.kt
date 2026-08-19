package cloud.wumboing.rpchat.adapter

import android.graphics.BitmapFactory
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cloud.wumboing.rpchat.R
import cloud.wumboing.rpchat.data.AppSettings
import cloud.wumboing.rpchat.data.Message
import cloud.wumboing.rpchat.databinding.ItemMessageBinding
import cloud.wumboing.rpchat.util.BitmapUtils
import cloud.wumboing.rpchat.util.ThemeUtils
import cloud.wumboing.rpchat.util.clipToCircle
import cloud.wumboing.rpchat.util.loadAvatarOrInitials
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter(
    private val items: MutableList<Message>,
    private val selfAvatarProvider: () -> String?,
    private val selfNameProvider: () -> String,
    private val otherAvatarProvider: () -> String?,
    private val otherNameProvider: () -> String,
    private val otherSeed: String,
    private val settingsProvider: () -> AppSettings,
    private val pinnedIdProvider: () -> String?,
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
        val settings = settingsProvider()
        val isPinned = message.id == pinnedIdProvider()

        if (message.isNarrator) {
            b.contentRow.visibility = View.GONE
            b.txtNarrator.visibility = View.VISIBLE
            b.txtNarrator.typeface = ThemeUtils.typefaceFor(settings.fontFamily)
            b.txtNarrator.text = if (isPinned) "📌 ${message.text}" else message.text
            b.txtNarrator.setOnLongClickListener {
                onLongPress(message, holder.bindingAdapterPosition)
                true
            }
            return
        } else {
            b.contentRow.visibility = View.VISIBLE
            b.txtNarrator.visibility = View.GONE
        }

        b.txtMessage.text = message.text
        b.txtMessage.visibility = if (message.text.isEmpty()) View.GONE else View.VISIBLE
        b.txtMessage.typeface = ThemeUtils.typefaceFor(settings.fontFamily)
        b.txtMessage.setTextSize(TypedValue.COMPLEX_UNIT_SP, settings.fontSizeSp)

        val timeText = timeFormat.format(Date(message.timestamp))
        val timeLabel = if (message.edited) {
            "$timeText · ${b.root.context.getString(R.string.edited_label)}"
        } else {
            timeText
        }
        b.txtTime.text = if (isPinned) "📌 $timeLabel" else timeLabel

        val rowParams = b.contentRow.layoutParams as? android.widget.LinearLayout.LayoutParams

        if (message.isSelf) {
            b.bubble.background = ThemeUtils.bubbleDrawable(b.root.context, settings.bubbleSelfColor)
            rowParams?.gravity = Gravity.END
            b.imgAvatarLeft.visibility = View.GONE
            b.imgAvatarRight.visibility = View.VISIBLE
            b.imgAvatarRight.loadAvatarOrInitials(selfAvatarProvider(), selfNameProvider(), "self")
        } else {
            b.bubble.background = ThemeUtils.bubbleDrawable(b.root.context, settings.bubbleOtherColor)
            rowParams?.gravity = Gravity.START
            b.imgAvatarRight.visibility = View.GONE
            b.imgAvatarLeft.visibility = View.VISIBLE
            b.imgAvatarLeft.loadAvatarOrInitials(otherAvatarProvider(), otherNameProvider(), otherSeed)
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

        b.photoFrame.visibility = View.GONE
        b.imgPlayOverlay.visibility = View.GONE
        b.mediaFileRow.visibility = View.GONE
        if (!message.mediaPath.isNullOrEmpty()) {
            when (message.mediaType) {
                "photo" -> {
                    val bmp = BitmapUtils.decodeSampledFromFile(message.mediaPath!!, 800)
                    if (bmp != null) {
                        b.imgMessagePhoto.setImageBitmap(bmp)
                        b.photoFrame.visibility = View.VISIBLE
                    }
                }
                "video" -> {
                    val thumb = BitmapUtils.videoThumbnail(message.mediaPath!!)
                    if (thumb != null) {
                        b.imgMessagePhoto.setImageBitmap(thumb)
                        b.photoFrame.visibility = View.VISIBLE
                        b.imgPlayOverlay.visibility = View.VISIBLE
                    } else {
                        b.mediaFileRow.visibility = View.VISIBLE
                        b.imgMediaIcon.setImageResource(R.drawable.ic_video)
                        b.txtMediaName.text = File(message.mediaPath!!).name
                    }
                }
                "audio", "document" -> {
                    b.mediaFileRow.visibility = View.VISIBLE
                    b.imgMediaIcon.setImageResource(
                        if (message.mediaType == "audio") R.drawable.ic_audio else R.drawable.ic_document
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
