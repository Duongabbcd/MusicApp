package com.example.scrollbar.view

fun interface OnScrollChangedListener {
    fun onScrollChanged(l: Int, t: Int, oldL: Int, oldT: Int)
}