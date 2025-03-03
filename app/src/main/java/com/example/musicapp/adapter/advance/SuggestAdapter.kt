package com.example.musicapp.adapter.advance

import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.musicapp.R
import com.example.musicapp.activity.advance.search.SuggestItem
import com.example.musicapp.databinding.ItemSuggestSongBinding
import com.example.musicapp.util.Utils.getColorFromAttr

class SuggestAdapter(
    private val onGetSuggest: (String) -> Unit,
    private val onClickSuggest: (String) -> Unit
) : RecyclerView.Adapter<SuggestAdapter.SuggestViewHolder>() {
    private val dataList = mutableListOf<SuggestItem>()
    private var key = ""

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SuggestAdapter.SuggestViewHolder {
        val binding =
            ItemSuggestSongBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SuggestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SuggestAdapter.SuggestViewHolder, position: Int) {
        holder.setData(dataList[position])
    }

    override fun getItemCount(): Int = dataList.size

    fun updateData(list: List<SuggestItem>, newKey: String = "") {
        dataList.clear()
        dataList.addAll(list)
        println("updateData: $list")
        key = newKey
        notifyDataSetChanged()
    }

    inner class SuggestViewHolder(private val binding: ItemSuggestSongBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun setData(data: SuggestItem) {
            val context = binding.root.context

            val spannable = SpannableString(data.content)
            val startIndex = data.content.indexOf(key, ignoreCase = true)
            if (data.content.startsWith(key.trim())) {
                spannable.setSpan(
                    ForegroundColorSpan(
                        context.getColorFromAttr(R.attr.textColorHighlight)
                    ),
                    startIndex, startIndex + key.trim().length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            binding.songName.text = spannable

            if (data.type == SuggestItem.TYPE_HISTORY) {
                binding.songIcon.setImageResource(R.drawable.icon_unselected_clock)
            } else {
                binding.songIcon.setImageResource(R.drawable.icon_search_purple)
            }

            binding.downloadSongBtn.setOnClickListener {
                onGetSuggest(data.content)
            }
            binding.root.setOnClickListener {
                onClickSuggest(data.content)
            }
        }
    }
}