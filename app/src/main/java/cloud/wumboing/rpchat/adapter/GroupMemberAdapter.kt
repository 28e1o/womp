package cloud.wumboing.rpchat.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cloud.wumboing.rpchat.data.GroupMember
import cloud.wumboing.rpchat.databinding.ItemGroupMemberRowBinding
import cloud.wumboing.rpchat.util.clipToCircle
import cloud.wumboing.rpchat.util.loadAvatarOrInitials

class GroupMemberAdapter(
    private val items: MutableList<GroupMember>,
    private val onClick: (GroupMember) -> Unit
) : RecyclerView.Adapter<GroupMemberAdapter.VH>() {

    inner class VH(val binding: ItemGroupMemberRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemGroupMemberRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.imgMemberRowAvatar.clipToCircle()
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val member = items[position]
        holder.binding.txtMemberRowName.text = member.name
        holder.binding.imgMemberRowAvatar.loadAvatarOrInitials(member.avatarPath, member.name, member.id)
        holder.binding.root.setOnClickListener { onClick(member) }
    }

    override fun getItemCount() = items.size

    fun update(newItems: List<GroupMember>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
