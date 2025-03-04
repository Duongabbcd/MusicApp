package com.example.musicapp.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.example.musicapp.R
import com.example.musicapp.activity.MainActivity

abstract  class BaseFragment<T : ViewBinding>(private val inflate: Inflate<T>) : Fragment() {
    protected val binding: T by lazy { inflate(layoutInflater) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        reloadData(false)
    }

    open fun reloadData(isDisplayLoading: Boolean) {
        if(MainActivity.isChangeTheme) {
            return
        }
    }

    open fun openFragment(fragment: Fragment) {
        val transaction = parentFragmentManager.beginTransaction()
        transaction.replace(R.id.frameLayout, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }
}