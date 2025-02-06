package com.example.musicapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.musicapp.databinding.DrawerMenuItemBinding

class NavigationRVAdapter(
    private val items: ArrayList<NavigationItemModel>,
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<
        NavigationRVAdapter.NavigationItemViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NavigationItemViewHolder {
        val itemBinding =
            DrawerMenuItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NavigationItemViewHolder(itemBinding)
    }

    override fun onBindViewHolder(
        holder: NavigationItemViewHolder,
        position: Int
    ) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = items.size

    inner class NavigationItemViewHolder(private val itemBinding: DrawerMenuItemBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {

        fun bind(position: Int) {
            val menuItem = items[position]
            itemBinding.navLineItem.visibility = View.GONE
            Glide.with(itemBinding.root).load(menuItem.icon)
                .into(itemBinding.functionImg)
            itemBinding.functionName.text = menuItem.title
            itemBinding.root.setOnClickListener {
                onClick(position)
            }
        }
    }
}

data class NavigationItemModel(var icon: Int = 0, var title: String = "")