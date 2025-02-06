package com.example.musicapp.bottomsheet

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.View
import com.example.musicapp.R
import com.example.musicapp.databinding.BottomSheetTimeOptionsBinding
import com.example.service.service.MusicPlayerRemote
import com.example.service.service.MusicService
import com.example.musicapp.util.Utils
import com.example.musicapp.util.Utils.getColorFromAttr
import com.google.android.material.bottomsheet.BottomSheetDialog

class BottomTimeSleepOptions(private val context: Context) : BottomSheetDialog(context) {
    private val binding by lazy { BottomSheetTimeOptionsBinding.inflate(layoutInflater) }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateUI()
        }
    }

    init {
        setContentView(binding.root)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateUI()
        val intentFilter = IntentFilter(MusicService.ACTION_UPDATE_SLEEP_TIMER)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                broadcastReceiver, intentFilter, Context.RECEIVER_EXPORTED
            )
        } else {
            context.registerReceiver(broadcastReceiver, intentFilter)
        }

        binding.apply {
            turnOffOption.setOnClickListener {
                Utils.setIconVisibility(
                    listOf(turnOffIcon),
                    listOf(tenMinuteIcon, twentyMinuteIcon, halfHourIcon, oneHourIcon)
                )
                Utils.setTextColor(
                    listOf(turnOffTitle),
                    listOf(tenMinuteTitle, twentyMinuteTitle, halfHourTitle, oneHourTitle),
                    R.color.high_light_color, R.color.purple_text
                )
            }

            tenMinuteOption.setOnClickListener {
                Utils.setIconVisibility(
                    listOf(tenMinuteIcon),
                    listOf(turnOffIcon, twentyMinuteIcon, halfHourIcon, oneHourIcon)
                )
                Utils.setTextColor(
                    listOf(tenMinuteTitle),
                    listOf(turnOffTitle, twentyMinuteTitle, halfHourTitle, oneHourTitle),
                    R.color.high_light_color, R.color.purple_text
                )
            }

            twentyMinuteOption.setOnClickListener {
                Utils.setIconVisibility(
                    listOf(twentyMinuteIcon),
                    listOf(turnOffIcon, tenMinuteIcon, halfHourIcon, oneHourIcon)
                )
                Utils.setTextColor(
                    listOf(twentyMinuteTitle),
                    listOf(turnOffTitle, tenMinuteTitle, halfHourTitle, oneHourTitle),
                    R.color.high_light_color, R.color.purple_text
                )
            }

            halfHourOption.setOnClickListener {
                Utils.setIconVisibility(
                    listOf(halfHourIcon),
                    listOf(turnOffIcon, tenMinuteIcon, twentyMinuteIcon, oneHourIcon)
                )
                Utils.setTextColor(
                    listOf(halfHourTitle),
                    listOf(turnOffTitle, tenMinuteTitle, twentyMinuteTitle, oneHourTitle),
                    R.color.high_light_color, R.color.purple_text
                )
            }

            halfHourOption.setOnClickListener {
                Utils.setIconVisibility(
                    listOf(oneHourIcon),
                    listOf(turnOffIcon, tenMinuteIcon, twentyMinuteIcon, halfHourIcon)
                )
                Utils.setTextColor(
                    listOf(oneHourTitle),
                    listOf(turnOffTitle, tenMinuteTitle, twentyMinuteTitle, halfHourTitle),
                    R.color.high_light_color, R.color.purple_text
                )
            }

            binding.customSettingOption.setOnClickListener {
                val dialog = BottomSheetCustomTime(context)
                dialog.show()
                dismiss()
            }

            countDownAction()

        }
    }

    private fun updateUI() {
        binding.apply {
            if (MusicPlayerRemote.timeLeft() > 0) {
                Utils.setIconVisibility(
                    listOf(customSettingIcon),
                    listOf(turnOffIcon, tenMinuteIcon, twentyMinuteIcon, halfHourIcon, oneHourIcon)
                )
                Utils.setTextColor(
                    listOf(customSettingTitle, customSettingButton),
                    listOf(
                        turnOffTitle,
                        tenMinuteTitle,
                        twentyMinuteTitle,
                        halfHourTitle,
                        oneHourTitle
                    ),
                    R.color.high_light_color, R.color.purple_text
                )
                customSettingIcon.setImageResource(R.drawable.icon_added)
                customSettingButton.text = com.example.service.utils.Utils.getDuration(MusicPlayerRemote.timeLeft())
                customSettingOption.setBackgroundColor(context.getColorFromAttr(R.attr.timeSleeperBackground))
                turnOffOption.setBackgroundColor(context.resources.getColor(R.color.transparent))
            } else {
                customSettingIcon.setImageResource(R.drawable.icon_next)
                customSettingButton.text = context.resources.getString(R.string.set)
                turnOffIcon.visibility = View.VISIBLE
                turnOffIcon.setImageResource(R.drawable.icon_added)
                Utils.setIconVisibility(
                    listOf(customSettingIcon, turnOffIcon) ,
                    listOf(turnOffIcon, tenMinuteIcon, twentyMinuteIcon, halfHourIcon, oneHourIcon)
                )
                turnOffOption.setBackgroundColor(context.getColorFromAttr(R.attr.timeSleeperBackground))
                customSettingIcon.setBackgroundColor(context.resources.getColor(R.color.transparent))
                Utils.setTextColor(
                    listOf(turnOffTitle),
                    listOf(
                        tenMinuteTitle,
                        twentyMinuteTitle,
                        halfHourTitle,
                        oneHourTitle,
                        customSettingTitle,
                        customSettingButton
                    ),
                    R.color.high_light_color, R.color.purple_text
                )
                customSettingOption.setBackgroundColor(context.resources.getColor(R.color.transparent))
            }
        }

    }

    private fun countDownAction() {
        binding.apply {
            tenMinuteOption.setOnClickListener {
                MusicPlayerRemote.startTimer(10 * 60000L)
                updateUI()
            }
            twentyMinuteOption.setOnClickListener {
                MusicPlayerRemote.startTimer(20 * 60000L)
                updateUI()
            }

            halfHourOption.setOnClickListener {
                MusicPlayerRemote.startTimer(30 * 60000L)
                updateUI()
            }

            oneHourOption.setOnClickListener {
                MusicPlayerRemote.startTimer(60 * 60000L)
                updateUI()
            }

            turnOffOption.setOnClickListener {
                MusicPlayerRemote.cancelTimer()
                updateUI()
            }
        }
    }
}