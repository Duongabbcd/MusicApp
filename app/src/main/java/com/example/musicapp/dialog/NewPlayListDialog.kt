package com.example.musicapp.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.example.musicapp.R
import com.example.musicapp.databinding.DialogNameBinding
import com.example.service.model.Audio
import com.example.service.model.MediaObject
import com.example.service.utils.PlaylistUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NewPlayListDialog(context: Context) : Dialog(context) {
    private val binding by lazy { DialogNameBinding.inflate(layoutInflater) }
    private var onReload: (String) -> Unit = {}
    private var data: Audio? = null
    private val listData = mutableListOf<Audio>()

    init {
        setContentView(binding.root)
        window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    fun setData(audio: Audio?, dataList: MutableList<Audio> = mutableListOf()) {
        data = audio
        listData.clear()
        listData.addAll(dataList)
    }

    fun setOnReloadData(reload: (String) -> Unit) {
        onReload = reload
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.textTitle.text = context.getString(R.string.create_new_playlist)
        binding.editTextName.hint = context.getString(R.string.enter_playlist_name)
        binding.editTextName.requestFocus()

        binding.editTextName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                //do nothing
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    binding.imageClose.isGone = true
                } else binding.imageClose.isVisible = true
            }

            override fun afterTextChanged(s: Editable?) {
                //do nothing
            }

        })
        binding.imageClose.setOnClickListener {
            binding.editTextName.setText("")
        }
        binding.textOk.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                val title = binding.editTextName.text.toString()
                if (title.isEmpty() || title == " ") {
                    binding.editTextName.error = context.getString(R.string.title_must_be_not_empty)
                } else if (PlaylistUtils.getInstance(context)?.checkNameExists(title) == true) {
                    binding.editTextName.error =  context.getString(R.string.title_is_exist)
                } else {
                    withContext(Dispatchers.IO) {
                        if (data != null) {
                            val listData = listOf(data!!)
                            PlaylistUtils.getInstance(context)
                                ?.newPlaylist(title, listData, context)
                        } else {
                            PlaylistUtils.getInstance(context)
                                ?.newPlaylist(title, listData, context)
                        }
                    }
                    onReload(title)
                    dismiss()
                }
            }

        }
        binding.textCancel.setOnClickListener {
            dismiss()
        }
    }

}