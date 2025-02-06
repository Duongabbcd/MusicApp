package com.example.musicapp.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.musicapp.R
import org.w3c.dom.Text
import java.util.concurrent.TimeUnit
import kotlin.math.abs

object Utils {
    fun setImageResource(
        selected: ImageView,
        selectedImage: Int,
        unselected: List<ImageView>,
        mapOfImage: Map<ImageView, Int>
    ) {
        selected.setImageResource(selectedImage)
        unselected.onEach {

            mapOfImage[it]?.let {
                it1 ->
                it.setImageResource(it1) }
        }
    }

    fun setTextColor(
        selected: List<TextView>,
        unselected: List<TextView>,
        selectedColor: Int,
        unselectedColor: Int
    ) {
        selected.onEach {
            it.setTextColor(ContextCompat.getColor(it.context, selectedColor))
        }
        unselected.onEach {
            it.setTextColor(ContextCompat.getColor(it.context, unselectedColor))
        }
    }

    fun setIconVisibility(
        selected: List<View>,
        unselected: List<View>,
    ) {
        selected.onEach {
            it.visibility = View.VISIBLE
        }
        unselected.onEach {
            it.visibility = View.GONE
        }
    }

    fun setBackground(
        selected: View,
        selectedBackground: Int,
        unselected: List<View>,
        unselectedBackground: Int
    ) {
        selected.setBackgroundResource(selectedBackground)
        unselected.onEach {
            it.setBackgroundResource(unselectedBackground)
        }
    }

    @SuppressLint("DefaultLocale")
    fun formatDuration(duration: Long): String {
        val minutes = TimeUnit.MINUTES.convert(duration, TimeUnit.MILLISECONDS)
        val seconds = (TimeUnit.SECONDS.convert(duration, TimeUnit.MILLISECONDS) -
                minutes * TimeUnit.SECONDS.convert(1, TimeUnit.MINUTES))
        return String.format("%02d:%02d", minutes, seconds)
    }

    @SuppressLint("Range")
    fun getThumb(
        progress: Int,
        max: Int,
        textView: TextView,
        seekBar: View,
        resources: Resources
    ): Drawable {
        val currentTime = formatDuration(progress.toLong())
        val totalTime = formatDuration(max.toLong())
        textView.text = "$currentTime/$totalTime"
        val spec = View.MeasureSpec.makeMeasureSpec(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            View.MeasureSpec.UNSPECIFIED
        )
        seekBar.measure(spec, spec)

        val bitmap = Bitmap.createBitmap(
            seekBar.measuredWidth,
            seekBar.measuredHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)

        seekBar.layout(0, 0, seekBar.measuredWidth, seekBar.measuredHeight)
        seekBar.draw(canvas)

        return BitmapDrawable(resources, bitmap)
    }

    fun setMarginDialog(dialogSort: LinearLayout, margin: Int? = null) : ViewGroup.LayoutParams{
        val currentMargin = margin ?: dialogSort.resources.getDimensionPixelSize(R.dimen.margin_20dp)
        val params = dialogSort.layoutParams as ViewGroup.MarginLayoutParams
        params.setMargins(currentMargin, currentMargin, currentMargin, currentMargin)
        return params
    }


    fun Context.getColorFromAttr(attr: Int): Int {
        val typedValue = TypedValue()
        val theme: Resources.Theme = theme
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    fun Context.hideKeyBoard(editText: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(editText.windowToken, 0)
    }



    fun View.setOnSWipeListener(activity: Activity) {
        val gestureDetector = GestureDetector(activity, SwipeGestureListener(activity))
        this.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
        }
    }


    fun isViewCurrentlySelected(view: TextView, selectedColor: Int ): Boolean {
        return view.currentTextColor == selectedColor.also {
            println("isViewCurrentlySelected: ${view.currentTextColor == R.color.high_light_color}")
            println("isViewCurrentlySelected: ${view.currentTextColor} and ${R.color.high_light_color}")
        }
    }

}

class SwipeGestureListener(val activity: Activity) : GestureDetector.SimpleOnGestureListener() {
    private val SWIPE_THRESHOLD = 100
    private val SWIPE_VELOCITY_THRESHOLD = 100

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        println("SwipeGestureListener - onFLing: $velocityY")
        try {
            val diffY = e2.y.minus(e1!!.y)
            val diffX = e2.x.minus(e1.x)

            if (abs(diffX) <= abs(diffY)) {
                if (abs(diffY) > SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        activity.finish()
                        activity.overridePendingTransition(
                            R.anim.fade_in_quick, R.anim.exit_to_top
                        )
                    }
                    return true
                }
            }
        } catch (ex: Exception) {
            println("SwipeGestureListener: ${ex.printStackTrace()}")
        }

        return false
    }

}
