package com.example.musicapp.adapter.advance

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.musicapp.R
import com.example.musicapp.databinding.ItemSettingBinding
import com.example.service.utils.PreferenceUtil

class SettingAdapter(private val onClickItem: (Setting) -> Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val allSettingOptions =
        listOf(Setting.THEMES, Setting.LANGUAGES, Setting.SHARE, Setting.RATING)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ItemSettingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SettingViewHolder(binding)
    }

    override fun getItemCount(): Int = allSettingOptions.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is SettingViewHolder) {
            holder.bindData(position)
        }
    }

    inner class SettingViewHolder(private val binding: ItemSettingBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bindData(position: Int) {
            val context = binding.root.context
            val item = allSettingOptions[position]
            val currentTheme = PreferenceUtil.getInstance(context)?.let {
                it.getTheme(context)
            } ?: PreferenceUtil.DARK_THEME

            binding.apply {
                val icon = when(item) {
                    Setting.THEMES -> R.drawable.icon_theme
                    Setting.LANGUAGES -> R.drawable.icon_globe
                    Setting.SHARE -> R.drawable.icon_white_share
                    Setting.RATING -> R.drawable.icon_white_rate
                    else -> R.drawable.icon_white_rate
                }
                settingIcon.setImageResource(icon)
                settingTitle.text = when(item) {
                    Setting.THEMES -> context.getString(R.string.app_theme_title)
                    Setting.LANGUAGES -> context.getString(R.string.language_title)
                    Setting.SHARE -> context.getString(R.string.share_app_title)
                    Setting.RATING -> context.getString(R.string.rating_app_title)
                    else -> context.getString(R.string.rating_app_title)
                }
                settingSubTitle.text = when(item) {
                    Setting.THEMES -> context.getString(R.string.app_theme_sub_title)
                    Setting.LANGUAGES -> context.getString(R.string.language_sub_title)
                    Setting.SHARE -> context.getString(R.string.share_app_sub_title)
                    else -> context.getString(R.string.rating_app_sub_title)
                }

                root.setOnClickListener {
                    onClickItem(item)
                }
            }
        }
    }

}

enum class Setting {
    THEMES,
    LANGUAGES,
    SHARE,
    RATING,
    DEFAULT
}