package com.mindmatrix.shishusneh.util

import android.content.Context
import android.content.SharedPreferences

/**
 * ProfilePreferences — Manages the active baby profile.
 *
 * For Phase 4 (Multiple Babies Support), we need to track which
 * baby's dashboard is currently being viewed. This simple wrapper
 * around SharedPreferences stores the active profile ID.
 */
class ProfilePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("shishu_sneh_prefs", Context.MODE_PRIVATE)

    /**
     * The currently active baby profile ID.
     * Returns 0 if no profile is active (e.g. first launch).
     */
    var activeProfileId: Int
        get() = prefs.getInt("active_profile_id", 0)
        set(value) = prefs.edit().putInt("active_profile_id", value).apply()
}
