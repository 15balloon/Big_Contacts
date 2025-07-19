package com.l5balloon.bigcontacts.widget

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.l5balloon.bigcontacts.data.WidgetTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.core.net.toUri

// Utility functions for contact-related operations

fun queryContactName(context: Context, lookupUriString: String): String? {
    val uri: Uri = lookupUriString.toUri()
    val projection = arrayOf(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            return cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY))
        }
    }
    return null
}

data class Contact(val id: Long, val lookupKey: String, val name: String)

fun loadContacts(context: Context): List<Contact> {
    val contacts = mutableListOf<Contact>()
    val projection = arrayOf(
        ContactsContract.Contacts._ID,
        ContactsContract.Contacts.LOOKUP_KEY,
        ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
    )
    val cursor = context.contentResolver.query(
        ContactsContract.Contacts.CONTENT_URI,
        projection,
        null,
        null,
        ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + " ASC"
    )
    cursor?.use {
        val idColumn = it.getColumnIndex(ContactsContract.Contacts._ID)
        val lookupKeyColumn = it.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY)
        val nameColumn = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)

        if (idColumn == -1 || lookupKeyColumn == -1 || nameColumn == -1) {
            return emptyList()
        }

        while (it.moveToNext()) {
            val id = it.getLong(idColumn)
            val lookupKey = it.getString(lookupKeyColumn)
            val name = it.getString(nameColumn)

            if (name.isNullOrBlank()) {
                continue
            }

            contacts.add(Contact(id, lookupKey, name))
        }
    }
    return contacts
}

// WidgetTheme DTO and conversion functions for safe serialization
data class WidgetThemeDto(
    val key: String,
    val nameResId: Int?,
    val name: String?,
    val backgroundColor: Int,
    val textColor: Int
)

fun WidgetTheme.toDto() = WidgetThemeDto(key, nameResId, name, backgroundColor.toArgb(), textColor.toArgb())
fun WidgetThemeDto.toTheme() = WidgetTheme(key, nameResId, name, Color(backgroundColor), Color(textColor))

fun List<WidgetTheme>.toJson(): String = Gson().toJson(this.map { it.toDto() })
fun String.toThemeList(): List<WidgetTheme> =
    runCatching {
        Gson().fromJson<List<WidgetThemeDto>>(this, object : TypeToken<List<WidgetThemeDto>>(){}.type)
            .map { it.toTheme() }
    }.getOrDefault(emptyList())