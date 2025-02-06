package com.example.musicapp.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import com.example.musicapp.base.BaseActivity
import com.example.musicapp.databinding.ActivitySplashBinding
class SplashActivity: BaseActivity<ActivitySplashBinding>(ActivitySplashBinding::inflate) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Handler().postDelayed({
            // Your code to be executed after the delay
            startActivity(Intent(this, MainActivity::class.java))
        }, 3000L)
    }
}