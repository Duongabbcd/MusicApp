package com.example.musicapp.dialog

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.example.musicapp.R
import com.example.musicapp.databinding.DialogSortBinding
import com.example.musicapp.util.Constant
import com.example.service.utils.DisplayMode
import com.example.service.utils.PreferenceUtil
import com.example.musicapp.util.Utils
import com.example.musicapp.util.Utils.getColorFromAttr

class SortAudioDialog(context: Context) :
    Dialog(context) {
    private val binding by lazy { DialogSortBinding.inflate(layoutInflater) }
    private val sortOrder = PreferenceUtil.getInstance(context)?.getSongSortOrder() ?: ""
    private var songSortOrder =
        PreferenceUtil.getInstance(context)?.getSongSortOrder() ?: Constant.ODER_SORT_FILE_DEFAULT
    
    init {
        setContentView(binding.root)
        window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private val CURRENT_SELECTED_COLOR = ContextCompat.getColor(context, R.color.high_light_color)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.dialogSort.layoutParams = Utils.setMarginDialog(binding.dialogSort)

        when (sortOrder) {
            Constant.ODER_SORT_TITLE_ASC, Constant.ODER_SORT_TITLE_DESC -> sortAudiosByTitle()
            Constant.ODER_SORT_DURATION_DESC, Constant.ODER_SORT_DURATION_ASC -> sortAudiosByDuration()
            Constant.ODER_SORT_DATE_ADD_ASC, Constant.ODER_SORT_DATE_ADD_DESC -> sortAudiosByDate()
            Constant.ODER_SORT_FILE_SIZE_DESC, Constant.ODER_SORT_FILE_SIZE_ASC -> sortAudiosBySize()
        }

        displayButtonByCondition()

        binding.sortByAlphabet.setOnClickListener {
            sortAudiosByTitle()
        }

        binding.sortByDate.setOnClickListener {
            sortAudiosByDate()
        }

        binding.sortByDuration.setOnClickListener {
            sortAudiosByDuration()
        }

        binding.sortBySize.setOnClickListener {
            sortAudiosBySize()
        }

        binding.cancelButton.setOnClickListener {
            dismiss()
        }

    }

    private fun sortAudiosBySize() {
        val unselected =
            listOf(binding.sortByDateText, binding.sortByAlphabetText, binding.sortByClockText)
        val unselectedImage =
            listOf(binding.sortByDateIcon, binding.sortByAlphabetIcon, binding.sortByClockIcon)
        val selectedImage = R.drawable.icon_selected_size
        Utils.setImageResource(
            selected = binding.sortBySizeIcon,
            selectedImage = selectedImage,
            unselected = unselectedImage,
            mapOfImage = mapOfImage
        )
        Utils.setTextColor(
            listOf(binding.sortBySizeText),
            unselected,
            R.color.high_light_color,
            R.color.purple_text
        )

        binding.imageLeftText.text = context.resources.getString(R.string.largest)
        binding.imageRightText.text = context.resources.getString(R.string.smallest)

        when (sortOrder) {
            Constant.ODER_SORT_FILE_SIZE_DESC -> {
                displayButtonByCondition(isLeft = true, isRight = false)
            }
            Constant.ODER_SORT_FILE_SIZE_ASC -> {
                displayButtonByCondition(isLeft = false, isRight = true)
            }
            else -> {
                displayButtonByCondition()
            }
        }

        if(Utils.isViewCurrentlySelected(binding.imageLeftText, CURRENT_SELECTED_COLOR)) {
            songSortOrder = Constant.ODER_SORT_FILE_SIZE_DESC
        }
        if(Utils.isViewCurrentlySelected(binding.imageRightText, CURRENT_SELECTED_COLOR)) {
            songSortOrder = Constant.ODER_SORT_FILE_SIZE_ASC
        }

        binding.buttonLeft.setOnClickListener {
            songSortOrder = Constant.ODER_SORT_FILE_SIZE_DESC
            displayButtonByCondition(isLeft = true, isRight = false)
        }
        binding.buttonRight.setOnClickListener {
            songSortOrder = Constant.ODER_SORT_FILE_SIZE_ASC
            displayButtonByCondition(isLeft = false, isRight = true)
        }

        binding.okButton.setOnClickListener {

            sortByCondition(songSortOrder)

            dismiss()
        }
    }

    private fun sortAudiosByDuration() {
        val unselected =
            listOf(binding.sortByDateText, binding.sortBySizeText, binding.sortByAlphabetText)
        val unselectedImage =
            listOf(binding.sortByDateIcon, binding.sortBySizeIcon, binding.sortByAlphabetIcon)
        val selectedImage = R.drawable.icon_selected_clock
        Utils.setImageResource(
            selected = binding.sortByClockIcon,
            selectedImage = selectedImage,
            unselected = unselectedImage,
            mapOfImage = mapOfImage
        )
        Utils.setTextColor(
            listOf(binding.sortByClockText),
            unselected,
            R.color.high_light_color,
            R.color.purple_text
        )

        when (sortOrder) {
            Constant.ODER_SORT_DURATION_DESC -> {
                displayButtonByCondition(isLeft = true, isRight = false)
            }
            Constant.ODER_SORT_DURATION_ASC -> {
                displayButtonByCondition(isLeft = false, isRight = true)
            }
            else -> {
                displayButtonByCondition()
            }
        }

        if(Utils.isViewCurrentlySelected(binding.imageLeftText, CURRENT_SELECTED_COLOR)) {
            songSortOrder  = Constant.ODER_SORT_DURATION_DESC
        }
        if(Utils.isViewCurrentlySelected(binding.imageRightText, CURRENT_SELECTED_COLOR)) {
            songSortOrder = Constant.ODER_SORT_DURATION_ASC
        }

        binding.imageLeftText.text = context.resources.getString(R.string.longest)
        binding.imageRightText.text = context.resources.getString(R.string.shortest)

        binding.buttonLeft.setOnClickListener {
            songSortOrder = Constant.ODER_SORT_DURATION_DESC
            displayButtonByCondition(isLeft = true, isRight = false)
        }
        binding.buttonRight.setOnClickListener {
            songSortOrder = Constant.ODER_SORT_DURATION_ASC
            displayButtonByCondition(isLeft = false, isRight = true)
        }

        binding.okButton.setOnClickListener {
            sortByCondition(songSortOrder)
            dismiss()
        }
    }

    private fun sortAudiosByDate() {
        val unselected =
            listOf(binding.sortByAlphabetText, binding.sortBySizeText, binding.sortByClockText)
        val unselectedImage =
            listOf(binding.sortByAlphabetIcon, binding.sortBySizeIcon, binding.sortByClockIcon)
        val selectedImage = R.drawable.icon_selected_calendar
        Utils.setImageResource(
            selected = binding.sortByDateIcon,
            selectedImage = selectedImage,
            unselected = unselectedImage,
            mapOfImage = mapOfImage
        )
        Utils.setTextColor(
            listOf(binding.sortByDateText),
            unselected,
            R.color.high_light_color,
            R.color.purple_text
        )

        binding.imageLeftText.text = context.resources.getString(R.string.newest)
        binding.imageRightText.text = context.resources.getString(R.string.oldest)

        when (sortOrder) {
            Constant.ODER_SORT_DATE_ADD_DESC -> {
                displayButtonByCondition(isLeft = true, isRight = false)
            }
            Constant.ODER_SORT_DATE_ADD_ASC -> {
                displayButtonByCondition(isLeft = false, isRight = true)
            }
            else -> {
                displayButtonByCondition()
            }
        }
        if(Utils.isViewCurrentlySelected(binding.imageLeftText, CURRENT_SELECTED_COLOR)) {
            songSortOrder  = Constant.ODER_SORT_DATE_ADD_DESC
        }
        if(Utils.isViewCurrentlySelected(binding.imageRightText, CURRENT_SELECTED_COLOR)) {
            songSortOrder = Constant.ODER_SORT_DATE_ADD_ASC
        }

        binding.buttonLeft.setOnClickListener {
            songSortOrder = Constant.ODER_SORT_DATE_ADD_DESC
            displayButtonByCondition(isLeft = true, isRight = false)
        }
        binding.buttonRight.setOnClickListener {
            songSortOrder = Constant.ODER_SORT_DATE_ADD_ASC
            displayButtonByCondition(isLeft = false, isRight = true)
        }

        binding.okButton.setOnClickListener {
            sortByCondition(songSortOrder)
            dismiss()
        }
    }

    private fun sortAudiosByTitle() {
        val unselected =
            listOf(binding.sortByDateText, binding.sortBySizeText, binding.sortByClockText)
        val unselectedImage =
            listOf(binding.sortByDateIcon, binding.sortBySizeIcon, binding.sortByClockIcon)
        val selectedImage = R.drawable.icon_selected_alphabet
        Utils.setImageResource(
            selected = binding.sortByAlphabetIcon,
            selectedImage = selectedImage,
            unselected = unselectedImage,
            mapOfImage = mapOfImage
        )
        Utils.setTextColor(
            listOf(binding.sortByAlphabetText),
            unselected,
            R.color.high_light_color,
            R.color.purple_text
        )

        binding.imageLeftText.text = context.resources.getString(R.string.a_to_z)
        binding.imageRightText.text = context.resources.getString(R.string.z_to_a)

        when (sortOrder) {
            Constant.ODER_SORT_TITLE_ASC -> {
                displayButtonByCondition(isLeft = true, isRight = false)
            }
            Constant.ODER_SORT_TITLE_DESC -> {
                displayButtonByCondition(isLeft = false, isRight = true)
            }
            else -> {
                displayButtonByCondition()
            }
        }

         if(Utils.isViewCurrentlySelected(binding.imageLeftText,CURRENT_SELECTED_COLOR)) {
             songSortOrder  = Constant.ODER_SORT_TITLE_ASC
        }
        if(Utils.isViewCurrentlySelected(binding.imageRightText, CURRENT_SELECTED_COLOR)) {
            songSortOrder = Constant.ODER_SORT_TITLE_DESC
        }

        binding.buttonLeft.setOnClickListener {
            songSortOrder = Constant.ODER_SORT_TITLE_ASC
            displayButtonByCondition(isLeft = true, isRight = false)

        }
        binding.buttonRight.setOnClickListener {
            songSortOrder = Constant.ODER_SORT_TITLE_DESC
            displayButtonByCondition(isLeft = false, isRight = true)
        }
        binding.okButton.setOnClickListener {
            sortByCondition(songSortOrder)
            dismiss()
        }
    }


    private fun sortByCondition(sortOrder: String) {
        PreferenceUtil.getInstance(context)?.setSongSortOrder(sortOrder)
        context.sendBroadcast(Intent(Constant.ACTION_FINISH_DOWNLOAD))
    }

    private fun displayButtonByCondition(isLeft: Boolean = false, isRight: Boolean = false) {
        if (isLeft) {
            binding.buttonLeft.setBackgroundResource(R.drawable.background_corner_left_selected)
            binding.imageLeftIcon.setImageResource(R.drawable.icon_selected_arrow_down)
            binding.imageLeftText.setTextColor(
                ContextCompat.getColor(
                    binding.imageLeftText.context,
                    R.color.high_light_color
                )
            )

            binding.buttonRight.setBackgroundResource(R.drawable.background_corner_right_unselected)
            binding.imageRightIcon.setImageResource(R.drawable.icon_unselected_arrow_up)
            binding.imageRightText.setTextColor(
                context.getColorFromAttr(R.attr.textColor)
            )
        }

        if (isRight) {
            binding.buttonRight.setBackgroundResource(R.drawable.background_corner_right_selected)
            binding.imageRightIcon.setImageResource(R.drawable.icon_selected_arrow_up)
            binding.imageRightText.setTextColor(
                ContextCompat.getColor(
                    binding.imageRightText.context,
                    R.color.high_light_color
                )
            )

            binding.buttonLeft.setBackgroundResource(R.drawable.background_corner_left_unselected)
            binding.imageLeftIcon.setImageResource(R.drawable.icon_unselected_arrow_down)
            binding.imageLeftText.setTextColor(
                context.getColorFromAttr(R.attr.textColor)
            )
        }
    }

    private val mapOfImage: Map<ImageView, Int> = mapOf(
        binding.sortByDateIcon to R.drawable.icon_unselected_calendar,
        binding.sortByAlphabetIcon to R.drawable.icon_unselected_alphabet,
        binding.sortBySizeIcon to R.drawable.icon_unselected_size,
        binding.sortByClockIcon to R.drawable.icon_unselected_clock,
    )
}
