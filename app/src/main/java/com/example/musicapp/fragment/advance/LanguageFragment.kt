package com.example.musicapp.fragment.advance

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicapp.R
import com.example.musicapp.activity.basic.SettingActivity
import com.example.musicapp.adapter.advance.LanguageAdapter
import com.example.musicapp.adapter.advance.LanguageItem
import com.example.musicapp.base.BaseFragment
import com.example.musicapp.databinding.FragmentLanguageBinding
import com.example.musicapp.dialog.LanguageDialog
import com.example.service.utils.PreferenceUtil

class LanguageFragment: BaseFragment<FragmentLanguageBinding>(FragmentLanguageBinding::inflate) {
    private val languageAdapter = LanguageAdapter { selected ->
        val dialog = LanguageDialog(requireContext())
        dialog.setData(selected)
        dialog.show()
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            val currentLanguage = getCurrentLanguage()
            println("onViewCreated: ${getCurrentLanguage()}")
            languageAdapter.updateData(getAllLanguages(), currentLanguage)
            languageRV.adapter = languageAdapter
            languageRV.layoutManager = LinearLayoutManager(requireContext())

            backBtn.setOnClickListener {
                openFragment(SettingFragment())
            }
        }
    }

    private fun getAllLanguages(): List<LanguageItem> {
        return listOf(
            LanguageItem("", requireContext().getString(R.string.default_language)),
            LanguageItem("en","English"),
            LanguageItem("vi","Tiếng Việt"),
        )
    }

    private fun getCurrentLanguage(): LanguageItem {
        return PreferenceUtil.getInstance(requireContext())?.getLanguage()
            ?.let { LanguageItem(it, "") }
            ?: LanguageItem("","")
    }

    override fun onResume() {
        super.onResume()
        SettingActivity.position = 2
    }
}