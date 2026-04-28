package com.mindmatrix.shishusneh.ui.profile

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.mindmatrix.shishusneh.databinding.ActivityManageProfilesBinding
import com.mindmatrix.shishusneh.ui.dashboard.DashboardViewModel
import com.mindmatrix.shishusneh.util.ProfilePreferences

class ManageProfilesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageProfilesBinding
    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageProfilesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        val prefs = ProfilePreferences(this)
        
        val adapter = ProfileAdapter(
            activeProfileId = prefs.activeProfileId,
            onProfileClick = { profile ->
                viewModel.switchProfile(profile.id)
                finish()
            },
            onEditClick = { profile ->
                val intent = Intent(this, EditProfileActivity::class.java).apply {
                    putExtra("PROFILE_ID", profile.id)
                }
                startActivity(intent)
            }
        )

        binding.recyclerViewProfiles.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewProfiles.adapter = adapter

        viewModel.allProfiles.observe(this) { profiles ->
            adapter.submitList(profiles)
        }

        binding.buttonAddBaby.setOnClickListener {
            val intent = Intent(this, com.mindmatrix.shishusneh.ui.onboarding.OnboardingActivity::class.java).apply {
                putExtra("IS_ADD_MODE", true)
            }
            startActivity(intent)
        }
    }
}
