package com.example.musicapp.adapter.advance

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.musicapp.databinding.ItemLanguageBinding

class LanguageAdapter(private val onClickItem: (LanguageItem) -> Unit) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val allLanguages = mutableListOf<LanguageItem>()
    private var currentLanguage = LanguageItem()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding =
            ItemLanguageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LanguageViewHolder(binding)
    }

    override fun getItemCount(): Int = allLanguages.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is LanguageViewHolder) {
            holder.bind(position)
        }
    }

    fun updateData(listData: List<LanguageItem>, selected: LanguageItem) {
        allLanguages.clear()
        allLanguages.addAll(listData)
        currentLanguage = selected
        notifyDataSetChanged()
    }

    inner class LanguageViewHolder(private val binding: ItemLanguageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            val language = allLanguages[position]

            binding.languageName.text = language.name
            if (language.code == currentLanguage.code) {
                binding.selectedIcon.visibility = View.VISIBLE
            } else binding.selectedIcon.visibility = View.GONE

            binding.root.setOnClickListener {
                onClickItem(language)
            }
        }
    }
}

data class LanguageItem(val code: String = "", val name: String = "")