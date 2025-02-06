package com.example.musicapp.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import com.example.musicapp.databinding.DialogInformationBinding
import com.example.service.model.Audio
import com.example.service.utils.Utils
import java.io.File

class InformationDialog(context: Context) : Dialog(context) {
    private val binding by lazy { DialogInformationBinding.inflate(layoutInflater) }

    init {
        setContentView(binding.root)
        window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
    fun setData(audio: Audio) {
        binding.apply {
            infoNameValue.text = audio.mediaObject?.title ?: ""
            infoPathValue.text = audio.mediaObject?.path ?: ""
            infoGenresValue.text = audio.genres
            infoArtistValue.text = audio.artist
            infoAlbumValue.text = audio.albumName
            infoDurationValue.text = audio.timeSong
            infoSizeValue.text = Utils.showFileSize(File(audio.mediaObject!!.path).length())
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.apply {
            textCancel.setOnClickListener{
                dismiss()
            }

            textOk.setOnClickListener{
                dismiss()
            }
        }
    }
}