package cloud.wumboing.rpchat.adapter

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cloud.wumboing.rpchat.data.Character
import cloud.wumboing.rpchat.databinding.ItemCharacterBinding
import java.io.File

class CharacterAdapter(
    private val items: MutableList<Character>,
    private val previewProvider: (String) -> String?,
    private val onClick: (Character) -> Unit,
    private val onLongClick: (Character) -> Unit
) : RecyclerView.Adapter<CharacterAdapter.VH>() {

    inner class VH(val binding: ItemCharacterBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCharacterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val character = items[position]
        holder.binding.txtName.text = character.name
        holder.binding.txtPreview.text = previewProvider(character.id) ?: ""

        character.avatarPath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                val bmp = BitmapFactory.decodeFile(path)
                if (bmp != null) holder.binding.imgAvatar.setImageBitmap(bmp)
            }
        }

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
