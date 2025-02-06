package com.example.musicapp.activity.basic

import android.os.Bundle
import com.example.musicapp.R
import com.example.musicapp.base.BaseActivity
import com.example.musicapp.databinding.ActivitySettingBinding
import com.example.musicapp.fragment.advance.SettingFragment

class SettingActivity : BaseActivity<ActivitySettingBinding>(ActivitySettingBinding::inflate) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.apply {
            val fragment = SettingFragment()
            openFragment(fragment)
        }
    }

    private fun openFragment(fragment: SettingFragment) {
        supportFragmentManager.beginTransaction().replace(R.id.frameLayout, fragment).commit()
    }
}