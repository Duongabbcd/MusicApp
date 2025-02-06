package com.example.scrollbar.viewhelper

import com.example.scrollbar.HorizontalScrollableView
import com.example.scrollbar.ScrollableView
import com.example.scrollbar.view.CustomHorizontalScrollView

internal class HorizontalScrollViewHelper(
    private val scrollView: CustomHorizontalScrollView
): HorizontalScrollableView{
    override val viewWidth: Int
        get() = scrollView.width
    override val viewHeight: Int
        get() =scrollView.height
    override val scrollRange: Int
        get() =scrollView.calculateHorizontalScrollRange() + scrollView.paddingStart + scrollView.paddingEnd
    override val scrollOffset: Int
        get() =scrollView.calculateHorizontalScrollOffset()


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
        scrollView.scrollTo(offSet, scrollView.scrollY)
    }
}