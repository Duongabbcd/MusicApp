package com.example.musicapp.fragment.basic

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.example.musicapp.activity.advance.search.SearchSongActivity
import com.example.musicapp.activity.basic.SettingActivity
import com.example.musicapp.base.BaseFragment
import com.example.musicapp.bottomsheet.BottomTimeSleepOptions
import com.example.musicapp.databinding.FragmentHomeBinding

class HomeFragment : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            searchLayout.setOnClickListener {
                startSearchingSong()
            }

            searchBar.setOnClickListener {
                startSearchingSong()
            }

            sleepTimer.setOnClickListener {
                val dialog = BottomTimeSleepOptions(requireContext())
                dialog.show()
            }

            setting.setOnClickListener {
                startActivity(Intent(requireContext(), SettingActivity::class.java))
            }
        }
    }

    private fun startSearchingSong() {
        val intent = Intent(requireContext(), SearchSongActivity::class.java)
        intent.putExtra(SearchSongActivity.IS_ONLINE, true)
        startActivity(intent)
    }
}