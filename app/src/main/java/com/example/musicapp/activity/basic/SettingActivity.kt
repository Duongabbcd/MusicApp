package com.example.musicapp.activity.basic

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.example.musicapp.R
import com.example.musicapp.base.BaseActivity
import com.example.musicapp.databinding.ActivitySettingBinding
import com.example.musicapp.fragment.advance.LanguageFragment
import com.example.musicapp.fragment.advance.SettingFragment
import com.example.musicapp.fragment.advance.ThemeFragment

class SettingActivity : BaseActivity<ActivitySettingBinding>(ActivitySettingBinding::inflate) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.apply {
            val fragment: Fragment = when(position) {
                0 -> SettingFragment()
                1 -> ThemeFragment()
                2 -> LanguageFragment()
                else -> SettingFragment()
            }
            openFragment(fragment)
        }
    }

    private fun openFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.frameLayout, fragment).addToBackStack(null).commit()
    }

     companion object {
          var position = -1
     }
}