package com.example.scrollbar

interface VerticalScrollableView : ScrollableView{
    override val scrollOffsetRange: Int
        get() = scrollRange - viewHeight
}