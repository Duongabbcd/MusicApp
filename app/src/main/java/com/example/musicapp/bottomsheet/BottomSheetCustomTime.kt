package com.example.musicapp.bottomsheet

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.cheonjaeung.powerwheelpicker.android.SampleItemEffector
import com.example.musicapp.R
import com.cheonjaeung.powerwheelpicker.android.sample.TimePickerAdapter
import com.example.musicapp.databinding.BottomSheetCustomTimeSleeperBinding
import com.example.musicapp.util.Utils.getColorFromAttr
import com.example.service.service.MusicPlayerRemote
import com.google.android.material.bottomsheet.BottomSheetDialog

class BottomSheetCustomTime(private val context: Context) : BottomSheetDialog(context) {
    private val binding by lazy { BottomSheetCustomTimeSleeperBinding.inflate(layoutInflater) }

    private lateinit var hourAdapter: TimePickerAdapter
    private lateinit var minuteAdapter: TimePickerAdapter

    init {
        setContentView(binding.root)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.apply {
            hourAdapter = TimePickerAdapter(0, 23)
            minuteAdapter = TimePickerAdapter(0, 59)

            hourWheelPicker.adapter = hourAdapter
            minuteWheelPicker.adapter = minuteAdapter


            hourWheelPicker.addItemEffector(
                SampleItemEffector(
                    hourWheelPicker,
                    ContextCompat.getColor(context, R.color.high_light_color),
                    context.getColorFromAttr(R.attr.iconAnonymous),  28f, 1f
                )
            )
            minuteWheelPicker.addItemEffector(
                SampleItemEffector(
                    hourWheelPicker,
                    ContextCompat.getColor(context, R.color.high_light_color),
                    context.getColorFromAttr(R.attr.iconAnonymous), 28f, 1f
                )
            )

            behavior.isDraggable = false
            root.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        behavior.isDraggable = true
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        behavior.isDraggable = false
                    }
                }
                false
            }

            doneButton.setOnClickListener {
                val hour = hourWheelPicker.currentPosition
                val minute = minuteWheelPicker.currentPosition

                if(hour == 0 && minute == 0) {
                    Toast.makeText(context, "Time must be greater than 0", Toast.LENGTH_SHORT).show()
                } else {
                    MusicPlayerRemote.startTimer(hour * 3600000L  + minute * 60000L)
                }
                this@BottomSheetCustomTime.dismiss()
            }
        }
    }

}