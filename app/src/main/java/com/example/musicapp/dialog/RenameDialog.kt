package com.example.musicapp.dialog

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.example.musicapp.R
import com.example.musicapp.activity.advance.PlaylistActivity
import com.example.musicapp.base.BaseActivity
import com.example.musicapp.databinding.DialogRenameBinding
import com.example.musicapp.util.Utils.hideKeyBoard
import com.example.service.model.Audio
import com.example.service.model.Playlist
import com.example.service.utils.PlaylistUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RenameDialog(context: Context) : Dialog(context) {
    private val binding by lazy { DialogRenameBinding.inflate(layoutInflater) }

    init {
        setContentView(binding.root)
        window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.editTextName.requestFocus()

        binding.editTextName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                //do nothing
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrEmpty()) {
                    binding.imageClose.isVisible = true
                } else binding.imageClose.isGone = true
            }

            override fun afterTextChanged(s: Editable?) {
                //do nothing
            }

        })
        binding.imageClose.setOnClickListener {
            binding.editTextName.setText("")
            context.hideKeyBoard(binding.editTextName)
        }
        binding.textCancel.setOnClickListener {
            dismiss()
        }
    }

    fun setDataForPlaylist(playlist: Playlist) {
        binding.textTitle.text = context.resources.getString(R.string.rename_playlist)
        binding.editTextName.hint = context.resources.getString(R.string.enter_playlist_name)
        binding.editTextName.setText(playlist.title)
        binding.editTextName.setSelection(playlist.title.length)

        binding.textOk.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                val title = binding.editTextName.text.toString()
                if (title.isEmpty() || title == " ") {
                    binding.editTextName.error = context.resources.getString(R.string.title_must_be_not_empty)
                } else if (PlaylistUtils.getInstance(context)!!.checkNameExists(title)) {
                    binding.editTextName.error = context.resources.getString(R.string.title_is_exist)
                } else {
                    playlist.title = title
                    CoroutineScope(Dispatchers.IO).launch {
                        PlaylistUtils.getInstance(context)?.savePlaylist(playlist)
                    }
                    context.sendBroadcast(Intent(PlaylistActivity.ACTION_UPDATE_DATA_PLAYLIST))
                    dismiss()
                }
            }

        }
    }

    fun setDataForAudio(audio: Audio) {
        binding.textTitle.text = context.resources.getString(R.string.create_new_playlist)
        binding.editTextName.hint = context.resources.getString(R.string.enter_playlist_name)
        audio.mediaObject?.title?.let {
            binding.editTextName.setText(it)
            binding.editTextName.setSelection(it.length)
        }



        binding.textOk.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                val title = binding.editTextName.text.toString()
                if (title.isEmpty()) {
                    binding.editTextName.error = context.resources.getString(R.string.title_must_be_not_empty)
                } else if (PlaylistUtils.getInstance(context)!!.checkNameExists(title)) {
                    binding.editTextName.error = context.resources.getString(R.string.title_is_exist)
                } else {
                    val intent = Intent(BaseActivity.ACTION_RENAME_FILE)
                    intent.putExtra(BaseActivity.KEY_ID_SONG, audio.mediaObject?.id.toString())
                    intent.putExtra(BaseActivity.KEY_PATH_SONG, audio.mediaObject?.path.toString())
                    intent.putExtra(BaseActivity.KEY_NAME_SONG, audio.mediaObject?.title.toString())
                    context.sendBroadcast(intent)
                    dismiss()
                }
            }

        }
    }

}