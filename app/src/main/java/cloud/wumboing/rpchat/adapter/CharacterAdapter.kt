package cloud.wumboing.rpchat.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cloud.wumboing.rpchat.data.Character
import cloud.wumboing.rpchat.databinding.ItemCharacterBinding
import cloud.wumboing.rpchat.util.ChatDateUtils
import cloud.wumboing.rpchat.util.clipToCircle
import cloud.wumboing.rpchat.util.loadAvatarOrInitials

class CharacterAdapter(
    private val items: MutableList<Character>,
    private val previewProvider: (String) -> String?,
    private val timeProvider: (String) -> Long?,
    private val onClick: (Character) -> Unit,
    private val onLongClick: (Character) -> Unit
) : RecyclerView.Adapter<CharacterAdapter.VH>() {

    inner class VH(val binding: ItemCharacterBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCharacterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.imgAvatar.clipToCircle()
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val character = items[position]
        holder.binding.txtName.text = character.name
        val draft = character.draftText
        holder.binding.txtPreview.text = if (!draft.isNullOrEmpty()) {
            "Draft: $draft"
        } else {
            previewProvider(character.id) ?: character.bio ?: ""
        }

        val timestamp = timeProvider(character.id)
        holder.binding.txtTime.text = if (timestamp != null) ChatDateUtils.formatChatTime(timestamp) else ""

        holder.binding.imgAvatar.loadAvatarOrInitials(character.avatarPath, character.name, character.id)

        holder.binding.root.setOnClickListener { onClick(character) }
        holder.binding.root.setOnLongClickListener {
            onLongClick(character)
            true
        }
    }

    override fun getItemCount() = items.size

    fun update(newItems: List<Character>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
