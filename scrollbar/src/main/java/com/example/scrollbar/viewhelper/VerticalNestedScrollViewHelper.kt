package com.example.scrollbar.viewhelper

import android.widget.ScrollView
import com.example.scrollbar.ScrollableView
import com.example.scrollbar.VerticalScrollableView
import com.example.scrollbar.view.CustomNestedScrollView

class VerticalNestedScrollViewHelper(
    private val scrollView: CustomNestedScrollView
) : VerticalScrollableView {
    override val viewWidth: Int
        get() = scrollView.width
    override val viewHeight: Int
        get() = scrollView.height
    override val scrollRange: Int
        get() = scrollView.calculateHorizontalScrollRange()
    override val scrollOffset: Int
        get() = scrollView.calculateHorizontalScrollOffset()

    override fun addOnScrollChangedListener(onScrollChanged: (caller: ScrollableView) -> Unit) {
        scrollView.addOnScrollListener { _, _, _, _ ->
            onScrollChanged(this)
        }
    }

    override fun addOnDraw(onDraw: (caller: ScrollableView) -> Unit) {
        scrollView.addOnDrawListener {
            onDraw(this)
        }
    }

    override fun scrollTo(offSet: Int) {
        scrollView.scrollTo(scrollView.scrollX, offSet)
    }
}