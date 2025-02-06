package com.example.scrollbar

interface HorizontalScrollableView: ScrollableView{

    override val scrollOffsetRange: Int
        get() = scrollRange - viewWidth
}