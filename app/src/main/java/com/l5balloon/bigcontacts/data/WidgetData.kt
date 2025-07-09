package com.l5balloon.bigcontacts.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class WidgetData(
    val contactName: String = "",
    val contactLookupUri: String = "",
    val theme: String = WidgetKeys.DEFAULT_THEME_KEY,
    val is4x1: Boolean = false,
    val backgroundColor: Int = 0,
    val textColor: Int = 0
) : Parcelable