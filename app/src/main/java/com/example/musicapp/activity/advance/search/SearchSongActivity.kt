package com.example.musicapp.activity.advance.search

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
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
import com.example.service.utils.Utils
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

    private var isConnectedToInternet = false

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    private fun onGetSuggest(s: String) {
        println("onGetSuggest: $s and $isConnectedToInternet")
        if (!isConnectedToInternet) {
            return
        }

        binding.searchBarText.setText(s)
        binding.searchBarText.setSelection(s.length)
    }

    private fun displayNoInternetConnection(isLostConnection: Boolean = false) {
        CoroutineScope(Dispatchers.Main).launch {
            binding.apply {
                println("displayNoInternetConnection: $isLostConnection")
                if (isLostConnection) {
                    emptyLayout.visibility = View.VISIBLE
                    swipeRefreshLayout.visibility = View.GONE

                    emptyTitle.text = this@SearchSongActivity.getString(R.string.no_internet)
                    emptyDescription.text = this@SearchSongActivity.getString(R.string.lost_connection)
                } else {
                    binding.swipeRefreshLayout.visibility = View.VISIBLE
                    binding.emptyLayout.visibility = View.GONE
                }
            }
        }

    }

    private var isSearching = false

    private var nextPage: Any? = null
    private var isLoadingMore = false

    private fun onClickSuggest(s: String) {
        println("onClickSuggest: $s and $isConnectedToInternet")
        if (!isConnectedToInternet) {
            return
        }
        binding.apply {
            searchBarText.setText(s)
            searchBarText.setSelection(s.length)
            downloadSongRV.isVisible = false
            isSearching = true
            hideKeyBoard(searchBarText)
            swipeRefreshLayout.isRefreshing = true
            CoroutineScope(Dispatchers.IO).launch {
                ApiServices.search(this@SearchSongActivity, s) { resultList, p2 ->
                    nextPage = p2
                    isLoadingMore = false
                    val videoList = resultList.map { it as VideoEntity }.toMutableList()
                    if (videoList.isEmpty()) {
                        CoroutineScope(Dispatchers.Main).launch {
                            emptyLayout.visibility = View.VISIBLE
                            swipeRefreshLayout.visibility = View.GONE

                            emptyTitle.text =
                                this@SearchSongActivity.getString(R.string.no_result_found)
                            emptyDescription.text =
                                this@SearchSongActivity.getString(R.string.search_other_content)
                        }
                    } else {

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
                            emptyLayout.visibility = View.GONE
                            swipeRefreshLayout.visibility = View.VISIBLE

                            downloadSongRV.adapter = searchSongAdapter
                            searchSongAdapter.updateData(audioList)
                            downloadSongRV.isVisible = true
                            swipeRefreshLayout.isRefreshing = false
                        }
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
        // Initialize the ConnectivityManager
        connectivityManager = getSystemService(ConnectivityManager::class.java)

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

            searchBarText.setOnEditorActionListener { _, i, _ ->
                if (i == EditorInfo.IME_ACTION_SEARCH || i == EditorInfo.IME_ACTION_DONE || i == EditorInfo.IME_ACTION_GO) {
                    onClickSuggest(searchBarText.text.toString())
                }
                true
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



            checkNetworkCallback()
        }
    }

    private fun checkNetworkCallback() {
            // Create a NetworkCallback to listen for network changes
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    // Network is available (connected to Wi-Fi or mobile data)
                    Log.d("NetworkCallback", "Network is available")
                    displayNoInternetConnection(false)
                    isConnectedToInternet = true
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    // Network is lost (Wi-Fi or mobile data disconnected)
                    Log.d("NetworkCallback", "Network is lost")
                    displayNoInternetConnection(true)
                    isConnectedToInternet = false

                }

                override fun onCapabilitiesChanged(network: Network, capabilities: android.net.NetworkCapabilities) {
                    super.onCapabilitiesChanged(network, capabilities)
                    // Handle network capabilities changes, e.g., Wi-Fi or mobile data
                    if (capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)) {
                        Log.d("NetworkCallback", "Connected to Wi-Fi")
                        displayNoInternetConnection(false)
                        isConnectedToInternet = true
                    } else if (capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        Log.d("NetworkCallback", "Connected to Mobile Data (3G/4G)")
                        displayNoInternetConnection(false)
                        isConnectedToInternet = true
                    }
                }
            }

    }

    private fun updateData(key: String = "") {
        println("displayNoInternetConnection 2: $isConnectedToInternet")
        if (key.isEmpty()) {
            binding.searchBarIcon.setImageResource(R.drawable.icon_search_purple)
            if( isConnectedToInternet) {
                searchSongAdapter.updateData(list = listSong)
            } else {
                displayNoInternetConnection(true)
            }

        } else {
            previousSearch = key
            binding.searchBarIcon.setImageResource(R.drawable.icon_close)
            if(isConnectedToInternet) {
                getSuggestText(key)
            } else {
                displayNoInternetConnection(true)
            }
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

    override fun onStart() {
        super.onStart()

        // Create a NetworkRequest to monitor network state
        val networkRequest = NetworkRequest.Builder().build()

        // Register the callback with the ConnectivityManager
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    override fun onStop() {
        super.onStop()

        // Unregister the callback when the activity is no longer in the foreground
        connectivityManager.unregisterNetworkCallback(networkCallback)
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