package com.example.barcodescanner.utils

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    var lastTagType: String
        get() = prefs.getString("last_tag_type", "A4") ?: "A4"
        set(value) = prefs.edit().putString("last_tag_type", value).apply()

    var lastUnitType: String
        get() = prefs.getString("last_unit_type", "PCS") ?: "PCS"
        set(value) = prefs.edit().putString("last_unit_type", value).apply()
}
