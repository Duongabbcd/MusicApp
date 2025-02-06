package com.example.musicapp.adapter.advance

import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import com.example.service.model.Audio
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.musicapp.R
import com.example.musicapp.databinding.ItemSongBinding
import com.example.musicapp.util.Utils.getColorFromAttr

class AddSongPlaylistAdapter(private val onClickItem: (List<Audio>) -> Unit) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val listSong = mutableListOf<Any>()
    private val listSongAdded = mutableListOf<Audio>()
    private var keys = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ItemSongBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AddSongViewHolder(binding)
    }

    override fun getItemCount(): Int = listSong.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is AddSongViewHolder) {
            holder.bindData(position)
        }
    }

    fun updateData(listData: List<Any>, listAdd: List<Audio>, key: String = "") {
        listSong.clear()
        listSong.addAll(listData)
        listSongAdded.clear()
        listSongAdded.addAll(listAdd)
        keys = key
        notifyDataSetChanged()
    }


    inner class AddSongViewHolder(private val binding: ItemSongBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bindData(position: Int) {
            val song = listSong[position] as Audio
            val title = song.mediaObject?.title ?: ""
            val context = binding.root.context

            binding.songAddMore.setImageResource(R.drawable.icon_unselected)
            binding.songDuration.text = song.timeSong
            binding.songArtist.text = song.artist
            binding.songName.text = song.mediaObject?.title

            binding.root.setOnClickListener {
                if (listSongAdded.contains(song)) {
                    listSongAdded.remove(song)
                } else {
                    listSongAdded.add(song)
                }
                onClickItem(listSongAdded.toList())
                notifyDataSetChanged()
            }

            if (listSongAdded.contains(song)) {
                binding.songAddMore.setImageResource(R.drawable.icon_selected)
            } else {
                binding.songAddMore.setImageResource(R.drawable.icon_unselected)

            }

            val spannable = SpannableString(title)
            val startIndex = title.indexOf(keys, ignoreCase = true)
            if (startIndex >= 0) {
                spannable.setSpan(
                    ForegroundColorSpan(context.getColorFromAttr(R.attr.textColorHighlight)),
                    startIndex, startIndex + keys.trim().length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            binding.songName.text = spannable

        }
    }

}

