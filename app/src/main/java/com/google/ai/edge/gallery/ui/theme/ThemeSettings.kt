/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ai.edge.gallery.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import com.google.ai.edge.gallery.proto.Theme

object ThemeSettings {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME = "selected_theme"
    private lateinit var prefs: SharedPreferences

    val themeOverride = mutableStateOf<Theme>(Theme.THEME_AUTO)

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedTheme = prefs.getInt(KEY_THEME, Theme.THEME_AUTO_VALUE)
        themeOverride.value = Theme.forNumber(savedTheme) ?: Theme.THEME_AUTO
    }

    fun setTheme(theme: Theme) {
        themeOverride.value = theme
        prefs.edit().putInt(KEY_THEME, theme.number).apply()
    }
}
