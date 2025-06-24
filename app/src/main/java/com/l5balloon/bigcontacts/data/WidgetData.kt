package com.l5balloon.bigcontacts.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class WidgetData(
    val contactName: String = "",
    val contactLookupUri: String = "",
    val theme: String = WidgetKeys.DEFAULT_THEME_KEY,
    val is4x1: Boolean = false
) : Parcelable {
    fun isValid(): Boolean {
        return contactName.isNotBlank() && contactLookupUri.isNotBlank()
    }
} 