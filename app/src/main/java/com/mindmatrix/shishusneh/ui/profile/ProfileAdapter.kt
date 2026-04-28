package com.mindmatrix.shishusneh.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mindmatrix.shishusneh.data.local.BabyProfile
import com.mindmatrix.shishusneh.databinding.ItemProfileBinding

class ProfileAdapter(
    private val activeProfileId: Int,
    private val onProfileClick: (BabyProfile) -> Unit,
    private val onEditClick: (BabyProfile) -> Unit
) : ListAdapter<BabyProfile, ProfileAdapter.ProfileViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileViewHolder {
        val binding = ItemProfileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProfileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProfileViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProfileViewHolder(private val binding: ItemProfileBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(profile: BabyProfile) {
            binding.textBabyName.text = profile.babyName
            binding.textActiveStatus.visibility = if (profile.id == activeProfileId) View.VISIBLE else View.GONE
            
            binding.root.setOnClickListener { onProfileClick(profile) }
            binding.btnEdit.setOnClickListener { onEditClick(profile) }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<BabyProfile>() {
            override fun areItemsTheSame(oldItem: BabyProfile, newItem: BabyProfile): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: BabyProfile, newItem: BabyProfile): Boolean = oldItem == newItem
        }
    }
}
