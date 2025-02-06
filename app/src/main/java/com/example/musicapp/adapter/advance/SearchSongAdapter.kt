package com.example.musicapp.adapter.advance

import android.app.Activity
import android.content.Context
import android.graphics.PorterDuff
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.example.musicapp.R
import com.example.musicapp.activity.MainActivity
import com.example.musicapp.databinding.ItemDownloadSongBinding
import com.example.musicapp.util.Constant
import com.example.musicapp.util.Utils
import com.example.musicapp.util.Utils.getColorFromAttr
import com.example.service.download.service.DownloadManagerService
import com.example.service.model.Audio
import com.example.service.service.MusicPlayerRemote
import com.example.service.utils.MDManager
import com.music.searchapi.ApiServices
import com.music.searchapi.callback.GetMusicLinkCallback
import com.music.searchapi.`object`.VideoEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Dispatcher
import java.lang.Exception

class SearchSongAdapter(private val activity: Activity) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val items = mutableListOf<Any>()
    private var keys = ""
    var lastVisibleItemPosition = -1

    private val TYPE_NORMAL = 1
    private val TYPE_ADMOB = 2
    private val TYPE_TOPON = 3

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val itemDownloadSongBinding =
            ItemDownloadSongBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SongViewHolder(itemDownloadSongBinding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is SongViewHolder) {
            holder.bind(position)

        }
    }

    override fun getItemCount(): Int = items.size

    fun getCurrentList() = items

    fun updateData(list: List<Any>, newKey: String = "") {
        lastVisibleItemPosition = 9999
        items.clear()
        items.addAll(list)
        keys = newKey
        notifyDataSetChanged()
    }


    inner class SongViewHolder(private val binding: ItemDownloadSongBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val context: Context = binding.root.context
        val currentSong = MusicPlayerRemote.getCurrentSong()
        private lateinit var onlineSong: Audio
        fun bind(position: Int) {
            onlineSong = items[position] as Audio
            val title = onlineSong.mediaObject!!.title
            binding.songDuration.text = Utils.formatDuration(onlineSong.timeCount)
            val artist = if (!onlineSong.artist[0].isLetter() || onlineSong.artist.contains(
                    Constant.UNKNOWN_STRING,
                    true
                )
            ) context.resources.getString(R.string.unknown_artist) else onlineSong.artist
            binding.songArtist.text = artist
            binding.songName.text = title

            binding.root.setOnClickListener {
                playSong()
            }

            if (onlineSong.mediaObject?.path == currentSong.mediaObject?.path && onlineSong.mediaObject?.path != null) {
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
                val unselectedColor = context.getColorFromAttr(R.attr.textColor)
                binding.songName.setTextColor(unselectedColor)
                binding.songArtist.setTextColor(color)
                binding.songDuration.setTextColor(color)
                binding.songDot.setColorFilter(color, PorterDuff.Mode.SRC_IN)
            }

            val spannable = SpannableString(title)
            val startIndex = title.indexOf(keys, ignoreCase = true)
            if (startIndex >= 0) {
                spannable.setSpan(
                    ForegroundColorSpan(
                        context.getColorFromAttr(R.attr.textColorHighlight)
                    ),
                    startIndex, startIndex + keys.trim().length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            binding.songName.text = spannable

            binding.downloadSongBtn.setOnClickListener {
                downloadSong()
            }

        }

        private fun playSong() {
            binding.apply {
                progressLoading.isVisible = true
                downloadSongBtn.isVisible = false
                val filePath = onlineSong.mediaObject?.path
                if (filePath != "" && filePath != null && filePath != "null") {
                    MusicPlayerRemote.openQueue(arrayListOf(onlineSong), 0, true)
                    progressLoading.isVisible = false
                    downloadSongBtn.isVisible = true
                    notifyDataSetChanged()
                } else {
                    val videoEntity = onlineSong.videoEntity as VideoEntity
                    CoroutineScope(Dispatchers.IO).launch {
                        ApiServices.getLink(context, videoEntity, object : GetMusicLinkCallback {
                            override fun a(streamLink: String) {
                                onlineSong.mediaObject?.path = streamLink
                                onlineSong.strLinkDownload = streamLink
                                CoroutineScope(Dispatchers.Main).launch {
                                    MusicPlayerRemote.openQueue(arrayListOf(onlineSong), 0, true)
                                    progressLoading.isVisible = false
                                    downloadSongBtn.isVisible = true
                                    notifyDataSetChanged()
                                }
                            }

                            override fun a(p0: Exception?) {
                                TODO("Not yet implemented")
                            }

                        })
                    }
                }
            }
        }

        private fun downloadSong() {
            binding.apply {
                val filePath = onlineSong.mediaObject?.path
                progressLoading.isVisible = true
                downloadSongBtn.isVisible = false
                if (filePath != "" && filePath != null && filePath != "null") {
                    if (DownloadManagerService.listUrlDownloading.contains(onlineSong.mediaObject?.path)) {
                        MDManager.getInstance(context)?.showMessage(
                            context,
                            context.resources.getString(R.string.download_song_notification, onlineSong.mediaObject?.title)
                        )
                    } else {
                        MainActivity.startMission(
                            onlineSong.mediaObject!!.path, onlineSong.mediaObject!!.title,
                            System.currentTimeMillis().toString(),
                            onlineSong.videoType, onlineSong.ccmixterReferrer, context
                        )
                    }
                    progressLoading.isVisible = false
                    downloadSongBtn.isVisible = true
                } else {
                    CoroutineScope(Dispatchers.IO).launch {
                        ApiServices.getLink(
                            context,
                            onlineSong.videoEntity as VideoEntity,
                            object : GetMusicLinkCallback {
                                override fun a(streamLink: String) {
                                    onlineSong.mediaObject?.path = streamLink
                                    onlineSong.strLinkDownload = streamLink
                                    CoroutineScope(Dispatchers.Main).launch {
                                        if (DownloadManagerService.listUrlDownloading.contains(
                                                onlineSong.mediaObject?.path
                                            )
                                        ) {
                                            MDManager.getInstance(context)?.showMessage(
                                                context,
                                                context.resources.getString(R.string.download_song_notification, onlineSong.mediaObject?.title)
                                            )
                                        } else {
                                            MainActivity.startMission(
                                                onlineSong.mediaObject!!.path,
                                                onlineSong.mediaObject!!.title,
                                                System.currentTimeMillis().toString(),
                                                onlineSong.videoType,
                                                onlineSong.ccmixterReferrer, context
                                            )
                                        }
                                        progressLoading.isVisible = false
                                        downloadSongBtn.isVisible = true
                                    }
                                }

                                override fun a(p0: Exception?) {
                                    TODO("Not yet implemented")
                                }

                            })
                    }
                }
            }
        }

    }


}

data class SuggestItem(val content: String, val type: Int) {
    companion object {
        const val TYPE_HISTORY = 1
        const val TYPE_ONLINE = 2
    }
}