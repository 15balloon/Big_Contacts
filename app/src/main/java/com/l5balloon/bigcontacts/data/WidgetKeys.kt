package com.l5balloon.bigcontacts.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey

object WidgetKeys {
    private const val PREF_KEY_CONTACT_NAME = "contactName"
    private const val PREF_KEY_CONTACT_LOOKUP_URI = "contactLookupUri"
    private const val PREF_KEY_IS_4X1 = "is4x1"
    private const val PREF_KEY_THEME = "theme"
    private const val PREF_KEY_BACKGROUND_COLOR = "backgroundColor"
    private const val PREF_KEY_TEXT_COLOR = "textColor"

    val ContactName = stringPreferencesKey(PREF_KEY_CONTACT_NAME)
    val ContactLookupUri = stringPreferencesKey(PREF_KEY_CONTACT_LOOKUP_URI)
    val Is4x1 = booleanPreferencesKey(PREF_KEY_IS_4X1)
    val Theme = stringPreferencesKey(PREF_KEY_THEME)
    val BackgroundColor = intPreferencesKey(PREF_KEY_BACKGROUND_COLOR)
    val TextColor = intPreferencesKey(PREF_KEY_TEXT_COLOR)
    const val DEFAULT_THEME_KEY = "Blue"
} 