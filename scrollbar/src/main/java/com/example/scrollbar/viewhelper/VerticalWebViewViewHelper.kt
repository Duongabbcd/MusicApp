package com.example.scrollbar.viewhelper

import com.example.scrollbar.ScrollableView
import com.example.scrollbar.VerticalScrollableView
import com.example.scrollbar.view.CustomWebView

class VerticalWebViewViewHelper(
    private val webView: CustomWebView
): VerticalScrollableView{
    override val viewWidth: Int
        get() = webView.width

    override val viewHeight: Int
        get() = webView.height

    override val scrollRange: Int
        get() = webView.calculateVerticalScrollRange()

    override val scrollOffset: Int
        get() = webView.calculateVerticalScrollOffset()

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

    override fun scrollTo(offset: Int) {
        webView.scrollTo(webView.scrollX, offset)
    }
}