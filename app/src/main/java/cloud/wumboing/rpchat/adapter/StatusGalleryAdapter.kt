package cloud.wumboing.rpchat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cloud.wumboing.rpchat.databinding.ItemStatusPhotoBinding
import cloud.wumboing.rpchat.util.AvatarUtils
import cloud.wumboing.rpchat.util.BitmapUtils

class StatusGalleryAdapter(
    private val paths: List<String>,
    private val name: String,
    private val seed: String
) : RecyclerView.Adapter<StatusGalleryAdapter.VH>() {

    inner class VH(val binding: ItemStatusPhotoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemStatusPhotoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = maxOf(1, paths.size)

    override fun onBindViewHolder(holder: VH, position: Int) {
        val b = holder.binding
        val bmp = if (position < paths.size) BitmapUtils.decodeSampledFromFile(paths[position], 1200) else null

        if (bmp != null) {
            b.imgStatusPhoto.setImageBitmap(bmp)
            b.imgStatusPhoto.visibility = View.VISIBLE
            b.letterContainer.visibility = View.GONE
        } else {
            b.imgStatusPhoto.visibility = View.GONE
            b.letterContainer.visibility = View.VISIBLE
            b.letterContainer.setBackgroundColor(AvatarUtils.colorFor(seed))
            b.txtLetter.text = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        }
    }
}
