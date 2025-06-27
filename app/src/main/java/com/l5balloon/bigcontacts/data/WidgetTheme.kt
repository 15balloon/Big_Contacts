package com.l5balloon.bigcontacts.data

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.l5balloon.bigcontacts.R

data class WidgetTheme(
    val key: String,
    @StringRes val nameResId: Int?, // 기본 테마만
    val name: String?, // 커스텀 테마만
    val backgroundColor: Color,
    val textColor: Color
) {
    companion object {
        val THEMES = listOf(
            WidgetTheme(WidgetKeys.DEFAULT_THEME_KEY, R.string.theme_blue, null, Color(0xFF007AFF), Color.White),
            WidgetTheme("Green", R.string.theme_green, null, Color(0xFF34C759), Color.White),
            WidgetTheme("Red", R.string.theme_red, null, Color(0xFFFF3B30), Color.White),
            WidgetTheme("Black", R.string.theme_black, null, Color.Black, Color.White),
            WidgetTheme("White", R.string.theme_white, null, Color.White, Color.Black)
        )
    }
} 