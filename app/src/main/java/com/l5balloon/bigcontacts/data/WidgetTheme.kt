package com.l5balloon.bigcontacts.data

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.l5balloon.bigcontacts.R

data class WidgetTheme(
    val key: String,
    @StringRes val nameResId: Int,
    val backgroundColor: Color,
    val textColor: Color
) {
    companion object {
        val THEMES = listOf(
            WidgetTheme(WidgetKeys.DEFAULT_THEME_KEY, R.string.theme_blue, Color(0xFF007AFF), Color.White),
            WidgetTheme("Green", R.string.theme_green, Color(0xFF34C759), Color.White),
            WidgetTheme("Red", R.string.theme_red, Color(0xFFFF3B30), Color.White),
            WidgetTheme("Black", R.string.theme_black, Color.Black, Color.White),
            WidgetTheme("White", R.string.theme_white, Color.White, Color.Black)
        )
    }
} 