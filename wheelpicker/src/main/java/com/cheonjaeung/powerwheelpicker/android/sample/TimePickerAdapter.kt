package com.cheonjaeung.powerwheelpicker.android.sample

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cheonjaeung.powerwheelpicker.android.databinding.NumberPickerBinding

class TimePickerAdapter(start: Int, end: Int) :
    RecyclerView.Adapter<TimePickerAdapter.ViewHolder>() {
    val items: List<Int> = (start..end).toList()
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(
            NumberPickerBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val number = items[position]
        holder.bind(number)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(private val binding: NumberPickerBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(number: Int) {
            binding.timeValueText.text = String.format("%02d", number)
            binding.timeValueText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 36f)
        }
    }
}