package com.example.musicapp.adapter.basic

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import com.example.musicapp.R
import com.example.musicapp.adapter.SongPlayingAdapter
import com.example.musicapp.bottomsheet.BottomSheetMore
import com.example.musicapp.databinding.ItemAlbumBinding
import com.example.musicapp.databinding.ItemAlbumHeaderBinding
import com.example.service.model.Album
import com.example.service.utils.DisplayMode

class AlbumAdapter(private val onClickItem: (Album) -> Unit) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val listData = mutableListOf<Any>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == SongPlayingAdapter.TYPE_HEADER) {
            val binding =
                ItemAlbumHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return AlbumHeaderViewHolder(binding)
        } else {
            val binding =
                ItemAlbumBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return AlbumItemViewHolder(binding)
        }
    }

    var lastVisibleItemPosition = -1

    override fun getItemCount(): Int = listData.size + 2

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is AlbumItemViewHolder) {
            holder.bind(position - 2)
            if (position - 2 > lastVisibleItemPosition) {
                val animation =
                    AnimationUtils.loadAnimation(holder.itemView.context, R.anim.anim_zoom_in)
                holder.itemView.startAnimation(animation)
            }
        } else if (holder is AlbumHeaderViewHolder) {
            holder.updateData(position)
        }
    }

    fun updateData(list: List<Any>) {
        lastVisibleItemPosition = 9999
        listData.clear()
        listData.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0 || position == 1) SongPlayingAdapter.TYPE_HEADER else SongPlayingAdapter.TYPE_SONG
    }

    inner class AlbumHeaderViewHolder(private val binding: ItemAlbumHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun updateData(position: Int) {
            val context = binding.root.context
            if (position == 0) {
                val quantityString = context.resources.getQuantityString(
                    R.plurals.album_count,
                    listData.size,
                    listData.size,
                    listData.size
                )
                binding.albumCount.text = quantityString
            }

            binding.albumSort.visibility = View.GONE
            binding.albumCount.visibility = if (position == 0) View.VISIBLE else View.GONE
            binding.playAllButton.visibility = View.GONE
            binding.shuffleButton.visibility = View.GONE

        }

    }

    inner class AlbumItemViewHolder(private val binding: ItemAlbumBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            val context = binding.root.context
            val data = listData[position] as Album
            binding.root.setOnClickListener {
                onClickItem(data)
            }
            binding.albumMoreIcon.setOnClickListener {
                val bottomSheetMore = BottomSheetMore(context, DisplayMode.ALBUM )
                bottomSheetMore.setAlbum(data)
                bottomSheetMore.show()
            }
            binding.albumName.text = data.albumName

            val quantityString = context.resources.getQuantityString(
                R.plurals.song_count,
                data.numberSong.toInt(),
                data.numberSong.toInt(),
                data.numberSong.toInt(),
            )
            binding.albumSongCount.text = quantityString
        }

    }
}