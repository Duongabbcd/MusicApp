package com.example.musicapp.fragment.advance

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.example.musicapp.activity.MainActivity
import com.example.musicapp.activity.basic.SettingActivity
import com.example.musicapp.adapter.advance.Setting
import com.example.musicapp.adapter.advance.SettingAdapter
import com.example.musicapp.base.BaseFragment
import com.example.musicapp.databinding.FragmentSettingBinding

class SettingFragment: BaseFragment<FragmentSettingBinding>(FragmentSettingBinding::inflate) {
    private val settingAdapter: SettingAdapter = SettingAdapter() {
        settingOption = it
        chooseOneFunction()
    }

    private fun chooseOneFunction() {
        when(settingOption) {
            Setting.THEMES -> {
                openFragment(ThemeFragment())
            }
            Setting.LANGUAGES -> {
                openFragment(LanguageFragment())
            }

            else -> {
                //do nothing
            }
        }
    }

    private var settingOption = Setting.DEFAULT
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            val layoutManager = object : GridLayoutManager(requireContext(), 2) {
                override fun canScrollVertically(): Boolean {
                    return false
                }
            }
            settingRV.layoutManager = layoutManager
            settingRV.adapter = settingAdapter
            backBtn.setOnClickListener {
                SettingActivity.position = -1
                requireContext().startActivity(Intent(requireContext(), MainActivity::class.java))
            }
            downloadLayout.setOnClickListener {
                //do nothing
            }
            privacyLayout.setOnClickListener {
                //do nothing
            }
            fullScreenLayout.setOnClickListener {
                //do nothing
            }
            versionLayout.setOnClickListener {
                //do nothing
            }
        }
    }

    override fun onResume() {
        super.onResume()
        SettingActivity.position = 0
    }
}