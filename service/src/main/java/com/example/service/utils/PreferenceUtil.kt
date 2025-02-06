package com.example.service.utils

import android.app.UiModeManager
import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.preference.PreferenceManager
import android.provider.MediaStore.Audio.Media
import com.example.service.utils.Utils.ODER_SORT_FILE_DEFAULT
import com.example.service.utils.Utils.ODER_SORT_TITLE_ASC

class PreferenceUtil(context: Context) {

    private var mPreferences: SharedPreferences? = null

    init {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    }

    fun registerOnSharedPreferenceChangedListener(sharedPreferenceChangeListener: OnSharedPreferenceChangeListener) {
        mPreferences?.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
    }


    fun unRegisterOnSharedPreferenceChangedListener(sharedPreferenceChangeListener: OnSharedPreferenceChangeListener) {
        mPreferences?.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
    }

    fun audioDucking(): Boolean {
        return mPreferences?.getBoolean(COLORED_APP_SHORTCUTS, true) ?: true
    }

    fun albumArtOnLockScreen(): Boolean {
        return mPreferences?.getBoolean(ALBUM_ART_ON_LOCKSCREEN, true) ?: true
    }

    fun rememberShuffle(): Boolean {
        return mPreferences?.getBoolean(REMEMBER_SHUFFLE, true) ?: false
    }

    //Sort order
    fun getSongSortOrder(): String? {
        return mPreferences!!.getString(
            SONG_SORT_ORDER,
            ODER_SORT_TITLE_ASC
        )
    }
    fun setSongSortOrder(sortOrder: String?) {
        val editor = mPreferences!!.edit()
        editor.putString(SONG_SORT_ORDER, sortOrder)
        editor.apply()
    }

    fun getAlbumSortOrder(): String? {
        return mPreferences!!.getString(
            ALBUM_SORT_ORDER,
            Media.ALBUM + " ASC"
        )
    }

    fun setAlbumSortOrder(sortOrder: String?) {
        val editor = mPreferences!!.edit()
        editor.putString(ALBUM_SORT_ORDER, sortOrder)
        editor.apply()
    }

    fun setGeneralTheme(theme: Int) {
        val editor = mPreferences?.edit()
        editor?.let {
            editor.putInt(GENERAL_THEME, theme)
            editor.apply()
        }
    }

    fun getThemeDefault(): Int {
        return mPreferences?.getInt(GENERAL_THEME, DEFAULT_THEME) ?: DEFAULT_THEME
    }

    fun getTheme(context: Context): Int {
        var currentTheme = mPreferences?.getInt(GENERAL_THEME, DEFAULT_THEME) ?: DEFAULT_THEME
        if(currentTheme == DEFAULT_THEME) {
            val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
            val isDarkMode = uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES
            currentTheme = if(isDarkMode) DARK_THEME else LIGHT_THEME
        }
        return currentTheme
    }

    fun getLanguage(): String {
        return  mPreferences?.getString(KEY_LANGUAGE, "") ?: ""
    }

    fun setLanguage(languageCode: String) {
        val editor = mPreferences?.edit()
        editor?.putString(KEY_LANGUAGE, languageCode)
        editor?.apply()
    }

    companion object {
        const val DEFAULT_THEME = -1
        const val LIGHT_THEME = 1
        const val DARK_THEME = 2

        const val SONG_SORT_ORDER: String = "song_sort_order"

        const val GENERAL_THEME: String = "general_theme"
        const val REMEMBER_LAST_TAB: String = "remember_last_tab"
        const val LAST_PAGE: String = "last_start_page"
        const val LAST_MUSIC_CHOOSER: String = "last_music_chooser"
        const val NOW_PLAYING_SCREEN_ID: String = "now_playing_screen_id"

        const val ARTIST_SORT_ORDER: String = "artist_sort_order"
        const val ARTIST_SONG_SORT_ORDER: String = "artist_song_sort_order"
        const val ARTIST_ALBUM_SORT_ORDER: String = "artist_album_sort_order"
        const val ALBUM_SORT_ORDER: String = "album_sort_order"
        const val ALBUM_SONG_SORT_ORDER: String = "album_song_sort_order"
        const val GENRE_SORT_ORDER: String = "genre_sort_order"
        const val ALBUM_GRID_SIZE: String = "album_grid_size"
        const val ALBUM_GRID_SIZE_LAND: String = "album_grid_size_land"

        const val SONG_GRID_SIZE: String = "song_grid_size"
        const val SONG_GRID_SIZE_LAND: String = "song_grid_size_land"

        const val ARTIST_GRID_SIZE: String = "artist_grid_size"
        const val ARTIST_GRID_SIZE_LAND: String = "artist_grid_size_land"


        const val ALBUM_COLORED_FOOTERS: String = "album_colored_footers"
        const val SONG_COLORED_FOOTERS: String = "song_colored_footers"
        const val ARTIST_COLORED_FOOTERS: String = "artist_colored_footers"
        const val ALBUM_ARTIST_COLORED_FOOTERS: String = "album_artist_colored_footers"

        const val FORCE_SQUARE_ALBUM_COVER: String = "force_square_album_art"
        const val COLORED_APP_SHORTCUTS: String = "colored_app_shortcuts"

        const val AUDIO_DUCKING: String = "audio_ducking"

        const val GAPLESS_PLAYBACK: String = "gapless_playback"

        const val LAST_ADDED_CUTOFF: String = "last_added_interval"

        const val ALBUM_ART_ON_LOCKSCREEN: String = "album_art_on_lockscreen"

        const val BLURRED_ALBUM_ART: String = "blurred_album_art"

        const val LAST_SLEEP_TIMER_VALUE: String = "last_sleep_timer_value"
        const val NEXT_SLEEP_TIMER_ELAPSED_REALTIME: String = "next_sleep_timer_elapsed_real_time"

        const val IGNORE_MEDIA_STORE_ARTWORK: String = "ignore_media_store_artwork"

        const val LAST_CHANGELOG_VERSION: String = "last_changelog_version"

        const val INTRO_SHOWN: String = "intro_shown"


        const val AUTO_DOWNLOAD_IMAGES_POLICY: String = "auto_download_images_policy"
        const val START_DIRECTORY: String = "start_directory"
        const val SYNCHRONIZED_LYRICS_SHOW: String = "synchronized_lyrics_show"
        const val INITIALIZED_BLACKLIST: String = "initialized_blacklist"
        const val LIBRARY_CATEGORIES: String = "library_categories"

        private const val REMEMBER_SHUFFLE: String = "remember_shuffle"

        const val IS_INITED_CONFIG: String = "IS_INITED_CONFIG"
        const val IS_ON_HOME_SCREEN: String = "IS_ON_HOME_SCREEN"
        const val KEY_LANGUAGE: String = "KEY_LANGUAGE"


        private var sInstance: PreferenceUtil? = null
        fun getInstance(context: Context): PreferenceUtil? {
            if (sInstance == null) {
                sInstance = PreferenceUtil(context.applicationContext)
            }
            return sInstance
        }
    }
}