package com.example.musicapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import com.example.musicapp.R
import com.example.musicapp.bottomsheet.BottomSheetDetailPlaylist
import com.example.musicapp.databinding.ItemPlaylistBinding
import com.example.service.model.Playlist
import com.example.service.utils.PlaylistUtils

class PlaylistAdapter(private val onClickItem: (Playlist) -> Unit) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val listData = mutableListOf<Any>()
    var lastVisibleItemPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding =
            ItemPlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlaylistViewHolder(binding)
    }

    override fun getItemCount(): Int = listData.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is PlaylistViewHolder) {
            holder.setData(position)
            if (position > lastVisibleItemPosition) {
                val animation =
                    AnimationUtils.loadAnimation(holder.itemView.context, R.anim.anim_zoom_in)
                holder.itemView.startAnimation(animation)
            }
        }
    }

    fun updateData(list: List<Any>) {
        lastVisibleItemPosition = 9999
        listData.clear()
        listData.addAll(list)
        notifyDataSetChanged()
    }

    inner class PlaylistViewHolder(private val binding: ItemPlaylistBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun setData(position: Int) {
            val context = binding.root.context
            val data = listData[position] as Playlist

            binding.playlistName.text = data.title
            val quantityString = binding.root.context.resources.getQuantityString(
                R.plurals.song_count, data.tracks.size, data.tracks.size, data.tracks.size
            )
            binding.playlistSongCount.text = quantityString
            binding.root.setOnClickListener {
                onClickItem(data)
            }

            if (data.id == PlaylistUtils.favouriteId()) {
               binding.favSongIcon.visibility =View.VISIBLE
               binding.songIcon.visibility =View.GONE
            } else {
                binding.favSongIcon.visibility =View.GONE
                binding.songIcon.visibility =View.VISIBLE
            }

            binding.playlistMoreFunctions.setOnClickListener {
                val dialogName = BottomSheetDetailPlaylist(context)
                dialogName.setData(selected = null, position, listData)
                dialogName.show()
            }

            binding.playlistAdd.visibility = View.GONE
            binding.playlistMoreFunctions.visibility = View.VISIBLE
        }

    }
}