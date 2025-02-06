package com.example.scrollbar

import android.view.View
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scrollbar.view.CustomHorizontalScrollView
import com.example.scrollbar.view.CustomNestedScrollView
import com.example.scrollbar.view.CustomScrollView
import com.example.scrollbar.view.CustomWebView
import com.example.scrollbar.viewhelper.HorizontalRecyclerViewHelper
import com.example.scrollbar.viewhelper.HorizontalScrollViewHelper
import com.example.scrollbar.viewhelper.HorizontalWebViewViewHelper
import com.example.scrollbar.viewhelper.VerticalNestedScrollViewHelper
import com.example.scrollbar.viewhelper.VerticalRecyclerViewHelper
import com.example.scrollbar.viewhelper.VerticalScrollViewHelper
import com.example.scrollbar.viewhelper.VerticalWebViewViewHelper

val View.isLayoutRtl: Boolean
    get() = layoutDirection == View.LAYOUT_DIRECTION_RTL

fun View.updateLayout(
    left: Int = this.left,
    top: Int = this.top,
    right: Int = this.right,
    bottom: Int = this.bottom
) {
    layout(left, top, right, bottom)
}

fun StandaloneScrollBar.attachTo(recyclerView: RecyclerView) {
    val layoutManager = recyclerView.layoutManager
    if (layoutManager is LinearLayoutManager) {
        when (layoutManager.orientation) {
            RecyclerView.VERTICAL -> attachTo(VerticalRecyclerViewHelper(recyclerView))
            RecyclerView.HORIZONTAL -> attachTo(HorizontalRecyclerViewHelper(recyclerView))
        }
    } else {
        throw IllegalArgumentException("LayoutManager must be instance of LinearLayoutManager and have to be set before attach with StandaloneScrollBar")
    }
}

fun StandaloneScrollBar.attachTo(scrollView: CustomNestedScrollView) {
    attachTo(VerticalNestedScrollViewHelper(scrollView))
}

fun StandaloneScrollBar.attachTo(scrollView: CustomScrollView) {
    attachTo(VerticalScrollViewHelper(scrollView))
}

fun StandaloneScrollBar.attachTo(scrollView: CustomHorizontalScrollView) {
    attachTo(HorizontalScrollViewHelper(scrollView))
}

fun StandaloneScrollBar.attachTo(webView: CustomWebView, orientation: StandaloneScrollBar.Orientation) {
    when (orientation) {
        StandaloneScrollBar.Orientation.VERTICAL -> attachTo(VerticalWebViewViewHelper(webView))
        StandaloneScrollBar.Orientation.HORIZONTAL -> attachTo(HorizontalWebViewViewHelper(webView))
    }
}