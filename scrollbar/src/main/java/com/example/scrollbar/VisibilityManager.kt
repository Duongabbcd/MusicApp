package com.example.scrollbar

import android.view.View
import androidx.core.view.isVisible

interface VisibilityManager {
    fun isTrackViewShowing(trackView: View) = trackView.isVisible

    fun isThumbViewShowing(thumbView: View) = thumbView.isVisible

    fun showScrollbar(trackView: View, thumbView: View, isLayoutRtl: Boolean) {
        trackView.isVisible = true
        thumbView.isVisible = true
    }

    fun hideScrollbar(trackView: View, thumbView: View, isLayoutRtl: Boolean) {
        trackView.isVisible = false
        thumbView.isVisible = false
    }
}