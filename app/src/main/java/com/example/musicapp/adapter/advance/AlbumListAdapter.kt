package com.example.musicapp.adapter.advance

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.musicapp.R
import com.example.musicapp.databinding.ItemAlbumSquareBinding
import com.example.service.model.Album

class AlbumListAdapter(private val onClickItem: (Album) -> Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val listData = mutableListOf<Album>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding =
            ItemAlbumSquareBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemAlbumViewHolder(binding)
    }

    override fun getItemCount(): Int = listData.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemAlbumViewHolder) {
            holder.bind(position)
        }
    }

    fun updateData(list: List<Album>) {
        listData.clear()
        listData.addAll(list)
        notifyDataSetChanged()
    }

    inner class ItemAlbumViewHolder(private val binding: ItemAlbumSquareBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            val album = listData[position]
            binding.albumName.text = album.albumName
            val quantityString = binding.root.context.resources.getQuantityString(
                R.plurals.song_count, album.numberSong.toInt(), album.numberSong.toInt(), album.numberSong.toInt()
            )
            binding.albumTrackCount.text = quantityString

            binding.root.setOnClickListener {
                onClickItem(album)
            }
        }
    }
}