package com.example.musicapp.adapter.basic

import android.content.Context
import android.graphics.PorterDuff
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.example.musicapp.R
import com.example.musicapp.adapter.SongPlayingAdapter
import com.example.musicapp.bottomsheet.BottomSheetMore
import com.example.musicapp.databinding.ItemAlbumHeaderBinding
import com.example.musicapp.databinding.ItemSongBinding
import com.example.musicapp.dialog.SortAudioDialog
import com.example.musicapp.util.Constant
import com.example.musicapp.util.Utils.getColorFromAttr
import com.example.service.model.Artist
import com.example.service.model.Audio
import com.example.service.service.MusicPlayerRemote
import com.example.service.utils.DisplayMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SongAdapter(
    private val displayMode: DisplayMode = DisplayMode.DETAIL,
    private val isDownloadSong: Boolean = false,
    private val setOnClickListener: (String) -> Unit,
    private val onClickItem: (Artist) -> Unit,
    private val onLongClickListener: (Audio, isSelected: Boolean) -> Unit,
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var setUpMultipleSelect: Boolean = false
    private var items: ArrayList<Any> = arrayListOf()
    private var keys = ""
    private lateinit var context: Context

    var lastVisibleItemPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        context = parent.context
        if (viewType == SongPlayingAdapter.TYPE_HEADER) {
            val artistHeader =
                ItemAlbumHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ArtistHeaderViewHolder(artistHeader)
        }
        val itemSongBinding =
            ItemSongBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SongViewHolder(itemSongBinding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is SongViewHolder) {
            val newPos = getItemByPosition(position)
            when (displayMode) {
                DisplayMode.AUDIO, DisplayMode.ALBUM -> {
                    holder.bind(newPos)
                }

                DisplayMode.ARTIST -> {
                    holder.bindArtist(newPos)
                }

                else -> {
                    holder.bind(newPos)
                }
            }

            if (position > lastVisibleItemPosition) {
                val animation =
                    AnimationUtils.loadAnimation(holder.itemView.context, R.anim.anim_zoom_in)
                holder.itemView.startAnimation(animation)
            }

        } else if (holder is ArtistHeaderViewHolder) {
            holder.bind(displayMode, position)
        }
    }

    private fun getItemByPosition(position: Int): Int {
        return if(isDownloadSong) {
            position
        } else {
            when (displayMode) {
                in listOf(
                    DisplayMode.ARTIST,
                    DisplayMode.ALBUM,
                ) -> {
                    position - 1
                }

                DisplayMode.AUDIO -> {
                    position - 2

                }

                else -> {
                    position
                }
            }
        }

    }

    override fun getItemViewType(position: Int): Int {
        return if ((displayMode in listOf(DisplayMode.ARTIST, DisplayMode.ALBUM) && position == 0) ||
            (displayMode == DisplayMode.AUDIO && position in listOf(0, 1)) && !isDownloadSong
        ) {
            SongPlayingAdapter.TYPE_HEADER
        } else SongPlayingAdapter.TYPE_SONG
    }


    override fun getItemCount(): Int = getSize()


    private fun getSize(): Int {
        return if(isDownloadSong) {
            items.size
        } else {
            when (displayMode) {
                in listOf(
                    DisplayMode.ARTIST,
                    DisplayMode.ALBUM
                )
                -> {
                    items.size + 1
                }

                DisplayMode.AUDIO -> {
                    items.size + 2
                }

                else -> {
                    items.size

                }
            }
        }
    }

    fun updateData(list: List<Any>, text: String = "") {
        lastVisibleItemPosition = 9999
        items.clear()
        items.addAll(list)
        keys = text
        val startIndex = if (displayMode == DisplayMode.AUDIO) 1 else 0
        notifyItemRangeChanged(startIndex, getSize())
    }

    inner class ArtistHeaderViewHolder(private val binding: ItemAlbumHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val context: Context = binding.root.context

        fun bind(displayMode: DisplayMode, position: Int) {
            if (position == 0) {
                when (displayMode) {
                    DisplayMode.ARTIST -> {
                        binding.linearLayout.visibility = View.GONE
                        binding.albumCount.visibility = View.VISIBLE
                        binding.albumSort.visibility = View.GONE
                    }

                    DisplayMode.AUDIO -> {
                        binding.linearLayout.visibility = View.VISIBLE
                        binding.albumCount.visibility = View.GONE
                        binding.albumSort.visibility = View.GONE
                    }

                    else -> {}
                }
            }

            val albumCountString = if (displayMode == DisplayMode.ARTIST) {
                context.resources.getQuantityString(
                    R.plurals.artist_count, items.size, items.size, items.size
                )
            } else context.resources.getQuantityString(
                R.plurals.song_count, items.size, items.size, items.size
            )

            binding.albumCount.text = albumCountString

            //search song
            binding.searchBarIcon.visibility = View.GONE
            binding.searchBarIconLeft.visibility = View.VISIBLE
            try {
                binding.searchBarText.addTextChangedListener { s ->
                    val text = s.toString()
                    if (text.isNotEmpty()) {
                        binding.searchBarIcon.visibility = View.VISIBLE
                        binding.searchBarIconLeft.visibility = View.GONE
                        binding.searchBarIcon.setImageResource(R.drawable.icon_close)
                        setOnClickListener(s.toString())
                    } else {
                        binding.searchBarIcon.setImageResource(R.drawable.icon_search_purple)
                        setOnClickListener("")
                    }

                }
            } catch (e: Exception) {
                e.printStackTrace()
            }


            binding.searchBarIcon.setOnClickListener {
                binding.searchBarText.setText("")
                setOnClickListener("")
            }

            if (displayMode == DisplayMode.ARTIST || displayMode == DisplayMode.AUDIO) {
                binding.shuffleButton.visibility = View.GONE
                binding.playAllButton.visibility = View.GONE
            } else {
                binding.shuffleButton.visibility = View.VISIBLE
                binding.playAllButton.visibility = View.VISIBLE
            }

            binding.apply {
                albumSort.setOnClickListener {
                    val dialogName = SortAudioDialog(context, isDownloadSong)
                    dialogName.show()
                }
                playAllButton.setOnClickListener {
                    MusicPlayerRemote.openQueue(ArrayList(items), 0, true)
                    notifyDataSetChanged()
                }
                shuffleButton.setOnClickListener {
                    MusicPlayerRemote.openAndShuffleQueue(ArrayList(items), true)
                    notifyDataSetChanged()
                }
            }
        }
    }

    inner class SongViewHolder(private val binding: ItemSongBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val context: Context = binding.root.context
        fun bind(position: Int) {
            val song = items[position] as Audio
            var isSelected = false
            val string =
                song.mediaObject?.path?.let { it.substring(it.lastIndexOf('/') + 1, it.length) }
            val songTitle = song.mediaObject?.title
            val title = if (songTitle.isNullOrEmpty()) string else songTitle
            binding.songDuration.text = song.timeSong
            binding.songArtist.text =
                if (!song.artist[0].isLetter() || song.artist.contains(
                        Constant.UNKNOWN_STRING,
                        true
                    )
                ) context.resources.getString(R.string.unknown_artist) else song.artist
            binding.songName.text = title

            binding.root.setOnClickListener {
                MusicPlayerRemote.openQueue(ArrayList(items), position, true)
                CoroutineScope(Dispatchers.Main).launch {
                    delay(500)
                    notifyItemChanged(position)
                }
            }

//            binding.root.setOnLongClickListener {
//                setUpMultipleSelect = !setUpMultipleSelect
//                notifyDataSetChanged()
//                true
//            }

//            if (setUpMultipleSelect) {
//                binding.songAddMore.visibility = View.INVISIBLE
//                binding.songSelect.visibility = View.VISIBLE
//            } else {
//                binding.songAddMore.visibility = View.VISIBLE
//                binding.songSelect.visibility = View.GONE
//            }

            binding.songSelect.setOnClickListener {
                isSelected = !isSelected
                if (isSelected) {
                    binding.songSelect.setImageResource(R.drawable.icon_selected)
                    onLongClickListener(song, true)
                } else {
                    binding.songSelect.setImageResource(R.drawable.icon_unselected)
                    onLongClickListener(song, false)
                }

            }

            binding.songAddMore.setOnClickListener {
                val bottomSheetMore = BottomSheetMore(context, DisplayMode.AUDIO)
                bottomSheetMore.setAudio(position, items, this@SongAdapter)
                bottomSheetMore.show()
            }

            val currentSong = MusicPlayerRemote.getCurrentSong()
            if (song.mediaObject?.path == currentSong.mediaObject?.path && song.mediaObject?.path != null) {
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
            val startIndex = title?.indexOf(keys, ignoreCase = true) ?: 0
            if (startIndex >= 0) {
                spannable.setSpan(
                    ForegroundColorSpan(
                        context.getColorFromAttr(R.attr.textColorHighlight)
                    ),
                    startIndex, startIndex + keys.trim().length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            binding.songName.text = spannable

        }

        fun bindArtist(position: Int) {
            val artist = items[position] as Artist
            binding.root.setOnClickListener {
                onClickItem(artist)
            }
            binding.frameSongIcon.visibility = View.GONE

            if (!artist.nameArtist[0].isLetter() || artist.nameArtist.contains(
                    Constant.UNKNOWN_STRING,
                    true
                )
            ) {
                binding.songName.text = context.resources.getString(R.string.unknown_artist)
                binding.artistIcon.visibility = View.GONE
                binding.anonymousIcon.visibility = View.VISIBLE
            } else {
                binding.artistIcon.visibility = View.VISIBLE.also {
                    binding.artistName.text = artist.nameArtist[0].toString().uppercase()
                }
                binding.songName.text = artist.nameArtist
                binding.anonymousIcon.visibility = View.GONE
            }

            val albumCountString = context.resources.getQuantityString(
                R.plurals.album_count, artist.numberOfAlbum.toInt(), artist.numberOfAlbum.toInt()
            )
            binding.songArtist.text = albumCountString

            val songCountString = context.resources.getQuantityString(
                R.plurals.song_count, artist.numberSong.toInt(),
                artist.numberSong.toInt(),
                artist.numberSong.toInt()
            )
            binding.songDuration.text = songCountString
            binding.songAddMore.setOnClickListener {
                val bottomSheetMore = BottomSheetMore(context, DisplayMode.ARTIST)
                bottomSheetMore.setArtist(position, items)
                bottomSheetMore.show()
            }
            val color = ContextCompat.getColor(context, R.color.purple_text)
            binding.songArtist.setTextColor(color)
            binding.songDuration.setTextColor(color)
            binding.songDot.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        }

    }

}