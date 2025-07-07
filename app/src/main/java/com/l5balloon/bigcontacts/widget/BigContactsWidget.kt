package com.l5balloon.bigcontacts.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.core.net.toUri
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceComposable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.unit.ColorProvider
import com.l5balloon.bigcontacts.R
import com.l5balloon.bigcontacts.WidgetConfigActivity
import com.l5balloon.bigcontacts.data.WidgetData
import com.l5balloon.bigcontacts.data.WidgetKeys
import com.l5balloon.bigcontacts.data.WidgetTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BigContactsWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)

        provideContent {
            val prefs = currentState<Preferences>()
            val contactLookupUri = prefs[WidgetKeys.ContactLookupUri]
            val contactName = contactLookupUri?.let { lookupUri ->
                val name = queryContactName(context, lookupUri)
                name
            }

            val action: Action = if (contactLookupUri != null) {
                actionStartActivity(
                    Intent(Intent.ACTION_VIEW, contactLookupUri.toUri())
                )
            } else {
                val intent = Intent(context, WidgetConfigActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                actionStartActivity(intent)
            }

            val widgetThemeKey = prefs[WidgetKeys.Theme] ?: WidgetKeys.DEFAULT_THEME_KEY
            val backgroundColor = prefs[WidgetKeys.BackgroundColor]
            val textColor = prefs[WidgetKeys.TextColor]
            val widgetTheme = WidgetTheme.THEMES.find { it.key == widgetThemeKey }
                ?: if (backgroundColor != null && textColor != null) {
                    WidgetTheme(
                        key = widgetThemeKey,
                        nameResId = null,
                        name = null,
                        backgroundColor = androidx.compose.ui.graphics.Color(backgroundColor),
                        textColor = androidx.compose.ui.graphics.Color(textColor)
                    )
                } else {
                    WidgetTheme.THEMES.first()
                }

            val tapToConfigureText = context.getString(R.string.tap_to_configure)

            WidgetContent(
                contactName = contactName,
                widgetTheme = widgetTheme,
                tapToConfigureText = tapToConfigureText,
                action = action
            )
        }
    }

    private fun queryContactName(context: Context, lookupUriString: String): String? {
        val uri: Uri = lookupUriString.toUri()
        val projection = arrayOf(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))
            }
        }
        return null
    }

    companion object {
        fun updateWidget(context: Context, appWidgetId: Int, widgetData: WidgetData) {
            CoroutineScope(Dispatchers.IO).launch {
                val glanceManager = GlanceAppWidgetManager(context)
                val glanceId = glanceManager.getGlanceIdBy(appWidgetId)
                if (glanceId != null) {
                    updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                        val mutablePrefs = prefs.toMutablePreferences()
                        mutablePrefs[WidgetKeys.ContactName] = widgetData.contactName
                        mutablePrefs[WidgetKeys.ContactLookupUri] = widgetData.contactLookupUri
                        mutablePrefs[WidgetKeys.Is4x1] = widgetData.is4x1
                        mutablePrefs[WidgetKeys.Theme] = widgetData.theme
                        mutablePrefs[WidgetKeys.BackgroundColor] = widgetData.backgroundColor
                        mutablePrefs[WidgetKeys.TextColor] = widgetData.textColor
                        mutablePrefs
                    }
                    BigContactsWidget().update(context, glanceId)
                }
            }
        }
    }
}

@Composable
@GlanceComposable
private fun WidgetContent(
    contactName: String?,
    widgetTheme: WidgetTheme,
    tapToConfigureText: String,
    action: Action
) {
    val context = LocalContext.current
    val backgroundColor = ColorProvider(widgetTheme.backgroundColor)
    val textColor = widgetTheme.textColor.toArgb()

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(backgroundColor)
            .clickable(action)
    ) {
        AndroidRemoteViews(
            remoteViews = RemoteViews(context.packageName, R.layout.widget_layout_main).apply {
                val textToShow = contactName ?: tapToConfigureText
                setTextViewText(R.id.widget_text, textToShow)
                setTextColor(R.id.widget_text, textColor)
            }
        )
    }
}

class BigContactsWidgetReceiver2x1 : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BigContactsWidget()
}

class BigContactsWidgetReceiver4x1 : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BigContactsWidget()
}