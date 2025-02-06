package com.example.musicapp.dialog

import android.app.Dialog
import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.text.Spannable
import android.text.SpannableString
import android.text.style.TextAppearanceSpan
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.musicapp.R
import com.example.musicapp.adapter.advance.LanguageItem
import com.example.musicapp.base.BaseActivity
import com.example.musicapp.databinding.DialogChangeLanguageBinding
import com.example.service.utils.PreferenceUtil

class LanguageDialog(context: Context) : Dialog(context) {
    private val binding by lazy { DialogChangeLanguageBinding.inflate(layoutInflater) }
    private var language = LanguageItem()
    init {
        setContentView(binding.root)
        window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    fun setData(item: LanguageItem) {
        language = item
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = language.name
        val text = context.getString(R.string.change_language_question, title)
        binding.apply {
            val spannable = SpannableString(text)
            val startIndex = text.indexOf(title, ignoreCase = true)
            val style = TextAppearanceSpan(context, R.style.CustomTextMediumStyle16sp)
            if (startIndex >= 0) {
                spannable.setSpan(
                    style,
                    startIndex, startIndex + title.trim().length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            clearQueueSubText.text = spannable
            textCancel.setOnClickListener {
                dismiss()
            }

            textOk.setOnClickListener {
                updateLanguage(language)
                dismiss()
            }
        }
    }

    private fun updateLanguage(selected: LanguageItem) {
        val code = selected.code
        PreferenceUtil.getInstance(context)?.setLanguage(code)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).applicationLocales =
                LocaleList.forLanguageTags(code)
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))
        }
        context.sendBroadcast(Intent(BaseActivity.ACTION_RECREATE))
    }
}