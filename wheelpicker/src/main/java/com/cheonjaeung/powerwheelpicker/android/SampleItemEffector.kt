package com.cheonjaeung.powerwheelpicker.android

import android.animation.ValueAnimator
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.TextView
import kotlin.math.abs

class SampleItemEffector(
    private val wheelPicker: WheelPicker,
    private val primaryColor: Int,
    private val secondaryColor: Int,
    private val textSize: Float,
    private val alpha: Float = 0f
) : WheelPicker.ItemEffector() {
    private val defaultCenterItemColor = primaryColor
    private val defaultOtherItemColor = secondaryColor

    private var targetColor = defaultCenterItemColor
    private var currentColor = defaultOtherItemColor

    private var colorAnimator: ValueAnimator? = null

    private val wheelPickerHeight: Float
        get() = wheelPicker.measuredHeight.toFloat()


    override fun applyEffectOnScrollStateChanged(
        view: View,
        newState: Int,
        positionOffset: Int,
        centerOffset: Int,
    ) {
        val textView = view.findViewById<TextView>(R.id.timeValueText)
        targetColor = if (newState != WheelPicker.SCROLL_STATE_IDLE) {
            defaultCenterItemColor
        } else {
            defaultOtherItemColor
        }

        colorAnimator = ValueAnimator.ofArgb(textView.currentTextColor, targetColor).apply {
            duration = 250
            addUpdateListener {
                currentColor = it.animatedValue as Int
                textView.setTextColor(if(positionOffset == 0) defaultCenterItemColor else currentColor)
            }
        }
        colorAnimator?.start()
    }

    override fun applyEffectOnScrolled(
        view: View,
        delta: Int,
        positionOffset: Int,
        centerOffset: Int,
    ) {
        view.alpha = if(alpha != 0f) alpha else 1f - abs(centerOffset) / (wheelPickerHeight / 2f)
        view.scaleX = 1f - abs(centerOffset) / (wheelPickerHeight / 1.2f)
        view.scaleY = 1f - abs(centerOffset) / (wheelPickerHeight / 1.2f)
        val textView = view.findViewById<TextView>(R.id.timeValueText)
        textView.setTextColor(currentColor)
    }

    override fun applyEffectOnItemSelected(view: View, position: Int) {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)

        val textView = view.findViewById<TextView>(R.id.timeValueText)
        textView.setTextColor(primaryColor)
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
    }
}