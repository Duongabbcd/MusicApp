package com.example.scrollbar.viewhelper

import androidx.recyclerview.widget.RecyclerView
import com.example.scrollbar.ScrollableView
import com.example.scrollbar.VerticalScrollableView
import com.example.scrollbar.view.CustomScrollView

class VerticalScrollViewHelper(
    private val scrollView: CustomScrollView
): VerticalScrollableView {

    override val viewWidth: Int
        get() = scrollView.width

    override val viewHeight: Int
        get() = scrollView.height

    override val scrollRange: Int
        get() = scrollView.calculateHorizontalScrollRange() + scrollView.paddingTop + scrollView.paddingBottom

    override val scrollOffset: Int
        get() = scrollView.calculateHorizontalScrollOffset()

    override fun addOnScrollChangedListener(onScrollChanged: (caller: ScrollableView) -> Unit) {
        scrollView.addOnScrollListener { _, _, _, _ ->
            onScrollChanged(this)
        }
    }

    override fun addOnDraw(onDraw: (caller: ScrollableView) -> Unit) {
        scrollView.addOnDrawListener() {
            onDraw(this)
        }
    }
    override fun scrollTo(offSet: Int) {
        //Stop any scroll in progress for RecyclerView.
        scrollView.scrollTo(scrollView.scrollX, offSet)
    }

}