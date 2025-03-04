package com.example.musicapp.adapter

import android.graphics.PorterDuff
import com.example.service.model.Audio
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.example.musicapp.R
import com.example.musicapp.bottomsheet.BottomSheetMore
import com.example.musicapp.databinding.ItemSongDetailBinding
import com.example.musicapp.util.Utils.getColorFromAttr
import com.example.service.service.MusicPlayerRemote
import com.example.service.utils.DisplayMode

class SongPlayingAdapter() : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var songToRemove: Audio? = null
    private val listData = mutableListOf<Any>()
    var lastVisibleItemPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding =
            ItemSongDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemSongViewHolder(binding)

    }

    override fun getItemCount(): Int =
        listData.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemSongViewHolder) {
            holder.bind(position)
            if (position > lastVisibleItemPosition) {
                val animation =
                    AnimationUtils.loadAnimation(holder.itemView.context, R.anim.anim_zoom_in)
                holder.itemView.startAnimation(animation)
            }
        }
    }


    fun updateData(list: List<Any>) {
        println("SongPlayingAdapter: ${list[0]}")
        lastVisibleItemPosition = 9999
        listData.clear()
        listData.addAll(list)
        notifyDataSetChanged()
    }

    inner class ItemSongViewHolder(val binding: ItemSongDetailBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(position: Int) {
            val song = listData[position] as Audio
            binding.songDuration.text = song.timeSong
            binding.songArtist.text = song.artist
            binding.songName.text = song.mediaObject?.title

            binding.root.setOnClickListener {
                    MusicPlayerRemote.playSongAt(position)
                notifyDataSetChanged()
            }

            val context = binding.root.context

            binding.songAddMore.setOnClickListener {
                val bottomSheetMore = BottomSheetMore(context, DisplayMode.PLAYING_SONG)
                bottomSheetMore.setAudio(position, listData, this@SongPlayingAdapter)
                bottomSheetMore.show()
            }

            if (song == MusicPlayerRemote.getCurrentSong() && song.mediaObject?.path != null) {
                binding.songIcon.visibility = View.GONE
                binding.animation.visibility = View.VISIBLE

                if (MusicPlayerRemote.isPlaying()) {
                    binding.animation.playAnimation()
                } else {
                    binding.animation.pauseAnimation()
                }
                val color = ContextCompat.getColor(context, R.color.high_light_color)
                binding.songName.setTextColor(color)
                binding.songArtist.setTextColor(color)
                binding.songDuration.setTextColor(color)
                binding.songDot.setColorFilter(color, PorterDuff.Mode.SRC_IN)
                binding.animation.addValueCallback(
                    KeyPath("**"), LottieProperty.STROKE_COLOR
                ) {
                    color
                }

            } else {
                binding.songIcon.visibility = View.VISIBLE
                binding.animation.visibility = View.GONE

                val color = ContextCompat.getColor(context, R.color.purple_text)
                binding.songName.setTextColor(context.getColorFromAttr(R.attr.textColor))
                binding.songArtist.setTextColor(color)
                binding.songDuration.setTextColor(color)
                binding.songDot.setColorFilter(color, PorterDuff.Mode.SRC_IN)
            }

            binding.iconLine.visibility = View.GONE
        }
    }

    companion object {
        const val TYPE_SONG = 1
        const val TYPE_HEADER = 0
    }

}


