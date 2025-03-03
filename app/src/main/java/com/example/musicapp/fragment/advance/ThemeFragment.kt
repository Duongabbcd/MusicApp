package com.example.musicapp.fragment.advance

import android.app.UiModeManager
import android.content.Context.UI_MODE_SERVICE
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.example.musicapp.R
import com.example.musicapp.activity.MainActivity
import com.example.musicapp.activity.basic.SettingActivity
import com.example.musicapp.base.BaseFragment
import com.example.musicapp.databinding.FragmentThemeBinding
import com.example.musicapp.util.Utils
import com.example.musicapp.util.Utils.getColorFromAttr
import com.example.service.utils.PreferenceUtil

class ThemeFragment : BaseFragment<FragmentThemeBinding>(FragmentThemeBinding::inflate) {
    private var currentTheme = PreferenceUtil.DEFAULT_THEME
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            var settingTheme = PreferenceUtil.DEFAULT_THEME
            currentTheme = PreferenceUtil.getInstance(requireContext())?.let {
                it.getThemeDefault()
            } ?: PreferenceUtil.DARK_THEME
            when(currentTheme) {
                PreferenceUtil.LIGHT_THEME -> setLightTheme()
                PreferenceUtil.DARK_THEME -> setDarkTheme()
                PreferenceUtil.DEFAULT_THEME -> setSystemTheme()
            }
            checkCurrentTheme(currentTheme, currentTheme)

            backBtn.setOnClickListener {
                openFragment(SettingFragment())
            }

            darkThemeOption.setOnClickListener {
                setDarkTheme()
                settingTheme = PreferenceUtil.DARK_THEME
                checkCurrentTheme(currentTheme,settingTheme)
            }

            lightThemeOption.setOnClickListener {
                setLightTheme()
                settingTheme = PreferenceUtil.LIGHT_THEME
                checkCurrentTheme(currentTheme, settingTheme)
            }

            systemThemeOption.setOnClickListener {
                setSystemTheme()
                settingTheme = PreferenceUtil.DEFAULT_THEME
                checkCurrentTheme(currentTheme, settingTheme)
            }

            applyBtn.setOnClickListener {
                PreferenceUtil.getInstance(requireContext())?.let {
                    it.setGeneralTheme(
                        settingTheme
                    )
                }
                MainActivity.isChangeTheme = true
                requireActivity().recreate()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        SettingActivity.position = 1
    }

    private fun checkCurrentTheme(currentTheme: Int, statusTheme: Int) {
        if(currentTheme != statusTheme) {
            binding.applyBtn.text = requireContext().resources.getString(R.string.use_now)
            binding.applyBtn.setTextColor(ContextCompat.getColor(requireContext(), R.color.white_text))
            binding.applyBtn.setBackgroundResource(R.drawable.background_highlight_round)
        } else {
            binding.applyBtn.text = requireContext().resources.getString(R.string.using)
            binding.applyBtn.setTextColor(requireContext().getColorFromAttr(R.attr.textColor))
            binding.applyBtn.setBackgroundResource(R.drawable.background_gray_corner_round)
        }
        when(statusTheme) {
            PreferenceUtil.DARK_THEME -> {
                binding.nightSampleScreen.visibility = View.VISIBLE
                binding.daySampleScreen.visibility = View.GONE
            }
            PreferenceUtil.LIGHT_THEME -> {
                binding.nightSampleScreen.visibility = View.GONE
                binding.daySampleScreen.visibility = View.VISIBLE
            }
            else -> {
                val uiModeManager= requireContext().getSystemService(UI_MODE_SERVICE) as UiModeManager
                val iSDarkMode = uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES
                if(iSDarkMode) {
                    binding.nightSampleScreen.visibility = View.VISIBLE
                    binding.daySampleScreen.visibility = View.GONE
                } else {
                    binding.nightSampleScreen.visibility = View.GONE
                    binding.daySampleScreen.visibility = View.VISIBLE
                }
            }
        }
    }


    private fun setDarkTheme() {
        val selectedOption = binding.darkThemeIcon
        val unSelectedOption = listOf(binding.lightThemeIcon, binding.systemThemeIcon)
        val selectedIcon = R.drawable.icon_selected_dark_mode
        Utils.setImageResource(selectedOption, selectedIcon, unSelectedOption, mapOfImage)
        Utils.setIconVisibility(
            listOf(binding.darkThemeSelected),
            listOf(binding.lightThemeSelected, binding.systemThemeSelected)
        )

        val selectedBackground = R.drawable.background_transparent_highlight_stroke
        val unSelectedBackground = R.drawable.gray_menu_item_bg
        Utils.setBackground(
            selectedOption,
            selectedBackground,
            unSelectedOption,
            unSelectedBackground
        )
        binding.nightSampleScreen.visibility = View.VISIBLE
        binding.daySampleScreen.visibility = View.GONE
    }

    private fun setLightTheme() {
        val selectedOption = binding.lightThemeIcon
        val unSelectedOption = listOf(binding.darkThemeIcon, binding.systemThemeIcon)
        val selectedIcon = R.drawable.icon_selected_light_mode
        Utils.setImageResource(selectedOption, selectedIcon, unSelectedOption, mapOfImage)
        Utils.setIconVisibility(
            listOf(binding.lightThemeSelected),
            listOf(binding.darkThemeSelected, binding.systemThemeSelected)
        )

        val selectedBackground = R.drawable.background_transparent_highlight_stroke
        val unSelectedBackground = R.drawable.gray_menu_item_bg
        Utils.setBackground(
            selectedOption,
            selectedBackground,
            unSelectedOption,
            unSelectedBackground
        )
        binding.nightSampleScreen.visibility = View.GONE
        binding.daySampleScreen.visibility = View.VISIBLE
    }

    private fun setSystemTheme() {
        val selectedOption = binding.systemThemeIcon
        val unSelectedOption = listOf(binding.lightThemeIcon, binding.darkThemeIcon)
        val selectedIcon = R.drawable.icon_selected_system_mode
        Utils.setImageResource(selectedOption, selectedIcon, unSelectedOption, mapOfImage)
        Utils.setIconVisibility(
            listOf(binding.systemThemeSelected),
            listOf(binding.lightThemeSelected, binding.darkThemeSelected)
        )

        val selectedBackground = R.drawable.background_transparent_highlight_stroke
        val unSelectedBackground = R.drawable.gray_menu_item_bg
        Utils.setBackground(
            selectedOption,
            selectedBackground,
            unSelectedOption,
            unSelectedBackground
        )
        binding.nightSampleScreen.visibility = View.GONE
        binding.daySampleScreen.visibility = View.VISIBLE
    }

    private val mapOfImage: Map<ImageView, Int> by lazy {
        mapOf(
            binding.darkThemeIcon to R.drawable.icon_unselected_dark_mode,
            binding.lightThemeIcon to R.drawable.icon_unselected_light_mode,
            binding.systemThemeIcon to R.drawable.icon_unselected_system_mode,
        )
    }
}