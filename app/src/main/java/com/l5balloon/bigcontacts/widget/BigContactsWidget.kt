package com.l5balloon.bigcontacts.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.TypedValue.COMPLEX_UNIT_DIP
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
import androidx.glance.layout.Alignment
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
            val contactName = prefs[WidgetKeys.ContactName]
            val contactLookupUri = prefs[WidgetKeys.ContactLookupUri]

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
            val widgetTheme = WidgetTheme.THEMES.find { it.key == widgetThemeKey }
                ?: WidgetTheme.THEMES.first()

            val tapToConfigureText = context.getString(R.string.tap_to_configure)

            WidgetContent(
                contactName = contactName,
                widgetTheme = widgetTheme,
                tapToConfigureText = tapToConfigureText,
                action = action
            )
        }
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
    val textSize = if (contactName != null) 36.0f else 24.0f

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(backgroundColor)
            .clickable(action),
        contentAlignment = Alignment.Center
    ) {
        val textToShow = contactName ?: tapToConfigureText

        AndroidRemoteViews(
            remoteViews = RemoteViews(context.packageName, R.layout.widget_text).apply {
                setTextViewText(R.id.widget_text, textToShow)
                setTextColor(R.id.widget_text, textColor)
                setTextViewTextSize(R.id.widget_text, COMPLEX_UNIT_DIP, textSize)
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