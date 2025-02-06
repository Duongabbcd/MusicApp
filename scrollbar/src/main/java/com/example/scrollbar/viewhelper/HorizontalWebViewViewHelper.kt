package com.example.scrollbar.viewhelper

import com.example.scrollbar.HorizontalScrollableView
import com.example.scrollbar.ScrollableView
import com.example.scrollbar.view.CustomWebView

class HorizontalWebViewViewHelper(
    private val webView: CustomWebView
) : HorizontalScrollableView {

    override val viewWidth: Int
        get() = webView.width
    override val viewHeight: Int
        get() = webView.height
    override val scrollRange: Int
        get() = webView.calculateHorizontalScrollRange()
    override val scrollOffset: Int
        get() = webView.calculateHorizontalScrollOffset()

    override fun addOnScrollChangedListener(onScrollChanged: (caller: ScrollableView) -> Unit) {
        webView.addOnScrollListener { _, _, _, _ ->
            onScrollChanged(this)
        }
    }

    override fun addOnDraw(onDraw: (caller: ScrollableView) -> Unit) {
        webView.addOnDrawListener {
            onDraw(this)
        }
    }

    override fun scrollTo(offSet: Int) {
        webView.scrollTo(offSet, webView.scrollY)
    }
}