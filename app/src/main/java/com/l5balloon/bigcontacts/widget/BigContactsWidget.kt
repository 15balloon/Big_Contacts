package com.l5balloon.bigcontacts.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.GlanceComposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.Preferences
import androidx.glance.background
import androidx.glance.currentState
import com.l5balloon.bigcontacts.R
import androidx.glance.action.Action
import androidx.glance.state.GlanceStateDefinition
import com.l5balloon.bigcontacts.WidgetConfigActivity
import com.l5balloon.bigcontacts.data.WidgetData
import com.l5balloon.bigcontacts.data.WidgetKeys
import com.l5balloon.bigcontacts.data.WidgetTheme
import androidx.core.net.toUri

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
                contactLookupUri = contactLookupUri,
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
    contactLookupUri: String?,
    widgetTheme: WidgetTheme,
    tapToConfigureText: String,
    action: Action
) {
    val backgroundColor = ColorProvider(widgetTheme.backgroundColor)
    val textColor = ColorProvider(widgetTheme.textColor)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(backgroundColor)
            .clickable(action),
        contentAlignment = Alignment.Center
    ) {
        if (contactName == null) {
            Text(
                text = tapToConfigureText,
                style = TextStyle(
                    color = textColor,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = contactName,
                    style = TextStyle(
                        color = textColor,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

class BigContactsWidgetReceiver2x1 : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BigContactsWidget()
}

class BigContactsWidgetReceiver4x1 : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BigContactsWidget()
} 