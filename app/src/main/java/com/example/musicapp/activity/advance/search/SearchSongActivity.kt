package com.example.musicapp.activity.advance.search

import android.os.Bundle
import android.util.Log
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicapp.R
import com.example.musicapp.adapter.advance.SearchSongAdapter
import com.example.musicapp.adapter.advance.SuggestAdapter
import com.example.musicapp.base.BaseActivity
import com.example.musicapp.databinding.ActivitySearchOfflineBinding
import com.example.musicapp.util.Utils.hideKeyBoard
import com.example.service.model.Audio
import com.example.service.model.MediaObject
import com.example.service.model.Video
import com.example.service.service.SongLoader
import com.music.searchapi.ApiServices
import com.music.searchapi.callback.SearchCallback
import com.music.searchapi.`object`.VideoEntity
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

class SearchSongActivity :
    BaseActivity<ActivitySearchOfflineBinding>(ActivitySearchOfflineBinding::inflate) {
    private lateinit var searchSongAdapter: SearchSongAdapter
    private lateinit var suggestAdapter: SuggestAdapter
    private var listSong = mutableListOf<Audio>()
    private var previousSearch = ""
    private val databaseSearch by lazy { DatabaseSearchHistory(this@SearchSongActivity) }

    private fun onGetSuggest(s: String) {
        binding.searchBarText.setText(s)
        binding.searchBarText.setSelection(s.length)
    }

    private var isSearching = false

    private var nextPage: Any? = null
    private var isLoadingMore = false

    private fun onClickSuggest(s: String) {
        binding.apply {
            searchBarText.setText(s)
            searchBarText.setSelection(s.length)
            isSearching = true
            hideKeyBoard(binding.searchBarText)
            swipeRefreshLayout.isRefreshing = true
            CoroutineScope(Dispatchers.IO).launch {
                ApiServices.search(this@SearchSongActivity, s) { resultList, p2 ->
                    nextPage = p2
                    isLoadingMore = false
                    val videoList = resultList.map { it as VideoEntity }.toMutableList()
                    val audioList = videoList.map {
                        val audio = Audio(
                            mediaObject = MediaObject(
                                id = it.videoId ?: "",
                                title = it.videoTile,
                                imageThumb = it.image_link,
                                path = it.stream_link
                            ),
                            artist = it.artist_name,
                            timeSong = it.durationString ?: "",
                            albumName = it.artist_name,
                            timeCount = it.duration.toLong() * 1000L
                        )
                        audio.isOnline = true
                        audio.videoEntity = it
                        audio.ccmixterReferrer = it.ccmixterReferrer
                        audio.videoType = it.videoType
                        audio
                    }
                    listSong.clear()
                    listSong.addAll(audioList)
                    CoroutineScope(Dispatchers.Main).launch {
                        downloadSongRV.adapter = searchSongAdapter
                        searchSongAdapter.updateData(audioList)
                        downloadSongRV.isVisible = true
                        swipeRefreshLayout.isRefreshing = false
                    }
                }
                try {
                    databaseSearch.addHistoryNotExist(s)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }

            downloadSongRV.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val totalItemCount = layoutManager.itemCount
                    val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                    searchSongAdapter.lastVisibleItemPosition = lastVisibleItemPosition
                    if (!isLoadingMore && totalItemCount <= (lastVisibleItemPosition + 3)) {
                        loadMoreItem()
                    }
                }
            })
        }
    }

    private fun loadMoreItem() {
        if (isLoadingMore || nextPage == null) {
            return
        }
        isLoadingMore = true
        val text = binding.searchBarText.text.toString()
        CoroutineScope(Dispatchers.IO).launch {
            ApiServices.searchMore(
                this@SearchSongActivity,
                text,
                nextPage,
                SearchCallback { listResult, any ->
                    isLoadingMore = false
                    nextPage = any
                    if (listResult != null) {
                        val videoList = listResult.map { it as Video }.toMutableList()
                        val audioList = videoList.map {
                            val audio = Audio(
                                mediaObject = MediaObject(
                                    id = it.videoId,
                                    title = it.videoTitle,
                                    imageThumb = it.imageLink,
                                    path = it.streamLink
                                ),
                                artist = it.artistName,
                                timeSong = it.durationString,
                                albumName = it.artistName,
                                timeCount = it.duration.toLong() * 1000L
                            )
                            audio.isOnline = true
                            audio.videoEntity = it
                            audio.ccmixterReferrer = it.ccmixterReferrer
                            audio.videoType = it.videoType
                            audio
                        }
                        searchSongAdapter.getCurrentList().addAll(audioList)
                        CoroutineScope(Dispatchers.Main).launch {
                            searchSongAdapter.notifyDataSetChanged()
                        }
                    }
                })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        searchSongAdapter = SearchSongAdapter(this)
        suggestAdapter = SuggestAdapter(::onGetSuggest, ::onClickSuggest)
        listSong = SongLoader.getAllSongs(this)
        binding.apply {
            cancelButton.setOnClickListener {
                finish()
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            }

            downloadSongRV.layoutManager = LinearLayoutManager(this@SearchSongActivity)
            downloadSongRV.adapter = searchSongAdapter

            searchBar.setOnClickListener {
                //donothing
            }
            swipeRefreshLayout.setOnRefreshListener {
                swipeRefreshLayout.isRefreshing = false
            }

            searchBarText.addTextChangedListener {
                val key = it.toString()
                updateData(key)
            }

            searchBarIcon.setOnClickListener {
                searchBarText.setText("")
                this@SearchSongActivity.hideKeyBoard(searchBarText)
            }

            downloadSongRV.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    this@SearchSongActivity.hideKeyBoard(searchBarText)
                }

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                    binding.scrollToTop.isVisible = firstVisibleItemPosition > 0
                }
            })

            scrollToTop.setOnClickListener {
                downloadSongRV.smoothScrollToPosition(0)
            }
        }
    }

    private fun updateData(key: String = "") {
        if (key.isEmpty()) {
            binding.searchBarIcon.setImageResource(R.drawable.icon_search_purple)
            searchSongAdapter.updateData(list = listSong)
        } else {
            previousSearch = key
            binding.searchBarIcon.setImageResource(R.drawable.icon_close)
            getSuggestText(key)
        }
    }

    private fun getSuggestText(key: String) {
        val searchUrl =
            "http://suggestqueries.google.com/complete/search?q=$key&client=firefox".replace(
                " ", "%20"
            )
        CoroutineScope(Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
            Log.e("getSuggestText", throwable.message.toString())
        }).launch {
            val listHistory = databaseSearch.getHistoriesFromString(key)
            println("getSuggestText: $key")
            println("getSuggestText: $searchUrl")

            val keyData = ArrayList<String>()
            val okHttpClient = OkHttpClient.Builder().build()
            val request = Request.Builder().get().url(searchUrl).build()
            val response = okHttpClient.newCall(request).execute()
            val jsonString = response.body?.string()
            val json = JSONArray(jsonString)
            val jsonData = json.getJSONArray(1)
            for (i in 0 until jsonData.length()) {
                keyData.add(jsonData[i].toString())
            }
            println("getSuggestText: $keyData")
            val listSuggest = mutableListOf<SuggestItem>()
            listSuggest.addAll(listHistory.map { SuggestItem(it, SuggestItem.TYPE_HISTORY) })
            listSuggest.addAll(keyData.map { SuggestItem(it, SuggestItem.TYPE_ONLINE) })
            withContext(Dispatchers.Main) {
                binding.downloadSongRV.adapter = suggestAdapter
                suggestAdapter.updateData(listSuggest, key)
            }
        }

    }

    override fun setUpMiniPlayer() {
        super.setUpMiniPlayer()
        searchSongAdapter.notifyDataSetChanged()
    }

    companion object {
        const val IS_ONLINE = "isOnline"
    }
}

data class SuggestItem(val content: String, val type: Int) {
    companion object {
        const val TYPE_HISTORY = 1
        const val TYPE_ONLINE = 2
    }
}