package com.example.scrollbar.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.core.widget.NestedScrollView

class CustomNestedScrollView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : NestedScrollView(context, attrs, defStyleAttr){
    private val drawListener = mutableSetOf<OnDrawListener>()
    private val scrollChangeListener = mutableSetOf<OnScrollChangedListener>()

    fun addOnDrawListener(onDrawListener: OnDrawListener) {
        drawListener.add(onDrawListener)
    }

    fun addOnScrollListener(onScrollChangeListener: OnScrollChangedListener) {
        scrollChangeListener.add(onScrollChangeListener)
    }

    @SuppressLint("RestrictedApi")
    fun calculateHorizontalScrollRange(): Int = computeHorizontalScrollRange()

    @SuppressLint("RestrictedApi")
    fun calculateHorizontalScrollOffset(): Int = computeHorizontalScrollOffset()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawListener.forEach { it.onDraw(canvas) }
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        scrollChangeListener.forEach {
            it.onScrollChanged(l, t, oldl, oldt)
        }
    }

}