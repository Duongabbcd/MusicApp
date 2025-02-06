package com.example.musicapp.adapter.basic

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.musicapp.R
import com.example.musicapp.databinding.ItemPlaylistBinding
import com.example.service.model.Playlist
import com.example.service.utils.PlaylistUtils

class AddPlaylistAdapter( private val onClickItem: (Int) -> Unit) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val listData = mutableListOf<Any>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding =
            ItemPlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlaylistViewHolder(binding)
    }

    override fun getItemCount(): Int = listData.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is PlaylistViewHolder) {
            holder.setData(position)
        }
    }

    fun updateData(list: List<Any>?) {
        listData.clear()
        list?.let {
            listData.addAll(it)
        }
        notifyDataSetChanged()
    }

    inner class PlaylistViewHolder(private val binding: ItemPlaylistBinding) : RecyclerView.ViewHolder(binding.root) {
        fun setData(position: Int) {
           val context = binding.root.context
           val data = listData[position] as Playlist
           binding.playlistName.text = data.title
            val quantityString =
                context.resources.getQuantityString(R.plurals.song_count, data.tracks.size, data.tracks.size, data.tracks.size)
            binding.playlistSongCount.text = quantityString

            binding.root.setOnClickListener {
                val currentPlaylistId = data.id ?: -1
                onClickItem(currentPlaylistId)
            }

            if (data.id == PlaylistUtils.favouriteId()) {
                binding.favSongIcon.visibility =View.VISIBLE
                binding.songIcon.visibility =View.GONE
            } else {
                binding.favSongIcon.visibility =View.GONE
                binding.songIcon.visibility =View.VISIBLE
            }

            binding.playlistAdd.visibility = View.VISIBLE
            binding.playlistMoreFunctions.visibility =  View.GONE
        }
    }
}